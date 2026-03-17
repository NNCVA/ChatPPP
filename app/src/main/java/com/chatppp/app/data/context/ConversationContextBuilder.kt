package com.chatppp.app.data.context

import com.chatppp.app.data.local.entity.ConversationSummaryEntity
import com.chatppp.app.data.local.entity.MessageEntity
import com.chatppp.app.data.remote.model.ChatMessageDto
import com.chatppp.app.domain.model.MessageStatus

data class ConversationContextBuildResult(
    val requestMessages: List<ChatMessageDto>,
    val messagesToSummarize: List<MessageEntity>,
    val estimatedRequestTokens: Int
)

class ConversationContextBuilder(
    private val tokenEstimator: ContextTokenEstimator = ContextTokenEstimator()
) {
    fun build(
        messages: List<MessageEntity>,
        storedSummary: ConversationSummaryEntity?,
        budget: CompressionBudget
    ): ConversationContextBuildResult {
        val eligibleMessages = messages
            .filter(::isEligibleMessage)
            .sortedBy { it.createdAt }
        val turns = partitionIntoTurns(eligibleMessages)
        val summaryMessage = storedSummary?.summaryText
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { ChatMessageDto(role = "system", content = "Conversation summary:\n$it") }

        val selectedTurns = selectTurnsWithinBudget(
            turns = turns,
            summaryMessage = summaryMessage,
            budget = budget
        )
        val selectedMessages = selectedTurns.flatten()
        val includeSummary = summaryMessage != null && selectedMessages.size < eligibleMessages.size
        val requestMessages = buildRequestMessages(selectedMessages, summaryMessage.takeIf { includeSummary })
        val estimatedRequestTokens = tokenEstimator.estimateRequestTokens(
            messages = requestMessages,
            reservedResponseTokens = budget.reservedResponseTokens
        )
        val coveredUntilCreatedAt = storedSummary?.coveredUntilCreatedAt ?: Long.MIN_VALUE
        val messagesToSummarize = eligibleMessages.filter { message ->
            message !in selectedMessages && message.createdAt > coveredUntilCreatedAt
        }

        return ConversationContextBuildResult(
            requestMessages = requestMessages,
            messagesToSummarize = messagesToSummarize,
            estimatedRequestTokens = estimatedRequestTokens
        )
    }

    private fun selectTurnsWithinBudget(
        turns: List<List<MessageEntity>>,
        summaryMessage: ChatMessageDto?,
        budget: CompressionBudget
    ): List<List<MessageEntity>> {
        if (turns.isEmpty()) {
            return emptyList()
        }

        val selectedTurns = ArrayDeque<List<MessageEntity>>()
        turns.asReversed().forEach { turn ->
            val candidateTurns = listOf(turn) + selectedTurns.toList()
            val candidateMessages = buildRequestMessages(
                selectedMessages = candidateTurns.flatten(),
                summaryMessage = summaryMessage
            )
            val estimatedTokens = tokenEstimator.estimateRequestTokens(
                messages = candidateMessages,
                reservedResponseTokens = budget.reservedResponseTokens
            )
            if (selectedTurns.isEmpty() || estimatedTokens <= budget.targetCompressedTokens) {
                selectedTurns.addFirst(turn)
            }
        }
        return selectedTurns.toList()
    }

    private fun buildRequestMessages(
        selectedMessages: List<MessageEntity>,
        summaryMessage: ChatMessageDto?
    ): List<ChatMessageDto> = buildList {
        summaryMessage?.let(::add)
        selectedMessages.forEach { message ->
            add(
                ChatMessageDto(
                    role = message.role.lowercase(),
                    content = message.content
                )
            )
        }
    }

    private fun partitionIntoTurns(messages: List<MessageEntity>): List<List<MessageEntity>> {
        if (messages.isEmpty()) {
            return emptyList()
        }

        val turns = mutableListOf<MutableList<MessageEntity>>()
        var currentTurn = mutableListOf<MessageEntity>()
        messages.forEach { message ->
            if (message.role == "USER" && currentTurn.isNotEmpty()) {
                turns += currentTurn
                currentTurn = mutableListOf()
            }
            currentTurn += message
        }
        if (currentTurn.isNotEmpty()) {
            turns += currentTurn
        }
        return turns
    }

    private fun isEligibleMessage(message: MessageEntity): Boolean {
        if (message.status != MessageStatus.SUCCESS.name) {
            return false
        }
        if (message.content.isBlank()) {
            return false
        }
        return true
    }
}
