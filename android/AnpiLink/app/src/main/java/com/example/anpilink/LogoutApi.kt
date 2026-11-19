package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * ログアウトAPIが返したHTTPエラー。
 */
class LogoutApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

/**
 * サーバー側の認証トークンを失効させる。
 */
object LogoutApi {

    private const val LOGOUT_URL =
        ApiConfig.BASE_URL + "/api/auth/Logout.action"

    suspend fun logout(
        refreshToken: String
    ): Unit = withContext(Dispatchers.IO) {

        val requestBody = JSONObject()
            .put("refreshToken", refreshToken)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val connection = URL(LOGOUT_URL)
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

            connection.setFixedLengthStreamingMode(
                requestBody.size
            )

            connection.outputStream.use { output ->
                output.write(requestBody)
            }

            val statusCode = connection.responseCode

            // 失効済みのトークンもAPI側では204を返す
            if (statusCode != HttpURLConnection.HTTP_NO_CONTENT) {
                connection.errorStream?.close()

                val message = when (statusCode) {
                    400 ->
                        "ログアウトの送信内容を確認できませんでした。"

                    503 ->
                        "現在ログアウトできません。時間をおいてお試しください。"

                    else ->
                        "ログアウトに失敗しました。（HTTP $statusCode）"
                }

                throw LogoutApiException(
                    statusCode,
                    message
                )
            }

        } finally {
            connection.disconnect()
        }
    }
}