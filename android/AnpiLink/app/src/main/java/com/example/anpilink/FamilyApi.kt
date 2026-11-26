package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 保護者に関連付けられた生徒情報。
 */
data class FamilyStudentItem(
    val studentUserId: Long,
    val studentName: String,
    val status: String?,
    val dangerType: String? = null
)

/**
 * 保護者向けAPIのHTTPエラー。
 */
class FamilyApiException(
    val statusCode: Int,
    message: String
) : IOException(message)
/**
 * 安否回答の詳細。
 */
data class FamilyReportDetail(
    val reportId: Long,
    val status: String,
    val dangerType: String?,
    val reportedAt: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracy: Double?,
    val locationAcquiredAt: String?,
    val comment: String? = null,
    val dangerTypeName: String? = null
)

/**
 * 生徒情報と最新の安否回答。
 */
data class FamilyStudentDetail(
    val studentUserId: Long,
    val studentName: String,
    val latestReport: FamilyReportDetail?
)
/**
 * 保護者に関連付けられた生徒一覧を取得する。
 */

sealed interface FamilyNotice {
    val createdAt: String
    val readAt: String?
}

data class FamilySchoolNotice(
    val dispatchId: Long,
    val title: String,
    val message: String,
    override val createdAt: String,
    override val readAt: String? = null
) : FamilyNotice

data class FamilyReminder(
    val reminderId: Long,
    val dispatchId: Long,
    val studentName: String,
    val title: String,
    val deadlineAt: String,
    override val createdAt: String,
    override val readAt: String? = null
) : FamilyNotice

object FamilyApi {

    private const val STUDENTS_URL =
        ApiConfig.BASE_URL + "/api/family/Students.action"

    suspend fun getStudents(
        accessToken: String
    ): List<FamilyStudentItem> = withContext(Dispatchers.IO) {

        val connection = URL(STUDENTS_URL)
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
                    403 -> "生徒情報を閲覧する権限がありません。"
                    503 -> "現在利用できません。時間をおいてお試しください。"
                    else -> "生徒一覧を取得できませんでした。（HTTP $statusCode）"
                }

