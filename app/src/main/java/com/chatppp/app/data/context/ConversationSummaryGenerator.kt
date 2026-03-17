package com.chatppp.app.data.context

import com.chatppp.app.data.local.entity.ConversationSummaryEntity
import com.chatppp.app.data.local.entity.MessageEntity
import com.chatppp.app.data.remote.model.ChatMessageDto
import com.chatppp.app.data.remote.model.ChatRequestDto
import com.chatppp.app.data.remote.provider.ChatProvider

class ConversationSummaryGenerator {
    suspend fun generate(
        provider: ChatProvider,
        model: String,
        existingSummary: ConversationSummaryEntity?,
        messagesToSummarize: List<MessageEntity>,
        budget: CompressionBudget = CompressionBudget()
    ): String {
        if (messagesToSummarize.isEmpty()) {
            return existingSummary?.summaryText.orEmpty()
        }

        val boundedMessages = trimMessagesToFitBudget(
            existingSummary = existingSummary,
            messagesToSummarize = messagesToSummarize,
            budget = budget
        )

        val request = ChatRequestDto(
            model = model,
            messages = listOf(
                ChatMessageDto(
                    role = "system",
                    content = "Compress the prior conversation into a compact running summary. Keep user preferences, confirmed facts, goals, constraints, and unfinished work."
                ),
                ChatMessageDto(
                    role = "user",
                    content = buildPrompt(existingSummary, boundedMessages)
                )
            ),
            stream = false
        )
        return provider.send(request)
    }

    private fun buildPrompt(
        existingSummary: ConversationSummaryEntity?,
        messagesToSummarize: List<MessageEntity>
    ): String = buildString {
        appendLine("Existing summary:")
        appendLine(existingSummary?.summaryText.orEmpty())
        appendLine()
        appendLine("New messages to fold in:")
        messagesToSummarize.forEach { message ->
            appendLine("${message.role.lowercase()}: ${message.content}")
        }
    }.trim()

    private fun trimMessagesToFitBudget(
        existingSummary: ConversationSummaryEntity?,
        messagesToSummarize: List<MessageEntity>,
        budget: CompressionBudget
    ): List<MessageEntity> {
        val estimator = ContextTokenEstimator()
        val selected = ArrayDeque<MessageEntity>()
        messagesToSummarize.asReversed().forEach { message ->
            val candidateMessages = listOf(message) + selected.toList()
            val candidateRequest = listOf(
                ChatMessageDto(
                    role = "system",
                    content = "Compress the prior conversation into a compact running summary. Keep user preferences, confirmed facts, goals, constraints, and unfinished work."
                ),
                ChatMessageDto(
                    role = "user",
                    content = buildPrompt(existingSummary, candidateMessages)
                )
            )
            val estimatedTokens = estimator.estimateRequestTokens(
                messages = candidateRequest,
                reservedResponseTokens = budget.reservedResponseTokens
            )
            if (selected.isEmpty() || estimatedTokens <= budget.maxContextTokens) {
                selected.addFirst(message)
            }
        }
        return selected.toList()
    }
}
