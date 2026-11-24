package com.example.anpilink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Web版のモバイル表示に合わせたログインフォーム。
 * 認証処理と状態管理は呼び出し元で行う。
 */
@Composable
fun LoginFormContent(
    loginId: String,
    password: String,
    loading: Boolean,
    errorMessage: String?,
    onLoginIdChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navy = Color(0xFF16304E)
    val blue = Color(0xFF2866C9)
    val muted = Color(0xFF7C8DA1)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF25415E),
        unfocusedTextColor = Color(0xFF25415E),
        disabledTextColor = Color(0xFF718096),
        focusedContainerColor = Color(0xFFFBFCFE),
        unfocusedContainerColor = Color(0xFFFBFCFE),
        disabledContainerColor = Color(0xFFF3F6FB),
        focusedBorderColor = blue,
        unfocusedBorderColor = Color(0xFFD9E3EF),
        disabledBorderColor = Color(0xFFD9E3EF),
        cursorColor = blue
    )

    Column(
        modifier = modifier
            .widthIn(max = 344.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "AnpiLink",
            color = navy,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(25.dp))

        Text(
            text = "ログイン",
            color = navy,
            fontSize = 30.sp,
            lineHeight = 45.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "アカウント情報を入力してください",
            color = muted,
            fontSize = 12.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(31.dp))

        if (errorMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF2F3),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFEFC5CA)
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),
                    color = Color(0xFFC44350),
                    fontSize = 13.sp,
                    lineHeight = 21.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = "ユーザーID",
            color = navy,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = loginId,
            onValueChange = { value ->
                if (value.length <= 100) {
                    onLoginIdChange(value)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            enabled = !loading,
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
            textStyle = TextStyle(
                fontSize = 14.sp
            ),
            placeholder = {
                Text(
                    text = "ユーザーIDを入力",
                    color = Color(0xFF9AA9BA),
                    fontSize = 12.sp
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            colors = fieldColors
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "パスワード",
            color = navy,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            enabled = !loading,
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
            textStyle = TextStyle(
                fontSize = 14.sp
            ),
            placeholder = {
                Text(
                    text = "パスワードを入力",
                    color = Color(0xFF9AA9BA),
                    fontSize = 12.sp
                )
            },
            visualTransformation =
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            colors = fieldColors
        )

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = onLogin,
            enabled = !loading
                    && loginId.isNotBlank()
                    && password.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = blue,
                contentColor = Color.White,
                disabledContainerColor =
                    blue.copy(alpha = 0.45f),
                disabledContentColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = if (loading) {
                        "処理中…"
                    } else {
                        "ログイン"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Web版と同じ右向きの矢印
                Canvas(
                    modifier = Modifier.size(19.dp)
                ) {
                    val stroke = 1.7.dp.toPx()
                    val centerY = size.height / 2

                    drawLine(
                        color = Color.White,
                        start = Offset(
                            size.width * 0.2f,
                            centerY
                        ),
                        end = Offset(
                            size.width * 0.8f,
                            centerY
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = Color.White,
                        start = Offset(
                            size.width * 0.58f,
                            size.height * 0.28f
                        ),
                        end = Offset(
                            size.width * 0.8f,
                            centerY
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = Color.White,
                        start = Offset(
                            size.width * 0.58f,
                            size.height * 0.72f
                        ),
                        end = Offset(
                            size.width * 0.8f,
                            centerY
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        TextButton(
            onClick = onRegister,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = blue
            )
        ) {
            Text(
                text = "新規登録（保護者）",
                fontSize = 14.sp
            )
        }
    }
}