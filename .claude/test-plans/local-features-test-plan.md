# DD Poker — Local Features Test Plan

**Date:** 2026-02-22
**Scope:** All local/offline features (no online multiplayer)
**Platform:** Windows desktop (Java Swing)

---

## How to Use This Plan

- Work through sections in order; later sections often depend on earlier setup.
- Each test case has an **ID**, **Steps**, and **Expected Result**.
- Mark each as **PASS**, **FAIL**, or **SKIP** (with reason).
- File issues for any FAIL before moving on.

---

## Section 1 — Application Launch & Main Menu

### 1.1 Application Start
| ID | Test | Expected |
|----|------|----------|
| L-001 | Launch DDPoker.jar | Splash screen appears, then main menu loads without errors |
| L-002 | Verify version string shown in main menu or About dialog | Version number displayed (e.g., 3.3.0) |
| L-003 | Resize main window | Window resizes gracefully; layout adapts |
| L-004 | Minimize and restore main window | Window minimizes to taskbar and restores correctly |

### 1.2 Main Menu Navigation
| ID | Test | Expected |
|----|------|----------|
| L-010 | Click **Practice** (single-player) button | Navigates to tournament selection/setup screen |
| L-011 | Click **Poker Night** button | Navigates to home game / host tournament screen |
| L-012 | Click **Options/Preferences** | Opens preferences dialog |
| L-013 | Click **Help** | Opens help system |
| L-014 | Click **Quit/Exit** | Application closes cleanly |
| L-015 | Use keyboard shortcut for each main menu item (if any) | Correct action triggered |

---

## Section 2 — Practice Mode Setup

### 2.1 Tournament Profile Selection
| ID | Test | Expected |
|----|------|----------|
| P-001 | Open Practice mode; view list of built-in tournament profiles | List of profiles shown (e.g., Beginner, Standard, etc.) |
| P-002 | Select each built-in profile; verify description/details shown | Correct details displayed per profile |
| P-003 | Click **New Profile** | Opens tournament profile editor with blank/default values |
| P-004 | Click **Edit Profile** on a built-in profile | Opens editor pre-populated with that profile's settings |
| P-005 | Click **Delete Profile** on a user-created profile | Confirmation prompt; profile removed from list |
| P-006 | Attempt to delete a built-in (non-deletable) profile | Delete button disabled or warning shown |
| P-007 | Click **Duplicate Profile** | Creates a copy of the selected profile |

### 2.2 Tournament Profile Editor — General Settings
| ID | Test | Expected |
|----|------|----------|
| P-010 | Set **Profile Name** to empty string and save | Validation error; save blocked |
| P-011 | Set number of players: min (2), mid (10), max (120) | Accepted; reflected in summary |
| P-012 | Set seats per table: min (2), max (10) | Accepted |
| P-013 | Enter a non-integer in numeric field (e.g., "abc") | Validation error or field reverts |
| P-014 | Enter negative value in buy-in field | Validation error |
| P-015 | Set **Game Type** to No-Limit Hold'em | Accepted; betting rules reflect NL |
| P-016 | Set **Game Type** to Pot-Limit Hold'em | Accepted; betting rules reflect PL |
| P-017 | Set **Game Type** to Limit Hold'em | Accepted; betting rules reflect Limit |

### 2.3 Tournament Profile Editor — Blind Structure
| ID | Test | Expected |
|----|------|----------|
| P-020 | Add blind level: small blind, big blind, ante | Level added to table |
| P-021 | Edit existing blind level | Changes reflected in table |
| P-022 | Delete a blind level | Level removed |
| P-023 | Reorder blind levels (drag or up/down buttons) | Order updates correctly |
| P-024 | Set level duration via **Time** mode (1–120 minutes) | Accepted |
| P-025 | Set level advance via **Hands per Level** mode (1–100 hands) | Accepted |
| P-026 | Toggle between Time and Hands-per-Level modes | UI switches correctly; appropriate fields enabled |
| P-027 | Set ante to 0 | Accepted; no ante shown during play |
| P-028 | Set big blind smaller than small blind | Validation error or warning |

