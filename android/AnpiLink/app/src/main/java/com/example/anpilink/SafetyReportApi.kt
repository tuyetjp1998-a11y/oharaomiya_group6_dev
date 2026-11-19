package com.example.anpilink

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID


/**
 * 安否回答の登録結果。
 */
data class SafetyReportResult(
    val reportId: Long,
    val status: String,
    val dangerType: String?,
    val reportedAt: String,
    val comment: String? = null,
    val location: StudentReportLocation? = null
)

/**
 * 安否回答APIのHTTPエラー。
 */
class SafetyReportApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

/**
 * 生徒の安否回答を送信する。
 */
object SafetyReportApi {

    private const val SUBMIT_URL =
        ApiConfig.BASE_URL + "/api/student/AnswerSubmit.action"

    private val dangerTypes = setOf(
        "ACCIDENT",
        "EARTHQUAKE",
        "FIRE",
        "LANDSLIDE",
        "STALKING"
    )

    private val worriedTypes = setOf(
        "ACCIDENT",
        "EARTHQUAKE",
        "FIRE",
        "LANDSLIDE",
        "HEATSTROKE"
    )

    /**
     * 設定を確認して位置情報を取得し、回答を送信する。
     * 位置情報が取得できなくても回答は送信する。
     */
    suspend fun submit(
        session: LoginSession,
        status: String,
        dangerType: String? = null,
        comment: String? = null,
        context: Context? = null,
        requestId: String = UUID.randomUUID().toString()
    ): SafetyReportResult {

        require(
            requestId.matches(
                Regex(
                    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                            "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                            "[0-9a-fA-F]{12}"
                )
            )
        ) {
            "送信IDの形式が正しくありません。"
        }
        val location = if (context != null) {
            android.util.Log.d(
                "AnpiLinkLocation",
                "位置情報の確認を開始"
            )
            getOptionalLocation(
                context = context,
                session = session
            )
        } else {
            android.util.Log.w(
                "AnpiLinkLocation",
                "Contextなし：呼び出し元のcontext指定を確認"
            )

            null
        }

        try {
            val token = session.getValidAccessToken()

            return try {
                submit(
                    accessToken = token,
                    status = status,
                    dangerType = dangerType,
                    comment = comment,
                    location = location,
                    requestId = requestId
                )
            } catch (e: SafetyReportApiException) {
                if (e.statusCode != 401) {
                    throw e
                }

                // 同じ位置情報で、認証失敗時だけ1回再要求する。
                val updatedToken =
                    session.refreshAfterUnauthorized(token)

                submit(
                    accessToken = updatedToken,
                    status = status,
                    dangerType = dangerType,
                    comment = comment,
                    location = location,
                    requestId = requestId
                )
            }
        } catch (e: RefreshApiException) {
            throw SafetyReportApiException(
                e.statusCode,
                e.message ?: "再度ログインしてください。"
            )
        }
    }
    /**
     * 一時保存した回答を同じ送信ID・内容で送信する。
     * 再送時に位置情報を取り直さない。
     */
    suspend fun submitPending(
        session: LoginSession,
        pending: PendingSafetyReport
    ): SafetyReportResult {

        require(
            session.role == "STUDENT" &&
                    session.userId == pending.userId &&
                    pending.serverBaseUrl == ApiConfig.BASE_URL
        ) {
            "保存した回答の利用者または接続先が一致しません。"
        }

        require(
            pending.requestId.matches(
                Regex(
                    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                            "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                            "[0-9a-fA-F]{12}"
                )
            )
        ) {
            "保存した回答の送信IDが正しくありません。"
        }

        suspend fun sendWithToken(
            token: String
        ): SafetyReportResult {
            return submit(
                accessToken = token,
                status = pending.status,
                dangerType = pending.dangerType,
                comment = pending.comment,
                location = pending.location,
                requestId = pending.requestId
            )
        }

        try {
            val token = session.getValidAccessToken()

            return try {
                sendWithToken(token)

            } catch (e: SafetyReportApiException) {
                if (e.statusCode != 401) {
                    throw e
                }

                val updatedToken =
                    session.refreshAfterUnauthorized(token)

                sendWithToken(updatedToken)
            }

        } catch (e: RefreshApiException) {
            throw SafetyReportApiException(
                e.statusCode,
                e.message ?: "再度ログインしてください。"
            )
        }
    }
    /**
     * 通信を始める前に、送信する回答を端末へ保存する。
     * 未送信の回答がある場合は上書きしない。
     */
    suspend fun preparePending(
        context: Context,
        session: LoginSession,
        status: String,
        dangerType: String? = null,
        comment: String? = null
    ): PendingSafetyReport {
        require(session.role == "STUDENT") {
            "生徒アカウントでログインしてください。"
        }

        val appContext = context.applicationContext

        if (PendingSafetyReportStore.load(
                appContext,
                session.userId
            ) != null
        ) {
            throw IllegalArgumentException(
                "端末に未処理の回答があります。" +
                        "新しい回答で上書きせず、通信状態画面で確認してください。"
            )
        }

        // 取得できない場合も、位置情報なしで回答を保存する。
        val location = getOptionalLocation(
            context = appContext,
            session = session
        )

        return PendingSafetyReportStore.create(
            context = appContext,
            userId = session.userId,
            status = status,
            dangerType = dangerType,
            comment = comment,
            location = location
        )
    }
    /**
     * オフライン時は保存済みの利用設定を使い、その時点の位置を取得する。
     * 過去の座標をキャッシュから補完したり、再送時に取り直したりしない。
     */
    private suspend fun getOptionalLocation(
        context: Context,
        session: LoginSession
    ): StudentReportLocation? {
        return try {
            val settings = StudentSettingsApi.getSettings(
                session = session,
                context = context.applicationContext
            )

            android.util.Log.d(
                "AnpiLinkLocation",
                "位置情報の利用設定：source=" +
                        (if (settings.fromCache) "device" else "server") +
                        ", locationEnabled=${settings.locationEnabled}"
            )

            val location = StudentLocationProvider.getOnce(
                context = context.applicationContext,
                locationEnabled = settings.locationEnabled
            )

            android.util.Log.d(
                "AnpiLinkLocation",
                if (location != null) {
                    "回答時の位置情報を取得成功"
                } else {
                    "回答時の位置情報なし：位置情報なしで回答を保存・送信"
                }
            )
            location
        } catch (e: CancellationException) {
            throw e
        } catch (e: StudentSettingsApiException) {
            android.util.Log.w(
                "AnpiLinkLocation",
                "利用設定の確認失敗：HTTP ${e.statusCode}"
            )
            null
        } catch (e: IOException) {
            android.util.Log.w(
                "AnpiLinkLocation",
                "利用設定を確認できません：" +
                        e.javaClass.simpleName +
                        "。オンラインで設定画面を開き、設定を保存してください。"
            )
            null
        } catch (e: JSONException) {
            android.util.Log.w(
                "AnpiLinkLocation",
                "設定のJSON読み取り失敗"
            )
            null
        }
    }

