package com.example.altairbrowser.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// 最新リリース情報
data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkName: String,
)

// 更新チェックの結果
sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data object NoReleaseFound : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

// GitHub Releases を使ったアプリ自動更新マネージャー
object UpdateManager {
    private const val GITHUB_REPO = "Altairu/Altairbrowser"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    // 現在のアプリバージョン名を取得
    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "0.0.0"
        } catch (_: Exception) {
            "0.0.0"
        }
    }

    // タグ名と現在のバージョンをセマンティックバージョニングで比較する
    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = parseVersionParts(current)
        val latestParts = parseVersionParts(latest)
        val maxLen = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until maxLen) {
            val cur = currentParts.getOrElse(i) { 0 }
            val lat = latestParts.getOrElse(i) { 0 }
            if (lat > cur) return true
            if (lat < cur) return false
        }
        return false
    }

    private fun parseVersionParts(version: String): List<Int> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        return cleaned.split(".")
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
    }

    // GitHub Releases から最新バージョンを確認する
    suspend fun checkForUpdate(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersionName(context)
        var connection: HttpURLConnection? = null
        try {
            val url = URL(LATEST_RELEASE_API)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AltairBrowser-Android")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.NoReleaseFound
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("GitHub応答エラー (HTTP $responseCode)")
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "")
            val versionName = tagName.trim().removePrefix("v").removePrefix("V")
            val releaseNotes = json.optString("body", "").trim()
            val assets = json.optJSONArray("assets")

            var apkDownloadUrl = ""
            var apkName = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkName = name
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        if (name.contains("release", ignoreCase = true)) {
                            break
                        }
                    }
                }
            }

            if (tagName.isBlank() || apkDownloadUrl.isBlank()) {
                return@withContext UpdateCheckResult.NoReleaseFound
            }

            if (isNewerVersion(currentVersion, versionName)) {
                UpdateCheckResult.UpdateAvailable(
                    ReleaseInfo(
                        tagName = tagName,
                        versionName = versionName,
                        releaseNotes = releaseNotes,
                        apkDownloadUrl = apkDownloadUrl,
                        apkName = apkName,
                    )
                )
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "更新の確認に失敗しました")
        } finally {
            connection?.disconnect()
        }
    }

    // APKファイルをキャッシュディレクトリにダウンロードする
    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Int) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // リダイレクトに追従してダウンロードする
            var currentUrl = release.apkDownloadUrl
            var redirectCount = 0
            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "AltairBrowser-Android")
                }
                val code = connection.responseCode
                if (code in listOf(
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        HttpURLConnection.HTTP_SEE_OTHER,
                        307,
                        308,
                    )
                ) {
                    val location = connection.getHeaderField("Location")
                        ?: return@withContext Result.failure(IllegalStateException("リダイレクト先が不明です"))
                    connection.disconnect()
                    currentUrl = location
                    redirectCount++
                    if (redirectCount > 5) {
                        return@withContext Result.failure(IllegalStateException("リダイレクト回数が上限を超えました"))
                    }
                } else {
                    break
                }
            }

            val activeConnection = connection!!
            val responseCode = activeConnection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    IllegalStateException("ダウンロードに失敗しました (HTTP $responseCode)")
                )
            }

            val totalBytes = activeConnection.contentLengthLong
            var downloadedBytes = 0L
            var lastReportedProgress = -1

            activeConnection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (progress != lastReportedProgress) {
                                lastReportedProgress = progress
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
            }
            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    // 不明なアプリのインストール権限が付与されているか確認する
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    // インストール権限許可の設定画面を開く
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // ダウンロードしたAPKのパッケージインストーラーを起動する
    fun installApk(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
