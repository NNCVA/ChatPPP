package com.chatppp.app.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chatppp.app.domain.model.ProviderType
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppPreferences(
    private val dataStore: DataStore<Preferences>
) : ConfigPresetPreferencesStore {
    private val json = Json { ignoreUnknownKeys = true }

    val providerType: Flow<ProviderType> = dataStore.data.map { preferences ->
        preferences[Keys.PROVIDER_TYPE]
            ?.let(ProviderType::valueOf)
            ?: ProviderType.DIRECT
    }

    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.BASE_URL].orEmpty()
    }

    val model: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.MODEL].orEmpty()
    }

    val lastConversationId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.LAST_CONVERSATION_ID]
    }

    val streamEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.STREAM_ENABLED] ?: true
    }

    val summaryCompressionEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.SUMMARY_COMPRESSION_ENABLED] ?: true
    }

    override val configPresetMetadata: Flow<List<ConfigPresetMetadata>> = dataStore.data.map { preferences ->
        preferences[Keys.CONFIG_PRESET_METADATA]
            ?.takeIf { it.isNotBlank() }
            ?.let { encoded ->
                runCatching {
                    json.decodeFromString<List<ConfigPresetMetadata>>(encoded)
                }.getOrDefault(emptyList())
            }
            ?: emptyList()
    }

    override val activePresetId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.ACTIVE_PRESET_ID]
    }

    suspend fun setProviderType(providerType: ProviderType) {
        dataStore.edit { preferences ->
            preferences[Keys.PROVIDER_TYPE] = providerType.name
        }
    }

    suspend fun setBaseUrl(baseUrl: String) {
        dataStore.edit { preferences ->
            preferences[Keys.BASE_URL] = baseUrl
        }
    }

    suspend fun setModel(model: String) {
        dataStore.edit { preferences ->
            preferences[Keys.MODEL] = model
        }
    }

    suspend fun setLastConversationId(conversationId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_CONVERSATION_ID] = conversationId
        }
    }

    suspend fun clearLastConversationId() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.LAST_CONVERSATION_ID)
        }
    }

    suspend fun setStreamEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.STREAM_ENABLED] = enabled
        }
    }

    suspend fun setSummaryCompressionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SUMMARY_COMPRESSION_ENABLED] = enabled
        }
    }

    suspend fun setConfigPresetMetadata(metadata: List<ConfigPresetMetadata>) {
        editConfigPresetMetadata { preferences ->
            preferences[Keys.CONFIG_PRESET_METADATA] = json.encodeToString(metadata)
        }
    }

    override suspend fun updateConfigPresetMetadata(
        transform: (List<ConfigPresetMetadata>) -> List<ConfigPresetMetadata>
    ) {
        editConfigPresetMetadata { preferences ->
            val currentMetadata = preferences[Keys.CONFIG_PRESET_METADATA]
                ?.takeIf { it.isNotBlank() }
                ?.let { encoded ->
                    runCatching {
                        json.decodeFromString<List<ConfigPresetMetadata>>(encoded)
                    }.getOrDefault(emptyList())
                }
                ?: emptyList()
            preferences[Keys.CONFIG_PRESET_METADATA] = json.encodeToString(transform(currentMetadata))
        }
    }

    override suspend fun setActivePresetId(presetId: String?) {
        dataStore.edit { preferences ->
            if (presetId.isNullOrBlank()) {
                preferences.remove(Keys.ACTIVE_PRESET_ID)
            } else {
                preferences[Keys.ACTIVE_PRESET_ID] = presetId
            }
        }
    }

    suspend fun updateSummaryCompressionSettings(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SUMMARY_COMPRESSION_ENABLED] = enabled
        }
    }

    suspend fun updateChatSettings(
        baseUrl: String,
        model: String,
        streamEnabled: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.BASE_URL] = baseUrl
            preferences[Keys.MODEL] = model
            preferences[Keys.STREAM_ENABLED] = streamEnabled
        }
    }

    suspend fun updateProviderAndChatSettings(
        providerType: ProviderType,
        baseUrl: String,
        model: String,
        streamEnabled: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.PROVIDER_TYPE] = providerType.name
            preferences[Keys.BASE_URL] = baseUrl
            preferences[Keys.MODEL] = model
            preferences[Keys.STREAM_ENABLED] = streamEnabled
        }
    }

    suspend fun updateRuntimeSettings(
        baseUrl: String,
        model: String,
        streamEnabled: Boolean,
        summaryCompressionEnabled: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.BASE_URL] = baseUrl
            preferences[Keys.MODEL] = model
            preferences[Keys.STREAM_ENABLED] = streamEnabled
            preferences[Keys.SUMMARY_COMPRESSION_ENABLED] = summaryCompressionEnabled
        }
    }

    private object Keys {
        val PROVIDER_TYPE = stringPreferencesKey("provider_type")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val LAST_CONVERSATION_ID = stringPreferencesKey("last_conversation_id")
        val STREAM_ENABLED = booleanPreferencesKey("stream_enabled")
        val SUMMARY_COMPRESSION_ENABLED = booleanPreferencesKey("summary_compression_enabled")
        val CONFIG_PRESET_METADATA = stringPreferencesKey("config_preset_metadata")
        val ACTIVE_PRESET_ID = stringPreferencesKey("active_preset_id")
    }

    private companion object {
        const val CONFIG_PRESET_WRITE_RETRIES = 3
        const val CONFIG_PRESET_WRITE_RETRY_DELAY_MS = 25L
    }

    private suspend fun editConfigPresetMetadata(
        block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ) {
        repeat(CONFIG_PRESET_WRITE_RETRIES) { attempt ->
            try {
                dataStore.edit(block)
                return
            } catch (ioException: IOException) {
                if (attempt == CONFIG_PRESET_WRITE_RETRIES - 1) {
                    throw ioException
                }
                delay(CONFIG_PRESET_WRITE_RETRY_DELAY_MS)
            }
        }
    }
}

@Serializable
data class ConfigPresetMetadata(
    val id: String,
    val name: String,
    val providerType: ProviderType,
    val baseUrl: String,
    val model: String,
    val streamEnabled: Boolean
)
