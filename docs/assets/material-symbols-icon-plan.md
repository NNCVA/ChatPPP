# ChatPPP Material Symbols Icon Plan

## Official Sources

- Material Symbols guide: https://developers.google.com/fonts/docs/material_symbols
- Material Symbols library: https://fonts.google.com/icons
- Android Vector Asset Studio: https://developer.android.com/studio/write/vector-asset-studio.html
- Compose icons guidance: https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary
- Material Symbols GitHub repository: https://github.com/google/material-design-icons

## Why This Set

This app is a native Android AI chat client. The icon set below is focused on:

- chat-first navigation
- provider and API configuration
- common response actions
- future-friendly file and image expansion

The recommended source is Google Material Symbols Outlined at `24dp`, `wght=400`, `GRAD=0`, `opsz=24`, with `FILL=0` by default.

## Recommended Icons

| Usage | Material Symbol name | Suggested resource name |
|---|---|---|
| Send message | `send` | `ic_send.xml` |
| Stop generating | `stop_circle` | `ic_stop_circle.xml` |
| Retry or regenerate | `refresh` | `ic_refresh.xml` |
| Copy response | `content_copy` | `ic_content_copy.xml` |
| Chat entry | `chat_bubble` | `ic_chat_bubble.xml` |
| AI assistant mark | `smart_toy` | `ic_smart_toy.xml` |
| Conversation history | `history` | `ic_history.xml` |
| New conversation | `edit_square` | `ic_edit_square.xml` |
| Add item | `add` | `ic_add.xml` |
| Delete conversation | `delete` | `ic_delete.xml` |
| More menu | `more_vert` | `ic_more_vert.xml` |
| Back navigation | `arrow_back` | `ic_arrow_back.xml` |
| Settings | `settings` | `ic_settings.xml` |
| API key field | `vpn_key` | `ic_vpn_key.xml` |
| Backend or endpoint | `dns` | `ic_dns.xml` |
| User or account | `person` | `ic_person.xml` |
| Advanced parameters | `tune` | `ic_tune.xml` |
| Show secret | `visibility` | `ic_visibility.xml` |
| Hide secret | `visibility_off` | `ic_visibility_off.xml` |
| Future file upload | `attach_file` | `ic_attach_file.xml` |
| Future image upload | `image` | `ic_image.xml` |

## Import Rules

For each icon:

1. Open Android Studio in the project root for this repository
2. Right-click `app/src/main/res`
3. Choose `New > Vector Asset`
4. Select `Material Icon`
5. Search by the exact Material Symbol name from the table above
6. Set the resource name to the suggested `ic_*.xml` name
7. Keep size at `24dp` unless a specific screen needs another size

## Compose Usage Notes

Prefer local drawable resources for this project rather than depending on a large icon bundle.

Benefits:

- latest official Material Symbols
- predictable naming in the project
- smaller and more intentional icon set
- easier future replacement if the visual language changes

## First Batch To Import Now

If we only import one batch for immediate development, use these first:

- `send`
- `stop_circle`
- `refresh`
- `content_copy`
- `smart_toy`
- `history`
- `edit_square`
- `settings`
- `vpn_key`
- `dns`
- `visibility`
- `visibility_off`

## Notes For Later UI Work

- Use `smart_toy` for assistant message bubbles or the app header badge.
- Use `edit_square` instead of plain `add` for "new chat" because it reads closer to the current chat-app pattern.
- Use `history` for the conversation list until we decide whether the product language should be "history" or "conversations".
- Use `dns` only in settings or provider config screens, not in the main chat surface.
