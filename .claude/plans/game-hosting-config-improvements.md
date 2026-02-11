# Game Hosting Configuration Improvements

## Context

The host's tournament configuration options (via `TournamentProfile` and `TournamentProfileDialog`) are functional but haven't evolved with modern poker platform expectations. Key limitations include low online player caps, no late registration, no bounty support, no blind schedule generation tools, and a starting chip cap of 50,000. This plan catalogs all improvement opportunities, prioritized with an online-play focus.

## Current Architecture

- **Data model**: `TournamentProfile.java` — stores all settings in a `DMTypedHashMap` with string keys
- **UI**: `TournamentProfileDialog.java` — 5-tab Swing dialog (Description, Levels, Details, Opponent Mix, Online)
- **Engine**: `PokerGame.java` (runtime state), `TournamentDirector.java` (online orchestration), `GameClock.java` (level timing)
- **Constants**: `PokerConstants.java` — game types, payout types, option keys
- **HTML summary**: `TournamentProfileHtml.java` — renders profile for display

---

## Improvements Catalog

### P1 — Quick Wins (very low effort, high confidence)

#### 1. Raise MAX_ONLINE_PLAYERS (30 → 90)
- **Why**: The existing code comment says `// TODO: is this too few?`. 30 players (3 tables) is very limiting. The table assignment/balancing logic already handles arbitrary table counts.
- **Change**: `TournamentProfile.java` line ~76: change `MAX_ONLINE_PLAYERS = 30` to `90`
- **Risk**: Network performance at higher counts needs testing

#### 2. Raise MAX_CHIPS (50,000 → 1,000,000)
- **Why**: Modern deep-stack tournaments use 100K+ starting chips. `MAX_REBUY_CHIPS` is already 1M, so the engine handles large values.
- **Change**: `TournamentProfile.java` line ~79: change `MAX_CHIPS` constant

#### 3. Display Starting Depth (big blinds)
- **Why**: "Starting stack: 1,500 / BB: 30 = 50 BBs deep" is the most important structure metric. Hosts currently do this math manually.
- **Change**: Add a calculated label in `TournamentProfileDialog.java` (levels or details tab) showing `buyinChips / bigBlind[level0]`

#### 4. Profile Import/Export
- **Why**: Hosts can't share tournament structures. Profiles already serialize via `marshal()`/`demarshal()`.
- **Change**: Add Import/Export buttons to `TournamentOptions.java` / `ProfileList` using `JFileChooser` + existing serialization

#### 5. Per-Level Duration Clarity
- **Why**: Each level can already have its own minutes override, but the UI doesn't make this obvious.
- **Change**: Add tooltip/help text on the minutes column header in the levels tab

---

### P2 — Online Experience (medium effort, high impact)

#### 6. Late Registration
- **Why**: Standard on every modern platform. Without it, all players must be present before the game starts.
- **New params**: `PARAM_LATE_REG` (boolean), `PARAM_LATE_REG_UNTIL` (level number), `PARAM_LATE_REG_CHIPS` ("starting" or "average")
- **UI**: New section in Online tab with enable checkbox, until-level spinner, chip mode radio
- **Engine**: `TournamentDirector` accepts join messages after tournament start up to cutoff level. New player gets assigned to a table with an empty seat. Prize pool recalculates dynamically (already does via `getTotalSpent()`).

#### 7. Scheduled Start Time
- **Why**: Currently host must manually click "Start". A scheduled time lets players join at their leisure.
- **New params**: `PARAM_SCHEDULED_START` (boolean), `PARAM_START_TIME` (long millis), `PARAM_MIN_PLAYERS` (int, default 2)
- **UI**: Date/time input + min-players spinner in Online tab
- **Engine**: `TournamentDirector` checks `currentTime >= startTime && players >= minPlayers` in registration phase loop

#### 8. Raise MAX_OBSERVERS (10 → 30)
- **Why**: 10 observers is restrictive for community events. The testing constant is already 30.
- **Change**: `TournamentProfile.java`: change `MAX_OBSERVERS = 10` to `30`

#### 9. Per-Street Action Timeouts
- **Why**: Players need less time pre-flop, more time on river. Many platforms offer this.
- **New params**: `PARAM_TIMEOUT_PREFLOP`, `PARAM_TIMEOUT_FLOP`, `PARAM_TIMEOUT_TURN`, `PARAM_TIMEOUT_RIVER` — all optional, default to `PARAM_TIMEOUT`
- **UI**: Collapsible "Advanced Timeout" section in Online tab
- **Engine**: Timeout lookup checks current round and uses the appropriate per-street value

