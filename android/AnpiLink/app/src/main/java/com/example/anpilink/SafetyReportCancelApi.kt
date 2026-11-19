package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 危険回答の取り消しAPIのHTTPエラー。
 */
class SafetyReportCancelApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

/**
 * 保存済みの危険回答を取り消す。
 */
object SafetyReportCancelApi {

    private const val CANCEL_URL =
        ApiConfig.BASE_URL + "/api/student/AnswerCancel.action"

    /**
     * 認証失敗時だけアクセストークンを更新し、一度再要求する。
     */
    suspend fun cancel(
        session: LoginSession,
        reportId: Long
    ) {
        require(reportId > 0) {
            "回答IDが正しくありません。"
        }

        try {
            val token = session.getValidAccessToken()

            try {
                sendCancellation(
                    accessToken = token,
                    reportId = reportId
                )

            } catch (e: SafetyReportCancelApiException) {
                if (e.statusCode != 401) {
                    throw e
                }

                val updatedToken =
                    session.refreshAfterUnauthorized(token)

                sendCancellation(
                    accessToken = updatedToken,
                    reportId = reportId
                )
            }

        } catch (e: RefreshApiException) {
            throw SafetyReportCancelApiException(
                statusCode = e.statusCode,
                message = e.message
                    ?: "再度ログインしてください。"
            )
        }
    }

    private suspend fun sendCancellation(
        accessToken: String,
        reportId: Long
    ): Unit = withContext(Dispatchers.IO) {

        val requestBody = JSONObject()
            .put("reportId", reportId)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val connection = URL(CANCEL_URL)
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

            connection.setFixedLengthStreamingMode(
                requestBody.size
            )

            connection.outputStream.use { output ->
                output.write(requestBody)
            }

            val statusCode = connection.responseCode

            if (statusCode != HttpURLConnection.HTTP_NO_CONTENT) {
                if (statusCode >= 400) {
                    connection.errorStream?.close()
                } else {
                    connection.inputStream.close()
                }

                val message = when (statusCode) {
                    400 -> "取り消す回答を確認してください。"
                    401 -> "再度ログインしてください。"
                    403 -> "この回答を取り消す権限がありません。"
                    404 -> "取り消す回答が見つかりません。"
                    409 ->
                        "取り消し期限が過ぎたか、" +
                                "取り消しできない回答です。"
                    503 ->
                        "現在利用できません。" +
                                "時間をおいてお試しください。"
                    else ->
                        "取り消し結果を確認できませんでした。" +
                                "（HTTP $statusCode）"
                }

                throw SafetyReportCancelApiException(
                    statusCode = statusCode,
                    message = message
                )
            }

            // HTTP 204にはレスポンス本文がない。
        } finally {
            connection.disconnect()
        }
    }
}