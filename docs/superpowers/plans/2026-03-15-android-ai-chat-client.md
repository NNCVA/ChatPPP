# Android AI Chat Client Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the existing `ChatPPP` Compose template into a native Android AI chat client with conversation persistence, streamed responses, and switchable direct or relay provider modes.

**Architecture:** Keep a single-app Android project rooted at the repository root (`.`). Build the app with `Compose + ViewModel + StateFlow + Repository + Provider abstraction`, persisting conversations locally with Room and storing runtime configuration separately from secrets.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Navigation Compose, AndroidX ViewModel, Hilt, Room, DataStore, OkHttp, kotlinx.serialization, Coroutines, JUnit4, Turbine, MockWebServer, Compose UI Test

---

**Target root:** repository root (`.`)

**Spec reference:** `<path-to-design-spec>/2026-03-15-android-ai-chat-client-design.md`

## File Structure Map

### Existing template files to normalize

- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle.properties`
- Modify: `.gitignore`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/example/chatppp/MainActivity.kt`
- Modify: `app/src/main/java/com/example/chatppp/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/example/chatppp/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/chatppp/ui/theme/Type.kt`
- Delete later: `app/src/test/java/com/example/chatppp/ExampleUnitTest.kt`
- Delete later: `app/src/androidTest/java/com/example/chatppp/ExampleInstrumentedTest.kt`

### New app structure

- Create: `app/src/main/java/com/chatppp/app/ChatPppApplication.kt`
- Create: `app/src/main/java/com/chatppp/app/navigation/ChatPppNavGraph.kt`
- Create: `app/src/main/java/com/chatppp/app/di/AppModule.kt`
- Create: `app/src/main/java/com/chatppp/app/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/chatppp/app/di/NetworkModule.kt`
- Create: `app/src/main/java/com/chatppp/app/di/ProviderModule.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/Conversation.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/Message.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/ChatConfig.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/MessageRole.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/MessageStatus.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/ProviderType.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/ChatChunk.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/ChatError.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/repository/ChatRepository.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/db/ChatPppDatabase.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/db/ConversationDao.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/db/MessageDao.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/entity/ConversationEntity.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/entity/MessageEntity.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/preferences/AppPreferences.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/secrets/SecretStore.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/secrets/EncryptedSecretStore.kt`
- Create: `app/src/main/java/com/chatppp/app/data/mapper/ConversationMappers.kt`
- Create: `app/src/main/java/com/chatppp/app/data/mapper/MessageMappers.kt`
- Create: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/model/ChatRequestDto.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/model/ChatResponseDto.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/model/StreamChunkDto.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/parser/ChatStreamParser.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/ChatProvider.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/DirectApiProvider.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/RelayApiProvider.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatAction.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatRoute.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/ComposerBar.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageList.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/common/UiMessage.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListRoute.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListScreen.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListViewModel.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/settings/SettingsRoute.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/theme/Type.kt`

### Tests

- Create: `app/src/test/java/com/chatppp/app/data/local/entity/EntityMappingTest.kt`
- Create: `app/src/test/java/com/chatppp/app/data/local/preferences/AppPreferencesTest.kt`
- Create: `app/src/test/java/com/chatppp/app/data/remote/parser/ChatStreamParserTest.kt`
- Create: `app/src/test/java/com/chatppp/app/data/remote/provider/DirectApiProviderTest.kt`
- Create: `app/src/test/java/com/chatppp/app/data/remote/provider/RelayApiProviderTest.kt`
- Create: `app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt`
- Create: `app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt`
- Create: `app/src/test/java/com/chatppp/app/ui/conversations/ConversationListViewModelTest.kt`
- Create: `app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt`
- Create: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`

## Chunk 1: Normalize The Template

