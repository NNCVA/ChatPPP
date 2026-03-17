package com.chatppp.app.ui.chat

import com.chatppp.app.ui.common.UiMessage

data class ChatUiState(
    val inputText: String = "",
    val messages: List<UiMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val availablePresets: List<ChatPresetUiModel> = emptyList(),
    val selectedPresetId: String? = null,
    val selectedPresetName: String? = null
)

data class ChatPresetUiModel(
    val id: String,
    val name: String
)
