package com.example.altairbrowser.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.altairbrowser.data.HistoryEntry
import com.example.altairbrowser.theme.*
import com.example.altairbrowser.update.ReleaseInfo
import com.example.altairbrowser.update.UpdateCheckResult
import com.example.altairbrowser.update.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

// ホーム画面（アドレス入力＋履歴リスト）
@Composable
fun HomeScreen(
    history: List<HistoryEntry>,
    onNavigate: (host: String, port: String) -> Unit,
    onRemoveEntry: (HistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("8765") }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val portFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val currentVersion = remember { UpdateManager.getCurrentVersionName(context) }

    var showClearDialog by remember { mutableStateOf(false) }
    var availableRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }

    // 起動時にGitHub Releasesから最新バージョンを自動チェック
    LaunchedEffect(Unit) {
        when (val result = UpdateManager.checkForUpdate(context)) {
            is UpdateCheckResult.UpdateAvailable -> {
                availableRelease = result.release
            }
            else -> Unit
        }
    }

    availableRelease?.let { release ->
        UpdateDialog(
            currentVersion = currentVersion,
            release = release,
            onDismiss = { availableRelease = null },
        )
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                onClearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        ) {
            item {
                HeaderSection(
                    currentVersion = currentVersion,
                    isCheckingUpdate = isCheckingUpdate,
                    updateStatusMessage = updateStatusMessage,
                    onCheckUpdateClick = {
                        if (!isCheckingUpdate) {
                            coroutineScope.launch {
                                isCheckingUpdate = true
                                updateStatusMessage = null
                                when (val result = UpdateManager.checkForUpdate(context)) {
                                    is UpdateCheckResult.UpdateAvailable -> {
                                        availableRelease = result.release
                                    }
                                    is UpdateCheckResult.UpToDate -> {
                                        updateStatusMessage = "最新バージョンです"
                                    }
                                    UpdateCheckResult.NoReleaseFound -> {
                                        updateStatusMessage = "公開済みのリリースはありません"
                                    }
                                    is UpdateCheckResult.Error -> {
                                        updateStatusMessage = "確認できませんでした: ${result.message}"
                                    }
                                }
                                isCheckingUpdate = false
                            }
                        }
                    },
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            item {
                InputSection(
                    host = host,
                    port = port,
                    onHostChange = { host = it },
                    onPortChange = { port = it },
                    portFocusRequester = portFocusRequester,
                    onOpenClick = {
                        focusManager.clearFocus()
                        if (host.isNotBlank() && port.isNotBlank()) {
                            onNavigate(host, port)
                        }
                    },
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (history.isNotEmpty()) {
                item {
                    HistoryHeader(onClearAll = { showClearDialog = true })
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    HistoryListCard(
                        history = history,
                        onSelect = { entry ->
                            host = entry.host
                            port = entry.port
                            onNavigate(entry.host, entry.port)
                        },
                        onDelete = onRemoveEntry,
                    )
                }
            } else {
                item {
                    EmptyHistoryHint()
                }
            }
        }
    }
}

// ミニマルなヘッダー
@Composable
private fun HeaderSection(
    currentVersion: String,
    isCheckingUpdate: Boolean,
    updateStatusMessage: String?,
    onCheckUpdateClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Altair Browser",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "v$currentVersion",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            TextButton(
                onClick = onCheckUpdateClick,
                enabled = !isCheckingUpdate,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 1.8.dp,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "確認中",
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )
                } else {
                    Text(
                        text = "更新確認",
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (updateStatusMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = updateStatusMessage,
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

// 接続先入力フォーム
@Composable
private fun InputSection(
    host: String,
    port: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    portFocusRequester: FocusRequester,
    onOpenClick: () -> Unit,
) {
    val isValid = host.isNotBlank() && port.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "127.0.0.1",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                    )
                },
                label = {
                    Text("ホスト", fontSize = 12.sp)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { portFocusRequester.requestFocus() },
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextMuted,
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                ),
            )

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = port,
                onValueChange = { input ->
                    if (input.length <= 5 && input.all { it.isDigit() }) {
                        onPortChange(input)
                    }
                },
                modifier = Modifier
                    .width(96.dp)
                    .focusRequester(portFocusRequester),
                placeholder = {
                    Text(
                        text = "8765",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                    )
                },
                label = {
                    Text("ポート", fontSize = 12.sp)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onOpenClick() }),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextMuted,
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onOpenClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = isValid,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TextPrimary,
                contentColor = BackgroundDark,
                disabledContainerColor = SurfaceElevated,
                disabledContentColor = TextMuted,
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
            ),
        ) {
            Text(
                text = "開く",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// 履歴セクションのヘッダー
@Composable
private fun HistoryHeader(onClearAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "履歴",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "すべて削除",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClearAll)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// 1つのクリーンなリストコンテナにまとめた履歴一覧
@Composable
private fun HistoryListCard(
    history: List<HistoryEntry>,
    onSelect: (HistoryEntry) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
    ) {
        history.forEachIndexed { index, entry ->
            HistoryRow(
                entry = entry,
                onClick = { onSelect(entry) },
                onDelete = { onDelete(entry) },
            )
            if (index < history.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = BorderSubtle,
                )
            }
        }
    }
}

