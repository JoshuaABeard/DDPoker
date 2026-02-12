# Plan: Remove Demo Mode from DD Poker

## Context

DD Poker was originally a commercial product with a "demo mode" — a freemium/trial system that limited hands played (30 offline, 15 online), tournament levels (3 max), time per level (5 min), simulator hands (10K), and disabled stats tabs. It also tracked demo players in online games and let hosts choose whether to allow them.

Now that DD Poker is open-source (GPL-3.0, v3.3.0 Community Edition), demo mode is dormant but the infrastructure remains across ~60 files. This plan removes all of it cleanly, including breaking backward wire compatibility (confirmed by user).

## Approach

Work bottom-up through the module dependency chain in 7 batches. Each batch compiles independently before moving on.

```
common → gamecommon → gameengine → pokerengine → pokernetwork → poker → pokerserver
```

---

## Batch 1: Foundation — Version & Constants

Remove the demo flag from the wire protocol and debug constants.

| File | Changes |
|------|---------|
| `code/common/src/main/java/.../comms/Version.java` | Remove `bDemo_` field, `setDemo()`, `isDemo()`, 'd' suffix parsing (lines 115-120), marshal/demarshal of `bDemo_`, `toString()` 'd' suffix, demo+alpha/beta validation |
| `code/gamecommon/src/main/java/.../config/EngineConstants.java` | Remove `TESTING_DEMO` constant (line 67) |
| `code/common/src/test/.../comms/VersionTest.java` | Remove demo-related test cases |

**Verify:** `mvn compile -pl common,gamecommon -am` + `mvn test -pl common`

---

## Batch 2: Game Engine Layer

Remove demo plumbing from GameEngine, the TODO phase-interception mechanism in GameContext (exists only for demo), GamePlayer demo flag, and delete Demo.java/Order.java entirely.

| File | Changes |
|------|---------|
| `code/gameengine/.../engine/GameEngine.java` | Remove `bDemo_` field, `setDemoMode()`, `isDemo()`, `setDemoMsgDisplayed()`, `isBDemo()`, `setBDemo()`, `processingTODO()`, demo title logic, demo init (line 144) |
| `code/gameengine/.../engine/GameContext.java` | Remove `TODOphase_`/`TODOparams_`/`TODOhistory_` fields, `processTODO()`, `hasTODO()`, demo interception in `_processPhase()` (lines 446-470), `isBDemo()` check (line 934) |
| `code/gamecommon/.../config/GamePlayer.java` | Remove static `DEMO` field, static `setDemo()`, instance `bDemo_`, constructor init, `isDemo()`, `setDemo(boolean)`, serialization of `bDemo_` |
| `code/gameengine/.../engine/Demo.java` | **DELETE** (demo info screen phase) |
| `code/gameengine/.../engine/Order.java` | **DELETE** (purchase URL opener) |
| `code/gameengine/.../engine/InitializeGame.java` | Remove demo check (line 70) |
| `code/gameengine/.../engine/Support.java` | Remove demo handling |
| `code/gameengine/.../engine/MenuBackground.java` | Remove demo menu background code |
| `code/gameengine/.../engine/GameListPanel.java` | Remove `bDemo_` field and demo behavior |

**Verify:** `mvn compile -pl gameengine -am` + `mvn test -pl gameengine`

---

## Batch 3: Poker Engine & Model

Remove demo restrictions from TournamentProfile.

| File | Changes |
|------|---------|
| `code/pokerengine/.../model/TournamentProfile.java` | Remove `PARAM_DEMO`, `PARAM_ALLOW_DEMO`, `setDemo()`/`isDemo()`, `isAllowDemo()`/`setAllowDemo()`, 5-min demo level time limit (line 621-623) |

**Verify:** `mvn compile -pl pokerengine -am` + `mvn test -pl pokerengine`

---

## Batch 4: Poker Core Logic

Remove demo limits, enforcement, and all `isDemo()` checks from game logic classes.

