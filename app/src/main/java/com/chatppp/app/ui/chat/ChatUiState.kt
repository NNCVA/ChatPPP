package com.chatppp.app.ui.chat

import com.chatppp.app.ui.common.UiMessage

data class ChatUiState(
    val inputText: String = "",
    val messages: List<UiMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val availablePresets: List<ChatPresetUiModel> = emptyList(),
    val selectedPresetId: String? = null,
    val selectedPresetName: String? = null,
    val requiresSetup: Boolean = true,
    val readinessLabel: String = "Not ready",
    val recoveryActionLabel: String? = null,
    val recoveryActionType: RecoveryActionType? = null,
    val requestPhaseLabel: String? = null,
    val compressionNotice: String? = null,
    val copiedMessageContent: String? = null
)

enum class RecoveryActionType {
    RETRY,
    OPEN_SETTINGS,
    NONE
}

data class ChatPresetUiModel(
    val id: String,
    val name: String
)
