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
import androidx.compose.material3.Switch
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
private val SettingsNavy = Color(0xFF16304E)
private val SettingsMuted = Color(0xFF8192A6)
private val SettingsBorder = Color(0xFFE0E8F2)

/**
 * 保護者の設定画面。
 */
@Composable
fun FamilySettingsContent(
    session: LoginSession,
    onBack: () -> Unit,
    onChildAdd: (() -> Unit)? = null,
    onChildRequests: (() -> Unit)? = null
) {
    var settings by remember(session) {
        mutableStateOf<FamilySettingsResult?>(null)
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
            settings = FamilySettingsApi.getSettings(
                session = session
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FamilyApiException) {
            errorMessage = e.message
                ?: "設定を取得できませんでした。"
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

    // スクロールはFamilyAppContent側で処理する。
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "設定",
                modifier = Modifier.weight(1f),
                color = SettingsNavy,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SettingsBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF2866C9)
                )
            ) {
                Text(
                    text = "← 戻る",
                    fontSize = 13.sp
                )
            }
        }

        val error = errorMessage
        val currentSettings = settings

        when {
            loading -> {
                Text(
                    text = "設定を読み込んでいます。",
                    color = SettingsMuted,
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

            currentSettings != null -> {
                FamilyPreference(session, "子どもの安否回答を通知", currentSettings.notificationEnabled,
                    school = false) { saved ->
                    settings = settings?.copy(notificationEnabled = saved)
                }
                currentSettings.schoolNotificationEnabled?.let { enabled ->
                    FamilyPreference(session, "学校からの通知", enabled, school = true) { saved ->
                        settings = settings?.copy(schoolNotificationEnabled = saved)
                    }
                }

                Text(
                    text = "お子さん",
                    color = SettingsNavy,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (currentSettings.students.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(
                            1.dp,
                            SettingsBorder
                        )
                    ) {
                        Text(
                            text = "関連付けられている生徒はいません。",
                            modifier = Modifier.padding(18.dp),
                            color = SettingsMuted,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                } else {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        currentSettings.students.forEach { student ->
                            FamilyApprovedChildCard(
                                student = student
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        SettingsBorder
                    )
                ) {
                    Column {
                        FamilySettingsMenuRow(
                            label = "お子さんを追加",
                            enabled = onChildAdd != null,
                            onClick = {
                                onChildAdd?.invoke()
                            }
                        )

                        HorizontalDivider(
                            color = Color(0xFFEDF1F4)
                        )

                        FamilySettingsMenuRow(
                            label = "申請履歴",
                            enabled = onChildRequests != null,
                            onClick = {
                                onChildRequests?.invoke()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyPreference(
    session: LoginSession,
    label: String,
    enabled: Boolean,
    school: Boolean,
    onSaved: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    var savingNotification by remember(session) {
        mutableStateOf(false)
    }

    var notificationError by remember(session) {
        mutableStateOf<String?>(null)
    }
    FamilyNotificationSettingCard(
        label = label,
        enabled = enabled,
        saving = savingNotification,
        errorMessage = notificationError,
        onEnabledChange = { requestedEnabled ->
            if (!savingNotification) {
                savingNotification = true
                notificationError = null

                scope.launch {
                    try {
                        val savedEnabled =
                            FamilySettingsApi.updateNotificationEnabled(
                                session = session,
                                enabled = requestedEnabled,
                                school = school
                            )

                        // サーバーが返した保存結果を表示する。
                        onSaved(savedEnabled)

                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: FamilyApiException) {
                        notificationError = e.message
                            ?: "設定を保存できませんでした。"
                    } catch (e: IOException) {
                        // 通信切断時はサーバー側で保存済みの可能性がある。
                        notificationError =
                            "保存結果を確認できませんでした。" +
                                    "接続状況を確認し、" +
                                    "設定画面を開き直してください。"
                    } catch (e: JSONException) {
                        notificationError =
                            "保存結果を読み取れませんでした。" +
                                    "設定画面を開き直してください。"
                    } finally {
                        savingNotification = false
                    }
                }
            }
        }
    )
}

/**
 * 保護者の安否回答通知設定を表示・変更する。
 */
@Composable
private fun FamilyNotificationSettingCard(
    label: String,
    enabled: Boolean,
    saving: Boolean,
    errorMessage: String?,
    onEnabledChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SettingsBorder)
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
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = SettingsNavy,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                        enabled = !saving
                    )

                    Text(
                        text = if (enabled) "ON" else "OFF",
                        color = SettingsMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (saving) {
                Text(
                    text = "保存しています。",
                    color = SettingsMuted,
                    fontSize = 12.sp
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFB33A48),
                    fontSize = 12.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * 承認済みの生徒を表示する。
 */
@Composable
private fun FamilyApprovedChildCard(
    student: FamilySettingsStudent
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SettingsBorder)
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
                color = SettingsNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(7.dp),
                color = Color(0xFFE5F5EE)
            ) {
                Text(
                    text = "承認済み",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    color = Color(0xFF288361),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 子どもの追加と申請履歴のメニュー。
 */
@Composable
private fun FamilySettingsMenuRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
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
                color = SettingsNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "›",
                color = SettingsMuted,
                fontSize = 26.sp
            )
        }
    }
}