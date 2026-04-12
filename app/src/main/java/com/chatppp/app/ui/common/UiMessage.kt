package com.chatppp.app.ui.common

import com.chatppp.app.domain.model.Message
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import com.chatppp.app.ui.chat.RecoveryActionType

data class UiMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val thinkingContent: String? = null,
    val isThinkingExpanded: Boolean = false,
    val status: MessageStatus,
    val recoveryActionType: RecoveryActionType? = null
)

fun Message.toUiMessage(
    isThinkingExpanded: Boolean = false,
    recoveryActionType: RecoveryActionType? = null
): UiMessage = UiMessage(
    id = id,
    role = role,
    content = content,
    thinkingContent = thinkingContent,
    isThinkingExpanded = isThinkingExpanded,
    status = status,
    recoveryActionType = recoveryActionType
)
