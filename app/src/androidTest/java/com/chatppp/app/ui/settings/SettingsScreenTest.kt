package com.chatppp.app.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chatppp.app.domain.model.ProviderType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun direct_mode_shows_direct_api_key_field() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(providerType = ProviderType.DIRECT),
                onBack = {},
                onProviderTypeChanged = {},
                onBaseUrlChanged = {},
                onModelChanged = {},
                onStreamEnabledChanged = {},
                onSummaryCompressionEnabledChanged = {},
                onPresetDraftNameChanged = {},
                onSaveCurrentPreset = {},
                onActivatePreset = {},
                onStartRenamingPreset = {},
                onDeletePreset = {},
                onDirectApiKeyChanged = {},
                onRelayTokenChanged = {},
                onToggleDirectApiKeyVisibility = {},
                onToggleRelayTokenVisibility = {},
                onRunConnectionTest = {},
                onResetConnectionStatus = {},
                onApplyProviderTemplate = {}
            )
        }

        composeRule.onNodeWithText("OpenAI Base URL").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Direct API Key").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("direct-api-key-input").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("relay-token-input").assertCountEquals(0)
    }

    @Test
    fun relay_mode_shows_relay_token_field() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(providerType = ProviderType.RELAY),
                onBack = {},
                onProviderTypeChanged = {},
                onBaseUrlChanged = {},
                onModelChanged = {},
                onStreamEnabledChanged = {},
                onSummaryCompressionEnabledChanged = {},
                onPresetDraftNameChanged = {},
                onSaveCurrentPreset = {},
                onActivatePreset = {},
                onStartRenamingPreset = {},
                onDeletePreset = {},
                onDirectApiKeyChanged = {},
                onRelayTokenChanged = {},
                onToggleDirectApiKeyVisibility = {},
                onToggleRelayTokenVisibility = {},
                onRunConnectionTest = {},
                onResetConnectionStatus = {},
                onApplyProviderTemplate = {}
            )
        }

        composeRule.onNodeWithText("Relay Token").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Relay mode requires your own backend").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("relay-token-input").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("direct-api-key-input").assertCountEquals(0)
    }

    @Test
    fun presets_section_shows_saved_preset_controls() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(
                    savedPresets = listOf(
                        SettingsPresetUiModel(
                            id = "preset-1",
                            name = "Work Direct",
                            providerType = ProviderType.DIRECT,
                            model = "gpt-4.1-mini",
                            isActive = true
                        )
                    )
                ),
                onBack = {},
                onProviderTypeChanged = {},
                onBaseUrlChanged = {},
                onModelChanged = {},
                onStreamEnabledChanged = {},
                onSummaryCompressionEnabledChanged = {},
                onPresetDraftNameChanged = {},
                onSaveCurrentPreset = {},
                onActivatePreset = {},
                onStartRenamingPreset = {},
                onDeletePreset = {},
                onDirectApiKeyChanged = {},
                onRelayTokenChanged = {},
                onToggleDirectApiKeyVisibility = {},
                onToggleRelayTokenVisibility = {},
                onRunConnectionTest = {},
                onResetConnectionStatus = {},
                onApplyProviderTemplate = {}
            )
        }

        composeRule.onNodeWithTag("preset-name-input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Work Direct").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("activate-preset-preset-1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("rename-preset-preset-1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("delete-preset-preset-1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun provider_templates_section_is_visible() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(),
                onBack = {},
                onProviderTypeChanged = {},
                onBaseUrlChanged = {},
                onModelChanged = {},
                onStreamEnabledChanged = {},
                onSummaryCompressionEnabledChanged = {},
                onPresetDraftNameChanged = {},
                onSaveCurrentPreset = {},
                onActivatePreset = {},
                onStartRenamingPreset = {},
                onDeletePreset = {},
                onDirectApiKeyChanged = {},
                onRelayTokenChanged = {},
                onToggleDirectApiKeyVisibility = {},
                onToggleRelayTokenVisibility = {},
                onRunConnectionTest = {},
                onResetConnectionStatus = {},
                onApplyProviderTemplate = {}
            )
        }

        composeRule.onNodeWithText("Provider templates").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun recent_raw_messages_field_is_not_shown() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(),
                onBack = {},
                onProviderTypeChanged = {},
                onBaseUrlChanged = {},
                onModelChanged = {},
                onStreamEnabledChanged = {},
                onSummaryCompressionEnabledChanged = {},
                onPresetDraftNameChanged = {},
                onSaveCurrentPreset = {},
                onActivatePreset = {},
                onStartRenamingPreset = {},
                onDeletePreset = {},
                onDirectApiKeyChanged = {},
                onRelayTokenChanged = {},
                onToggleDirectApiKeyVisibility = {},
                onToggleRelayTokenVisibility = {},
                onRunConnectionTest = {},
                onResetConnectionStatus = {},
                onApplyProviderTemplate = {}
            )
        }

        composeRule.onAllNodesWithTag("recent-raw-window-input").assertCountEquals(0)
        composeRule.onAllNodesWithTag("summary-compression-switch").assertCountEquals(1)
    }
}