// 個別の履歴行
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${entry.host}:${entry.port}",
            color = TextPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp),
        ) {
            CloseIcon(
                color = TextMuted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// 細いベクター風の閉じるアイコン
@Composable
private fun CloseIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = 1.6.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

// 全削除確認ダイアログ
@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .padding(22.dp),
        ) {
            Text(
                text = "履歴を削除",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "保存されている履歴をすべて削除します。",
                color = TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, BorderSubtle),
                ) {
                    Text("キャンセル", fontSize = 14.sp)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = TextPrimary,
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                ) {
                    Text("削除", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// 更新確認・ダウンロードダイアログ
@Composable
private fun UpdateDialog(
    currentVersion: String,
    release: ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canInstall by remember { mutableStateOf(UpdateManager.canInstallPackages(context)) }

    // 設定画面から戻ったときにインストール権限を再確認する
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canInstall = UpdateManager.canInstallPackages(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Dialog(onDismissRequest = { if (!isDownloading) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .padding(22.dp),
        ) {
            Text(
                text = "アップデートがあります",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "v$currentVersion -> v${release.versionName}",
                color = AccentBlue,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
            )

            if (release.releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    Text(
                        text = release.releaseNotes,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentBlue,
                    trackColor = BorderSubtle,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ダウンロード中 $downloadProgress%",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }

            if (downloadedFile != null && !canInstall) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "更新をインストールするには設定画面でこの提供元のアプリを許可してください。",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isDownloading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, BorderSubtle),
                ) {
                    Text("あとで", fontSize = 14.sp)
                }

                val readyFile = downloadedFile
                when {
                    readyFile != null && !canInstall -> {
                        Button(
                            onClick = { UpdateManager.openInstallPermissionSettings(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextPrimary,
                                contentColor = BackgroundDark,
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                        ) {
                            Text("設定を開く", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    readyFile != null -> {
                        Button(
                            onClick = { UpdateManager.installApk(context, readyFile) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextPrimary,
                                contentColor = BackgroundDark,
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                        ) {
                            Text("インストール", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                if (!isDownloading) {
                                    coroutineScope.launch {
                                        isDownloading = true
                                        errorMessage = null
                                        downloadProgress = 0
                                        val result = UpdateManager.downloadApk(
                                            context = context,
                                            release = release,
                                            onProgress = { progress -> downloadProgress = progress },
                                        )
                                        isDownloading = false
                                        result.onSuccess { file ->
                                            downloadedFile = file
                                            canInstall = UpdateManager.canInstallPackages(context)
                                            if (canInstall) {
                                                UpdateManager.installApk(context, file)
                                            }
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage ?: "ダウンロードに失敗しました"
                                        }
                                    }
                                }
                            },
                            enabled = !isDownloading,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextPrimary,
                                contentColor = BackgroundDark,
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                        ) {
                            Text("更新する", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// 履歴が空の場合の控えめな表示
@Composable
private fun EmptyHistoryHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "履歴はありません",
            color = TextMuted,
            fontSize = 13.sp,
        )
    }
}
