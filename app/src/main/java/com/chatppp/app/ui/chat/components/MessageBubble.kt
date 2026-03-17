package com.chatppp.app.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import com.chatppp.app.ui.common.UiMessage

@Composable
fun MessageBubble(
    message: UiMessage,
    onRetryClick: (String) -> Unit,
    onToggleThinkingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = message.content,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (!message.thinkingContent.isNullOrBlank()) {
                        TextButton(
                            onClick = { onToggleThinkingClick(message.id) }
                        ) {
                            Text(
                                text = if (message.isThinkingExpanded) {
                                    "Hide thinking"
                                } else {
                                    "Show thinking"
                                }
                            )
                        }

                        if (message.isThinkingExpanded) {
                            Text(
                                text = message.thinkingContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (message.status == MessageStatus.STREAMING) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(2.dp)
                            .semantics {
                                contentDescription = "Streaming response"
                            },
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Streaming response",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (message.status == MessageStatus.ERROR) {
                IconButton(
                    onClick = { onRetryClick(message.id) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Retry message"
                    )
                }
            }
        }
    }
}
