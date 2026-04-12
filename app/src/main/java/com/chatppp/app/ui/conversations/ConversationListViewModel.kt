package com.chatppp.app.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatppp.app.domain.model.Conversation
import com.chatppp.app.domain.model.ConversationPreview
import com.chatppp.app.domain.model.ProviderType
import com.chatppp.app.domain.repository.ChatRepository
import com.chatppp.app.domain.session.LastConversationStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val repository: ChatRepository,
    private val lastConversationStore: LastConversationStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ConversationListEffect>()
    val effects: SharedFlow<ConversationListEffect> = _effects.asSharedFlow()
    private var lastDeletedConversationWasSelected = false

    init {
        viewModelScope.launch {
            repository.observeConversations().collect { conversations ->
                val previews = conversations
                    .sortedByDescending { it.updatedAt }
                    .map { conversation ->
                        val lastMessage = repository.getLastMessage(conversation.id)
                        ConversationPreview(
                            id = conversation.id,
                            title = conversation.title,
                            lastMessagePreview = lastMessage?.content?.take(50),
                            relativeUpdatedAt = formatRelativeTime(conversation.updatedAt),
                            providerType = conversation.providerType
                        )
                    }
                _uiState.update {
                    it.copy(conversations = previews)
                }
            }
        }
    }

    fun createConversation() {
        viewModelScope.launch {
            val conversationId = repository.createConversation()
            lastConversationStore.setLastConversationId(conversationId)
            _effects.emit(ConversationListEffect.OpenConversation(conversationId))
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val conversations = repository.observeConversations().first()
            val conversation = conversations.find { it.id == conversationId }
            if (conversation != null) {
                val wasSelected = lastConversationStore.lastConversationId.first() == conversationId
                val deletedMessages = repository.observeMessages(conversationId).first()
                repository.deleteConversation(conversationId)
                lastDeletedConversationWasSelected = wasSelected
                _uiState.update {
                    it.copy(
                        lastDeletedConversation = conversation,
                        lastDeletedMessages = deletedMessages
                    )
                }
                if (wasSelected) {
                    lastConversationStore.clearLastConversationId()
                }
                _effects.emit(ConversationListEffect.ShowUndoDelete(conversationId, conversation.title))
            }
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            val deletedConversation = _uiState.value.lastDeletedConversation
            val deletedMessages = _uiState.value.lastDeletedMessages
            if (deletedConversation != null) {
                repository.restoreConversation(deletedConversation, deletedMessages)
                if (lastDeletedConversationWasSelected) {
                    lastConversationStore.setLastConversationId(deletedConversation.id)
                }
                lastDeletedConversationWasSelected = false
                _uiState.update {
                    it.copy(
                        lastDeletedConversation = null,
                        lastDeletedMessages = emptyList()
                    )
                }
            }
        }
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameConversation(conversationId, newTitle)
        }
    }

    fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            lastConversationStore.setLastConversationId(conversationId)
        }
    }

    fun dismissUndo() {
        lastDeletedConversationWasSelected = false
        _uiState.update {
            it.copy(
                lastDeletedConversation = null,
                lastDeletedMessages = emptyList()
            )
        }
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hours ago"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }
}
