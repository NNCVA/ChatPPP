package com.chatppp.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatppp.app.di.AppEntryPoint
import com.chatppp.app.ui.common.rememberViewModelFactory
import dagger.hilt.android.EntryPointAccessors

@Composable
fun SettingsRoute(
    onBack: () -> Unit
) {
    val applicationContext = LocalContext.current.applicationContext
    val entryPoint = remember(applicationContext) {
        EntryPointAccessors.fromApplication(applicationContext, AppEntryPoint::class.java)
    }
    val viewModel: SettingsViewModel = viewModel(
        factory = rememberViewModelFactory {
            SettingsViewModel(
                appPreferences = entryPoint.appPreferences(),
                secretStore = entryPoint.secretStore(),
                configPresetStore = entryPoint.configPresetStore()
            )
        }
    )
    val state by viewModel.uiState.collectAsState()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onProviderTypeChanged = viewModel::updateProviderType,
        onBaseUrlChanged = viewModel::updateBaseUrl,
        onModelChanged = viewModel::updateModel,
        onStreamEnabledChanged = viewModel::updateStreamEnabled,
        onSummaryCompressionEnabledChanged = viewModel::updateSummaryCompressionEnabled,
        onPresetDraftNameChanged = viewModel::updatePresetDraftName,
        onSaveCurrentPreset = viewModel::saveCurrentConfigAsPreset,
        onActivatePreset = viewModel::activatePreset,
        onStartRenamingPreset = viewModel::startRenamingPreset,
        onDeletePreset = viewModel::deletePreset,
        onDirectApiKeyChanged = viewModel::saveDirectApiKey,
        onRelayTokenChanged = viewModel::saveRelayToken,
        onToggleDirectApiKeyVisibility = viewModel::toggleDirectApiKeyVisibility,
        onToggleRelayTokenVisibility = viewModel::toggleRelayTokenVisibility
    )
}
