package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * 最後に送信した安否回答と、その位置情報。
 */
data class StudentLatestReportLocation(
    val reportId: Long,
    val reportedAt: String,
    val location: StudentReportLocation?,
    val status: String? = null,
    val dangerType: String? = null,
    val dangerTypeName: String? = null
)

/**
 * 生徒位置情報APIのHTTPエラー。
 */
class StudentLocationApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

/**
 * 生徒本人の最新回答の位置情報を取得する。
 */
object StudentLocationApi {

    private const val LOCATION_URL =
        ApiConfig.BASE_URL + "/api/student/Location.action"

    /**
     * 未回答の場合はnullを返す。
     */
    suspend fun getLatest(
        session: LoginSession
    ): StudentLatestReportLocation? {
        try {
            val token = session.getValidAccessToken()

            return try {
                requestLatest(token)
            } catch (e: StudentLocationApiException) {
                if (e.statusCode != 401) {
                    throw e
                }

                // 認証失敗時だけトークンを更新し、1回再取得する。
                val updatedToken =
                    session.refreshAfterUnauthorized(token)

                requestLatest(updatedToken)
            }
        } catch (e: RefreshApiException) {
            throw StudentLocationApiException(
                e.statusCode,
                e.message ?: "再度ログインしてください。"
            )
        }
    }

    private suspend fun requestLatest(
        accessToken: String
    ): StudentLatestReportLocation? = withContext(Dispatchers.IO) {

        val connection = URL(LOCATION_URL)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )
            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            val statusCode = connection.responseCode

            if (statusCode != HttpURLConnection.HTTP_OK) {
                connection.errorStream?.close()

                val message = when (statusCode) {
                    401 -> "再度ログインしてください。"
                    403 -> "位置情報を閲覧する権限がありません。"
                    503 ->
                        "現在利用できません。" +
                                "時間をおいてお試しください。"
                    else ->
                        "位置情報を取得できませんでした。" +
                                "（HTTP $statusCode）"
                }

                throw StudentLocationApiException(
                    statusCode,
                    message
                )
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val body = JSONObject(responseText)

            if (!body.has("latestReport")) {
                throw JSONException(
                    "最新の回答情報が含まれていません。"
                )
            }

            if (body.isNull("latestReport")) {
                return@withContext null
            }

            val report = body.getJSONObject("latestReport")

            val locationKeys = listOf(
                "latitude",
                "longitude",
                "locationAccuracy",
                "locationAcquiredAt"
            )

            if (locationKeys.any { !report.has(it) }) {
                throw JSONException(
                    "位置情報の項目が不足しています。"
                )
            }

            val nullCount = locationKeys.count {
                report.isNull(it)
            }

            val location = when (nullCount) {
                locationKeys.size -> null

                0 -> {
                    val latitude = report.getDouble("latitude")
                    val longitude = report.getDouble("longitude")
                    val accuracy =
                        report.getDouble("locationAccuracy")

                    if (
                        !latitude.isFinite() ||
                        !longitude.isFinite() ||
                        !accuracy.isFinite() ||
                        latitude !in -90.0..90.0 ||
                        longitude !in -180.0..180.0 ||
                        accuracy < 0
                    ) {
                        throw JSONException(
                            "位置情報の値が正しくありません。"
                        )
                    }

                    StudentReportLocation(
                        latitude = latitude,
                        longitude = longitude,
                        locationAccuracy = accuracy,
                        locationAcquiredAt =
                            report.getString("locationAcquiredAt")
                    )
                }

                else -> throw JSONException(
                    "位置情報の項目が揃っていません。"
                )
            }

            StudentLatestReportLocation(
                reportId = report.getLong("reportId"),
                reportedAt = report.getString("reportedAt"),
                location = location,
                // 旧サーバーで項目がない場合は不明として扱う。
                status = if (report.isNull("status")) null else report.getString("status"),
                dangerType = if (report.isNull("dangerType")) null else report.getString("dangerType"),
                dangerTypeName = if (report.isNull("dangerTypeName")) null else report.getString("dangerTypeName")
            )
        } finally {
            connection.disconnect()
        }
    }
}