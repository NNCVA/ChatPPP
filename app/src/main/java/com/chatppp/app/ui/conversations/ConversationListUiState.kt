package com.chatppp.app.ui.conversations

import com.chatppp.app.domain.model.Conversation
import com.chatppp.app.domain.model.ConversationPreview
import com.chatppp.app.domain.model.Message

data class ConversationListUiState(
    val conversations: List<ConversationPreview> = emptyList(),
    val lastDeletedConversation: Conversation? = null,
    val lastDeletedMessages: List<Message> = emptyList()
)
