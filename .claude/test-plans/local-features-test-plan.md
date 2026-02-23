# DD Poker — Local Features Test Plan

**Date:** 2026-02-22
**Scope:** All local/offline features (no online multiplayer, no online profile features)
**Platform:** Windows desktop (Java Swing)
**Verification:** API-driven game control with screenshot + state comparison for validation

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
| L-002 | Verify version string shown in main menu | Version number displayed (e.g., 3.3.0) |
| L-003 | Resize main window | Window resizes gracefully; layout adapts |
| L-004 | Minimize and restore main window | Window minimizes to taskbar and restores correctly |
| L-005 | Toggle fullscreen mode (if window mode preference exists) | App switches between windowed and fullscreen correctly |
| L-006 | Close and relaunch; verify window position/size persisted | Window restores to previous position and dimensions |

### 1.2 Main Menu Navigation
| ID | Test | Expected |
|----|------|----------|
| L-010 | Click **Practice** button | Navigates to tournament selection/setup screen |
| L-011 | Click **Analysis** button | Navigates to statistics viewer / hand history analysis screen |
| L-012 | Click **Poker Clock** button | Navigates to Poker Night / home game setup screen |
| L-013 | Click **Calculator** button (control bar) | Opens hand simulator / calculator tool |
| L-014 | Click **Options** button (control bar) | Opens preferences dialog |
| L-015 | Click **Support** button (control bar) | Opens support dialog |
| L-016 | Click **Help** button (control bar) | Opens help system |
| L-017 | Click **Exit** button (control bar) | Application closes cleanly |

### 1.3 First Launch & Player Profile
| ID | Test | Expected |
|----|------|----------|
| L-020 | Launch with no existing profile (fresh `~/.ddpoker`) | Profile creation dialog appears before main menu |
| L-021 | Enter a player name and confirm | Profile created; name shown in main menu profile summary |
| L-022 | View player profile summary on main menu | Profile name and cumulative stats displayed |
| L-023 | Click **Profile** button on main menu | Profile options screen opens |

---

## Section 2 — Player Profile Management

| ID | Test | Expected |
|----|------|----------|
| PP-001 | Open player profile options from main menu | Profile management screen displayed with current profile |
| PP-002 | View current profile name and statistics | Name, total prize money, money spent, and profit/loss shown |
| PP-003 | Create a new player profile | New profile created; appears in profile list |
| PP-004 | Switch to a different profile | Active profile changes; main menu summary updates |
| PP-005 | Edit player name on existing profile | Name updated throughout UI |
| PP-006 | Delete a user-created profile | Confirmation prompt; profile removed from list |
| PP-007 | Player name shown correctly at poker table during game | Correct name displayed at human seat |
| PP-008 | Play a tournament; verify profile statistics update | Prize money, money spent, profit/loss recalculated |
| PP-009 | Restart application; verify profile and statistics persist | All profile data intact after relaunch |

---

## Section 3 — Practice Mode Setup

### 3.1 Tournament Profile Selection
| ID | Test | Expected |
|----|------|----------|
| P-001 | Open Practice mode; view list of built-in tournament profiles | List of profiles shown (e.g., Beginner, Standard, etc.) |
| P-002 | Select each built-in profile; verify description/details shown | Correct details displayed per profile |
| P-003 | Click **New Profile** | Opens tournament profile editor with blank/default values |
| P-004 | Click **Edit Profile** on a built-in profile | Opens editor pre-populated with that profile's settings |
| P-005 | Click **Delete Profile** on a user-created profile | Confirmation prompt; profile removed from list |
| P-006 | Attempt to delete a built-in (non-deletable) profile | Delete button disabled or warning shown |
| P-007 | Click **Duplicate Profile** | Creates a copy of the selected profile |

### 3.2 Tournament Profile Editor — General Settings
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

### 3.3 Tournament Profile Editor — Blind Structure
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
| P-029 | Use **Quick Blind Setup** wizard | Opens quick setup dialog; select a preset structure; levels populated automatically |

### 3.4 Tournament Profile Editor — Rebuys & Add-ons
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

### 3.5 Tournament Profile Editor — Payouts
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
| P-048 | Open **Prize Pool** summary dialog | Prize pool breakdown displayed with correct totals |

### 3.6 Tournament Profile Editor — Advanced Options
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

## Section 4 — Starting a Practice Game

### 4.1 Game Start
| ID | Test | Expected |
|----|------|----------|
| G-001 | Select profile; click **Start** | Game loads; table displayed with correct player count |
| G-002 | Verify correct number of AI opponents seated | Count matches profile setting |
| G-003 | Verify starting chip stacks match profile | Human and AI stacks correct |
| G-004 | Verify game type shown matches profile (NL/PL/Limit) | Label/indicator correct |
| G-005 | Verify blind amounts on first hand match profile | Correct small/big blind posted |
| G-006 | Verify dealer button placed correctly | Button on correct seat |

