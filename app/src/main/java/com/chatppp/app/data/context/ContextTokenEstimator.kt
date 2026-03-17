package com.chatppp.app.data.context

import com.chatppp.app.data.remote.model.ChatMessageDto
import kotlin.math.ceil

class ContextTokenEstimator {
    fun estimateMessageTokens(message: ChatMessageDto): Int {
        val reasoningTokens = message.reasoningContent?.let(::estimateTextTokens) ?: 0
        return MESSAGE_OVERHEAD_TOKENS +
            estimateTextTokens(message.role) +
            estimateTextTokens(message.content) +
            reasoningTokens
    }

    fun estimateRequestTokens(
        messages: List<ChatMessageDto>,
        reservedResponseTokens: Int = 0
    ): Int = REQUEST_OVERHEAD_TOKENS +
        messages.sumOf(::estimateMessageTokens) +
        reservedResponseTokens

    fun estimateTextTokens(text: String): Int {
        if (text.isBlank()) {
            return 0
        }
        return ceil(text.length / CHARS_PER_TOKEN).toInt().coerceAtLeast(1)
    }

    private companion object {
        const val CHARS_PER_TOKEN = 4.0
        const val REQUEST_OVERHEAD_TOKENS = 3
        const val MESSAGE_OVERHEAD_TOKENS = 4
    }
}
