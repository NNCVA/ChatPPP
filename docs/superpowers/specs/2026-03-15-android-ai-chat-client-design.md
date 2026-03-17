# Android AI Chat Client Design

**Date:** 2026-03-15

**Project Goal:** Build a resume-oriented native Android AI chat client as a separate app.

## Summary

This design defines a new native Android application focused on AI chat. The first release is a pure chat MVP designed to strengthen Android internship positioning by showcasing modern native UI, state management, streaming responses, local persistence, and extensible provider integration.

The app will be built with Jetpack Compose and a lightweight layered architecture based on `ViewModel + StateFlow + Repository + Provider abstraction`. The current project phase prioritizes one production-usable integration mode and keeps the second as a reserved extension path behind the same abstraction:

- direct model access with user-supplied API key
- backend relay compatibility reserved for a future user-owned backend project

The first phase intentionally excludes files, images, search, payment, and agent-style orchestration so the project stays tight, demoable, and resume-relevant.

## Motivation

The existing music player project already demonstrates core Android fundamentals such as media playback, local data, custom views, and CI. A second project should add a different capability profile rather than repeat the same strengths.

An AI chat client complements the current portfolio by adding evidence in these areas:

- modern Android UI with Compose
- network communication and streaming response handling
- structured UI state management
- local conversation persistence
- architecture that supports multiple upstream providers

This creates a stronger overall project mix for Android internship applications than adding another traditional utility app.

## Goals

- Build a separate native Android app using Kotlin and Jetpack Compose.
- Ship a pure chat MVP with a polished, interview-ready architecture.
- Support direct API key mode end to end, while preserving a relay-compatible client abstraction for future backend integration.
- Persist conversations and settings locally so the app behaves like a real product rather than a stateless demo.
- Make the streaming message flow, error handling, and state transitions explicit and testable.
- Keep the scope narrow enough to finish and verify without drifting into backend-heavy or multimodal work.

## Non-Goals

- No image upload or image understanding in phase one.
- No file upload, parsing, or document chat in phase one.
- No voice input, TTS, search, or agent tooling.
- No payment, membership purchase flow, or admin tooling.
- No multi-device sync requirement in phase one.
- No attempt to reproduce the full DeepSeek product surface.

## Product Scope

### Primary user

The primary user is the developer and interviewer audience: the app must demonstrate sound Android engineering choices while still feeling like a coherent user product.

### MVP user value

Users can:

- create and manage chat conversations
- send prompts and receive streamed AI responses
- configure direct API mode and preserve compatibility with a future relay mode
- configure model endpoint settings
- resume previous conversations from local storage
- save reusable runtime presets and bind them per conversation

## Recommended Architecture

### High-level approach

Use a pragmatic architecture centered on:

- Compose UI
- screen-level `ViewModel`
- `StateFlow`-driven `UiState`
- repository layer for orchestration
- provider abstraction for upstream chat integration
- Room and DataStore for persistence

This keeps the project lighter than a full Clean Architecture implementation while still creating clear boundaries that are easy to explain in interviews.

### Layers

#### UI layer

Compose screens and reusable UI components for:

- chat screen
- conversation list screen
- settings screen

The UI should render immutable screen state and dispatch user actions upward rather than holding business logic inside composables.

#### Presentation layer

Each screen has a `ViewModel` that:

- exposes a single `UiState`
- accepts user actions or events
- coordinates asynchronous work through repositories
- maps data and failures into renderable screen state

This is intentionally MVVM-shaped, while adopting some MVI discipline by routing user actions through a unified event path.

#### Data layer

The data layer owns:

- conversation and message persistence
- provider and settings persistence
- upstream chat request execution
- mapping transport responses into domain-safe results

#### Provider layer

The provider layer defines a common chat capability contract that hides whether the app is talking directly to a model endpoint or to a relay-compatible backend.

Planned implementations:

- `DirectApiProvider`
- `RelayApiProvider`

Both implementations should expose the same response semantics so the rest of the app does not branch on transport details.

## Core Screens

### 1. Chat screen

This is the main product surface and must be the strongest screen in the app.

