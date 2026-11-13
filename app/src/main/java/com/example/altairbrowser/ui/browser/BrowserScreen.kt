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
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import com.example.altairbrowser.MainActivity
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

    // 全画面表示用（動画やフルスクリーン要素）
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    
    val context = LocalContext.current

    // ブラウザ画面表示中はシステムバーを非表示にし、画面を離れるときに元に戻す
    DisposableEffect(Unit) {
        (context as? MainActivity)?.hideSystemUI()
        onDispose {
            (context as? MainActivity)?.showSystemUI()
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Androidの戻るボタン処理
    // 全画面表示中の場合は通常画面に戻す。それ以外はWebView内を戻るかホーム画面へ。
    BackHandler {
        if (customView != null) {
            // 全画面を閉じて縦画面(ユーザー設定)に戻す
            customViewCallback?.onCustomViewHidden()
            customView = null
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            val wv = webView
            if (wv != null && wv.canGoBack()) {
                wv.goBack()
            } else {
                onBack()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // WebView本体（全画面）
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    // ハードウェアアクセラレーションを有効化して動画・Canvas等の黒画面を防ぐ
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    settings.apply {
                        // JavaScriptを有効化
                        javaScriptEnabled = true
                        // DOMストレージ・DBを有効化（ローカルストレージ使用アプリ対応）
                        domStorageEnabled = true
                        databaseEnabled = true
                        // HTTPページ内リソースの混在コンテンツを許可
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        // ビューポートのサポート
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        // ズーム操作を許可
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        // メディアの自動再生を許可
                        mediaPlaybackRequiresUserGesture = false
                    }

                    // 読み込み進捗・完了・エラーの追跡と、全画面機能のサポート
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loadingProgress = newProgress
                            isLoading = newProgress < 100
                        }

                        // フルスクリーン要求（動画など）があったときの処理
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            super.onShowCustomView(view, callback)
                            if (customView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customView = view
                            customViewCallback = callback
                            
                            // 横画面に固定する
                            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }

                        // フルスクリーン終了時の処理
                        override fun onHideCustomView() {
                            super.onHideCustomView()
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                            
                            // 画面の向きを元に戻す
                            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

        // 全画面ビューがある場合はそれを最前面に表示する
        customView?.let { targetView ->
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        (targetView.parent as? ViewGroup)?.removeView(targetView)
                        addView(
                            targetView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                },
                update = { container ->
                    if (targetView.parent !== container) {
                        container.removeAllViews()
                        (targetView.parent as? ViewGroup)?.removeView(targetView)
                        container.addView(
                            targetView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

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
                    .height(2.dp),
                color = AccentBlue,
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
                    .background(BackgroundDark),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text(
                        text = "ページを開けませんでした",
                        color = TextPrimary,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = url,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "戻る操作で入力画面に戻ります",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
