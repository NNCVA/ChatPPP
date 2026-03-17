package com.chatppp.app.data.presets

import com.chatppp.app.data.local.preferences.ConfigPresetMetadata
import com.chatppp.app.data.local.preferences.ConfigPresetPreferencesStore
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.domain.model.ConfigPreset
import com.chatppp.app.domain.model.ProviderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfigPresetStoreTest {

    @Test
    fun complete_presets_round_trip_with_active_selection() = runTest {
        val secretStore = FakePresetSecretStore()
        val store = DefaultConfigPresetStore(
            appPreferences = FakeConfigPresetPreferencesStore(),
            secretStore = secretStore
        )

        store.savePreset(
            ConfigPreset(
                id = "direct-preset",
                name = "Direct preset",
                providerType = ProviderType.DIRECT,
                baseUrl = "https://api.openai.com/v1",
                model = "gpt-4.1-mini",
                streamEnabled = true,
                directApiKey = "direct-secret"
            )
        )
        store.savePreset(
            ConfigPreset(
                id = "relay-preset",
                name = "Relay preset",
                providerType = ProviderType.RELAY,
                baseUrl = "https://relay.example.com/v1",
                model = "deepseek-chat",
                streamEnabled = false,
                relayToken = "relay-secret"
            )
        )
        store.setActivePresetId("relay-preset")

        assertEquals(
            listOf("direct-preset", "relay-preset"),
            store.observePresets().first().map { it.id }.sorted()
        )
        assertEquals(
            "direct-secret",
            store.getPreset("direct-preset")?.directApiKey
        )
        assertEquals(
            "relay-secret",
            store.getPreset("relay-preset")?.relayToken
        )
        assertEquals("relay-preset", store.activePresetId.first())
    }

    @Test
    fun deleting_preset_removes_associated_secrets() = runTest {
        val secretStore = FakePresetSecretStore()
        val store = DefaultConfigPresetStore(
            appPreferences = FakeConfigPresetPreferencesStore(),
            secretStore = secretStore
        )

        store.savePreset(
            ConfigPreset(
                id = "preset-1",
                name = "Preset 1",
                providerType = ProviderType.DIRECT,
                baseUrl = "https://api.openai.com/v1",
                model = "gpt-4.1-mini",
                streamEnabled = true,
                directApiKey = "direct-secret"
            )
        )

        store.deletePreset("preset-1")

        assertEquals(emptyList<ConfigPreset>(), store.observePresets().first())
        assertNull(secretStore.getPresetDirectApiKey("preset-1"))
        assertNull(secretStore.getPresetRelayToken("preset-1"))
    }
}

private class FakePresetSecretStore : SecretStore {
    private var directApiKey: String? = null
    private var relayToken: String? = null
    private val presetDirectApiKeys = mutableMapOf<String, String>()
    private val presetRelayTokens = mutableMapOf<String, String>()

    override fun getDirectApiKey(): String? = directApiKey

    override fun saveDirectApiKey(value: String) {
        directApiKey = value
    }

    override fun clearDirectApiKey() {
        directApiKey = null
    }

    override fun getRelayToken(): String? = relayToken

    override fun saveRelayToken(value: String) {
        relayToken = value
    }

    override fun clearRelayToken() {
        relayToken = null
    }

    override fun getPresetDirectApiKey(presetId: String): String? = presetDirectApiKeys[presetId]

    override fun savePresetDirectApiKey(
        presetId: String,
        value: String
    ) {
        presetDirectApiKeys[presetId] = value
    }

    override fun clearPresetDirectApiKey(presetId: String) {
        presetDirectApiKeys.remove(presetId)
    }

    override fun getPresetRelayToken(presetId: String): String? = presetRelayTokens[presetId]

    override fun savePresetRelayToken(
        presetId: String,
        value: String
    ) {
        presetRelayTokens[presetId] = value
    }

    override fun clearPresetRelayToken(presetId: String) {
        presetRelayTokens.remove(presetId)
    }
}

private class FakeConfigPresetPreferencesStore : ConfigPresetPreferencesStore {
    private val metadata = MutableStateFlow<List<ConfigPresetMetadata>>(emptyList())
    private val activePreset = MutableStateFlow<String?>(null)

    override val configPresetMetadata = metadata

    override val activePresetId = activePreset

    override suspend fun updateConfigPresetMetadata(
        transform: (List<ConfigPresetMetadata>) -> List<ConfigPresetMetadata>
    ) {
        metadata.value = transform(metadata.value)
    }

    override suspend fun setActivePresetId(presetId: String?) {
        activePreset.value = presetId
    }
}
