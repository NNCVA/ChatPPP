package com.chatppp.app.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    state: ConversationListUiState,
    onBack: () -> Unit,
    onCreateConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Conversations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Navigate up"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateConversation
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "New conversation"
                )
            }
        }
    ) { innerPadding ->
        if (state.conversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No conversations yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Create a new chat to get started.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(
                    items = state.conversations,
                    key = { it.id }
                ) { conversation ->
                    ListItem(
                        headlineContent = { Text(conversation.title) },
                        supportingContent = { Text(conversation.providerType.name) },
                        trailingContent = {
                            IconButton(onClick = { onDeleteConversation(conversation.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete conversation"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConversationClick(conversation.id) }
                    )
                }
            }
        }
    }
}
