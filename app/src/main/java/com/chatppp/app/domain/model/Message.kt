package com.chatppp.app.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val thinkingContent: String? = null,
    val status: MessageStatus,
    val createdAt: Long
)