### 4.2 Dealing & Hand Flow
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

### 4.3 Human Player Actions
| ID | Test | Expected |
|----|------|----------|
| G-020 | **Fold** action | Hand folded; pot awarded to opponent(s) |
| G-021 | **Check** action (when available) | Check registered; next player acts |
| G-022 | **Call** action | Chips moved to pot; correct amount |
| G-023 | **Bet** action with slider/input | Correct bet amount entered; pot updated |
| G-024 | **Raise** action | Raise amount accepted; others must respond |
| G-025 | **All-in** (push all chips) | All chips in pot; all-in status shown |
| G-026 | **Pot-size bet** (P shortcut) | Bet amount equals pot; pot updated |
| G-027 | Attempt to bet more than chip stack | Capped at chip stack; all-in |
| G-028 | Attempt to raise below minimum raise | Capped at minimum |
| G-029 | **Check/Fold** checkbox: check when bet made by opponent | Hand folds automatically |
| G-030 | **Check/Fold** always-allow option (`OPTION_CHECKFOLD`) enabled | Fold button always available even when check is valid |

### 4.4 Keyboard Shortcuts
| ID | Test | Expected |
|----|------|----------|
| G-035 | Press **D** during deal prompt | Next hand deals |
| G-036 | Press **F** during human turn | Hand folded |
| G-037 | Press **C** during human turn | Check (if available) or Call executed |
| G-038 | Press **B** during human turn | Bet action initiated |
| G-039 | Press **R** during human turn | Raise action initiated |
| G-040 | Press **A** during human turn | All-in executed |
| G-041 | Press **E** during human turn | Check/Fold checkbox toggled |
| G-042 | Press **P** during human turn | Pot-size bet entered |
| G-043 | Press **Ctrl+T** from any screen | Calculator/simulator dialog opens |
| G-044 | Press **Ctrl+P** during game | Screenshot saved |
| G-045 | Enable **Disable Shortcuts** option; retry all shortcuts | Shortcuts have no effect |
| G-046 | Disable **Disable Shortcuts** option; retry shortcuts | Shortcuts work again |

### 4.5 Blind Posting & Antes
| ID | Test | Expected |
|----|------|----------|
| G-050 | Small and big blind posted automatically | Correct amounts deducted from respective seats |
| G-051 | Ante posted at levels with antes | Correct ante per player |
| G-052 | Player who can't cover blind posts partial blind | Partial blind posted; side pot created |

---

## Section 5 — Game Clock & Level Advancement

### 5.1 Clock Display
| ID | Test | Expected |
|----|------|----------|
| C-001 | Clock visible in dashboard/HUD | Current level and time remaining shown |
| C-002 | Clock counts down correctly | One second decrements per real second |
| C-003 | Clock shows correct level number | Level 1, 2, 3, ... displayed |
| C-004 | Current blind amounts shown in clock panel | Small blind / big blind correct for level |
| C-005 | Next level's blind amounts shown (upcoming) | Next blind level preview displayed |

### 5.2 Level Transitions
| ID | Test | Expected |
|----|------|----------|
| C-010 | Timer reaches zero | Level-end event triggered (bell, announcement, etc.) |
| C-011 | Bell sound plays at 10 seconds remaining (if enabled) | Bell audible |
| C-012 | **Clock Pause** option enabled: level ends, pause prompt appears | Clock pauses; "advance level" prompt shown |
| C-013 | Confirm level advance from pause prompt | Blinds increase to next level |
| C-014 | Without pause option: auto-advance to next level | Blinds increase automatically |
| C-015 | After final blind level: last level repeats | Blinds stay at final level indefinitely |

### 5.3 Color-Up / Chip Race
| ID | Test | Expected |
|----|------|----------|
| C-020 | **Color-Up Notification** option enabled; level with color-up reached | Notification shown indicating chip race will occur |
| C-021 | **Pause on Color-Up** option enabled; color-up level reached | Game pauses; color-up prompt shown |
| C-022 | Confirm color-up; chip race executes | Low-denomination chips raced off; visual chip exchange animation plays |
| C-023 | Verify player chip totals correct after color-up | No chips lost in race (rounded correctly) |
| C-024 | Color-up with **Pause on Color-Up** disabled | Chip race executes automatically without pause |

### 5.4 Hands-per-Level Mode
| ID | Test | Expected |
|----|------|----------|
| C-030 | Profile set to hands-per-level; start game | Clock shows hand count, not time |
| C-031 | Hand count increments after each dealt hand | Counter increases |
| C-032 | Level advances after configured hand count | Blinds increase |

### 5.5 Clock Pause (Manual)
| ID | Test | Expected |
|----|------|----------|
| C-040 | Manually pause clock (if UI control exists) | Clock stops; label indicates paused |
| C-041 | Resume clock | Clock resumes from paused time |

