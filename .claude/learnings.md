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

- [pokergameserver] `ServerTournamentDirectorTest.multiTableTournamentConsolidates`, `interHandPausePreventsRacing`, and `playerEliminatedEventsPublished` are timing-sensitive — they use `thread.join(30000–120000)`. They pass reliably in isolation but occasionally timeout under `mvn test -P dev` (4-thread parallel build) due to CPU load. Re-run the module alone to distinguish real failures from load-induced flakiness (2026-02-20)
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

- [dev-server] `PlayerProfile.getProfileList()` requires `ConfigManager` to be initialized — throws NPE in test environments that don't start the full desktop app. Wrap in try-catch and return empty list (2026-02-19)
- [dev-server] `Map.of()` throws `NullPointerException` for null values — use `LinkedHashMap` when response fields may be null (e.g., `defaultProfile` when no profile exists) (2026-02-19)
- [dev-server] Pre-commit hook pattern `api[_-]?(key|secret)\s*[:=]\s*"?[a-zA-Z0-9]{16,}` will false-positive on method names like `loadOrGenerateKey` (17 alphanumeric chars). Keep method names shorter than 16 chars when the LHS contains "apiKey" or similar (2026-02-19)
- [dev-server] `ShowTournamentTable.poststart()` always calls `PracticeGameLauncher.launch()` which creates a new server-side game. Added `game_.getWebSocketConfig() == null` guard so the resume flow can pre-set config and skip the launcher (2026-02-19)

## Git & Workflow

- [worktree] Always create worktrees from the main worktree root, not from inside another worktree (2026-02-12)
- [ci] CI runs on push to main and on PRs to main (2026-02-12)
- [hooks] Claude Code `PostToolUse` hooks cause persistent "hook error" messages on Windows — even with a no-op `exit 0` script. Avoid using PostToolUse hooks entirely (2026-02-12)
- [hooks] Claude Code `PreToolUse` hooks are unreliable on Windows — sometimes work, sometimes error. Don't use for git hooks (2026-02-12)
- [hooks] Use git native hooks via `core.hooksPath = .claude/hooks` instead of Claude Code hooks for pre-commit/post-commit — works reliably across all worktrees (2026-02-12)
- [hooks] Claude Code `SessionStart` hooks work on Windows when using PowerShell (`pwsh -NoProfile -File script.ps1`) instead of bash (2026-02-12)
- [hooks] The `find` command in bash scripts on Windows behaves differently than Unix — use PowerShell `Get-ChildItem` for Windows-compatible hooks (2026-02-12)
