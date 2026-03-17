package com.chatppp.app.ui.chat.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatppp.app.ui.common.UiMessage

@Composable
fun MessageList(
    messages: List<UiMessage>,
    onRetryClick: (String) -> Unit,
    onToggleThinkingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            MessageBubble(
                message = message,
                onRetryClick = onRetryClick,
                onToggleThinkingClick = onToggleThinkingClick,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
