# ChatPPP User Experience Improvements Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the highest-value user-facing improvements from the `2026-04-11` UX spec, focusing on setup guidance, safer conversation management, clearer chat recovery, and better day-to-day usability.

**Architecture:** Keep the existing `Compose + ViewModel + StateFlow + Repository` structure, and add the new behavior as small vertical slices. Prefer extending current screens and repository interfaces over adding large new layers, but introduce focused helpers where they reduce coupling or make validation/testability clearer.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Navigation Compose, AndroidX ViewModel, Room, DataStore, OkHttp, Coroutines, JUnit4, MockWebServer, Compose UI Test

---

**Target root:** repository root (`.`)

**Spec reference:** `docs/superpowers/specs/2026-04-11-user-experience-improvements.md`

## File Structure Map

### Existing files to modify

- Modify: `app/src/main/java/com/chatppp/app/navigation/ChatPppNavGraph.kt`
- Modify: `app/src/main/java/com/chatppp/app/domain/repository/ChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/db/ConversationDao.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/remote/model/ChatRequestDto.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatAction.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatRoute.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/components/ComposerBar.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/common/UiMessage.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListRoute.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsRoute.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Modify: `docs/help/apikey.md`

### New files to create

- Create: `app/src/main/java/com/chatppp/app/domain/model/ConversationPreview.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/ConnectionTestService.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/SetupRequiredCard.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageActionMenu.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListEffect.kt`
- Create: `docs/help/provider-templates.md`

### Tests to create or extend

- Modify: `app/src/test/java/com/chatppp/app/data/remote/provider/ProviderSelectorTest.kt`
- Create: `app/src/test/java/com/chatppp/app/data/remote/provider/ConnectionTestServiceTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/conversations/ConversationListViewModelTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/ui/settings/SettingsScreenTest.kt`
- Create: `app/src/androidTest/java/com/chatppp/app/ui/conversations/ConversationListScreenTest.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/navigation/ChatPppNavigationTest.kt`

## Chunk 1: Setup Readiness And First Success

### Task 1: Add shared configuration validation state for settings

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Test: `app/src/test/java/com/chatppp/app/data/remote/provider/ProviderSelectorTest.kt`
- Test: `app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt`
- Test: `app/src/androidTest/java/com/chatppp/app/ui/settings/SettingsScreenTest.kt`

- [ ] **Step 1: Write the failing tests for validation state**

```kotlin
@Test
fun invalid_chat_completions_suffix_is_exposed_as_inline_validation() = runTest {
    viewModel.updateBaseUrl("https://api.openai.com/v1/chat/completions")
    advanceUntilIdle()

    assertEquals(
        "OpenAI Base URL must not include /chat/completions",
        viewModel.uiState.value.baseUrlError
    )
}
```

- [ ] **Step 2: Run the targeted tests and confirm they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.data.remote.provider.ProviderSelectorTest" --tests "com.chatppp.app.ui.settings.SettingsViewModelTest"`

Expected: FAIL because `SettingsUiState` does not yet expose validation fields and the current settings flow only fails after message send.

- [ ] **Step 3: Implement minimal validation state**

Add explicit UI state fields such as:

```kotlin
val baseUrlError: String? = null
val modelError: String? = null
val credentialError: String? = null
val readinessLabel: String = "Not ready"
```

Use the same validation rules as runtime request building so settings and send-time behavior stay aligned.

- [ ] **Step 4: Run unit and UI tests until they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.data.remote.provider.ProviderSelectorTest" --tests "com.chatppp.app.ui.settings.SettingsViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.settings.SettingsScreenTest"`

Expected: PASS, with settings screen showing inline validation copy and a visible readiness state.

- [ ] **Step 5: Commit the validation slice**

```bash
git add app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt app/src/test/java/com/chatppp/app/data/remote/provider/ProviderSelectorTest.kt app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/settings/SettingsScreenTest.kt
git commit -m "feat: add inline runtime configuration validation"
```

### Task 2: Add setup-required empty state and route users to settings before first failure

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/navigation/ChatPppNavGraph.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatRoute.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/SetupRequiredCard.kt`
- Test: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`
- Test: `app/src/androidTest/java/com/chatppp/app/navigation/ChatPppNavigationTest.kt`

- [ ] **Step 1: Write the failing UI tests for the empty-state CTA**

```kotlin
@Test
fun incomplete_setup_shows_open_settings_cta() {
    composeRule.onNodeWithText("Finish setup to start chatting").assertIsDisplayed()
    composeRule.onNodeWithText("Open settings").assertIsDisplayed()
}
```

