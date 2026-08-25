package com.chenfa.openclashinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chenfa.openclashinstaller.data.model.ConnFields

/**
 * 设置对话框：3 个下载 URL + 4 个连接字段（IP/用户/密码/端口）。
 * 等价 Windows 版 IDD_SETTINGSBOX，但把连接字段也合并进来。
 */
@Composable
fun SettingsDialog(
    initialKernelUrl: String,
    initialOpenclashUrl: String,
    initialFanUrl: String,
    initialFields: ConnFields,
    onDismiss: () -> Unit,
    onSave: (kernelUrl: String, openclashUrl: String, fanUrl: String, fields: ConnFields) -> Unit,
) {
    var kernelUrl by remember { mutableStateOf(initialKernelUrl) }
    var openclashUrl by remember { mutableStateOf(initialOpenclashUrl) }
    var fanUrl by remember { mutableStateOf(initialFanUrl) }
    var ip by remember { mutableStateOf(initialFields.ip) }
    var user by remember { mutableStateOf(initialFields.user) }
    var password by remember { mutableStateOf(initialFields.password) }
    var port by remember { mutableStateOf(initialFields.port) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("下载地址", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = kernelUrl, onValueChange = { kernelUrl = it },
                    label = { Text("内核 URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = openclashUrl, onValueChange = { openclashUrl = it },
                    label = { Text("OpenClash ipk URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = fanUrl, onValueChange = { fanUrl = it },
                    label = { Text("风扇控制 ipk URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(8.dp))
                Text("路由器连接", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = ip, onValueChange = { ip = it },
                    label = { Text("IP 地址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
                OutlinedTextField(
                    value = user, onValueChange = { user = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = port, onValueChange = { port = it },
                    label = { Text("端口") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    kernelUrl, openclashUrl, fanUrl,
                    ConnFields(ip, user, password, port),
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
