package com.chatppp.app.ui.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatppp.app.di.AppEntryPoint
import com.chatppp.app.ui.common.rememberViewModelFactory
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ConversationListRoute(
    onBack: () -> Unit,
    onConversationClick: (String) -> Unit
) {
    val applicationContext = LocalContext.current.applicationContext
    val entryPoint = remember(applicationContext) {
        EntryPointAccessors.fromApplication(applicationContext, AppEntryPoint::class.java)
    }
    val viewModel: ConversationListViewModel = viewModel(
        factory = rememberViewModelFactory {
            ConversationListViewModel(
                repository = entryPoint.chatRepository(),
                lastConversationStore = entryPoint.lastConversationStore()
            )
        }
    )
    val state by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ConversationListEffect.OpenConversation -> {
                    onConversationClick(effect.conversationId)
                }
                is ConversationListEffect.ShowUndoDelete -> {
                    // Handled by the screen via state
                }
            }
        }
    }

    ConversationListScreen(
        state = state,
        effects = viewModel.effects,
        onBack = onBack,
        onCreateConversation = viewModel::createConversation,
        onDeleteConversation = viewModel::deleteConversation,
        onConversationClick = { conversationId ->
            viewModel.selectConversation(conversationId)
            onConversationClick(conversationId)
        },
        onUndoDelete = viewModel::undoDelete,
        onDismissUndo = viewModel::dismissUndo
    )
}