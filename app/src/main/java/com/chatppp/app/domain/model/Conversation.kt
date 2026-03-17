package com.chatppp.app.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val providerType: ProviderType,
    val presetId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
