package com.example.anpilink

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * お子さんの追加申請結果。
 */
data class FamilyChildRequestResult(
    val status: String,
    val message: String
)

/**
 * お子さんの追加申請を送信する。
 */
object FamilyChildRequestApi {

    private const val REQUEST_URL =
        ApiConfig.BASE_URL + "/api/family/ChildRequest.action"

    suspend fun submit(
        session: LoginSession,
        studentLoginId: String
    ): FamilyChildRequestResult {
        val normalizedId = studentLoginId.trim()

        require(
            normalizedId.isNotBlank() &&
                    normalizedId.length <= 40
        ) {
            "生徒IDを1～40文字で入力してください。"
        }

        try {
            val accessToken = session.getValidAccessToken()

            return try {
                sendRequest(
                    accessToken = accessToken,
                    studentLoginId = normalizedId
                )
            } catch (e: FamilyApiException) {
                if (e.statusCode != 401) {
                    throw e
                }

                // 認証で拒否された場合だけ、更新後に1回再送信する。
                val refreshedToken =
                    session.refreshAfterUnauthorized(accessToken)

                sendRequest(
                    accessToken = refreshedToken,
                    studentLoginId = normalizedId
                )
            }
        } catch (e: RefreshApiException) {
            throw FamilyApiException(
                e.statusCode,
                e.message ?: "再度ログインしてください。"
            )
        }
    }

    private suspend fun sendRequest(
        accessToken: String,
        studentLoginId: String
    ): FamilyChildRequestResult = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("studentLoginId", studentLoginId)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val connection = URL(REQUEST_URL)
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

            connection.setFixedLengthStreamingMode(
                requestBody.size
            )

            connection.outputStream.use { output ->
                output.write(requestBody)
            }

            val statusCode = connection.responseCode

            if (statusCode != HttpURLConnection.HTTP_CREATED) {
                connection.errorStream?.close()

                val message = when (statusCode) {
                    400 -> "生徒IDを1～40文字で入力してください。"
                    401 -> "再度ログインしてください。"
                    403 -> "追加申請を行う権限がありません。"
                    409 ->
                        "申請できませんでした。" +
                                "登録済み・申請中でないか、" +
                                "生徒IDや所属情報を確認してください。"
                    503 ->
                        "申請結果を確認できませんでした。" +
                                "申請履歴を確認してください。"
                    else ->
                        "申請結果を確認できませんでした。" +
                                "（HTTP $statusCode）"
                }

                throw FamilyApiException(
                    statusCode,
                    message
                )
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader ->
                    reader.readText()
                }

            val body = JSONObject(responseText)
            val status = body.getString("status")

            if (status != "PENDING") {
                throw JSONException(
                    "申請結果の状態が正しくありません。"
                )
            }

            FamilyChildRequestResult(
                status = status,
                message = body.getString("message")
            )
        } finally {
            connection.disconnect()
        }
    }
}