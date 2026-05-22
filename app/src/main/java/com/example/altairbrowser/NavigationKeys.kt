package com.example.altairbrowser

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// ホーム画面（アドレス入力＋履歴）
@Serializable data object Home : NavKey

// ブラウザ画面（全画面WebView）
// url: 接続先URL（例: http://127.0.0.1:8765）
@Serializable data class Browser(val url: String) : NavKey
