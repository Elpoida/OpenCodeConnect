# OpenCode Connect — Agent Guide

## Project Overview
An Android app (Kotlin + Jetpack Compose) that connects to `opencode serve` running on a PC. The app mirrors the opencode TUI in a mobile interface — it provides chat with AI coding agents, file browsing, shell access, session management, and provider/model switching through the opencode REST API.

## Architecture
- **MVVM** pattern with Hilt dependency injection
- **Single Activity** with Jetpack Navigation Compose
- **OkHttp + Retrofit** for REST API calls (40+ endpoints)
- **OkHttp SSE** for real-time event streaming
- **kotlinx.serialization** for JSON
- **DataStore** for preferences

## Key Patterns

### Server Communication
The app never runs `opencode` itself — it connects to a running `opencode serve` instance via HTTP/SSE. The server URL and password are dynamic, handled through a `ServerConfig` singleton and `DynamicUrlInterceptor`. Retrofit uses a placeholder base URL (`http://placeholder/`) that the interceptor rewrites.

### SSE Event Handling
The `SseClient` wraps OkHttp's SSE in a Kotlin Flow. The `ChatViewModel` collects events and dispatches them by type. Key events:
- `message.part.delta` — streaming text (append to assistant message)
- `message.part.updated` — tool output (extract from `state.output`)
- `permission.asked` — show permission dialog
- `question.asked` — show question modal with options

### State Management
Each screen uses a `ViewModel` with `StateFlow<UiState>`. Compose screens collect state and recompose on changes. The SSE handler updates state directly via `_state.update {}`.

### Data Models
All data classes in `Models.kt` map to opencode API JSON responses. Critical details:
- Use `@SerialName` for camelCase → Kotlin convention mapping
- `explicitNulls = false` to avoid sending null in request bodies
- `ignoreUnknownKeys = true` for tolerance to API additions

## Common Gotchas

### Android API Compatibility
- **`List.removeLast()` is unavailable** on Android (Java 21 API). Use `removeAt(list.lastIndex)`.
- `animateContentSize()` is experimental in Compose. Test thoroughly.
- `SelectionContainer` can interfere with touch events. Balance with `clickable`.

### opencode Server Behavior
- Shell commands run through the user's login shell (fish on this system)
- `ls` is aliased to `eza -al` which suppresses output when piped without a directory
- Solution: prepend `command` to bypass aliases, skip for shell builtins
- The `ask_user` tool creates a `question.asked` SSE event but has no REST API for response
- Solution: abort the stuck session and send answer as a new chat message
- `POST /session/:id/shell` returns `ToolPart` with output in `state.output`, not `text`
- Session creation rejects `null` in body — always omit null fields

### Model/Provider Structure
- `GET /provider` returns all 170+ providers (each with `env` array but no `key`)
- `GET /config/providers` returns only configured providers (with `key` field)
- Merge both to get full list with connected status
- Provider models are stored as `Map<String, Model>` keyed by model ID

### SSE Event Types
- Tool output comes via `message.part.updated` (type: tool) — NOT `message.part.delta`
- Permission prompts use `permission.asked` (not `permission.updated`)
- Questions use `question.asked` (separate from permissions)
- Response values must be `"once"`, `"always"`, or `"reject"` (not `"allow"`/`"deny"`)

### SSE Reconnection & Dedup
- opencode server replays ALL events when a new SSE connection is established
- Over WiFi, connections drop more often — reconnecting replays events, causing garbled/duplicated text
- Fixed by tracking event IDs from JSON data (`root["id"]`) in `seenEventIds` set
- Replayed events with already-seen IDs are skipped
- Set is cleared on `loadSession()`

### Content Formatting
- Parts are separated by `\n` (single newline) during both streaming and API reload
- On reload, use `mapNotNull` to filter out empty/null parts before joining — avoids blank lines
- Bubble text padding: `horizontal = 10.dp, vertical = 6.dp`
- Bubble `lineHeight = 16.sp` with `fontSize = 14.sp` for a compact but readable look
- List `contentPadding = (8.dp, 6.dp)` with `spacedBy(6.dp)` between bubbles

### URL Handling
- Normalize user-entered URLs in `ConnectViewModel`: prepend `http://` if no scheme present
- Saved `ConnectionConfig.baseUrl` always has scheme (normalized before save)
## Building
```bash
export JAVA_HOME=$HOME/.jdks/jdk-21.0.11+10
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$PATH
cd thin-client && ./gradlew assembleDebug
```