---

## Section 6 — Multi-Table Tournaments

| ID | Test | Expected |
|----|------|----------|
| MT-001 | Start tournament with 20+ players across multiple tables | Multiple tables created; players distributed evenly |
| MT-002 | Players eliminated; tables rebalance | Players moved between tables to maintain even distribution |
| MT-003 | Table move notification displayed | "Player moved to Table X" message shown in dealer chat |
| MT-004 | Table breaks when too few players remain | Remaining players moved to other tables |
| MT-005 | Final table formed | All remaining players consolidated to one table |
| MT-006 | Verify chip leader panel updates as players are eliminated | Rankings reflect current standings |
| MT-007 | Open **Game Info** dialog during multi-table tournament | Shows all tables, all player standings, chip counts |

---

## Section 7 — In-Game Dialogs

### 7.1 Game Info Dialog
| ID | Test | Expected |
|----|------|----------|
| GI-001 | Open Game Info dialog during a tournament | Dialog displays current tournament state |
| GI-002 | Verify player standings tab | All players listed with chip counts, sorted by stack |
| GI-003 | Verify payout preview tab | Payout spots and amounts shown correctly |
| GI-004 | Verify hand history tab within Game Info | Recent hands listed and viewable |
| GI-005 | Close Game Info; game resumes normally | No state corruption |

### 7.2 Side Pots Dialog
| ID | Test | Expected |
|----|------|----------|
| GI-010 | Create an all-in situation with side pots | Side pots created |
| GI-011 | Open Side Pots dialog | All side pots listed with amounts and eligible players |
| GI-012 | Verify pot amounts match expected calculations | Main pot and side pots sum correctly |

### 7.3 Tournament Results / Game Over
| ID | Test | Expected |
|----|------|----------|
| GI-020 | Human wins tournament (or use Never Broke cheat) | Game Over screen displayed |
| GI-021 | Verify finishing positions shown | All players listed in order of elimination |
| GI-022 | Verify payout amounts displayed | Correct payouts per finishing position |
| GI-023 | Verify player profile statistics updated | Prize money and money spent reflect tournament results |
| GI-024 | Human eliminated mid-tournament | Game Over screen shown with human's finishing position |
| GI-025 | Click **Play Again** from Game Over screen | New tournament starts with same profile |
| GI-026 | Click **Quit** from Game Over screen | Returns to main menu |

---

## Section 8 — Rebuy & Add-on Phases

| ID | Test | Expected |
|----|------|----------|
| RB-001 | Human eliminated during rebuy period | Rebuy prompt appears |
| RB-002 | Accept rebuy | Chips replenished to rebuy amount; play continues |
| RB-003 | Decline rebuy | Human eliminated; game continues (or Game Over if last rebuy) |
| RB-004 | Multiple rebuys up to max allowed | Each rebuy accepted until max reached |
| RB-005 | Attempt rebuy after max rebuys used | Rebuy option not available |
| RB-006 | Attempt rebuy after last rebuy level passed | Rebuy option not available |
| RB-007 | AI player eliminated during rebuy period | AI rebuys automatically when eligible |
| RB-008 | Add-on break occurs at end of rebuy period | Add-on prompt appears for human |
| RB-009 | Accept add-on | Chips added to stack; add-on cost deducted from buy-in tracking |
| RB-010 | Decline add-on | No chips added; play continues |
| RB-011 | AI players take add-on | AI automatically takes add-on |
| RB-012 | Verify prize pool updated after rebuys and add-ons | Prize pool reflects all additional buy-ins |

---

## Section 9 — AI Opponents

### 9.1 AI Player Types
| ID | Test | Expected |
|----|------|----------|
| A-001 | Open AI player type roster from preferences | All built-in player types listed |
| A-002 | Select each player type; view profile | Correct description, style sliders, and quadrant (tight/loose, aggressive/passive) shown |
| A-003 | Create new custom player type | Type saved; available in roster |
| A-004 | Edit a custom player type; adjust sliders | Changes persist |
| A-005 | Delete a custom player type | Confirmation; type removed |
| A-006 | Attempt to delete built-in player type | Not permitted |

### 9.2 AI Behavior in Game
| ID | Test | Expected |
|----|------|----------|
| A-010 | AI acts within configured timeout period | No indefinite hangs |
| A-011 | Tight AI folds frequently preflop | Observe tight play pattern over ~50 hands |
| A-012 | Aggressive AI raises frequently | Observe aggressive play pattern |
| A-013 | AI handles all-in situations correctly | Goes all-in when appropriate; side pots correct |
| A-014 | AI handles rebuy (if profile has rebuys) | AI rebuys when eligible and broke |
| A-015 | AI handles add-on phase | AI takes add-on at end of rebuy period |
| A-016 | **Show Player Type** option enabled | Player type indicator shown on HUD for each AI |
| A-017 | **Show Player Type** option disabled | Player type indicators hidden |

