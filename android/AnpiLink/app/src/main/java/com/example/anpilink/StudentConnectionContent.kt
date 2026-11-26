package com.example.anpilink

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val ConnectionNavy = Color(0xFF16304E)
private val ConnectionBlue = Color(0xFF2866C9)
private val ConnectionMuted = Color(0xFF8192A6)
private val ConnectionBorder = Color(0xFFE0E8F2)
private val ConnectionError = Color(0xFFAC3341)

/**
 * HTMLの通信状態画面をComposeで表示する。
 * ヘッダー・フッター・タブ・スクロールは親画面が担当する。
 * ネットワークが復旧しただけでは送信完了にしない。
 */
@Composable
fun StudentConnectionContent(
    session: LoginSession,
    onBack: () -> Unit,
    onBusyChanged: (Boolean) -> Unit = {},
    automaticResult: PendingSendResult? = null,
    automaticSending: Boolean = false,
    onRelogin: (() -> Unit)? = null,
    onAnswerAgain: ((PendingSafetyReport) -> Unit)? = null,
    // 送信履歴を確認した呼び出し元だけがtrueを渡す。
    canReplacePending: Boolean = false
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var network by remember(session) { mutableStateOf(connectionNetwork(context)) }
    var pending by remember(session) { mutableStateOf<PendingSafetyReport?>(null) }
    var sentAnswer by remember(session) { mutableStateOf<PendingSafetyReport?>(null) }
    var loading by remember(session) { mutableStateOf(true) }
    var sending by remember(session) { mutableStateOf(false) }
    var storageError by remember(session) { mutableStateOf(false) }
    var state by remember(session) { mutableStateOf<PendingSendState?>(null) }
    var message by remember(session) { mutableStateOf<String?>(null) }
    var showReplaceDialog by remember(session) { mutableStateOf(false) }
    val localBusy = loading || sending
    val busy = localBusy || automaticSending

    fun applyResult(result: PendingSendResult) {
        state = result.state
        message = result.message
        when (result.state) {
            PendingSendState.SENT -> {
                sentAnswer = result.pending ?: pending
                pending = null
            }
            PendingSendState.NO_PENDING -> pending = null
            else -> result.pending?.let { pending = it }
        }
    }

    LaunchedEffect(session, automaticResult) {
        loading = true
        storageError = false
        try {
            val loaded = PendingSafetyReportStore.load(context, session.userId)
            pending = loaded
            val result = automaticResult
            val sameAnswer = loaded != null &&
                    loaded.requestId == result?.pending?.requestId
            if (result != null &&
                (sameAnswer || (loaded == null && result.state == PendingSendState.SENT))
            ) {
                applyResult(result)
                // ディスク上の送信開始記録を古い画面通知で巻き戻さない。
                if (loaded != null && result.state != PendingSendState.SENT &&
                    result.state != PendingSendState.NO_PENDING) pending = loaded
            }
        } catch (e: IOException) {
            storageError = true
            message = "保存した回答を読み取れませんでした。データを削除せず、確認してください。"
        } finally {
            loading = false
        }
    }

    // 自動送信のbusyを親へ戻すと自動処理を自分で停止するため、ローカル分だけ通知する。
    LaunchedEffect(localBusy) { onBusyChanged(localBusy) }
    DisposableEffect(session, onBusyChanged) {
        onDispose { onBusyChanged(false) }
    }

    DisposableEffect(context, session) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val handler = Handler(Looper.getMainLooper())
        var active = true
        var registered = false
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(n: Network, caps: NetworkCapabilities) {
                val label = if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) "オンライン" else "接続確認中"
                handler.post { if (active) network = label }
            }
            override fun onLost(n: Network) {
                handler.post { if (active) network = "オフライン" }
            }
        }
        if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                manager.registerDefaultNetworkCallback(callback)
                registered = true
            } catch (e: SecurityException) {
                network = "確認できません"
            }
        }
        onDispose {
            active = false
            if (registered && manager != null) manager.unregisterNetworkCallback(callback)
        }
    }

    fun recheckAndSend() {
        if (busy) return
        sending = true
        message = null
        scope.launch {
            try {
                network = connectionNetwork(context)
                pending = PendingSafetyReportStore.load(context, session.userId)
                storageError = false
                if (pending == null) {
                    if (state != PendingSendState.SENT) state = PendingSendState.NO_PENDING
                    message = "通信状態を再確認しました。"
                } else if (state == PendingSendState.BLOCKED || state == PendingSendState.SENT) {
                    message = "保存データの確認が必要です。同じ回答の再送信は停止しています。"
                } else {
                    // オフラインでも押せる。実際の送信結果で状態を判定する。
                    applyResult(PendingSafetyReportSender.send(context, session))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                storageError = true
                message = "保存データを確認できませんでした。データを削除せず、確認してください。"
            } finally {
                network = connectionNetwork(context)
                sending = false
            }
        }
    }

    // 受付済みで保留回答がなく、端末がオンラインなら通常のオンライン画面へ戻す。
    val onlineIdle = network == "オンライン" && pending == null && !busy && !storageError &&
            (state == null || state == PendingSendState.SENT || state == PendingSendState.NO_PENDING)
    val title = when {
        storageError -> "保存データの確認が必要です"
        loading -> "確認中"
        sending || automaticSending -> "送信中"
        onlineIdle -> "オンライン"
        state == PendingSendState.SENT -> "送信完了"
        state == PendingSendState.BLOCKED -> "回答の確認が必要です"
        state == PendingSendState.LOGIN_REQUIRED -> "一時保存済み・受付未確認"
        state == PendingSendState.RETRYABLE_ERROR && pending != null -> "送信完了を確認できません"
        pending != null -> "一時保存済み・受付未確認"
        network == "オフライン" -> "オフライン"
        else -> network
    }
    val description = when {
        storageError -> "回答の保存状況を確認してください。"
        loading -> "端末に保存した回答を確認しています。"
        sending || automaticSending -> "サーバーの受付を確認しています。"
        onlineIdle -> "未送信の回答はありません。"
        state == PendingSendState.SENT -> "サーバーが回答を受け付けました。"
        state == PendingSendState.LOGIN_REQUIRED -> "再ログインが必要です。回答は端末に保存されています。"
        state == PendingSendState.BLOCKED -> "自動再送信を停止しています。表示された内容を確認してください。"
        pending != null -> "回答は端末に保存されています。通信できる状態で、サーバーの受付確認を再試行します。"
        network == "オフライン" -> "インターネットに接続できません。安否回答は端末に一時保存します。"
        else -> "未送信の回答はありません。"
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("通信状態", Modifier.weight(1f), color = ConnectionNavy,
                fontSize = 26.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onBack, enabled = !busy,
                shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, ConnectionBorder)) {
                Text("← 戻る", color = ConnectionBlue)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("端末の通信", color = ConnectionNavy, fontSize = 14.sp)
            Text(network, color = if (network == "オンライン") Color(0xFF288361) else ConnectionMuted,
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        ConnectionCard {
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, color = ConnectionNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(description, color = ConnectionNavy, fontSize = 14.sp, lineHeight = 24.sp)
                if (pending != null) {
                    Text("学校・保護者への送信完了を確認できていません。",
                        color = ConnectionError, fontSize = 13.sp, lineHeight = 22.sp)
                }
            }
        }
        val content = if (onlineIdle) null
            else if (state == PendingSendState.SENT && pending == null) sentAnswer else pending
        if (content != null) {
            ConnectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (state == PendingSendState.SENT && pending == null) "送信した回答" else "未送信の回答",
                        color = ConnectionNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("安否", color = ConnectionNavy)
                        val badge = when (content.status) {
                            "SAFE" -> Triple("無事", Color(0xFFE5F5EE), Color(0xFF288361))
                            "DANGER" -> Triple("危険", Color(0xFFFBE7E9), ConnectionError)
                            else -> Triple("困っている", Color(0xFFFFF4D5), Color(0xFF765300))
                        }
                        Surface(color = badge.second, shape = RoundedCornerShape(8.dp)) {
                            Text(badge.first, Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = badge.third, fontWeight = FontWeight.Bold)
                        }
                    }
                    ConnectionField("内容", connectionDetail(context, content))
                    ConnectionField("コメント", content.comment ?: "なし")
                    ConnectionField("保存日時", connectionTime(content.savedAt))
                }
            }
        }
        if (state == PendingSendState.LOGIN_REQUIRED) {
            ConnectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("再ログインが必要です", color = ConnectionNavy,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("ログアウトの確認後、同じアカウントで再ログインしてください。保存した回答の送信を再開します。",
                        color = ConnectionNavy, fontSize = 14.sp, lineHeight = 24.sp)
                    Button(onClick = { onRelogin?.invoke() }, enabled = !busy && onRelogin != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectionBlue)) {
                        Text("再ログインする")
                    }
                }
            }
        }
        Button(onClick = { recheckAndSend() }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConnectionBlue, contentColor = Color.White)) {
            Text(if (busy) "確認中…" else "通信状態を再確認", fontWeight = FontWeight.Bold)
        }
        // 通常のオンライン画面では受付成功の重複表示を省く。削除失敗などの説明は残す。
        val feedback = message?.takeUnless {
            onlineIdle && it == "サーバーが回答を受け付けました。"
        }
        feedback?.let {
            Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = if (state == PendingSendState.SENT) Color(0xFF288361) else ConnectionMuted,
                fontSize = 13.sp, lineHeight = 22.sp)
        }
        if (pending != null && state != PendingSendState.SENT) {
            OutlinedButton(onClick = { showReplaceDialog = true },
                enabled = !busy && canReplacePending && pending?.attempted == false &&
                        onAnswerAgain != null && !storageError,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)) {
                Text("回答し直す")
            }
            if (!canReplacePending || pending?.attempted != false) {
                Text("保存中の回答は保持しています。受付状況を確認するまで置き換えはできません。",
                    color = ConnectionMuted, fontSize = 12.sp, lineHeight = 20.sp)
            }
        }
    }
    if (showReplaceDialog) {
        AlertDialog(onDismissRequest = { showReplaceDialog = false },
            title = { Text("未送信の回答があります") },
            text = { Text("新しい回答を保存するまでは、元の回答を残します。新しく回答しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceDialog = false
                    if (!busy && canReplacePending && pending?.attempted == false) pending?.let { onAnswerAgain?.invoke(it) }
                }) { Text("新しく回答する") }
            },
            dismissButton = { TextButton(onClick = { showReplaceDialog = false }) { Text("キャンセル") } })
    }
}

