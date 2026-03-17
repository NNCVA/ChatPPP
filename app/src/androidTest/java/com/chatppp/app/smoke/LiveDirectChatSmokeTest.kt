package com.chatppp.app.smoke

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chatppp.app.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveDirectChatSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun direct_mode_can_send_real_streaming_message_and_restore_history() {
        val arguments = InstrumentationRegistry.getArguments()
        val baseUrl = arguments.getString("liveBaseUrl").orEmpty()
        val model = arguments.getString("liveModel").orEmpty()
        val apiKey = arguments.getString("liveApiKey").orEmpty()

        assumeTrue(baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank())

        val verificationToken = "SMOKE-314159"
        val prompt = "Reply with exactly $verificationToken."

        composeRule.onNodeWithText("ChatPPP").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithTag("base-url-input").performTextReplacement(baseUrl)
        composeRule.onNodeWithTag("model-input").performTextReplacement(model)
        composeRule.onNodeWithTag("direct-api-key-input").performTextReplacement(apiKey)
        composeRule.onNodeWithContentDescription("Navigate up").performClick()
        composeRule.onNodeWithText("ChatPPP").assertIsDisplayed()

        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement(prompt)
        composeRule.onNodeWithContentDescription("Send message").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithText(verificationToken, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot(useUnmergedTree = true).printToLog("LiveSmokeAfterUserSend")

        composeRule.waitUntil(timeoutMillis = 60_000) {
            val matchingMessages = composeRule
                .onAllNodesWithText(verificationToken, substring = true)
                .fetchSemanticsNodes().size
            val isStreaming = composeRule
                .onAllNodesWithContentDescription("Streaming response")
                .fetchSemanticsNodes().isNotEmpty()
            matchingMessages >= 2 && !isStreaming
        }

        composeRule.onAllNodesWithText(verificationToken, substring = true)[1].assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(verificationToken, substring = true)[0].assertIsDisplayed()
        composeRule.onAllNodesWithText(verificationToken, substring = true)[1].assertIsDisplayed()
    }
}
