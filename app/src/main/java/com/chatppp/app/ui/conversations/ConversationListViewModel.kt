package com.chatppp.app.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatppp.app.domain.repository.ChatRepository
import com.chatppp.app.domain.session.LastConversationStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val repository: ChatRepository,
    private val lastConversationStore: LastConversationStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeConversations().collect { conversations ->
                _uiState.update {
                    it.copy(
                        conversations = conversations.sortedByDescending { conversation ->
                            conversation.updatedAt
                        }
                    )
                }
            }
        }
    }

    fun createConversation(title: String = "New Chat") {
        viewModelScope.launch {
            repository.createConversation(title)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (lastConversationStore.lastConversationId.first() == conversationId) {
                lastConversationStore.clearLastConversationId()
            }
        }
    }

    fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            lastConversationStore.setLastConversationId(conversationId)
        }
    }
}