### Task 1: Re-root the template for the real app

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle.properties`
- Modify: `.gitignore`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Delete: `app/src/test/java/com/example/chatppp/ExampleUnitTest.kt`
- Delete: `app/src/androidTest/java/com/example/chatppp/ExampleInstrumentedTest.kt`

- [ ] **Step 1: Initialize Git in the real project root**

Run: `git init`
Expected: Git repository initialized in the repository root

- [ ] **Step 2: Update ignore rules for Android development**

Keep ignored:
- `.gradle/`
- `local.properties`
- `app/build/`
- IDE workspace caches

Keep tracked:
- wrapper files
- Gradle scripts
- source files
- `docs/superpowers/plans/`

- [ ] **Step 3: Rename the visible app identity**

Change:
- app name from template value to `ChatPPP`
- manifest labels to use the updated string resource

- [ ] **Step 4: Raise baseline app constraints**

Update `app/build.gradle.kts` so the project aligns with the approved design:
- `minSdk = 26`
- Compose remains enabled
- add modern dependencies needed for the architecture

- [ ] **Step 5: Replace template tests with project-owned test packages**

Delete the generated example tests so the suite reflects only real app behavior.

- [ ] **Step 6: Run a smoke build**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL with no template test failures

- [ ] **Step 7: Commit**

Run:
```bash
git add .
git commit -m "chore: normalize chatppp android template"
```

### Task 2: Replace the placeholder package and entry point

**Files:**
- Delete: `app/src/main/java/com/example/chatppp/MainActivity.kt`
- Delete: `app/src/main/java/com/example/chatppp/ui/theme/Color.kt`
- Delete: `app/src/main/java/com/example/chatppp/ui/theme/Theme.kt`
- Delete: `app/src/main/java/com/example/chatppp/ui/theme/Type.kt`
- Create: `app/src/main/java/com/chatppp/app/ChatPppApplication.kt`
- Create: `app/src/main/java/com/chatppp/app/MainActivity.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/theme/Type.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write a failing smoke test for the new package namespace**

Create a tiny unit test in `app/src/test/java/com/chatppp/app/AppPackageSmokeTest.kt` that imports the new package path.

Run: `.\gradlew.bat testDebugUnitTest --tests "*AppPackageSmokeTest"`
Expected: FAIL because the package files do not exist yet

- [ ] **Step 2: Move the app namespace to `com.chatppp.app`**

Update:
- `namespace`
- `applicationId`
- manifest application class
- source packages

- [ ] **Step 3: Replace the placeholder entry screen**

Implement:
- `ChatPppApplication`
- `MainActivity`
- a minimal Compose surface showing `ChatPPP`

- [ ] **Step 4: Re-run the smoke test**

Run: `.\gradlew.bat testDebugUnitTest --tests "*AppPackageSmokeTest"`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add app/src/main app/src/test/java/com/chatppp/app/AppPackageSmokeTest.kt
git commit -m "refactor: move app to chatppp namespace"
```

## Chunk 2: Local Models And Storage

### Task 3: Add the core domain and entity model

**Files:**
- Create: `app/src/main/java/com/chatppp/app/domain/model/Conversation.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/Message.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/ChatConfig.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/MessageRole.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/MessageStatus.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/ProviderType.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/entity/ConversationEntity.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/entity/MessageEntity.kt`
- Create: `app/src/main/java/com/chatppp/app/data/mapper/ConversationMappers.kt`
- Create: `app/src/main/java/com/chatppp/app/data/mapper/MessageMappers.kt`
- Test: `app/src/test/java/com/chatppp/app/data/local/entity/EntityMappingTest.kt`

- [ ] **Step 1: Write failing mapper tests**

Cover:
- conversation timestamps survive mapping
- message role and status survive mapping

Run: `.\gradlew.bat testDebugUnitTest --tests "*EntityMappingTest"`
Expected: FAIL because models and mappers are missing

- [ ] **Step 2: Implement the domain enums and data classes**

Keep only fields approved in the spec.

- [ ] **Step 3: Implement Room entities and mapper functions**

Use:
- one conversation table
- one message table keyed by `conversationId`

- [ ] **Step 4: Re-run mapper tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "*EntityMappingTest"`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/domain app/src/main/java/com/chatppp/app/data/local/entity app/src/main/java/com/chatppp/app/data/mapper app/src/test/java/com/chatppp/app/data/local/entity/EntityMappingTest.kt
git commit -m "feat: add chat domain and entity models"
```

### Task 4: Add Room, DataStore, and secret storage

**Files:**
- Create: `app/src/main/java/com/chatppp/app/data/local/db/ChatPppDatabase.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/db/ConversationDao.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/db/MessageDao.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/preferences/AppPreferences.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/secrets/SecretStore.kt`
- Create: `app/src/main/java/com/chatppp/app/data/local/secrets/EncryptedSecretStore.kt`
- Create: `app/src/main/java/com/chatppp/app/di/DatabaseModule.kt`
- Test: `app/src/test/java/com/chatppp/app/data/local/preferences/AppPreferencesTest.kt`

- [ ] **Step 1: Write failing preferences tests**

Cover:
- default provider is `DIRECT`
- base URL and model round-trip correctly
- stream-enabled flag persists

Run: `.\gradlew.bat testDebugUnitTest --tests "*AppPreferencesTest"`
Expected: FAIL because preferences classes are missing

- [ ] **Step 2: Implement DAOs and database**

Expose:
- conversations ordered by `updatedAt DESC`
- messages ordered by `createdAt ASC`

- [ ] **Step 3: Implement DataStore-backed settings**

Persist:
- provider type
- base URL
- model
- stream enabled

- [ ] **Step 4: Implement secret storage abstraction**

Store direct API key and relay token in separate slots.

- [ ] **Step 5: Re-run preferences tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "*AppPreferencesTest"`
Expected: PASS

