package com.example.anpilink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 保護者画面のヘッダー、本文、メインメニューを表示する。
 */
@Composable
fun FamilyAppContent(
    session: LoginSession,
    loggingOut: Boolean,
    logoutError: String?,
    onLogout: () -> Unit
) {
    var screen by remember(session) {
        mutableStateOf("HOME")
    }

    var homeVersion by remember(session) {
        mutableStateOf(0)
    }
    var childRequestMessage by remember(session) {
        mutableStateOf<String?>(null)
    }

    val navy = Color(0xFF16304E)
    val blue = Color(0xFF2866C9)
    val muted = Color(0xFF8192A6)

    // その他画面から端末の戻る操作でホームへ戻る。
    BackHandler(enabled = screen != "HOME") {
        screen = when (screen) {
            "NOTICE_DETAIL" -> "NOTICES"
            "NOTICES", "SETTINGS" -> "MORE"
            "CHILD_ADD", "CHILD_REQUESTS" -> {
                childRequestMessage = null
                "SETTINGS"
            }
            else -> "HOME"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ヘッダー
        Surface(
            color = Color.White,
            contentColor = navy
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 22.dp,
                        vertical = 22.dp
                    )
            ) {
                Text(
                    text = "AnpiLink",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 本文
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = Color(0xFFF5F8FE),
            contentColor = navy
        ) {
            key(screen, homeVersion) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = 24.dp,
                            vertical = 32.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(24.dp)
                ) {
                    logoutError?.let { message ->
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
                                text = message,
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFFB33A48),
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    when (screen) {
                        "HOME" -> {
                            FamilyHomeContent(
                                session = session
                            )
                        }

                        "NOTICES" -> {
                            FamilyNoticesContent(
                                onBack = {
                                    screen = "MORE"
                                },
                                onDetail = {
                                    screen = "NOTICE_DETAIL"
                                }
                            )
                        }

                        "NOTICE_DETAIL" -> {
                            FamilyNoticeDetailContent(
                                onBack = {
                                    screen = "NOTICES"
                                }
                            )
                        }

                        "SETTINGS" -> {
                            FamilySettingsContent(
                                session = session,
                                onBack = {
                                    screen = "MORE"
                                },
                                onChildAdd = {
                                    childRequestMessage = null
                                    screen = "CHILD_ADD"
                                },
                                onChildRequests = {
                                    childRequestMessage = null
                                    screen = "CHILD_REQUESTS"
                                }
                            )
                        }

                        "CHILD_ADD" -> {
                            FamilyChildAddContent(
                                session = session,
                                onBack = {
                                    screen = "SETTINGS"
                                },
                                onSubmitted = { message ->
                                    childRequestMessage = message
                                    screen = "CHILD_REQUESTS"
                                }
                            )
                        }

                        "CHILD_REQUESTS" -> {
                            FamilyChildRequestsContent(
                                session = session,
                                message = childRequestMessage,
                                onBack = {
                                    childRequestMessage = null
                                    screen = "SETTINGS"
                                }
                            )
                        }

                        else -> {
                            FamilyMoreContent(
                                loggingOut = loggingOut,
                                onLogout = onLogout,
                                onNotices = {
                                    screen = "NOTICES"
                                },
                                onSettings = {
                                    screen = "SETTINGS"
                                }
                            )
                        }
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
        }

        // 下部メニュー
        NavigationBar(
            modifier = Modifier.height(76.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = screen == "HOME",
                onClick = {
                    // 詳細を表示中でもホームの生徒一覧へ戻す。
                    homeVersion++
                    screen = "HOME"
                },
                icon = {
                    FamilyMenuIcon(home = true)
                },
                label = {
                    Text(
                        text = "ホーム",
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = blue,
                    selectedTextColor = blue,
                    unselectedIconColor = muted,
                    unselectedTextColor = muted,
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = screen == "MORE",
                onClick = {
                    screen = "MORE"
                },
                icon = {
                    FamilyMenuIcon(home = false)
                },
                label = {
                    Text(
                        text = "その他",
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = blue,
                    selectedTextColor = blue,
                    unselectedIconColor = muted,
                    unselectedTextColor = muted,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * ホームとその他のメニューアイコン。
 */
@Composable
private fun FamilyMenuIcon(home: Boolean) {
    val color = LocalContentColor.current

    Canvas(
        modifier = Modifier.size(22.dp)
    ) {
        val unit = size.minDimension / 24f
        val strokeWidth = 1.7f * unit

        if (home) {
            val path = Path().apply {
                moveTo(3f * unit, 10f * unit)
                lineTo(12f * unit, 3f * unit)
                lineTo(21f * unit, 10f * unit)

                moveTo(5f * unit, 9f * unit)
                lineTo(5f * unit, 21f * unit)
                lineTo(19f * unit, 21f * unit)
                lineTo(19f * unit, 9f * unit)

                moveTo(9f * unit, 21f * unit)
                lineTo(9f * unit, 14f * unit)
                lineTo(15f * unit, 14f * unit)
                lineTo(15f * unit, 21f * unit)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        } else {
            listOf(6f, 12f, 18f).forEach { y ->
                drawLine(
                    color = color,
                    start = Offset(
                        x = 5f * unit,
                        y = y * unit
                    ),
                    end = Offset(
                        x = 19f * unit,
                        y = y * unit
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}