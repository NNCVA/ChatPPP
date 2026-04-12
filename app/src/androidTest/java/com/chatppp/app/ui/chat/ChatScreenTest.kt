package com.chatppp.app.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chatppp.app.domain.model.MessageRole
import com.chatppp.app.domain.model.MessageStatus
import com.chatppp.app.ui.common.UiMessage
import com.chatppp.app.ui.theme.ChatPppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun user_message_bubble_renders() {
        composeRule.setContent {
            ChatPppTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            UiMessage(
                                id = "user-1",
                                role = MessageRole.USER,
                                content = "Hello ChatPPP",
                                status = MessageStatus.SUCCESS
                            )
                        )
                    ),
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithText("Hello ChatPPP").assertIsDisplayed()
    }

    @Test
    fun assistant_streaming_row_renders() {
        composeRule.setContent {
            ChatPppTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            UiMessage(
                                id = "assistant-1",
                                role = MessageRole.ASSISTANT,
                                content = "Thinking...",
                                status = MessageStatus.STREAMING
                            )
                        ),
                        isStreaming = true
                    ),
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithText("Thinking...").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Streaming response").assertIsDisplayed()
    }

    @Test
    fun send_button_disables_for_blank_input() {
        composeRule.setContent {
            ChatPppTheme {
                ChatScreen(
                    state = ChatUiState(inputText = "   "),
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Send message").assertIsNotEnabled()
    }

    @Test
    fun retry_action_is_visible_on_error_state() {
        composeRule.setContent {
            ChatPppTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            UiMessage(
                                id = "assistant-2",
                                role = MessageRole.ASSISTANT,
                                content = "Request failed",
                                status = MessageStatus.ERROR
                            )
                        )
                    ),
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun config_error_shows_open_settings_without_retry() {
        composeRule.setContent {
            ChatPppTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            UiMessage(
                                id = "assistant-config-error",
                                role = MessageRole.ASSISTANT,
                                content = "Direct mode requires an API key",
                                status = MessageStatus.ERROR,
                                recoveryActionType = RecoveryActionType.OPEN_SETTINGS
                            )
                        )
                    ),
                    onAction = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithText("Open settings").assertIsDisplayed()
        composeRule.onAllNodesWithText("Retry").assertCountEquals(0)
    }

    @Test
    fun empty_conversation_placeholder_is_visible() {
        composeRule.setContent {
            ChatPppTheme {
                ChatScreen(
                    state = ChatUiState(requiresSetup = false),
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithText("Start a conversation").assertIsDisplayed()
        composeRule.onNodeWithText("Your replies will appear here once you send a message.").assertIsDisplayed()
    }

    @Test
    fun thinking_content_is_hidden_by_default_and_shown_after_toggle() {
        composeRule.setContent {
            ChatPppTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            UiMessage(
                                id = "assistant-thinking",
                                role = MessageRole.ASSISTANT,
                                content = "Final answer",
                                thinkingContent = "Internal reasoning",
                                isThinkingExpanded = false,
                                status = MessageStatus.SUCCESS
                            )
                        )
                    ),
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithText("Final answer").assertIsDisplayed()
        composeRule.onNodeWithText("Show thinking").assertIsDisplayed()
    }
}
