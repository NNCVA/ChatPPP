package com.chatppp.app.data.context

import com.chatppp.app.data.remote.model.ChatMessageDto
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextTokenEstimatorTest {
    private val estimator = ContextTokenEstimator()

    @Test
    fun estimate_message_tokens_grows_with_message_content() {
        val shortTokens = estimator.estimateMessageTokens(
            ChatMessageDto(role = "user", content = "short")
        )
        val longTokens = estimator.estimateMessageTokens(
            ChatMessageDto(role = "user", content = "This is a much longer message payload than the short one.")
        )

        assertTrue(longTokens > shortTokens)
    }

    @Test
    fun estimate_request_tokens_includes_multiple_messages_and_response_reserve() {
        val tokens = estimator.estimateRequestTokens(
            messages = listOf(
                ChatMessageDto(role = "system", content = "Conversation summary"),
                ChatMessageDto(role = "user", content = "Latest request")
            ),
            reservedResponseTokens = 6_144
        )

        assertTrue(tokens > 6_144)
    }
}
