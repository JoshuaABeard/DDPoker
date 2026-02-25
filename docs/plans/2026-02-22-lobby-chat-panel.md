# Plan: Lobby Chat UI Panel

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

## Context
The server has a fully implemented `/ws/lobby` WebSocket endpoint and
`LobbyChatWebSocketClient` transport, but no Swing UI displays or sends lobby
chat. Users on the "Find Games" screen have no way to see or participate in
pre-game lobby chat. This plan wires the existing transport to a new chat panel
embedded in `FindGames`.

---

## Worktree
```bash
git worktree add -b feature-lobby-chat-panel ../DDPoker-feature-lobby-chat-panel
```

---

## Files

### Create
| File | Purpose |
|------|---------|
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/LobbyChatPanel.java` | New Swing panel |
| `code/poker/src/test/java/com/donohoedigital/games/poker/online/LobbyChatPanelTest.java` | Unit tests |

### Modify
| File | Change |
|------|--------|
| `code/poker/src/main/java/com/donohoedigital/games/poker/online/FindGames.java` | Override `getExtra()`, `start()`, `finish()` |
| `code/poker/src/main/resources/config/poker/client.properties` | Add lobby chat message keys |

---

## `LobbyChatPanel.java`

**Package:** `com.donohoedigital.games.poker.online`
**Copyright:** Community (new file — Template 3)
**Extends:** `DDPanel`
**Implements:** `LobbyChatWebSocketClient.LobbyMessageListener`

### Layout
```
LobbyChatPanel (BorderLayout, gap 5,0)
├── NORTH: DDLabelBorder ("lobbychat", STYLE)
│            └── statusLabel_ (DDLabel, right-aligned)
├── CENTER: ChatListPanel (VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER)
└── SOUTH: DDPanel (input row, gap 0,5)
    ├── CENTER: DDTextField msg_ (500-char limit, regex "^.+$")
    └── EAST:   GlassButton "lobbychat.send"
```

### Constructor
```java
public LobbyChatPanel(GameContext context, String style, String bevelStyle, String localPlayerName)
```
- Creates `ChatListPanel` (max 100 messages, same constructor signature as in `ChatPanel`)
- Reads colors from `StylesConfig`:
  `cLocal_` ← `Chat.local`, `cRemote_` ← `Chat.remote`, `cSystem_` ← `Chat.director`
- Creates `LobbyChatWebSocketClient(this)` and stores it

### Public API
```java
void connect(String serverUrl, String jwt)   // delegates to wsClient_.connect(...)
void disconnect()                            // delegates to wsClient_.disconnect()
```

### `LobbyMessageListener` implementation
All callbacks dispatch to EDT via `SwingUtilities.invokeLater`:

| Callback | Action |
|----------|--------|
| `onConnected()` | Update `statusLabel_` text to `msg.lobbychat.status.connected`; enable `msg_` and send button |
| `onDisconnected()` | Update `statusLabel_` text to `msg.lobbychat.status.disconnected`; disable `msg_` and send button |
| `onPlayerList(players)` | Post system msg listing online count; no per-player message (avoids spam on connect) |
| `onPlayerJoined(id, name)` | Display `msg.lobbychat.join` system message |
| `onPlayerLeft(id, name)` | Display `msg.lobbychat.leave` system message |
| `onChatReceived(id, name, msg)` | Display `msg.lobbychat.msg` — color `cLocal_` if `name.equals(localPlayerName_)`, else `cRemote_` |

### `sendChat()` (private)
```
encode msg text with Utils.encodeHTML
call wsClient_.sendChat(encoded)
display as local message immediately (optimistic — same as ChatPanel)
clear msg_ text field
```

### `displayMessage(String html)` (private)
```
ChatPanel.ChatMessage msg = new ChatPanel.ChatMessage(html, -1)
chatList_.displayMessage(msg)
```

---

## `FindGames.java` changes

Add field:
```java
private LobbyChatPanel lobbyChat_;
```

Override `getExtra()` — called once by `ListGames.init()` after `context_` and `STYLE` are set:
```java
@Override
protected JComponent getExtra() {
    lobbyChat_ = new LobbyChatPanel(context_, STYLE, "BrushedMetal", profile_.getName());
    return lobbyChat_;
}
```

Override `start()`:
```java
@Override
public void start() {
    super.start();
    EmbeddedGameServer srv = ((PokerMain) engine_).getEmbeddedServer();
    if (srv != null && srv.isRunning()) {
        lobbyChat_.connect("http://localhost:" + srv.getPort(), srv.getLocalUserJwt());
    }
}
```

Override `finish()`:
```java
@Override
public void finish() {
    super.finish();
    if (lobbyChat_ != null) {
        lobbyChat_.disconnect();
    }
}
```

---

## `client.properties` additions

```properties
# Lobby chat panel
msg.lobbychat.panel.title=Lobby Chat
msg.lobbychat.status.connected=\u25CF Connected
msg.lobbychat.status.disconnected=\u25CB Reconnecting\u2026
msg.lobbychat.online=<font color="{0}">{1} player(s) in lobby.</font>
msg.lobbychat.join=<font color="{0}"><B>{1}</B> joined the lobby.</font>
msg.lobbychat.leave=<font color="{0}"><B>{1}</B> left the lobby.</font>
msg.lobbychat.msg=<font color="{0}"><B>{1}</B>: {2}</font>
```

Also need a label key for the send button:
```properties
lobbychat.send.text=Send
lobbychat.send.tooltip=Send chat message
```

And the panel border label key (DDLabelBorder uses key `"lobbychat"`):
```properties
lobbychat.text=Lobby Chat
```

---

## `LobbyChatPanelTest.java`

Tests exercise the listener callbacks directly without a real WebSocket:
1. `testChatReceivedFromRemote` — call `onChatReceived` for a remote player, verify one message added
2. `testChatReceivedFromLocal` — call `onChatReceived` with local player name, verify local color in message
3. `testPlayerJoinedAddsSystemMessage` — call `onPlayerJoined`, verify message added
4. `testPlayerLeftAddsSystemMessage` — call `onPlayerLeft`, verify message added
5. `testPlayerListDisplaysOnlineCount` — call `onPlayerList` with 3 players, verify message added
6. `testStatusLabelUpdatesOnConnect` — call `onConnected`/`onDisconnected`, verify status label text
7. `testSendButtonDisabledWhenDisconnected` — verify send button starts disabled before connect

Each test creates the panel in headless mode with a no-op `GameContext` stub or uses
`SwingUtilities.invokeAndWait` to flush pending EDT tasks after each callback.

---

## Verification
1. Build: `mvn test -P dev -pl poker` (all existing tests must pass)
2. New tests: `mvn test -pl poker -Dtest=LobbyChatPanelTest`
3. Manual smoke test:
   - Launch the app, navigate to Find Games
   - Confirm the Lobby Chat panel appears below the game list
   - Confirm the status shows "Connected" once the embedded server starts
   - Send a chat message and confirm it appears in the list
   - Observe join/leave messages when a second client connects