- [ ] **Step 2: Run the targeted instrumentation tests and confirm failure**

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`

Expected: FAIL because the empty state currently only shows `Start a conversation`.

- [ ] **Step 3: Implement chat readiness plumbing**

Expose setup readiness in `ChatViewModel`, then render one of two empty states:

```kotlin
if (state.requiresSetup) {
    SetupRequiredCard(
        readinessLabel = state.readinessLabel,
        onOpenSettings = onOpenSettings
    )
} else {
    DefaultEmptyConversationState()
}
```

Keep the implementation minimal: no wizard yet, just a clear CTA and pre-send guidance.

- [ ] **Step 4: Re-run the instrumentation coverage**

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.navigation.ChatPppNavigationTest"`

Expected: PASS, including a test that tapping the CTA opens the settings route.

- [ ] **Step 5: Commit the first-success entry point**

```bash
git add app/src/main/java/com/chatppp/app/navigation/ChatPppNavGraph.kt app/src/main/java/com/chatppp/app/ui/chat/ChatRoute.kt app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt app/src/main/java/com/chatppp/app/ui/chat/components/SetupRequiredCard.kt app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt app/src/androidTest/java/com/chatppp/app/navigation/ChatPppNavigationTest.kt
git commit -m "feat: guide incomplete chat setup from empty state"
```

### Task 3: Add a dedicated test-connection action in settings

**Files:**
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/ConnectionTestService.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/remote/model/ChatRequestDto.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Create: `app/src/test/java/com/chatppp/app/data/remote/provider/ConnectionTestServiceTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/ui/settings/SettingsScreenTest.kt`

- [ ] **Step 1: Write failing tests for connection-test states**

```kotlin
@Test
fun connection_test_success_updates_status_chip() = runTest {
    viewModel.runConnectionTest()
    advanceUntilIdle()

    assertEquals("Ready", viewModel.uiState.value.connectionStatusLabel)
}
```

- [ ] **Step 2: Run the new test target and confirm it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.data.remote.provider.ConnectionTestServiceTest" --tests "com.chatppp.app.ui.settings.SettingsViewModelTest"`

Expected: FAIL because there is no connection-test service or UI state for the result.

- [ ] **Step 3: Implement a minimal non-stream connection probe**

Use a tiny non-stream request and explicit status mapping:

```kotlin
sealed interface ConnectionTestResult {
    data object Success : ConnectionTestResult
    data class Failure(val message: String) : ConnectionTestResult
}
```

If a tiny request requires adding an optional `maxTokens` field to `ChatRequestDto`, keep the DTO change additive and default-safe.

- [ ] **Step 4: Run the test suite for settings connection coverage**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.data.remote.provider.ConnectionTestServiceTest" --tests "com.chatppp.app.ui.settings.SettingsViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.settings.SettingsScreenTest"`

Expected: PASS, including visible `Testing`, `Ready`, and failure states in settings.

- [ ] **Step 5: Commit the connection-test slice**

```bash
git add app/src/main/java/com/chatppp/app/data/remote/model/ChatRequestDto.kt app/src/main/java/com/chatppp/app/data/remote/provider/ConnectionTestService.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt app/src/test/java/com/chatppp/app/data/remote/provider/ConnectionTestServiceTest.kt app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/settings/SettingsScreenTest.kt
git commit -m "feat: add settings connection test flow"
```

## Chunk 2: Conversation Management That Scales

### Task 4: Add conversation rename support and automatic first-message titles

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/domain/repository/ChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/db/ConversationDao.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListViewModel.kt`
- Modify: `app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/conversations/ConversationListViewModelTest.kt`

- [ ] **Step 1: Write failing tests for automatic title upgrades**

```kotlin
@Test
fun first_user_message_replaces_default_new_chat_title() = runTest {
    repository.sendMessage(conversationId, "Summarize the latest Compose changes")

    assertEquals("Summarize the latest Compose changes", conversation.title)
}
```

- [ ] **Step 2: Run the repository and view-model tests to confirm failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.data.repository.DefaultChatRepositoryTest" --tests "com.chatppp.app.ui.conversations.ConversationListViewModelTest"`

Expected: FAIL because the repository never updates titles after creation and has no rename API.

- [ ] **Step 3: Implement title updates with minimal schema impact**

Add one DAO update and one repository method:

```kotlin
@Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
suspend fun updateTitle(conversationId: String, title: String, updatedAt: Long)
```

Only auto-title conversations that still have the default placeholder title. Keep truncation deterministic and testable.

- [ ] **Step 4: Re-run targeted tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.data.repository.DefaultChatRepositoryTest" --tests "com.chatppp.app.ui.conversations.ConversationListViewModelTest"`

Expected: PASS for both auto-title and manual rename coverage.

- [ ] **Step 5: Commit the title-management slice**

```bash
git add app/src/main/java/com/chatppp/app/domain/repository/ChatRepository.kt app/src/main/java/com/chatppp/app/data/local/db/ConversationDao.kt app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt app/src/main/java/com/chatppp/app/ui/conversations/ConversationListViewModel.kt app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt app/src/test/java/com/chatppp/app/ui/conversations/ConversationListViewModelTest.kt
git commit -m "feat: support conversation auto titles and rename"
```