- [ ] **Step 6: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/data/local app/src/main/java/com/chatppp/app/di/DatabaseModule.kt app/src/test/java/com/chatppp/app/data/local/preferences/AppPreferencesTest.kt
git commit -m "feat: add room datastore and secret storage"
```

## Chunk 3: Provider Abstraction And Streaming

### Task 5: Add the provider contract and stream parser

**Files:**
- Create: `app/src/main/java/com/chatppp/app/domain/model/ChatChunk.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/ChatError.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/ChatProvider.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/model/ChatRequestDto.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/model/ChatResponseDto.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/model/StreamChunkDto.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/parser/ChatStreamParser.kt`
- Test: `app/src/test/java/com/chatppp/app/data/remote/parser/ChatStreamParserTest.kt`

- [ ] **Step 1: Write failing parser tests**

Cover:
- `data:` chunks are parsed in order
- `[DONE]` ends the stream
- malformed chunks become typed stream errors

Run: `.\gradlew.bat testDebugUnitTest --tests "*ChatStreamParserTest"`
Expected: FAIL because parser files do not exist

- [ ] **Step 2: Define the provider contract**

Include:
- non-stream send
- stream send returning `Flow<ChatChunk>`
- typed errors instead of raw exceptions

- [ ] **Step 3: Implement serialization models and parser**

Keep parsing isolated from HTTP concerns.

- [ ] **Step 4: Re-run parser tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "*ChatStreamParserTest"`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/domain/model/ChatChunk.kt app/src/main/java/com/chatppp/app/domain/model/ChatError.kt app/src/main/java/com/chatppp/app/data/remote app/src/test/java/com/chatppp/app/data/remote/parser/ChatStreamParserTest.kt
git commit -m "feat: add provider contract and stream parser"
```

### Task 6: Implement direct and relay providers

**Files:**
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/DirectApiProvider.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/RelayApiProvider.kt`
- Create: `app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt`
- Create: `app/src/main/java/com/chatppp/app/di/AppModule.kt`
- Create: `app/src/main/java/com/chatppp/app/di/NetworkModule.kt`
- Create: `app/src/main/java/com/chatppp/app/di/ProviderModule.kt`
- Test: `app/src/test/java/com/chatppp/app/data/remote/provider/DirectApiProviderTest.kt`
- Test: `app/src/test/java/com/chatppp/app/data/remote/provider/RelayApiProviderTest.kt`

- [ ] **Step 1: Write failing provider tests with MockWebServer**

Cover:
- direct mode sends bearer auth from stored API key
- relay mode sends relay token header
- stream responses emit ordered chunks
- `401` becomes auth error

Run: `.\gradlew.bat testDebugUnitTest --tests "*DirectApiProviderTest" --tests "*RelayApiProviderTest"`
Expected: FAIL because provider implementations are missing

- [ ] **Step 2: Implement the shared network setup**

Create Hilt-provided OkHttp and serialization plumbing.

- [ ] **Step 3: Implement `DirectApiProvider`**

Assume an OpenAI-compatible chat endpoint for phase one.

- [ ] **Step 4: Implement `RelayApiProvider`**

Assume a relay endpoint that preserves the same client message shape when possible.

- [ ] **Step 5: Re-run provider tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "*DirectApiProviderTest" --tests "*RelayApiProviderTest"`
Expected: PASS

- [ ] **Step 6: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/data/remote/provider app/src/main/java/com/chatppp/app/di app/src/test/java/com/chatppp/app/data/remote/provider
git commit -m "feat: implement direct and relay providers"
```

## Chunk 4: Repository And State Flow

### Task 7: Implement repository orchestration

