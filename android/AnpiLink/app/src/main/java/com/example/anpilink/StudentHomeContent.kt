package com.example.anpilink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException

private val StudentNavy = Color(0xFF16304E)
private val StudentBlue = Color(0xFF2866C9)
private val StudentMuted = Color(0xFF8192A6)
private val StudentBorder = Color(0xFFE3EBF6)
private val StudentBackground = Color(0xFFF5F8FE)

/**
 * 生徒ホームと各画面を切り替える。
 */
@Composable
fun StudentHomeContent(
    session: LoginSession,
    onLogout: () -> Unit,
    loggingOut: Boolean = false,
    logoutError: String? = null
) {
    var screen by remember(session) {
        mutableStateOf("HOME")
    }

    var replacement by remember(session) { mutableStateOf<PendingSafetyReport?>(null) }

    var sendingDanger by remember(session) {
        mutableStateOf(false)
    }

    var dangerError by remember(session) {
        mutableStateOf<String?>(null)
    }

    var dangerReport by remember(session) {
        mutableStateOf<SafetyReportResult?>(null)
    }

    var answerBusy by remember(session) {
        mutableStateOf(false)
    }

    val onAnswerBusyChanged: (Boolean) -> Unit =
        remember(session) {
            { busy: Boolean ->
                answerBusy = busy
            }
        }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext
    var autoSending by remember(session) {
        mutableStateOf(false)
    }

    var automaticResult by remember(session) {
        mutableStateOf<PendingSendResult?>(null)
    }

    val navigationEnabled =
        !sendingDanger &&
                !answerBusy &&
                !autoSending &&
                !loggingOut

    StudentPendingAutoSend(
        session = session,
        enabled = screen != "REPLACE_ANSWER" &&
                !sendingDanger &&
                !answerBusy &&
                !loggingOut,
        onBusyChanged = { busy ->
            autoSending = busy
        },
        onResult = { result ->
            automaticResult = result
        }
    )

    fun openHome() {
        screen = "HOME"
        dangerReport = null
        dangerError = null
    }

    fun openMore() {
        screen = "MORE"
        dangerReport = null
        dangerError = null
    }

    fun submitDanger() {
        if (sendingDanger || answerBusy || autoSending || loggingOut) {
            return
        }

        sendingDanger = true
        dangerError = null

        coroutineScope.launch {
            try {
                // 危険回答も、通信前に端末へ保存する。
                SafetyReportApi.preparePending(
                    context = context,
                    session = session,
                    status = "DANGER"
                )

                val result = PendingSafetyReportSender.send(
                    context = context,
                    session = session
                )

                if (result.state == PendingSendState.SENT) {
                    val report = result.report

                    if (report != null) {
                        dangerReport = report
                        screen = "DANGER_DONE"
                    } else {
                        dangerError =
                            "サーバーの受付結果を確認できませんでした。"
                    }
                } else {
                    dangerError = result.message
                        ?: "送信完了を確認できませんでした。" +
                                "回答は端末に保存されています。"
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: SafetyReportApiException) {
                dangerError = e.message
                    ?: "送信結果を確認できませんでした。"

            } catch (e: JSONException) {
                dangerError =
                    "サーバーの応答を読み取れませんでした。" +
                            "回答が保存されている場合があります。"

            } catch (e: IOException) {
                dangerError =
                    "回答の一時保存または送信処理を完了できませんでした。" +
                            "画面を閉じる前に、通信状態画面で保存状況を確認してください。"

            } finally {
                sendingDanger = false
            }
        }
    }

    // 回答画面内の戻る操作はStudentAnswerContentが処理する。
    BackHandler(
        enabled = !navigationEnabled ||
                (screen != "HOME" && screen != "ANSWER")
    ) {
        if (navigationEnabled) {
            when (screen) {
                "NOTICE_DETAIL" -> screen = "NOTICES"


                "NOTICES",
                "LOCATION",
                "OFFLINE",
                "REPLACE_ANSWER",
                "SETTINGS" -> openMore()

                else -> openHome()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            color = Color.White,
            contentColor = StudentNavy
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AnpiLink",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = StudentBackground,
            contentColor = StudentNavy
        ) {
            when (screen) {
                "ANSWER" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        StudentAnswerContent(
                            session = session,
                            onHome = { openHome() },
                            onSendingChanged = onAnswerBusyChanged
                        )
                    }
                }

                "DANGER_DONE" -> {
                    val report = dangerReport

                    if (report != null) {
                        StudentScrollablePage {
                            StudentAnswerDoneContent(
                                report = report,
                                onHome = { openHome() },
                                session = session,
                                onBusyChanged = onAnswerBusyChanged
                            )
                        }
                    }
                }

                "NOTICES" -> {
                    StudentScrollablePage {
                        StudentPageHeading(
                            title = "学校からの連絡",
                            onBack = { openMore() }
                        )

                        StudentMenuItem(
                            label = "避難訓練のお知らせ",
                            enabled = navigationEnabled,
                            onClick = {
                                screen = "NOTICE_DETAIL"
                            }
                        )

                        Text(
                            text =
                                "画面確認用のデモです。" +
                                        "実際の学校からの連絡ではありません。",
                            color = StudentMuted,
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                "NOTICE_DETAIL" -> {
                    StudentScrollablePage {
                        StudentPageHeading(
                            title = "学校からの連絡",
                            onBack = {
                                screen = "NOTICES"
                            }
                        )

                        StudentPreviewCard(
                            title = "避難訓練のお知らせ",
                            body =
                                "画面確認用のデモです。\n" +
                                        "学校からの連絡内容は、" +
                                        "配信機能の実装後に表示します。"
                        )
                    }
                }

                "LOCATION" -> {
                    StudentScrollablePage {
                        StudentLocationContent(
                            session = session,
                            onBack = { openMore() }
                        )
                    }
                }

                "REPLACE_ANSWER" -> {
                    val original = replacement
                    if (original != null) {
                        StudentScrollablePage {
                            StudentPendingReplacementContent(
                                session = session,
                                original = original,
                                onBack = { replacement = null; screen = "OFFLINE" },
                                onSaved = {
                                    automaticResult = null
                                    replacement = null
                                    screen = "OFFLINE"
                                },
                                onBusyChanged = onAnswerBusyChanged
                            )
                        }
                    }
                }

                "OFFLINE" -> {
                    StudentScrollablePage {
                        StudentConnectionContent(
                            session = session,
                            onBack = { openMore() },
                            onBusyChanged = onAnswerBusyChanged,
                            automaticResult = automaticResult,
                            automaticSending = autoSending,
                            onRelogin = onLogout,
                            canReplacePending = true,
                            onAnswerAgain = { original ->
                                replacement = original
                                screen = "REPLACE_ANSWER"
                            }
                        )
                    }
                }

                "ONLINE_DEMO" -> {
                    StudentScrollablePage {
                        StudentPageHeading(
                            title = "通信状態",
                            onBack = {
                                screen = "OFFLINE"
                            }
                        )

                        StudentPreviewCard(
                            title = "オンライン（デモ）",
                            body =
                                "通信復旧後の表示例です。\n" +
                                        "実際の回答送信は行っていません。"
                        )
                    }
                }

                "SETTINGS" -> {
                    StudentScrollablePage {
                        StudentSettingsContent(
                            session = session,
                            onBack = { openMore() }
                        )
                    }
                }

                "MORE" -> {
                    StudentScrollablePage {
                        Text(
                            text = "その他",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )

                        logoutError?.let { message ->
                            Text(
                                text = message,
                                color = Color(0xFFAC3341),
                                fontSize = 13.sp,
                                lineHeight = 22.sp
                            )
                        }

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            StudentMenuItem(
                                label = "学校からの連絡",
                                enabled = navigationEnabled,
                                onClick = {
                                    screen = "NOTICES"
                                }
                            )

                            StudentMenuItem(
                                label = "位置情報",
                                enabled = navigationEnabled,
                                onClick = {
                                    screen = "LOCATION"
                                }
                            )

                            StudentMenuItem(
                                label = "通信状態",
                                enabled = navigationEnabled,
                                onClick = {
                                    screen = "OFFLINE"
                                }
                            )

                            StudentMenuItem(
                                label = "設定",
                                enabled = navigationEnabled,
                                onClick = {
                                    screen = "SETTINGS"
                                }
                            )

                            StudentMenuItem(
                                label = if (loggingOut) {
                                    "ログアウト中…"
                                } else {
                                    "ログアウト"
                                },
                                enabled = navigationEnabled,
                                // 親画面の確認ダイアログを使用する。
                                onClick = onLogout
                            )
                        }
                    }
                }

                else -> {
                    StudentHomeBody(
                        userName = session.userName,
                        sending = !navigationEnabled,
                        errorMessage = dangerError,
                        onAnswer = {
                            dangerError = null
                            screen = "ANSWER"
                        },
                        onDanger = { submitDanger() }
                    )
                }
            }
        }

        // 回答・送信完了画面は子コンポーネントのフッターを使う。
        if (screen != "ANSWER" && screen != "DANGER_DONE") {
            Surface(color = StudentBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "＠AnpiLink",
                        color = Color(0xFF617991),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Surface(color = Color.White) {
            Column {
                HorizontalDivider(color = StudentBorder)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .selectableGroup()
                ) {
                    StudentTab(
                        label = "ホーム",
                        icon = "HOME",
                        selected = screen == "HOME",
                        enabled = navigationEnabled,
                        onClick = { openHome() },
                        modifier = Modifier.weight(1f)
                    )

                    StudentTab(
                        label = "その他",
                        icon = "MORE",
                        selected = screen == "MORE",
                        enabled = navigationEnabled,
                        onClick = { openMore() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 各画面共通のスクロール領域。
 */
@Composable
private fun StudentScrollablePage(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content
    )
}

/**
 * タイトルと戻るボタン。
 */
@Composable
private fun StudentPageHeading(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = StudentNavy,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StudentBorder)
        ) {
            Text(
                text = "← 戻る",
                color = StudentBlue,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * その他画面などのメニュー項目。
 */
@Composable
private fun StudentMenuItem(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StudentBorder)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 20.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = StudentNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "›",
                color = StudentMuted,
                fontSize = 26.sp
            )
        }
    }
}

/**
 * 未接続機能の画面確認用カード。
 */
@Composable
private fun StudentPreviewCard(
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StudentBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = StudentNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = body,
                color = StudentNavy,
                fontSize = 14.sp,
                lineHeight = 24.sp
            )
        }
    }
}

/**
 * 生徒ホーム。
 */
@Composable
private fun StudentHomeBody(
    userName: String,
    sending: Boolean,
    errorMessage: String?,
    onAnswer: () -> Unit,
    onDanger: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val minimumHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minimumHeight)
                .padding(
                    start = 24.dp,
                    top = 40.dp,
                    end = 24.dp,
                    bottom = 18.dp
                )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "生徒ホーム",
                    color = Color(0xFF70869F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )

                Text(
                    text = "${userName}さん",
                    color = StudentNavy,
                    fontSize = 28.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 30.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, StudentBorder),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 32.dp
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(18.dp)
                ) {
                    Button(
                        onClick = onAnswer,
                        enabled = !sending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            2.dp,
                            Color(0xFF174C99)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = StudentBlue,
                            disabledContentColor =
                                Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = "安否回答",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Button(
                        onClick = onDanger,
                        enabled = !sending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 88.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            2.dp,
                            Color(0xFFB94D5A)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFBE7E9),
                            contentColor = Color(0xFFAC3341),
                            disabledContainerColor =
                                Color(0xFFFBE7E9),
                            disabledContentColor =
                                Color(0xFFAC3341).copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = if (sending) {
                                "送信中…"
                            } else {
                                "危険"
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = Color(0xFFAC3341),
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 30.dp)
            )
        }
    }
}