### Task 5: Upgrade conversation list rows with preview, time, undo delete, and new-chat auto-open

**Files:**
- Create: `app/src/main/java/com/chatppp/app/domain/model/ConversationPreview.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListEffect.kt`
- Modify: `app/src/main/java/com/chatppp/app/domain/repository/ChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/db/ConversationDao.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListRoute.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListViewModel.kt`
- Create: `app/src/androidTest/java/com/chatppp/app/ui/conversations/ConversationListScreenTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/conversations/ConversationListViewModelTest.kt`

- [ ] **Step 1: Write the failing list-UX tests**

```kotlin
@Test
fun creating_conversation_emits_open_new_chat_effect() = runTest {
    viewModel.createConversation()
    advanceUntilIdle()

    assertEquals("conversation-1", viewModel.effects.first())
}
```

```kotlin
@Test
fun deleted_conversation_shows_undo_affordance() {
    composeRule.onNodeWithText("Undo").assertIsDisplayed()
}
```

- [ ] **Step 2: Run the targeted tests to verify failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.ui.conversations.ConversationListViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.conversations.ConversationListScreenTest"`

Expected: FAIL because there is no effect stream, no undo flow, and no richer list metadata.

- [ ] **Step 3: Implement conversation-preview and effect plumbing**

Introduce a preview model instead of overloading the domain `Conversation` object:

```kotlin
data class ConversationPreview(
    val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val relativeUpdatedAt: String,
    val providerType: ProviderType
)
```

Keep undo logic route-driven: the view model emits a deletion effect, and the route hosts the `Snackbar`.

- [ ] **Step 4: Re-run unit and instrumentation coverage**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.ui.conversations.ConversationListViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.conversations.ConversationListScreenTest"`

Expected: PASS, including new-chat auto-open behavior and undo visibility.

- [ ] **Step 5: Commit the conversation-list usability slice**

```bash
git add app/src/main/java/com/chatppp/app/domain/model/ConversationPreview.kt app/src/main/java/com/chatppp/app/ui/conversations/ConversationListEffect.kt app/src/main/java/com/chatppp/app/domain/repository/ChatRepository.kt app/src/main/java/com/chatppp/app/data/local/db/ConversationDao.kt app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt app/src/main/java/com/chatppp/app/ui/conversations/ConversationListRoute.kt app/src/main/java/com/chatppp/app/ui/conversations/ConversationListScreen.kt app/src/main/java/com/chatppp/app/ui/conversations/ConversationListUiState.kt app/src/main/java/com/chatppp/app/ui/conversations/ConversationListViewModel.kt app/src/test/java/com/chatppp/app/ui/conversations/ConversationListViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/conversations/ConversationListScreenTest.kt
git commit -m "feat: improve conversation list usability"
```

## Chunk 3: Clearer Chat Recovery And Better Message Actions

### Task 6: Make failure recovery explicit with retry and open-settings actions

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatAction.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt`

- [ ] **Step 1: Write failing tests for visible recovery controls**

```kotlin
@Test
fun configuration_error_shows_open_settings_action() {
    composeRule.onNodeWithText("Open settings").assertIsDisplayed()
    composeRule.onNodeWithText("Retry").assertIsDisplayed()
}
```

- [ ] **Step 2: Run the targeted tests and capture the failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.ui.chat.ChatViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`

Expected: FAIL because error rows currently expose only a small retry icon with no settings shortcut.

- [ ] **Step 3: Implement error classification and visible actions**

Model user-facing recovery hints explicitly:

```kotlin
val recoveryActionLabel: String? = "Open settings"
val canRetry: Boolean = true
```

Only show `Open settings` for config/auth classes of failure. Keep network and blank-response cases retryable without adding noise.

- [ ] **Step 4: Re-run chat unit and instrumentation tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.ui.chat.ChatViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`

Expected: PASS, with a visible recovery path for setup-related failures.

- [ ] **Step 5: Commit the recovery-action slice**

```bash
git add app/src/main/java/com/chatppp/app/ui/chat/ChatAction.kt app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt
git commit -m "feat: add explicit chat recovery actions"
```

### Task 7: Add copy and edit-resend message actions

**Files:**
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageActionMenu.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/common/UiMessage.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatAction.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt`

- [ ] **Step 1: Write the failing tests for copy and edit-resend**

```kotlin
@Test
fun edit_resend_prefills_composer_with_selected_user_message() = runTest {
    viewModel.onAction(ChatAction.EditMessage("user-1"))

    assertEquals("Original prompt", viewModel.uiState.value.inputText)
}
```