---

## Section 10 — Advisor

### 10.1 Advisor Basic
| ID | Test | Expected |
|----|------|----------|
| AD-001 | Enable **Default Advisor** in preferences | Advisor enabled for all future hands |
| AD-002 | Start a hand; verify advisor recommendation appears | Dashboard shows recommended action (fold/check/call/bet/raise) with reasoning |
| AD-003 | Advisor recommendation updates each betting round | New recommendation shown for flop, turn, river |
| AD-004 | Disable advisor; verify no recommendations shown | Advisor dashboard item blank or hidden |

### 10.2 Advisor Info Dialog
| ID | Test | Expected |
|----|------|----------|
| AD-010 | Click advisor recommendation to open detail dialog | AdvisorInfoDialog opens |
| AD-011 | Verify hand strength analysis shown | Current hand strength and win probability displayed |
| AD-012 | Verify strategy matrix / advisor grid shown | Grid panel displays recommended ranges |
| AD-013 | Verify opponent type consideration | Advisor factors in opponent playing styles |
| AD-014 | Close advisor dialog; game continues | No state corruption |

---

## Section 11 — Cheat Options

All cheats tested in practice mode with a live game running.

### 11.1 Enabling Cheats
| ID | Test | Expected |
|----|------|----------|
| CH-001 | Open Preferences → Practice tab → Cheat options | Cheat options list shown |
| CH-002 | Enable a cheat; verify indicator appears in dashboard | CheatDash shows active cheats |

### 11.2 Individual Cheat Tests
| ID | Cheat | Steps | Expected |
|----|-------|-------|----------|
| CH-010 | **Show AI Cards Face-Up** | Enable; deal a hand | All AI hole cards visible |
| CH-011 | **Show Folded Hands** | Enable; fold some opponents | Folded cards shown or available |
| CH-012 | **Show Mucked Cards** | Enable; hand ends without showdown | Mucked cards revealed |
| CH-013 | **Cheat Popup** | Enable; click on an opponent or community cards | Popup shows detailed probability/hand info |
| CH-014 | **Mouseover Hand Strength** | Enable; hover over opponent | Strength indicator shown on mouseover |
| CH-015 | **Show Winning Hand at Showdown** | Enable; reach showdown | Winning hand highlighted even when won by fold |
| CH-016 | **Never Broke** | Enable; lose all chips | Chip stack replenished (50% of starting) automatically |
| CH-017 | **Pause Before Cards** | Enable; wait for card reveal | Pause inserted before each card deal (hole, flop, turn, river) |
| CH-018 | **Rabbit Hunt** | Enable; fold before river | Remaining community cards shown after hand ends |
| CH-019 | **Manual Button** | Enable; end of hand | Dealer button does not auto-advance; requires manual control |
| CH-020 | Disable all cheats mid-game | All cheat indicators clear; all behaviors revert to normal |

---

## Section 12 — Dashboard

### 12.1 Dashboard Items
| ID | Test | Expected |
|----|------|----------|
| D-001 | **Clock** dashboard item visible | Level, time remaining, and blinds shown |
| D-002 | **Player Info** dashboard item | Current chip count, position, and status shown |
| D-003 | **Advisor** dashboard item (enable advisor in prefs) | Hand recommendation displayed each turn |
| D-004 | **Simulator** dashboard item | Quick simulator controls accessible; can run equity calc |
| D-005 | **Hand Strength / Odds** dashboard item | Current hand strength and win percentage displayed |
| D-006 | **Pot Odds** dashboard item | Pot odds calculation displayed based on current pot and bet |
| D-007 | **Improve Odds** dashboard item | Drawing odds and outs displayed for current hand |
| D-008 | **My Hand** dashboard item | Current hole cards displayed |
| D-009 | **My Table** dashboard item | Current table info (table number, seats, etc.) displayed |
| D-010 | **Rank** dashboard item | Current tournament ranking/standing displayed |
| D-011 | **Up Next** dashboard item | Next action or event indicator shown |
| D-012 | **Cheat** dashboard item (with cheats enabled) | Active cheats listed |
| D-013 | **Debug** dashboard item (if available in dev mode) | Debug info displayed |

### 12.2 Dashboard Customization
| ID | Test | Expected |
|----|------|----------|
| D-020 | Open dashboard editor dialog | List of all available dashboard items shown with add/remove controls |
| D-021 | Add a dashboard item | Item appears in dashboard immediately |
| D-022 | Remove a dashboard item | Item removed from dashboard |
| D-023 | Reorder dashboard items | Order reflected in dashboard layout |
| D-024 | Dashboard state persists after restart | Same item selection and layout after relaunch |

---

## Section 13 — Hand Simulator (Calculator)