Responsibilities:

- render the active conversation message list
- accept prompt input
- trigger send, stop, and retry actions
- display streaming assistant output incrementally
- show loading and error states clearly
- support clean plain-text transcript rendering in phase one
- keep room for future markdown and code block rendering without changing the chat state model

### 2. Conversation list screen

Responsibilities:

- show saved conversations
- create a new conversation
- switch active conversation
- delete a conversation
- leave room for future rename support without blocking the MVP

This screen is important because it makes the app feel like a persistent product rather than a one-shot request demo.

### 3. Settings screen

Responsibilities:

- choose integration mode
- configure direct mode fields such as base URL, model, and API key
- configure relay mode fields such as base URL and auth token
- toggle streaming behavior if needed
- manage summary-compression and preset-related settings
- reserve room for future advanced model parameters

### 4. Relay login or token entry screen

This screen is no longer part of the current MVP target. Relay remains a reserved client-side compatibility path configured from settings rather than a separate login flow. If a future backend project is added, token entry can remain in settings or evolve into a dedicated screen later.

## Data Model

The app should start with a small but extensible data model.

### Core entities

- `Conversation`
  - `id`
  - `title`
  - `providerType`
  - optional `presetId`
  - `createdAt`
  - `updatedAt`

- `Message`
  - `id`
  - `conversationId`
  - `role`
  - `content`
  - optional `thinkingContent`
  - `status`
  - `createdAt`

- `ChatConfig`
  - `providerType`
  - `baseUrl`
  - `model`
  - `streamEnabled`
  - auth material reference

- `ConversationSummary`
  - `conversationId`
  - `summaryText`
  - coverage boundary for summarized history
  - `updatedAt`

- `ConfigPreset`
  - `id`
  - `name`
  - `providerType`
  - `baseUrl`
  - `model`
  - `streamEnabled`
  - auth material reference

### Supporting enums or value objects

- `ProviderType` such as `DIRECT` and `RELAY`
- `MessageRole` such as `USER`, `ASSISTANT`, and optional `SYSTEM`
- `MessageStatus` such as `SENDING`, `STREAMING`, `SUCCESS`, `ERROR`

This model is intentionally structured so future multimodal expansion can add message types or attachments without replacing the current foundation.

## Persistence Strategy

### Room

Use Room for:

- conversations
- messages
- conversation summaries

This allows the chat history to survive process death and makes the app easier to test and demo.

### DataStore

Use DataStore for:

- selected provider type
- endpoint configuration
- model selection
- basic feature toggles
- summary compression toggle
- preset metadata and active preset selection

### Secure secret storage

Store sensitive values such as API keys or relay tokens with an Android-appropriate secure storage approach rather than plain text persistence.

Examples include:

- `EncryptedSharedPreferences`
- a keystore-backed secret wrapper

The exact implementation can be chosen during planning, but the design requires that secrets are handled separately from ordinary app settings.

## State Design

Each important screen should expose a single state object.

Example chat state concerns:

- active conversation id
- ordered message list
- input text
- sending or streaming flag
- current error banner or inline error state
- selected provider summary
- current preset summary

User interactions should flow through explicit actions, for example:

- `SendMessage`
- `StopGenerating`
- `RetryLastResponse`
- `CreateConversation`
- `SwitchConversation`
- `DeleteConversation`
- `ChangeProvider`

This approach keeps screen logic traceable and prevents scattered mutable state.

## Chat Request Flow

The most important runtime behavior is the message send and streaming pipeline.

Recommended sequence:

1. User submits input from the chat screen.
2. `ViewModel` validates input and dispatches a send operation.
3. The user message is persisted locally.
4. A placeholder assistant message is inserted with `STREAMING` status.
5. Repository selects the active provider based on current config or the bound conversation preset.
6. If the estimated context budget is exceeded, older conversation history is summarized before the request is assembled.
7. Provider starts the upstream request.
8. Each streamed chunk updates the assistant message content incrementally.
9. On completion, the assistant message is marked `SUCCESS` only if visible content was actually accumulated.
10. On failure, cancellation, or blank response, the assistant message is marked with the appropriate terminal state and the UI exposes retry or recovery actions.

