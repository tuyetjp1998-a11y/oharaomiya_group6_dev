package com.example.anpilink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 保護者のその他メニュー。
 */
@Composable
fun FamilyMoreContent(
    loggingOut: Boolean,
    onLogout: () -> Unit,
    onNotices: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "その他",
            color = Color(0xFF16304E),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(
                1.dp,
                Color(0xFFE0E8F2)
            )
        ) {
            Column {
                FamilyMoreMenuRow(
                    label = "学校からの連絡",
                    enabled = onNotices != null,
                    onClick = {
                        onNotices?.invoke()
                    }
                )

                HorizontalDivider(
                    color = Color(0xFFEDF1F4)
                )

                FamilyMoreMenuRow(
                    label = "設定",
                    enabled = onSettings != null,
                    onClick = {
                        onSettings?.invoke()
                    }
                )

                HorizontalDivider(
                    color = Color(0xFFEDF1F4)
                )

                FamilyMoreMenuRow(
                    label = if (loggingOut) {
                        "ログアウト中…"
                    } else {
                        "ログアウト"
                    },
                    enabled = !loggingOut,
                    color = Color(0xFFC44350),
                    onClick = onLogout
                )
            }
        }
    }
}

/**
 * メニューの1行を表示する。
 */
@Composable
private fun FamilyMoreMenuRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color = Color(0xFF16304E)
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 22.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "›",
                color = Color(0xFF8192A6),
                fontSize = 26.sp
            )
        }
    }
}