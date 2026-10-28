package com.chenfa.openclashinstaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chenfa.openclashinstaller.ui.MainScreen
import com.chenfa.openclashinstaller.ui.MainViewModel
import com.chenfa.openclashinstaller.ui.MainViewModelFactory
import com.chenfa.openclashinstaller.ui.theme.OpenClashTheme

/** App 唯一 Activity，界面全部由 Miuix + Compose 承载。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModelFactory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            OpenClashTheme(
                themeMode = state.themeMode,
                monetEnabled = state.monetEnabled,
            ) {
                MainScreen(vm)
            }
        }
    }
}