                throw FamilyApiException(
                    statusCode,
                    message
                )
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> reader.readText() }

            val students = JSONObject(responseText)
                .getJSONArray("students")

            val result = mutableListOf<FamilyStudentItem>()

            for (index in 0 until students.length()) {
                val student = students.getJSONObject(index)

                result.add(
                    FamilyStudentItem(
                        studentUserId =
                            student.getLong("studentUserId"),
                        studentName =
                            student.getString("studentName"),
                        status = if (student.isNull("status")) {
                            null
                        } else {
                            student.getString("status")
                        },
                        dangerType = if (student.isNull("dangerType")) {
                            null
                        } else {
                            student.getString("dangerType")
                        }
                    )
                )
            }

            result
        } finally {
            connection.disconnect()
        }
    }
    /**
     * 必要に応じてトークンを更新して生徒一覧を取得する。
     */
    suspend fun getStudents(
        session: LoginSession
    ): List<FamilyStudentItem> =
        withSessionToken(session) { token ->
            getStudents(accessToken = token)
        }

    /**
     * 必要に応じてトークンを更新して詳細を取得する。
     */
    suspend fun getStudentDetail(
        session: LoginSession,
        studentUserId: Long
    ): FamilyStudentDetail =
        withSessionToken(session) { token ->
            getStudentDetail(
                accessToken = token,
                studentUserId = studentUserId
            )
        }

    /**
     * 401の場合だけトークンを更新し、一度再実行する。
     */
    private suspend fun <T> withSessionToken(
        session: LoginSession,
        request: suspend (String) -> T
    ): T {

        try {
            val token = session.getValidAccessToken()

            return try {
                request(token)

            } catch (e: FamilyApiException) {
                if (e.statusCode != 401) {
                    throw e
                }
                android.util.Log.d(
                    "AnpiLinkAuth",
                    "401を受信。トークンを更新して再実行します。"
                )

                val updatedToken =
                    session.refreshAfterUnauthorized(token)

                request(updatedToken)
            }

        } catch (e: RefreshApiException) {
            throw FamilyApiException(
                e.statusCode,
                e.message ?: "再度ログインしてください。"
            )
        }
    }
    /**
     * 関連付けられた生徒の最新の安否回答を取得する。
     */
    suspend fun getStudentDetail(
        accessToken: String,
        studentUserId: Long
    ): FamilyStudentDetail = withContext(Dispatchers.IO) {

        require(studentUserId > 0) {
            "生徒IDが正しくありません。"
        }

        val detailUrl =
            ApiConfig.BASE_URL +
                    "/api/family/StudentStatus.action" +
                    "?studentUserId=$studentUserId"

        val connection = URL(detailUrl)
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
                    400 -> "生徒IDが正しくありません。"
                    401 -> "再度ログインしてください。"
                    403 -> "生徒情報を閲覧する権限がありません。"
                    404 -> "対象の生徒情報が見つかりません。"
                    503 -> "現在利用できません。時間をおいてお試しください。"
                    else -> "詳細を取得できませんでした。（HTTP $statusCode）"
                }

                throw FamilyApiException(statusCode, message)
            }

            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> reader.readText() }

            val body = JSONObject(responseText)
            val student = body.getJSONObject("student")

            val report = if (body.isNull("latestReport")) {
                null
            } else {
                val value = body.getJSONObject("latestReport")

                FamilyReportDetail(
                    reportId = value.getLong("reportId"),
                    status = value.getString("status"),
                    dangerType = if (value.isNull("dangerType")) {
                        null
                    } else {
                        value.getString("dangerType")
                    },
                    reportedAt = value.getString("reportedAt"),
                    latitude = if (value.isNull("latitude")) {
                        null
                    } else {
                        value.getDouble("latitude")
                    },
                    longitude = if (value.isNull("longitude")) {
                        null
                    } else {
                        value.getDouble("longitude")
                    },
                    locationAccuracy =
                        if (value.isNull("locationAccuracy")) {
                            null
                        } else {
                            value.getDouble("locationAccuracy")
                        },
                    locationAcquiredAt =
                        if (value.isNull("locationAcquiredAt")) {
                            null
                        } else {
                            value.getString("locationAcquiredAt")
                        },
                    comment = if (value.isNull("comment")) {
                        null
                    } else {
                        value.getString("comment")
                    },
                    dangerTypeName = if (value.isNull("dangerTypeName")) {
                        null
                    } else {
                        value.getString("dangerTypeName")
                    }
                )
            }

            FamilyStudentDetail(
                studentUserId = student.getLong("studentUserId"),
                studentName = student.getString("studentName"),
                latestReport = report
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getReminders(session: LoginSession): List<FamilyReminder> =
        withSessionToken(session) { token ->
            withContext(Dispatchers.IO) {
                val connection = URL(ApiConfig.BASE_URL + "/api/family/ReminderList.action")
                    .openConnection() as HttpURLConnection
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
                        throw FamilyApiException(code, "学校からの連絡を取得できませんでした。（HTTP $code）")
                    }
                    val array = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }).getJSONArray("notices")
                    List(array.length()) { index ->
                        val item = array.getJSONObject(index)
                        FamilyReminder(
                            reminderId = item.getLong("reminderId"),
                            dispatchId = item.getLong("dispatchId"),
                            studentName = item.getString("studentName"),
                            title = item.getString("title"),
                            deadlineAt = item.getString("deadlineAt"),
                            createdAt = item.getString("createdAt"),
                            readAt = if (item.isNull("readAt")) null else item.getString("readAt")
                        )
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }

    /** 本人宛ての通常通知を取得する。 */
    suspend fun getSchoolNotices(session: LoginSession): List<FamilySchoolNotice> =
        withSessionToken(session) { token ->
            withContext(Dispatchers.IO) {
                val connection = URL(ApiConfig.BASE_URL + "/api/family/NoticeList.action")
                    .openConnection() as HttpURLConnection
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
                        throw FamilyApiException(code, "学校からの連絡を取得できませんでした。（HTTP $code）")
                    }
                    val array = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }).getJSONArray("notices")
                    List(array.length()) { index ->
                        val item = array.getJSONObject(index)
                        FamilySchoolNotice(
                            dispatchId = item.getLong("dispatchId"),
                            title = item.getString("title"),
                            message = item.getString("message"),
                            createdAt = item.getString("createdAt"),
                            readAt = if (item.isNull("readAt")) null else item.getString("readAt")
                        )
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }

    /** 本人宛ての通知を既読にする。 */
    suspend fun markRead(session: LoginSession, notice: FamilyNotice): Unit =
        withSessionToken(session) { token ->
            withContext(Dispatchers.IO) {
                val path = when (notice) {
                    is FamilyReminder -> "ReminderRead.action?reminderId=${notice.reminderId}"
                    is FamilySchoolNotice -> "NoticeRead.action?dispatchId=${notice.dispatchId}"
                }
                val connection = URL(ApiConfig.BASE_URL + "/api/family/" + path)
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
                        throw FamilyApiException(code, "既読状態を登録できませんでした。（HTTP $code）")
                    }
                    val result = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8)
                        .use { it.readText() })
                    if (!result.optBoolean("read", false)) {
                        throw IOException("既読状態を登録できませんでした。")
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }

    /** 通常通知と催促通知を配信日時の新しい順に表示する。 */
    suspend fun getNotices(session: LoginSession): List<FamilyNotice> {
        val notices = mutableListOf<FamilyNotice>()
        notices.addAll(getReminders(session))
        notices.addAll(getSchoolNotices(session))
        return notices.sortedByDescending {
            java.time.OffsetDateTime.parse(it.createdAt).toInstant()
        }
    }
}
