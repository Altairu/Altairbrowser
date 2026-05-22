package com.example.altairbrowser.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 履歴エントリー（ホスト＋ポートの組み合わせ）
data class HistoryEntry(
    val host: String,
    val port: String,
) {
    // 表示用テキスト
    fun displayText(): String = "$host : $port"
    // 接続用URL
    fun toUrl(): String = "http://$host:$port"
}

// SharedPreferencesを使った履歴管理リポジトリ
class HistoryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 最大保存件数
    private val maxHistoryCount = 20

    // 内部ステート（Flowで外部に公開）
    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    // 新しいエントリーを先頭に追加（重複は除去）
    fun addEntry(host: String, port: String) {
        val newEntry = HistoryEntry(host.trim(), port.trim())
        val currentList = _history.value.toMutableList()

        // 同じホスト:ポートが既にある場合は削除してから先頭に追加
        currentList.removeAll { it.host == newEntry.host && it.port == newEntry.port }
        currentList.add(0, newEntry)

        // 最大件数を超えた分は削除
        if (currentList.size > maxHistoryCount) {
            currentList.subList(maxHistoryCount, currentList.size).clear()
        }

        _history.value = currentList
        saveHistory(currentList)
    }

    // 特定のエントリーを削除
    fun removeEntry(entry: HistoryEntry) {
        val currentList = _history.value.toMutableList()
        currentList.removeAll { it.host == entry.host && it.port == entry.port }
        _history.value = currentList
        saveHistory(currentList)
    }

    // 履歴を全件削除
    fun clearAll() {
        _history.value = emptyList()
        prefs.edit().clear().apply()
    }

    // SharedPreferencesから履歴を読み込む
    private fun loadHistory(): List<HistoryEntry> {
        val count = prefs.getInt(KEY_COUNT, 0)
        return (0 until count).mapNotNull { i ->
            val host = prefs.getString("${KEY_HOST}_$i", null) ?: return@mapNotNull null
            val port = prefs.getString("${KEY_PORT}_$i", null) ?: return@mapNotNull null
            HistoryEntry(host, port)
        }
    }

    // SharedPreferencesに履歴を保存する
    private fun saveHistory(list: List<HistoryEntry>) {
        prefs.edit().apply {
            putInt(KEY_COUNT, list.size)
            list.forEachIndexed { i, entry ->
                putString("${KEY_HOST}_$i", entry.host)
                putString("${KEY_PORT}_$i", entry.port)
            }
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "altair_browser_history"
        private const val KEY_COUNT = "history_count"
        private const val KEY_HOST = "history_host"
        private const val KEY_PORT = "history_port"
    }
}