| File | Changes |
|------|---------|
| `code/poker/.../PokerUtils.java` | Remove `DEMO_LIMIT`, `DEMO_LIMIT_ONLINE`, `isDemoOver()` |
| `code/poker/.../PokerPlayer.java` | Remove `bDemoLimit_`, `setDemoLimit()`, `isDemoLimit()` |
| `code/poker/.../CheckEndHand.java` | Remove demo enforcement block (lines 84-98) |
| `code/poker/.../GameOver.java` | Remove demo game-over dialog + Order button (lines 123-140), demo limit message (lines 182-185) |
| `code/poker/.../PokerNight.java` | Remove `DEMO_MAX`, demo level restrictions in `startClock()` and level checks |
| `code/poker/.../HoldemHand.java` | Remove `engine.isDemo()` check (line 187) |
| `code/poker/.../PokerGame.java` | Remove demo references |
| `code/poker/.../PokerTable.java` | Remove demo table behavior |
| `code/poker/.../PokerStartMenu.java` | Remove demo handling |
| `code/poker/.../PokerMain.java` | Remove demo handling |
| Tests: `PokerPlayerTest.java`, `MockGameEngine.java`, `MockPokerMain.java`, `PokerTableIntegrationTest.java`, `BasePhaseIntegrationTest.java` | Remove demo test cases and mock methods |

**Verify:** `mvn compile -pl poker -am` + `mvn test -pl poker`

---

## Batch 5: UI & Dialogs

Remove demo restrictions from UI panels and dialogs.

| File | Changes |
|------|---------|
| `code/poker/.../PokerShowdownPanel.java` | Remove `bDemo_`, 10K hand limit (use full limit always), demo warning dialog |
| `code/poker/.../PokerStatsPanel.java` | Remove `bDemo_`, demo tab disabling |
| `code/poker/.../TournamentProfileDialog.java` | Remove "Allow Demo Players" checkbox, player-count-disabled-for-demo |
| `code/poker/.../TournamentOptions.java` | Remove demo flag setting |
| `code/poker/.../ShowPokerNightTable.java` | Always use normal banner (not `pokermenu-demo`), remove button disabling at demo level |
| `code/poker/.../PlayerProfileDialog.java` | Remove demo considerations |
| `code/poker/.../GamePrefsPanel.java` | Remove demo settings |
| `code/poker/.../DeckDialog.java` | Remove demo deck limitations |

**Verify:** `mvn compile -pl poker -am`

---

## Batch 6: Network & Server

Remove demo tracking from online messages, server-side checks, and online game components.

| File | Changes |
|------|---------|
| `code/pokernetwork/.../OnlineMessage.java` | Remove `ON_DEMO`, `isPlayerDemo()`, `setPlayerDemo()` |
| `code/poker/.../online/OnlineManager.java` | Remove demo player rejection, `setDemo()` calls, `setPlayerDemo()` call |
| `code/poker/.../online/OnlineLobby.java` | Remove demo check |
| `code/poker/.../online/OnlineConfiguration.java` | Remove demo references |
| `code/poker/.../online/ChatPanel.java` | Remove demo chat restrictions |
| `code/poker/.../online/JoinGame.java` | Remove demo handling |
| `code/poker/.../dashboard/OnlineDash.java` | Remove demo sit-out prevention and label |
| `code/poker/.../ShowTournamentTable.java` | Remove demo sit-in prevention |
| `code/poker/.../engine/TournamentDirector.java` | Remove auto-fold for demo players |
| `code/pokerserver/.../server/PokerServlet.java` | Remove demo version check |
| `code/gameserver/.../server/EngineServlet.java` | Remove demo handling |

**Verify:** `mvn compile -pl pokernetwork,pokerserver,gameserver -am`

---

## Batch 7: Config, Resources & Documentation

Remove all demo strings, XML phase definitions, images, help text, and CSS.

