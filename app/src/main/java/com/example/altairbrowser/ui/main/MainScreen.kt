package com.example.altairbrowser.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.altairbrowser.data.HistoryEntry
import com.example.altairbrowser.theme.*

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
    val focusManager = LocalFocusManager.current
    val portFocusRequester = remember { FocusRequester() }

    // 全削除確認ダイアログの表示フラグ
    var showClearDialog by remember { mutableStateOf(false) }

    // 全削除確認ダイアログ
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
            .background(
                Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        ) {

            // ヘッダーロゴ部分
            item {
                HeaderSection()
                Spacer(modifier = Modifier.height(32.dp))
            }

            // アドレス入力カード
            item {
                InputCard(
                    host = host,
                    port = port,
                    onHostChange = { host = it },
                    onPortChange = { port = it },
                    portFocusRequester = portFocusRequester,
                    onPortDone = {
                        focusManager.clearFocus()
                        if (host.isNotBlank() && port.isNotBlank()) {
                            onNavigate(host, port)
                        }
                    },
                    onOpenClick = {
                        focusManager.clearFocus()
                        if (host.isNotBlank() && port.isNotBlank()) {
                            onNavigate(host, port)
                        }
                    },
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            // 履歴セクションのヘッダー
            if (history.isNotEmpty()) {
                item {
                    HistoryHeader(onClearAll = { showClearDialog = true })
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 履歴リスト（新しい順）
                itemsIndexed(
                    items = history,
                    key = { _, entry -> "${entry.host}:${entry.port}" },
                ) { index, entry ->
                    // 削除アニメーション付きで表示
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        ),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    ) {
                        HistoryItem(
                            entry = entry,
                            index = index,
                            onClick = {
                                host = entry.host
                                port = entry.port
                                onNavigate(entry.host, entry.port)
                            },
                            onDelete = { onRemoveEntry(entry) },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                // 履歴がない場合のヒント
                item {
                    EmptyHistoryHint()
                }
            }
        }
    }
}

// ヘッダーロゴセクション
@Composable
private fun HeaderSection() {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // アイコン
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ElectricBlue, ElectricBlueDark),
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✦",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Altair Browser",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "ローカルブラウザ",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
}

// アドレス入力カード
@Composable
private fun InputCard(
    host: String,
    port: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    portFocusRequester: FocusRequester,
    onPortDone: () -> Unit,
    onOpenClick: () -> Unit,
) {
    val isValid = host.isNotBlank() && port.isNotBlank()
    val buttonAlpha by animateFloatAsState(
        targetValue = if (isValid) 1f else 0.5f,
        label = "button_alpha",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = "接続先",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))

            // ホスト入力欄 + ポート入力欄
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ホスト入力フィールド
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "127.0.0.1",
                            color = TextHint,
                            fontFamily = FontFamily.Monospace,
                        )
                    },
                    label = {
                        Text("ホスト / IP", color = TextSecondary, fontSize = 11.sp)
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
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ElectricBlue,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedLabelColor = ElectricBlueLight,
                        unfocusedLabelColor = TextSecondary,
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                )

                Spacer(modifier = Modifier.width(10.dp))

                // ポート入力フィールド（幅固定）
                OutlinedTextField(
                    value = port,
                    onValueChange = { if (it.length <= 5 && it.all { c -> c.isDigit() }) onPortChange(it) },
                    modifier = Modifier
                        .width(90.dp)
                        .focusRequester(portFocusRequester),
                    placeholder = {
                        Text("8765", color = TextHint, fontFamily = FontFamily.Monospace)
                    },
                    label = {
                        Text("ポート", color = TextSecondary, fontSize = 11.sp)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onPortDone() }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ElectricBlue,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedLabelColor = ElectricBlueLight,
                        unfocusedLabelColor = TextSecondary,
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 接続URLプレビュー
            if (host.isNotBlank() && port.isNotBlank()) {
                Text(
                    text = "→  http://$host:$port",
                    color = ElectricBlueLight,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.alpha(0.8f),
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 開くボタン
            Button(
                onClick = onOpenClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .alpha(buttonAlpha),
                enabled = isValid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = Color.White,
                    disabledContainerColor = NavyBorder,
                    disabledContentColor = TextHint,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp,
                ),
            ) {
                Text(
                    text = "開く",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

// 履歴セクションのヘッダー（全削除ボタン付き）
@Composable
private fun HistoryHeader(onClearAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(ElectricBlue, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "履歴",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f),
        )
        // 全削除ボタン
        TextButton(
            onClick = onClearAll,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "全削除",
                color = ErrorRed.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// 個別の履歴アイテム（×ボタン付き）
@Composable
private fun HistoryItem(
    entry: HistoryEntry,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = ElectricBlue),
                    onClick = onClick,
                )
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // インデックス番号バッジ
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (index == 0) ElectricBlue.copy(alpha = 0.2f)
                        else NavyBorder,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    color = if (index == 0) ElectricBlueLight else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                // ホスト:ポート表示
                Text(
                    text = "${entry.host} : ${entry.port}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.toUrl(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // 矢印アイコン
            Text(
                text = "›",
                color = ElectricBlue,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            // 個別削除ボタン（×）
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Text(
                    text = "✕",
                    color = TextHint,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

// 全削除確認ダイアログ
@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🗑️",
                    fontSize = 36.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "履歴を全件削除",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "すべての履歴を削除します。\nこの操作は元に戻せません。",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // キャンセルボタン
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary,
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                    ) {
                        Text("キャンセル")
                    }
                    // 削除確定ボタン
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("削除する", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 履歴が空の場合のヒント表示
@Composable
private fun EmptyHistoryHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📋",
                fontSize = 36.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "まだ履歴がありません",
                color = TextSecondary,
                fontSize = 14.sp,
            )
            Text(
                text = "接続すると履歴が保存されます",
                color = TextHint,
                fontSize = 12.sp,
            )
        }
    }
}