| ID | Test | Expected |
|----|------|----------|
| S-001 | Open hand simulator via **Calculator** button on main menu | Simulator UI displayed with card entry fields |
| S-002 | Open hand simulator via **Ctrl+T** keyboard shortcut | Same simulator dialog opens |
| S-003 | Open simulator during a live game | Simulator opens; current hole cards and board pre-populated |
| S-004 | Enter human hole cards; run simulation | Win % calculated and displayed |
| S-005 | Enter opponent hole cards; run simulation | Equity split shown across all entered players |
| S-006 | Run vs random range (all hands) | Simulation runs; result shown |
| S-007 | Configure opponent mix panel (weighted hand ranges) | Opponent ranges accepted; simulation uses weighted ranges |
| S-008 | Adjust precision setting (fast vs accurate) | More/fewer simulation iterations used |
| S-009 | Cancel simulation mid-run | Simulation stops promptly; partial results may display |
| S-010 | Run with board cards (flop/turn/river) | Remaining equity calculated from current board |
| S-011 | View hand probability matrix | Visual matrix showing win % for each hand combination displayed |
| S-012 | Clear all cards; re-enter | Fields clear; new simulation runs correctly |

---

## Section 14 — Analysis & Statistics Viewer

### 14.1 Accessing Analysis
| ID | Test | Expected |
|----|------|----------|
| AV-001 | Click **Analysis** button from main menu | Statistics viewer screen opens |
| AV-002 | Verify list of past tournaments shown | Tournaments listed with dates and results |
| AV-003 | Select a tournament; view details | Tournament summary displayed (players, payouts, finishing position) |

### 14.2 Hand History Viewer
| ID | Test | Expected |
|----|------|----------|
| HH-001 | Play 5+ hands; open Hand History dialog (in-game or from Analysis) | Hands listed in chronological order |
| HH-002 | Click a hand in the list | Full hand transcript displayed |
| HH-003 | Scroll through long hand history | Scrolling works; no UI freeze |
| HH-004 | Hand shows correct hole cards, board, actions | Transcript accurate |
| HH-005 | Winning hand highlighted in transcript | Winner indicated |

### 14.3 History Export
| ID | Test | Expected |
|----|------|----------|
| HH-010 | Open export dialog from Analysis or Hand History | File path and format options shown |
| HH-011 | Export hands to file (Paradise Poker format) | File created; valid content |
| HH-012 | Export summary (hands + tournaments) separately | Both files created |
| HH-013 | Verify file size estimate shown before export | Estimate displayed |
| HH-014 | Export progress indicator shows during large export | Progress bar or indicator visible |
| HH-015 | Export chat log to HTML | HTML file created with formatted chat messages |

### 14.4 History Import
| ID | Test | Expected |
|----|------|----------|
| HH-020 | Import hand history from Paradise Poker format file | Hands imported successfully; appear in history |
| HH-021 | Import hand history from UltimateBet format file | Hands imported successfully; appear in history |
| HH-022 | Attempt to import an invalid/corrupt file | Error message shown; no crash |

### 14.5 Player Statistics
| ID | Test | Expected |
|----|------|----------|
| HH-030 | View player statistics after 10+ hands | Stats screen shows aggregate data |
| HH-031 | Hands seen per street (preflop/flop/turn/river) | Percentages calculated correctly |
| HH-032 | Win rate tracking | Wins counted correctly |
| HH-033 | Action frequency displayed | Fold/call/raise % shown |
| HH-034 | Position-based statistics | Stats broken down by position |

---

## Section 15 — Preferences / Options

### 15.1 Display Options
| ID | Test | Expected |
|----|------|----------|
| O-001 | Toggle **Large Cards** | Card size changes in game (exaggerated upper-left corner for readability) |
| O-002 | Toggle **Four Color Deck** | Suits use distinct colors (clubs=green, diamonds=blue) |
| O-003 | Toggle **Stylized Face Cards** | Face cards switch between stylized art and standard |
| O-004 | Toggle **Hole Cards Down** | Human hole cards displayed face-down / face-up |
| O-005 | Toggle **Right-Click Only** mode | Pop-up menus triggered by right-click only |
| O-006 | Toggle **Show Player Type** | Player type indicator shown/hidden on HUD |
| O-007 | Toggle **Check/Fold Always Allow** | Fold button always available even when check is valid |

### 15.2 Audio Options
| ID | Test | Expected |
|----|------|----------|
| O-010 | Disable background music | No background music plays |
| O-011 | Enable background music; adjust volume slider (5–100) | Volume audibly changes |
| O-012 | Disable sound effects | No FX sounds (shuffle, chips, bell, cheers) |
| O-013 | Enable sound effects; adjust volume slider (5–100) | FX volume audibly changes |
| O-014 | Verify shuffle sound plays when cards dealt | Shuffle sound audible with FX enabled |
| O-015 | Verify chip sounds play on bet/raise/call | Chip sounds audible |
| O-016 | Verify check sound plays on check action | Check sound audible |
| O-017 | Verify cheers sound plays on big win/all-in | Cheers sound audible |
| O-018 | Verify attention bell plays at level countdown | Bell plays at ~10 seconds remaining |

