package com.chenfa.openclashinstaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chenfa.openclashinstaller.ui.MainScreen
import com.chenfa.openclashinstaller.ui.theme.OpenClashTheme

/**
 * App 唯一 Activity。阶段 A 仅承载空 MainScreen。
 * 后续阶段接入 ViewModel 后由 MainScreen 内部用 viewModel() 取 ViewModel。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OpenClashTheme {
                MainScreen()
            }
        }
    }
}
