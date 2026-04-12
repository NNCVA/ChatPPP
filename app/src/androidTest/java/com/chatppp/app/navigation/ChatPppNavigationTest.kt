package com.chatppp.app.navigation

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chatppp.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatPppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun can_navigate_from_chat_to_conversations_and_settings() {
        composeRule.onNodeWithText("ChatPPP").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Open conversations").performClick()
        composeRule.onNodeWithText("Conversations").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("ChatPPP").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun created_conversation_persists_after_activity_recreation() {
        composeRule.onNodeWithContentDescription("Open conversations").performClick()
        composeRule.onNodeWithContentDescription("New conversation").performClick()
        composeRule.onNodeWithText("ChatPPP").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("ChatPPP").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open conversations").performClick()
        composeRule.onNodeWithText("Conversations").assertIsDisplayed()
        composeRule.onNodeWithText("New Chat").assertIsDisplayed()
    }

    @Test
    fun settings_values_persist_after_activity_recreation() {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithTag("base-url-input").performTextReplacement("https://relay.example.com")
        composeRule.onNodeWithTag("model-input").performTextReplacement("deepseek-chat")

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag("base-url-input").assertTextContains("https://relay.example.com")
        composeRule.onNodeWithTag("model-input").assertTextContains("deepseek-chat")
    }
}