@Composable
private fun ConnectionCard(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, ConnectionBorder)) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun ConnectionField(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(0.3f), color = ConnectionNavy, fontSize = 14.sp)
        Text(value, Modifier.weight(0.7f), color = ConnectionNavy, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

private fun connectionDetail(context: Context, report: PendingSafetyReport): String {
    val value = report.dangerType ?: return "なし"
    if (report.status == "WORRIED") {
        report.dangerTypeName?.takeIf { it.isNotBlank() }?.let { return it }
        readWorriedCatalog(context, "${report.serverBaseUrl}:${report.userId}")
            ?.firstOrNull { it.first == value }?.second?.let { return it }
    }
    return when (value) {
    "ACCIDENT" -> "事故"
    "EARTHQUAKE" -> "地震"
    "FIRE" -> "火災"
    "LANDSLIDE" -> "土砂崩れ"
    "HEATSTROKE" -> "熱中症"
    "STALKING" -> "不審者・つきまとい"
    else -> "種類不明"
    }
}

private fun connectionTime(value: String): String = try {
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.of("Asia/Tokyo"))
        .format(Instant.parse(value))
} catch (e: java.time.DateTimeException) { value }

private fun connectionNetwork(context: Context): String = try {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val network = manager?.activeNetwork
    if (manager == null || network == null) "オフライン" else {
        val caps = manager.getNetworkCapabilities(network)
        if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) "オンライン" else "接続確認中"
    }
} catch (e: SecurityException) { "確認できません" }
