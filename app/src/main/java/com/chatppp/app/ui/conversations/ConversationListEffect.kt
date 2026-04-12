package com.chatppp.app.ui.conversations

sealed interface ConversationListEffect {
    data class OpenConversation(val conversationId: String) : ConversationListEffect
    data class ShowUndoDelete(val conversationId: String, val title: String) : ConversationListEffect
}