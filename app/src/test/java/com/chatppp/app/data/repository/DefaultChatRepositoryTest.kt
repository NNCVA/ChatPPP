package com.chatppp.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.data.context.ConversationSummaryStore
import com.chatppp.app.data.local.db.ConversationDao
import com.chatppp.app.data.local.db.MessageDao
import com.chatppp.app.data.local.entity.ConversationEntity
import com.chatppp.app.data.local.entity.ConversationSummaryEntity
import com.chatppp.app.data.local.entity.MessageEntity
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.remote.model.ChatRequestDto
import com.chatppp.app.data.remote.provider.ChatProvider
import com.chatppp.app.data.remote.provider.ProviderSelector
import com.chatppp.app.domain.model.ChatChunk
import com.chatppp.app.domain.model.ChatError
import com.chatppp.app.domain.model.ConfigPreset
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import com.chatppp.app.domain.model.ProviderType
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultChatRepositoryTest {
    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    @Test
    fun send_message_stores_user_message_and_streams_into_placeholder() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = "https://api.example.com",
                model = "deepseek-chat",
                streamEnabled = true
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Test",
                providerType = ProviderType.DIRECT.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val directProvider = FakeChatProvider(
            chunks = listOf(
                ChatChunk.Content("Hel"),
                ChatChunk.Content("lo"),
                ChatChunk.Done
            ),
            onStreamStart = {
                val currentMessages = messageDao.snapshot("conversation-1")
                assertEquals(2, currentMessages.size)
                assertEquals("Hello", currentMessages[0].content)
                assertEquals(MessageStatus.STREAMING.name, currentMessages[1].status)
            }
        )

        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = directProvider,
                relayProvider = FakeChatProvider()
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage(
            conversationId = "conversation-1",
            userInput = "Hello"
        )

        val messages = repository.observeMessages("conversation-1").first()

        assertEquals(2, messages.size)
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals("Hello", messages[0].content)
        assertEquals(MessageStatus.SUCCESS, messages[0].status)
        assertEquals(MessageRole.ASSISTANT, messages[1].role)
        assertEquals("Hello", messages[1].content)
        assertEquals(MessageStatus.SUCCESS, messages[1].status)
        assertEquals("deepseek-chat", directProvider.requests.single().model)
    }

    @Test
    fun send_message_marks_assistant_message_as_error_when_provider_fails() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = "https://api.example.com",
                model = "deepseek-chat",
                streamEnabled = true
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Test",
                providerType = ProviderType.DIRECT.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = FakeChatProvider(error = ChatError.Auth()),
                relayProvider = FakeChatProvider()
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage(
            conversationId = "conversation-1",
            userInput = "Hello"
        )

        val messages = repository.observeMessages("conversation-1").first()

        assertEquals(2, messages.size)
        assertEquals(MessageStatus.ERROR, messages[1].status)
        assertEquals("Authentication failed for chat request", messages[1].content)
    }

    @Test
    fun send_message_maps_config_errors_into_readable_assistant_error_bubble() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = "",
                model = "deepseek-chat",
                streamEnabled = true
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Config",
                providerType = ProviderType.DIRECT.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = FakeChatProvider(),
                relayProvider = FakeChatProvider(),
                selectionError = ChatError.Config("Base URL is required before sending chat requests")
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage("conversation-1", "Hello")

        val messages = repository.observeMessages("conversation-1").first()

        assertEquals(2, messages.size)
        assertEquals(MessageStatus.ERROR, messages[1].status)
        assertEquals("Base URL is required before sending chat requests", messages[1].content)
    }

    @Test
    fun send_message_uses_relay_provider_when_provider_type_is_relay() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateProviderAndChatSettings(
                providerType = ProviderType.RELAY,
                baseUrl = "https://relay.example.com",
                model = "relay-model",
                streamEnabled = false
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Relay",
                providerType = ProviderType.RELAY.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val relayProvider = FakeChatProvider(response = "Relay reply")
        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = FakeChatProvider(response = "Direct reply"),
                relayProvider = relayProvider
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage("conversation-1", "Hello relay")

        val messages = repository.observeMessages("conversation-1").first()

        assertEquals("Relay reply", messages.last().content)
        assertEquals("relay-model", relayProvider.requests.single().model)
    }

    @Test
    fun stop_streaming_keeps_partial_assistant_content_and_marks_message_success() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = "https://api.example.com",
                model = "deepseek-chat",
                streamEnabled = true
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Stop test",
                providerType = ProviderType.DIRECT.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val blockingProvider = BlockingStreamChatProvider(
            firstChunk = "Hel"
        )

        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = blockingProvider,
                relayProvider = FakeChatProvider()
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val sendJob = launch {
            repository.sendMessage("conversation-1", "Hello")
        }

        blockingProvider.awaitStarted()
        val streamingMessages = repository.observeMessages("conversation-1").first()
        assertEquals("Hel", streamingMessages.last().content)
        assertEquals(MessageStatus.STREAMING, streamingMessages.last().status)

        repository.stopStreaming("conversation-1")
        withTimeout(1_000) {
            sendJob.join()
        }

        val finalMessages = repository.observeMessages("conversation-1").first()
        assertEquals("Hel", finalMessages.last().content)
        assertEquals(MessageStatus.SUCCESS, finalMessages.last().status)
        assertTrue(blockingProvider.wasCancelled)
    }

    @Test
    fun send_message_includes_stored_summary_only_after_context_exceeds_token_trigger_and_refreshes_conversation_timestamp() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = "https://api.example.com",
                model = "deepseek-chat",
                streamEnabled = false
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Summary test",
                providerType = ProviderType.DIRECT.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        val largeUser = "User message ".repeat(5000)
        val largeAssistant = "Assistant reply ".repeat(5000)
        messageDao.upsert(message("m1", "conversation-1", MessageRole.USER, largeUser, 1L))
        messageDao.upsert(message("m2", "conversation-1", MessageRole.ASSISTANT, largeAssistant, 2L))
        messageDao.upsert(message("m3", "conversation-1", MessageRole.USER, "Second user", 3L))
        messageDao.upsert(message("m4", "conversation-1", MessageRole.ASSISTANT, "Second reply", 4L))
        val directProvider = FakeChatProvider(response = "Final reply")

        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = directProvider,
                relayProvider = FakeChatProvider()
            ),
            conversationSummaryStore = FakeConversationSummaryStore(
                ConversationSummaryEntity(
                    conversationId = "conversation-1",
                    summaryText = "User prefers concise answers.",
                    coveredUntilMessageId = "m2",
                    coveredUntilCreatedAt = 2L,
                    updatedAt = 10L
                )
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage("conversation-1", "Latest user")

        assertEquals(1, directProvider.requests.size)
        val finalRequest = directProvider.requests.single()
        assertEquals(
            listOf(
                "system" to "Conversation summary:\nUser prefers concise answers.",
                "user" to "Second user",
                "assistant" to "Second reply",
                "user" to "Latest user"
            ),
            finalRequest.messages.map { it.role to it.content }
        )
        assertTrue(conversationDao.snapshot("conversation-1")!!.updatedAt > 1L)
    }

    @Test
    fun send_message_uses_conversation_bound_preset_even_when_global_settings_change() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateProviderAndChatSettings(
                providerType = ProviderType.DIRECT,
                baseUrl = "https://api.openai.com/v1",
                model = "global-direct-model",
                streamEnabled = false
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Preset bound",
                providerType = ProviderType.DIRECT.name,
                presetId = "relay-preset",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val relayProvider = FakeChatProvider(response = "Preset reply")
        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = FakeChatProvider(response = "Global reply"),
                relayProvider = relayProvider
            ),
            configPresetStore = FakeConfigPresetStore(
                activePresetId = null,
                presets = mapOf(
                    "relay-preset" to ConfigPreset(
                        id = "relay-preset",
                        name = "Relay preset",
                        providerType = ProviderType.RELAY,
                        baseUrl = "https://relay.example.com/v1",
                        model = "preset-relay-model",
                        streamEnabled = false,
                        relayToken = "relay-secret"
                    )
                )
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage("conversation-1", "Hello from preset")

        assertEquals("Preset reply", repository.observeMessages("conversation-1").first().last().content)
        assertEquals("preset-relay-model", relayProvider.requests.single().model)
    }

    @Test
    fun send_message_persists_thinking_content_separately_from_visible_answer() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = "https://api.example.com",
                model = "deepseek-chat",
                streamEnabled = true
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Thinking test",
                providerType = ProviderType.DIRECT.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val directProvider = FakeChatProvider(
            chunks = listOf(
                ChatChunk.Thinking("Reasoning"),
                ChatChunk.Content("Final"),
                ChatChunk.Done
            )
        )

        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = directProvider,
                relayProvider = FakeChatProvider()
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage("conversation-1", "Hello")

        val messages = repository.observeMessages("conversation-1").first()
        assertEquals("Final", messages.last().content)
        assertEquals("Reasoning", messages.last().thinkingContent)
    }

    @Test
    fun send_message_skips_summary_generation_when_context_is_below_token_trigger_even_with_many_short_messages() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateRuntimeSettings(
                baseUrl = "https://api.example.com",
                model = "deepseek-chat",
                streamEnabled = false,
                summaryCompressionEnabled = true
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Short context",
                providerType = ProviderType.DIRECT.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        repeat(4) { index ->
            val turn = index + 1
            messageDao.upsert(message("u$turn", "conversation-1", MessageRole.USER, "ok", (turn * 2 - 1).toLong()))
            messageDao.upsert(message("a$turn", "conversation-1", MessageRole.ASSISTANT, "ok", (turn * 2).toLong()))
        }
        val directProvider = FakeChatProvider(response = "Final reply")

        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = directProvider,
                relayProvider = FakeChatProvider()
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage("conversation-1", "go on")

        assertEquals(1, directProvider.requests.size)
        assertEquals(
            listOf("ok", "ok", "ok", "ok", "ok", "ok", "ok", "ok", "go on"),
            directProvider.requests.single().messages.map { it.content }
        )
    }

    @Test
    fun send_message_marks_blank_stream_response_as_error_instead_of_success() = runTest {
        val conversationDao = FakeConversationDao()
        val messageDao = FakeMessageDao()
        val preferences = createPreferences().also {
            it.updateChatSettings(
                baseUrl = "https://api.example.com",
                model = "deepseek-chat",
                streamEnabled = true
            )
        }

        conversationDao.upsert(
            ConversationEntity(
                id = "conversation-1",
                title = "Blank stream",
                providerType = ProviderType.DIRECT.name,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val directProvider = FakeChatProvider(
            chunks = listOf(ChatChunk.Done)
        )

        val repository = DefaultChatRepository(
            conversationDao = conversationDao,
            messageDao = messageDao,
            appPreferences = preferences,
            providerSelector = FakeProviderSelector(
                directProvider = directProvider,
                relayProvider = FakeChatProvider()
            ),
            idGenerator = fixedIds("user-1", "assistant-1"),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        repository.sendMessage("conversation-1", "Hello")

        val messages = repository.observeMessages("conversation-1").first()
        assertEquals(MessageStatus.ERROR, messages.last().status)
        assertEquals("No response received from model", messages.last().content)
    }

    private fun createPreferences(): AppPreferences {
        val directory = Files.createTempDirectory("chatppp-repository-test").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(directory, "app.preferences_pb") }
        )
        return AppPreferences(dataStore)
    }

    private fun fixedIds(vararg values: String): () -> String {
        val queue = ArrayDeque(values.toList())
        return { queue.removeFirst() }
    }

    private fun message(
        id: String,
        conversationId: String,
        role: MessageRole,
        content: String,
        createdAt: Long
    ) = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        content = content,
        status = MessageStatus.SUCCESS.name,
        createdAt = createdAt
    )
}

