package com.example.appopencounter.ui.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    onOpenUsageAccessSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "开启使用统计权限",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "开启 App 使用统计后，我们将帮助你记录每天各个 App 被打开的次数。",
                modifier = Modifier
                    .padding(top = 16.dp)
                    .widthIn(max = 420.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "我们只统计 App 被打开的次数，不读取 App 内的聊天、浏览、输入或文件内容。",
                modifier = Modifier
                    .padding(top = 12.dp)
                    .widthIn(max = 420.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onOpenUsageAccessSettings,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(text = "去开启权限")
            }
        }
    }
}
