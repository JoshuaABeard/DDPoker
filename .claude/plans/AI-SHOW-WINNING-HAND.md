# AI Show Winning Hand (Configurable)

**Created:** 2026-02-15
**Status:** draft
**Estimated Effort:** 4-6 hours

---

## Problem Statement

When an AI player wins a pot uncontested (all other players fold), the AI always mucks its cards -- the winning hand is never shown to the table. This is because `PokerPlayer.bShowWinning_` defaults to `false` and is reset to `false` every hand in `newHand()`.

The user wants a **configurable tournament setting** that causes AI players to always show their hand when they win uncontested pots. This makes the game more informative and entertaining for human players.

## Existing "Show Winning Hand" Cheat Option -- Why It's Not Enough

There is already a cheat option (`OPTION_CHEAT_SHOWWINNINGHAND = "showdown"`) that visually reveals the winner's hand. However, it is fundamentally different from what we need:

| Aspect | Existing Cheat Option | What We Want |
|--------|----------------------|--------------|
| **Level** | Display-only -- visually flips cards in `Showdown.displayShowdown()` | Game-logic level -- AI decides to show, setting `cardsExposed` in `resolvePot()` |
| **Scope** | Per-user local preference (stored in OS registry via `java.util.prefs`) | Per-tournament setting (stored in `TournamentProfile`, travels with the profile) |
| **Online games** | **Forced OFF** -- `isCheatOn()` hard-blocks all cheats online | **Available** -- tournament profile settings work online, controlled by host |
| **Visibility** | Only the local player sees the cards (client-side display hack) | All players at the table see the cards (game-state change) |
| **Chat messages** | Uses "uncontested" chat key (no hand info shown) | Uses "uncontested.show" chat key (hand info displayed) |

The cheat option is a local visual override that doesn't work online. We need a proper tournament-level setting where the **host** configures it and AI players actually expose their cards as part of the game state.

## Current Card Exposure Flow

1. **`PokerPlayer` defaults** (`poker/.../PokerPlayer.java:95-100`):
   - `bShowWinning_ = false` -- never voluntarily shows winning hands in uncontested pots
   - Reset every hand in `newHand()` (line 870)

2. **`HoldemHand.resolvePot()`** (`poker/.../HoldemHand.java:2697`):
   ```java
   if (!bUncontested || player.isShowWinning())
       player.setCardsExposed(true);
   ```
   In contested pots, winners always have cards exposed. In uncontested pots, only if `isShowWinning()` is `true`.

3. **`Showdown.displayShowdown()`** (`poker/.../Showdown.java:96-240`): Combines the `isCardsExposed()` flag with cheat options for final display.

4. **Chat display** (`HoldemHand.displayChatPot()` line 379): Uses `"uncontested.show"` key when player shows, `"uncontested"` when they don't.

**Result:** AI winners in uncontested pots never show their cards at the game-logic level.

## Proposed Solution

### Add a Tournament Profile Setting

Add a new boolean tournament profile option `PARAM_AI_SHOW_WINNING = "aishowwin"` that the host can configure when setting up the game/tournament. When enabled, AI players will have `showWinning` set to `true` each hand, causing `resolvePot()` to expose their cards in uncontested pots.

This follows the exact same pattern as existing profile options like `PARAM_ALLOW_DASH`, `PARAM_ALLOW_ADVISOR`, and `PARAM_FILL_COMPUTER`.

### What This Achieves

- **Contested pots:** No change -- winners already show their cards.
- **Uncontested pots (AI wins, setting ON):** AI's winning hand is exposed at the game-logic level, visible to all players, with hand info shown in chat.
- **Uncontested pots (AI wins, setting OFF):** Same as today -- AI mucks.
- **Uncontested pots (human wins):** No change -- humans still get the "Show hand?" dialog (if configured).
- **All-in showdowns:** No change -- all cards are already exposed unconditionally.
- **Losing hands:** No change -- AI still mucks losing hands.
- **Online games:** Setting travels with the tournament profile; host controls it.

### Where in the UI

The option fits in the **OnlineTab's "players" section** of `TournamentProfileDialog`, alongside related settings like "Fill With Computer Players", "Enable Dashboard Items", and "Enable Dashboard Advisor". These all control aspects of the AI/game experience that the host configures.

However, this setting is also relevant for practice/offline games. The OnlineTab options are still accessible when configuring practice tournaments, so this placement works for both modes.

## Implementation Steps

- [ ] 1. **TournamentProfile**: Add `PARAM_AI_SHOW_WINNING` constant and `isAIShowWinning()` getter
- [ ] 2. **client.properties**: Add `option.aishowwin.label`, `.default`, `.help` entries
- [ ] 3. **TournamentProfileDialog**: Add `OptionBoolean` checkbox in OnlineTab's players panel
- [ ] 4. **PokerPlayer.newHand()**: Read the profile setting and call `setShowWinning(true)` for AI players when enabled
- [ ] 5. **Unit tests**: Test the profile setting, the `newHand()` behavior, and card exposure in uncontested pots
- [ ] 6. **Manual testing**: Create tournament with setting on/off, verify AI shows/mucks in uncontested wins
- [ ] 7. **Verify no regressions**: Contested pots, all-in showdowns, human player behavior, online play

## Files Affected

| File | Change |
|------|--------|
| `code/pokerengine/.../model/TournamentProfile.java` | Add `PARAM_AI_SHOW_WINNING` constant + `isAIShowWinning()` getter |
| `code/poker/.../resources/config/poker/client.properties` | Add option label, default (false), and help text |
| `code/poker/.../TournamentProfileDialog.java` | Add `OptionBoolean` in OnlineTab players panel |
| `code/poker/.../PokerPlayer.java` | Modify `newHand()` to check profile setting for AI players |
| Test files (new or existing) | Unit tests for profile setting and card exposure behavior |

## Serialization / Online Compatibility

No serialization changes needed. `TournamentProfile` stores all settings in a `DMTypedHashMap` which automatically marshals/demarshals all key-value pairs. New keys are automatically:
- Saved to disk with the tournament profile
- Sent to all clients as part of game state
- Loaded from saved games (missing keys get the default value from `client.properties`)

Old clients that don't have this code will simply ignore the unknown key in the map. Old profiles that lack the key will default to `false` (current behavior preserved).

## Decisions (Resolved)

- **Default value:** `false` (off by default, preserves existing behavior)
- **UI label:** "AI Shows Winning Hand"
- **UI placement:** OnlineTab's "players" section in TournamentProfileDialog
