package com.example.anpilink
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException

/**
 * 生徒の位置情報・通知設定を表示する。
 * スクロール、ヘッダー、フッターは親画面で表示する。
 */
@Composable
fun StudentSettingsContent(
    session: LoginSession,
    onBack: () -> Unit
) {
    val navy = Color(0xFF16304E)
    val blue = Color(0xFF2866C9)
    val muted = Color(0xFF8192A6)

    var settings by remember(session) {
        mutableStateOf<StudentSettingsResult?>(null)
    }
    var loading by remember(session) {
        mutableStateOf(true)
    }
    var saving by remember(session) {
        mutableStateOf(false)
    }
    var errorMessage by remember(session) {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(session) {
        loading = true
        errorMessage = null

        try {
            settings = StudentSettingsApi.getSettings(
                session = session,
                context = context
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: StudentSettingsApiException) {
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

    fun saveSetting(
        locationSetting: Boolean,
        enabled: Boolean
    ) {
        val current = settings ?: return

        if (loading || saving) {
            return
        }

        saving = true
        errorMessage = null

        scope.launch {
            try {
                if (locationSetting) {
                    val saved =
                        StudentSettingsApi.updateLocationEnabled(
                            session = session,
                            enabled = enabled,
                            context = context
                        )

                    settings = current.copy(
                        locationEnabled = saved
                    )
                } else {
                    val saved =
                        StudentSettingsApi.updateNotificationEnabled(
                            session = session,
                            enabled = enabled,
                            context = context
                        )

                    settings = current.copy(
                        notificationEnabled = saved
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: StudentSettingsApiException) {
                errorMessage = e.message
                    ?: "設定を保存できませんでした。"
            } catch (e: IOException) {
                errorMessage =
                    "保存結果を確認できませんでした。" +
                            "接続状況を確認し、設定画面を開き直してください。"
            } catch (e: JSONException) {
                errorMessage =
                    "保存結果を読み取れませんでした。" +
                            "設定画面を開き直してください。"
            } finally {
                try {
                    val pendingOff = StudentSettingsCache.offPending(
                        context, session.userId
                    )
                    settings = settings?.let { value ->
                        value.copy(
                            locationEnabled = if (pendingOff) false else value.locationEnabled,
                            locationOffPending = pendingOff
                        )
                    }
                } finally {
                    saving = false
                }
            }
        }
    }
    // 位置情報の利用をONにする操作で権限を確認する。
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ] == true

            if (granted) {
                saveSetting(
                    locationSetting = true,
                    enabled = true
                )
            } else {
                errorMessage =
                    "位置情報の利用が許可されていません。" +
                            "利用する場合は、端末の設定から" +
                            "AnpiLinkの位置情報を許可してください。"
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
                text = "設定",
                modifier = Modifier.weight(1f),
                color = navy,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onBack,
                enabled = !saving,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE0E8F2)
                )
            ) {
                Text(
                    text = "← 戻る",
                    color = if (saving) muted else blue,
                    fontSize = 14.sp
                )
            }
        }

        if (loading) {
            Text(
                text = "設定を読み込んでいます。",
                color = muted,
                fontSize = 14.sp
            )
        }

        val current = settings

        if (!loading && current != null) {
            if (current.fromCache) {
                Text(
                    text = "端末に保存した設定を表示しています。" +
                            "位置情報のON・通知設定の変更は、" +
                            "通信できる状態で設定画面を開き直してから行ってください。",
                    color = muted,
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
            }

            if (current.locationOffPending) {
                Text(
                    text = "この端末では位置情報の利用をOFFにしています。" +
                            "サーバーへの反映は未確認です。" +
                            "通信できる状態で設定を読み込むと反映を試みます。",
                    color = muted,
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE0E8F2)
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 6.dp
                    )
                ) {
                    StudentSettingSwitchRow(
                        label = "位置情報の利用",
                        checked = current.locationEnabled,
                        enabled = !saving,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                saveSetting(
                                    locationSetting = true,
                                    enabled = false
                                )
                            } else if (current.fromCache) {
                                errorMessage =
                                    "通信できる状態で設定画面を開き直してから、ONにしてください。"
                            } else {
                                val fineGranted =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                val coarseGranted =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                if (fineGranted || coarseGranted) {
                                    saveSetting(
                                        locationSetting = true,
                                        enabled = true
                                    )
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        }
                    )

                    HorizontalDivider(
                        color = Color(0xFFEDF1F4)
                    )

                    StudentSettingSwitchRow(
                        label = "通知",
                        checked = current.notificationEnabled,
                        enabled = !saving && !current.fromCache,
                        onCheckedChange = { enabled ->
                            saveSetting(
                                locationSetting = false,
                                enabled = enabled
                            )
                        }
                    )
                }
            }
        }

        if (saving) {
            Text(
                text = "設定を保存しています。",
                color = muted,
                fontSize = 14.sp
            )
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFFB33A48),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 設定名とON・OFFスイッチを表示する。
 */
@Composable
private fun StudentSettingSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Color(0xFF16304E),
            fontSize = 16.sp
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2866C9)
            )
        )

        Text(
            text = if (checked) "ON" else "OFF",
            color = Color(0xFF16304E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}