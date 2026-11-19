package com.example.anpilink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 生徒・保護者向けの通知設定案内。
 */
@Composable
fun NotificationIntroContent(
    onEnableNotifications: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "AnpiLink",
            style = MaterialTheme.typography.headlineSmall
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "はじめに",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "通知を設定しましょう",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ログインの有効期限が近づくと、" +
                        "期限の7日前から1日1回、" +
                        "再ログインを促す通知をお送りします。\n" +
                        "大切なお知らせを受け取るため、" +
                        "通知を許可してください。",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Button(
            onClick = onEnableNotifications,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (loading) {
                    "設定しています…"
                } else {
                    "通知を設定する"
                }
            )
        }

        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        OutlinedButton(
            onClick = onLater,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("あとで設定する")
        }
    }
}