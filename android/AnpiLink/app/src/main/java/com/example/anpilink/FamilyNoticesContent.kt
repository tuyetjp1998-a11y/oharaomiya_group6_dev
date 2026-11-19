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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 保護者向けの学校からの連絡一覧。
 */
@Composable
fun FamilyNoticesContent(
    onBack: () -> Unit,
    onDetail: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FamilyNoticeHeading(
            title = "学校からの連絡",
            onBack = onBack
        )

        Surface(
            onClick = onDetail,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(
                1.dp,
                Color(0xFFE0E8F2)
            )
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
                    text = "避難訓練のお知らせ",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF16304E),
                    fontSize = 16.sp
                )

                Text(
                    text = "›",
                    color = Color(0xFF8192A6),
                    fontSize = 26.sp
                )
            }
        }

        Text(
            text = "表示内容は画面確認用のサンプルです。",
            color = Color(0xFF8192A6),
            fontSize = 12.sp,
            lineHeight = 20.sp
        )
    }
}

/**
 * 保護者向けの連絡詳細。
 */
@Composable
fun FamilyNoticeDetailContent(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FamilyNoticeHeading(
            title = "避難訓練のお知らせ",
            onBack = onBack
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
            Text(
                text = "10月9日に避難訓練を実施します。",
                modifier = Modifier.padding(18.dp),
                color = Color(0xFF16304E),
                fontSize = 14.sp,
                lineHeight = 24.sp
            )
        }

        Text(
            text = "現在は画面確認用のサンプルを表示しています。",
            color = Color(0xFF8192A6),
            fontSize = 12.sp,
            lineHeight = 20.sp
        )
    }
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