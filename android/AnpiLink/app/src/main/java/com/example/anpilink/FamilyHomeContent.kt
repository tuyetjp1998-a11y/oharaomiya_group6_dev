package com.example.anpilink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.json.JSONException

private val FamilyNavy = Color(0xFF16304E)
private val FamilyMuted = Color(0xFF8192A6)
private val FamilyBorder = Color(0xFFE0E8F2)

/**
 * 保護者ホームと関連する生徒一覧を表示する。
 */
@Composable
fun FamilyHomeContent(session: LoginSession) {
    var selectedStudentId by remember(session) {
        mutableStateOf<Long?>(null)
    }

    var students by remember(session) {
        mutableStateOf<List<FamilyStudentItem>>(emptyList())
    }

    var loading by remember(session) {
        mutableStateOf(true)
    }

    var errorMessage by remember(session) {
        mutableStateOf<String?>(null)
    }
    var homeRequests by remember(session) {
        mutableStateOf<List<FamilyChildRequestItem>>(emptyList())
    }

    var requestsLoading by remember(session) {
        mutableStateOf(true)
    }

    var requestsError by remember(session) {
        mutableStateOf<String?>(null)
    }
    // 初回表示時と生徒詳細から戻ったときに取得する。
    LaunchedEffect(session, selectedStudentId) {
        if (selectedStudentId == null) {
            loading = true
            errorMessage = null

            try {
                students = FamilyApi.getStudents(
                    session = session
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: FamilyApiException) {
                errorMessage = e.message
                    ?: "生徒一覧を取得できませんでした。"
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
    }
    // 初回表示時と生徒詳細から戻ったときに申請状況を取得する。
    LaunchedEffect(session, selectedStudentId) {
        if (selectedStudentId != null) {
            return@LaunchedEffect
        }

        requestsLoading = true
        requestsError = null
        homeRequests = emptyList()

        try {
            val history = FamilyChildRequestsApi.getHistory(
                session = session
            )

            // 生徒IDごとに最新の申請だけを使用する。
            // 承認済みになった申請の過去の却下履歴は表示しない。
            homeRequests = history
                .groupBy { it.studentLoginId }
                .values
                .mapNotNull { items ->
                    items.maxByOrNull { it.requestId }
                }
                .filter {
                    it.status == "PENDING" ||
                            it.status == "REJECTED"
                }
                .sortedByDescending { it.requestId }

        } catch (e: CancellationException) {
            throw e
        } catch (e: FamilyApiException) {
            requestsError = e.message
                ?: "申請状況を取得できませんでした。"
        } catch (e: IOException) {
            requestsError =
                "申請状況を取得できません。" +
                        "接続状況を確認してください。"
        } catch (e: JSONException) {
            requestsError =
                "申請状況の応答を読み取れませんでした。"
        } finally {
            requestsLoading = false
        }
    }

    val selectedId = selectedStudentId

    if (selectedId != null) {
        FamilyStudentDetailContent(
            session = session,
            studentUserId = selectedId,
            onBack = {
                selectedStudentId = null
            }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "保護者ホーム",
                color = FamilyNavy,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "保護者 ${session.userName} さん",
                color = FamilyMuted,
                fontSize = 14.sp
            )
        }

        Text(
            text = "お子さんの安否",
            color = FamilyNavy,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        val error = errorMessage

        when {
            loading -> {
                Text(
                    text = "生徒一覧を読み込んでいます。",
                    color = FamilyMuted,
                    fontSize = 14.sp
                )
            }

            error != null -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF2F3),
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

            students.isEmpty() -> {
                // 申請カードが表示される場合は、空の一覧メッセージを重ねない。
                if (!requestsLoading &&
                    requestsError == null &&
                    homeRequests.isEmpty()
                ) {
                    Text(
                        text = "現在、安否情報を確認できる生徒はいません。",
                        color = FamilyMuted,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            else -> {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    students.forEach { student ->
                        FamilyChildCard(
                            student = student,
                            onClick = {
                                selectedStudentId =
                                    student.studentUserId
                            }
                        )
                    }
                }
            }
        }

        // 申請状況の取得失敗で、取得済みの生徒一覧を隠さない。
        when {
            requestsLoading -> {
                Text(
                    text = "申請状況を読み込んでいます。",
                    color = FamilyMuted,
                    fontSize = 14.sp
                )
            }

            requestsError != null -> {
                Text(
                    text = requestsError
                        ?: "申請状況を取得できませんでした。",
                    color = Color(0xFFB33A48),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            else -> {
                homeRequests.forEach { item ->
                    FamilyHomeRequestCard(item = item)
                }
            }
        }
    }
}

/**
 * 生徒名と最新の安否状況を表示する。
 */
@Composable
private fun FamilyChildCard(
    student: FamilyStudentItem,
    onClick: () -> Unit
) {
    val detailLabel = when (student.dangerType) {
        "ACCIDENT" -> "事故"
        "EARTHQUAKE" -> "地震"
        "FIRE" -> "火災"
        "LANDSLIDE" -> "土砂崩れ"
        "HEATSTROKE" -> "熱中症"
        else -> null
    }

    val statusText = when (student.status) {
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

    val badgeBackground = when (student.status) {
        "SAFE" -> Color(0xFFE5F5EE)
        "DANGER" -> Color(0xFFFDEAEA)
        "WORRIED" -> Color(0xFFFFF4D5)
        else -> Color(0xFFEEF2F6)
    }

    val badgeForeground = when (student.status) {
        "SAFE" -> Color(0xFF288361)
        "DANGER" -> Color(0xFFC44350)
        "WORRIED" -> Color(0xFF765300)
        else -> Color(0xFF718096)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FamilyBorder)
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
                text = student.studentName,
                modifier = Modifier.weight(1f),
                color = FamilyNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(7.dp),
                color = badgeBackground
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    color = badgeForeground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "›",
                color = FamilyMuted,
                fontSize = 26.sp
            )
        }
    }

}
/**
 * ホームに承認待ち・却下の申請状況を表示する。
 */
@Composable
private fun FamilyHomeRequestCard(
    item: FamilyChildRequestItem
) {
    val pending = item.status == "PENDING"

    val badgeBackground = if (pending) {
        Color(0xFFFFF3D9)
    } else {
        Color(0xFFFDEAEA)
    }

    val badgeForeground = if (pending) {
        Color(0xFF8A641B)
    } else {
        Color(0xFFC44350)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, FamilyBorder)
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
                    text = if (pending) {
                        "生徒ID：${item.studentLoginId}"
                    } else {
                        "お子さんの追加申請"
                    },
                    modifier = Modifier.weight(1f),
                    color = FamilyNavy,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = badgeBackground,
                    shape = RoundedCornerShape(7.dp)
                ) {
                    Text(
                        text = if (pending) {
                            "承認待ち"
                        } else {
                            "却下"
                        },
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
                text = if (pending) {
                    "職員の承認後に閲覧できます。"
                } else {
                    "親子関係を確認できませんでした。" +
                            "学校へお問い合わせください。"
                },
                color = FamilyNavy,
                fontSize = 14.sp,
                lineHeight = 24.sp
            )
        }
    }
}