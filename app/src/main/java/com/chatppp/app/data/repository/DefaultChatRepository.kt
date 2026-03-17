package com.chatppp.app.data.repository

import com.chatppp.app.data.context.CompressionBudget
import com.chatppp.app.data.context.ConversationContextBuilder
import com.chatppp.app.data.context.ConversationSummaryGenerator
import com.chatppp.app.data.context.ConversationSummaryStore
import com.chatppp.app.data.context.ContextTokenEstimator
import com.chatppp.app.data.context.NoOpConversationSummaryStore
import com.chatppp.app.data.local.db.ConversationDao
import com.chatppp.app.data.local.db.MessageDao
import com.chatppp.app.data.local.entity.ConversationEntity
import com.chatppp.app.data.local.entity.ConversationSummaryEntity
import com.chatppp.app.data.local.entity.MessageEntity
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.mapper.toDomain
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.data.presets.NoOpConfigPresetStore
import com.chatppp.app.data.remote.model.ChatMessageDto
import com.chatppp.app.data.remote.model.ChatRequestDto
import com.chatppp.app.data.remote.provider.ProviderSelector
import com.chatppp.app.domain.model.ChatChunk
import com.chatppp.app.domain.model.ChatError
import com.chatppp.app.domain.model.Conversation
import com.chatppp.app.domain.model.Message
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import com.chatppp.app.domain.model.ProviderType
import com.chatppp.app.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class DefaultChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val appPreferences: AppPreferences,
    private val providerSelector: ProviderSelector,
    private val configPresetStore: ConfigPresetStore = NoOpConfigPresetStore,
    private val conversationSummaryStore: ConversationSummaryStore = NoOpConversationSummaryStore,
    private val conversationContextBuilder: ConversationContextBuilder = ConversationContextBuilder(),
    private val conversationSummaryGenerator: ConversationSummaryGenerator = ConversationSummaryGenerator(),
    private val tokenEstimator: ContextTokenEstimator = ContextTokenEstimator(),
    private val compressionBudget: CompressionBudget = CompressionBudget(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ChatRepository {
    private val activeStreamJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createConversation(title: String): String {
        val now = clock()
        val conversationId = idGenerator()
        val activePresetId = configPresetStore.activePresetId.first()
        val activePreset = activePresetId?.let { configPresetStore.getPreset(it) }
        conversationDao.upsert(
            ConversationEntity(
                id = conversationId,
                title = title,
                providerType = (activePreset?.providerType ?: appPreferences.providerType.first()).name,
                presetId = activePresetId,
                createdAt = now,
                updatedAt = now
            )
        )
        return conversationId
    }

    override suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteByConversationId(conversationId)
        conversationDao.deleteById(conversationId)
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeByConversationId(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun sendMessage(
        conversationId: String,
        userInput: String
    ) {
        val currentJob = currentCoroutineContext().job
        val now = clock()
        val userMessage = MessageEntity(
            id = idGenerator(),
            conversationId = conversationId,
            role = MessageRole.USER.name,
            content = userInput,
            status = MessageStatus.SUCCESS.name,
            createdAt = now
        )
        messageDao.upsert(userMessage)
        conversationDao.updateUpdatedAt(conversationId, now)

        val assistantMessageId = idGenerator()
        var assistantMessage = MessageEntity(
            id = assistantMessageId,
            conversationId = conversationId,
            role = MessageRole.ASSISTANT.name,
            content = "",
            thinkingContent = null,
            status = MessageStatus.STREAMING.name,
            createdAt = now + 1
        )
        messageDao.upsert(assistantMessage)

        try {
            val requestRuntime = resolveRuntimeConfig(conversationId)
            val provider = providerSelector.select(requestRuntime.providerType)
            activeStreamJobs[conversationId] = currentJob
            val model = requestRuntime.model.ifBlank { "deepseek-chat" }

            val request = ChatRequestDto(
                model = model,
                messages = buildRequestMessages(
                    conversationId = conversationId,
                    allMessages = messageDao.observeByConversationId(conversationId).first()
                        .filterNot { it.id == assistantMessageId },
                    provider = provider,
                    model = model
                ),
                stream = requestRuntime.streamEnabled
            )

            if (request.stream) {
                withContext(ioDispatcher) {
                    var assistantContent = ""
                    var assistantThinkingContent = ""
                    provider.stream(request).collect { chunk ->
                        when (chunk) {
                            is ChatChunk.Thinking -> {
                                assistantThinkingContent += chunk.text
                                assistantMessage = assistantMessage.copy(
                                    thinkingContent = assistantThinkingContent,
                                    status = MessageStatus.STREAMING.name
                                )
                                messageDao.upsert(assistantMessage)
                            }

                            is ChatChunk.Content -> {
                                assistantContent += chunk.text
                                assistantMessage = assistantMessage.copy(
                                    content = assistantContent,
                                    status = MessageStatus.STREAMING.name
                                )
                                messageDao.upsert(assistantMessage)
                            }

                            ChatChunk.Done -> {
                                assistantMessage = completeAssistantMessage(
                                    assistantMessage = assistantMessage,
                                    assistantContent = assistantContent
                                )
                                messageDao.upsert(assistantMessage)
                                conversationDao.updateUpdatedAt(conversationId, clock())
                            }
                        }
                    }
                    if (assistantMessage.status == MessageStatus.STREAMING.name) {
                        assistantMessage = completeAssistantMessage(
                            assistantMessage = assistantMessage,
                            assistantContent = assistantContent
                        )
                        messageDao.upsert(assistantMessage)
                        conversationDao.updateUpdatedAt(conversationId, clock())
                    }
                }
            } else {
                val response = withContext(ioDispatcher) {
                    provider.send(request)
                }
                assistantMessage = if (response.isBlank()) {
                    assistantMessage.copy(
                        content = EMPTY_RESPONSE_MESSAGE,
                        status = MessageStatus.ERROR.name
                    )
                } else {
                    assistantMessage.copy(
                        content = response,
                        status = MessageStatus.SUCCESS.name
                    )
                }
                messageDao.upsert(assistantMessage)
                conversationDao.updateUpdatedAt(conversationId, clock())
            }
        } catch (_: CancellationException) {
            withContext(NonCancellable) {
                assistantMessage = completeAssistantMessage(
                    assistantMessage = assistantMessage,
                    assistantContent = assistantMessage.content
                )
                messageDao.upsert(assistantMessage)
                conversationDao.updateUpdatedAt(conversationId, clock())
            }
        } catch (throwable: Throwable) {
            assistantMessage = assistantMessage.copy(
                content = throwable.userFacingMessage().ifBlank { assistantMessage.content },
                status = MessageStatus.ERROR.name
            )
            messageDao.upsert(assistantMessage)
            conversationDao.updateUpdatedAt(conversationId, clock())
        } finally {
            activeStreamJobs.remove(conversationId, currentJob)
        }
    }

    override suspend fun bindPresetToConversation(
        conversationId: String,
        presetId: String?
    ) {
        val providerType = presetId
            ?.let { configPresetStore.getPreset(it)?.providerType }
            ?: appPreferences.providerType.first()
        conversationDao.updatePresetBinding(
            conversationId = conversationId,
            presetId = presetId,
            providerType = providerType.name,
            updatedAt = clock()
        )
    }

    override suspend fun stopStreaming(conversationId: String) {
        activeStreamJobs.remove(conversationId)?.cancel()
    }

    private fun Throwable.userFacingMessage(): String = when (this) {
        is ChatError -> message.orEmpty()
        else -> ""
    }

    private suspend fun resolveRuntimeConfig(conversationId: String): ResolvedRuntimeConfig {
        val conversation = conversationDao.getById(conversationId)
        val preset = conversation?.presetId?.let { configPresetStore.getPreset(it) }
        return if (preset != null) {
            ResolvedRuntimeConfig(
                providerType = preset.providerType,
                model = preset.model,
                streamEnabled = preset.streamEnabled
            )
        } else {
            ResolvedRuntimeConfig(
                providerType = appPreferences.providerType.first(),
                model = appPreferences.model.first(),
                streamEnabled = appPreferences.streamEnabled.first()
            )
        }
    }

    private suspend fun buildRequestMessages(
        conversationId: String,
        allMessages: List<MessageEntity>,
        provider: com.chatppp.app.data.remote.provider.ChatProvider,
        model: String
    ): List<ChatMessageDto> {
        val rawRequestMessages = allMessages
            .filter(::isReplayableMessage)
            .map { entity ->
                ChatMessageDto(
                    role = entity.role.lowercase(),
                    content = entity.content
                )
            }
        val summaryCompressionEnabled = appPreferences.summaryCompressionEnabled.first()
        if (!summaryCompressionEnabled) {
            return rawRequestMessages
        }

        val rawEstimatedTokens = tokenEstimator.estimateRequestTokens(
            messages = rawRequestMessages,
            reservedResponseTokens = compressionBudget.reservedResponseTokens
        )
        if (rawEstimatedTokens <= compressionBudget.compressionTriggerTokens) {
            return rawRequestMessages
        }

        var storedSummary = conversationSummaryStore.get(conversationId)
        val initialContext = conversationContextBuilder.build(
            messages = allMessages,
            storedSummary = storedSummary,
            budget = compressionBudget
        )
        if (initialContext.messagesToSummarize.isNotEmpty()) {
            storedSummary = tryBuildAndStoreSummary(
                conversationId = conversationId,
                provider = provider,
                model = model,
                existingSummary = storedSummary,
                messagesToSummarize = initialContext.messagesToSummarize
            ) ?: storedSummary
        }

        return conversationContextBuilder.build(
            messages = allMessages,
            storedSummary = storedSummary,
            budget = compressionBudget
        ).requestMessages
    }

    private suspend fun tryBuildAndStoreSummary(
        conversationId: String,
        provider: com.chatppp.app.data.remote.provider.ChatProvider,
        model: String,
        existingSummary: ConversationSummaryEntity?,
        messagesToSummarize: List<MessageEntity>
    ): ConversationSummaryEntity? {
        return try {
            val summaryText = conversationSummaryGenerator.generate(
                provider = provider,
                model = model,
                existingSummary = existingSummary,
                messagesToSummarize = messagesToSummarize,
                budget = compressionBudget
            ).trim()
            if (summaryText.isEmpty()) {
                null
            } else {
                ConversationSummaryEntity(
                    conversationId = conversationId,
                    summaryText = summaryText,
                    coveredUntilMessageId = messagesToSummarize.last().id,
                    coveredUntilCreatedAt = messagesToSummarize.last().createdAt,
                    updatedAt = clock()
                ).also { summary ->
                    conversationSummaryStore.upsert(summary)
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun isReplayableMessage(message: MessageEntity): Boolean {
        if (message.status != MessageStatus.SUCCESS.name) {
            return false
        }
        if (message.content.isBlank()) {
            return false
        }
        return true
    }

    private fun completeAssistantMessage(
        assistantMessage: MessageEntity,
        assistantContent: String
    ): MessageEntity = if (assistantContent.isBlank()) {
        assistantMessage.copy(
            content = EMPTY_RESPONSE_MESSAGE,
            status = MessageStatus.ERROR.name
        )
    } else {
        assistantMessage.copy(status = MessageStatus.SUCCESS.name)
    }

    private data class ResolvedRuntimeConfig(
        val providerType: ProviderType,
        val model: String,
        val streamEnabled: Boolean
    )

    private companion object {
        const val EMPTY_RESPONSE_MESSAGE = "No response received from model"
    }
}
