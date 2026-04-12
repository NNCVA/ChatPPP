package com.chatppp.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.data.remote.provider.ProviderSelector
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: ChatRepository,
    private val lastConversationStore: LastConversationStore,
    private val configPresetStore: ConfigPresetStore,
    private val providerSelector: ProviderSelector,
    private val appPreferences: AppPreferences,
    private val secretStore: SecretStore
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
            combine(
                appPreferences.providerType,
                appPreferences.baseUrl
            ) { providerType, _ ->
                providerType
            }.collect { providerType ->
                refreshSetupState(providerTypeOverride = providerType)
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
                    val recoveryState = deriveRecoveryState(messages)
                    val requestPhaseLabel = deriveRequestPhaseLabel(messages, isStreaming)
                    _uiState.update { state ->
                        state.copy(
                            messages = messages.map { message ->
                                toUiMessage(
                                    message = message,
                                    recoveryActionType = recoveryActionFor(message)
                                )
                            },
                            isStreaming = isStreaming,
                            recoveryActionLabel = recoveryState.first,
                            recoveryActionType = recoveryState.second,
                            requestPhaseLabel = requestPhaseLabel
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
            is ChatAction.CopyMessage -> copyMessage(action.messageId)
            is ChatAction.EditMessage -> editMessage(action.messageId)
            ChatAction.DismissRecoveryBanner -> dismissRecoveryBanner()
            ChatAction.CopyHandled -> _uiState.update { it.copy(copiedMessageContent = null) }
        }
    }

    private fun sendCurrentInput() {
        val rawInput = _uiState.value.inputText
        val trimmedInput = rawInput.trim()
        if (trimmedInput.isEmpty()) {
            return
        }

        streamingDismissed = false
        _uiState.update { it.copy(inputText = "", requestPhaseLabel = "Connecting") }
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
            state.copy(
                messages = latestMessages.map { message ->
                    toUiMessage(
                        message = message,
                        recoveryActionType = recoveryActionFor(message)
                    )
                }
            )
        }
    }

    private fun deriveRecoveryState(messages: List<Message>): Pair<String?, RecoveryActionType?> {
        val errorMessage = messages.lastOrNull { it.status == MessageStatus.ERROR }
        if (errorMessage == null) {
            return null to null
        }
        val content = errorMessage.content
        return when {
            content.startsWith("Config:") ||
            content.startsWith("Authentication failed") ||
            content.contains("API key", ignoreCase = true) ||
            content.contains("relay token", ignoreCase = true) -> "Open settings" to RecoveryActionType.OPEN_SETTINGS
            else -> "Retry" to RecoveryActionType.RETRY
        }
    }

    fun dismissRecoveryBanner() {
        _uiState.update { it.copy(recoveryActionLabel = null, recoveryActionType = null) }
    }

    fun refreshSetupState() {
        refreshSetupState(providerTypeOverride = null)
    }

    private fun copyMessage(messageId: String) {
        val message = latestMessages.firstOrNull { it.id == messageId }
        if (message != null) {
            _uiState.update { it.copy(copiedMessageContent = message.content) }
        }
    }

    private fun editMessage(messageId: String) {
        val message = latestMessages.firstOrNull { it.id == messageId }
        if (message != null) {
            _uiState.update { it.copy(inputText = message.content) }
        }
    }

    private fun deriveRequestPhaseLabel(messages: List<Message>, isStreaming: Boolean): String? {
        if (!isStreaming) {
            return null
        }
        val streamingMessage = messages.lastOrNull { it.status == MessageStatus.STREAMING }
        return if (streamingMessage != null && streamingMessage.content.isNotEmpty()) {
            "Streaming"
        } else {
            "Connecting"
        }
    }

    private fun toUiMessage(
        message: Message,
        recoveryActionType: RecoveryActionType? = null
    ) = message.toUiMessage(
        isThinkingExpanded = message.id in expandedThinkingMessageIds,
        recoveryActionType = recoveryActionType
    )

    private fun recoveryActionFor(message: Message): RecoveryActionType? {
        if (message.status != MessageStatus.ERROR) {
            return null
        }
        val content = message.content
        return when {
            content.startsWith("Config:") ||
                content.startsWith("Authentication failed") ||
                content.contains("API key", ignoreCase = true) ||
                content.contains("relay token", ignoreCase = true) ->
                RecoveryActionType.OPEN_SETTINGS

            else -> RecoveryActionType.RETRY
        }
    }

    private fun refreshSetupState(providerTypeOverride: com.chatppp.app.domain.model.ProviderType?) {
        viewModelScope.launch {
            val providerType = providerTypeOverride ?: appPreferences.providerType.first()
            val validation = providerSelector.validate(
                providerType,
                appPreferences.baseUrl.first(),
                secretStore.getDirectApiKey(),
                secretStore.getRelayToken()
            )
            _uiState.update { state ->
                state.copy(
                    requiresSetup = validation.readinessLabel != "Ready",
                    readinessLabel = validation.readinessLabel
                )
            }
        }
    }

    private data class ChatPresetSnapshot(
        val availablePresets: List<ChatPresetUiModel>,
        val selectedPresetId: String?,
        val selectedPresetName: String?
    )
}
