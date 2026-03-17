package com.chatppp.app.ui.conversations

import com.chatppp.app.domain.model.Conversation

data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList()
)
