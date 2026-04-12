package com.chatppp.app.ui.settings

import com.chatppp.app.domain.model.ProviderType

data class SettingsUiState(
    val providerType: ProviderType = ProviderType.DIRECT,
    val baseUrl: String = "",
    val model: String = "",
    val streamEnabled: Boolean = true,
    val summaryCompressionEnabled: Boolean = true,
    val presetDraftName: String = "",
    val editingPresetId: String? = null,
    val savedPresets: List<SettingsPresetUiModel> = emptyList(),
    val activePresetId: String? = null,
    val directApiKey: String = "",
    val relayToken: String = "",
    val directApiKeyVisible: Boolean = false,
    val relayTokenVisible: Boolean = false,
    val baseUrlError: String? = null,
    val modelError: String? = null,
    val credentialError: String? = null,
    val readinessLabel: String = "Not ready",
    val connectionStatusLabel: String? = null,
    val providerTemplates: List<ProviderTemplateUiModel> = defaultProviderTemplates()
)

data class SettingsPresetUiModel(
    val id: String,
    val name: String,
    val providerType: ProviderType,
    val model: String,
    val isActive: Boolean
)

data class ProviderTemplateUiModel(
    val id: String,
    val title: String,
    val description: String,
    val providerType: ProviderType,
    val baseUrl: String,
    val model: String
)

internal fun defaultProviderTemplates(): List<ProviderTemplateUiModel> = listOf(
    ProviderTemplateUiModel(
        id = "openai-official",
        title = "OpenAI official",
        description = "Use the default OpenAI endpoint with a compact starter model.",
        providerType = ProviderType.DIRECT,
        baseUrl = "https://api.openai.com/v1",
        model = "gpt-4.1-mini"
    ),
    ProviderTemplateUiModel(
        id = "self-hosted-relay",
        title = "Self-hosted relay",
        description = "Point the app at your own backend relay and keep provider keys off-device.",
        providerType = ProviderType.RELAY,
        baseUrl = "https://your-relay.example.com/v1",
        model = "relay-default"
    )
)
