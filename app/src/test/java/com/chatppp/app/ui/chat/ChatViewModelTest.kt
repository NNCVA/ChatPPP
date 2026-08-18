package com.chatppp.app.ui.chat

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chatppp.app.data.local.preferences.AppPreferences
import com.chatppp.app.data.local.secrets.SecretStore
import com.chatppp.app.data.presets.ConfigPresetStore
import com.chatppp.app.data.remote.provider.ChatProvider
import com.chatppp.app.data.remote.provider.ProviderSelector
import com.chatppp.app.data.remote.provider.SettingsValidationResult
import com.chatppp.app.domain.model.ConfigPreset
import com.chatppp.app.domain.model.Conversation
import com.chatppp.app.domain.model.Message
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import com.chatppp.app.domain.model.ProviderType
import com.chatppp.app.domain.repository.ChatRepository
import com.chatppp.app.domain.session.LastConversationStore
import com.chatppp.app.ui.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun send_message_ignores_blank_input() = runTest {
        val repository = FakeChatRepository()
        val lastConversationStore = FakeLastConversationStore()
        val viewModel = ChatViewModel(
            repository = repository,
            lastConversationStore = lastConversationStore,
            configPresetStore = FakeChatConfigPresetStore(),
            providerSelector = FakeProviderSelector(),
            appPreferences = createAppPreferences(),
            secretStore = FakeSecretStore()
        )

        viewModel.onAction(ChatAction.UpdateInput("   "))
        viewModel.onAction(ChatAction.SendMessage)
        advanceUntilIdle()

        assertTrue(repository.sentInputs.isEmpty())
        assertEquals("   ", viewModel.uiState.value.inputText)
    }

    @Test
    fun send_message_clears_input_and_delegates_to_repository() = runTest {
        val repository = FakeChatRepository()
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        repository.conversations.value = listOf(
            testConversation(id = "conversation-1", updatedAt = 1L)
        )
        val viewModel = ChatViewModel(
            repository = repository,
            lastConversationStore = lastConversationStore,
            configPresetStore = FakeChatConfigPresetStore(),
            providerSelector = FakeProviderSelector(),
            appPreferences = createAppPreferences(),
            secretStore = FakeSecretStore()
        )

        viewModel.onAction(ChatAction.UpdateInput("Hello"))
        viewModel.onAction(ChatAction.SendMessage)
        advanceUntilIdle()

        assertEquals(listOf("conversation-1" to "Hello"), repository.sentInputs)
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isStreaming)
    }

    @Test
    fun first_send_creates_and_persists_new_conversation_when_none_is_restored() = runTest {
        val repository = FakeChatRepository()
        val lastConversationStore = FakeLastConversationStore()
        val viewModel = ChatViewModel(
            repository = repository,
            lastConversationStore = lastConversationStore,
            configPresetStore = FakeChatConfigPresetStore(),
            providerSelector = FakeProviderSelector(),
            appPreferences = createAppPreferences(),
            secretStore = FakeSecretStore()
        )

        viewModel.onAction(ChatAction.UpdateInput("Hello"))
        viewModel.onAction(ChatAction.SendMessage)
        advanceUntilIdle()

        assertEquals(listOf("New Chat"), repository.createdTitles)
        assertEquals(listOf("conversation-1" to "Hello"), repository.sentInputs)
        assertEquals("conversation-1", lastConversationStore.lastConversationId.value)
    }

    @Test
    fun observed_messages_are_exposed_in_ui_state() = runTest {
        val repository = FakeChatRepository()
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        repository.conversations.value = listOf(
            testConversation(id = "conversation-1", updatedAt = 1L)
        )
        val viewModel = ChatViewModel(
            repository = repository,
            lastConversationStore = lastConversationStore,
            configPresetStore = FakeChatConfigPresetStore(),
            providerSelector = FakeProviderSelector(),
            appPreferences = createAppPreferences(),
            secretStore = FakeSecretStore()
        )

        repository.messages.value = listOf(
            Message(
                id = "message-1",
                conversationId = "conversation-1",
                role = MessageRole.ASSISTANT,
                content = "Hi",
                status = MessageStatus.SUCCESS,
                createdAt = 1L
            )
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Hi", viewModel.uiState.value.messages.first().content)
    }

    @Test
    fun retry_message_replays_last_user_turn() = runTest {
        val repository = FakeChatRepository()
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        repository.conversations.value = listOf(
            testConversation(id = "conversation-1", updatedAt = 1L)
        )
        val viewModel = ChatViewModel(
            repository = repository,
            lastConversationStore = lastConversationStore,
            configPresetStore = FakeChatConfigPresetStore(),
            providerSelector = FakeProviderSelector(),
            appPreferences = createAppPreferences(),
            secretStore = FakeSecretStore()
        )

        repository.messages.value = listOf(
            Message(
                id = "message-1",
                conversationId = "conversation-1",
                role = MessageRole.USER,
                content = "Hello",
                status = MessageStatus.SUCCESS,
                createdAt = 1L
            ),
            Message(
                id = "message-2",
                conversationId = "conversation-1",
                role = MessageRole.ASSISTANT,
                content = "Something went wrong",
                status = MessageStatus.ERROR,
                createdAt = 2L
            )
        )
        advanceUntilIdle()

        viewModel.onAction(ChatAction.RetryMessage("message-2"))
        advanceUntilIdle()

        assertEquals(listOf("conversation-1" to "Hello"), repository.sentInputs)
    }

    @Test
    fun stop_generating_calls_repository_and_clears_streaming_state() = runTest {
        val repository = FakeChatRepository()
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        repository.conversations.value = listOf(
            testConversation(id = "conversation-1", updatedAt = 1L)
        )
        val viewModel = ChatViewModel(
            repository = repository,
            lastConversationStore = lastConversationStore,
            configPresetStore = FakeChatConfigPresetStore(),
            providerSelector = FakeProviderSelector(),
            appPreferences = createAppPreferences(),
            secretStore = FakeSecretStore()
        )

        repository.messages.value = listOf(
            Message(
                id = "message-1",
                conversationId = "conversation-1",
                role = MessageRole.ASSISTANT,
                content = "Typing",
                status = MessageStatus.STREAMING,
                createdAt = 1L
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isStreaming)
        viewModel.onAction(ChatAction.StopGenerating)
        advanceUntilIdle()

        assertEquals(1, repository.stopCalls)
        assertFalse(viewModel.uiState.value.isStreaming)
    }

    @Test
    fun selecting_preset_binds_it_to_current_conversation() = runTest {
        val repository = FakeChatRepository()
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        repository.conversations.value = listOf(
            testConversation(id = "conversation-1", updatedAt = 1L)
        )
        val configPresetStore = FakeChatConfigPresetStore(
            listOf(
                ConfigPreset(
                    id = "preset-1",
                    name = "Work Direct",
                    providerType = ProviderType.DIRECT,
                    baseUrl = "https://api.openai.com/v1",
                    model = "gpt-4.1-mini",
                    streamEnabled = true
                )
            )
        )
        val viewModel = ChatViewModel(
            repository = repository,
            lastConversationStore = lastConversationStore,
            configPresetStore = configPresetStore,
            providerSelector = FakeProviderSelector(),
            appPreferences = createAppPreferences(),
            secretStore = FakeSecretStore()
        )
        advanceUntilIdle()

        viewModel.onAction(ChatAction.SelectPreset("preset-1"))
        advanceUntilIdle()

        assertEquals(listOf("conversation-1" to "preset-1"), repository.boundPresets)
        assertEquals("preset-1", viewModel.uiState.value.selectedPresetId)
        assertEquals("Work Direct", viewModel.uiState.value.selectedPresetName)
    }

    @Test
    fun thinking_content_is_hidden_by_default_and_can_be_toggled() = runTest {
        val repository = FakeChatRepository()
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        repository.conversations.value = listOf(
            testConversation(id = "conversation-1", updatedAt = 1L)
        )
        val viewModel = ChatViewModel(
            repository = repository,
            lastConversationStore = lastConversationStore,
            configPresetStore = FakeChatConfigPresetStore(),
            providerSelector = FakeProviderSelector(),
            appPreferences = createAppPreferences(),
            secretStore = FakeSecretStore()
        )

        repository.messages.value = listOf(
            Message(
                id = "message-1",
                conversationId = "conversation-1",
                role = MessageRole.ASSISTANT,
                content = "Final answer",
                thinkingContent = "Internal reasoning",
                status = MessageStatus.SUCCESS,
                createdAt = 1L
            )
        )
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.messages.first().isThinkingExpanded)
        assertEquals("Internal reasoning", viewModel.uiState.value.messages.first().thinkingContent)

        viewModel.onAction(ChatAction.ToggleThinking("message-1"))
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.messages.first().isThinkingExpanded)
    }









    private fun createAppPreferences(): AppPreferences {
        val tempDirectory = Files.createTempDirectory("chat-viewmodel-test").toFile()
        val scope = CoroutineScope(SupervisorJob() + mainDispatcherRule.dispatcher)
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempDirectory.resolve("settings.preferences_pb") }
        )
        return AppPreferences(dataStore)
    }
}