### 2.4 Tournament Profile Editor — Rebuys & Add-ons
| ID | Test | Expected |
|----|------|----------|
| P-030 | Enable rebuys | Rebuy fields become active |
| P-031 | Set max rebuys: 1, 5, 99 | Accepted |
| P-032 | Set rebuy chip amount | Accepted |
| P-033 | Set rebuy cost/price | Accepted |
| P-034 | Set last rebuy level | Accepted; rebuys disallowed after that level |
| P-035 | Enable add-on | Add-on fields become active |
| P-036 | Set add-on chip amount and cost | Accepted |
| P-037 | Disable rebuys and add-ons | Fields grayed out; no rebuy option during play |

### 2.5 Tournament Profile Editor — Payouts
| ID | Test | Expected |
|----|------|----------|
| P-040 | Set number of payout spots (1, 3, 9, etc.) | Correct number of payout rows shown |
| P-041 | Enter percentage payouts that sum to 100% | Accepted |
| P-042 | Enter percentage payouts that do not sum to 100% | Warning or validation error |
| P-043 | Enter payout as fixed amounts | Accepted |
| P-044 | Enable satellite mode | Payout UI switches to seat-award mode |
| P-045 | Set house cut as a fixed amount | Accepted; deducted from prize pool |
| P-046 | Set house cut as a percentage | Accepted |
| P-047 | Set house cut to 0 | Accepted |

### 2.6 Tournament Profile Editor — Advanced Options
| ID | Test | Expected |
|----|------|----------|
| P-050 | Enable late registration; set last level for registration | Accepted |
| P-051 | Set minimum players required to start | Accepted |
| P-052 | Set player timeout (5–120 seconds) | Accepted |
| P-053 | Set per-round timeouts (preflop, flop, turn, river) | Each field accepts valid range |
| P-054 | Set think bank time (0–120 seconds) | Accepted |
| P-055 | Set bounty amount | Accepted |
| P-056 | Enable/disable chip race (color-up) | Accepted |
| P-057 | Set max raises per round (1–9 and unlimited) | Accepted |
| P-058 | Save profile; reload app; verify profile persists | Profile present after restart |

---

## Section 3 — Starting a Practice Game

### 3.1 Game Start
| ID | Test | Expected |
|----|------|----------|
| G-001 | Select profile; click **Start** | Game loads; table displayed with correct player count |
| G-002 | Verify correct number of AI opponents seated | Count matches profile setting |
| G-003 | Verify starting chip stacks match profile | Human and AI stacks correct |
| G-004 | Verify game type shown matches profile (NL/PL/Limit) | Label/indicator correct |
| G-005 | Verify blind amounts on first hand match profile | Correct small/big blind posted |
| G-006 | Verify dealer button placed correctly | Button on correct seat |

### 3.2 Dealing & Hand Flow
| ID | Test | Expected |
|----|------|----------|
| G-010 | Human hole cards dealt face-up | Two cards visible to human |
| G-011 | AI hole cards dealt face-down (cheats off) | AI cards not visible |
| G-012 | Preflop action proceeds in correct order | UTG acts first (or appropriate position) |
| G-013 | Flop dealt after preflop action complete | Three community cards appear |
| G-014 | Turn dealt after flop action complete | Fourth community card appears |
| G-015 | River dealt after turn action complete | Fifth community card appears |
| G-016 | Showdown reveals correct winner | Best hand wins; pot awarded correctly |
| G-017 | Folded hands not shown at showdown (cheats off) | Folded cards not revealed |
| G-018 | Side pots created correctly with all-in players | Multiple pots shown; correct amounts |
| G-019 | New hand starts correctly (auto-deal on) | Cards dealt after hand-end delay |

### 3.3 Human Player Actions
| ID | Test | Expected |
|----|------|----------|
| G-020 | **Fold** action | Hand folded; pot awarded to opponent(s) |
| G-021 | **Check** action (when available) | Check registered; next player acts |
| G-022 | **Call** action | Chips moved to pot; correct amount |
| G-023 | **Bet** action with slider/input | Correct bet amount entered; pot updated |
| G-024 | **Raise** action | Raise amount accepted; others must respond |
| G-025 | **All-in** (push all chips) | All chips in pot; all-in status shown |
| G-026 | Attempt to bet more than chip stack | Capped at chip stack; all-in |
| G-027 | Attempt to raise below minimum raise | Capped at minimum |
| G-028 | **Check/Fold** checkbox: check when bet made by opponent | Hand folds automatically |
| G-029 | Keyboard shortcuts for actions (if enabled) | Correct action triggered |
| G-030 | Disable keyboard shortcuts in options; retry shortcuts | Shortcuts have no effect |