private class FakeConversationDao : ConversationDao {
    private val items = MutableStateFlow<List<ConversationEntity>>(emptyList())

    override fun observeAll(): Flow<List<ConversationEntity>> = items

    override suspend fun getById(conversationId: String): ConversationEntity? =
        items.value.firstOrNull { it.id == conversationId }

    override suspend fun upsert(conversation: ConversationEntity) {
        items.update { existing ->
            (existing.filterNot { it.id == conversation.id } + conversation)
                .sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun updateUpdatedAt(conversationId: String, updatedAt: Long) {
        items.update { existing ->
            existing.map { conversation ->
                if (conversation.id == conversationId) {
                    conversation.copy(updatedAt = updatedAt)
                } else {
                    conversation
                }
            }.sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun updatePresetBinding(
        conversationId: String,
        presetId: String?,
        providerType: String,
        updatedAt: Long
    ) {
        items.update { existing ->
            existing.map { conversation ->
                if (conversation.id == conversationId) {
                    conversation.copy(
                        presetId = presetId,
                        providerType = providerType,
                        updatedAt = updatedAt
                    )
                } else {
                    conversation
                }
            }.sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun deleteById(conversationId: String) {
        items.update { existing -> existing.filterNot { it.id == conversationId } }
    }

    fun snapshot(conversationId: String): ConversationEntity? =
        items.value.firstOrNull { it.id == conversationId }
}

private class FakeMessageDao : MessageDao {
    private val items = MutableStateFlow<List<MessageEntity>>(emptyList())

    override fun observeByConversationId(conversationId: String): Flow<List<MessageEntity>> =
        items.map { existing ->
            existing.filter { it.conversationId == conversationId }.sortedBy { it.createdAt }
        }

    override suspend fun upsert(message: MessageEntity) {
        items.update { existing ->
            (existing.filterNot { it.id == message.id } + message).sortedBy { it.createdAt }
        }
    }

    override suspend fun deleteByConversationId(conversationId: String) {
        items.update { existing -> existing.filterNot { it.conversationId == conversationId } }
    }

    fun snapshot(conversationId: String): List<MessageEntity> =
        runBlocking { observeByConversationId(conversationId).first() }
}

private class FakeChatProvider(
    private val response: String = "",
    private val chunks: List<ChatChunk> = emptyList(),
    private val error: Throwable? = null,
    private val onStreamStart: (() -> Unit)? = null
) : ChatProvider {
    val requests = mutableListOf<ChatRequestDto>()

    override suspend fun send(request: ChatRequestDto): String {
        requests += request
        error?.let { throw it }
        return response
    }

    override fun stream(request: ChatRequestDto): Flow<ChatChunk> = kotlinx.coroutines.flow.flow {
        requests += request
        onStreamStart?.invoke()
        error?.let { throw it }
        chunks.forEach { emit(it) }
    }
}

private class FakeProviderSelector(
    val directProvider: ChatProvider,
    private val relayProvider: ChatProvider,
    private val selectionError: Throwable? = null
) : ProviderSelector {
    override suspend fun select(providerType: ProviderType): ChatProvider {
        selectionError?.let { throw it }
        return when (providerType) {
            ProviderType.DIRECT -> directProvider
            ProviderType.RELAY -> relayProvider
        }
    }
}

private class FakeConfigPresetStore(
    override val activePresetId: kotlinx.coroutines.flow.Flow<String?>,
    private val presets: Map<String, ConfigPreset>
) : ConfigPresetStore {
    constructor(
        activePresetId: String?,
        presets: Map<String, ConfigPreset>
    ) : this(MutableStateFlow(activePresetId), presets)

    override fun observePresets(): Flow<List<ConfigPreset>> = MutableStateFlow(presets.values.toList())

    override suspend fun getPreset(presetId: String): ConfigPreset? = presets[presetId]

    override suspend fun savePreset(preset: ConfigPreset) = Unit

    override suspend fun deletePreset(presetId: String) = Unit

    override suspend fun setActivePresetId(presetId: String?) = Unit
}

private class FakeConversationSummaryStore(
    private var summary: ConversationSummaryEntity? = null
) : ConversationSummaryStore {
    override suspend fun get(conversationId: String): ConversationSummaryEntity? = summary

    override suspend fun upsert(summary: ConversationSummaryEntity) {
        this.summary = summary
    }

    override suspend fun delete(conversationId: String) {
        summary = null
    }
}

private class BlockingStreamChatProvider(
    private val firstChunk: String
) : ChatProvider {
    private val started = CompletableDeferred<Unit>()
    var wasCancelled = false
        private set

    override suspend fun send(request: ChatRequestDto): String = error("Not used in blocking stream test")

    override fun stream(request: ChatRequestDto): Flow<ChatChunk> = kotlinx.coroutines.flow.flow {
        started.complete(Unit)
        emit(ChatChunk.Content(firstChunk))
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            wasCancelled = true
        }
    }

    suspend fun awaitStarted() {
        started.await()
    }
}
