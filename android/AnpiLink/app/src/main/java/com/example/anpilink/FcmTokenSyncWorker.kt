package com.example.anpilink

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 現在のFCMトークンをログイン中のアカウントへ登録する。
 * 認証トークンはWorkManagerの入力データに保存しない。
 */
class FcmTokenSyncWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        if (!NotificationManagerCompat.from(applicationContext)
                .areNotificationsEnabled()
        ) {
            return Result.success()
        }

        try {
            val tokenStore = TokenStore(applicationContext)

            val refreshToken = tokenStore.load()
                ?: return Result.success()

            // バックグラウンド処理から保存済み認証情報は削除しない。
            val refreshed = RefreshApi.refresh(refreshToken)

            val user = MeApi.getUser(
                accessToken = refreshed.accessToken
            )

            val session = LoginSession(
                accessToken = refreshed.accessToken,
                refreshToken = refreshToken,
                accessExpiresAt = refreshed.accessExpiresAt,
                refreshExpiresAt = refreshed.refreshExpiresAt,
                userId = user.id,
                userName = user.name,
                role = user.role
            )

            // 古いイベントの値ではなく、現在のFCMトークンを取得する。
            val fcmToken = withTimeoutOrNull(30_000L) {
                getCurrentFcmToken()
            } ?: return retryOrFail()

            // 処理中にログアウト・別アカウントへ変更された場合は中止する。
            if (isStopped || tokenStore.load() != refreshToken) {
                return Result.success()
            }

            FcmRegistrationApi.register(
                session = session,
                fcmToken = fcmToken
            )

            Log.d(
                "AnpiLinkFCM",
                "FCMバックグラウンド登録成功（HTTP 204）"
            )

            return Result.success()

        } catch (e: CancellationException) {
            throw e

        } catch (e: RefreshApiException) {
            return handleHttpError(e.statusCode)

        } catch (e: MeApiException) {
            return handleHttpError(e.statusCode)

        } catch (e: FcmRegistrationApiException) {
            return handleHttpError(e.statusCode)

        } catch (e: IOException) {
            Log.w(
                "AnpiLinkFCM",
                "FCMバックグラウンド通信失敗"
            )
            return retryOrFail()

        } catch (e: Exception) {
            Log.w(
                "AnpiLinkFCM",
                "FCMバックグラウンド登録失敗: " +
                        e.javaClass.simpleName
            )
            return Result.failure()
        }
    }

    /**
     * FCMトークンや認証情報はログへ出力しない。
     */
    private suspend fun getCurrentFcmToken(): String =
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        val token =
                            if (task.isSuccessful) task.result else null

                        if (token.isNullOrBlank()) {
                            continuation.resumeWithException(
                                IOException(
                                    "FCMトークンを取得できませんでした。"
                                )
                            )
                        } else {
                            continuation.resume(token)
                        }
                    }
                }
        }

    private fun handleHttpError(statusCode: Int): Result {
        Log.w(
            "AnpiLinkFCM",
            "FCMバックグラウンド登録失敗: HTTP $statusCode"
        )

        return when {
            statusCode == 408 ||
                    statusCode == 429 ||
                    statusCode in 500..599 -> retryOrFail()

            // 認証失効時は再ログインを待つ。
            else -> Result.failure()
        }
    }

    private fun retryOrFail(): Result {
        // 初回を含めて最大5回試行する。
        return if (runAttemptCount < 4) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME =
            "anpilink_fcm_token_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request =
                OneTimeWorkRequestBuilder<FcmTokenSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }
}