| File | Changes |
|------|---------|
| `code/poker/.../config/poker/gamedef.xml` | Remove `<phase name="Order">`, `<phase name="Demo">`, `order.phase` param |
| `code/poker/.../config/poker/images.xml` | Remove `pokermenu-demo` image entry |
| `code/poker/.../config/poker/images/pokermenu-demo.png` | **DELETE** |
| `code/poker/.../config/poker/client.properties` | Remove ~31 demo strings (msg.application.name.demo, msg.title.Demo, msg.gameover.out.demo, msg.gameover.demo, msg.pokernight.demo, msg.showdown.demo, msg.playerprofile.demo/demo2, msg.onlinedone.demo, msg.onlinelobby.demo, labelborder.demo.label, panel.demo.help, msg.activate.demo, button.demo.label, label.demosave.label, label.deckimagedemo.label, demo player names, demo chat formats, msg.nojoin.demo, checkbox.sitoutdemo, option.allowdemo, msg.sim.demo) |
| `code/pokerengine/.../config/poker/common.properties` | Remove `settings.debug.demo` |
| `code/pokerserver/.../config/poker/server.properties` | Remove `msg.nodemo`, "Allow Demo Users" HTML row |
| `code/pokerserver/.../data/disallowed.txt` | Remove `(demo)` and `[demo]` entries |
| Help HTML files (`whatsnew.html`, `settings.html`, `mainmenu.html`, `joinonline.html`, `hostonline.html`, `gameboard.html`) | Remove demo references |
| `code/pokerwicket/.../css/nav.css` | Remove `.demo-content` CSS class |

**Verify:** `mvn clean verify` (full build + all tests)

---

## Final Verification

1. Full build: `mvn clean verify` — zero warnings, all tests pass, coverage >= 65%
2. Grep sweep to confirm no demo remnants:
   - `grep -r "isDemo\|setDemo\|bDemo_\|DEMO_LIMIT\|DEMO_MAX\|isDemoOver\|isDemoLimit\|setDemoLimit" --include="*.java"`
   - `grep -r "\.demo\b" --include="*.properties"`
   - Should return zero results (excluding false positives like "demonstration")

## Scope Summary

- **~45 Java files** modified or deleted
- **2 Java files** deleted (Demo.java, Order.java)
- **1 image file** deleted (pokermenu-demo.png)
- **~31 property strings** removed
- **3 XML entries** removed
- **6 HTML help files** updated
- **5-6 test files** updated

---

## ✅ COMPLETION STATUS

**Completed:** 2026-02-11

All 7 batches have been successfully completed. See `.claude/plans/REMOVE-DEMO-MODE-SUMMARY.md` for detailed documentation of all changes made.

### Final Results
- ✅ All 21 modules compile successfully (zero errors, zero warnings)
- ✅ No demo references remain in production Java code
- ✅ No demo references remain in property files
- ✅ No demo references remain in XML configs
- ✅ No demo references remain in HTML help files
- ✅ No demo references remain in CSS files
- ✅ Wire protocol compatibility intentionally broken (user confirmed acceptable)
- ⚠️ Pre-existing test failures in OnlineProfileStubTest are unrelated to demo removal (license-related)

**Actual Stats:**
- 40 files modified
- 3 files deleted (Demo.java, Order.java, pokermenu-demo.png)
- ~500+ lines of code removed
- ~35 property strings removed

### Follow-up Cleanup (2026-02-11 - Later Session)

Additional cleanup of vestigial test code and confusing terminology:

**Files Modified:**
1. **PokerPlayerTest.java** - Removed empty "Demo Limit Tests" section header
2. **BasePhaseIntegrationTest.java** - Removed 2 test methods calling deleted `isUsedInDemo()` method:
   - `should_ReturnTrue_When_IsUsedInDemoCalled()`
   - `should_ReturnTrueByDefault_When_IsUsedInDemoCalledWithoutInit()`
   - Test count reduced from 31 to 29
3. **MockGameEngine.java** - Updated JavaDoc to remove outdated demo mode reference
4. **DeckRandomnessTest.java** - Renamed test methods and updated comments to clarify terminology:
   - `testDemoModeIsDeterministic()` → `testSeededModeIsDeterministic()`
   - `testProductionModeUsesSecureRandom()` → `testUnseededModeUsesSecureRandom()`
   - `testSeedZeroIsProductionMode()` → `testSeedZeroIsUnseededMode()`
   - Updated all "demo mode"/"production mode" comments to "seeded mode"/"unseeded mode"

**Verification:**
- ✅ All modules compile successfully
- ✅ BasePhaseIntegrationTest: 29 tests pass (2 removed)
- ✅ DeckRandomnessTest: 8 tests pass (renamed, not removed)
- ✅ Zero demo-related method references remain: `isDemoOver`, `setDemoLimit`, `isDemoLimit`, `bDemo_`, `DEMO_LIMIT`, `DEMO_MAX`