**Files:**
- Create: `app/src/main/java/com/chatppp/app/domain/repository/ChatRepository.kt`
- Create: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Test: `app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt`

- [ ] **Step 1: Write failing repository tests**

Cover:
- user message is stored before provider call
- placeholder assistant message is inserted before streaming
- streamed chunks append into one assistant message
- provider failures mark the assistant message as `ERROR`

Run: `.\gradlew.bat testDebugUnitTest --tests "*DefaultChatRepositoryTest"`
Expected: FAIL because repository files are missing

- [ ] **Step 2: Define the repository API**

Include:
- observe conversations
- observe messages
- create conversation
- send message
- retry failed message
- stop active stream

- [ ] **Step 3: Implement `DefaultChatRepository`**

Keep stream cancellation in repository scope rather than UI scope.

- [ ] **Step 4: Re-run repository tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "*DefaultChatRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/domain/repository/ChatRepository.kt app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt
git commit -m "feat: add repository streaming orchestration"
```

### Task 8: Add chat screen state and ViewModel

**Files:**
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatAction.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/common/UiMessage.kt`
- Test: `app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt`

- [ ] **Step 1: Write failing ViewModel tests**

Cover:
- blank sends are ignored
- sending clears composer state once submission begins
- stream updates appear in `ChatUiState`
- retry action replays the failed turn
- stop action clears streaming state

Run: `.\gradlew.bat testDebugUnitTest --tests "*ChatViewModelTest"`
Expected: FAIL because ViewModel files are missing

- [ ] **Step 2: Implement `ChatAction` and `ChatUiState`**

Keep a single state object for the screen.

- [ ] **Step 3: Implement `ChatViewModel`**

Collect repository flows and map them into UI-friendly state.

- [ ] **Step 4: Re-run ViewModel tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "*ChatViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/ui/chat app/src/main/java/com/chatppp/app/ui/common/UiMessage.kt app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt
git commit -m "feat: add chat state and viewmodel"
```

## Chunk 5: Compose UI And Navigation

### Task 9: Build the main chat screen

**Files:**
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatRoute.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/ComposerBar.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageList.kt`
- Test: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`

- [ ] **Step 1: Write failing Compose UI tests**

Cover:
- user message bubble renders
- assistant streaming row renders
- send button disables for blank input
- retry action is visible on error state

Run: `.\gradlew.bat connectedDebugAndroidTest`
Expected: FAIL because chat UI is missing

- [ ] **Step 2: Implement the message list and composer**

Keep the first version plain but complete.

- [ ] **Step 3: Connect `ChatRoute` to `ChatViewModel`**

Translate user actions only. Do not duplicate state inside composables.

- [ ] **Step 4: Re-run UI tests**

Run: `.\gradlew.bat connectedDebugAndroidTest`
Expected: PASS for chat screen tests

- [ ] **Step 5: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/ui/chat app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt
git commit -m "feat: implement compose chat surface"
```

### Task 10: Add navigation, conversations, and settings

**Files:**
- Create: `app/src/main/java/com/chatppp/app/navigation/ChatPppNavGraph.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListRoute.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListScreen.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/conversations/ConversationListViewModel.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/settings/SettingsRoute.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Test: `app/src/test/java/com/chatppp/app/ui/conversations/ConversationListViewModelTest.kt`
- Test: `app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt`

- [ ] **Step 1: Write failing conversation and settings tests**

Cover:
- conversations are exposed in most-recently-updated order
- creating a conversation inserts a new record
- switching provider persists `ProviderType`
- base URL and model changes persist

Run:
- `.\gradlew.bat testDebugUnitTest --tests "*ConversationListViewModelTest"`
- `.\gradlew.bat testDebugUnitTest --tests "*SettingsViewModelTest"`
Expected: FAIL because these ViewModels and screens are missing

- [ ] **Step 2: Implement conversation list ViewModel and screen**

Support:
- new conversation
- switch conversation
- delete conversation

- [ ] **Step 3: Implement settings ViewModel and screen**

Support:
- provider switching
- base URL
- model
- stream toggle
- saving direct key and relay token

- [ ] **Step 4: Implement the nav graph**

Destinations:
- chat
- conversations
- settings

- [ ] **Step 5: Re-run tests**

Run:
- `.\gradlew.bat testDebugUnitTest --tests "*ConversationListViewModelTest"`
- `.\gradlew.bat testDebugUnitTest --tests "*SettingsViewModelTest"`
Expected: PASS

- [ ] **Step 6: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/navigation app/src/main/java/com/chatppp/app/ui/conversations app/src/main/java/com/chatppp/app/ui/settings app/src/test/java/com/chatppp/app/ui/conversations/ConversationListViewModelTest.kt app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt
git commit -m "feat: add conversations settings and navigation"
```