@Composable
private fun StudentTab(
    label: String,
    icon: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) {
        Color(0xFF2D6DCD)
    } else {
        Color(0xFF8A9AB0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        StudentLineIcon(type = icon, color = color)

        Spacer(Modifier.height(5.dp))

        Text(
            text = label,
            color = color,
            fontSize = 10.sp
        )
    }
}

/**
 * HTMLのSVGに合わせた線画アイコン。
 */
@Composable
private fun StudentLineIcon(
    type: String,
    color: Color
) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val strokeWidth = 1.7f * scaleX

        fun point(x: Float, y: Float) =
            Offset(x * scaleX, y * scaleY)

        if (type == "HOME") {
            val path = Path().apply {
                moveTo(3f * scaleX, 10f * scaleY)
                lineTo(12f * scaleX, 3f * scaleY)
                lineTo(21f * scaleX, 10f * scaleY)

                moveTo(5f * scaleX, 9f * scaleY)
                lineTo(5f * scaleX, 21f * scaleY)
                lineTo(19f * scaleX, 21f * scaleY)
                lineTo(19f * scaleX, 9f * scaleY)

                moveTo(9f * scaleX, 21f * scaleY)
                lineTo(9f * scaleX, 14f * scaleY)
                lineTo(15f * scaleX, 14f * scaleY)
                lineTo(15f * scaleX, 21f * scaleY)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else {
            for (y in listOf(6f, 12f, 18f)) {
                drawLine(
                    color = color,
                    start = point(5f, y),
                    end = point(19f, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}