package com.chatppp.app.data.local.entity

import com.chatppp.app.data.mapper.toDomain
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappingTest {
    @Test
    fun conversation_entity_maps_all_core_fields() {
        val entity = ConversationEntity(
            id = "conversation-1",
            title = "First chat",
            providerType = "DIRECT",
            createdAt = 100L,
            updatedAt = 200L
        )

        val model = entity.toDomain()

        assertEquals("conversation-1", model.id)
        assertEquals("First chat", model.title)
        assertEquals(100L, model.createdAt)
        assertEquals(200L, model.updatedAt)
    }

    @Test
    fun message_entity_maps_role_and_status() {
        val entity = MessageEntity(
            id = "message-1",
            conversationId = "conversation-1",
            role = "ASSISTANT",
            content = "Hello",
            status = "STREAMING",
            createdAt = 300L
        )

        val model = entity.toDomain()

        assertEquals("message-1", model.id)
        assertEquals("conversation-1", model.conversationId)
        assertEquals(MessageRole.ASSISTANT, model.role)
        assertEquals(MessageStatus.STREAMING, model.status)
        assertEquals("Hello", model.content)
    }
}
