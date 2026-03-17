package com.chatppp.app.ui.conversations

import com.chatppp.app.domain.model.Conversation
import com.chatppp.app.domain.model.Message
import com.chatppp.app.domain.model.ProviderType
import com.chatppp.app.domain.repository.ChatRepository
import com.chatppp.app.domain.session.LastConversationStore
import com.chatppp.app.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun create_conversation_inserts_a_new_record() = runTest {
        val repository = FakeConversationChatRepository()
        val lastConversationStore = FakeLastConversationStore()
        val viewModel = ConversationListViewModel(repository, lastConversationStore)

        viewModel.createConversation()
        advanceUntilIdle()

        assertEquals(1, repository.createdTitles.size)
        assertEquals(1, viewModel.uiState.value.conversations.size)
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
        val repository = FakeConversationChatRepository()
        val lastConversationStore = FakeLastConversationStore("conversation-1")
        val viewModel = ConversationListViewModel(repository, lastConversationStore)

        viewModel.deleteConversation("conversation-1")
        advanceUntilIdle()

        assertEquals(null, lastConversationStore.lastConversationId.value)
    }
}

private class FakeConversationChatRepository : ChatRepository {
    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val createdTitles = mutableListOf<String>()

    override fun observeConversations(): Flow<List<Conversation>> = conversations

    override suspend fun createConversation(title: String): String {
        createdTitles += title
        val created = testConversation(
            id = "conversation-${createdTitles.size}",
            title = title,
            updatedAt = createdTitles.size.toLong()
        )
        conversations.value = conversations.value + created
        return created.id
    }

    override suspend fun deleteConversation(conversationId: String) = Unit

    override fun observeMessages(conversationId: String): Flow<List<Message>> = MutableStateFlow(emptyList())

    override suspend fun sendMessage(conversationId: String, userInput: String) = Unit

    override suspend fun bindPresetToConversation(conversationId: String, presetId: String?) = Unit

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
    updatedAt: Long
) = Conversation(
    id = id,
    title = title,
    providerType = ProviderType.DIRECT,
    createdAt = updatedAt,
    updatedAt = updatedAt
)
