package com.example.anpilink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import java.io.IOException
import org.json.JSONException

/** 保護者向けの学校からの連絡一覧。 */
@Composable
fun FamilyNoticesContent(
    session: LoginSession,
    onBack: () -> Unit,
    onDetail: (FamilyNotice) -> Unit
) {
    var notices by remember(session) { mutableStateOf<List<FamilyNotice>>(emptyList()) }
    var loading by remember(session) { mutableStateOf(true) }
    var error by remember(session) { mutableStateOf<String?>(null) }
    LaunchedEffect(session) {
        try {
            notices = FamilyApi.getNotices(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            error = e.message ?: "学校からの連絡を取得できませんでした。"
        } catch (e: JSONException) {
            error = "サーバーの応答を読み取れませんでした。"
        } finally {
            loading = false
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FamilyNoticeHeading(title = "学校からの連絡", onBack = onBack)
        if (loading) {
            Text("読み込み中…", color = Color(0xFF8192A6), fontSize = 14.sp)
        } else if (error != null) {
            Text(error.orEmpty(), color = Color(0xFFAC3341), fontSize = 14.sp)
        } else if (notices.isEmpty()) {
            Text("学校からの連絡はありません。", color = Color(0xFF8192A6), fontSize = 14.sp)
        } else {
            notices.forEach { notice ->
                Surface(
                    onClick = { onDetail(notice) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE0E8F2))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(9.dp).background(
                                if (notice.readAt == null) Color(0xFF2866C9) else Color.Transparent,
                                CircleShape
                            )
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (notice is FamilyReminder) "【催促通知】" else "【通常通知】",
                                color = Color(0xFF2866C9), fontSize = 13.sp
                            )
                            Text(noticeTitle(notice), color = Color(0xFF16304E), fontSize = 16.sp)
                            Text(formatReminderDate(notice.createdAt), color = Color(0xFF8192A6), fontSize = 12.sp)
                        }
                        Text("›", color = Color(0xFF8192A6), fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

/** 保護者向けの連絡詳細。 */
@Composable
fun FamilyNoticeDetailContent(
    reminder: FamilyNotice?,
    session: LoginSession,
    onBack: () -> Unit
) {
    var readSaved by remember(session, reminder) { mutableStateOf(reminder?.readAt != null) }
    var readError by remember(session, reminder) { mutableStateOf<String?>(null) }
    var retry by remember(session, reminder) { mutableStateOf(0) }
    LaunchedEffect(session, reminder, retry) {
        if (reminder != null && !readSaved) {
            readError = null
            try {
                FamilyApi.markRead(session, reminder)
                readSaved = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                readError = e.message ?: "既読状態を登録できませんでした。"
            } catch (e: JSONException) {
                readError = "既読状態を登録できませんでした。"
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FamilyNoticeHeading(title = reminder?.let(::noticeTitle) ?: "学校からの連絡", onBack = onBack)
        if (readError != null) {
            Text(readError.orEmpty(), color = Color(0xFFAC3341), fontSize = 14.sp)
            TextButton(onClick = { retry++ }) { Text("再試行") }
        }
        if (reminder != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE0E8F2))
            ) {
                Text(
                    text = when (reminder) {
                        is FamilyReminder -> "${reminder.studentName}さんは回答期限を過ぎた時点で安否確認に未回答でした。\n" +
                            "対象：${reminder.title}\n" +
                            "回答期限：${formatReminderDate(reminder.deadlineAt)}"
                        is FamilySchoolNotice -> reminder.message + "\n\n配信日時：${formatReminderDate(reminder.createdAt)}"
                    },
                    modifier = Modifier.padding(18.dp),
                    color = Color(0xFF16304E),
                    fontSize = 14.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

private fun noticeTitle(notice: FamilyNotice): String = when (notice) {
    is FamilyReminder -> "安否確認未回答のお知らせ"
    is FamilySchoolNotice -> notice.title
}

private fun formatReminderDate(value: String): String =
    try {
        java.time.OffsetDateTime.parse(value)
            .atZoneSameInstant(java.time.ZoneId.of("Asia/Tokyo"))
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
    } catch (e: java.time.DateTimeException) {
        value
    }

/**
 * 画面タイトルと戻るボタン。
 */
@Composable
private fun FamilyNoticeHeading(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = Color(0xFF16304E),
            fontSize = 26.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold
        )

        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF2866C9)
            )
        ) {
            Text("← 戻る")
        }
    }
}