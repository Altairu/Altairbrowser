package com.example.altairbrowser.ui.browser

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.altairbrowser.theme.*

// ブラウザ画面（全画面WebView）
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // WebViewの参照を保持（戻るボタン制御に使用）
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Androidの戻るボタン処理
    // WebView内で戻れる場合は戻る、戻れない場合はホーム画面へ
    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // WebView本体（全画面）
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        // JavaScriptを有効化
                        javaScriptEnabled = true
                        // DOMストレージを有効化（ローカルストレージ使用アプリ対応）
                        domStorageEnabled = true
                        // ビューポートのサポート
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        // ズーム操作を許可
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }

                    // 読み込み進捗・完了・エラーの追跡
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loadingProgress = newProgress
                            isLoading = newProgress < 100
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                            isLoading = false
                        }
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            // メインフレームのエラーのみ表示
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                errorMessage = error?.description?.toString()
                                    ?: "接続できませんでした"
                                isLoading = false
                            }
                        }

                        override fun onPageStarted(
                            view: WebView?,
                            loadedUrl: String?,
                            favicon: android.graphics.Bitmap?,
                        ) {
                            hasError = false
                            isLoading = true
                        }
                    }

                    // URLを読み込む
                    loadUrl(url)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ページ読み込み中のプログレスバー（上部）
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = ElectricBlue,
                trackColor = Color.Transparent,
            )
        }

        // エラー表示オーバーレイ
        AnimatedVisibility(
            visible = hasError,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepNavy),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text(
                        text = "⚠",
                        fontSize = 48.sp,
                        color = ErrorRed,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "接続エラー",
                        color = TextPrimary,
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = url,
                        color = ElectricBlueLight,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "戻るボタンで入力画面に戻る",
                        color = TextHint,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
