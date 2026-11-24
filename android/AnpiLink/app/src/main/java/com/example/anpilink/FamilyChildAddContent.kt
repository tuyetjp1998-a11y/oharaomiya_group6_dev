package com.example.anpilink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException

/**
 * お子さんの追加申請フォーム。
 */
@Composable
fun FamilyChildAddContent(
    session: LoginSession,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit
) {
    var studentLoginId by remember(session) {
        mutableStateOf("")
    }

    var sending by remember(session) {
        mutableStateOf(false)
    }

    var errorMessage by remember(session) {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val navy = Color(0xFF16304E)
    val blue = Color(0xFF2866C9)

    // 送信中は戻る操作を受け付けない。
    BackHandler {
        if (!sending) {
            onBack()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(
            enabled = !sending,
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(
                contentColor = blue
            )
        ) {
            Text("← 設定")
        }

        Text(
            text = "お子さんの追加申請",
            color = navy,
            fontSize = 26.sp,
            lineHeight = 36.sp,
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
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "お子さんの生徒ID",
                    color = navy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = studentLoginId,
                    onValueChange = { value ->
                        if (value.length <= 40) {
                            studentLoginId = value
                            errorMessage = null
                        }
                    },
                    enabled = !sending,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii
                    ),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFB33A48),
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                }

                Button(
                    enabled = !sending &&
                            studentLoginId.trim().isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blue,
                        contentColor = Color.White
                    ),
                    onClick = {
                        val submittedId = studentLoginId.trim()

                        if (!sending &&
                            submittedId.isNotEmpty() &&
                            submittedId.length <= 40
                        ) {
                            focusManager.clearFocus()
                            sending = true
                            errorMessage = null

                            scope.launch {
                                try {
                                    val result =
                                        FamilyChildRequestApi.submit(
                                            session = session,
                                            studentLoginId = submittedId
                                        )

                                    // Webと同じく申請履歴へ進む。
                                    onSubmitted(result.message)

                                } catch (e: CancellationException) {
                                    throw e

                                } catch (e: FamilyApiException) {
                                    errorMessage = e.message
                                        ?: "申請できませんでした。"

                                } catch (e: IOException) {
                                    errorMessage =
                                        "通信に失敗し、申請結果を確認できませんでした。" +
                                                "申請が保存されている場合があります。" +
                                                "再送信する前に申請履歴を確認してください。"

                                } catch (e: JSONException) {
                                    errorMessage =
                                        "サーバーの応答を読み取れませんでした。" +
                                                "申請が保存されている場合があります。" +
                                                "申請履歴を確認してください。"

                                } finally {
                                    sending = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (sending) {
                            "申請中…"
                        } else {
                            "申請する"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}