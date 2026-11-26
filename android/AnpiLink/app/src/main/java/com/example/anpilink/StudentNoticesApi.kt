package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 学校からの連絡一覧の取得結果。 */
data class StudentDispatchNotice(
    val dispatchId: Long,
    val title: String,
    val message: String,
    val dispatchedAt: String,
    val deadlineAt: String?,
    val readAt: String?,
    val answered: Boolean
)

class StudentNoticesApiException(val statusCode: Int, message: String) : IOException(message)

object StudentNoticesApi {
    private const val LIST_URL = ApiConfig.BASE_URL + "/api/student/DispatchList.action"

    suspend fun getNotices(session: LoginSession): List<StudentDispatchNotice> {
        require(session.role == "STUDENT")
        try {
            val token = session.getValidAccessToken()
            return try {
                fetch(token)
            } catch (e: StudentNoticesApiException) {
                if (e.statusCode != 401) throw e
                fetch(session.refreshAfterUnauthorized(token))
            }
        } catch (e: RefreshApiException) {
            throw StudentNoticesApiException(e.statusCode, e.message ?: "再度ログインしてください。")
        }
    }

    /** 本人宛ての通知を既読にする。401の場合は一度だけ再試行する。 */
    suspend fun markRead(session: LoginSession, dispatchId: Long) {
        require(session.role == "STUDENT" && dispatchId > 0)
        try {
            val token = session.getValidAccessToken()
            try {
                postRead(token, dispatchId)
            } catch (e: StudentNoticesApiException) {
                if (e.statusCode != 401) throw e
                postRead(session.refreshAfterUnauthorized(token), dispatchId)
            }
        } catch (e: RefreshApiException) {
            throw StudentNoticesApiException(e.statusCode, e.message ?: "再度ログインしてください。")
        }
    }

    private suspend fun postRead(token: String, dispatchId: Long): Unit = withContext(Dispatchers.IO) {
        val connection = URL(ApiConfig.BASE_URL + "/api/student/DispatchRead.action?dispatchId=$dispatchId")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                connection.errorStream?.close()
                throw StudentNoticesApiException(code, "既読状態を登録できませんでした。（HTTP $code）")
            }
            val result = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            if (!result.optBoolean("read", false)) throw IOException("既読状態を登録できませんでした。")
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetch(token: String): List<StudentDispatchNotice> = withContext(Dispatchers.IO) {
        val connection = URL(LIST_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                connection.errorStream?.close()
                val message = when (code) {
                    401 -> "再度ログインしてください。"
                    403 -> "連絡を取得する権限がありません。"
                    else -> "学校からの連絡を取得できませんでした。（HTTP $code）"
                }
                throw StudentNoticesApiException(code, message)
            }
            val body = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val array = body.getJSONArray("notices")
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                StudentDispatchNotice(
                    dispatchId = item.getLong("dispatchId"),
                    title = item.getString("title"),
                    message = item.getString("message"),
                    dispatchedAt = item.getString("dispatchedAt"),
                    deadlineAt = if (item.isNull("deadlineAt")) null else item.getString("deadlineAt"),
                    readAt = if (item.isNull("readAt")) null else item.getString("readAt"),
                    answered = item.getBoolean("answered")
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun formatStudentNoticeTime(value: String): String = try {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
        .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
} catch (e: java.time.DateTimeException) {
    value
}
