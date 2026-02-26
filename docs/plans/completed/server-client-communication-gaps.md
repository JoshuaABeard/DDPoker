# Server-Client Communication Gap Fixes

Status: completed

## Summary

Implemented all 9 server-client communication gaps in priority order.

## Changes

### Gap 1 — Rebuy/Addon Decline (Critical)
- **WebSocketTournamentDirector.java**: Added `ScheduledExecutorService` that auto-sends decline after timeout when user ignores rebuy/addon button in normal UI flow
- `doRebuy()`/`doAddon()` cancel the pending decline timer when user accepts
- Timer is cleaned up in `finish()`

### Gap 9 — Remove Optimistic Rebuy Update
- **WebSocketTournamentDirector.java**: Removed `player.addRebuy()` from `doRebuy()` and `player.addAddon()` from `doAddon()` — server's PLAYER_REBUY/PLAYER_ADDON message is now the sole update path

### Gap 3 — Advance Actions for WS Mode
- **AdvanceAction.java**: Added extended `getAdvanceActionWS()` with `canBet`, `maxBet`, `maxRaise` parameters; `_getAdvanceActionWS()` now handles bet, raise, betpot, raisepot
- **WebSocketTournamentDirector.java**: Updated caller to pass additional fields from `ActionOptionsData`

### Gap 2 — Lobby Settings Changed
- **Lobby.java**: `LOBBY_SETTINGS_CHANGED` handler now deserializes `LobbySettingsChangedData` and calls `updateFromLobbySettings()`
- **WebSocketTournamentDirector.java**: Improved logging in settings changed handler

### Gap 8 — Absolute Chip Values in Delta Messages
- **ServerMessageData.java**: Added `Integer chipCount` to `PlayerRebuyData`, `PlayerAddonData`, `WinnerData`; added `Integer fromChipCount`/`toChipCount` to `ChipsTransferredData`
- **GameEventBroadcaster.java**: Populates absolute chip counts from server state when constructing messages; added `lookupPlayerChips()` and `lookupPlayerChipsAtTable()` helpers
- **WebSocketTournamentDirector.java**: All handlers prefer absolute chip values when present

### Gap 5 — Sequence Gap Recovery
- **ClientMessageType.java**: Added `REQUEST_STATE`
- **WebSocketGameClient.java**: Added `sendRequestState()` and auto-sends it on gap detection
- **InboundMessageRouter.java**: Handles `REQUEST_STATE` by sending fresh `GAME_STATE` to the requesting connection

### Gap 6 — PLAYER_MOVED for Table Consolidation
- **ServerMessageType.java**: Added `PLAYER_MOVED`
- **ServerMessageData.java**: Added `PlayerMovedData(playerId, playerName, fromTableId, toTableId)`
- **GameEventBroadcaster.java**: Broadcasts `PLAYER_MOVED` instead of suppressing `PLAYER_LEFT` during consolidation
- **WebSocketTournamentDirector.java**: Added `onPlayerMoved()` handler — removes player from origin table, shows notification

### Gap 7 — PLAYER_RECONNECTED Distinction
- **ServerMessageData.java**: Added `boolean isReconnect` to `PlayerJoinedData`
- **OutboundMessageConverter.java**: Added overload for `createPlayerJoinedMessage()` with `isReconnect`
- **GameWebSocketHandler.java**: Passes `reconnecting` flag when broadcasting PLAYER_JOINED
- **GameEventBroadcaster.java**: Sets `isReconnect=false` for new joins
- **WebSocketTournamentDirector.java**: Shows "(reconnected)" vs "(joined)" in chat

### Gap 4 — TIMER_UPDATE Broadcasting
- **GameEventBroadcaster.java**: Added `ScheduledExecutorService` for periodic TIMER_UPDATE broadcasts every 5 seconds during action timeouts; cancelled on PlayerActed/ActionTimeout
- **GameWebSocketHandler.java**: Starts action timer when ACTION_REQUIRED is sent

## Test Impact
- Updated `GameEventBroadcasterTest.playerRemoved_activePlayer_sendsPlayerMoved` (was `suppressesPlayerLeft`)
- All 205 directly affected tests pass (0 failures, 0 errors)
