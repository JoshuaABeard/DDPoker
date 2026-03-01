# Web Client Core Gameplay Polish — Design

**Status:** COMPLETED (2026-02-28)
**Depends on:** `2026-02-25-web-client-parity-plan.md` (protocol fixes must be completed first)
**Scope:** 6 features that close the gap between the web client and desktop Java client for active gameplay

---

## Context

The web client already implements core poker gameplay (betting, hand progression, chat, game info panel). A feature comparison with the desktop Java Swing client revealed that several features are already working (sit out/come back, betting keyboard shortcuts F/C/R, side pots display, chip leader rankings in info panel). This design covers the **remaining gaps** in core gameplay polish.

## Feature 1: Admin Controls UI

### Problem
`sendAdminKick(playerId)`, `sendAdminPause()`, `sendAdminResume()` are fully implemented in `GameContext.tsx` (lines 194-205). The game state tracks `status === 'PAUSED'` and shows a "Game Paused" banner in `PokerTable.tsx` (lines 200-206). But there is **no UI** to trigger these actions — the admin has no buttons.

### Design

**Admin detection:**
- Add `isOwner: boolean` field to `GameState` (set from lobby ownership or GAME_STATE server message)
- The game creator/host is the admin; this role is already tracked in the lobby phase

**Kick UI (per PlayerSeat):**
- When `isOwner === true` and the seat is not the local player, show a small "X" icon button on hover over each `PlayerSeat`
- Click triggers a confirmation dialog ("Kick {playerName}?") → calls `sendAdminKick(playerId)`
- Icon: small, unobtrusive, positioned at the top-right corner of the seat

**Pause/Resume toolbar:**
- Visible only when `isOwner === true`
- Positioned near the top of `PokerTable`, adjacent to the "Game Paused" banner area
- Shows "Pause" button when `status === 'IN_PROGRESS'` → calls `sendAdminPause()`
- Shows "Resume" button when `status === 'PAUSED'` → calls `sendAdminResume()`
- Compact styling (small button, not prominent)

### Files
- Modify: `lib/game/gameReducer.ts` — add `isOwner` to state, set from game state messages
- Modify: `components/game/PlayerSeat.tsx` — add kick icon on hover (admin only)
- Modify: `components/game/PokerTable.tsx` — add pause/resume buttons (admin only)
- New: `components/ui/ConfirmDialog.tsx` — reusable confirmation dialog (~40 lines)

---

## Feature 2: Expanded Observer List

### Problem
`state.observers` array tracks `{ observerId, observerName }[]` and the reducer handles OBSERVER_JOINED/LEFT. But `PokerTable.tsx` (lines 125-131) only shows a count ("3 watching") with no way to see who is watching.

### Design

**ObserverPanel component:**
- Default state: compact counter "3 watching" (same as current)
- On click: expands to show a list of observer names
- If `isOwner`, each row shows a kick button
- Auto-collapses after 5 seconds of no interaction, or on click-away
- Position: top-left corner of table (where the counter already is)

### Files
- New: `components/game/ObserverPanel.tsx` (~60 lines)
- Modify: `components/game/PokerTable.tsx` — replace inline observer counter with `<ObserverPanel />`

---

## Feature 3: Chat Mute

### Problem
No mute infrastructure exists. The desktop client lets players mute individual users to hide their messages.

### Design

**Client-side only** — muting is a local preference, not broadcast to the server.

**State management:**
- `useMutedPlayers` hook backed by `localStorage` key `ddpoker-muted-{playerId}`
- Returns `{ mutedIds: Set<number>, mute(id), unmute(id), isMuted(id) }`

**Chat UI changes:**
- Each chat message shows a small mute icon (🔇) on hover next to the sender name
- Clicking mutes that player's ID
- Muted players' messages are filtered out in the render
- If any messages are hidden, show a subtle "N messages from muted players hidden" line at the bottom
- Clicking that line opens a muted players list for un-muting

### Files
- New: `lib/game/useMutedPlayers.ts` (~30 lines)
- Modify: `components/game/ChatPanel.tsx` — add mute icon, filter muted messages, hidden messages indicator

---

## Feature 4: Persistent Mini Chip Standings

### Problem
`GameInfoPanel.tsx` (lines 149-168) shows chip rankings sorted by count, but only when the info panel is toggled open with 'I'. Players lose awareness of standings during normal play.

### Design

**ChipLeaderMini component:**
- Always visible during gameplay (not behind a toggle)
- Shows top 3 players by chip count (compact format: "1. Name 15,000")
- If local player is not in top 3, shows their rank below ("You: 5th — 3,200")
- If ≤5 players remain, show all of them
- Positioned on the right side of the table, below `TournamentInfoBar`
- Updates live as chip counts change

### Files
- New: `components/game/ChipLeaderMini.tsx` (~50 lines)
- Modify: `components/game/PokerTable.tsx` — add `<ChipLeaderMini />` to table layout

---

## Feature 5: Betting Hotkey Improvements

### Problem
`ActionPanel.tsx` (lines 42-63) handles F (fold), C (check/call), R (raise/bet) keyboard shortcuts. Missing: Enter to confirm a pending bet, A for all-in, Escape to cancel pending bet.

### Design

**Additional key bindings in the existing keyboard handler:**
- **Enter**: When a pending bet/raise amount is active (slider visible), confirm the action
- **A**: Trigger all-in action if available in `availableActions`
- **Escape**: Cancel pending bet/raise, return to action buttons

### Files
- Modify: `components/game/ActionPanel.tsx` — extend `switch` statement in keyboard handler (~10 lines added)

---

## Feature 6: Hand History Export

### Problem
`HandHistory.tsx` displays up to 200 entries in an in-memory log. No way to save or share hand history.

### Design

**Export button:**
- Small download icon in the HandHistory panel header
- Click generates a plain text file formatted as standard poker hand history
- Format: one hand per block with hole cards, community cards, actions, winner
- Client-side download using `Blob` + `URL.createObjectURL`
- File named `hand-history-{gameId}-{date}.txt`

**Export utility:**
- `formatHandHistoryForExport(entries: HandHistoryEntry[]): string` in `lib/game/utils.ts`
- Converts internal entry format to human-readable text blocks

### Files
- Modify: `lib/game/utils.ts` — add `formatHandHistoryForExport()` (~30 lines)
- Modify: `components/game/HandHistory.tsx` — add export button in header

---

## Architecture Notes

- All features are additive — no existing functionality is changed
- Chat mute is client-only (localStorage); all other features use existing WebSocket messages
- Admin detection (`isOwner`) is the only new state field needed in the reducer
- All new components are small (<80 lines each), following existing patterns
- No new dependencies required
- Testing: each feature gets unit tests following existing Vitest + React Testing Library patterns
