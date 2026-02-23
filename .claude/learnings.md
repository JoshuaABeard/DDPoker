# Agent Learnings

Persistent knowledge discovered during development sessions. Read this at the start of non-trivial tasks to avoid rediscovering known issues.

**Format:** `- [module/area] Finding (discovered YYYY-MM-DD)`

**Rules:**
- Add entries when you discover something non-obvious that cost you time
- Remove entries when they become obsolete (e.g., bug was fixed, dependency changed)
- Keep entries concise — one line if possible
- Don't duplicate what's already in guides or DDPOKER-OVERVIEW.md

---

## Build & Maven

- [build] `mvn test -P dev` is the fast path — skips slow/integration tests, runs 4 threads (2026-02-12)
- [build] CI uses `-P dev` profile, not the full test suite (2026-02-12)
- [coverage] Coverage threshold is 65% enforced by JaCoCo; use `mvn verify -P coverage` to check (2026-02-12)
- [format] Spotless auto-formats Java code on compile — don't manually format, just run `mvn compile` (2026-02-12)
- [format] Spotless also reformats XML resource files (gamedef.xml, etc.) during `mvn compile`/`package`, reverting uncommitted edits to match git HEAD. Always commit XML edits before building, or they will be lost (2026-02-19)

- [build] Running `mvn test -pl <module>` in isolation uses installed JARs from `~/.m2` for upstream modules — if those JARs are stale (not reinstalled after changes), tests that depend on new classes in upstream modules will fail with `ClassNotFoundException`. Fix: run full `mvn clean test` from root, or `mvn install -pl <upstream> -DskipTests` first (2026-02-18)
- [pokerserver] `jjwt-jackson:0.12.5` forces `jackson-databind:2.12.7.1` which conflicts with `jackson-datatype-jsr310:2.19.x` pulled in by Spring Boot. Fixed in `pokerserver/pom.xml` via `<dependencyManagement>` pinning all three core Jackson artifacts to `2.19.4` (2026-02-18)

## Testing

- [pokergameserver] `ServerTournamentDirectorTest.multiTableTournamentConsolidates`, `interHandPausePreventsRacing`, `playerEliminatedEventsPublished`, and `levelChangedEventPublishedOnLevelAdvance` are timing-sensitive — they use `thread.join(30000–120000)`. They pass reliably in isolation but occasionally timeout under `mvn test -P dev` (4-thread parallel build) due to CPU load. Re-run the module alone to distinguish real failures from load-induced flakiness (2026-02-20)
- [pokerengine] AIStrategyNode tests depend on PropertyConfig state; tests must be resilient to initialization order (2026-02-12)
- [pokerengine] NEVER call setValue() on static Card constants (SPADES_A, etc.) in tests — they are shared singletons and modifications pollute all other tests. Create new Card instances instead (2026-02-13)
- [pokerengine] OnlineGame.hashCode() violates equals/hashCode contract by including super.hashCode() — equal objects (same URL) have different hash codes (2026-02-13)
- [db] ResultSet must be explicitly closed in ResultMap to prevent resource leaks (2026-02-12)

## Game Engine

- [pokerengine] `TournamentProfile.setLevel()` sets blind/ante values but does NOT set `PARAM_LASTLEVEL`. Without it, `BlindStructure` defaults `lastlevel` to 0 and returns 0 for all blinds/antes. Fix: call `profile.getMap().setInteger(TournamentProfile.PARAM_LASTLEVEL, N)` after defining levels (2026-02-18)
- [poker] AI code (`AIOutcome`, `BetRange`, `ClientStrategyProvider`) uses `Math.random()` — not `DiceRoller` — so `DiceRoller.setSeed()` does not guarantee deterministic AI behavior (2026-02-18)
- [poker] For all-AI tables, `PokerTable.createPokerAI()` only runs when `(!isAllComputer() || isCurrent()) && !isSimulation()`. Call `game.setCurrentTable(table)` to trigger AI initialization (2026-02-18)
- [poker] `HoldemHand.bet/call/raise/fold/check()` only record hand history — they do NOT deduct chips from the player. Chip deduction is done by `PokerPlayer.bet/call/raise/fold/check()`. Always drive hand actions through `PokerPlayer.processAction(HandAction)` to get both; calling hand methods directly silently breaks chip conservation (2026-02-19)
- [poker] `HoldemHand.getAmountToCall(GamePlayerInfo)` blindly casts its argument to `PokerPlayer` — passing any other `GamePlayerInfo` (e.g. `ClientV2AIContext.PokerPlayerAdapter`) causes `ClassCastException`. Use `HoldemHand.getCall(PokerPlayer)` with an unwrapped player instead (2026-02-19)
- [poker] `V2Player.getBetAmount()` calls `re.getBetRange()` which can return null (e.g. when no bet range is configured for the situation). The return value must be null-checked; returning 0 is safe because `PokerAI.getHandAction()` falls back to `call + minRaise` when `getBetAmount()` returns 0 (2026-02-19)
- [poker] `BlindStructure` reads the key `"doubleafterlast"` to decide whether to double blinds past the last defined level, but `TournamentProfile.PARAM_DOUBLE` is the string `"double"` — a pre-existing key mismatch. Set `"doubleafterlast"` directly on the profile map when this behaviour is needed (2026-02-19)

