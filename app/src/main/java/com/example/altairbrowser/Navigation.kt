package com.example.altairbrowser

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.altairbrowser.data.HistoryEntry
import com.example.altairbrowser.data.HistoryRepository
import com.example.altairbrowser.ui.browser.BrowserScreen
import com.example.altairbrowser.ui.main.HomeScreen

// メインナビゲーション
// Home <-> Browser の2画面構成
@Composable
fun MainNavigation() {
    val context = LocalContext.current

    // 履歴リポジトリ（コンテキストを渡して初期化）
    val historyRepository = remember { HistoryRepository(context) }

    // 履歴をStateとして収集
    val history by historyRepository.history.collectAsState()

    // バックスタックの初期値はHome画面
    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            // ホーム画面（アドレス入力＋履歴）
            entry<Home> {
                HomeScreen(
                    history = history,
                    onNavigate = { host, port ->
                        // 履歴に追加
                        historyRepository.addEntry(host, port)
                        // ブラウザ画面に遷移
                        backStack.add(Browser(url = "http://$host:$port"))
                    },
                    onRemoveEntry = { entry ->
                        // 個別削除
                        historyRepository.removeEntry(entry)
                    },
                    onClearAll = {
                        // 全件削除
                        historyRepository.clearAll()
                    },
                )
            }

            // ブラウザ画面（全画面WebView）
            entry<Browser> { key ->
                BrowserScreen(
                    url = key.url,
                    onBack = {
                        // ホーム画面に戻る
                        backStack.removeLastOrNull()
                    },
                )
            }
        },
    )
}
