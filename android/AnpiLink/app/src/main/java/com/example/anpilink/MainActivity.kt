package com.example.anpilink

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.anpilink.ui.theme.AnpiLinkTheme
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import android.content.Intent
import androidx.compose.runtime.key
class MainActivity : ComponentActivity() {
    // 新しい起動Intentを受け取ったら保護者ホームを再表示する。
    private var homeOpenVersion by mutableStateOf(0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        homeOpenVersion++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnpiFirebaseMessagingService.createNotificationChannel(this)
        // Firebaseとの接続を確認する。一時的な確認用。
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(
                            "AnpiLinkFCM",
                            "FCMトークン取得失敗",
                            task.exception
                        )
                        return@addOnCompleteListener
                    }

                    if (task.result.isNullOrBlank()) {
                        Log.w(
                            "AnpiLinkFCM",
                            "FCMトークンが空です"
                        )
                        return@addOnCompleteListener
                    }

                    // トークン自体はログに出力しない。
                    Log.d(
                        "AnpiLinkFCM",
                        "FCMトークン取得成功"
                    )
                }
        }
        // ローカル開発時に必要なネットワーク権限を確認する
        val localNetworkPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // 許可後、画面のログインボタンから接続する
        }

        val isDebug =
            (applicationInfo.flags and
                    ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (isDebug && Build.VERSION.SDK_INT >= 37) {
            val permission =
                "android.permission.ACCESS_LOCAL_NETWORK"

            if (checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                localNetworkPermission.launch(permission)
            }
        }

        enableEdgeToEdge()

        setContent {
            AnpiLinkTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.White
                ) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        homeOpenVersion = homeOpenVersion
                    )
                }
            }
        }
    }
}

/**
 * ログイン状態を管理し、認証状態に応じて画面を切り替える。
 */