## Configuration

- [config] PropertyConfig is a global singleton — tests that modify it can affect other tests running in the same JVM (2026-02-12)

## WebSocket Broadcasting

- [broadcaster] `GameEventBroadcaster` looks up tables with `getTable(e.tableId() - 1)` (0-based index) because events carry `table.getNumber()` which is 1-based. If you add a new event handler that looks up a table, always use `e.tableId() - 1` and bound-check with `e.tableId() > 0 && (e.tableId() - 1) < getNumTables()` (2026-02-22)
- [broadcaster] `GameEventBroadcaster.HandStarted` sends GAME_STATE → HAND_STARTED → HOLE_CARDS in that order. This is the sole mechanism that populates the client's `tables_` map before ACTION_REQUIRED arrives — do NOT add extra GAME_STATE sends from the WebSocket handler thread after `startGame()`, as they race with broadcaster events on the game thread and can cause out-of-order community card state (2026-02-22)
- [websocket] `PlayerConnection.sendMessage()` is `synchronized` — Spring's WebSocketSession.sendMessage() is not thread-safe (JSR-356). `LobbyWebSocketHandler.sendSafe()` bypasses this and calls `session.sendMessage()` directly; lobby sends are currently single-threaded so this is not an active race, but it's an inconsistency to note (2026-02-22)

## Server Game Engine (ServerTournamentDirector)

- [server] Inter-hand pause must hook `result.nextState() == TableState.BEGIN` (the DONE→BEGIN transition from `handleDone()`), NOT `nextState==CLEAN` or `TD.CheckEndHand` — both are dead code for auto-deal online games where `handleBegin()` with `isAutoDeal()=true` goes directly to `START_HAND` (2026-02-19)
- [server] `nextState==CLEAN` is always dead: CLEAN is reached via `pendingState`, never via `nextState` (2026-02-19)
- [server] `TD.CheckEndHand` phase is skipped for auto-deal games: `handleBegin(isAutoDeal=true)` → `nextState(START_HAND)`, never visits WaitForDeal or CheckEndHand (2026-02-19)
- [server] `aiActionDelayMs` in embedded mode is configured via `application-embedded.properties` as `game.server.ai-action-delay-ms=1000`. Default for server mode is 0 (2026-02-20)
- [server] All-AI hands (after human elimination) complete in <1ms — automation polling at 0.15s will miss PRE_FLOP transitions. Use HAND_STARTED timestamps in WebSocket debug log to verify pacing, not automation hand counter (2026-02-19)
- [automation] The `run-client-local.ps1` script does NOT include `pokergameserver` module classes directly — it's loaded as a JAR dependency from `code/poker/target/dependency/`. Rebuild BOTH `pokergameserver` AND `poker` (to update the copied JAR) when changing server code (2026-02-19)
- [automation] H2 database file lock: always kill ALL Java processes before relaunching the client. The lock file is NOT automatically cleared on crash and prevents startup (2026-02-19)
- [server] `handleOnHold()` in TournamentEngine also returns `nextState(BEGIN)`, which would trigger a spurious pause+incrementHandsPlayed at the DONE→BEGIN hook. Currently unreachable in ServerTournamentDirector (which never sets ON_HOLD), but must be guarded if multi-table ON_HOLD logic is ever added (2026-02-19)

## Dev Control Server

