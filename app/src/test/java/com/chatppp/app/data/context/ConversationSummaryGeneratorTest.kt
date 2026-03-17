package com.chatppp.app.data.context

import com.chatppp.app.data.local.entity.ConversationSummaryEntity
import com.chatppp.app.data.local.entity.MessageEntity
import com.chatppp.app.data.remote.model.ChatRequestDto
import com.chatppp.app.data.remote.provider.ChatProvider
import com.chatppp.app.domain.model.ChatChunk
import com.chatppp.app.domain.model.MessageStatus
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ConversationSummaryGeneratorTest {
    @Test
    fun generate_builds_incremental_summary_request_from_existing_summary_and_older_messages() = runTest {
        val provider = RecordingChatProvider(response = "Updated summary")
        val generator = ConversationSummaryGenerator()

        val summary = generator.generate(
            provider = provider,
            model = "deepseek-chat",
            existingSummary = ConversationSummaryEntity(
                conversationId = "conversation-1",
                summaryText = "Existing summary",
                coveredUntilMessageId = "m2",
                coveredUntilCreatedAt = 2L,
                updatedAt = 10L
            ),
            messagesToSummarize = listOf(
                message(id = "m3", role = "USER", content = "We are building an Android AI chat client.", createdAt = 3L),
                message(id = "m4", role = "ASSISTANT", content = "We still need summary compression.", createdAt = 4L)
            )
        )

        assertEquals("Updated summary", summary)
        assertEquals(1, provider.requests.size)

        val request = provider.requests.single()
        assertEquals("deepseek-chat", request.model)
        assertFalse(request.stream)
        assertEquals("system", request.messages.first().role)
        assertTrue(request.messages.first().content.contains("Compress"))
        assertTrue(request.messages.last().content.contains("Existing summary"))
        assertTrue(request.messages.last().content.contains("Android AI chat client"))
    }

    private fun message(
        id: String,
        role: String,
        content: String,
        createdAt: Long
    ) = MessageEntity(
        id = id,
        conversationId = "conversation-1",
        role = role,
        content = content,
        status = MessageStatus.SUCCESS.name,
        createdAt = createdAt
    )
}

private class RecordingChatProvider(
    private val response: String
) : ChatProvider {
    val requests = mutableListOf<ChatRequestDto>()

    override suspend fun send(request: ChatRequestDto): String {
        requests += request
        return response
    }

    override fun stream(request: ChatRequestDto) = emptyFlow<ChatChunk>()
}