## Chunk 6: Hardening And Verification

### Task 11: Polish empty, error, and retry states

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt`
- Modify: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`
- Modify: `app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt`

- [ ] **Step 1: Add failing tests for edge states**

Cover:
- empty conversation placeholder
- assistant error row retry affordance
- stop action hides streaming indicator

Run:
- `.\gradlew.bat testDebugUnitTest --tests "*ChatViewModelTest"`
- `.\gradlew.bat connectedDebugAndroidTest`
Expected: FAIL on the new assertions

- [ ] **Step 2: Implement minimal UX polish**

Add:
- empty state copy
- retry affordance
- cleaner streaming state handling

- [ ] **Step 3: Re-run the chat-focused test suites**

Run:
- `.\gradlew.bat testDebugUnitTest --tests "*ChatViewModelTest"`
- `.\gradlew.bat connectedDebugAndroidTest`
Expected: PASS

- [ ] **Step 4: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/ui/chat app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt
git commit -m "feat: polish chat edge states"
```

### Task 12: Final verification and developer docs

**Files:**
- Create: `README.md`
- Modify: `app/build.gradle.kts` if verification fixes are required
- Create: `docs/superpowers/plans/` if not already present in Git

- [x] **Step 1: Write a concise README**

Document:
- project purpose
- direct mode versus relay mode
- local setup expectations
- how to run tests
- current MVP limitations

- [x] **Step 2: Run full verification**

Run:
- `.\gradlew.bat lint`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

Expected:
- lint passes or surfaces only intentionally deferred issues
- unit tests pass
- debug APK builds

- [x] **Step 3: Record manual smoke checks**

Verify:
- create conversation
- send streamed direct message
- save direct config or preset
- reopen app and confirm history persists

Recorded in `README.md` on `2026-03-16`:
- fresh workspace verification commands completed
- focused emulator verification passed for chat, settings, and navigation instrumentation
- live direct API smoke remains optional and depends on valid local API credentials

- [ ] **Step 4: Commit**

Run:
```bash
git add README.md
git commit -m "docs: add setup and verification guide"
```

## Chunk 7: Next-Phase Runtime Improvements

### Task 13: Define and integrate a real relay backend contract

**Goal:** Turn the current placeholder relay mode into a production-usable backend-forwarding mode.

**Status:** On hold. The client-side relay abstraction, contract document, and validation UX are retained, but the current project phase will not implement or maintain a separate relay backend service. Direct API flows are the active priority.

**Files:**
- Modify: `README.md`
- Modify: `app/src/main/java/com/chatppp/app/data/remote/provider/RelayApiProvider.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Create: `docs/superpowers/specs/relay-backend-contract.md`
- Test: `app/src/test/java/com/chatppp/app/data/remote/provider/RelayApiProviderTest.kt`

- [x] **Step 1: Write the relay backend contract document**

Document:
- relay endpoint shape should remain OpenAI-compatible at `/chat/completions`
- client authentication uses `X-Relay-Token`
- backend is responsible for mapping relay token to provider credentials
- backend should forward both non-stream and SSE stream responses
- backend error payloads should stay compatible with current `ChatError` mapping

- [x] **Step 2: Add failing relay integration tests**

Cover:
- relay requests send `X-Relay-Token`
- relay accepts OpenAI Base URL and appends `/chat/completions`
- relay stream path supports the same SSE chunk format as direct mode
- relay-specific config errors surface readable assistant error bubbles

Run:
`.\gradlew.bat testDebugUnitTest --tests "*RelayApiProviderTest"`
Expected: FAIL on new relay contract assertions

- [x] **Step 3: Tighten client relay configuration UX**

Update:
- settings copy so relay mode clearly says it requires your own backend
- validation copy so users do not confuse relay mode with third-party model platforms
- README setup examples for both direct and relay deployment

- [x] **Step 4: Implement the minimal relay compatibility changes**

Keep:
- OpenAI Base URL semantics
- SSE parsing path shared with direct mode
- relay token header behavior unchanged unless the documented backend contract changes