private class FakeChatRepository : ChatRepository {
    val messages = MutableStateFlow<List<Message>>(emptyList())
    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val sentInputs = mutableListOf<Pair<String, String>>()
    val createdTitles = mutableListOf<String>()
    val boundPresets = mutableListOf<Pair<String, String?>>()
    var stopCalls = 0

    override fun observeConversations(): Flow<List<Conversation>> = conversations

    override suspend fun createConversation(title: String): String {
        createdTitles += title
        val id = "conversation-${createdTitles.size}"
        conversations.value = conversations.value + testConversation(
            id = id,
            title = title,
            updatedAt = createdTitles.size.toLong()
        )
        return id
    }

    override suspend fun deleteConversation(conversationId: String) = Unit

    override fun observeMessages(conversationId: String): Flow<List<Message>> = messages

    override suspend fun getLastMessage(conversationId: String): Message? =
        messages.value.lastOrNull()

    override suspend fun restoreConversation(conversation: Conversation, messages: List<Message>) = Unit

    override suspend fun updateConversationTitle(conversationId: String, title: String) {
        conversations.value = conversations.value.map { c ->
            if (c.id == conversationId) c.copy(title = title) else c
        }
    }

    override suspend fun renameConversation(conversationId: String, newTitle: String) {
        updateConversationTitle(conversationId, newTitle)
    }

