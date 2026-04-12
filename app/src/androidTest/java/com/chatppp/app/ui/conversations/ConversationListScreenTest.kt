package com.chatppp.app.ui.conversations

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chatppp.app.domain.model.ConversationPreview
import com.chatppp.app.domain.model.ProviderType
import com.chatppp.app.ui.theme.ChatPppTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationListScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun empty_state_shows_placeholder() {
        composeRule.setContent {
            ChatPppTheme {
                ConversationListScreen(
                    state = ConversationListUiState(conversations = emptyList()),
                    effects = MutableSharedFlow(),
                    onBack = {},
                    onCreateConversation = {},
                    onDeleteConversation = {},
                    onConversationClick = {},
                    onUndoDelete = {},
                    onDismissUndo = {}
                )
            }
        }

        composeRule.onNodeWithText("No conversations yet").assertIsDisplayed()
        composeRule.onNodeWithText("Create a new chat to get started.").assertIsDisplayed()
    }

    @Test
    fun conversations_list_shows_items() {
        composeRule.setContent {
            ChatPppTheme {
                ConversationListScreen(
                    state = ConversationListUiState(
                        conversations = listOf(
                            ConversationPreview(
                                id = "conv-1",
                                title = "Test Chat",
                                lastMessagePreview = "Last message content",
                                relativeUpdatedAt = "5 min ago",
                                providerType = ProviderType.DIRECT
                            )
                        )
                    ),
                    effects = MutableSharedFlow(),
                    onBack = {},
                    onCreateConversation = {},
                    onDeleteConversation = {},
                    onConversationClick = {},
                    onUndoDelete = {},
                    onDismissUndo = {}
                )
            }
        }

        composeRule.onNodeWithText("Test Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Last message content").assertIsDisplayed()
        composeRule.onNodeWithText("5 min ago").assertIsDisplayed()
    }

    @Test
    fun new_conversation_fab_is_displayed() {
        composeRule.setContent {
            ChatPppTheme {
                ConversationListScreen(
                    state = ConversationListUiState(),
                    effects = MutableSharedFlow(),
                    onBack = {},
                    onCreateConversation = {},
                    onDeleteConversation = {},
                    onConversationClick = {},
                    onUndoDelete = {},
                    onDismissUndo = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("New conversation").assertIsDisplayed()
    }

    @Test
    fun back_button_navigates_up() {
        var backPressed = false
        composeRule.setContent {
            ChatPppTheme {
                ConversationListScreen(
                    state = ConversationListUiState(),
                    effects = MutableSharedFlow(),
                    onBack = { backPressed = true },
                    onCreateConversation = {},
                    onDeleteConversation = {},
                    onConversationClick = {},
                    onUndoDelete = {},
                    onDismissUndo = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Navigate up").assertIsDisplayed()
    }
}