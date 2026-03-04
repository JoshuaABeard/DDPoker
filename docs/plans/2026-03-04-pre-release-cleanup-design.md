# Pre-Release Cleanup Design

**Date:** 2026-03-04
**Status:** APPROVED — ready for implementation planning

## Goal

Clean up the codebase before the initial Community Edition release. Remove all legacy/dead code, migrate the local client database from HSQLDB to H2, and confirm the thin-client architecture is consistent. No new features. No AI refactoring. No bulk for-loop modernization.

---

## Scope

Three sequential branches, ordered by risk.

---

## Branch 1 — HSQLDB → H2 Migration

### What changes

`PokerDatabase.java` is the desktop client's local embedded database for practice game stats and hand history. It currently uses `org.hsqldb.jdbcDriver` with a `jdbc:hsqldb:file:` connection URL.

**Design decisions:**
- Practice game stats stay local (machine-local H2 file). Online game stats already live on the server — no change there.
- Fresh H2 schema on first launch. HSQLDB and H2 binary formats are incompatible — existing local HSQLDB files cannot be migrated. This is acceptable: it's a new community edition release, old stats are not carried over.
- No HSQLDB compatibility mode needed. H2's native SQL dialect handles everything `PokerDatabase` uses.

**File changes:**
- `poker/src/main/java/.../PokerDatabase.java` — change driver class → `org.h2.Driver`, connection URL → `jdbc:h2:file:<appdata>/poker`, remove HSQLDB-specific property calls (e.g., `SET PROPERTY "hsqldb.cache_scale"`)
- `poker/pom.xml` — remove `hsqldb:hsqldb:1.8.0.10` dependency
- `gameserver/pom.xml` — remove `hsqldb:hsqldb:1.8.0.10` dependency (gameserver uses H2 already; HSQLDB dep was dead weight)
- `gameserver/src/main/resources/app-context-gameserver.xml` — remove commented-out HSQLDB datasource block and the inline HSQLDB design note (lines ~67–98)
- `poker/src/test/java/.../PokerDatabaseTest.java` — update test setup to use H2

**Verification:** Run `PokerDatabaseTest`, confirm local practice game stats write/read correctly with H2.

---

## Branch 2 — Dead Code Removal

### What changes

#### 2a. `gameserver` module

The `gameserver` module (6 files: `EngineServer`, `GameData`, `PlayerQueue`, registry classes) is legacy infrastructure fully superseded by `pokergameserver` (the Spring Boot server).

**Process:**
1. Grep all modules for `import com.donohoedigital.games.server.*` — expect zero hits.
2. Confirm `gameserver` is not declared as a dependency in any other module's POM.
3. Delete the `gameserver/` directory.
4. Remove `<module>gameserver</module>` from the root `pom.xml`.
5. Run full test suite.

#### 2b. Dead client-side phase chain

The following `ChainPhase` implementations in the `poker` module are dead code. Practice games now run on the embedded `pokergameserver` via WebSocket (`WebSocketTournamentDirector`), so the old client-side game loop phase chain is never entered.

**Files to delete** (after verification):
- `Bet.java`
- `CheckEndHand.java`
- `NewLevelActions.java`
- `ColorUp.java`
- `ColorUpFinish.java`
- `DealButton.java`
- `ButtonDisplay.java`
- `WaitForDeal.java`
- `PreShowdown.java`
- `DisplayTableMoves.java`

**Process:**
1. Grep for any non-phase-chain references to each class (e.g., direct instantiation, static method calls).
2. Check `gamedef.xml` / `poker.xml` for `<phase>` entries pointing to these classes — remove those entries.
3. Delete the files.
4. Run full test suite + existing scenario tests.

---

## Branch 3 — Code Quality Cleanup

### What changes

Low-risk, surgical changes. Can be done in one branch with commits grouped by category.

#### 3a. JUnit 4 → JUnit 5 (34 files in `pokerengine`)

Mechanical migration:
- `@org.junit.Test` → `@org.junit.jupiter.api.Test`
- `@Before` → `@BeforeEach`, `@After` → `@AfterEach`, `@BeforeClass` → `@BeforeAll`, `@AfterClass` → `@AfterAll`
- `@Ignore` → `@Disabled`
- `Assert.*` → `Assertions.*` (or use AssertJ already in the project)
- Remove `junit:junit` and `junit-vintage-engine` from `pokerengine/pom.xml`
- Audit other modules: remove `junit:junit` from any module whose tests are already fully JUnit 5

#### 3b. StringBuffer → StringBuilder (8 files)

Replace in non-synchronized contexts. Skip `ExplicitConstraints.java` (third-party code).

Files: `AIOutcome.java`, `Hide.java`, `GameState.java`, `GameServletRequest.java`, `ZipUtil.java`, `ZipUtilTest.java`, `Unhide.java`, `GameboardCenterLayout.java` (if applicable).

#### 3c. System.out.println → logger (production code only)

Replace in:
- `PokerMain.java` (3 instances) — use existing `Logger` field
- `PokerStats.java` (2 instances) — use existing `Logger` field

Leave batch tools (`PokerRankUpdater.java`, CLI tools) and test files as-is.

#### 3d. Remove commented-out dead config

- `app-context-gameserver.xml`: remove MySQL datasource comment block (lines ~67–72) and HSQLDB datasource comment block + design note (lines ~74–98). Keep the active H2-configured `dataSource` bean.

#### 3e. Non-AI TODO review

Walk the 68 TODO/FIXME/HACK comments. For each:
- **Trivially fixable** (e.g., `OptionCombo` "doesn't currently do anything"): fix or remove the comment
- **Stale observation** (e.g., `GameboardTerritoryManager` war-aoi specific): remove or replace with a brief factual comment
- **Genuine future work** (AI strategy, localization, scaling): leave as-is or prefix with `// Future:` for clarity
- **In tests** (stub test body TODOs): either implement the test or delete the stub

---

## What's Explicitly Out of Scope

- AI strategy improvements (`PureRuleEngine.java`, `V2Algorithm.java` TODOs)
- Old-style for-loop → stream modernization (1,253 instances — too broad)
- Moving practice game stats to the central server (keeping local H2 is the right boundary)
- Adding new features or changing any game behavior

---

## Thin-Client Architecture Assessment

The audit confirmed the thin-client migration is already complete:
- Online games: server-authoritative via `WebSocketTournamentDirector` (zero game logic on client)
- Practice games: run on embedded `pokergameserver`, client connects via WebSocket
- `RemotePokerTable` / `RemoteHoldemHand`: standalone view models, no game engine inheritance
- Hand evaluation on client (`HandInfoFast`, etc.): display/labeling only, not authoritative

No further thin-client migration work is required for this release.

---

## Success Criteria

- Zero HSQLDB references in code or POMs
- Zero JUnit 4 imports in `pokerengine` tests
- `gameserver` module removed from build
- Dead phase chain files deleted, no remaining references in XML configs
- No `System.out.println` in production Java source (outside batch tools)
- No `StringBuffer` in non-synchronized production code
- All commented-out dead config blocks removed
- Full test suite (`mvn test -P dev`) passes after each branch
