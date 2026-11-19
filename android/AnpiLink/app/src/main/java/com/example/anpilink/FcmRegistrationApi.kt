package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * FCM登録APIのHTTPエラー。
 */
class FcmRegistrationApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

/**
 * AndroidのFCMトークンをログイン中の利用者に関連付ける。
 */
object FcmRegistrationApi {

    private const val REGISTER_URL =
        ApiConfig.BASE_URL + "/api/push/Register.action"

    suspend fun register(
        session: LoginSession,
        fcmToken: String
    ): Unit = withContext(Dispatchers.IO) {

        if (fcmToken.isBlank()
            || fcmToken.length > 2048
            || !fcmToken.all { it.code in 33..126 }
        ) {
            throw IOException(
                "通知登録情報の形式が正しくありません。"
            )
        }

        val requestBody = JSONObject()
            .put("fcmToken", fcmToken)
            .toString()
            .toByteArray(Charsets.UTF_8)

        var accessToken = session.getValidAccessToken()

        var statusCode = send(
            accessToken = accessToken,
            requestBody = requestBody
        )

        // 認証失効の場合だけ、更新して1回再送する。
        if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            accessToken = session.refreshAfterUnauthorized(
                rejectedAccessToken = accessToken
            )

            statusCode = send(
                accessToken = accessToken,
                requestBody = requestBody
            )
        }

        if (statusCode != HttpURLConnection.HTTP_NO_CONTENT) {
            val message = when (statusCode) {
                400 -> "通知登録情報を確認してください。"
                401 -> "再度ログインしてください。"
                403 -> "通知登録は許可されていません。"
                503 -> "現在通知を登録できません。時間をおいてお試しください。"
                else -> "通知登録に失敗しました。（HTTP $statusCode）"
            }

            throw FcmRegistrationApiException(
                statusCode = statusCode,
                message = message
            )
        }
    }

    /**
     * トークンやレスポンス本文はログに出力しない。
     */
    private fun send(
        accessToken: String,
        requestBody: ByteArray
    ): Int {
        val connection = URL(REGISTER_URL)
            .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.doOutput = true

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )
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

            if (statusCode >= 400) {
                connection.errorStream?.close()
            } else {
                connection.inputStream.close()
            }

            statusCode

        } finally {
            connection.disconnect()
        }
    }
}