### 3.4 Blind Posting & Antes
| ID | Test | Expected |
|----|------|----------|
| G-035 | Small and big blind posted automatically | Correct amounts deducted from respective seats |
| G-036 | Ante posted at levels with antes | Correct ante per player |
| G-037 | Player who can't cover blind posts partial blind | Partial blind posted; side pot created |

---

## Section 4 — Game Clock & Level Advancement

### 4.1 Clock Display
| ID | Test | Expected |
|----|------|----------|
| C-001 | Clock visible in dashboard/HUD | Current level and time remaining shown |
| C-002 | Clock counts down correctly | One second decrements per real second |
| C-003 | Clock shows correct level number | Level 1, 2, 3, ... displayed |
| C-004 | Current blind amounts shown in clock panel | Small blind / big blind correct for level |

### 4.2 Level Transitions
| ID | Test | Expected |
|----|------|----------|
| C-010 | Timer reaches zero | Level-end event triggered (bell, announcement, etc.) |
| C-011 | Bell sound plays at 10 seconds remaining (if enabled) | Bell audible |
| C-012 | **OPTION_CLOCK_PAUSE**: level ends, pause prompt appears | Clock pauses; "advance level" prompt shown |
| C-013 | Confirm level advance from pause prompt | Blinds increase to next level |
| C-014 | Without pause option: auto-advance to next level | Blinds increase automatically |
| C-015 | Chip race / color-up executes at configured level | Low-denomination chips raced off |
| C-016 | Color-up pause option stops game for chip race | Pause shown; resume continues play |
| C-017 | After final blind level: last level repeats (or game ends) | Appropriate end behavior |

### 4.3 Hands-per-Level Mode
| ID | Test | Expected |
|----|------|----------|
| C-020 | Profile set to hands-per-level; start game | Clock shows hand count, not time |
| C-021 | Hand count increments after each dealt hand | Counter increases |
| C-022 | Level advances after configured hand count | Blinds increase |

### 4.4 Clock Pause (Manual)
| ID | Test | Expected |
|----|------|----------|
| C-030 | Manually pause clock (if UI control exists) | Clock stops; label indicates paused |
| C-031 | Resume clock | Clock resumes from paused time |

---

## Section 5 — AI Opponents

### 5.1 AI Player Types
| ID | Test | Expected |
|----|------|----------|
| A-001 | Open AI player type roster | All built-in player types listed |
| A-002 | Select each player type; view profile | Correct description/stats shown |
| A-003 | Create new custom player type | Type saved; available in roster |
| A-004 | Edit a custom player type | Changes persist |
| A-005 | Delete a custom player type | Confirmation; type removed |
| A-006 | Attempt to delete built-in player type | Not permitted |

### 5.2 AI Behavior in Game
| ID | Test | Expected |
|----|------|----------|
| A-010 | AI acts within configured timeout period | No indefinite hangs |
| A-011 | Tight AI folds frequently preflop | Observe tight play pattern over ~50 hands |
| A-012 | Aggressive AI raises frequently | Observe aggressive play pattern |
| A-013 | AI handles all-in situations correctly | Goes all-in when appropriate; side pots correct |
| A-014 | AI handles rebuy (if profile has rebuys) | AI rebuys when eligible and broke |
| A-015 | AI handles add-on phase | AI takes add-on at end of rebuy period |
| A-016 | Show Player Type option enabled | Player type indicator shown on HUD |
| A-017 | Advisor mode: recommendation shown for human | Advisor suggests an action each hand |

---

## Section 6 — Cheat Options

All cheats tested in practice mode with a live game running.

### 6.1 Enabling Cheats
| ID | Test | Expected |
|----|------|----------|
| CH-001 | Open Preferences → Cheat tab | Cheat options list shown |
| CH-002 | Enable a cheat; verify indicator appears in dashboard | CheatDash shows active cheats |

