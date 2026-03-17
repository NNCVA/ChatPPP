package com.chatppp.app.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.domain.model.ConfigPreset
import com.chatppp.app.domain.model.ProviderType
import com.chatppp.app.ui.MainDispatcherRule
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun switching_provider_persists_provider_type() = runTest {
        val appPreferences = createAppPreferences()
        val viewModel = SettingsViewModel(
            appPreferences = appPreferences,
            secretStore = FakeSecretStore(),
            configPresetStore = FakeSettingsConfigPresetStore()
        )

        viewModel.updateProviderType(ProviderType.RELAY)
        advanceUntilIdle()

        assertEquals(ProviderType.RELAY, appPreferences.providerType.first())
        assertEquals(ProviderType.RELAY, viewModel.uiState.value.providerType)
    }

    @Test
    fun base_url_and_model_changes_persist() = runTest {
        val appPreferences = createAppPreferences()
        val viewModel = SettingsViewModel(
            appPreferences = appPreferences,
            secretStore = FakeSecretStore(),
            configPresetStore = FakeSettingsConfigPresetStore()
        )

        viewModel.updateBaseUrl("https://relay.example.com")
        viewModel.updateModel("deepseek-chat")
        advanceUntilIdle()

        assertEquals("https://relay.example.com", appPreferences.baseUrl.first())
        assertEquals("deepseek-chat", appPreferences.model.first())
        assertEquals("https://relay.example.com", viewModel.uiState.value.baseUrl)
        assertEquals("deepseek-chat", viewModel.uiState.value.model)
    }

    @Test
    fun saving_provider_credentials_updates_secret_store_and_ui_state() = runTest {
        val appPreferences = createAppPreferences()
        val secretStore = FakeSecretStore()
        val viewModel = SettingsViewModel(
            appPreferences = appPreferences,
            secretStore = secretStore,
            configPresetStore = FakeSettingsConfigPresetStore()
        )

        viewModel.saveDirectApiKey("direct-secret")
        viewModel.saveRelayToken("relay-secret")
        advanceUntilIdle()

        assertEquals("direct-secret", secretStore.getDirectApiKey())
        assertEquals("relay-secret", secretStore.getRelayToken())
        assertEquals("direct-secret", viewModel.uiState.value.directApiKey)
        assertEquals("relay-secret", viewModel.uiState.value.relayToken)
    }

    @Test
    fun summary_compression_settings_persist() = runTest {
        val appPreferences = createAppPreferences()
        val viewModel = SettingsViewModel(
            appPreferences = appPreferences,
            secretStore = FakeSecretStore(),
            configPresetStore = FakeSettingsConfigPresetStore()
        )

        viewModel.updateSummaryCompressionEnabled(false)
        advanceUntilIdle()

        assertEquals(false, appPreferences.summaryCompressionEnabled.first())
        assertEquals(false, viewModel.uiState.value.summaryCompressionEnabled)
    }

    @Test
    fun saving_current_settings_as_preset_updates_store_and_ui_state() = runTest {
        val appPreferences = createAppPreferences()
        appPreferences.updateProviderAndChatSettings(
            providerType = ProviderType.DIRECT,
            baseUrl = "https://api.openai.com/v1",
            model = "gpt-4.1-mini",
            streamEnabled = true
        )
        val secretStore = FakeSecretStore()
        secretStore.saveDirectApiKey("direct-secret")
        val presetStore = FakeSettingsConfigPresetStore()
        val viewModel = SettingsViewModel(
            appPreferences = appPreferences,
            secretStore = secretStore,
            configPresetStore = presetStore
        )

        viewModel.updatePresetDraftName("Work Direct")
        viewModel.saveCurrentConfigAsPreset()
        advanceUntilIdle()

        assertEquals(1, presetStore.observePresets().first().size)
        assertEquals("Work Direct", presetStore.observePresets().first().first().name)
        assertEquals("Work Direct", viewModel.uiState.value.savedPresets.first().name)
    }

    @Test
    fun activating_preset_loads_it_into_runtime_settings() = runTest {
        val appPreferences = createAppPreferences()
        val presetStore = FakeSettingsConfigPresetStore(
            listOf(
                ConfigPreset(
                    id = "preset-1",
                    name = "Preset 1",
                    providerType = ProviderType.DIRECT,
                    baseUrl = "https://api.openai.com/v1",
                    model = "gpt-4.1-mini",
                    streamEnabled = false,
                    directApiKey = "direct-secret"
                )
            )
        )
        val secretStore = FakeSecretStore()
        val viewModel = SettingsViewModel(
            appPreferences = appPreferences,
            secretStore = secretStore,
            configPresetStore = presetStore
        )

        viewModel.activatePreset("preset-1")
        advanceUntilIdle()

        assertEquals("preset-1", presetStore.activePresetId.first())
        assertEquals("https://api.openai.com/v1", viewModel.uiState.value.baseUrl)
        assertEquals("gpt-4.1-mini", viewModel.uiState.value.model)
        assertEquals(ProviderType.DIRECT, viewModel.uiState.value.providerType)
    }

    @Test
    fun renaming_existing_preset_updates_store_and_clears_edit_state() = runTest {
        val appPreferences = createAppPreferences()
        val presetStore = FakeSettingsConfigPresetStore(
            listOf(
                ConfigPreset(
                    id = "preset-1",
                    name = "Old name",
                    providerType = ProviderType.DIRECT,
                    baseUrl = "https://api.openai.com/v1",
                    model = "gpt-4.1-mini",
                    streamEnabled = true,
                    directApiKey = "direct-secret"
                )
            )
        )
        val viewModel = SettingsViewModel(
            appPreferences = appPreferences,
            secretStore = FakeSecretStore(),
            configPresetStore = presetStore
        )

        viewModel.startRenamingPreset("preset-1")
        advanceUntilIdle()
        viewModel.updatePresetDraftName("Renamed preset")
        viewModel.saveCurrentConfigAsPreset()
        advanceUntilIdle()

        assertEquals("Renamed preset", presetStore.observePresets().first().first().name)
        assertEquals("Renamed preset", viewModel.uiState.value.savedPresets.first().name)
        assertEquals("", viewModel.uiState.value.presetDraftName)
        assertEquals(null, viewModel.uiState.value.editingPresetId)
    }

    private fun createAppPreferences(): AppPreferences {
        val tempDirectory = Files.createTempDirectory("settings-viewmodel-test").toFile()
        val scope = CoroutineScope(SupervisorJob() + mainDispatcherRule.dispatcher)
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempDirectory.resolve("settings.preferences_pb") }
        )
        return AppPreferences(dataStore)
    }
}

private class FakeSecretStore : SecretStore {
    private var directApiKey: String? = null
    private var relayToken: String? = null

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
}

private class FakeSettingsConfigPresetStore(
    initialPresets: List<ConfigPreset> = emptyList()
) : ConfigPresetStore {
    private val presets = kotlinx.coroutines.flow.MutableStateFlow(initialPresets)
    override val activePresetId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun observePresets() = presets

    override suspend fun getPreset(presetId: String): ConfigPreset? =
        presets.value.firstOrNull { it.id == presetId }

    override suspend fun savePreset(preset: ConfigPreset) {
        presets.value = presets.value.filterNot { it.id == preset.id } + preset
    }

    override suspend fun deletePreset(presetId: String) {
        presets.value = presets.value.filterNot { it.id == presetId }
    }

    override suspend fun setActivePresetId(presetId: String?) {
        activePresetId.value = presetId
    }
}