    override suspend fun sendMessage(conversationId: String, userInput: String) {
        sentInputs += conversationId to userInput
    }

    override suspend fun bindPresetToConversation(conversationId: String, presetId: String?) {
        boundPresets += conversationId to presetId
        conversations.value = conversations.value.map { conversation ->
            if (conversation.id == conversationId) {
                conversation.copy(presetId = presetId)
            } else {
                conversation
            }
        }
    }

    override suspend fun stopStreaming(conversationId: String) {
        stopCalls += 1
    }
}

private class FakeLastConversationStore(
    initialConversationId: String? = null
) : LastConversationStore {
    override val lastConversationId = MutableStateFlow(initialConversationId)

    override suspend fun setLastConversationId(conversationId: String) {
        lastConversationId.value = conversationId
    }

    override suspend fun clearLastConversationId() {
        lastConversationId.value = null
    }
}

private fun testConversation(
    id: String,
    title: String = "New Chat",
    presetId: String? = null,
    updatedAt: Long
) = Conversation(
    id = id,
    title = title,
    providerType = ProviderType.DIRECT,
    presetId = presetId,
    createdAt = updatedAt,
    updatedAt = updatedAt
)

private class FakeChatConfigPresetStore(
    presets: List<ConfigPreset> = emptyList()
) : ConfigPresetStore {
    private val presetFlow = MutableStateFlow(presets)
    override val activePresetId = MutableStateFlow<String?>(null)

    override fun observePresets(): Flow<List<ConfigPreset>> = presetFlow

    override suspend fun getPreset(presetId: String): ConfigPreset? =
        presetFlow.value.firstOrNull { it.id == presetId }

    override suspend fun savePreset(preset: ConfigPreset) = Unit

    override suspend fun deletePreset(presetId: String) = Unit

    override suspend fun setActivePresetId(presetId: String?) {
        activePresetId.value = presetId
    }
}

private class FakeProviderSelector(
    private val validation: (ProviderType, String, String?, String?) -> SettingsValidationResult =
        { _, _, _, _ -> SettingsValidationResult.ready() }
) : ProviderSelector {
    override suspend fun select(providerType: ProviderType): ChatProvider {
        throw NotImplementedError("Not needed for tests")
    }

    override fun validate(
        providerType: ProviderType,
        baseUrl: String,
        directApiKey: String?,
        relayToken: String?
    ): SettingsValidationResult {
        return validation(providerType, baseUrl, directApiKey, relayToken)
    }
}

private class FakeSecretStore : SecretStore {
    private var directApiKey: String? = null
    private var relayToken: String? = null

    override fun getDirectApiKey(): String? = directApiKey

    override fun saveDirectApiKey(value: String) {
        directApiKey = value
    }

    override fun clearDirectApiKey() {
        directApiKey = null
    }

    override fun getRelayToken(): String? = relayToken

    override fun saveRelayToken(value: String) {
        relayToken = value
    }

    override fun clearRelayToken() {
        relayToken = null
    }
}
