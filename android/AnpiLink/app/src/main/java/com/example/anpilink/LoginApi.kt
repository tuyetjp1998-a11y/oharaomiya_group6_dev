package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
/**
 * ログイン成功時の認証情報。
 * トークンはメモリ内で保持し、ログに出力しない。
 */
class LoginSession(
    accessToken: String,
    val refreshToken: String,
    accessExpiresAt: String,
    refreshExpiresAt: String,
    val userId: Long,
    val userName: String,
    val role: String
) {

    /**
     * トークンと有効期限をまとめて更新する。
     */
    private class TokenState(
        val accessToken: String,
        val accessExpiresAt: String,
        val refreshExpiresAt: String
    )

    @Volatile
    private var tokenState = TokenState(
        accessToken = accessToken,
        accessExpiresAt = accessExpiresAt,
        refreshExpiresAt = refreshExpiresAt
    )

    private val refreshMutex = Mutex()
    // 再ログインが必要になったことを画面へ通知する
    private val _requiresReauthentication =
        MutableStateFlow(false)

    val requiresReauthentication =
        _requiresReauthentication.asStateFlow()
    val accessToken: String
        get() = tokenState.accessToken

    val accessExpiresAt: String
        get() = tokenState.accessExpiresAt

    val refreshExpiresAt: String
        get() = tokenState.refreshExpiresAt

    /**
     * 有効なアクセストークンを取得する。
     * 有効期限の30秒前から更新する。
     */
    suspend fun getValidAccessToken(): String =
        refreshMutex.withLock {

            val current = tokenState

            val usable = runCatching {
                OffsetDateTime.parse(current.accessExpiresAt)
                    .isAfter(
                        OffsetDateTime.now().plusSeconds(30)
                    )
            }.getOrDefault(false)

            if (usable) {
                current.accessToken
            } else {
                updateAccessToken()
            }
        }

    /**
     * サーバーから401が返った場合に更新する。
     * 別の処理が更新済みなら、そのトークンを使用する。
     */
    suspend fun refreshAfterUnauthorized(
        rejectedAccessToken: String
    ): String = refreshMutex.withLock {

        val current = tokenState

        if (current.accessToken != rejectedAccessToken) {
            current.accessToken
        } else {
            updateAccessToken()
        }
    }

    /**
     * refreshMutexで保護された処理から呼び出す。
     */
    private suspend fun updateAccessToken(): String {

        val result = try {
            RefreshApi.refresh(refreshToken)

        } catch (e: RefreshApiException) {
            // 通信障害ではログアウトせず、認証失効時だけ通知する
            if (e.statusCode == 401) {
                _requiresReauthentication.value = true
            }

            throw e
        }

        tokenState = TokenState(
            accessToken = result.accessToken,
            accessExpiresAt = result.accessExpiresAt,
            refreshExpiresAt = result.refreshExpiresAt
        )

        Log.d(
            "AnpiLinkAuth",
            "アクセストークン更新成功"
        )

        return result.accessToken
    }
}
/**
 * APIが返したHTTPエラー。
 */
class LoginApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

/**
 * AndroidエミュレーターからローカルAPIへ接続する。
 */
object LoginApi {

    private const val LOGIN_URL =
        ApiConfig.BASE_URL + "/api/auth/Login.action"

    suspend fun login(
        loginId: String,
        password: String
    ): LoginSession = withContext(Dispatchers.IO) {

        val requestBody = JSONObject()
            .put("loginId", loginId)
            .put("password", password)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val connection = URL(LOGIN_URL)
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
                    401 -> "ユーザーIDまたはパスワードが正しくありません。"
                    503 -> "現在ログインできません。時間をおいてお試しください。"
                    else -> "ログインに失敗しました。（HTTP $statusCode）"
                }

                throw LoginApiException(statusCode, message)
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> reader.readText() }

            val body = JSONObject(responseText)
            val user = body.getJSONObject("user")

            LoginSession(
                accessToken = body.getString("accessToken"),
                refreshToken = body.getString("refreshToken"),
                accessExpiresAt = body.getString("accessExpiresAt"),
                refreshExpiresAt = body.getString("refreshExpiresAt"),
                userId = user.getLong("id"),
                userName = user.getString("name"),
                role = user.getString("role")
            )
        } finally {
            connection.disconnect()
        }
    }
}