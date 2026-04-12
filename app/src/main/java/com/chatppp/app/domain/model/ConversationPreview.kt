package com.chatppp.app.domain.model

data class ConversationPreview(
    val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val relativeUpdatedAt: String,
    val providerType: ProviderType
)