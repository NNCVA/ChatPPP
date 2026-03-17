package com.chatppp.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.domain.model.Message
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import com.chatppp.app.domain.repository.ChatRepository
import com.chatppp.app.domain.session.LastConversationStore
import com.chatppp.app.ui.common.toUiMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: ChatRepository,
    private val lastConversationStore: LastConversationStore,
    private val configPresetStore: ConfigPresetStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val currentConversationId = MutableStateFlow<String?>(null)
    private var latestMessages: List<Message> = emptyList()
    private var expandedThinkingMessageIds = emptySet<String>()
    private var streamingDismissed = false

    init {
        viewModelScope.launch {
            combine(
                lastConversationStore.lastConversationId,
                repository.observeConversations()
            ) { storedConversationId, conversations ->
                val validConversationId = storedConversationId?.takeIf { storedId ->
                    conversations.any { it.id == storedId }
                }
                storedConversationId to validConversationId
            }
                .distinctUntilChanged()
                .collect { (storedConversationId, validConversationId) ->
                    if (storedConversationId != null && validConversationId == null) {
                        lastConversationStore.clearLastConversationId()
                    }
                    currentConversationId.value = validConversationId
                }
        }

        viewModelScope.launch {
            combine(
                currentConversationId,
                repository.observeConversations(),
                configPresetStore.observePresets(),
                configPresetStore.activePresetId
            ) { conversationId, conversations, presets, activePresetId ->
                val currentConversation = conversations.firstOrNull { it.id == conversationId }
                val selectedPresetId = currentConversation?.presetId ?: activePresetId
                val selectedPresetName = presets.firstOrNull { it.id == selectedPresetId }?.name
                ChatPresetSnapshot(
                    availablePresets = presets.map { ChatPresetUiModel(id = it.id, name = it.name) },
                    selectedPresetId = selectedPresetId,
                    selectedPresetName = selectedPresetName
                )
            }.collect { snapshot ->
                _uiState.update { state ->
                    state.copy(
                        availablePresets = snapshot.availablePresets,
                        selectedPresetId = snapshot.selectedPresetId,
                        selectedPresetName = snapshot.selectedPresetName
                    )
                }
            }
        }

        viewModelScope.launch {
            currentConversationId
                .flatMapLatest { conversationId ->
                    observeMessages(conversationId)
                }
                .collect { messages ->
                    latestMessages = messages
                    val isStreaming = messages.any { it.status == MessageStatus.STREAMING } && !streamingDismissed
                    _uiState.update { state ->
                        state.copy(
                            messages = messages.map(::toUiMessage),
                            isStreaming = isStreaming
                        )
                    }
                    if (messages.none { it.status == MessageStatus.STREAMING }) {
                        streamingDismissed = false
                    }
                }
        }
    }

    private fun observeMessages(conversationId: String?): Flow<List<Message>> {
        return if (conversationId == null) {
            flowOf(emptyList())
        } else {
            repository.observeMessages(conversationId)
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.UpdateInput -> {
                _uiState.update { it.copy(inputText = action.value) }
            }

            ChatAction.SendMessage -> sendCurrentInput()
            is ChatAction.SelectPreset -> selectPreset(action.presetId)
            is ChatAction.RetryMessage -> retryMessage(action.failedMessageId)
            is ChatAction.ToggleThinking -> toggleThinking(action.messageId)
            ChatAction.StopGenerating -> stopGenerating()
        }
    }

    private fun sendCurrentInput() {
        val rawInput = _uiState.value.inputText
        val trimmedInput = rawInput.trim()
        if (trimmedInput.isEmpty()) {
            return
        }

        streamingDismissed = false
        _uiState.update { it.copy(inputText = "") }
        viewModelScope.launch {
            val conversationId = currentConversationId.value ?: repository.createConversation().also { createdId ->
                currentConversationId.value = createdId
                lastConversationStore.setLastConversationId(createdId)
            }
            repository.sendMessage(conversationId, trimmedInput)
        }
    }

    private fun retryMessage(failedMessageId: String) {
        val failedIndex = latestMessages.indexOfFirst { it.id == failedMessageId }
        if (failedIndex == -1) {
            return
        }

        val userMessage = latestMessages
            .take(failedIndex)
            .lastOrNull { it.role == MessageRole.USER }
            ?: return

        streamingDismissed = false
        viewModelScope.launch {
            val conversationId = currentConversationId.value ?: return@launch
            repository.sendMessage(conversationId, userMessage.content)
        }
    }

    private fun selectPreset(presetId: String) {
        viewModelScope.launch {
            val conversationId = currentConversationId.value
            if (conversationId == null) {
                configPresetStore.setActivePresetId(presetId)
            } else {
                repository.bindPresetToConversation(conversationId, presetId)
            }
        }
    }

    private fun stopGenerating() {
        streamingDismissed = true
        _uiState.update { it.copy(isStreaming = false) }
        viewModelScope.launch {
            val conversationId = currentConversationId.value ?: return@launch
            repository.stopStreaming(conversationId)
        }
    }

    private fun toggleThinking(messageId: String) {
        expandedThinkingMessageIds = expandedThinkingMessageIds.toMutableSet().apply {
            if (!add(messageId)) {
                remove(messageId)
            }
        }
        _uiState.update { state ->
            state.copy(messages = latestMessages.map(::toUiMessage))
        }
    }

    private fun toUiMessage(message: Message) = message.toUiMessage(
        isThinkingExpanded = message.id in expandedThinkingMessageIds
    )

    private data class ChatPresetSnapshot(
        val availablePresets: List<ChatPresetUiModel>,
        val selectedPresetId: String?,
        val selectedPresetName: String?
    )
}
