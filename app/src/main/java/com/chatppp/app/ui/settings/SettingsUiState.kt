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
    val relayTokenVisible: Boolean = false
)

data class SettingsPresetUiModel(
    val id: String,
    val name: String,
    val providerType: ProviderType,
    val model: String,
    val isActive: Boolean
)
