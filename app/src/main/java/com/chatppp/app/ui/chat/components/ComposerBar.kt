package com.chatppp.app.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

@Composable
fun ComposerBar(
    inputText: String,
    isStreaming: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { androidx.compose.material3.Text("Ask anything") }
            )

            if (isStreaming) {
                IconButton(
                    onClick = onStopClick
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StopCircle,
                        contentDescription = "Stop generating"
                    )
                }
            } else {
                IconButton(
                    onClick = onSendClick,
                    enabled = inputText.trim().isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}
