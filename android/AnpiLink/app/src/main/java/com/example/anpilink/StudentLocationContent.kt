package com.example.anpilink

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import org.json.JSONException

/**
 * 最後に送信した回答の位置情報を表示する。
 * ヘッダー、スクロール、フッターは親画面で表示する。
 */
@Composable
fun StudentLocationContent(
    session: LoginSession,
    onBack: () -> Unit
) {
    val navy = Color(0xFF16304E)
    val muted = Color(0xFF8192A6)

    var latestReport by remember(session) {
        mutableStateOf<StudentLatestReportLocation?>(null)
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
            latestReport = StudentLocationApi.getLatest(session)

        } catch (e: CancellationException) {
            throw e

        } catch (e: StudentLocationApiException) {
            errorMessage = e.message
                ?: "位置情報を取得できませんでした。"

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
                text = "位置情報",
                modifier = Modifier.weight(1f),
                color = navy,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE0E8F2)
                )
            ) {
                Text(
                    text = "← 戻る",
                    color = Color(0xFF2866C9),
                    fontSize = 14.sp
                )
            }
        }

        val error = errorMessage
        val report = latestReport

        when {
            loading -> {
                Text(
                    text = "位置情報を読み込んでいます。",
                    color = muted,
                    fontSize = 14.sp
                )
            }

            error != null -> {
                Text(
                    text = error,
                    color = Color(0xFFB33A48),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            else -> {
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
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {
                        val location = report?.location

                        // 位置の有無にかかわらず、同じ最新回答の内容を1行で表示する。
                        if (report != null) {
                            Text(
                                text = "送信した回答：" + formatStudentSentAnswer(report),
                                modifier = Modifier.fillMaxWidth(),
                                color = navy,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (report == null) {
                            Text(
                                text = "安否回答はまだありません。",
                                color = muted,
                                fontSize = 14.sp
                            )

                        } else if (location == null) {
                            Text(
                                text = "未取得",
                                color = muted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text =
                                    "最後に送信した回答に" +
                                            "位置情報はありません。",
                                color = navy,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )

                            Text(
                                text = "送信日時：" +
                                        formatStudentLocationTime(
                                            report.reportedAt
                                        ),
                                color = muted,
                                fontSize = 12.sp
                            )

                        } else {
                            StudentSavedLocationMap(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )

                            Text(
                                text = "取得済み",
                                color = navy,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text =
                                    "最後に送信した回答の位置情報です。",
                                color = navy,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )

                            Text(
                                text = String.format(
                                    Locale.JAPAN,
                                    "緯度：%.6f\n経度：%.6f\n精度：約%.0f m",
                                    location.latitude,
                                    location.longitude,
                                    location.locationAccuracy
                                ),
                                color = navy,
                                fontSize = 14.sp,
                                lineHeight = 24.sp
                            )

                            Text(
                                text = "取得日時：" +
                                        formatStudentLocationTime(
                                            location.locationAcquiredAt
                                        ),
                                color = muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 保存済みの位置を地図に表示する。
 * 端末の現在位置は取得しない。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StudentSavedLocationMap(
    latitude: Double,
    longitude: Double
) {
    val mapUrl = remember(latitude, longitude) {
        val west = (longitude - 0.005).coerceAtLeast(-180.0)
        val east = (longitude + 0.005).coerceAtMost(180.0)
        val south = (latitude - 0.003).coerceIn(-85.0, 85.0)
        val north = (latitude + 0.003).coerceIn(-85.0, 85.0)

        Uri.parse(
            "https://www.openstreetmap.org/export/embed.html"
        ).buildUpon()
            .appendQueryParameter(
                "bbox",
                "$west,$south,$east,$north"
            )
            .appendQueryParameter("layer", "mapnik")
            .appendQueryParameter(
                "marker",
                "$latitude,$longitude"
            )
            .build()
            .toString()
    }

    var mapError by remember(mapUrl) {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true

                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setGeolocationEnabled(false)

                    webViewClient = object : WebViewClient() {

                        override fun onPageStarted(
                            view: WebView,
                            url: String,
                            favicon: Bitmap?
                        ) {
                            mapError = false

                            // URLには位置情報が含まれるため記録しない。
                            Log.d(
                                "AnpiLinkMap",
                                "地図ページの読み込み開始"
                            )
                        }

                        override fun onPageFinished(
                            view: WebView,
                            url: String
                        ) {
                            Log.d(
                                "AnpiLinkMap",
                                "地図ページの読み込み終了"
                            )

                            // 表示サイズが確定してから地図へ通知する。
                            view.post {
                                Log.d(
                                    "AnpiLinkMap",
                                    "WebViewサイズ：" +
                                            "${view.width}x${view.height}"
                                )

                                view.evaluateJavascript(
                                    """
            (function() {
                window.dispatchEvent(new Event('resize'));
            })();
            """.trimIndent(),
                                    null
                                )

                                view.invalidate()
                            }
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            Log.w(
                                "AnpiLinkMap",
                                "地図の通信エラー：" +
                                        "code=${error.errorCode}, " +
                                        "mainFrame=" +
                                        request.isForMainFrame
                            )

                            if (request.isForMainFrame) {
                                mapError = true
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: WebResourceResponse
                        ) {
                            Log.w(
                                "AnpiLinkMap",
                                "地図のHTTPエラー：" +
                                        "status=" +
                                        errorResponse.statusCode +
                                        ", mainFrame=" +
                                        request.isForMainFrame
                            )

                            if (request.isForMainFrame) {
                                mapError = true
                            }
                        }
                    }

                    loadUrl(mapUrl)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            }
        )

        if (mapError) {
            Text(
                text =
                    "地図を表示できませんでした。" +
                            "位置情報は下の緯度・経度で確認できます。",
                color = Color(0xFF8192A6),
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * 日本時間で日時を表示する。
 */
private fun formatStudentLocationTime(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
            .format(
                DateTimeFormatter.ofPattern(
                    "yyyy/MM/dd HH:mm"
                )
            )
    }.getOrDefault(value)
}

/**
 * サーバーが返した同一回答の安否状況と種類をまとめる。
 * 不明な値を「無事」として表示しない。
 */
private fun formatStudentSentAnswer(report: StudentLatestReportLocation): String {
    val status = when (report.status) {
        "SAFE" -> "無事"
        "DANGER" -> "危険"
        "WORRIED" -> "困っている"
        else -> return "確認できません"
    }
    if (report.status == "SAFE") return status
    // 「困っている」はDBの名称を優先する。旧サーバーの固定項目にも対応する。
    val dynamicName = if (report.status == "WORRIED" && report.dangerType != null) {
        report.dangerTypeName?.takeIf { it.isNotBlank() }
    } else null
    val detail = dynamicName ?: when (report.dangerType) {
        null -> null
        "ACCIDENT" -> "事故"
        "EARTHQUAKE" -> "地震"
        "FIRE" -> "火災"
        "LANDSLIDE" -> "土砂崩れ"
        "HEATSTROKE" -> "熱中症"
        "STALKING" -> "不審者・つきまとい"
        else -> "種類不明"
    }
    return if (detail == null) status else "$status（$detail）"
}
