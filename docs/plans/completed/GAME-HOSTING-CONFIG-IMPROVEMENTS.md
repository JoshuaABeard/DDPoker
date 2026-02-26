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

### ✅ P1 — Quick Wins (COMPLETED Feb 12, 2026 — commit 797d5a1)

#### ✅ 1. Raise MAX_ONLINE_PLAYERS (30 → 90)
- **Why**: The existing code comment says `// TODO: is this too few?`. 30 players (3 tables) is very limiting. The table assignment/balancing logic already handles arbitrary table counts.
- **Change**: `TournamentProfile.java` line 75: changed `MAX_ONLINE_PLAYERS = 30` to `90`
- **Risk**: Network performance at higher counts needs testing

#### ✅ 2. Raise MAX_CHIPS (50,000 → 1,000,000)
- **Why**: Modern deep-stack tournaments use 100K+ starting chips. `MAX_REBUY_CHIPS` is already 1M, so the engine handles large values.
- **Change**: `TournamentProfile.java` line 78: changed `MAX_CHIPS` constant to 1,000,000

#### ✅ 3. Display Starting Depth (big blinds)
- **Why**: "Starting stack: 1,500 / BB: 30 = 50 BBs deep" is the most important structure metric. Hosts currently do this math manually.
- **Change**: Added calculated label in `TournamentProfileDialog.java` buy-in section showing `buyinChips / bigBlind[level0]`, plus display in HTML summary

#### ✅ 4. Profile Import/Export
- **Why**: Hosts can't share tournament structures. Profiles already serialize via `marshal()`/`demarshal()`.
- **Change**: Added Import/Export buttons to `TournamentOptions.java` / `ProfileList` using `JFileChooser` + `.ddprofile` extension

#### ✅ 5. Per-Level Duration Clarity
- **Why**: Each level can already have its own minutes override, but the UI doesn't make this obvious.
- **Change**: Shortened header to "Mins", added tooltip, updated help text to mention blank fields use default

#### ✅ 8. Raise MAX_OBSERVERS (10 → 30)
- **Why**: 10 observers is restrictive for community events. The testing constant is already 30.
- **Change**: `TournamentProfile.java` line 76: changed `MAX_OBSERVERS = 10` to `30`

#### ✅ 10. Increase Think Bank Max (60 → 120s)
- **Why**: 60 seconds of total think bank time is very tight for multi-hour tournaments.
- **Change**: `TournamentProfile.java` line 92: changed `MAX_THINKBANK` to `120`

#### ✅ 16. Raise MAX_TIMEOUT (60 → 120s)
- **Why**: 60s max can feel tight for complex tournament decisions.
- **Change**: `TournamentProfile.java` line 91: changed `MAX_TIMEOUT` to `120`

---

### P2 — Online Experience (medium effort, high impact)

#### ✅ 6. Late Registration (COMPLETED — commit 6780456)
- **Why**: Standard on every modern platform. Without it, all players must be present before the game starts.
- **Change**: Added PARAM_LATE_REG, PARAM_LATE_REG_UNTIL, PARAM_LATE_REG_CHIPS with UI controls in Online tab and server logic in TournamentDirector.

#### ✅ 7. Scheduled Start Time (COMPLETED)
- **Why**: Currently host must manually click "Start". A scheduled time lets players join at their leisure.
- **Change**: Added PARAM_SCHEDULED_START (boolean), PARAM_START_TIME (long millis), PARAM_MIN_PLAYERS_START (int, default 2) with "hours from now" UI control in Online tab. TournamentDirector auto-starts when time and player conditions are met.

#### ✅ 9. Per-Street Action Timeouts (COMPLETED)
- **Why**: Players need less time pre-flop, more time on river. Many platforms offer this.
- **New params**: `PARAM_TIMEOUT_PREFLOP`, `PARAM_TIMEOUT_FLOP`, `PARAM_TIMEOUT_TURN`, `PARAM_TIMEOUT_RIVER` — all optional, default to `PARAM_TIMEOUT`
- **UI**: Collapsible "Advanced Timeout" section in Online tab
- **Engine**: Timeout lookup checks current round and uses the appropriate per-street value

