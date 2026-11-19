package com.example.anpilink

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 設定画面に表示する承認済みの生徒。
 */
data class FamilySettingsStudent(
    val studentUserId: Long,
    val studentName: String
)

/**
 * 保護者の通知設定と生徒一覧。
 */
data class FamilySettingsResult(
    val notificationEnabled: Boolean,
    val students: List<FamilySettingsStudent>,
    val schoolNotificationEnabled: Boolean? = null
)

/**
 * 保護者の設定を取得する。
 */
object FamilySettingsApi {

    private const val SETTINGS_URL =
        ApiConfig.BASE_URL + "/api/family/Settings.action"

    suspend fun getSettings(
        session: LoginSession
    ): FamilySettingsResult {
        try {
            val accessToken = session.getValidAccessToken()

            return try {
                requestSettings(accessToken)
            } catch (e: FamilyApiException) {
                if (e.statusCode != 401) {
                    throw e
                }

                // 認証失敗時だけトークンを更新し、1回再取得する。
                val refreshedToken =
                    session.refreshAfterUnauthorized(accessToken)

                requestSettings(refreshedToken)
            }
        } catch (e: RefreshApiException) {
            throw FamilyApiException(
                e.statusCode,
                e.message ?: "再度ログインしてください。"
            )
        }
    }

    private suspend fun requestSettings(
        accessToken: String
    ): FamilySettingsResult = withContext(Dispatchers.IO) {
        val connection = URL(SETTINGS_URL)
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
                    403 -> "設定を閲覧する権限がありません。"
                    503 -> "現在利用できません。時間をおいてお試しください。"
                    else -> "設定を取得できませんでした。（HTTP $statusCode）"
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
            val studentArray = body.getJSONArray("students")

            val students =
                mutableListOf<FamilySettingsStudent>()

            for (index in 0 until studentArray.length()) {
                val student =
                    studentArray.getJSONObject(index)

                students.add(
                    FamilySettingsStudent(
                        studentUserId =
                            student.getLong("studentUserId"),
                        studentName =
                            student.getString("studentName")
                    )
                )
            }

            FamilySettingsResult(
                notificationEnabled =
                    body.getBoolean("notificationEnabled"),
                students = students,
                schoolNotificationEnabled = if (body.has("schoolNotificationEnabled")) {
                    body.getBoolean("schoolNotificationEnabled")
                } else null
            )
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 通知設定を保存する。
     * 401の場合だけトークンを更新して1回再送する。
     */
    suspend fun updateNotificationEnabled(
        session: LoginSession,
        enabled: Boolean,
        school: Boolean = false
    ): Boolean {
        val key = if (school) "schoolNotificationEnabled" else "notificationEnabled"
        try {
            val accessToken = session.getValidAccessToken()

            return try {
                requestNotificationUpdate(accessToken, key, enabled)
            } catch (e: FamilyApiException) {
                if (e.statusCode != 401) {
                    throw e
                }

                val refreshedToken =
                    session.refreshAfterUnauthorized(accessToken)

                requestNotificationUpdate(refreshedToken, key, enabled)
            }
        } catch (e: RefreshApiException) {
            throw FamilyApiException(
                e.statusCode,
                e.message ?: "再度ログインしてください。"
            )
        }
    }

    /**
     * 通知設定の変更をAPIへ送信する。
     */
    private suspend fun requestNotificationUpdate(
        accessToken: String,
        key: String,
        enabled: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put(key, enabled)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val connection = URL(SETTINGS_URL)
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

            connection.setFixedLengthStreamingMode(requestBody.size)

            connection.outputStream.use { output ->
                output.write(requestBody)
            }

            val statusCode = connection.responseCode

            if (statusCode != HttpURLConnection.HTTP_OK) {
                connection.errorStream?.close()

                val message = when (statusCode) {
                    400 -> "通知設定を確認してください。"
                    401 -> "再度ログインしてください。"
                    403 -> "設定を変更する権限がありません。"
                    503 ->
                        "設定を保存できませんでした。" +
                                "時間をおいてお試しください。"
                    else ->
                        "設定を保存できませんでした。" +
                                "（HTTP $statusCode）"
                }

                throw FamilyApiException(statusCode, message)
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            JSONObject(responseText)
                .getBoolean(key)
        } finally {
            connection.disconnect()
        }
    }
}