#### 10. Increase Think Bank Max (60 → 120s)
- **Why**: 60 seconds of total think bank time is very tight for multi-hour tournaments.
- **Change**: `TournamentProfile.java`: change `MAX_THINKBANK` to `120`

---

### P3 — Tournament Structure (medium effort)

#### 11. Blind Level Quick Setup / Templates
- **Why**: Manually entering up to 40 levels is the most tedious part of tournament setup.
- **UI**: "Quick Setup" button opens a dialog with: starting blinds, speed preset (Slow/Standard/Turbo/Hyper), include antes checkbox, include breaks checkbox
- **Logic**: Generate levels following standard ~1.5-2x progression, populate existing level params
- **No data model changes** — generated levels use existing storage

#### 12. Standard Payout Presets
- **Why**: Current "Auto (Fibonacci)" is a single algorithm. Platforms offer top-heavy, standard, and flat distributions.
- **UI**: "Presets" dropdown next to allocation radio buttons: Top-Heavy (~50% to winner), Standard (~25-30%), Flat (~15-20%)
- **No data model changes** — presets populate existing spot percentage fields

#### 13. Bounty/Knockout Tournament Support
- **Why**: Extremely popular format. Purely a payout-side feature — doesn't change hand gameplay.
- **New params**: `PARAM_BOUNTY` (boolean), `PARAM_BOUNTY_AMOUNT` (int, ≤ buyin cost)
- **UI**: Bounty checkbox + amount spinner in Details tab payout section
- **Engine**: On elimination in `HoldemHand`/`PokerGame`, award bounty to eliminating player. `PokerPlayer` gets `bountyWon` field. Prize pool label shows "Prize Pool: $X (after $Y in bounties)".

#### 14. Profile Validation Warnings
- **Why**: "Verify" button only normalizes data. Doesn't warn about strategically questionable structures.
- **Checks**: Unreachable levels, payout spots > player count, rebuy level after last level, starting depth < 10 BBs
- **UI**: Warning icons on tabs with issues (infrastructure already exists via `error` ImageIcon)

---

### P4 — Nice to Have (lower priority)

#### 15. Hands-Per-Level Advancement
- **Why**: In offline play, minutes-per-level depends on hands/hour settings. Hands-per-level is more consistent.
- **New params**: `PARAM_LEVEL_ADVANCE_MODE` ("time"/"hands"), `PARAM_HANDS_PER_LEVEL` (int)
- **Engine**: Parallel code path in `PokerGame` level transition logic

#### 16. Raise MAX_TIMEOUT (60 → 120s)
- **Why**: 60s max can feel tight for complex tournament decisions.
- **Change**: `TournamentProfile.java`: change `MAX_TIMEOUT` to `120`

#### 17. Configurable Table Size Default
- **Why**: 6-max and short-handed formats are popular. Table seats is configurable (2-9) but defaults to 10 which seems wrong (profile default). Making the default more prominent or adding format presets (Full Ring/6-Max/Heads-Up) would help.
- **UI**: Radio group or dropdown for format in Details tab, sets table seats automatically

---

## Key Files to Modify

| File | Changes |
|------|---------|
| `code/pokerengine/src/main/java/.../model/TournamentProfile.java` | New params, raised constants |
| `code/poker/src/main/java/.../TournamentProfileDialog.java` | New UI controls per feature |
| `code/pokerengine/src/main/java/.../engine/PokerConstants.java` | New option keys, constants |
| `code/pokerengine/src/main/java/.../engine/TournamentProfileHtml.java` | Display new settings in summary |
| `code/poker/src/main/java/.../PokerGame.java` | Bounty tracking, hands-per-level |
| `code/poker/src/main/java/.../online/TournamentDirector.java` | Late registration, scheduled start |
| `code/poker/src/main/java/.../TournamentOptions.java` | Profile import/export buttons |
| `code/pokerengine/src/main/java/.../engine/PokerPlayer.java` | Bounty won tracking |

## Verification

- Existing tests pass after each change
- Manual testing: create tournament with new options, verify they persist across save/load
- Online testing: host a game with late registration / scheduled start enabled, verify join flow
- Profile import/export: export a profile, delete it, re-import, verify identical
- Blind template generation: generate a turbo structure, verify level progression is reasonable