This flow is preferred because it aligns UI behavior, persistence, and recovery handling into one consistent data path.

## Provider Strategy

The design depends on a common provider contract with stable semantics for:

- single response requests
- streaming response requests
- error mapping

### Direct mode

In direct mode, the app uses a user-supplied API key to call a model-compatible endpoint directly from the client.

Benefits:

- fastest path to a working MVP
- no backend dependency required for the first demo
- easy to validate provider abstraction early

### Relay mode

Relay mode remains a reserved client-side compatibility path. The Android client keeps the provider abstraction, request contract, and validation UX, but the current repository does not ship a relay backend service.

Benefits:

- keeps the architecture extensible for platform account systems or quotas later
- avoids redesigning the client if a backend relay is added in the future
- allows the direct-mode MVP to stay focused without discarding provider abstraction work

The client should still treat relay mode as another provider implementation, not as a special-case app flow, but it is not a phase-one completion requirement.

## Error Handling

Error handling is a first-class design concern because chat apps fail in visible ways.

The app should distinguish at least these categories:

- invalid or missing configuration
- authentication failure
- network timeout or connectivity failure
- non-success server response
- malformed stream or interrupted stream
- user-initiated cancellation

Expected behavior:

- render actionable errors in the UI
- keep conversation history intact
- preserve partial assistant output when reasonable
- allow retry without forcing the user to recreate the conversation

Avoid relying only on transient toasts for critical failure states.

## Testing Strategy

The project should create resume-relevant evidence through focused verification.

### ViewModel tests

Cover:

- send action state transitions
- streaming content accumulation
- provider switching behavior
- failure and retry states
- thinking-content expand or collapse behavior when present

### Repository and provider tests

Cover:

- direct provider contract compliance and relay compatibility where client behavior exists
- success and failure mapping
- streaming chunk assembly behavior
- configuration validation
- token-budget compression behavior

### UI tests

Cover a small but meaningful set of flows:

- message list rendering
- sending a prompt
- displaying loading and error states

The priority is not broad UI automation coverage, but strong protection around the core chat flow.

## MVP Acceptance Criteria

Phase one is complete when the app can:

- create and persist conversations
- send prompts in an active conversation
- render streamed assistant responses
- configure direct mode and keep relay compatibility settings in a reserved state
- persist chat history locally
- persist reusable presets and bind them per conversation
- compress long conversations with summary plus recent-turn replay
- keep optional thinking content hidden by default but user-expandable
- handle common errors and expose retry behavior
- run meaningful tests for core state and provider flows

## Deferred Enhancements

These are explicitly postponed until after the MVP:

- image message support
- file attachment support
- voice input or speech synthesis
- markdown and code block rendering polish
- conversation rename UI
- a real relay backend service
- model marketplace or provider catalog
- search or web-grounded responses
- sync across devices
- payments or subscription UI

## Resume Value

If implemented and verified, this project should create strong interview discussion points in areas that differ from the existing music player:

- Jetpack Compose in a real product flow
- streaming network response handling
- local-first conversation persistence
- multi-provider client architecture
- testable UI state management

Together with the music player project, this creates a more balanced Android portfolio: one project demonstrates media and platform fundamentals, and the other demonstrates modern interactive client architecture.

## Risks And Mitigations

- Compose learning curve may slow delivery.
  - Mitigation: keep the MVP screen set small and reuse predictable state patterns.

- Streaming APIs can be tricky to model correctly.
  - Mitigation: centralize stream parsing and state updates behind the provider and repository layers.

- Supporting both direct and relay modes can inflate scope.
  - Mitigation: use one provider abstraction and keep relay mode minimal in phase one.

- Security handling for API credentials can be overlooked in a client-first prototype.
  - Mitigation: treat secret storage as a design requirement, not a later cleanup.

## Recommended Next Step

After this design is approved in written form, create a detailed implementation plan that:

- breaks the work into small vertical slices
- defines the initial package and module structure
- sequences data, provider, and UI work safely
- includes verification checkpoints for each milestone
