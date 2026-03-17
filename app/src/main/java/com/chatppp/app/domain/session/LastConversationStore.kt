package com.chatppp.app.domain.session

import kotlinx.coroutines.flow.Flow

interface LastConversationStore {
    val lastConversationId: Flow<String?>

    suspend fun setLastConversationId(conversationId: String)

    suspend fun clearLastConversationId()
}