- [ ] **Step 2: Run the targeted tests and confirm the current gap**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.ui.chat.ChatViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`

Expected: FAIL because the action types and menu UI do not exist yet.

- [ ] **Step 3: Implement the smallest useful action set**

Start with these actions only:

```kotlin
data class EditMessage(val messageId: String) : ChatAction
data class CopyMessage(val messageId: String) : ChatAction
```

Use Compose clipboard integration in the screen layer, not in the view model.

- [ ] **Step 4: Re-run targeted tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.ui.chat.ChatViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`

Expected: PASS for both user-message edit-resend and assistant-message copy affordances.

- [ ] **Step 5: Commit the message-action slice**

```bash
git add app/src/main/java/com/chatppp/app/ui/chat/components/MessageActionMenu.kt app/src/main/java/com/chatppp/app/ui/common/UiMessage.kt app/src/main/java/com/chatppp/app/ui/chat/ChatAction.kt app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt
git commit -m "feat: add copy and edit-resend chat actions"
```

### Task 8: Surface request phase, active preset, and compression visibility

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`

- [ ] **Step 1: Write failing tests for richer status copy**

```kotlin
@Test
fun active_preset_and_request_phase_are_exposed_in_ui_state() = runTest {
    assertEquals("Preset: Work Direct", viewModel.uiState.value.selectedPresetLabel)
    assertEquals("Connecting", viewModel.uiState.value.requestPhaseLabel)
}
```

- [ ] **Step 2: Run the targeted tests to confirm failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.ui.chat.ChatViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`

Expected: FAIL because the UI only distinguishes generic streaming and hidden preset state.

- [ ] **Step 3: Implement minimal status signals**

Add only the smallest user-visible states needed for clarity:

```kotlin
val requestPhaseLabel: String? = null
val compressionNotice: String? = null
val selectedPresetLabel: String? = null
```

Do not add a full token accounting dashboard in this slice. A simple compression notice is enough.

- [ ] **Step 4: Re-run the targeted tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.chatppp.app.ui.chat.ChatViewModelTest"`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`

Expected: PASS, with clearer request lifecycle messaging and visible preset context.

- [ ] **Step 5: Commit the chat-clarity slice**

```bash
git add app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt
git commit -m "feat: expose preset and request status context"
```

## Chunk 4: Trust And Help Content

### Task 9: Replace real-looking credential docs with safe help content and provider templates

**Files:**
- Modify: `docs/help/apikey.md`
- Create: `docs/help/provider-templates.md`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsRoute.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/ui/settings/SettingsScreenTest.kt`

- [ ] **Step 1: Write the failing doc and UI checks**

Use a simple repository check for secrets-in-docs and one UI test for template visibility:

```bash
rg "sk-|ms-" docs/help
```

```kotlin
composeRule.onNodeWithText("Provider templates").assertIsDisplayed()
```

- [ ] **Step 2: Run the checks and confirm failure**

Run: `rg "sk-|ms-" docs/help`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.settings.SettingsScreenTest"`

Expected: `rg` should find active-looking secrets in `docs/help/apikey.md`, and the settings screen should not yet show template/help content.

- [ ] **Step 3: Implement safe docs and lightweight in-app help entry points**

Change help content to placeholders and examples only:

```markdown
url: https://api.openai.com/v1
key: <your-api-key>
```

Keep provider templates simple: prefill base URL and model suggestions without storing secrets.

- [ ] **Step 4: Re-run the checks**

Run: `rg "sk-|ms-" docs/help`

Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.settings.SettingsScreenTest"`

Expected: `rg` returns no matches, and the settings template/help UI renders successfully.

- [ ] **Step 5: Commit the trust-and-help slice**

```bash
git add docs/help/apikey.md docs/help/provider-templates.md app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsRoute.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt app/src/androidTest/java/com/chatppp/app/ui/settings/SettingsScreenTest.kt
git commit -m "docs: replace credential examples with safe provider help"
```

## Final Verification Pass

- [ ] Run: `.\gradlew.bat testDebugUnitTest`
- [ ] Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest"`
- [ ] Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.settings.SettingsScreenTest"`
- [ ] Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.conversations.ConversationListScreenTest"`
- [ ] Run: `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.navigation.ChatPppNavigationTest"`
- [ ] Run: `rg "sk-|ms-" docs/help`

Expected:

- Unit tests pass.
- The targeted UI tests pass on a connected emulator/device.
- Help docs contain no active-looking secrets.

## Notes For The Implementer

- Keep `P0` and `P1` within the current app scope. Do not add files, image chat, or backend relay work in this plan.
- Reuse existing screen-level view models unless a slice becomes too tangled to test cleanly.
- Avoid broad visual redesign in the same commits as behavior changes.
- Prefer small, reviewable commits that match the tasks above.

Plan complete and saved to `docs/superpowers/plans/2026-04-11-user-experience-improvements.md`. Ready to execute?
