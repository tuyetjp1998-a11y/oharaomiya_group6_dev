package com.example.anpilink

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 保護者の関連付け申請履歴。
 */
data class FamilyChildRequestItem(
    val requestId: Long,
    val studentLoginId: String,
    val status: String,
    val requestedAtText: String,
    val rejectionReason: String?
)

/**
 * ログイン中の保護者の申請履歴を取得する。
 */
object FamilyChildRequestsApi {

    private const val HISTORY_URL =
        ApiConfig.BASE_URL + "/api/family/ChildRequests.action"

    suspend fun getHistory(
        session: LoginSession
    ): List<FamilyChildRequestItem> {
        try {
            val accessToken = session.getValidAccessToken()

            return try {
                requestHistory(accessToken)
            } catch (e: FamilyApiException) {
                if (e.statusCode != 401) {
                    throw e
                }

                // 認証失敗時だけ更新し、1回再取得する。
                val refreshedToken =
                    session.refreshAfterUnauthorized(accessToken)

                requestHistory(refreshedToken)
            }
        } catch (e: RefreshApiException) {
            throw FamilyApiException(
                e.statusCode,
                e.message ?: "再度ログインしてください。"
            )
        }
    }

    private suspend fun requestHistory(
        accessToken: String
    ): List<FamilyChildRequestItem> =
        withContext(Dispatchers.IO) {
            val connection = URL(HISTORY_URL)
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
                        403 -> "申請履歴を閲覧する権限がありません。"
                        503 ->
                            "現在利用できません。" +
                                    "時間をおいてお試しください。"
                        else ->
                            "申請履歴を取得できませんでした。" +
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

                val requests = JSONObject(responseText)
                    .getJSONArray("requests")

                val result =
                    mutableListOf<FamilyChildRequestItem>()

                for (index in 0 until requests.length()) {
                    val item = requests.getJSONObject(index)

                    result.add(
                        FamilyChildRequestItem(
                            requestId =
                                item.getLong("requestId"),
                            studentLoginId =
                                item.getString("studentLoginId"),
                            status =
                                item.getString("status"),
                            requestedAtText =
                                item.getString("requestedAtText"),
                            rejectionReason =
                                if (item.isNull("rejectionReason")) {
                                    null
                                } else {
                                    item.getString("rejectionReason")
                                        .ifBlank { null }
                                }
                        )
                    )
                }

                result
            } finally {
                connection.disconnect()
            }
        }
}
