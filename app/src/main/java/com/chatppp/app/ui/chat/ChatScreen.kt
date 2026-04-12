package com.chatppp.app.ui.chat

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.chatppp.app.ui.chat.components.ComposerBar
import com.chatppp.app.ui.chat.components.MessageList
import com.chatppp.app.ui.chat.components.SetupRequiredCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onAction: (ChatAction) -> Unit,
    onOpenConversations: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(state.copiedMessageContent) {
        state.copiedMessageContent?.let { content ->
            clipboardManager.setText(buildAnnotatedString { append(content) })
            onAction(ChatAction.CopyHandled)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ChatPPP",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onOpenConversations) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Open conversations"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Open settings"
                        )
                    }
                }
            )
        },
        bottomBar = {
            ComposerBar(
                inputText = state.inputText,
                isStreaming = state.isStreaming,
                onInputChanged = { onAction(ChatAction.UpdateInput(it)) },
                onSendClick = { onAction(ChatAction.SendMessage) },
                onStopClick = { onAction(ChatAction.StopGenerating) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.availablePresets.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.selectedPresetName?.let { "Preset: $it" } ?: "Select a preset",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        state.availablePresets.forEach { preset ->
                            FilterChip(
                                selected = preset.id == state.selectedPresetId,
                                onClick = { onAction(ChatAction.SelectPreset(preset.id)) },
                                label = { Text(preset.name) }
                            )
                        }
                    }
                    if (state.requestPhaseLabel != null) {
                        Text(
                            text = state.requestPhaseLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.compressionNotice != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.compressionNotice,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (state.messages.isEmpty()) {
                if (state.requiresSetup) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        SetupRequiredCard(
                            readinessLabel = state.readinessLabel,
                            onOpenSettings = onOpenSettings
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Start a conversation",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Your replies will appear here once you send a message.",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                MessageList(
                    messages = state.messages,
                    onRetryClick = { onAction(ChatAction.RetryMessage(it)) },
                    onToggleThinkingClick = { onAction(ChatAction.ToggleThinking(it)) },
                    onOpenSettingsClick = onOpenSettings,
                    onCopyClick = { onAction(ChatAction.CopyMessage(it)) },
                    onEditResendClick = { onAction(ChatAction.EditMessage(it)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
