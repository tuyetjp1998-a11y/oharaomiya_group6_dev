package com.example.anpilink

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import org.json.JSONException

/**
 * 一時保存した回答の送信状態。
 */
enum class PendingSendState {
    SENT,
    NO_PENDING,
    BUSY,
    LOGIN_REQUIRED,
    RETRYABLE_ERROR,
    BLOCKED
}

/**
 * 送信結果と画面表示用の情報。
 */
data class PendingSendResult(
    val state: PendingSendState,
    val pending: PendingSafetyReport? = null,
    val report: SafetyReportResult? = null,
    val message: String? = null
)

/**
 * 自動送信と手動再送信を共通で処理する。
 * 同時に複数の送信処理を実行しない。
 */
object PendingSafetyReportSender {

    private val sendMutex = Mutex()

    suspend fun send(
        context: Context,
        session: LoginSession
    ): PendingSendResult {

        if (!sendMutex.tryLock()) {
            return PendingSendResult(
                state = PendingSendState.BUSY,
                message = "回答を送信中です。"
            )
        }

        try {
            if (session.role != "STUDENT") {
                return PendingSendResult(
                    state = PendingSendState.BLOCKED,
                    message = "生徒アカウントでログインしてください。"
                )
            }

            val appContext = context.applicationContext

            var pending = try {
                PendingSafetyReportStore.load(
                    context = appContext,
                    userId = session.userId
                )

            } catch (e: IOException) {
                return PendingSendResult(
                    state = PendingSendState.BLOCKED,
                    message =
                        "一時保存した回答を読み取れませんでした。" +
                                "新しい回答で上書きせず、確認してください。"
                )
            }

            if (pending == null) {
                return PendingSendResult(
                    state = PendingSendState.NO_PENDING,
                    message = "未送信の回答はありません。"
                )
            }

            if (session.requiresReauthentication.value) {
                return PendingSendResult(
                    state = PendingSendState.LOGIN_REQUIRED,
                    pending = pending,
                    message =
                        "同じアカウントで再ログインしてください。" +
                                "回答は端末に保存されています。"
                )
            }

            // オフラインでHTTPを開始していない回答は、置き換え可能なまま保持する。
            val online = try {
                val manager = appContext.getSystemService(ConnectivityManager::class.java)
                val network = manager?.activeNetwork
                val caps = if (manager != null && network != null)
                    manager.getNetworkCapabilities(network) else null
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } catch (e: SecurityException) { false }
            if (!online) {
                return PendingSendResult(PendingSendState.RETRYABLE_ERROR, pending = pending,
                    message = "オフラインです。回答を保存しています。通信復旧後に再送信します。")
            }

            val report = try {
                if (!PendingSafetyReportStore.markAttempted(appContext, session.userId, pending.requestId)) {
                    return PendingSendResult(PendingSendState.BUSY,
                        message = "保存した回答が更新されました。もう一度確認してください。")
                }
                pending = pending.copy(attempted = true)
                // 保存時の送信ID・内容・位置情報をそのまま使用する。
                SafetyReportApi.submitPending(
                    session = session,
                    pending = pending
                )

            } catch (e: CancellationException) {
                // 処理が中断されても保存データは残す。
                throw e

            } catch (e: SafetyReportApiException) {
                val state = when {
                    e.statusCode == 401 ->
                        PendingSendState.LOGIN_REQUIRED

                    e.statusCode == 408 ||
                            e.statusCode == 429 ||
                            e.statusCode >= 500 ->
                        PendingSendState.RETRYABLE_ERROR

                    else ->
                        PendingSendState.BLOCKED
                }

                return PendingSendResult(
                    state = state,
                    pending = pending,
                    message = e.message
                        ?: "回答を送信できませんでした。"
                )

            } catch (e: JSONException) {
                return PendingSendResult(
                    state = PendingSendState.RETRYABLE_ERROR,
                    pending = pending,
                    message =
                        "サーバーの受付結果を確認できませんでした。" +
                                "回答は端末に残っています。"
                )

            } catch (e: IOException) {
                return PendingSendResult(
                    state = PendingSendState.RETRYABLE_ERROR,
                    pending = pending,
                    message =
                        "通信できず、送信完了を確認できませんでした。" +
                                "回答は端末に残っています。"
                )

            } catch (e: IllegalArgumentException) {
                return PendingSendResult(
                    state = PendingSendState.BLOCKED,
                    pending = pending,
                    message = e.message
                        ?: "保存した回答内容を確認してください。"
                )
            }

            // サーバー受付成功後だけ一時保存を削除する。
            // 削除失敗を、回答の送信失敗とは扱わない。
            val cleanupMessage = try {
                val removed = PendingSafetyReportStore.remove(
                    context = appContext,
                    userId = session.userId,
                    requestId = pending.requestId
                )

                if (removed) {
                    null
                } else {
                    "送信は完了しましたが、" +
                            "一時保存データの削除を確認できませんでした。"
                }

            } catch (e: IOException) {
                "送信は完了しましたが、" +
                        "一時保存データを削除できませんでした。"
            }

            return PendingSendResult(
                state = PendingSendState.SENT,
                pending = pending,
                report = report,
                message = cleanupMessage
                    ?: "サーバーが回答を受け付けました。"
            )

        } finally {
            sendMutex.unlock()
        }
    }
}