### 15.3 Chat / Dealer Message Options
| ID | Test | Expected |
|----|------|----------|
| O-020 | Set dealer chat to **All** | All dealer messages shown (actions, results, commentary) |
| O-021 | Set dealer chat to **No Player Action** | Only non-action dealer messages shown |
| O-022 | Set dealer chat to **None** | No dealer messages shown |
| O-023 | Change chat font size: min (8), mid (12), max (24) | Font size updates in chat panel |
| O-024 | Set chat display mode to **One Window** | All chat in single pane |
| O-025 | Set chat display mode to **Tabbed** | Chat organized into tabs |
| O-026 | Set chat display mode to **Split** | Chat in side-by-side split pane |

### 15.4 Practice-Specific Options
| ID | Test | Expected |
|----|------|----------|
| O-030 | Enable **Zip Mode** | After human folds, jumps to end of hand; animations skipped |
| O-031 | Disable **Zip Mode** | Full hand plays out with animations |
| O-032 | Set **Animation Delay** to 0 (tenths of seconds) | Fastest non-zip speed; minimal pauses |
| O-033 | Set **Animation Delay** to max (40 = 4 seconds) | Long pauses between AI actions and community cards |
| O-034 | Enable **Auto-Deal** | New hand deals automatically after configurable delay |
| O-035 | Set **Auto-Deal Hand Delay** (0–100 tenths of seconds) | Delay between hands matches setting |
| O-036 | Set **Auto-Deal Fold Delay** (0–100 tenths of seconds) | Shorter delay used when human folds |
| O-037 | Disable **Auto-Deal** | Hand requires manual deal click or D key |
| O-038 | Set **Hands per Hour** target (10–250) | AI pacing and level clock adjust accordingly |
| O-039 | Enable **Pause on All-In** | Game pauses when a player goes all-in; community cards shown one at a time |
| O-040 | Disable **Pause on All-In** | All-in hands play out without extra pause |
| O-041 | Enable **Pause on Color-Up** | Game pauses before chip race |
| O-042 | Disable **Pause on Color-Up** | Chip race executes automatically |

### 15.5 Clock Options
| ID | Test | Expected |
|----|------|----------|
| O-050 | Enable **Clock Color-Up Notification** | Color-up notification shown based on future blind levels |
| O-051 | Disable **Clock Color-Up Notification** | No color-up notification |
| O-052 | Enable **Clock Pause at Level End** | Clock pauses at end of each level; manual advance required |
| O-053 | Disable **Clock Pause at Level End** | Levels advance automatically |

### 15.6 Screenshot Options
| ID | Test | Expected |
|----|------|----------|
| O-060 | Set max screenshot width (640–2560) | Value accepted |
| O-061 | Set max screenshot height (480–1600) | Value accepted |
| O-062 | Take screenshot via Ctrl+P | File saved at configured resolution |
| O-063 | Verify camera shutter sound plays on screenshot (FX enabled) | Shutter sound audible |

### 15.7 Preferences Persistence
| ID | Test | Expected |
|----|------|----------|
| O-070 | Change several options across tabs; close and reopen preferences | All changes persisted |
| O-071 | Change options; restart application | Options still applied after full restart |

---

## Section 16 — Table & Deck Customization

### 16.1 Table Design
| ID | Test | Expected |
|----|------|----------|
| TD-001 | Open table design selector (from preferences) | Available designs listed with preview |
| TD-002 | Select each available built-in table design | Table felt/color changes in preview |
| TD-003 | Apply design; start game | Correct design shown at poker table |
| TD-004 | Customize felt color (top/bottom gradient) | Color changes reflected in preview |
| TD-005 | Save a custom table design | Design appears in list with user-given name |
| TD-006 | Delete a custom table design | Design removed from list |
| TD-007 | Attempt to delete a built-in table design | Not permitted |

### 16.2 Deck Customization
| ID | Test | Expected |
|----|------|----------|
| TD-010 | Open deck profile selector (from preferences) | Available deck back images listed with preview |
| TD-011 | Select each built-in deck back | Preview updates to show selected back |
| TD-012 | Apply deck back; deal a hand | Correct card back shown for face-down cards |
| TD-013 | Create a custom deck profile (if supported) | Deck profile saved |
| TD-014 | Change deck back mid-session via preferences | Cards update to new back on next hand |

---

## Section 17 — Poker Night & Poker Clock

