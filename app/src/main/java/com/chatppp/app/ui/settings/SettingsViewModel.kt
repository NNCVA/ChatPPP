package com.chatppp.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.data.remote.provider.ConnectionTestService
import com.chatppp.app.data.remote.provider.ConnectionTestResult
import com.chatppp.app.data.remote.provider.ProviderSelector
import com.chatppp.app.domain.model.ConfigPreset
import com.chatppp.app.domain.model.ProviderType
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val secretStore: SecretStore,
    private val configPresetStore: ConfigPresetStore,
    private val providerSelector: ProviderSelector,
    private val connectionTestService: ConnectionTestService
) : ViewModel() {
    private val providerTemplates = defaultProviderTemplates()
    private val preferencesWriteMutex = Mutex()
    private var settingsPersistJob: Job? = null
    private var connectionStatusVersion = 0L

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            directApiKey = secretStore.getDirectApiKey().orEmpty(),
            relayToken = secretStore.getRelayToken().orEmpty(),
            providerTemplates = providerTemplates
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appPreferences.providerType,
                appPreferences.baseUrl,
                appPreferences.model,
                appPreferences.streamEnabled,
                appPreferences.summaryCompressionEnabled,
                configPresetStore.observePresets(),
                configPresetStore.activePresetId
            ) { values ->
                val providerType = values[0] as ProviderType
                val baseUrl = values[1] as String
                val model = values[2] as String
                val streamEnabled = values[3] as Boolean
                val summaryCompressionEnabled = values[4] as Boolean
                val presets = values[5] as List<ConfigPreset>
                val activePresetId = values[6] as String?
                val directApiKey = secretStore.getDirectApiKey().orEmpty()
                val relayToken = secretStore.getRelayToken().orEmpty()
                val validation = providerSelector.validate(
                    providerType,
                    baseUrl,
                    directApiKey,
                    relayToken
                )
                SettingsUiState(
                    providerType = providerType,
                    baseUrl = baseUrl,
                    model = model,
                    streamEnabled = streamEnabled,
                    summaryCompressionEnabled = summaryCompressionEnabled,
                    savedPresets = presets.map { preset ->
                        SettingsPresetUiModel(
                            id = preset.id,
                            name = preset.name,
                            providerType = preset.providerType,
                            model = preset.model,
                            isActive = preset.id == activePresetId
                        )
                    },
                    activePresetId = activePresetId,
                    directApiKey = directApiKey,
                    relayToken = relayToken,
                    baseUrlError = validation.baseUrlError,
                    modelError = validation.modelError,
                    credentialError = validation.credentialError,
                    readinessLabel = validation.readinessLabel,
                    providerTemplates = providerTemplates
                )
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(
                        presetDraftName = current.presetDraftName,
                        editingPresetId = current.editingPresetId,
                        directApiKeyVisible = current.directApiKeyVisible,
                        relayTokenVisible = current.relayTokenVisible,
                        connectionStatusLabel = current.connectionStatusLabel
                    )
                }
            }
        }
    }

    fun updateProviderType(providerType: ProviderType) {
        viewModelScope.launch {
            invalidateConnectionStatus()
            preferencesWriteMutex.withLock {
                appPreferences.setProviderType(providerType)
            }
            recomputeValidation()
        }
    }

    fun updateBaseUrl(baseUrl: String) {
        _uiState.update { it.copy(baseUrl = baseUrl) }
        invalidateConnectionStatus()
        scheduleChatSettingsPersist()
        recomputeValidation()
    }

    fun updateModel(model: String) {
        _uiState.update { it.copy(model = model) }
        invalidateConnectionStatus()
        scheduleChatSettingsPersist()
    }

    fun updateStreamEnabled(enabled: Boolean) {
        _uiState.update { it.copy(streamEnabled = enabled) }
        scheduleChatSettingsPersist()
    }

    fun updateSummaryCompressionEnabled(enabled: Boolean) {
        _uiState.update { it.copy(summaryCompressionEnabled = enabled) }
        scheduleChatSettingsPersist()
    }

    fun updatePresetDraftName(value: String) {
        _uiState.update { it.copy(presetDraftName = value) }
    }

    fun startRenamingPreset(presetId: String) {
        viewModelScope.launch {
            val preset = configPresetStore.getPreset(presetId) ?: return@launch
            _uiState.update {
                it.copy(
                    presetDraftName = preset.name,
                    editingPresetId = presetId
                )
            }
        }
    }

    fun saveDirectApiKey(value: String) {
        secretStore.saveDirectApiKey(value)
        _uiState.update { it.copy(directApiKey = value) }
        invalidateConnectionStatus()
        recomputeValidation()
    }

    fun saveRelayToken(value: String) {
        secretStore.saveRelayToken(value)
        _uiState.update { it.copy(relayToken = value) }
        invalidateConnectionStatus()
        recomputeValidation()
    }

    fun applyProviderTemplate(templateId: String) {
        val template = providerTemplates.firstOrNull { it.id == templateId } ?: return
        val streamEnabled = uiState.value.streamEnabled
        viewModelScope.launch {
            invalidateConnectionStatus()
            preferencesWriteMutex.withLock {
                appPreferences.updateProviderAndChatSettings(
                    providerType = template.providerType,
                    baseUrl = template.baseUrl,
                    model = template.model,
                    streamEnabled = streamEnabled
                )
            }
            recomputeValidation()
        }
    }

    fun toggleDirectApiKeyVisibility() {
        _uiState.update { state ->
            state.copy(directApiKeyVisible = !state.directApiKeyVisible)
        }
    }

    fun toggleRelayTokenVisibility() {
        _uiState.update { state ->
            state.copy(relayTokenVisible = !state.relayTokenVisible)
        }
    }

    fun runConnectionTest() {
        val state = uiState.value
        val requestVersion = connectionStatusVersion + 1
        connectionStatusVersion = requestVersion
        _uiState.update { it.copy(connectionStatusLabel = "Testing...") }
        viewModelScope.launch {
            val result = connectionTestService.testConnection(
                baseUrl = state.baseUrl,
                model = state.model,
                apiKey = state.directApiKey.takeIf { state.providerType == ProviderType.DIRECT },
                relayToken = state.relayToken.takeIf { state.providerType == ProviderType.RELAY },
                isRelay = state.providerType == ProviderType.RELAY
            )
            if (connectionStatusVersion != requestVersion) {
                return@launch
            }
            _uiState.update { current ->
                current.copy(
                    connectionStatusLabel = when (result) {
                        is ConnectionTestResult.Success -> "Ready"
                        is ConnectionTestResult.Failure -> "Failed: ${result.message}"
                    }
                )
            }
        }
    }

    fun resetConnectionStatus() {
        connectionStatusVersion += 1
        _uiState.update { it.copy(connectionStatusLabel = null) }
    }

    fun saveCurrentConfigAsPreset() {
        val state = uiState.value
        val name = state.presetDraftName.trim()
        if (name.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val presetToSave = state.editingPresetId
                ?.let { editingId ->
                    configPresetStore.getPreset(editingId)?.copy(name = name)
                }
                ?: ConfigPreset(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    providerType = state.providerType,
                    baseUrl = state.baseUrl,
                    model = state.model,
                    streamEnabled = state.streamEnabled,
                    directApiKey = state.directApiKey.takeIf { state.providerType == ProviderType.DIRECT },
                    relayToken = state.relayToken.takeIf { state.providerType == ProviderType.RELAY }
                )
            if (presetToSave != null) {
                configPresetStore.savePreset(presetToSave)
            }
            _uiState.update { it.copy(presetDraftName = "", editingPresetId = null) }
        }
    }

    fun activatePreset(presetId: String) {
        viewModelScope.launch {
            val preset = configPresetStore.getPreset(presetId) ?: return@launch
            invalidateConnectionStatus()
            configPresetStore.setActivePresetId(presetId)
            preferencesWriteMutex.withLock {
                appPreferences.updateProviderAndChatSettings(
                    providerType = preset.providerType,
                    baseUrl = preset.baseUrl,
                    model = preset.model,
                    streamEnabled = preset.streamEnabled
                )
            }
            when (preset.providerType) {
                ProviderType.DIRECT -> {
                    preset.directApiKey?.let(secretStore::saveDirectApiKey)
                }

                ProviderType.RELAY -> {
                    preset.relayToken?.let(secretStore::saveRelayToken)
                }
            }
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            configPresetStore.deletePreset(presetId)
            _uiState.update { state ->
                if (state.editingPresetId == presetId) {
                    state.copy(presetDraftName = "", editingPresetId = null)
                } else {
                    state
                }
            }
        }
    }

    private fun scheduleChatSettingsPersist() {
        settingsPersistJob?.cancel()
        settingsPersistJob = viewModelScope.launch {
            val state = uiState.value
            preferencesWriteMutex.withLock {
                appPreferences.updateRuntimeSettings(
                    baseUrl = state.baseUrl,
                    model = state.model,
                    streamEnabled = state.streamEnabled,
                    summaryCompressionEnabled = state.summaryCompressionEnabled
                )
            }
        }
    }

    private fun recomputeValidation() {
        val state = uiState.value
        val validation = providerSelector.validate(
            state.providerType,
            state.baseUrl,
            state.directApiKey.takeIf { state.providerType == ProviderType.DIRECT },
            state.relayToken.takeIf { state.providerType == ProviderType.RELAY }
        )
        _uiState.update {
            it.copy(
                baseUrlError = validation.baseUrlError,
                modelError = validation.modelError,
                credentialError = validation.credentialError,
                readinessLabel = validation.readinessLabel
            )
        }
    }

    private fun invalidateConnectionStatus() {
        connectionStatusVersion += 1
        _uiState.update { current ->
            if (current.connectionStatusLabel == null) {
                current
            } else {
                current.copy(connectionStatusLabel = null)
            }
        }
    }
}
