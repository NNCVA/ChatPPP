package com.chatppp.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chatppp.app.domain.model.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onProviderTypeChanged: (ProviderType) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onStreamEnabledChanged: (Boolean) -> Unit,
    onSummaryCompressionEnabledChanged: (Boolean) -> Unit,
    onPresetDraftNameChanged: (String) -> Unit,
    onSaveCurrentPreset: () -> Unit,
    onActivatePreset: (String) -> Unit,
    onStartRenamingPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onDirectApiKeyChanged: (String) -> Unit,
    onRelayTokenChanged: (String) -> Unit,
    onToggleDirectApiKeyVisibility: () -> Unit,
    onToggleRelayTokenVisibility: () -> Unit,
    onRunConnectionTest: () -> Unit,
    onResetConnectionStatus: () -> Unit,
    onApplyProviderTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Navigate up"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Provider",
                style = MaterialTheme.typography.titleMedium
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = state.providerType == ProviderType.DIRECT,
                    onClick = { onProviderTypeChanged(ProviderType.DIRECT) },
                    label = { Text("Direct") }
                )
                FilterChip(
                    selected = state.providerType == ProviderType.RELAY,
                    onClick = { onProviderTypeChanged(ProviderType.RELAY) },
                    label = { Text("Relay") }
                )
            }

            if (state.providerType == ProviderType.RELAY) {
                Text(
                    text = "Relay mode requires your own backend",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = state.readinessLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (state.readinessLabel == "Ready") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.testTag("readiness-label")
            )

            OutlinedButton(
                onClick = onRunConnectionTest,
                modifier = Modifier.testTag("test-connection-button")
            ) {
                Text("Test connection")
            }

            state.connectionStatusLabel?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status == "Ready" || status == "Testing...") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.testTag("connection-status-label")
                )
            }

            Text(
                text = "Provider templates",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Apply a starter endpoint and model preset. Secrets stay untouched until you paste your own key or relay token.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.providerTemplates.forEach { template ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "URL: ${template.baseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Model: ${template.model}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            onApplyProviderTemplate(template.id)
                            onResetConnectionStatus()
                        },
                        modifier = Modifier.testTag("provider-template-${template.id}")
                    ) {
                        Text("Apply template")
                    }
                }
            }

            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("base-url-input"),
                label = { Text("OpenAI Base URL") },
                isError = state.baseUrlError != null,
                supportingText = state.baseUrlError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = state.model,
                onValueChange = onModelChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("model-input"),
                label = { Text("Model") }
            )

            if (state.providerType == ProviderType.DIRECT) {
                OutlinedTextField(
                    value = state.directApiKey,
                    onValueChange = onDirectApiKeyChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct-api-key-input"),
                    label = { Text("Direct API Key") },
                    isError = state.credentialError != null,
                    supportingText = state.credentialError?.let { { Text(it) } },
                    visualTransformation = if (state.directApiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = onToggleDirectApiKeyVisibility
                        ) {
                            Icon(
                                imageVector = if (state.directApiKeyVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = "Toggle direct API key visibility"
                            )
                        }
                    }
                )
            } else {
                OutlinedTextField(
                    value = state.relayToken,
                    onValueChange = onRelayTokenChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("relay-token-input"),
                    label = { Text("Relay Token") },
                    isError = state.credentialError != null,
                    supportingText = state.credentialError?.let { { Text(it) } },
                    visualTransformation = if (state.relayTokenVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = onToggleRelayTokenVisibility
                        ) {
                            Icon(
                                imageVector = if (state.relayTokenVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = "Toggle relay token visibility"
                            )
                        }
                    }
                )
                Text(
                    text = "Use a token issued by your backend relay, not an upstream model API key",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Stream response",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Enable token-by-token response rendering",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.streamEnabled,
                    onCheckedChange = onStreamEnabledChanged
                )
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Summary compression",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Summarize older messages and keep recent raw turns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.summaryCompressionEnabled,
                    onCheckedChange = onSummaryCompressionEnabledChanged,
                    modifier = Modifier.testTag("summary-compression-switch")
                )
            }

            Text(
                text = "Config presets",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = state.presetDraftName,
                onValueChange = onPresetDraftNameChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preset-name-input"),
                label = { Text("Preset name") }
            )

            OutlinedButton(
                onClick = onSaveCurrentPreset,
                modifier = Modifier.testTag("save-preset-button")
            ) {
                Text(if (state.editingPresetId == null) "Save current as preset" else "Rename preset")
            }

            state.savedPresets.forEach { preset ->
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${preset.providerType.name.lowercase()} · ${preset.model}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onActivatePreset(preset.id) },
                            modifier = Modifier.testTag("activate-preset-${preset.id}")
                        ) {
                            Text(if (preset.isActive) "Active" else "Use")
                        }
                        OutlinedButton(
                            onClick = { onStartRenamingPreset(preset.id) },
                            modifier = Modifier.testTag("rename-preset-${preset.id}")
                        ) {
                            Text("Rename")
                        }
                        OutlinedButton(
                            onClick = { onDeletePreset(preset.id) },
                            modifier = Modifier.testTag("delete-preset-${preset.id}")
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