@Composable
private fun LoginScreen(
    modifier: Modifier = Modifier,
    homeOpenVersion: Int = 0
) {
    val appContext = LocalContext.current.applicationContext
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current

    val tokenStore = remember(appContext) {
        TokenStore(appContext)
    }

    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 起動時はログイン状態の復元が終わるまで操作を待つ
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // アクセストークンと利用者情報はメモリ内で保持する
    var session by remember {
        mutableStateOf<LoginSession?>(null)
    }

    val scope = rememberCoroutineScope()

    var showLogoutConfirmation by remember {
        mutableStateOf(false)
    }
    var loggingOut by remember {
        mutableStateOf(false)
    }
    var logoutError by remember {
        mutableStateOf<String?>(null)
    }

    // 認証が失効した場合はログイン画面へ戻す
    LaunchedEffect(session) {
        val observedSession = session
            ?: return@LaunchedEffect

        observedSession.requiresReauthentication.collect { required ->

            if (required && session === observedSession) {
                var message =
                    "ログインの有効期限が切れました。" +
                            "再度ログインしてください。"

                try {
                    tokenStore.clear()

                } catch (e: IOException) {
                    message =
                        "再度ログインしてください。" +
                                "端末に保存した認証情報を削除できませんでした。"
                }

                session = null
                loginId = ""
                password = ""
                showLogoutConfirmation = false
                logoutError = null
                errorMessage = message
            }
        }
    }

    // 保存済みトークンからログイン状態を復元する
    LaunchedEffect(tokenStore) {
        loading = true
        errorMessage = null

        try {
            session = SessionRestorer.restore(tokenStore)

        } catch (e: CancellationException) {
            throw e

        } catch (e: RefreshApiException) {
            errorMessage = e.message
                ?: "再度ログインしてください。"

        } catch (e: MeApiException) {
            errorMessage = e.message
                ?: "利用者情報を確認できませんでした。"

        } catch (e: IOException) {
            errorMessage =
                "ログイン状態を復元できませんでした。" +
                        "接続状況を確認してアプリを開き直すか、" +
                        "再度ログインしてください。"

        } catch (e: JSONException) {
            errorMessage =
                "サーバーの応答を読み取れませんでした。" +
                        "再度ログインしてください。"

        } finally {
            loading = false
        }
    }

    val currentSession = session

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (currentSession == null) {
            Color.White
        } else {
            MaterialTheme.colorScheme.background
        }
    ) {
        if (currentSession == null) {

            // キーボード表示時や小さい画面ではスクロールできる
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                val availableHeight = maxHeight
                var showFamilyRegistration by rememberSaveable { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = availableHeight)
                        .padding(
                            horizontal = 32.dp,
                            vertical = 32.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showFamilyRegistration) {
                        FamilyRegistrationContent(
                            onBackToLogin = { showFamilyRegistration = false }
                        )
                    } else {
                    LoginFormContent(
                        loginId = loginId,
                        password = password,
                        loading = loading,
                        errorMessage = errorMessage,
                        onLoginIdChange = {
                            loginId = it
                        },
                        onPasswordChange = {
                            password = it
                        },
                        onLogin = {
                            if (!loading
                                && loginId.isNotBlank()
                                && password.isNotEmpty()
                            ) {
                                focusManager.clearFocus()

                                loading = true
                                errorMessage = null

                                // ボタンを押した時点の入力値を使用する
                                val submittedLoginId = loginId
                                val submittedPassword = password

                                scope.launch {
                                    try {
                                        val result = LoginApi.login(
                                            loginId = submittedLoginId,
                                            password = submittedPassword
                                        )

                                        // 保存後にログイン済み画面へ進む
                                        tokenStore.save(
                                            result.refreshToken
                                        )

                                        password = ""
                                        session = result

                                    } catch (e: CancellationException) {
                                        throw e

                                    } catch (e: LoginApiException) {
                                        errorMessage = e.message
                                            ?: "ログインに失敗しました。"

                                    } catch (e: IOException) {
                                        errorMessage =
                                            "ログイン処理を完了できませんでした。" +
                                                    "接続状況と端末の保存領域を確認してください。"

                                    } catch (e: JSONException) {
                                        errorMessage =
                                            "サーバーの応答を読み取れませんでした。"

                                    } finally {
                                        loading = false
                                    }
                                }
                            }
                        },
                        onRegister = {
                            focusManager.clearFocus()
                            showFamilyRegistration = true
                        }
                    )}
                }
            }

        } else if (currentSession.role == "STUDENT") {
            NotificationIntroGate(session = currentSession) {
                StudentHomeContent(
                    session = currentSession,
                    loggingOut = loggingOut,
                    logoutError = logoutError,
                    onLogout = {
                        if (!loggingOut) {
                            logoutError = null
                            showLogoutConfirmation = true
                        }
                    }
                )
            }

        } else if (currentSession.role == "FAMILY") {
            NotificationIntroGate(session = currentSession) {
                // 新しい起動IntentごとにHomeを作り直して、
                // 生徒一覧と申請状況をサーバーから取得する。
                // ログイン状態はLoginScreen側で保持する。
                key(currentSession, homeOpenVersion) {
                    FamilyAppContent(
                        session = currentSession,
                        loggingOut = loggingOut,
                        logoutError = logoutError,
                        onLogout = {
                            if (!loggingOut) {
                                logoutError = null
                                showLogoutConfirmation = true
                            }
                        }
                    )
                }
            }

        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "このアプリを利用する権限がありません。"
                )

                logoutError?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    enabled = !loggingOut,
                    onClick = {
                        logoutError = null
                        showLogoutConfirmation = true
                    }
                ) {
                    Text(
                        text = if (loggingOut) {
                            "ログアウト中…"
                        } else {
                            "ログアウト"
                        }
                    )
                }
            }
        }


    // ログアウトの確認
    if (showLogoutConfirmation && currentSession != null) {
        AlertDialog(
            onDismissRequest = {
                showLogoutConfirmation = false
            },
            title = {
                Text("ログアウト")
            },
            text = {
                Text("ログアウトしますか？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmation = false

                        if (!loggingOut) {
                            loggingOut = true
                            logoutError = null

                            scope.launch {
                                try {
                                    LogoutApi.logout(
                                        refreshToken =
                                            currentSession.refreshToken
                                    )

                                    tokenStore.clear()

                                    session = null
                                    loginId = ""
                                    password = ""
                                    errorMessage = null

                                } catch (e: CancellationException) {
                                    throw e

                                } catch (e: LogoutApiException) {
                                    logoutError = e.message
                                        ?: "ログアウトに失敗しました。"

                                } catch (e: IOException) {
                                    logoutError =
                                        "ログアウトの完了を確認できませんでした。" +
                                                "接続状況と端末の保存領域を確認して、" +
                                                "もう一度お試しください。"

                                } finally {
                                    loggingOut = false
                                }
                            }
                        }
                    }
                ) {
                    Text("ログアウト")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmation = false
                    }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }
}
}