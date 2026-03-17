package com.chatppp.app.ui.common

import com.chatppp.app.domain.model.Message
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus

data class UiMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val thinkingContent: String? = null,
    val isThinkingExpanded: Boolean = false,
    val status: MessageStatus
)

fun Message.toUiMessage(
    isThinkingExpanded: Boolean = false
): UiMessage = UiMessage(
    id = id,
    role = role,
    content = content,
    thinkingContent = thinkingContent,
    isThinkingExpanded = isThinkingExpanded,
    status = status
)
