package com.example.appopencounter.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsRoute(
    viewModelFactory: SettingsViewModel.Factory,
    onBack: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsageAccessStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenUsageAccessSettings = onOpenUsageAccessSettings,
        onUsageCollectionEnabledChange = viewModel::setUsageCollectionEnabled,
        onRequestClearData = viewModel::requestClearDataConfirmation,
        onDismissClearData = viewModel::dismissClearDataConfirmation,
        onConfirmClearData = viewModel::confirmClearData,
        onShowPrivacy = viewModel::showPrivacyDialog,
        onDismissPrivacy = viewModel::dismissPrivacyDialog,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onUsageCollectionEnabledChange: (Boolean) -> Unit,
    onRequestClearData: () -> Unit,
    onDismissClearData: () -> Unit,
    onConfirmClearData: () -> Unit,
    onShowPrivacy: () -> Unit,
    onDismissPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UsageAccessStatusCard(
                hasUsageAccess = uiState.hasUsageAccess,
                onOpenUsageAccessSettings = onOpenUsageAccessSettings,
            )
            UsageCollectionSwitchCard(
                enabled = uiState.usageCollectionEnabled,
                onEnabledChange = onUsageCollectionEnabledChange,
            )
            LocalDataCard(
                isClearingData = uiState.isClearingData,
                onRequestClearData = onRequestClearData,
            )
            PrivacyCard(onShowPrivacy = onShowPrivacy)
        }
    }

    if (uiState.showClearDataConfirmation) {
        ClearDataConfirmationDialog(
            isClearingData = uiState.isClearingData,
            onDismiss = onDismissClearData,
            onConfirm = onConfirmClearData,
        )
    }

    if (uiState.showPrivacyDialog) {
        PrivacyDialog(onDismiss = onDismissPrivacy)
    }
}

@Composable
private fun UsageAccessStatusCard(
    hasUsageAccess: Boolean,
    onOpenUsageAccessSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Usage Access 权限",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (hasUsageAccess) "已开启" else "未开启，无法统计 App 打开次数",
                color = if (hasUsageAccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenUsageAccessSettings) {
                Text(text = "打开系统权限设置")
            }
        }
    }
}

@Composable
private fun UsageCollectionSwitchCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "统计开关",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "关闭后不会写入新的统计数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun LocalDataCard(
    isClearingData: Boolean,
    onRequestClearData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "本地数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "清除后会删除本机保存的 App 信息和每日打开次数。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
            Button(
                onClick = onRequestClearData,
                enabled = !isClearingData,
            ) {
                Text(text = if (isClearingData) "清除中..." else "清除本地统计数据")
            }
        }
    }
}

@Composable
private fun PrivacyCard(
    onShowPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "隐私说明",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "查看我们如何处理你的本地统计数据。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
            OutlinedButton(onClick = onShowPrivacy) {
                Text(text = "查看隐私说明")
            }
        }
    }
}

@Composable
private fun ClearDataConfirmationDialog(
    isClearingData: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!isClearingData) {
                onDismiss()
            }
        },
        title = { Text(text = "确认清除本地数据？") },
        text = {
            Text(text = "此操作会删除本机保存的 App 信息和每日打开次数，无法撤销。")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isClearingData,
            ) {
                Text(text = "确认清除")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isClearingData,
            ) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun PrivacyDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "隐私说明") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "我们只统计 App 被打开的次数，不读取 App 内的聊天内容、浏览内容、输入内容、账号信息或文件内容。")
                Text(text = "数据默认保存在你的设备本地，不会未经同意上传。")
                Text(text = "你可以随时关闭统计，也可以清除本地统计数据。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "我知道了")
            }
        },
    )
}