### 6.2 Individual Cheat Tests
| ID | Cheat | Steps | Expected |
|----|-------|-------|----------|
| CH-010 | **Show AI Cards Face-Up** | Enable; deal a hand | All AI hole cards visible |
| CH-011 | **Show Folded Hands** | Enable; fold some opponents | Folded cards briefly shown or available |
| CH-012 | **Show Mucked Cards** | Enable; hand ends without showdown | Cards revealed on muck |
| CH-013 | **Win Probability Popup** | Enable; deal a hand | Popup shows % win chance for human |
| CH-014 | **Mouseover Hand Strength** | Enable; hover over hand | Strength indicator shown |
| CH-015 | **Show Winning Hand at Showdown** | Enable; reach showdown | Winning hand highlighted |
| CH-016 | **Never Broke** | Enable; lose all chips | Chip stack replenished automatically |
| CH-017 | **Pause Before Cards** | Enable; wait for card reveal | Pause inserted before hole card display |
| CH-018 | **Rabbit Hunt** | Enable; fold pre-flop | Remaining board cards shown |
| CH-019 | **Manual Button** | Enable; end of hand | Button does not auto-advance; manual control |
| CH-020 | Disable all cheats mid-game | All cheat indicators clear; behaviors revert |

---

## Section 7 — Dashboard

### 7.1 Dashboard Items
| ID | Test | Expected |
|----|------|----------|
| D-001 | Clock dashboard item visible | Level, time, blinds shown |
| D-002 | Player Info dashboard item | Current chip count, position shown |
| D-003 | Advisor dashboard item (enable in prefs) | Hand recommendation displayed |
| D-004 | Simulator dashboard item | Simulator controls accessible |
| D-005 | Cheat dashboard item (with cheats enabled) | Active cheats listed |
| D-006 | Debug dashboard item (if available) | Debug info displayed |

### 7.2 Dashboard Customization
| ID | Test | Expected |
|----|------|----------|
| D-010 | Open dashboard editor | List of available dashboard items shown |
| D-011 | Add a dashboard item | Item appears in dashboard |
| D-012 | Remove a dashboard item | Item removed from dashboard |
| D-013 | Reorder dashboard items | Order reflected in dashboard |
| D-014 | Dashboard state persists after restart | Same layout after relaunch |

---

## Section 8 — Hand Simulator (Calculator)

| ID | Test | Expected |
|----|------|----------|
| S-001 | Open hand simulator dialog | Simulator UI displayed with card entry fields |
| S-002 | Enter human hole cards; run simulation | Win % calculated and displayed |
| S-003 | Enter opponent hole cards; run simulation | Equity split shown across all players |
| S-004 | Run vs random range (all hands) | Simulation runs; result shown |
| S-005 | Adjust precision setting (fast vs accurate) | More/fewer simulation iterations used |
| S-006 | Cancel simulation mid-run | Simulation stops promptly |
| S-007 | Run with board cards (flop/turn/river) | Remaining equity calculated from current board |
| S-008 | Clear all cards; re-enter | Fields clear; new simulation runs correctly |

---

## Section 9 — Hand History & Statistics

### 9.1 Hand History Viewer
| ID | Test | Expected |
|----|------|----------|
| HH-001 | Play 5+ hands; open Hand History dialog | Hands listed in order |
| HH-002 | Click a hand in the list | Full hand transcript displayed |
| HH-003 | Scroll through long hand history | Scrolling works; no UI freeze |
| HH-004 | Hand shows correct hole cards, board, actions | Transcript accurate |
| HH-005 | Winning hand highlighted in transcript | Winner indicated |

### 9.2 History Export
| ID | Test | Expected |
|----|------|----------|
| HH-010 | Open export dialog | File path and options shown |
| HH-011 | Export hands to file (Paradise Poker format) | File created; valid content |
| HH-012 | Export summary (hands + tournaments) separately | Both files created |
| HH-013 | Verify file size estimate shown | Estimate displayed before export |
| HH-014 | Export progress indicator shows during large export | Progress bar or indicator visible |

### 9.3 Player Statistics
| ID | Test | Expected |
|----|------|----------|
| HH-020 | View player statistics after 10+ hands | Stats screen shows aggregate data |
| HH-021 | Hands seen per street (preflop/flop/turn/river) | Percentages calculated correctly |
| HH-022 | Win rate tracking | Wins counted correctly |
| HH-023 | Action frequency displayed | Fold/call/raise % shown |
| HH-024 | Position-based statistics | Stats broken down by position |

---

## Section 10 — Preferences / Options

