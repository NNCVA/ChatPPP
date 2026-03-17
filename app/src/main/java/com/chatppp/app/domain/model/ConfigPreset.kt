package com.chatppp.app.domain.model

data class ConfigPreset(
    val id: String,
    val name: String,
    val providerType: ProviderType,
    val baseUrl: String,
    val model: String,
    val streamEnabled: Boolean,
    val directApiKey: String? = null,
    val relayToken: String? = null
)
