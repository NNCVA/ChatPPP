package com.chatppp.app.domain.repository

import com.chatppp.app.domain.model.Conversation
import com.chatppp.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>

    suspend fun createConversation(title: String = "New Chat"): String

    suspend fun deleteConversation(conversationId: String)

    fun observeMessages(conversationId: String): Flow<List<Message>>

    suspend fun sendMessage(
        conversationId: String,
        userInput: String
    )

    suspend fun bindPresetToConversation(
        conversationId: String,
        presetId: String?
    )

    suspend fun stopStreaming(conversationId: String)
}
