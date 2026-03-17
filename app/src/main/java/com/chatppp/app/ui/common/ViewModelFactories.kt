package com.chatppp.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Composable
inline fun <reified VM : ViewModel> rememberViewModelFactory(
    crossinline create: @DisallowComposableCalls () -> VM
): ViewModelProvider.Factory = remember {
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
}