- [dev-server] `BaseHandler.handle()` must catch `Throwable`, not just `Exception` — `PokerUtils.<clinit>` throws `ExceptionInInitializerError` (an `Error`) without a running game. If `Error` escapes the handler, the JDK HttpServer thread terminates without sending a response, leaving the HTTP client hanging indefinitely (2026-02-22)
- [dev-server] Handler tests that assert "returns 500 when no GameEngine" are order-dependent — if another test in the same JVM initializes GameEngine first, the handler returns 200 instead and the test fails. Don't write tests that depend on GameEngine absence (2026-02-22)
- [dev-server] When testing `poker` module dev handlers in isolation (`-pl poker`), the `pokergameserver` dependency is resolved from `~/.m2`. If `pokergameserver` was recently changed (new classes), run `mvn install -pl pokergameserver -am -DskipTests` first, or the handler will get `ClassNotFoundException` at runtime (not compile time) (2026-02-22)
- [dev-server] Piping Maven output to `tail` in background tasks (`mvn ... | tail -25`) prevents output from appearing in the task output file until Maven exits — the `tail` buffers internally. Never pipe to `tail` in background Bash tasks; just redirect with `2>&1` (2026-02-22)
- [dev-server] `PlayerProfile.getProfileList()` requires `ConfigManager` to be initialized — throws NPE in test environments that don't start the full desktop app. Wrap in try-catch and return empty list (2026-02-19)
- [dev-server] `Map.of()` throws `NullPointerException` for null values — use `LinkedHashMap` when response fields may be null (e.g., `defaultProfile` when no profile exists) (2026-02-19)
- [dev-server] Pre-commit hook pattern `api[_-]?(key|secret)\s*[:=]\s*"?[a-zA-Z0-9]{16,}` will false-positive on method names like `loadOrGenerateKey` (17 alphanumeric chars). Keep method names shorter than 16 chars when the LHS contains "apiKey" or similar (2026-02-19)
- [dev-server] `ShowTournamentTable.poststart()` always calls `PracticeGameLauncher.launch()` which creates a new server-side game. Added `game_.getWebSocketConfig() == null` guard so the resume flow can pre-set config and skip the launcher (2026-02-19)

## Git & Workflow

- [worktree] Always create worktrees from the main worktree root, not from inside another worktree (2026-02-12)
- [hooks] The pre-commit hook that blocks code commits to main also discards working tree changes to the blocked files — re-apply edits in the worktree, don't copy from main (2026-02-22)
- [ci] CI runs on push to main and on PRs to main (2026-02-12)
- [hooks] Claude Code `PostToolUse` hooks cause persistent "hook error" messages on Windows — even with a no-op `exit 0` script. Avoid using PostToolUse hooks entirely (2026-02-12)
- [hooks] Claude Code `PreToolUse` hooks are unreliable on Windows — sometimes work, sometimes error. Don't use for git hooks (2026-02-12)
- [hooks] Use git native hooks via `core.hooksPath = .claude/hooks` instead of Claude Code hooks for pre-commit/post-commit — works reliably across all worktrees (2026-02-12)
- [hooks] Claude Code `SessionStart` hooks work on Windows when using PowerShell (`pwsh -NoProfile -File script.ps1`) instead of bash (2026-02-12)
- [hooks] The `find` command in bash scripts on Windows behaves differently than Unix — use PowerShell `Get-ChildItem` for Windows-compatible hooks (2026-02-12)

## Scenario Test Suite (embedded GameControlServer)

