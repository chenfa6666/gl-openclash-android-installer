package com.chenfa.openclashinstaller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chenfa.openclashinstaller.data.model.ConnFields
import com.chenfa.openclashinstaller.ui.components.CollapsibleCard
import com.chenfa.openclashinstaller.ui.components.PrimaryButton
import com.chenfa.openclashinstaller.ui.theme.LocalGlassTokens

/**
 * 「设置」页：3 个下载 URL + 4 个连接字段（IP/用户/密码/端口）。
 * 字段编辑状态、保存回调与旧设置对话框完全一致；保存成功后回到安装页。
 */
@Composable
fun SettingsPage(
    vm: MainViewModel,
    contentPadding: PaddingValues,
    onSaved: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    // 每次进入页面（切走即 dispose）从当前 VM 状态初始化一份本地编辑副本
    var kernelUrl by remember { mutableStateOf(state.kernelUrl) }
    var openclashUrl by remember { mutableStateOf(state.openclashUrl) }
    var fanUrl by remember { mutableStateOf(state.fanUrl) }
    var ip by remember { mutableStateOf(state.fields.ip) }
    var user by remember { mutableStateOf(state.fields.user) }
    var password by remember { mutableStateOf(state.fields.password) }
    var port by remember { mutableStateOf(state.fields.port) }

    val tokens = LocalGlassTokens.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = tokens.fieldBorder.copy(alpha = 0.55f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = tokens.onGlassEmphasis,
        focusedTextColor = tokens.onGlass,
        unfocusedTextColor = tokens.onGlass,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CollapsibleCard(title = "下载地址", defaultExpanded = true) {
            OutlinedTextField(
                value = kernelUrl, onValueChange = { kernelUrl = it },
                label = { Text("内核 URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
            )
            OutlinedTextField(
                value = openclashUrl, onValueChange = { openclashUrl = it },
                label = { Text("OpenClash ipk URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
            )
            OutlinedTextField(
                value = fanUrl, onValueChange = { fanUrl = it },
                label = { Text("风扇控制 ipk URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
            )
        }

        CollapsibleCard(title = "路由器连接", defaultExpanded = true) {
            OutlinedTextField(
                value = ip, onValueChange = { ip = it },
                label = { Text("IP 地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = user, onValueChange = { user = it },
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
            )
            OutlinedTextField(
                value = port, onValueChange = { port = it },
                label = { Text("端口") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        PrimaryButton(
            text = "保存",
            onClick = {
                vm.saveSettings(
                    kernelUrl,
                    openclashUrl,
                    fanUrl,
                    ConnFields(ip, user, password, port),
                )
                onSaved()
            },
        )
        Spacer(Modifier.height(20.dp))
    }
}