### 10.1 Display Options
| ID | Test | Expected |
|----|------|----------|
| O-001 | Toggle **Large Cards** | Card size changes in game |
| O-002 | Toggle **Four Color Deck** | Suits use distinct colors (clubs=green, diamonds=blue) |
| O-003 | Toggle **Stylized Face Cards** | Face cards use stylized art |
| O-004 | Toggle **Hole Cards Down** | Human hole cards displayed face-down |
| O-005 | Toggle **Right-Click Only** mode | Actions require right-click |
| O-006 | Toggle **Show Player Type** | Player type indicator shown/hidden |

### 10.2 Audio Options
| ID | Test | Expected |
|----|------|----------|
| O-010 | Disable music | No background music |
| O-011 | Enable music; adjust volume slider | Volume audibly changes |
| O-012 | Disable sound effects | No FX sounds |
| O-013 | Enable FX; adjust volume | FX volume audibly changes |
| O-014 | Toggle bell at level countdown | Bell plays/suppressed at 10s remaining |

### 10.3 Chat Options
| ID | Test | Expected |
|----|------|----------|
| O-020 | Set dealer chat to **All** | All dealer messages shown |
| O-021 | Set dealer chat to **Player Action Only** | Only action-related messages shown |
| O-022 | Set dealer chat to **None** | No dealer messages shown |
| O-023 | Change chat font size (8–24) | Font size updates in chat panel |
| O-024 | Change chat display mode (One window / Tabbed / Split) | Layout changes accordingly |

### 10.4 Practice-Specific Options
| ID | Test | Expected |
|----|------|----------|
| O-030 | Enable **Zip Mode** | Animations skipped; hands play very fast |
| O-031 | Disable **Zip Mode** | Normal animation speed |
| O-032 | Set **Animation Delay** to 0 | Fastest non-zip speed |
| O-033 | Set **Animation Delay** to max (40s) | Slowest animation speed |
| O-034 | Enable **Auto-Deal** with delay | New hand deals automatically after delay |
| O-035 | Disable **Auto-Deal** | Hand requires manual deal click |
| O-036 | Set **Hands per Hour** target | AI pacing adjusts accordingly |
| O-037 | Enable **Pause on All-In** | Game pauses when all-in situation arises |
| O-038 | Disable **Pause on All-In** | No pause; all-in hands play out immediately |
| O-039 | Enable **Pause on Color-Up** | Game pauses before chip race |

### 10.5 Screenshot Options
| ID | Test | Expected |
|----|------|----------|
| O-040 | Set max screenshot width (640–2560) and height (480–1600) | Values accepted |
| O-041 | Take screenshot (if screenshot button exists) | File saved at configured resolution |

### 10.6 Preferences Persistence
| ID | Test | Expected |
|----|------|----------|
| O-050 | Change several options; close and reopen preferences | Changes persisted |
| O-051 | Change options; restart application | Options still applied after full restart |

---

## Section 11 — Table & Deck Customization

### 11.1 Table Design
| ID | Test | Expected |
|----|------|----------|
| TD-001 | Open table design selector | Available designs listed |
| TD-002 | Select each available table design | Table felt/color changes in preview |
| TD-003 | Apply design; start game | Correct design shown in game |
| TD-004 | Customize felt color (top/bottom) if supported | Color changes reflected in preview |
| TD-005 | Save a custom table design | Design appears in list |
| TD-006 | Delete a custom table design | Design removed |

### 11.2 Deck Customization
| ID | Test | Expected |
|----|------|----------|
| TD-010 | Open deck back selector | Available deck back images listed |
| TD-011 | Select each deck back | Preview updates |
| TD-012 | Apply deck back; deal a hand | Correct card back shown in game |
| TD-013 | Change deck during a session | Cards update to new back |

---

## Section 12 — Player Profile Management

| ID | Test | Expected |
|----|------|----------|
| PP-001 | View current player profile/name | Name shown |
| PP-002 | Change player name | Name updated throughout UI |
| PP-003 | Player name shown at table | Correct name displayed |
| PP-004 | Statistics attached to profile persist across sessions | Cumulative stats correct after restart |

---

## Section 13 — Poker Night (Home Game Mode)

### 13.1 Setup
| ID | Test | Expected |
|----|------|----------|
| PN-001 | Open **Poker Night** from main menu | Home tournament setup screen displayed |
| PN-002 | Configure player names for home game | Names entered and displayed |
| PN-003 | Select tournament profile | Profile applied to home game |
| PN-004 | Start home game | Game starts with entered player names |

