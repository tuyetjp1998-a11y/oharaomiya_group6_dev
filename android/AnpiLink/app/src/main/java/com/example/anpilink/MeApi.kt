package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * 認証済み利用者の情報。
 */
data class AuthUser(
    val id: Long,
    val name: String,
    val role: String
)

/**
 * 利用者情報取得APIのHTTPエラー。
 */
class MeApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

/**
 * トークンに対応する最新の利用者情報を取得する。
 */
object MeApi {

    private const val ME_URL =
        ApiConfig.BASE_URL + "/api/auth/Me.action"

    suspend fun getUser(
        accessToken: String
    ): AuthUser = withContext(Dispatchers.IO) {

        val connection = URL(ME_URL)
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
                    401 ->
                        "再度ログインしてください。"

                    403 ->
                        "このアプリを利用する権限がありません。"

                    503 ->
                        "現在利用できません。時間をおいてお試しください。"

                    else ->
                        "利用者情報を取得できませんでした。（HTTP $statusCode）"
                }

                throw MeApiException(
                    statusCode,
                    message
                )
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> reader.readText() }

            val user = JSONObject(responseText)
                .getJSONObject("user")

            val id = user.getLong("id")
            val name = user.getString("name")
            val role = user.getString("role")

            if (id <= 0
                || role !in setOf("STUDENT", "FAMILY")
            ) {
                throw JSONException(
                    "利用者情報の応答形式が正しくありません。"
                )
            }

            AuthUser(
                id = id,
                name = name,
                role = role
            )

        } finally {
            connection.disconnect()
        }
    }
}