package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * アクセストークンの更新結果。
 * トークンをログに出力しない。
 */
class RefreshResult(
    val accessToken: String,
    val accessExpiresAt: String,
    val refreshExpiresAt: String
)

/**
 * トークン更新APIのHTTPエラー。
 */
class RefreshApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

/**
 * リフレッシュトークンでアクセストークンを更新する。
 */
object RefreshApi {

    private const val REFRESH_URL =
        ApiConfig.BASE_URL + "/api/auth/Refresh.action"

    suspend fun refresh(
        refreshToken: String
    ): RefreshResult = withContext(Dispatchers.IO) {

        val requestBody = JSONObject()
            .put("refreshToken", refreshToken)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val connection = URL(REFRESH_URL)
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

            if (statusCode != HttpURLConnection.HTTP_OK) {
                connection.errorStream?.close()

                val message = when (statusCode) {
                    401 ->
                        "ログインの有効期限が切れました。再度ログインしてください。"

                    503 ->
                        "現在ログインを更新できません。時間をおいてお試しください。"

                    else ->
                        "ログインの更新に失敗しました。（HTTP $statusCode）"
                }

                throw RefreshApiException(
                    statusCode,
                    message
                )
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> reader.readText() }

            val body = JSONObject(responseText)

            val accessToken = body.getString("accessToken")

            if (body.getString("tokenType") != "Bearer"
                || !accessToken.matches(
                    Regex("[A-Za-z0-9_-]{43}")
                )
            ) {
                throw JSONException(
                    "トークン更新の応答形式が正しくありません。"
                )
            }

            // リフレッシュトークン自体は変更しない
            RefreshResult(
                accessToken = accessToken,
                accessExpiresAt =
                    body.getString("accessExpiresAt"),
                refreshExpiresAt =
                    body.getString("refreshExpiresAt")
            )

        } finally {
            connection.disconnect()
        }
    }
}