### 13.2 Game Management
| ID | Test | Expected |
|----|------|----------|
| PN-010 | Clock displayed for home game | Level, time, blinds shown |
| PN-011 | Manual level advance (if host control exists) | Level advances on demand |
| PN-012 | Player elimination tracking | Eliminated players shown in standings |
| PN-013 | Final table play | Game continues to heads-up and completes |
| PN-014 | Payout report at end | Correct payout amounts shown per place |

---

## Section 14 — Save & Load Game

| ID | Test | Expected |
|----|------|----------|
| SL-001 | During practice game, choose **Save** | Save dialog appears; file written |
| SL-002 | Quit game after saving | Game exits without warning |
| SL-003 | Launch app; choose **Load** / continue saved game | Game restores to exact state |
| SL-004 | Verify chip stacks, level, and hand count restored | All match save state |
| SL-005 | Complete hand after loading | Game continues normally |
| SL-006 | Save over an existing file (overwrite) | Overwrite prompt; file updated |
| SL-007 | Delete a saved game file | File removed from list |
| SL-008 | Attempt to load a corrupted save file | Error shown; app does not crash |

---

## Section 15 — Hand Groups & Starting Hand Analysis

| ID | Test | Expected |
|----|------|----------|
| HG-001 | Open hand groups configuration | Predefined groups listed (premium, marginal, etc.) |
| HG-002 | View hands assigned to each group | Correct hands shown per group |
| HG-003 | Create a custom hand group | Group saved; hands assignable |
| HG-004 | Assign hands to custom group | Assignments persist |
| HG-005 | Delete a custom hand group | Group removed |
| HG-006 | View hand statistics broken down by group | Stats per group shown |
| HG-007 | Hand ladder / hand range visualizer shows correct layout | All 169 hand combos displayed |

---

## Section 16 — Help System

| ID | Test | Expected |
|----|------|----------|
| HL-001 | Click **Help** from main menu | Help window opens |
| HL-002 | Navigate help topics | Topics navigable; content loads |
| HL-003 | Open context-sensitive help from a dialog (F1 or ? button) | Relevant help topic shown |
| HL-004 | Search help content (if search supported) | Relevant topics returned |
| HL-005 | Close help window | Window closes; game unaffected |

---

## Section 17 — Edge Cases & Error Handling

| ID | Test | Expected |
|----|------|----------|
| E-001 | Heads-up play (2 players total, 1 AI) | Blind order correct; play completes |
| E-002 | Large field (e.g., 100+ players across multiple tables) | All tables populate; elimination reduces count |
| E-003 | All AI opponents (human folds every hand immediately) | Tournament finishes correctly |
| E-004 | Human eliminated (chips = 0, no rebuy) | Game ends; result shown |
| E-005 | Human takes rebuy after busting | Chips replenished; play continues |
| E-006 | Human takes add-on at break | Chips added; confirmed |
| E-007 | Very long tournament (50+ levels) | No performance degradation |
| E-008 | Rapid clicking through actions | No double-actions or UI freeze |
| E-009 | Run out of predefined blind levels | Last level repeats or graceful stop |
| E-010 | Open/close preferences repeatedly mid-game | Game state unaffected |
| E-011 | Open multiple dialogs simultaneously | No crash or UI corruption |
| E-012 | Press Escape in various dialogs | Dialog closes; no state corruption |

---

## Appendix A — Test Environment Setup

1. Install Java 11+ (JRE or JDK)
2. Build with `mvn clean package -DskipTests` from `code/`
3. Run `poker/target/DDPokerCE-3.3.0.jar`
4. Use a fresh user profile directory for each full test run (rename `~/.ddpoker` to back it up)

## Appendix B — Known Issues / Exclusions

- Online multiplayer (host/join online games) is explicitly out of scope.
- Online account registration, login, and activation are out of scope.
- REST API (GameControlServer dev mode) is out of scope for this plan.
- Auto-update check should be disabled for testing (set `OPTION_AUTO_CHECK_UPDATE=false`).

## Appendix C — Test Data Notes

- For timing-sensitive tests (clock, level advance), set level duration to 1 minute to speed up testing.
- For large-field tests, set all AI timeouts to minimum for speed.
- When testing payouts, use a 9-player, 3-spot payout structure for easy verification.
