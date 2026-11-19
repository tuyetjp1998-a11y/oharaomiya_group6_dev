package com.example.anpilink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException
import androidx.compose.ui.platform.LocalContext

/**
 * 無事、困っている内容、送信完了を表示する。
 */
@Composable
fun StudentAnswerContent(
    session: LoginSession,
    onHome: () -> Unit,
    onSendingChanged: (Boolean) -> Unit = {}
) {
    var showWorried by rememberSaveable(session.userId) {
        mutableStateOf(false)
    }

    var selectedDetail by rememberSaveable(session.userId) {
        mutableStateOf<String?>(null)
    }

    var comment by rememberSaveable(session.userId) {
        mutableStateOf("")
    }

    var sending by remember(session) {
        mutableStateOf(false)
    }

    var errorMessage by remember(session) {
        mutableStateOf<String?>(null)
    }

    var savedReport by remember(session) {
        mutableStateOf<SafetyReportResult?>(null)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext

    val navy = Color(0xFF16304E)
    val blue = Color(0xFF2866C9)
    val worriedBackground = Color(0xFFFFF4D5)
    val worriedText = Color(0xFF765300)
    val worriedBorder = Color(0xFFA8842B)

    val details = remember {
        listOf(
            "ACCIDENT" to "事故",
            "EARTHQUAKE" to "地震",
            "FIRE" to "火災",
            "LANDSLIDE" to "土砂崩れ",
            "HEATSTROKE" to "熱中症"
        )
    }

    // 画面から離れた場合も親画面の送信中状態を解除する。
    DisposableEffect(session, onSendingChanged) {
        onDispose {
            onSendingChanged(false)
        }
    }

    fun goBack() {
        if (sending) {
            return
        }

        if (savedReport != null) {
            onHome()
        } else if (showWorried) {
            showWorried = false
            errorMessage = null
        } else {
            onHome()
        }
    }

    fun submitAnswer(status: String) {
        if (sending || savedReport != null) {
            return
        }

        val detail = if (status == "WORRIED") {
            selectedDetail
        } else {
            null
        }

        val answerComment = if (status == "WORRIED") {
            comment.trim().takeIf { it.isNotBlank() }
        } else {
            null
        }

        if (status == "WORRIED" &&
            detail == null &&
            answerComment == null
        ) {
            errorMessage =
                "項目を選択するか、コメントを入力してください。"
            return
        }

        sending = true
        onSendingChanged(true)
        errorMessage = null

        scope.launch {
            try {
                // 先に端末へ保存する。保存できなければ送信しない。
                SafetyReportApi.preparePending(
                    context = context,
                    session = session,
                    status = status,
                    dangerType = detail,
                    comment = answerComment
                )

                val result = PendingSafetyReportSender.send(
                    context = context,
                    session = session
                )

                if (result.state == PendingSendState.SENT) {
                    // サーバーが受け付けた回答だけ完了画面に表示する。
                    savedReport = result.report

                    if (savedReport == null) {
                        errorMessage =
                            "サーバーの受付結果を確認できませんでした。"
                    }
                } else {
                    errorMessage = result.message
                        ?: "送信完了を確認できませんでした。" +
                                "回答は端末に保存されています。"
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: SafetyReportApiException) {
                errorMessage = e.message
                    ?: "送信結果を確認できませんでした。"

            } catch (e: IllegalArgumentException) {
                errorMessage = e.message
                    ?: "回答内容を確認してください。"

            } catch (e: JSONException) {
                errorMessage =
                    "サーバーの応答を読み取れませんでした。" +
                            "回答が保存されている場合があります。"

            } catch (e: IOException) {
                errorMessage =
                    "回答の一時保存または送信処理を完了できませんでした。" +
                            "画面を閉じる前に、通信状態画面で保存状況を確認してください。"
            } finally {
                sending = false
                onSendingChanged(false)
            }
        }
    }

    BackHandler {
        goBack()
    }

    val report = savedReport

    if (report != null) {
        StudentAnswerDoneContent(
            report = report,
            onHome = onHome
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (showWorried) {
                    "困っている内容"
                } else {
                    "現在の状況"
                },
                modifier = Modifier.weight(1f),
                color = navy,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                enabled = !sending,
                onClick = {
                    goBack()
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE0E8F2)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = blue
                )
            ) {
                Text("← 戻る")
            }
        }

        if (sending) {
            Text(
                text = "安否を送信しています…",
                color = blue,
                fontSize = 14.sp
            )
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFFC44350),
                fontSize = 13.sp,
                lineHeight = 22.sp
            )
        }

        if (!showWorried) {
            StudentAnswerChoice(
                label = "無事",
                enabled = !sending,
                background = Color(0xFFE5F5EE),
                foreground = Color(0xFF288361),
                borderColor = Color(0xFF348875),
                onClick = {
                    submitAnswer("SAFE")
                }
            )

            StudentAnswerChoice(
                label = "困っている",
                enabled = !sending,
                background = worriedBackground,
                foreground = worriedText,
                borderColor = worriedBorder,
                onClick = {
                    showWorried = true
                    errorMessage = null
                }
            )

        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                details.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { (value, label) ->
                            val selected = selectedDetail == value

                            Surface(
                                onClick = {
                                    // 選択済みの項目を再度押すと解除する。
                                    selectedDetail = if (selected) {
                                        null
                                    } else {
                                        value
                                    }

                                    errorMessage = null
                                },
                                enabled = !sending,
                                selected = selected,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 64.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    worriedBackground
                                } else {
                                    Color.White
                                },
                                contentColor = if (selected) {
                                    worriedText
                                } else {
                                    Color(0xFF384B65)
                                },
                                border = BorderStroke(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) {
                                        worriedBorder
                                    } else {
                                        Color(0xFFE0E8F2)
                                    }
                                )
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 16.sp,
                                        fontWeight = if (selected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // 最後の項目も他と同じ幅にする。
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "コメント",
                    color = navy,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        comment = it
                        errorMessage = null
                    },
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Button(
                enabled = !sending,
                onClick = {
                    submitAnswer("WORRIED")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (sending) {
                        "送信中…"
                    } else {
                        "送信する"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "＠AnpiLink",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = Color(0xFF8192A6),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StudentAnswerChoice(
    label: String,
    enabled: Boolean,
    background: Color,
    foreground: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = foreground,
            disabledContainerColor = background,
            disabledContentColor = foreground.copy(alpha = 0.6f)
        )
    ) {
        Text(
            text = label,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}