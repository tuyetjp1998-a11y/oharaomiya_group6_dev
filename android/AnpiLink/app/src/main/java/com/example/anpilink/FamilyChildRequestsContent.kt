package com.example.anpilink

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.json.JSONException

/**
 * 保護者の申請履歴を表示する。
 */
@Composable
fun FamilyChildRequestsContent(
    session: LoginSession,
    message: String?,
    onBack: () -> Unit
) {
    var requests by remember(session) {
        mutableStateOf<List<FamilyChildRequestItem>>(emptyList())
    }
    var loading by remember(session) {
        mutableStateOf(true)
    }
    var errorMessage by remember(session) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(session) {
        loading = true
        errorMessage = null

        try {
            requests = FamilyChildRequestsApi.getHistory(
                session = session
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FamilyApiException) {
            errorMessage = e.message
                ?: "申請履歴を取得できませんでした。"
        } catch (e: IOException) {
            errorMessage =
                "サーバーに接続できません。" +
                        "接続状況を確認してください。"
        } catch (e: JSONException) {
            errorMessage =
                "サーバーの応答を読み取れませんでした。"
        } finally {
            loading = false
        }
    }

    // スクロールとフッターはFamilyAppContent側で表示する。
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF2866C9)
            )
        ) {
            Text("← 設定")
        }

        Text(
            text = "申請履歴",
            color = Color(0xFF16304E),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = Color(0xFF16304E),
                fontSize = 14.sp,
                lineHeight = 24.sp
            )
        }

        val error = errorMessage

        when {
            loading -> {
                Text(
                    text = "申請履歴を読み込んでいます。",
                    color = Color(0xFF8192A6),
                    fontSize = 14.sp
                )
            }

            error != null -> {
                Text(
                    text = error,
                    color = Color(0xFFB33A48),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            requests.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFE0E8F2)
                    )
                ) {
                    Text(
                        text = "申請履歴はありません。",
                        modifier = Modifier.padding(18.dp),
                        color = Color(0xFF8192A6),
                        fontSize = 14.sp
                    )
                }
            }

            else -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    requests.forEach { item ->
                        FamilyRequestHistoryCard(item = item)
                    }
                }
            }
        }
    }
}

/**
 * 申請履歴の1件を表示する。
 */
@Composable
private fun FamilyRequestHistoryCard(
    item: FamilyChildRequestItem
) {
    val statusLabel = when (item.status) {
        "PENDING" -> "承認待ち"
        "APPROVED" -> "承認済み"
        "REJECTED" -> "却下"
        else -> "不明"
    }

    val badgeBackground = when (item.status) {
        "PENDING" -> Color(0xFFFFF3D9)
        "APPROVED" -> Color(0xFFE5F5EE)
        "REJECTED" -> Color(0xFFFDEAEA)
        else -> Color(0xFFEEF2F6)
    }

    val badgeForeground = when (item.status) {
        "PENDING" -> Color(0xFF8A641B)
        "APPROVED" -> Color(0xFF288361)
        "REJECTED" -> Color(0xFFC44350)
        else -> Color(0xFF8192A6)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            Color(0xFFE0E8F2)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "生徒ID：${item.studentLoginId}",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF16304E),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = badgeBackground
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        color = badgeForeground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "申請日：${item.requestedAtText}",
                color = Color(0xFF8192A6),
                fontSize = 12.sp
            )

            when (item.status) {
                "PENDING" -> {
                    Text(
                        text = "教員の承認をお待ちください。",
                        color = Color(0xFF16304E),
                        fontSize = 14.sp,
                        lineHeight = 24.sp
                    )
                }

                "REJECTED" -> {
                    Text(
                        text =
                            "親子関係を確認できませんでした。" +
                                    "学校へお問い合わせください。",
                        color = Color(0xFF16304E),
                        fontSize = 14.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}