package com.example.anpilink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import org.json.JSONException

private val DetailNavy = Color(0xFF16304E)
private val DetailBlue = Color(0xFF2866C9)
private val DetailMuted = Color(0xFF8192A6)
private val DetailBorder = Color(0xFFE0E8F2)

/**
 * 関連付けられた生徒の最新の安否回答を表示する。
 * スクロールは呼び出し元で管理する。
 */
@Composable
fun FamilyStudentDetailContent(
    session: LoginSession,
    studentUserId: Long,
    onBack: () -> Unit
) {
    var detail by remember(session, studentUserId) {
        mutableStateOf<FamilyStudentDetail?>(null)
    }

    var loading by remember(session, studentUserId) {
        mutableStateOf(true)
    }

    var errorMessage by remember(session, studentUserId) {
        mutableStateOf<String?>(null)
    }

    BackHandler {
        onBack()
    }

    LaunchedEffect(session, studentUserId) {
        loading = true
        errorMessage = null
        detail = null

        try {
            detail = FamilyApi.getStudentDetail(
                session = session,
                studentUserId = studentUserId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FamilyApiException) {
            errorMessage =
                e.message ?: "安否情報を取得できませんでした。"
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "お子さんの回答詳細",
                modifier = Modifier.weight(1f),
                color = DetailNavy,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DetailBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DetailBlue
                )
            ) {
                Text(
                    text = "← 戻る",
                    fontSize = 13.sp
                )
            }
        }

        val currentDetail = detail
        val error = errorMessage

        when {
            loading -> {
                Text(
                    text = "安否情報を読み込んでいます。",
                    color = DetailMuted,
                    fontSize = 14.sp
                )
            }

            error != null -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFF2F3),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFEFC5CA)
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFB33A48),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            currentDetail != null -> {
                FamilyReportCard(
                    detail = currentDetail
                )
            }
        }
    }
}

/**
 * 氏名・安否・送信日時・送信位置を表示する。
 */
@Composable
private fun FamilyReportCard(
    detail: FamilyStudentDetail
) {
    val report = detail.latestReport

    val detailLabel = when (report?.dangerType) {
        "ACCIDENT" -> "事故"
        "EARTHQUAKE" -> "地震"
        "FIRE" -> "火災"
        "LANDSLIDE" -> "土砂崩れ"
        "HEATSTROKE" -> "熱中症"
        else -> null
    }

    val statusText = when (report?.status) {
        "SAFE" -> "無事"
        "DANGER" -> "危険"
        "WORRIED" -> {
            if (detailLabel == null) {
                "困っている"
            } else {
                "困っている（$detailLabel）"
            }
        }
        null -> "未回答"
        else -> "不明"
    }

    val badgeBackground = when (report?.status) {
        "SAFE" -> Color(0xFFE5F5EE)
        "DANGER" -> Color(0xFFFDEAEA)
        "WORRIED" -> Color(0xFFFFF4D5)
        else -> Color(0xFFEEF2F6)
    }

    val badgeForeground = when (report?.status) {
        "SAFE" -> Color(0xFF288361)
        "DANGER" -> Color(0xFFC44350)
        "WORRIED" -> Color(0xFF765300)
        else -> Color(0xFF718096)
    }

    val reportedAtText = report?.let {
        runCatching {
            OffsetDateTime.parse(it.reportedAt)
                .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                .format(
                    DateTimeFormatter.ofPattern(
                        "yyyy/MM/dd HH:mm"
                    )
                )
        }.getOrDefault(it.reportedAt)
    } ?: "—"

    val latitude = report?.latitude
    val longitude = report?.longitude

    val hasLocation =
        latitude != null &&
                longitude != null &&
                latitude.isFinite() &&
                longitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0

    // 地名を推測せず、保存された座標を表示する。
    val locationText = when {
        report == null -> "—"
        hasLocation -> String.format(
            Locale.JAPAN,
            "%.6f, %.6f",
            latitude,
            longitude
        )
        else -> "未取得"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DetailBorder)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 4.dp
            )
        ) {
            FamilyDetailRow(label = "氏名") {
                FamilyDetailValue(
                    text = detail.studentName,
                    bold = true
                )
            }

            HorizontalDivider(
                color = Color(0xFFEDF1F4)
            )

            FamilyDetailRow(label = "安否") {
                Surface(
                    color = badgeBackground,
                    shape = RoundedCornerShape(7.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        color = badgeForeground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFEDF1F4)
            )

            FamilyDetailRow(label = "送信日時") {
                FamilyDetailValue(
                    text = reportedAtText
                )
            }

            HorizontalDivider(
                color = Color(0xFFEDF1F4)
            )

            FamilyDetailRow(label = "送信位置") {
                FamilyDetailValue(
                    text = locationText
                )
            }
        }
    }
}

/**
 * 項目名と値を左右に配置する。
 */
@Composable
private fun FamilyDetailRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.3f),
            color = DetailNavy,
            fontSize = 14.sp
        )

        Box(
            modifier = Modifier
                .weight(0.7f)
                .padding(start = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            content()
        }
    }
}

@Composable
private fun FamilyDetailValue(
    text: String,
    bold: Boolean = false
) {
    Text(
        text = text,
        color = DetailNavy,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        fontWeight = if (bold) {
            FontWeight.Bold
        } else {
            FontWeight.Normal
        },
        textAlign = TextAlign.End
    )
}