- [x] **Step 5: Re-run relay tests**

Run:
`.\gradlew.bat testDebugUnitTest --tests "*RelayApiProviderTest"`
Expected: PASS

- [ ] **Step 6: Commit**

Run:
```bash
git add README.md app/src/main/java/com/chatppp/app/data/remote/provider/RelayApiProvider.kt app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt app/src/main/java/com/chatppp/app/ui/settings app/src/test/java/com/chatppp/app/data/remote/provider/RelayApiProviderTest.kt docs/superpowers/specs/relay-backend-contract.md
git commit -m "feat: define relay backend contract"
```

### Task 14: Replace count-based compression with token-budget context management

**Goal:** Keep requests safely under a `32K` estimated context ceiling by triggering summary compression from token budget thresholds instead of message-count heuristics.

**Defaults for version two:**
- `maxContextTokens = 32768`
- `compressionTriggerTokens = 24576`
- `targetCompressedTokens = 14336`
- `reservedResponseTokens = 6144`

**Notes:**
- The current count-based implementation is considered transitional and should be replaced.
- Version two should preserve complete recent turns, not an arbitrary number of raw messages.
- Version two should prevent empty assistant placeholders from being treated as successful context history.

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/preferences/AppPreferences.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/context/ConversationContextBuilder.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/context/ConversationSummaryGenerator.kt`
- Create: `app/src/main/java/com/chatppp/app/data/context/ContextTokenEstimator.kt`
- Create: `app/src/main/java/com/chatppp/app/data/context/CompressionBudget.kt`
- Test: `app/src/test/java/com/chatppp/app/data/context/ConversationContextBuilderTest.kt`
- Test: `app/src/test/java/com/chatppp/app/data/context/ConversationSummaryGeneratorTest.kt`
- Test: `app/src/test/java/com/chatppp/app/data/context/ContextTokenEstimatorTest.kt`
- Test: `app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt`
- Test: `app/src/test/java/com/chatppp/app/data/local/preferences/AppPreferencesTest.kt`
- Test: `app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt`

- [x] **Step 1: Write failing tests for token-budget-based request assembly**

Cover:
- context estimation grows with system summary, user content, assistant content, and current user input
- compression does not trigger while estimated request tokens stay below `24576`
- compression triggers once estimated request tokens cross `24576`
- post-compression request assembly targets `14336` tokens or less before the final chat request is sent
- recent raw context is preserved as complete turns, not split message fragments
- empty assistant placeholders and errored assistant messages are excluded from both summary input and raw context replay

Run:
- `./gradlew.bat testDebugUnitTest --tests "*ConversationContextBuilderTest"`
- `./gradlew.bat testDebugUnitTest --tests "*ConversationSummaryGeneratorTest"`
- `./gradlew.bat testDebugUnitTest --tests "*ContextTokenEstimatorTest"`
- `./gradlew.bat testDebugUnitTest --tests "*DefaultChatRepositoryTest"`
Expected: FAIL on new token-budget assertions because the current implementation is still count-based

- [x] **Step 2: Introduce a focused token budget model and estimator**

Add:
- a `CompressionBudget` value object that centralizes the `32K / 24K / 14K / 6K` defaults
- a lightweight `ContextTokenEstimator` that uses provider-agnostic estimated tokens rather than exact tokenizer bindings
- helper methods for estimating one message, a full request payload, and a summary prompt payload

- [x] **Step 3: Rework `ConversationContextBuilder` around token budgets and complete turns**

Replace count-based selection with:
- complete recent user/assistant turns preserved in oldest-to-newest order
- current user input always retained as raw content
- stored summary injected only when needed
- raw replay trimmed by estimated token budget, not by message count
- no split turns where a user message survives but its paired assistant reply is dropped unless the assistant message is invalid

- [x] **Step 4: Trigger summary generation only when request budget is exceeded**

Update repository flow so that:
- normal sends skip summarization work while the estimated request stays below `compressionTriggerTokens`
- summary generation runs only after budget overflow is detected
- summary prompts themselves respect the reserved budget instead of consuming the full ceiling
- summary generation failure falls back to raw context replay rather than blocking the user send

- [x] **Step 5: Tighten empty-response handling so blank assistant turns do not poison context**

Ensure:
- a streamed assistant placeholder is not marked `SUCCESS` if no visible content was ever accumulated
- empty assistant turns are either removed, marked error, or otherwise excluded from future replay
- follow-up sends cannot reinterpret a prior empty assistant turn as “the model has not answered yet”

- [x] **Step 6: Simplify user-facing settings to match the new default-budget approach**

Version two should:
- keep the existing `Summary compression` toggle
- remove the `recent raw messages` setting from the UI and persisted runtime settings
- keep the token-budget defaults in code for now
- leave future budget customization as a later enhancement once stability is proven

- [x] **Step 7: Re-run context, repository, preferences, settings, and focused instrumentation tests**

Run:
- `./gradlew.bat testDebugUnitTest --tests "*ConversationContextBuilderTest"`
- `./gradlew.bat testDebugUnitTest --tests "*ConversationSummaryGeneratorTest"`
- `./gradlew.bat testDebugUnitTest --tests "*ContextTokenEstimatorTest"`
- `./gradlew.bat testDebugUnitTest --tests "*DefaultChatRepositoryTest"`
- `./gradlew.bat testDebugUnitTest --tests "*AppPreferencesTest"`
- `./gradlew.bat testDebugUnitTest --tests "*SettingsViewModelTest"`
- `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.settings.SettingsScreenTest"`
Expected: PASS

