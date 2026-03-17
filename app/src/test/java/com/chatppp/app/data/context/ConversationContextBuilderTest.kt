package com.chatppp.app.data.context

import com.chatppp.app.data.local.entity.ConversationSummaryEntity
import com.chatppp.app.data.local.entity.MessageEntity
import com.chatppp.app.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContextBuilderTest {
    private val tokenEstimator = ContextTokenEstimator()

    @Test
    fun build_preserves_complete_recent_turns_when_trimming_by_token_budget() {
        val builder = ConversationContextBuilder(tokenEstimator)
        val messages = listOf(
            message(id = "m1", role = "USER", content = "First user request", createdAt = 1L),
            message(id = "m2", role = "ASSISTANT", content = "First assistant reply", createdAt = 2L),
            message(id = "m3", role = "USER", content = "Second user request", createdAt = 3L),
            message(id = "m4", role = "ASSISTANT", content = "Second assistant reply", createdAt = 4L),
            message(id = "m5", role = "USER", content = "Latest user request", createdAt = 5L)
        )

        val result = builder.build(
            messages = messages,
            storedSummary = ConversationSummaryEntity(
                conversationId = "conversation-1",
                summaryText = "First turn already summarized.",
                coveredUntilMessageId = "m2",
                coveredUntilCreatedAt = 2L,
                updatedAt = 10L
            ),
            budget = CompressionBudget(
                maxContextTokens = 32_768,
                compressionTriggerTokens = 80,
                targetCompressedTokens = 72,
                reservedResponseTokens = 6
            )
        )

        assertEquals(
            listOf(
                "system" to "Conversation summary:\nFirst turn already summarized.",
                "user" to "Second user request",
                "assistant" to "Second assistant reply",
                "user" to "Latest user request"
            ),
            result.requestMessages.map { it.role to it.content }
        )
        assertEquals(emptyList<String>(), result.messagesToSummarize.map { it.id })
        assertTrue(result.estimatedRequestTokens <= 72)
    }

    @Test
    fun build_excludes_error_and_empty_assistant_messages_from_raw_context_and_summary_candidates() {
        val builder = ConversationContextBuilder(tokenEstimator)
        val messages = listOf(
            message(id = "m1", role = "USER", content = "Keep me", createdAt = 1L),
            message(
                id = "m2",
                role = "ASSISTANT",
                content = "Authentication failed for chat request",
                status = MessageStatus.ERROR.name,
                createdAt = 2L
            ),
            message(
                id = "m3",
                role = "ASSISTANT",
                content = "",
                status = MessageStatus.SUCCESS.name,
                createdAt = 3L
            ),
            message(id = "m4", role = "USER", content = "Newest user", createdAt = 4L)
        )

        val result = builder.build(
            messages = messages,
            storedSummary = null,
            budget = CompressionBudget(
                maxContextTokens = 32_768,
                compressionTriggerTokens = 12,
                targetCompressedTokens = 12,
                reservedResponseTokens = 4
            )
        )

        assertEquals(listOf("m1"), result.messagesToSummarize.map { it.id })
        assertEquals(
            listOf("user" to "Newest user"),
            result.requestMessages.map { it.role to it.content }
        )
    }

    private fun message(
        id: String,
        role: String,
        content: String,
        createdAt: Long,
        status: String = MessageStatus.SUCCESS.name
    ) = MessageEntity(
        id = id,
        conversationId = "conversation-1",
        role = role,
        content = content,
        status = status,
        createdAt = createdAt
    )
}
