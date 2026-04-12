package com.chatppp.app.ui.conversations

import com.chatppp.app.domain.model.Conversation
import com.chatppp.app.domain.model.Message
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import com.chatppp.app.domain.model.ProviderType
import com.chatppp.app.domain.repository.ChatRepository
import com.chatppp.app.domain.session.LastConversationStore
import com.chatppp.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun conversations_are_exposed_in_descending_updated_order() = runTest {
        val repository = FakeConversationChatRepository().apply {
            conversations.value = listOf(
                testConversation(id = "old", updatedAt = 10L),
                testConversation(id = "new", updatedAt = 30L),
                testConversation(id = "mid", updatedAt = 20L)
            )
        }
        val lastConversationStore = FakeLastConversationStore()

        val viewModel = ConversationListViewModel(repository, lastConversationStore)
        advanceUntilIdle()

        assertEquals(
            listOf("new", "mid", "old"),
            viewModel.uiState.value.conversations.map { it.id }
        )
    }

    @Test
    fun undo_delete_restores_conversation() = runTest {
        val repository = FakeConversationChatRepository().apply {
            conversations.value = listOf(
                testConversation(id = "conv-1", title = "Restored Chat", updatedAt = 10L)
            )
            messagesByConversation["conv-1"] = listOf(
                Message(
                    id = "message-1",
                    conversationId = "conv-1",
                    role = MessageRole.USER,
                    content = "Prompt before delete",
                    status = MessageStatus.SUCCESS,
                    createdAt = 11L
                )
            )
        }
        val lastConversationStore = FakeLastConversationStore()
        val viewModel = ConversationListViewModel(repository, lastConversationStore)
        advanceUntilIdle()

        viewModel.deleteConversation("conv-1")
        advanceUntilIdle()

        assertEquals(0, repository.conversations.value.size)

        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals(1, repository.conversations.value.size)
        assertEquals("Restored Chat", repository.conversations.value.first().title)
        assertEquals(1, repository.restoredMessages["conv-1"]?.size)
        assertEquals("Prompt before delete", repository.restoredMessages["conv-1"]?.first()?.content)
        assertNull(viewModel.uiState.value.lastDeletedConversation)
    }

    @Test
    fun rename_conversation_calls_repository() = runTest {
        val repository = FakeConversationChatRepository()
        val lastConversationStore = FakeLastConversationStore()
        val viewModel = ConversationListViewModel(repository, lastConversationStore)
        advanceUntilIdle()

        viewModel.renameConversation("conv-1", "New Title")
        advanceUntilIdle()

        assertEquals("New Title", repository.renamedTitles["conv-1"])
    }

    @Test
    fun selecting_conversation_persists_last_conversation_id() = runTest {
        val repository = FakeConversationChatRepository()
        val lastConversationStore = FakeLastConversationStore()
        val viewModel = ConversationListViewModel(repository, lastConversationStore)

        viewModel.selectConversation("conversation-9")
        advanceUntilIdle()

        assertEquals("conversation-9", lastConversationStore.lastConversationId.value)
    }

    @Test
    fun deleting_selected_conversation_clears_last_conversation_id() = runTest {
        val repository = FakeConversationChatRepository().apply {
            conversations.value = listOf(
                testConversation(id = "conversation-1", updatedAt = 10L)
            )
        }
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        val viewModel = ConversationListViewModel(repository, lastConversationStore)
        advanceUntilIdle()

        viewModel.deleteConversation("conversation-1")
        advanceUntilIdle()

        assertEquals(null, lastConversationStore.lastConversationId.value)
    }

    @Test
    fun undo_delete_restores_last_selected_conversation() = runTest {
        val repository = FakeConversationChatRepository().apply {
            conversations.value = listOf(
                testConversation(id = "conversation-1", title = "Restored Chat", updatedAt = 10L)
            )
        }
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        val viewModel = ConversationListViewModel(repository, lastConversationStore)
        advanceUntilIdle()

        viewModel.deleteConversation("conversation-1")
        advanceUntilIdle()
        assertEquals(null, lastConversationStore.lastConversationId.value)

        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals("conversation-1", lastConversationStore.lastConversationId.value)
    }
}

private class FakeConversationChatRepository : ChatRepository {
    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val createdTitles = mutableListOf<String>()
    val renamedTitles = mutableMapOf<String, String>()
    val restoredConversations = mutableListOf<Conversation>()
    val restoredMessages = mutableMapOf<String, List<Message>>()
    val messagesByConversation = mutableMapOf<String, List<Message>>()
    val effects = ArrayDeque<ConversationListEffect>()

    override fun observeConversations(): Flow<List<Conversation>> = conversations

    override suspend fun createConversation(title: String): String {
        createdTitles += title
        val created = testConversation(
            id = "conversation-${createdTitles.size}",
            title = title,
            updatedAt = createdTitles.size.toLong()
        )
        conversations.value = conversations.value + created
        effects.add(ConversationListEffect.OpenConversation(created.id))
        return created.id
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversations.value = conversations.value.filterNot { it.id == conversationId }
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        MutableStateFlow(messagesByConversation[conversationId].orEmpty())

    override suspend fun getLastMessage(conversationId: String): Message? =
        messagesByConversation[conversationId]?.lastOrNull()

    override suspend fun sendMessage(conversationId: String, userInput: String) = Unit

    override suspend fun bindPresetToConversation(conversationId: String, presetId: String?) = Unit

    override suspend fun updateConversationTitle(conversationId: String, title: String) = Unit

    override suspend fun renameConversation(conversationId: String, newTitle: String) {
        renamedTitles[conversationId] = newTitle
        conversations.value = conversations.value.map {
            if (it.id == conversationId) it.copy(title = newTitle) else it
        }
    }

    override suspend fun restoreConversation(conversation: Conversation, messages: List<Message>) {
        restoredConversations += conversation
        restoredMessages[conversation.id] = messages
        messagesByConversation[conversation.id] = messages
        conversations.value = conversations.value + conversation
    }

    override suspend fun stopStreaming(conversationId: String) = Unit
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
    providerType: ProviderType = ProviderType.DIRECT,
    presetId: String? = null,
    createdAt: Long = 0L,
    updatedAt: Long
) = Conversation(
    id = id,
    title = title,
    providerType = providerType,
    presetId = presetId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