    suspend fun submit(
        accessToken: String,
        status: String,
        dangerType: String? = null,
        comment: String? = null,
        location: StudentReportLocation? = null,
        requestId: String = UUID.randomUUID().toString()
    ): SafetyReportResult = withContext(Dispatchers.IO) {

        val normalizedComment = comment
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        require(normalizedComment?.contains('\u0000') != true) {
            "コメントに使用できない文字が含まれています。"
        }

        val valid = when (status) {
            "SAFE" ->
                dangerType == null && normalizedComment == null

            "DANGER" ->
                (dangerType == null || dangerType in dangerTypes) &&
                        normalizedComment == null

            "WORRIED" ->
                (dangerType == null || dangerType in worriedTypes) &&
                        (dangerType != null || normalizedComment != null)

            else -> false
        }

        require(valid) {
            "安否回答の内容が正しくありません。"
        }

        val requestJson = JSONObject()
            .put("requestId", requestId)
            .put("status", status)
            .put("dangerType", dangerType ?: JSONObject.NULL)
            .put("comment", normalizedComment ?: JSONObject.NULL)

        // 取得できた場合だけ4項目をまとめて送信する。
        if (location != null) {
            requestJson
                .put("latitude", location.latitude)
                .put("longitude", location.longitude)
                .put("locationAccuracy", location.locationAccuracy)
                .put("locationAcquiredAt", location.locationAcquiredAt)
        }

        val requestBody = requestJson.toString()

        require(requestBody.length <= 8192) {
            "送信内容が長すぎます。コメントを短くしてください。"
        }

        val requestBytes = requestBody.toByteArray(Charsets.UTF_8)

        val connection = URL(SUBMIT_URL)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.doOutput = true

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )
            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setFixedLengthStreamingMode(requestBytes.size)

            connection.outputStream.use { output ->
                output.write(requestBytes)
            }

            val statusCode = connection.responseCode

            if (statusCode != HttpURLConnection.HTTP_CREATED) {
                if (statusCode >= 400) {
                    connection.errorStream?.close()
                } else {
                    connection.inputStream.close()
                }

                val message = when (statusCode) {
                    400 -> "回答内容を確認してください。"
                    401 -> "再度ログインしてください。"
                    403 -> "安否回答を送信する権限がありません。"
                    413 ->
                        "送信内容が長すぎます。" +
                                "コメントを短くしてください。"
                    503 ->
                        "現在利用できません。" +
                                "時間をおいてお試しください。"
                    409 ->
                        "送信IDと回答内容が一致しません。" +
                                "通信状態画面で確認してください。"

                    410 ->
                        "この回答は取り消し済み、または削除済みです。" +
                                "同じ回答の再送信は行いません。"
                    else ->
                        "送信結果を確認できませんでした。" +
                                "（HTTP $statusCode）"
                }

                throw SafetyReportApiException(statusCode, message)
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val body = JSONObject(responseText)

            // 送信した値ではなく、サーバーの保存結果を使用する。
            val savedLocation = if (
                !body.isNull("latitude") &&
                !body.isNull("longitude") &&
                !body.isNull("locationAccuracy") &&
                !body.isNull("locationAcquiredAt")
            ) {
                StudentReportLocation(
                    latitude = body.getDouble("latitude"),
                    longitude = body.getDouble("longitude"),
                    locationAccuracy =
                        body.getDouble("locationAccuracy"),
                    locationAcquiredAt =
                        body.getString("locationAcquiredAt")
                )
            } else {
                null
            }

            SafetyReportResult(
                reportId = body.getLong("reportId"),
                status = body.getString("status"),
                dangerType = if (body.isNull("dangerType")) {
                    null
                } else {
                    body.getString("dangerType")
                },
                reportedAt = body.getString("reportedAt"),
                comment = if (body.isNull("comment")) {
                    null
                } else {
                    body.getString("comment")
                },
                location = savedLocation
            )
        } finally {
            connection.disconnect()
        }
    }
}