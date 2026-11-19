package com.example.anpilink

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** 生徒の位置情報・通知設定。後ろの2項目は端末上の表示用。 */
data class StudentSettingsResult(
    val locationEnabled: Boolean,
    val notificationEnabled: Boolean,
    val fromCache: Boolean = false,
    val locationOffPending: Boolean = false
)

class StudentSettingsApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

object StudentSettingsApi {
    private const val SETTINGS_URL =
        ApiConfig.BASE_URL + "/api/student/Settings.action"

    /**
     * Context指定時は設定を端末に保存する。
     * 通信できない場合は、同じ接続先・利用者の保存済み設定を使用する。
     * 401・403などの認証／権限エラーではキャッシュに切り替えない。
     */
    suspend fun getSettings(
        session: LoginSession,
        context: Context? = null
    ): StudentSettingsResult {
        if (context == null) return getRemoteSettings(session)
        require(session.role == "STUDENT") {
            "生徒アカウントでログインしてください。"
        }
        val appContext = context.applicationContext

        if (!StudentSettingsCache.hasNetwork(appContext)) {
            return StudentSettingsCache.load(appContext, session.userId)
                ?: throw IOException("オフラインで利用できる保存済み設定がありません。")
        }

        // オフライン中のOFF操作を、設定の読み込み時にサーバーへ反映する。
        val remote: StudentSettingsResult
        val revision: Long
        try {
            if (StudentSettingsCache.offPending(appContext, session.userId)) {
                updateLocationEnabled(session, false, appContext)
            }
            revision = StudentSettingsCache.revision(appContext, session.userId)
            remote = getRemoteSettings(session)
        } catch (e: StudentSettingsApiException) {
            if (e.statusCode == 408 || e.statusCode == 429 || e.statusCode >= 500) {
                StudentSettingsCache.load(appContext, session.userId)?.let { return it }
            }
            throw e
        } catch (e: IOException) {
            StudentSettingsCache.load(appContext, session.userId)?.let { return it }
            throw e
        }

        return StudentSettingsCache.rememberServerSettings(
            appContext, session.userId, revision, remote
        )
    }

    private suspend fun getRemoteSettings(
        session: LoginSession
    ): StudentSettingsResult = withSessionToken(session) { token ->
        val body = request(token, "GET")
        StudentSettingsResult(
            locationEnabled = body.getBoolean("locationEnabled"),
            notificationEnabled = body.getBoolean("notificationEnabled")
        )
    }

    /** OFFは端末に先に保存する。ONはサーバー確認後だけ反映する。 */
    suspend fun updateLocationEnabled(
        session: LoginSession,
        enabled: Boolean,
        context: Context? = null
    ): Boolean {
        if (context == null) {
            return updateSetting(session, "locationEnabled", enabled)
        }
        val appContext = context.applicationContext
        if (enabled && !StudentSettingsCache.hasNetwork(appContext)) {
            throw IOException("位置情報をONにするには、通信できる状態で設定を開き直してください。")
        }

        val revision = StudentSettingsCache.beginUpdate(
            appContext, session.userId, locationOff = !enabled
        )
        if (!enabled && !StudentSettingsCache.hasNetwork(appContext)) {
            return false
        }

        val saved = updateSetting(session, "locationEnabled", enabled)
        return StudentSettingsCache.confirmUpdate(
            appContext, session.userId, revision,
            locationSetting = true, enabled = saved
        )
    }

    suspend fun updateNotificationEnabled(
        session: LoginSession,
        enabled: Boolean,
        context: Context? = null
    ): Boolean {
        if (context == null) {
            return updateSetting(session, "notificationEnabled", enabled)
        }
        val appContext = context.applicationContext
        if (!StudentSettingsCache.hasNetwork(appContext)) {
            throw IOException("通知設定の変更には通信が必要です。")
        }
        val revision = StudentSettingsCache.beginUpdate(
            appContext, session.userId, locationOff = false
        )
        val saved = updateSetting(session, "notificationEnabled", enabled)
        return StudentSettingsCache.confirmUpdate(
            appContext, session.userId, revision,
            locationSetting = false, enabled = saved
        )
    }

    private suspend fun updateSetting(
        session: LoginSession,
        setting: String,
        enabled: Boolean
    ): Boolean = withSessionToken(session) { token ->
        val response = request(
            token, "POST",
            JSONObject().put("setting", setting).put("enabled", enabled)
        )
        if (response.getString("setting") != setting) {
            throw JSONException("更新された設定項目を確認できませんでした。")
        }
        val savedEnabled = response.getBoolean("enabled")
        if (savedEnabled != enabled) {
            throw JSONException("更新された設定値を確認できませんでした。")
        }
        savedEnabled
    }

    private suspend fun <T> withSessionToken(
        session: LoginSession,
        request: suspend (String) -> T
    ): T {
        try {
            val token = session.getValidAccessToken()
            return try {
                request(token)
            } catch (e: StudentSettingsApiException) {
                if (e.statusCode != 401) throw e
                request(session.refreshAfterUnauthorized(token))
            }
        } catch (e: RefreshApiException) {
            throw StudentSettingsApiException(
                e.statusCode, e.message ?: "再度ログインしてください。"
            )
        }
    }

    private suspend fun request(
        accessToken: String,
        method: String,
        requestBody: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(SETTINGS_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            if (requestBody != null) {
                val bytes = requestBody.toString().toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val statusCode = connection.responseCode
            if (statusCode != HttpURLConnection.HTTP_OK) {
                connection.errorStream?.close()
                val message = when (statusCode) {
                    400 -> "設定内容を確認してください。"
                    401 -> "再度ログインしてください。"
                    403 -> "設定を操作する権限がありません。"
                    503 -> "現在利用できません。時間をおいてお試しください。"
                    else -> "設定の処理に失敗しました。（HTTP $statusCode）"
                }
                throw StudentSettingsApiException(statusCode, message)
            }
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
