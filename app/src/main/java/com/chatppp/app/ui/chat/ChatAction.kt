package com.chatppp.app.ui.chat

sealed interface ChatAction {
    data class UpdateInput(val value: String) : ChatAction

    data object SendMessage : ChatAction

    data class SelectPreset(val presetId: String) : ChatAction

    data class RetryMessage(val failedMessageId: String) : ChatAction

    data class ToggleThinking(val messageId: String) : ChatAction

    data object StopGenerating : ChatAction
}
