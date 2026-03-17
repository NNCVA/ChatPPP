package com.chatppp.app.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatppp.app.di.AppEntryPoint
import com.chatppp.app.ui.common.rememberViewModelFactory
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ChatRoute(
    onOpenConversations: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val applicationContext = LocalContext.current.applicationContext
    val entryPoint = remember(applicationContext) {
        EntryPointAccessors.fromApplication(applicationContext, AppEntryPoint::class.java)
    }
    val viewModel: ChatViewModel = viewModel(
        factory = rememberViewModelFactory {
            ChatViewModel(
                repository = entryPoint.chatRepository(),
                lastConversationStore = entryPoint.lastConversationStore(),
                configPresetStore = entryPoint.configPresetStore()
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    ChatScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenConversations = onOpenConversations,
        onOpenSettings = onOpenSettings
    )
}
