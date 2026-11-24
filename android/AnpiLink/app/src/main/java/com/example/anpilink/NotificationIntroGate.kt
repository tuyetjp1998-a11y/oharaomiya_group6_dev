package com.example.anpilink

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 通知が未許可の場合、利用者ごとに日本時間で1日1回案内する。
 * 通知が許可されている場合はFCM登録を行う。
 */
@Composable
fun NotificationIntroGate(
    session: LoginSession,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val preferences = remember(context) {
        context.applicationContext.getSharedPreferences(
            "anpilink_notification_intro",
            Context.MODE_PRIVATE
        )
    }

    var initialized by rememberSaveable(session.userId) {
        mutableStateOf(false)
    }

    var showIntro by rememberSaveable(session.userId) {
        mutableStateOf(false)
    }

    var requesting by rememberSaveable(session.userId) {
        mutableStateOf(false)
    }

    var openSettings by rememberSaveable(session.userId) {
        mutableStateOf(false)
    }

    var message by rememberSaveable(session.userId) {
        mutableStateOf<String?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        requesting = false

        if (granted) {
            showIntro = false
            message = null
        } else {
            openSettings = true
            message =
                "通知が許可されていません。" +
                        "「通知を設定する」から端末の設定を開くか、" +
                        "「あとで設定する」でホームへ進めます。"
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requesting = false

        if (NotificationManagerCompat.from(context)
                .areNotificationsEnabled()
        ) {
            showIntro = false
            message = null
        } else {
            message =
                "通知が無効になっています。" +
                        "あとで設定することもできます。"
        }
    }

    // 通知の許可状態と、当日の案内表示履歴を確認する。
    LaunchedEffect(session.userId) {
        if (initialized) {
            return@LaunchedEffect
        }

        val notificationsEnabled =
            NotificationManagerCompat.from(context)
                .areNotificationsEnabled()

        val today = LocalDate.now(
            ZoneId.of("Asia/Tokyo")
        ).toString()

        val dateKey = "shown_on_${session.userId}"

        showIntro = withContext(Dispatchers.IO) {
            if (notificationsEnabled
                || preferences.getString(dateKey, null) == today
            ) {
                false
            } else {
                // 案内を開いた日を保存する。
                // 保存失敗時はホームへ進む。
                preferences.edit()
                    .putString(dateKey, today)
                    .commit()
            }
        }

        initialized = true
    }

    // 案内の表示条件とは別に、許可済みのFCM登録を行う。
    LaunchedEffect(session, initialized, showIntro) {
        if (!initialized || showIntro) {
            return@LaunchedEffect
        }

        if (!NotificationManagerCompat.from(context)
                .areNotificationsEnabled()
        ) {
            return@LaunchedEffect
        }

        try {
            val fcmToken = withTimeout(30_000L) {
                suspendCancellableCoroutine<String> { continuation ->
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (continuation.isActive) {
                                if (!task.isSuccessful) {
                                    continuation.resumeWithException(
                                        IOException(
                                            "FCMトークンを取得できませんでした。"
                                        )
                                    )
                                } else {
                                    val token = task.result

                                    if (token.isNullOrBlank()) {
                                        continuation.resumeWithException(
                                            IOException(
                                                "FCMトークンが空です。"
                                            )
                                        )
                                    } else {
                                        continuation.resume(token)
                                    }
                                }
                            }
                        }
                }
            }

            FcmRegistrationApi.register(
                session = session,
                fcmToken = fcmToken
            )

            // FCMトークンや認証トークンは出力しない。
            Log.d(
                "AnpiLinkFCM",
                "FCM登録成功（HTTP 204）"
            )

        } catch (e: TimeoutCancellationException) {
            Log.w(
                "AnpiLinkFCM",
                "FCMトークン取得タイムアウト"
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: FcmRegistrationApiException) {
            Log.w(
                "AnpiLinkFCM",
                "FCM登録失敗: HTTP ${e.statusCode}"
            )

        } catch (e: Exception) {
            // 通知登録に失敗してもホームは利用できる。
            Log.w(
                "AnpiLinkFCM",
                "FCM登録処理失敗: ${e.javaClass.simpleName}"
            )
        }
    }

    if (!initialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

    } else if (showIntro) {
        NotificationIntroContent(
            loading = requesting,
            message = message,
            onEnableNotifications = {
                if (NotificationManagerCompat.from(context)
                        .areNotificationsEnabled()
                ) {
                    showIntro = false

                } else if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU &&
                    !openSettings
                ) {
                    requesting = true

                    permissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )

                } else {
                    requesting = true

                    val intent = Intent(
                        Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    ).putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        context.packageName
                    )

                    settingsLauncher.launch(intent)
                }
            },
            onLater = {
                showIntro = false
            }
        )

    } else {
        content()
    }
}