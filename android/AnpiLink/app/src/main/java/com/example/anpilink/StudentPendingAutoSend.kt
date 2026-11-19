package com.example.anpilink

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * 生徒画面が開いている間、通信復旧後に保存済みの回答を送信する。
 */
@Composable
fun StudentPendingAutoSend(
    session: LoginSession,
    enabled: Boolean,
    onBusyChanged: (Boolean) -> Unit,
    onResult: (PendingSendResult) -> Unit
) {
    val context = LocalContext.current.applicationContext

    LaunchedEffect(session, enabled) {
        if (!enabled || session.role != "STUDENT") {
            return@LaunchedEffect
        }

        observeStudentInternet(context).collectLatest { online ->
            if (!online) {
                return@collectLatest
            }

            var retryDelayMs = 30_000L

            while (true) {
                val pending = try {
                    PendingSafetyReportStore.load(
                        context = context,
                        userId = session.userId
                    )
                } catch (e: IOException) {
                    onResult(
                        PendingSendResult(
                            state = PendingSendState.BLOCKED,
                            message =
                                "保存した回答を読み取れませんでした。" +
                                        "通信状態画面で確認してください。"
                        )
                    )
                    return@collectLatest
                }

                if (pending == null) {
                    return@collectLatest
                }

                // 受付済み・要確認の回答を自動で繰り返し送らない。
                if (isAutoSendStopped(context, pending)) {
                    return@collectLatest
                }

                val result: PendingSendResult

                onBusyChanged(true)

                try {
                    result = PendingSafetyReportSender.send(
                        context = context,
                        session = session
                    )
                } catch (e: CancellationException) {
                    throw e
                } finally {
                    onBusyChanged(false)
                }

                when (result.state) {
                    PendingSendState.SENT,
                    PendingSendState.BLOCKED -> {
                        val recorded = stopAutoSend(
                            context = context,
                            pending = pending
                        )

                        onResult(
                            if (recorded) {
                                result
                            } else {
                                result.copy(
                                    message =
                                        (result.message ?: "") +
                                                "\n自動送信の停止状態を保存できませんでした。" +
                                                "通信状態画面で確認してください。"
                                )
                            }
                        )

                        return@collectLatest
                    }

                    PendingSendState.LOGIN_REQUIRED,
                    PendingSendState.NO_PENDING -> {
                        onResult(result)
                        return@collectLatest
                    }

                    PendingSendState.BUSY -> {
                        // 手動送信など、別の送信処理の終了を待つ。
                        delay(5_000L)
                    }

                    PendingSendState.RETRYABLE_ERROR -> {
                        onResult(result)

                        // 通信エラーでは回答を保持し、間隔を空けて再試行する。
                        delay(retryDelayMs)
                        retryDelayMs =
                            (retryDelayMs * 2).coerceAtMost(300_000L)
                    }
                }
            }
        }
    }
}

/**
 * 端末のインターネット接続状態を監視する。
 * サーバーの受付成功は、送信処理の応答で別途確認する。
 */
private fun observeStudentInternet(
    context: Context
): Flow<Boolean> {
    val manager = context.getSystemService(
        ConnectivityManager::class.java
    )

    // Android 7.0未満では一定間隔で確認する。
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        return flow {
            while (true) {
                emit(hasStudentInternet(manager))
                delay(5_000L)
            }
        }.distinctUntilChanged()
    }

    return callbackFlow {
        if (manager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                trySend(
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    ) &&
                            capabilities.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_VALIDATED
                            )
                )
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        try {
            trySend(hasStudentInternet(manager))
            manager.registerDefaultNetworkCallback(callback)
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            manager.unregisterNetworkCallback(callback)
        }
    }.conflate().distinctUntilChanged()
}

private fun hasStudentInternet(
    manager: ConnectivityManager?
): Boolean {
    if (manager == null) {
        return false
    }

    val network = manager.activeNetwork ?: return false
    val capabilities =
        manager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(
        NetworkCapabilities.NET_CAPABILITY_INTERNET
    ) &&
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
}

/**
 * 停止記録は、接続先・利用者・送信IDごとに分ける。
 * 回答内容やアクセストークンは保存しない。
 */
private fun autoSendStopKey(
    pending: PendingSafetyReport
): String {
    return pending.serverBaseUrl +
            "|" + pending.userId +
            "|" + pending.requestId
}

private suspend fun isAutoSendStopped(
    context: Context,
    pending: PendingSafetyReport
): Boolean = withContext(Dispatchers.IO) {
    context.getSharedPreferences(
        "student_pending_auto_send",
        Context.MODE_PRIVATE
    ).getBoolean(autoSendStopKey(pending), false)
}

private suspend fun stopAutoSend(
    context: Context,
    pending: PendingSafetyReport
): Boolean = withContext(Dispatchers.IO) {
    context.getSharedPreferences(
        "student_pending_auto_send",
        Context.MODE_PRIVATE
    ).edit()
        .putBoolean(autoSendStopKey(pending), true)
        .commit()
}