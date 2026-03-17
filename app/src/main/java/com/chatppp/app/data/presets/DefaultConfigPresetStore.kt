package com.chatppp.app.data.presets

import com.chatppp.app.data.local.preferences.ConfigPresetPreferencesStore
import com.chatppp.app.data.local.preferences.ConfigPresetMetadata
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.domain.model.ConfigPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DefaultConfigPresetStore(
    private val appPreferences: ConfigPresetPreferencesStore,
    private val secretStore: SecretStore
) : ConfigPresetStore {
    override fun observePresets(): Flow<List<ConfigPreset>> =
        appPreferences.configPresetMetadata.map { metadata ->
            metadata.map { item -> item.toDomain(secretStore) }
        }

    override val activePresetId: Flow<String?> = appPreferences.activePresetId

    override suspend fun getPreset(presetId: String): ConfigPreset? =
        appPreferences.configPresetMetadata.first()
            .firstOrNull { it.id == presetId }
            ?.toDomain(secretStore)

    override suspend fun savePreset(preset: ConfigPreset) {
        appPreferences.updateConfigPresetMetadata { metadata ->
            metadata.filterNot { it.id == preset.id } + preset.toMetadata()
        }

        preset.directApiKey
            ?.takeIf { it.isNotBlank() }
            ?.let { secretStore.savePresetDirectApiKey(preset.id, it) }
            ?: secretStore.clearPresetDirectApiKey(preset.id)

        preset.relayToken
            ?.takeIf { it.isNotBlank() }
            ?.let { secretStore.savePresetRelayToken(preset.id, it) }
            ?: secretStore.clearPresetRelayToken(preset.id)
    }

    override suspend fun deletePreset(presetId: String) {
        appPreferences.updateConfigPresetMetadata { metadata ->
            metadata.filterNot { it.id == presetId }
        }
        secretStore.clearPresetDirectApiKey(presetId)
        secretStore.clearPresetRelayToken(presetId)
        if (appPreferences.activePresetId.first() == presetId) {
            appPreferences.setActivePresetId(null)
        }
    }

    override suspend fun setActivePresetId(presetId: String?) {
        appPreferences.setActivePresetId(presetId)
    }

    private fun ConfigPresetMetadata.toDomain(secretStore: SecretStore): ConfigPreset =
        ConfigPreset(
            id = id,
            name = name,
            providerType = providerType,
            baseUrl = baseUrl,
            model = model,
            streamEnabled = streamEnabled,
            directApiKey = secretStore.getPresetDirectApiKey(id),
            relayToken = secretStore.getPresetRelayToken(id)
        )

    private fun ConfigPreset.toMetadata(): ConfigPresetMetadata =
        ConfigPresetMetadata(
            id = id,
            name = name,
            providerType = providerType,
            baseUrl = baseUrl,
            model = model,
            streamEnabled = streamEnabled
        )
}