- [ ] **Step 8: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/data/context app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt app/src/main/java/com/chatppp/app/data/local/preferences/AppPreferences.kt app/src/main/java/com/chatppp/app/ui/settings app/src/test/java/com/chatppp/app/data/context/ConversationContextBuilderTest.kt app/src/test/java/com/chatppp/app/data/context/ConversationSummaryGeneratorTest.kt app/src/test/java/com/chatppp/app/data/context/ContextTokenEstimatorTest.kt app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt app/src/test/java/com/chatppp/app/data/local/preferences/AppPreferencesTest.kt app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/settings/SettingsScreenTest.kt
git commit -m "refactor: switch context compression to token budgets"
```
### Task 15: Add reusable full-config presets with conversation-level switching

**Goal:** Let users save multiple complete API configurations and switch presets per conversation without losing chat history.

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/domain/model/Conversation.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/entity/ConversationEntity.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/mapper/ConversationMappers.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/preferences/AppPreferences.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/secrets/SecretStore.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsUiState.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatRoute.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Create: `app/src/main/java/com/chatppp/app/domain/model/ConfigPreset.kt`
- Create: `app/src/main/java/com/chatppp/app/data/presets/ConfigPresetStore.kt`
- Create: `app/src/main/java/com/chatppp/app/data/presets/DefaultConfigPresetStore.kt`
- Test: `app/src/test/java/com/chatppp/app/data/presets/ConfigPresetStoreTest.kt`
- Test: `app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt`
- Test: `app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt`

- [x] **Step 1: Write failing preset store tests**

Cover:
- complete presets round-trip with `providerType / baseUrl / model / streamEnabled / directApiKey / relayToken`
- multiple presets can coexist
- deleting a preset removes its associated secrets
- one preset can be marked active for a conversation

Run:
`.\gradlew.bat testDebugUnitTest --tests "*ConfigPresetStoreTest"`
Expected: FAIL because preset storage does not exist yet

- [x] **Step 2: Implement a focused preset domain model and storage layer**

Keep version one simple:
- global preset list
- each preset stores a full runnable configuration
- secrets remain outside plain preferences where possible
- conversation stores only the bound `presetId`

- [x] **Step 3: Route provider selection through the conversation's current preset**

Update:
- repository request dispatch uses the active conversation preset
- switching presets inside a conversation does not create a new conversation
- existing message history remains unchanged after switching

- [x] **Step 4: Add settings and chat UI for preset management**

Support:
- save current configuration as preset
- select another preset for the current conversation
- rename and delete presets
- show which preset the current conversation is using

- [x] **Step 5: Re-run preset, repository, and settings tests**

Run:
- `.\gradlew.bat testDebugUnitTest --tests "*ConfigPresetStoreTest"`
- `.\gradlew.bat testDebugUnitTest --tests "*DefaultChatRepositoryTest"`
- `.\gradlew.bat testDebugUnitTest --tests "*SettingsViewModelTest"`
Expected: PASS

- [ ] **Step 6: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/domain/model/ConfigPreset.kt app/src/main/java/com/chatppp/app/data/presets app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt app/src/main/java/com/chatppp/app/data/remote/provider/ProviderSelector.kt app/src/main/java/com/chatppp/app/ui/settings app/src/main/java/com/chatppp/app/ui/chat app/src/test/java/com/chatppp/app/data/presets/ConfigPresetStoreTest.kt app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt app/src/test/java/com/chatppp/app/ui/settings/SettingsViewModelTest.kt
git commit -m "feat: add conversation preset switching"
```

