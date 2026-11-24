package com.example.anpilink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 保存成功後の完了画面。
 * ホームからの危険回答は送信日時から30秒以内に取り消せる。
 */
@Composable
fun StudentAnswerDoneContent(
    report: SafetyReportResult,
    onHome: () -> Unit,
    session: LoginSession? = null,
    onBusyChanged: (Boolean) -> Unit = {}
) {
    val navy = Color(0xFF16304E)
    val blue = Color(0xFF2866C9)
    val muted = Color(0xFF8192A6)

    val scope = rememberCoroutineScope()

    var cancelling by remember(report.reportId) {
        mutableStateOf(false)
    }

    var cancelled by rememberSaveable(report.reportId) {
        mutableStateOf(false)
    }

    var cancellationRejected by rememberSaveable(report.reportId) {
        mutableStateOf(false)
    }

    var errorMessage by rememberSaveable(report.reportId) {
        mutableStateOf<String?>(null)
    }

    val canOfferCancellation =
        session != null &&
                report.status == "DANGER" &&
                report.dangerType == null

    val deadline = remember(
        report.reportId,
        report.reportedAt
    ) {
        runCatching {
            OffsetDateTime.parse(report.reportedAt)
                .toInstant()
                .plusSeconds(30)
        }.getOrNull()
    }

    fun remainingSeconds(): Int {
        val end = deadline ?: return 0

        val milliseconds = Duration.between(
            Instant.now(),
            end
        ).toMillis()

        if (milliseconds <= 0L) {
            return 0
        }

        return ((milliseconds + 999L) / 1000L)
            .coerceAtMost(30L)
            .toInt()
    }

    var remaining by remember(
        report.reportId,
        report.reportedAt
    ) {
        mutableStateOf(remainingSeconds())
    }

    // 表示用の残り時間。取り消し可否の最終判断はサーバーが行う。
    LaunchedEffect(
        report.reportId,
        report.reportedAt,
        cancelled,
        cancellationRejected,
        canOfferCancellation
    ) {
        if (!canOfferCancellation ||
            cancelled ||
            cancellationRejected
        ) {
            return@LaunchedEffect
        }

        while (true) {
            remaining = remainingSeconds()

            if (remaining == 0) {
                break
            }

            delay(250L)
        }
    }

    val statusLabel: String
    val badgeBackground: Color
    val badgeText: Color

    if (report.status == "SAFE") {
        statusLabel = "無事"
        badgeBackground = Color(0xFFE5F5EE)
        badgeText = Color(0xFF288361)

    } else if (report.status == "WORRIED") {
        val detailLabel = when (report.dangerType) {
            "ACCIDENT" -> "事故"
            "EARTHQUAKE" -> "地震"
            "FIRE" -> "火災"
            "LANDSLIDE" -> "土砂崩れ"
            "HEATSTROKE" -> "熱中症"
            else -> null
        }

        statusLabel = if (detailLabel == null) {
            "困っている"
        } else {
            "困っている（$detailLabel）"
        }

        badgeBackground = Color(0xFFFFF4D5)
        badgeText = Color(0xFF765300)

    } else if (report.status == "WORRIED") {
        val detailLabel = when (report.dangerType) {
            "ACCIDENT" -> "事故"
            "EARTHQUAKE" -> "地震"
            "FIRE" -> "火災"
            "LANDSLIDE" -> "土砂崩れ"
            "HEATSTROKE" -> "熱中症"
            else -> null
        }

        statusLabel = if (detailLabel == null) {
            "困っている"
        } else {
            "困っている（$detailLabel）"
        }

        badgeBackground = Color(0xFFFFF4D5)
        badgeText = Color(0xFF765300)

    } else if (report.status == "WORRIED") {
        val detailLabel = when (report.dangerType) {
            "ACCIDENT" -> "事故"
            "EARTHQUAKE" -> "地震"
            "FIRE" -> "火災"
            "LANDSLIDE" -> "土砂崩れ"
            "HEATSTROKE" -> "熱中症"
            else -> null
        }

        statusLabel = if (detailLabel == null) {
            "困っている"
        } else {
            "困っている（$detailLabel）"
        }

        badgeBackground = Color(0xFFFFF4D5)
        badgeText = Color(0xFF765300)

    } else {
        when (report.dangerType) {
            "ACCIDENT" -> {
                statusLabel = "事故"
                badgeBackground = Color(0xFFFFF0DF)
                badgeText = Color(0xFF9A5515)
            }

            "EARTHQUAKE" -> {
                statusLabel = "地震"
                badgeBackground = Color(0xFFF0E9DF)
                badgeText = Color(0xFF75563A)
            }

            "FIRE" -> {
                statusLabel = "火災"
                badgeBackground = Color(0xFFFDE4E2)
                badgeText = Color(0xFFA52E28)
            }

            "LANDSLIDE" -> {
                statusLabel = "土砂崩れ"
                badgeBackground = Color(0xFFEEE7D5)
                badgeText = Color(0xFF6E5B25)
            }

            "STALKING" -> {
                statusLabel = "付きまとわれている"
                badgeBackground = Color(0xFFEEE8F8)
                badgeText = Color(0xFF62458A)
            }

            else -> {
                statusLabel = "危険"
                badgeBackground = Color(0xFFFDEAEA)
                badgeText = Color(0xFFC44350)
            }
        }
    }

    val reportedAtText = remember(report.reportedAt) {
        runCatching {
            OffsetDateTime.parse(report.reportedAt)
                .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                .format(
                    DateTimeFormatter.ofPattern(
                        "yyyy/MM/dd HH:mm"
                    )
                )
        }.getOrDefault(report.reportedAt)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = if (cancelled) {
                "取り消し完了"
            } else {
                "送信完了"
            },
            color = navy,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (cancelled) {
                "危険の回答を取り消しました"
            } else {
                "安否を送信しました"
            },
            color = navy,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        if (!cancelled) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                contentColor = navy,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE0E8F2)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    AnswerSummaryRow(label = "状況") {
                        Surface(
                            color = badgeBackground,
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(
                                    horizontal = 9.dp,
                                    vertical = 4.dp
                                ),
                                color = badgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFFEDF1F4)
                    )

                    AnswerSummaryRow(label = "送信日時") {
                        Text(
                            text = reportedAtText,
                            color = navy,
                            fontSize = 14.sp,
                            textAlign = TextAlign.End
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFFEDF1F4)
                    )

                    AnswerSummaryRow(label = "位置情報") {
                        Text(
                            text = if (report.location != null) {
                                "取得済み"
                            } else {
                                "未取得"
                            },
                            color = if (report.location != null) {
                                navy
                            } else {
                                muted
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (report.status == "WORRIED" &&
                !report.comment.isNullOrBlank()
            ) {
                HorizontalDivider(
                    color = Color(0xFFEDF1F4)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "コメント",
                        color = navy,
                        fontSize = 14.sp
                    )

                    Text(
                        text = report.comment.orEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        color = navy,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
            if (canOfferCancellation) {
                OutlinedButton(
                    enabled = !cancelling &&
                            !cancellationRejected &&
                            remaining > 0,
                    onClick = {
                        val currentSession = session

                        if (currentSession != null &&
                            !cancelling &&
                            !cancellationRejected &&
                            remainingSeconds() > 0
                        ) {
                            cancelling = true
                            onBusyChanged(true)
                            errorMessage = null

                            scope.launch {
                                try {
                                    SafetyReportCancelApi.cancel(
                                        session = currentSession,
                                        reportId = report.reportId
                                    )

                                    // APIの成功確認後だけ取り消し完了にする。
                                    cancelled = true

                                } catch (e: CancellationException) {
                                    throw e

                                } catch (
                                    e: SafetyReportCancelApiException
                                ) {
                                    if (e.statusCode == 409 ||
                                        e.statusCode == 404
                                    ) {
                                        cancellationRejected = true
                                    }

                                    errorMessage = e.message
                                        ?: "取り消し結果を確認できませんでした。"

                                } catch (e: IOException) {
                                    errorMessage =
                                        "通信に失敗し、取り消し結果を" +
                                                "確認できませんでした。" +
                                                "回答状況を確認してください。"

                                } finally {
                                    cancelling = false
                                    onBusyChanged(false)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFE0E8F2)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = blue,
                        disabledContainerColor =
                            Color(0xFFEDF0F4),
                        disabledContentColor =
                            Color(0xFF788493)
                    )
                ) {
                    Text(
                        text = when {
                            cancelling -> "取り消しています…"
                            cancellationRejected ->
                                "取り消しできません"
                            remaining <= 0 ->
                                "取り消し期限が過ぎました"
                            else ->
                                "取り消す（あと${remaining}秒）"
                        },
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFC44350),
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Button(
            onClick = onHome,
            enabled = !cancelling,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = blue,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "ホームへ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "＠AnpiLink",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            color = muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * ラベルと値を左右に配置する。
 */
@Composable
private fun AnswerSummaryRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF16304E),
            fontSize = 14.sp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            content()
        }
    }
}