### 17.1 Setup
| ID | Test | Expected |
|----|------|----------|
| PN-001 | Open **Poker Clock** from main menu | Poker Night setup screen displayed |
| PN-002 | Configure player names for home game | Names entered and displayed in player list |
| PN-003 | Add and remove players from roster | Player count updates correctly |
| PN-004 | Select tournament profile for home game | Profile applied; blind structure and payouts configured |
| PN-005 | Start home game / poker clock | Game starts with entered player names and clock running |

### 17.2 Clock Management
| ID | Test | Expected |
|----|------|----------|
| PN-010 | Clock displays current level, time remaining, and blinds | All clock elements visible and correct |
| PN-011 | Clock counts down in real-time | Timer decrements one second per second |
| PN-012 | Pause clock manually | Clock stops; paused indicator shown |
| PN-013 | Resume clock from pause | Clock resumes from paused time |
| PN-014 | Bell sound plays at countdown (configurable) | Bell audible near level end |
| PN-015 | Level auto-advances when timer reaches zero | Blinds increase to next level |
| PN-016 | Level pauses at zero (if clock pause option enabled) | Manual advance required |
| PN-017 | Manual level advance button | Level advances on demand regardless of timer |
| PN-018 | Countdown panel visible during level transitions | Visual countdown display shown |

### 17.3 Player & Game Management
| ID | Test | Expected |
|----|------|----------|
| PN-020 | Eliminate a player (mark as out) | Player shown as eliminated in standings |
| PN-021 | Track elimination order | Finishing positions recorded correctly |
| PN-022 | Player rebuy during rebuy period | Player un-eliminated; chip count adjusted |
| PN-023 | Add-on break at end of rebuy period | Add-on prompt or notification shown |
| PN-024 | Color-up notification at appropriate level | Color-up alert shown |
| PN-025 | View chip leader standings | Players ranked by chip count |
| PN-026 | Final player standing — tournament complete | Payout report displayed with correct amounts per position |

### 17.4 Poker Night Results
| ID | Test | Expected |
|----|------|----------|
| PN-030 | Tournament ends (all but one eliminated) | Final results screen displayed |
| PN-031 | Payout report shows correct amounts per position | Payouts match tournament profile |
| PN-032 | Return to main menu from results | Clean navigation back to start |

---

## Section 18 — Hand Groups & Starting Hand Analysis

| ID | Test | Expected |
|----|------|----------|
| HG-001 | Open hand groups configuration (from preferences) | Predefined groups listed (premium, marginal, etc.) |
| HG-002 | View hands assigned to each group | Correct hands shown per group |
| HG-003 | Create a custom hand group | Group saved with user-given name |
| HG-004 | Edit hand group description | Description updated and persisted |
| HG-005 | Assign hands to custom group via grid panel | Visual 13x13 grid displayed; hands selectable by click |
| HG-006 | Navigate hand grid with keyboard (Tab, arrow keys) | Grid navigation works; selection follows keys |
| HG-007 | Delete a custom hand group | Confirmation; group removed |
| HG-008 | Attempt to delete a built-in hand group | Not permitted |
| HG-009 | View hand statistics broken down by group | Stats per group shown |
| HG-010 | Hand range visualizer shows all 169 hand combos | Full grid displayed: pairs on diagonal, suited above, offsuit below |

---

## Section 19 — Save & Load Game

| ID | Test | Expected |
|----|------|----------|
| SL-001 | During practice game, choose **Save** (quit/save prompt) | Save dialog appears; game file written |
| SL-002 | Quit game after saving | Game exits cleanly without "unsaved" warning |
| SL-003 | Launch app; choose to continue/load saved game | Game restores to exact state |
| SL-004 | Verify chip stacks, level, hand count, and blind amounts restored | All match save state |
| SL-005 | Complete hand after loading | Game continues normally with correct action flow |
| SL-006 | Save over an existing file (overwrite) | Overwrite prompt; file updated |
| SL-007 | Delete a saved game file | File removed from saved games list |
| SL-008 | Attempt to load a corrupted save file | Error shown; app does not crash |
| SL-009 | Auto-save triggers after manual save (if auto-save enabled) | Game auto-saves periodically without prompt |

---

## Section 20 — Support Dialog

| ID | Test | Expected |
|----|------|----------|
| SP-001 | Click **Support** from main menu control bar | Support dialog opens |
| SP-002 | Verify version information displayed | Correct version number shown |
| SP-003 | Copy logs / view log files option | Log file location shown or logs copied to clipboard |
| SP-004 | View configuration file paths | Config directory path displayed |
| SP-005 | Close support dialog | Dialog closes; main menu unaffected |

---

## Section 21 — Help System