### Task 16: Add hidden-by-default thinking content controls

**Goal:** Support models that emit thinking/reasoning content while keeping the default chat transcript clean and user-controllable.

**Files:**
- Modify: `app/src/main/java/com/chatppp/app/domain/model/Message.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/entity/MessageEntity.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/mapper/MessageMappers.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/remote/model/ChatRequestDto.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/remote/parser/ChatStreamParser.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/common/UiMessage.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatAction.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageList.kt`
- Modify: `app/src/main/java/com/chatppp/app/ui/chat/components/MessageBubble.kt`
- Modify: `app/src/main/java/com/chatppp/app/data/local/db/ChatPppDatabase.kt`
- Test: `app/src/test/java/com/chatppp/app/data/remote/parser/ChatStreamParserTest.kt`
- Test: `app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt`
- Test: `app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt`
- Test: `app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt`

- [x] **Step 1: Write failing tests for reasoning payload handling**

Cover:
- parser can distinguish answer text from thinking text when provider payload includes both
- repository persists answer content and optional thinking content separately
- chat UI hides thinking blocks by default
- user can manually expand a message to reveal its thinking content

Run:
- `.\gradlew.bat testDebugUnitTest --tests "*ChatStreamParserTest" --tests "*DefaultChatRepositoryTest" --tests "*ChatViewModelTest"`
- `.\gradlew.bat connectedDebugAndroidTest`
Expected: FAIL on the new reasoning-content assertions

- [x] **Step 2: Extend message and parser models for optional thinking content**

Keep version one narrow:
- answer content remains the primary visible transcript
- thinking content is stored separately
- no message-level historical provider badge is added in this phase

- [x] **Step 3: Add hidden-by-default UI controls**

Support:
- collapsed by default
- per-message expand/collapse
- optional global setting that controls whether thinking is shown by default in future

- [x] **Step 4: Re-run parser, repository, ViewModel, and UI tests**

Run:
- `.\gradlew.bat testDebugUnitTest --tests "*ChatStreamParserTest" --tests "*DefaultChatRepositoryTest" --tests "*ChatViewModelTest"`
- `.\gradlew.bat compileDebugAndroidTestKotlin --rerun-tasks`
- `cmd /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chatppp.app.ui.chat.ChatScreenTest,com.chatppp.app.ui.settings.SettingsScreenTest,com.chatppp.app.navigation.ChatPppNavigationTest"`
- `.\gradlew.bat testDebugUnitTest assembleDebug`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add app/src/main/java/com/chatppp/app/domain/model/Message.kt app/src/main/java/com/chatppp/app/data/local/entity/MessageEntity.kt app/src/main/java/com/chatppp/app/data/mapper/MessageMappers.kt app/src/main/java/com/chatppp/app/data/remote/model/StreamChunkDto.kt app/src/main/java/com/chatppp/app/data/remote/parser/ChatStreamParser.kt app/src/main/java/com/chatppp/app/data/repository/DefaultChatRepository.kt app/src/main/java/com/chatppp/app/ui/common/UiMessage.kt app/src/main/java/com/chatppp/app/ui/chat app/src/main/java/com/chatppp/app/ui/settings app/src/test/java/com/chatppp/app/data/remote/parser/ChatStreamParserTest.kt app/src/test/java/com/chatppp/app/data/repository/DefaultChatRepositoryTest.kt app/src/test/java/com/chatppp/app/ui/chat/ChatViewModelTest.kt app/src/androidTest/java/com/chatppp/app/ui/chat/ChatScreenTest.kt
git commit -m "feat: add thinking content controls"
```

## Manual Review Notes

The original skill expects a plan-review subagent loop. This Codex harness does not expose a dedicated plan-reviewer subagent, so do a manual review before execution:

- confirm every file path uses the `ChatPPP` root
- remove any stale `com.example.chatppp` references before starting implementation
- keep TDD as the default even when Android Studio scaffolding tempts direct coding
- adjust only if Android Studio regenerates files with different package folders

Plan complete and saved to `docs/superpowers/plans/2026-03-15-android-ai-chat-client.md`. Ready to execute?
