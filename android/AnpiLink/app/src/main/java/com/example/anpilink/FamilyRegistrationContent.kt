package com.example.anpilink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException

@Composable
fun FamilyRegistrationContent(onBackToLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var loginId by rememberSaveable { mutableStateOf("") }
    var studentLoginId by rememberSaveable { mutableStateOf("") }

    // パスワードは端末の保存状態に含めない。
    var password by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler {
        if (!sending) onBackToLogin()
    }

    Column(
        modifier = Modifier.widthIn(max = 344.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AnpiLink", color = RegistrationNavy, fontSize = 32.sp,
            lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            text = if (completed) "登録完了" else "保護者の新規登録",
            color = RegistrationNavy, fontSize = 28.sp, lineHeight = 38.sp,
            fontWeight = FontWeight.Bold
        )
        if (completed) {
            Text("保護者アカウントを登録しました。", color = RegistrationNavy)
            Surface(color = Color(0xFFFFF5DC), shape = RoundedCornerShape(12.dp)) {
                Text("親子関係の承認待ち", modifier = Modifier.padding(12.dp, 6.dp),
                    color = Color(0xFF88600C), fontWeight = FontWeight.Bold)
            }
            Text("職員の承認後に閲覧できます。", color = RegistrationNavy)
            RegistrationButton("ログインへ", onClick = onBackToLogin)
        } else {
            RegistrationField("氏名", name, !sending,
                onValueChange = { if (it.length <= 40) name = it })
            RegistrationField("ユーザーID", loginId, !sending,
                keyboardType = KeyboardType.Ascii,
                onValueChange = { if (it.length <= 40) loginId = it })
            RegistrationField("パスワード", password, !sending,
                keyboardType = KeyboardType.Password,
                onValueChange = { password = it })
            RegistrationField("お子さんの生徒ID", studentLoginId, !sending,
                keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done,
                onValueChange = { if (it.length <= 40) studentLoginId = it })

            error?.let { message ->
                Surface(color = Color(0xFFFFF2F3), shape = RoundedCornerShape(9.dp)) {
                    Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = Color(0xFFB73D4F), fontSize = 14.sp)
                }
            }

            RegistrationButton(
                text = if (sending) "登録中…" else "登録する",
                enabled = !sending && name.isNotBlank() && loginId.isNotBlank()
                        && password.isNotEmpty() && studentLoginId.isNotBlank(),
                onClick = {
                    if (!sending) {
                        if (password.toByteArray(Charsets.UTF_8).size > 72) {
                            error = "パスワードはUTF-8で1～72バイトにしてください。"
                        } else {
                            sending = true
                            error = null
                            scope.launch {
                                try {
                                    FamilyRegistrationApi.register(
                                        name, loginId, password, studentLoginId
                                    )
                                    password = ""
                                    completed = true
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: FamilyRegistrationException) {
                                    error = e.message
                                } catch (e: IOException) {
                                    error = "通信エラーで登録結果を確認できませんでした。" +
                                            "接続を確認し、入力したIDでログインをお試しください。"
                                } catch (e: JSONException) {
                                    error = "サーバーの登録結果を確認できませんでした。"
                                } finally {
                                    sending = false
                                }
                            }
                        }
                    }
                }
            )
            TextButton(
                onClick = onBackToLogin, enabled = !sending,
                colors = ButtonDefaults.textButtonColors(contentColor = RegistrationBlue)
            ) {
                Text("ログインへ")
            }
        }
    }
}

private val RegistrationNavy = Color(0xFF16304E)
private val RegistrationBlue = Color(0xFF2866C9)

@Composable
private fun RegistrationField(
    label: String,
    value: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(label, color = RegistrationNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            shape = RoundedCornerShape(9.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            visualTransformation = if (keyboardType == KeyboardType.Password) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RegistrationNavy,
                unfocusedTextColor = RegistrationNavy,
                focusedContainerColor = Color(0xFFFBFCFE),
                unfocusedContainerColor = Color(0xFFFBFCFE),
                focusedBorderColor = RegistrationBlue,
                unfocusedBorderColor = Color(0xFFD9E3EF),
                cursorColor = RegistrationBlue
            )
        )
    }
}

@Composable
private fun RegistrationButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RegistrationBlue, contentColor = Color.White)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