| ID | Test | Expected |
|----|------|----------|
| HL-001 | Click **Help** from main menu | Help window opens with topic list |
| HL-002 | Navigate to **Practice Mode** help topic | Content loads; describes practice mode features |
| HL-003 | Navigate to **Poker Clock** help topic | Content loads; describes poker night/clock features |
| HL-004 | Navigate to **Calculator Tool** help topic | Content loads; describes simulator |
| HL-005 | Navigate to **Rank of Hands** help topic | Poker hand rankings displayed |
| HL-006 | Navigate to **Texas Hold'em Rules** help topic | Game rules content displayed |
| HL-007 | Navigate to **Glossary** help topic | Poker terms and definitions listed |
| HL-008 | Navigate to **Keyboard Shortcuts** help topic | All shortcuts documented |
| HL-009 | Navigate to **What's New** help topic | Version changelog displayed |
| HL-010 | Navigate to **Credits & Legal** help topic | Attribution and license info shown |
| HL-011 | Open context-sensitive help from a dialog (F1 or ? button) | Relevant help topic opened for current screen |
| HL-012 | Close help window | Window closes; game/menu unaffected |

---

## Section 22 — Edge Cases & Error Handling

| ID | Test | Expected |
|----|------|----------|
| E-001 | Heads-up play (2 players total, 1 AI) | Blind order correct (SB=dealer=button); play completes |
| E-002 | Large field (100+ players across multiple tables) | All tables populate; elimination reduces count; tables balance |
| E-003 | Human folds every hand (let AI play out tournament) | Tournament finishes correctly; standings shown |
| E-004 | Human eliminated (chips = 0, no rebuy available) | Game Over screen shown; finishing position correct |
| E-005 | Human takes rebuy after busting | Chips replenished; play continues at same table |
| E-006 | Human takes add-on at break | Chips added; correct amount; play continues |
| E-007 | Very long tournament (50+ levels) | No performance degradation; last level repeats |
| E-008 | Rapid clicking through actions | No double-actions or UI freeze |
| E-009 | Run out of predefined blind levels | Last level repeats indefinitely |
| E-010 | Open/close preferences repeatedly mid-game | Game state unaffected |
| E-011 | Open multiple dialogs simultaneously (Game Info + Hand History) | No crash or UI corruption |
| E-012 | Press Escape in various dialogs | Dialog closes; no state corruption |
| E-013 | Three-way all-in with different stack sizes | Main pot and two side pots created correctly |
| E-014 | Split pot (identical hands at showdown) | Pot divided equally between tied players |
| E-015 | Player timeout (if timeout configured) | AI auto-acts if human doesn't respond within timeout |
| E-016 | Start game with minimum players (2) in every game type (NL/PL/Limit) | All three game types play correctly heads-up |

---

## Appendix A — Test Environment Setup

1. Install Java 11+ (JRE or JDK)
2. Build with `mvn clean package -DskipTests -P dev` from `code/`
3. Run `poker/target/DDPokerCE-3.3.0.jar`
4. Use a fresh user profile directory for each full test run (rename `~/.ddpoker` to back it up)
5. Automation API available at `http://localhost:<port>` (port from `~/.ddpoker/control-server.port`)

## Appendix B — Known Issues / Exclusions

- **Online multiplayer** (host/join online games, lobbies, matchmaking) — covered in separate online test plan.
- **Online profile features** (passwords, account registration, login, activation, muted/banned player lists) — covered in separate online test plan.
- **Online-specific preferences** (online audio alerts, online auto-deal, UDP mode, countdown timer, online chat, online pause) — covered in separate online test plan.
- **REST API / GameControlServer** — the automation API itself is not under test; it is the test harness.
- Auto-update check should be disabled for testing (set `OPTION_AUTO_CHECK_UPDATE=false`).

## Appendix C — Test Data Notes

- For timing-sensitive tests (clock, level advance), set level duration to 1 minute to speed up testing.
- For large-field tests, set all AI timeouts to minimum for speed.
- When testing payouts, use a 9-player, 3-spot payout structure for easy verification.
- For rebuy testing, use a profile with max rebuys = 3, last rebuy level = 3 for predictable behavior.
- For color-up testing, configure blind levels with chip denominations that require a race (e.g., 25/50 → 50/100).
- For multi-table testing, use 20+ players with 6 seats per table to force 4+ tables.

## Appendix D — Keyboard Shortcut Reference

| Key | Action | Context |
|-----|--------|---------|
| D | Deal next hand | Between hands (DEAL mode) |
| F | Fold | Human turn |
| C | Check / Call | Human turn |
| B | Bet | Human turn (when bet available) |
| R | Raise | Human turn (when raise available) |
| A | All-in | Human turn |
| E | Toggle Check/Fold checkbox | Human turn |
| P | Pot-size bet | Human turn |
| Ctrl+T | Open Calculator / Simulator | Any screen |
| Ctrl+P | Take Screenshot | Any screen |
| Tab | Navigate hand group grid | Hand group editor |
| Arrow keys | Navigate hand group grid | Hand group editor |