---

### P3 — Tournament Structure (medium effort)

#### ✅ 11. Blind Level Quick Setup / Templates (COMPLETED)
- **Why**: Manually entering up to 40 levels is the most tedious part of tournament setup.
- **Implemented**:
  - `BlindTemplate` enum with 4 templates: SLOW (1.5x, 20min), STANDARD (2x, 15min), TURBO (2x, 10min), HYPER (2x, 5min)
  - `BlindQuickSetupDialog` with template selection, num levels (1-40), include breaks, break frequency
  - Preview display showing first few generated levels
  - "Quick Setup" button integrated in TournamentProfileDialog Levels tab
  - `generateLevels()` method with progressive blind structure and optional breaks
  - All 17 template tests passing, 1536 total tests passing

#### ✅ 12. Standard Payout Presets (COMPLETED)
- **Why**: Current "Auto (Fibonacci)" is a single algorithm. Platforms offer top-heavy, standard, and flat distributions.
- **Implemented**:
  - `PayoutPreset` enum with 4 presets: CUSTOM, TOP_HEAVY (50/30/20), STANDARD (40/25/17.5/12.5/5), FLAT (25/20/15/12.5/10/7.5/5/5)
  - UI dropdown in allocation section of DetailsTab
  - `applyPayoutPreset()` method updates profile and refreshes UI
  - Bug fix: Added `setAlloc(ALLOC_PERC)` to properly set allocation mode
  - All 19 preset tests passing, 1536 total tests passing

#### ✅ 13. Bounty/Knockout Tournament Support (COMPLETED)
- **Why**: Extremely popular format. Purely a payout-side feature — doesn't change hand gameplay.
- **Implemented**:
  - `MAX_BOUNTY = 10000` constant in TournamentProfile
  - `PARAM_BOUNTY` (boolean) and `PARAM_BOUNTY_AMOUNT` (int) parameters
  - `PokerPlayer` fields: `nBountyCollected_` and `nBountyCount_`
  - `addBounty()` method in PokerPlayer (adds to prize total)
  - Bounty UI in DetailsTab with checkbox and amount spinner
  - Bounty awarding logic in HoldemHand.java on elimination
  - Full serialization support for bounty data
  - All 1536 tests passing

#### ✅ 14. Profile Validation Warnings (COMPLETED)
- **Why**: "Verify" button only normalizes data. Doesn't warn about strategically questionable structures.
- **Implemented**:
  - Validation backend complete: `ValidationWarning` enum, `ValidationResult` class, `ProfileValidator.validateProfile()`
  - All 4 warning checks: unreachable levels, too many payout spots, shallow starting depth, excessive house take
  - UI displays warnings as status text in DetailsTab (orange color, HTML formatted)
  - Warnings visible but don't block profile saving (soft warnings)
  - All 28 validation tests passing, 1536 total tests passing

---

### P4 — Nice to Have (lower priority)

#### ✅ 15. Hands-Per-Level Advancement (COMPLETED)
- **Why**: In offline play, minutes-per-level depends on hands/hour settings. Hands-per-level is more consistent.
- **Implemented**:
  - Created `LevelAdvanceMode` enum (TIME/HANDS)
  - Added `PARAM_LEVEL_ADVANCE_MODE` and `PARAM_HANDS_PER_LEVEL` to TournamentProfile
  - Track hands played in current level in PokerGame
  - Auto-advance levels when hand count reached in HANDS mode
  - Added UI controls in Levels tab: radio buttons for mode selection, conditional spinner for hands per level
  - All 1509 tests passing

#### ✅ 17. Configurable Table Size Default
- **Why**: 6-max and short-handed formats are popular. Table seats is configurable (2-9) but defaults to 10 which seems wrong (profile default). Making the default more prominent or adding format presets (Full Ring/6-Max/Heads-Up) would help.
- **Change**: Replaced spinner with 3 radio buttons (Full Ring 10 / 6-Max 6 / Heads-Up 2) in Details tab. Updated HTML summary to show format name.

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
