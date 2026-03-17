# Relay Backend Contract

## Goal

Define the minimum backend contract required for ChatPPP relay mode so the Android client can forward requests through a user-owned backend instead of sending model credentials directly from the device.

## Request Shape

- The client stores an OpenAI-compatible base URL.
- The client appends `/chat/completions` to that base URL.
- Relay requests use the same JSON body shape as direct mode:
  - `model`
  - `messages`
  - `stream`

## Authentication

- The client must send `X-Relay-Token` on relay requests.
- The relay token authenticates the end user or tenant to the backend relay.
- The backend relay is responsible for mapping the relay token to server-side provider credentials.
- The client must never send upstream provider API keys when relay mode is selected.

## Response Shape

- Non-stream relay responses should remain OpenAI-compatible chat completion payloads.
- Stream relay responses should remain SSE-compatible and preserve `data:` chunk semantics.
- `[DONE]` must terminate the stream in the same way as direct mode.

## Error Contract

- Authentication failures should surface as HTTP `401`.
- Relay-specific validation failures should use readable error payloads or status codes that can be mapped into the existing client `ChatError` model.
- The Android client should present relay configuration problems as user-facing guidance rather than low-level transport errors.

## Client UX Expectations

- Relay mode copy must clearly state that users need their own backend.
- The base URL field should describe relay mode as an OpenAI-compatible backend endpoint, not a third-party hosted model URL pasted directly into the app.
- The relay token field should describe the token as a backend relay credential.
