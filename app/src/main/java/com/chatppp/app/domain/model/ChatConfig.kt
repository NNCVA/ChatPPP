package com.chatppp.app.domain.model

data class ChatConfig(
    val providerType: ProviderType,
    val baseUrl: String,
    val model: String,
    val streamEnabled: Boolean
)
