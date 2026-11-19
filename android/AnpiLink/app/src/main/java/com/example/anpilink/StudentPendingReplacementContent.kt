package com.example.anpilink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException

/**
 * HTTP送信を開始していない回答だけを置き換える。
 * 画面を開いた時点では削除せず、新しい回答をAtomicFileで保存する。
 */
@Composable
fun StudentPendingReplacementContent(
    session: LoginSession,
    original: PendingSafetyReport,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onBusyChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var status by remember(original.requestId) {
        mutableStateOf(if (original.status == "WORRIED") "WORRIED" else "SAFE")
    }
    var detail by remember(original.requestId) {
        mutableStateOf(original.dangerType?.takeIf {
            it in setOf("ACCIDENT", "EARTHQUAKE", "FIRE", "LANDSLIDE", "HEATSTROKE")
        })
    }
    var comment by remember(original.requestId) { mutableStateOf(original.comment ?: "") }
    var saving by remember(original.requestId) { mutableStateOf(false) }
    var error by remember(original.requestId) { mutableStateOf<String?>(null) }
    LaunchedEffect(saving) { onBusyChanged(saving) }
    DisposableEffect(onBusyChanged) { onDispose { onBusyChanged(false) } }
    BackHandler { if (!saving) onBack() }

    fun save() {
        if (saving) return
        if (status == "WORRIED" && detail == null && comment.isBlank()) {
            error = "項目を選択するか、コメントを入力してください。"
            return
        }
        saving = true
        error = null
        scope.launch {
            try {
                require(session.role == "STUDENT" && session.userId == original.userId &&
                    original.serverBaseUrl == ApiConfig.BASE_URL) {
                    "保存した回答と同じ生徒アカウントで操作してください。"
                }
                // 置き換えを確定した時点の位置を取得する。再送信時には取り直さない。
                val location = try {
                    val settings = StudentSettingsApi.getSettings(session, context)
                    StudentLocationProvider.getOnce(context, settings.locationEnabled)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    null
                } catch (e: JSONException) {
                    null
                }
                PendingSafetyReportStore.create(
                    context = context,
                    userId = session.userId,
                    status = status,
                    dangerType = if (status == "WORRIED") detail else null,
                    comment = if (status == "WORRIED") comment else null,
                    location = location,
                    replacementRequestId = original.requestId
                )
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                error = e.message ?: "保存できませんでした。元の回答を確認してください。"
            } catch (e: IllegalArgumentException) {
                error = e.message ?: "回答内容を確認してください。"
            } finally {
                saving = false
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("現在の状況", color = Color(0xFF16304E), fontSize = 26.sp,
            fontWeight = FontWeight.Bold)
        Text("新しい回答を保存するまでは、元の回答を残します。",
            color = Color(0xFF617991), fontSize = 14.sp)
        OutlinedButton(onClick = { status = "SAFE" }, enabled = !saving,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2866C9)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (status == "SAFE") "✓ 無事" else "無事")
        }
        OutlinedButton(onClick = { status = "WORRIED" }, enabled = !saving,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2866C9)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (status == "WORRIED") "✓ 困っている" else "困っている")
        }
        if (status == "WORRIED") {
            val choices = listOf(
                "ACCIDENT" to "事故", "EARTHQUAKE" to "地震", "FIRE" to "火災",
                "LANDSLIDE" to "土砂崩れ", "HEATSTROKE" to "熱中症"
            )
            choices.forEach { (code, label) ->
                OutlinedButton(onClick = { detail = if (detail == code) null else code },
                    enabled = !saving, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2866C9))) {
                    Text(if (detail == code) "✓ $label" else label)
                }
            }
            OutlinedTextField(value = comment, onValueChange = { comment = it },
                enabled = !saving, label = { Text("コメント") },
                modifier = Modifier.fillMaxWidth(), minLines = 3)
        }
        error?.let { Text(it, color = Color(0xFFAC3341), fontSize = 14.sp) }
        Button(onClick = { save() }, enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2866C9), contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(if (saving) "保存中…" else "新しい回答を保存")
        }
        OutlinedButton(onClick = onBack, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("キャンセル")
        }
    }
}
