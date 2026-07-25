# OpenCode Connect

An Android app that connects to `opencode serve` running on your PC, bringing OpenCode's AI coding assistant to your phone.

## Overview

OpenCode Connect is a native Android app (Kotlin + Jetpack Compose) that communicates with an [OpenCode](https://opencode.ai) server via its REST API and Server-Sent Events (SSE) streaming. It mirrors the core functionality of the desktop terminal interface in a mobile-friendly form.

**Package:** `com.opencode.thin`  
**Min SDK:** 29 (Android 10)  
**Target SDK:** 34 (Android 14)

## Architecture

The app follows an MVVM pattern with Hilt dependency injection:

```
thin-client/
  app/src/main/java/com/opencode/thin/
    OpencodeApp.kt          # @HiltAndroidApp
    MainActivity.kt         # Single activity, Jetpack Compose
    di/
      AppModule.kt          # Hilt DI: OkHttp, Retrofit, Json, SseClient
    data/
      model/Models.kt       # All data models matching opencode API types
      remote/
        OpencodeApi.kt      # Retrofit interface for all 40+ REST endpoints
        SseClient.kt        # OkHttp SSE client for event streaming
        ServerConfig.kt     # Mutable singleton for dynamic server URL/auth
        DynamicUrlInterceptor.kt  # OkHttp interceptor rewriting base URL
      repository/
        SessionRepository.kt
        ConfigRepository.kt
        FileRepository.kt
        ConnectionRepository.kt
    ui/
      theme/Theme.kt        # Orange-based dark theme with agent colors
      navigation/
        NavGraph.kt         # Jetpack Navigation Compose
        Screen.kt           # Route definitions
      screens/
        connect/            # Server connection with logo and auth
        chat/               # Main chat with SSE streaming, agents, models
        sessions/           # Session list grouped by date with swipe delete
        files/              # File browser with path copy
        providers/          # Provider list with search and connected status
        shell/              # Shell command execution
        git/                # Git diff viewer
```

## Features

### Connection
- Connects to any `opencode serve` instance via URL + basic auth
- Dynamic base URL switching using OkHttp interceptor
- Auto-prepends `http://` if no scheme provided in URL
- Secure password storage via DataStore
- ADB reverse tunnel support for USB-connected devices

### Chat
- Real-time AI response streaming via SSE
- Agent mode toggle: **BUILD** / **PLAN** (tappable chip in header)
- Current model indicator in header
- Model selector dialog with connected provider models
- Assign models per agent mode (BUILD / PLAN / Both)
- Colored bubbles: user=dark, build=blue (#5C9CF5), plan=orange (#F5A742)
- Text selection in bubbles (long press)
- Stop button (pulses during generation) to abort session
- Live streaming of tool output with `$ command` prefix
- Multiple assistant message handling (new bubble per message)
- Parts separated by `\n` (single newline) in both streaming and API reload
- Bubble padding: 10dp horizontal, 6dp vertical, lineHeight 16sp

### Sessions
- Session list grouped by date: Today, Yesterday, This Week, This Month, Older
- Sorted by last updated time
- Silent background refresh on resume
- Swipe-left to delete with confirmation dialog
- Cancel button in blue (#58A6FF)
- Date format: "d MMM, HH:mm" (e.g., "24 Jul, 13:21")

### Files
- Browse server files via `/file` API
- Shows server working directory in header
- Navigate into directories, go back via `..`
- Tap file path to copy to clipboard
- Retry on error

### Shell
- Run commands on the PC server via `/session/:id/shell`
- Auto-prefixes `command` to bypass shell aliases
- Skips prefix for shell builtins (cd, export, etc.) and variable assignments
- Shows `$ command` and output in chat

### Providers
- List all 170+ bundled providers from `/provider`
- Connected providers sorted to top with green badge
- Search by name or ID
- Set / Change API keys (blue button for "Set Key")

### Permissions & Questions
- **Permission dialogs** for agent requests (bash, edit, web_fetch, external_directory)
  - Allow Once / Always Allow / Deny buttons
  - Custom text input field for answers
- **Question modal** for `ask_user` tool prompts
  - Options with descriptions (tappable)
  - Text input for custom answers
  - Sends answer as new chat message (auto-aborts stuck session)

### Git
- View live working-tree diffs via `GET /vcs/diff?mode=git`
- Shows file name, status badge (added/modified/deleted), additions (+), deletions (-), and full patch content
- Current branch displayed in top bar
- **Stage All** — runs `git add -A` via shell endpoint
- **Unstage** — runs `git restore --staged .`
- **Commit** — dialog with message input, runs `git commit -m`
- **Discard** — confirmation dialog, runs `git restore .`

## Theme
- Primary: #CA4C07 (orange)
- Secondary: #EA6A24 (lighter orange)
- Background: #0D1117 (dark)
- Surface: #161B22, #21262D
- Build agent: #5C9CF5 (blue)
- Plan agent: #F5A742 (orange)
- Connected indicator: #3FB950 (green)
- Links/actions: #58A6FF (blue)
- Error: #F85149 (red)

## Setup

### On PC
```bash
# Start opencode server
OPENCODE_SERVER_PASSWORD=yourpassword opencode serve --hostname 0.0.0.0
```

### On Phone (WiFi)
1. Install `app-debug.apk` on your phone
2. Enter your PC's IP:port (e.g., `http://192.168.0.157:4096`) and password
3. Tap Connect

### On Phone (USB)
```bash
# Set up ADB reverse tunnel
adb reverse tcp:4096 tcp:4096

# Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk
```
Then enter `http://localhost:4096` in the app.

## Building
```bash
export JAVA_HOME=$HOME/.jdks/jdk-21.0.11+10
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$PATH

cd thin-client
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## Key Technical Details

### SSE Event Types Handled
| Event | Purpose |
|---|---|
| `server.connected` | Initial connection |
| `message.updated` | Message created/updated |
| `message.part.delta` | Streaming text deltas |
| `message.part.updated` | Complete parts (tool output, reasoning) |
| `session.idle` / `session.status` | Session completion |
| `permission.asked` | Agent permission requests |
| `permission.replied` | Permission response confirmation |
| `question.asked` | Agent question modal |
| `session.diff` | Session diff updates |

### SSE Reconnection and Deduplication
The opencode server replays all events from the beginning whenever a new SSE connection is established. Over WiFi, connections can drop frequently, causing replayed events to produce garbled or duplicated text. The app tracks processed event IDs in a set (`seenEventIds`) and skips any event whose ID has already been seen. The set is cleared on session load.

### Dynamic URL Handling
The app uses a placeholder base URL (`http://placeholder/`) in Retrofit, with a `DynamicUrlInterceptor` that rewrites requests to the actual server URL stored in `ServerConfig`. This avoids recreating the Retrofit instance when the user changes servers.

### Alias Bypass
Shell commands are auto-prefixed with `command` to bypass fish shell aliases (e.g., `ls` is aliased to `eza -al`). Known shell builtins are excluded from this prefix.

## Troubleshooting

### "Failed to connect" on WiFi
- Ensure PC and phone are on the same network
- Check firewall isn't blocking port 4096
- Use ADB reverse tunnel instead: `adb reverse tcp:4096 tcp:4096`

### 400 Bad Request on session creation
The opencode server rejects `null` in JSON bodies. The app uses `explicitNulls = false` in kotlinx.serialization to omit null fields.

### Tool output not showing
Tool parts use `message.part.updated` events with output in `state.output`. The handler extracts both text parts and tool parts. On reload, content is extracted from `state.output` and `state.input.command`.

### "NoSuchMethodError: removeLast()"
`List.removeLast()` is a Java 21 API not available on Android. Use `removeAt(list.lastIndex)` instead.