- [scenario-tests] The embedded server auto-deals each hand immediately after the previous hand ends — DEAL mode does NOT appear between hands. Tests must detect hand completion via community card resets or phase transitions, not DEAL mode (2026-02-23)
- [scenario-tests] BETWEEN_HANDS phase is too brief (~ms) to reliably catch at 0.15s polling. Detect new hands by watching for PRE_FLOP transitions from a non-PRE_FLOP phase, or by dealer seat change when phase stays PRE_FLOP (all-folded-preflop hands) (2026-02-23)
- [scenario-tests] After river (5 community cards), the game goes into QUITSAVE (AI acting in new hand) not DEAL mode. Detect "hand over" by waiting 3 seconds after recording the river card (2026-02-23)
- [scenario-tests] ValidateHandler chip conservation uses game.getNumPlayers() (initial count) not currently-seated numPlayers — eliminated players are removed from their seat, so numPlayers * buyinChips underestimates the true chip total (2026-02-23)
- [scenario-tests] HoldemHand.getCommunityCards() was reading community_ field directly instead of calling getCommunity(), bypassing RemoteHoldemHand override. Community cards always appeared empty in API state. Fix: use polymorphic getCommunity() (2026-02-23)
- [scenario-tests] Scripts that call advance_to_human_turn as state=$(fn) have log output swallowed by command substitution — log messages go into $state not the terminal. Use stderr for debug output inside such functions (2026-02-23)
- [scenario-tests] Both tests racing to build simultaneously will fail with Maven file lock on common/target. Run sequentially or add --skip-build after first build (2026-02-23)
- [scenario-tests] Windows: DDPoker config files are in %APPDATA%/ddpoker (C:\Users\...\AppData\Roaming\ddpoker), not ~/.ddpoker. lib.sh must detect $APPDATA env var and use cygpath -u to convert to Unix path (2026-02-23)
- [dev-server] REBUY_CHECK mode now works via `onRebuyOffered()` interceptor in `WebSocketTournamentDirector`. The server sends REBUY_OFFERED → interceptor blocks the EDT on a latch → sets MODE_REBUY_CHECK → test sends REBUY or DECLINE_REBUY → latch resolved → EDT unblocks (2026-02-23)
- [dev-server] Neverbroke cheat (NEVER_BROKE_ACTIVE path): chips are restored via `human.setChipCount(nAdd)` BEFORE `EngineUtils.displayInformationDialog()` blocks the EDT. Poll /state chip count to detect restoration even while the info dialog is blocking. Requires `rebuys=false` (otherwise REBUY_OFFERED fires with a blocking modal first) (2026-02-23)
- [dev-server] For rebuy tests, `setChips` cheat is CLIENT-ONLY — the embedded server tracks chips independently. The server never sees client-side chip changes and won't send REBUY_OFFERED. To trigger a rebuy, use natural chip loss (small buyinChips + high blinds, fold every hand) (2026-02-23)
- [dev-server] `GameStartHandler.buildProfile()` with `rebuys=true` must set 4 fields to make the server's rebuy logic fire: (1) `setLastRebuyLevel(lastLevel)` — `getLastLevel()` returns 0 for fresh profiles; (2) `setInteger(PARAM_REBUYCOST, buyinChips)` — `offerRebuy()` returns false when cost==0; (3) `setInteger(PARAM_REBUYCHIPS, buyinChips)` — otherwise rebuy gives 0 chips; (4) `setInteger(PARAM_MAXREBUYS, MAX_REBUYS)` — `isRebuyPeriodActive()` returns false when maxRebuys==0 (2026-02-23)
- [pokergameserver] `GameInstance.offerRebuy()` calls `future.get(actionTimeoutSeconds, SECONDS)`. In embedded/practice mode `actionTimeoutSeconds==0`, so the future times out INSTANTLY and returns false. Use `if (timeout <= 0) future.get()` for indefinite wait (same pattern as `waitForContinue()`). Same applies to `offerAddon()` (2026-02-23)
- [scenario-tests] `screenshot()` blocks when the EDT is frozen (e.g. while `waitForDecision()` holds the latch). Never call `screenshot()` inside a REBUY_CHECK detection block — call it AFTER the REBUY/DECLINE_REBUY action is sent and the EDT is unblocked (2026-02-23)
- [scenario-tests] `ps aux | grep java | grep -v grep` exits with code 1 (no matches) when no Java processes exist — with `set -euo pipefail`, this kills the script. Always add `|| true` at the end of grep pipelines inside command substitution: `java_pids=$(... | grep ... || true)` (2026-02-23)
- [dev-server] Multi-table tournament (WebSocketTournamentDirector): only the human's current table appears in `game.getTables()`. Other tables are managed server-side without client notification. `tableCount` is always 1. `ValidateHandler` chip conservation always fails (expects numPlayers×chips but only sees one table). Use `tournament.totalPlayers` to check full player count (2026-02-23)
- [dev-server] `ValidateHandler` chip conservation must use `TournamentProfile.getNumPlayers()` (initial count, constant) rather than `game.getNumPlayers()` (shrinks as eliminated players are removed from the seat map). Using the game-level count causes an ever-shrinking expected total that never matches actual chips (2026-02-23)
- [dev-server] When `pokergameserver` record fields change (e.g. new `autoDeal` field in `PracticeConfig`), the `poker` module silently resolves the old record from `~/.m2` at compile time. Compile succeeds but `ClassNotFoundException`/`NoSuchMethodError` occurs at runtime. Always `mvn install -pl pokergameserver -DskipTests` first when adding record fields (2026-02-23)
- [dev-server] Practice game tournaments (client-side) also auto-deal between hands — DEAL mode never appears. Use wall-clock timing for play loops, not DEAL-mode hand counting (2026-02-23)
- [poker] `HandGroup(File, boolean)` does NOT call `clearContents()`, so `pairs_`/`suited_`/`offsuit_` arrays are null for groups loaded from file that have 0 hands (no "hands" key in map). `expand()` NPEs on such groups. Fixed in HandGroup.read() by calling clearContents() before parsing the map (2026-02-23)
- [poker] `HoldemSimulator.simulate()` computes `Math.log(list.size())` for each hand group's iteration count — `log(0)` = -Infinity → Integer.MIN_VALUE as handCount for empty groups. Skip groups where list.size()==0 after expand() (2026-02-23)
- [scenario-tests] `curl -f` exits non-zero AND produces no output for 4xx responses. Use `--fail-with-body` instead so error response JSON is captured in $() subshells for error-checking tests (2026-02-23)
- [scenario-tests] Interrupted test-hand-groups runs can leave stale empty hand group .dat files in ~/.ddpoker/save/handgroups/. These have no "hands" key and cause HoldemSimulator NPE. The HandGroup.read() fix prevents the crash, but stale files may skew group counts in assertions (2026-02-23)
- [scenario-tests] `TournamentProfilesHandler` POST /tournament-profiles returns `{"created": true, "profile": {...}}` — the profile name is at `o.profile.name`, NOT `o.name`. Test scripts must extract with `jget ... 'o.profile?.name'` (2026-02-23)
