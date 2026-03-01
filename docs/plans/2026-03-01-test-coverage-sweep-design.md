# Test Coverage Sweep Design

**Date:** 2026-03-01
**Status:** APPROVED
**Goal:** Raise overall test coverage to 60%+ across all modules

## Overview

Bottom-up module sweep to systematically raise every module's test coverage floor,
prioritized by testability and impact. Five phases, each self-contained and deliverable,
with JaCoCo thresholds raised after each phase to lock in gains.

**Estimated total:** ~320 new tests across 5 phases.

## Current State

| Module           | Coverage | Threshold | Test Files |
|------------------|----------|-----------|------------|
| poker (AI pkg)   | 50%      | 0.50      | High       |
| jsp              | 37%      | 0.37      | 2          |
| pokergameserver  | 36%      | 0.36      | 60+        |
| poker (main)     | 19%      | 0.15      | 72         |
| wicket           | 12%      | 0.12      | 2          |
| gameserver       | 9%       | 0.09      | 4          |
| pokerwicket      | 9%       | 0.09      | 2          |
| common           | 5%       | 0.05      | 23+        |
| db               | 4%       | 0.04      | 0          |
| pokerengine      | 2%       | 0.02      | 1          |
| server           | 1%       | 0.01      | 0          |
| udp              | 1%       | 0.01      | 0          |
| gui              | 0%       | 0.00      | 0          |
| gamecommon       | 0%       | 0.00      | 0          |
| gameengine       | 0%       | 0.00      | 0          |

## Design Decisions

- **No AssertJ Swing.** GUI testing done via Dev Control Server API + screenshots,
  not by driving Swing components directly. AssertJ Swing does not work well in this project.
- **AssertJ core retained** for assertions (`assertThat().isEqualTo()`) — all existing tests use it.
- **JUnit 5** for all new tests. No JUnit 4.
- **Mockito** for mocking where needed.
- **E2E tests via Dev Control Server** HTTP API (already built out). Tagged `@Tag("e2e")`
  and `@Tag("slow")` to exclude from fast builds.
- **Module-by-module JaCoCo thresholds** raised after each phase to prevent regression.

## Phase 1: gamecommon + udp (~120 tests)

**Priority:** Highest ROI — zero-coverage modules with pure, deterministic, testable logic.

### gamecommon (60-70 tests)

Target classes:

- **`GameState` / `GameStateEntry`** — binary marshal/unmarshal round-trip tests.
  Verify state can be serialized, deserialized, and compared for equality.
- **`Border`, `BorderPoint`, `Territory`, `Territories`** — geometry operations,
  boundary conditions, empty collections, coordinate math.
- **`GamePieceContainerImpl`** — add/remove/query piece operations, capacity limits.
- **`GamePlayer`, `GamePlayerList`** — property accessors, list operations, ordering.
- **`GamePhase`, `GamePhases`** — phase registration, lookup, transitions.
- **`GameboardConfig`, `GamedefConfig`** — config parsing from XML, defaults.
- **`SaveDetails`, `SaveFile`** — save metadata accessors, file path handling.

### udp (50-60 tests)

Target classes:

- **`IncomingQueue`, `OutgoingQueue`** — FIFO ordering, capacity limits, empty queue behavior.
- **`AckList`** — acknowledgment tracking, timeout expiry, duplicate detection.
- **`UDPMessage`** — message structure, serialization/deserialization round-trips.
- **`ByteData`, `UDPData`** — binary data encoding/decoding, boundary values.
- **`UDPLink`** — connection lifecycle state machine (created → connected → closed).
- **`UDPLinkMonitor`** — health check logic, timeout detection.

### Expected coverage lift

- gamecommon: 0% → ~45%
- udp: 1% → ~50%

## Phase 2: common + server (~65 tests)

### common (40-50 tests)

Target classes:

- **`SecurityUtils`** — encryption/hashing correctness, known-answer tests.
- **`PasswordGenerator`** — output format, length constraints, character set coverage.
- **`RandomGUID`** — uniqueness across generations, format validation (UUID pattern).
- **`EscapeStringTokenizer`** — parsing edge cases: empty strings, escaped delimiters,
  consecutive delimiters, nested escapes.
- **`Format`** — string conversion, number formatting, locale edge cases.
- **`ZipUtil`** — compress/decompress round-trip, empty input, large input.
- **`SimpleXMLEncoder`** — XML encoding/decoding round-trip, special characters, nested objects.
- **`CommandLine`** — argument parsing: flags, values, missing args, unknown args.
- **`TypedHashMap`** — type-safe get/put, type mismatch handling.

### server (15-25 tests)

Target classes:

- **`ThreadPool`** — worker lifecycle: add/remove workers, pool sizing, shutdown behavior.
- **`WorkerPool`** — pool management: creation, assignment, return, pool exhaustion.
- **`GameServer`** — initialization/shutdown sequence, port binding.
- **`ServerSecurityProvider`** — authentication logic, credential validation.

### Expected coverage lift

- common: 5% → ~25%
- server: 1% → ~40%

## Phase 3: pokerengine + gameengine (~60 tests)

### pokerengine (20-30 tests)

Target classes:

- **`HandInfoFaster`** — hand evaluation correctness: compare against known hands,
  verify ranking order (royal flush > straight flush > ... > high card).
- **`PayoutCalculator`** — payout distribution math: heads-up, 3-way,
  full table, rounding edge cases, odd chip handling.
- **`LevelValidator`** — blind level validation: valid progressions, gaps, invalid configs.
- **Model serialization** — round-trip tests for `OnlineProfile`, `TournamentHistory`,
  `BlindTemplate`, `PayoutPreset`.

### gameengine (20-30 tests)

Target classes:

- **`SaveGame` / `LoadSavedGame`** — save/load round-trip, file handling, corrupt file recovery.
- **`GameMessenger`** — message dispatch logic, message type routing.
- **`GameStateFactory`** — state creation from byte arrays, files, empty state.
- **`GameConfigUtils`** — configuration utility methods.
- Skip all Phase/UI/Dialog classes (50+ files that are pure Swing).

### Expected coverage lift

- pokerengine: 2% → ~20%
- gameengine: 0% → ~15% (limited by large number of untestable UI classes)

## Phase 4: E2E Tests via Dev Control Server (~25 tests)

### Test infrastructure

- JUnit 5 test class that:
  1. Builds the fat JAR with `-P dev` (if not already built)
  2. Starts DDPoker process
  3. Waits for Control Server health endpoint
  4. Runs test scenarios
  5. Shuts down process
- Helper methods for:
  - Polling game state (`/state`)
  - Submitting actions (`/action`)
  - Starting games (`/game/start`)
  - Waiting for human turn
- Tagged `@Tag("e2e")` and `@Tag("slow")`
- Deterministic scenarios via card injection where possible

### Test scenarios

**Game lifecycle:**
- Start a practice game with 3 players, play through completion
- Start a practice game with 9 players, verify all seats filled
- Verify game ends cleanly when one player has all chips

**Tournament flow:**
- Blinds increase correctly over time
- Players eliminated at correct chip counts
- Final table consolidation with correct player count

**Hand mechanics:**
- Multi-hand sequences: verify chip conservation across N hands
- All-in scenarios: side pot creation and distribution
- Fold-around: verify pot awarded to last standing

**Action validation:**
- Submit invalid action type → verify error response
- Submit action when not human turn → verify error response
- Verify available actions match input mode

**State verification:**
- Game state JSON contains expected fields at each stage
- Player chip counts sum to total chips in play
- Community cards appear in correct order (flop/turn/river)

### Expected value

Catches integration regressions that unit tests miss. Validates the full game stack
works end-to-end: UI → game engine → AI → server → state management.

## Phase 5: Raise Existing Thresholds + gui (~50 tests)

### pokergameserver (36% → ~45%, ~20 tests)

- Fill gaps found during E2E testing
- Additional controller edge cases and error paths
- WebSocket message handling edge cases

### poker module (19% → ~30%, ~20 tests)

- Game table logic, player action validation
- Practice game integration with embedded server
- UI-independent game state management

### gui (0% → ~5%, 10-15 tests)

- **`TextUtil`** — text formatting pure functions (no Swing dependency)
- **`GuiUtils`** — utility methods (non-UI portions only)
- Skip all Swing component and L&F code entirely
- Visual testing handled by Phase 4 E2E tests + Control Server

### Expected coverage lift

- pokergameserver: 36% → ~45%
- poker: 19% → ~30%
- gui: 0% → ~5%

## JaCoCo Threshold Strategy

After each phase, raise the module's JaCoCo minimum in its `pom.xml` to lock in gains
and prevent regression:

| Module           | Current | After Phase | New Threshold |
|------------------|---------|-------------|---------------|
| gamecommon       | 0.00    | Phase 1     | 0.40          |
| udp              | 0.01    | Phase 1     | 0.45          |
| common           | 0.05    | Phase 2     | 0.20          |
| server           | 0.01    | Phase 2     | 0.35          |
| pokerengine      | 0.02    | Phase 3     | 0.15          |
| gameengine       | 0.00    | Phase 3     | 0.10          |
| pokergameserver  | 0.36    | Phase 5     | 0.42          |
| poker            | 0.15    | Phase 5     | 0.25          |
| gui              | 0.00    | Phase 5     | 0.03          |

## Test Conventions

- **Framework:** JUnit 5 + AssertJ core (no AssertJ Swing)
- **Naming:** `ClassNameTest.java` in matching package under `src/test/java`
- **Style:** Arrange/Act/Assert pattern
- **Method naming:** `should_DoSomething_When_Condition()` or descriptive `@DisplayName`
- **Tags:**
  - `@Tag("e2e")` — E2E tests requiring running application
  - `@Tag("slow")` — tests taking >5 seconds
  - `@Tag("integration")` — tests requiring external resources
- **No changes to existing tests** unless fixing a discovered bug
- **Reuse existing test infrastructure:** `ConfigTestHelper`, mock patterns from pokergameserver

## Success Criteria

- Overall project coverage reaches 60%+
- Every module with source code has a non-zero JaCoCo threshold
- E2E test suite validates full game lifecycle
- All tests pass in `mvn test` (fast tests) and `mvn verify -P coverage` (full suite)
- No flaky tests introduced (timing-sensitive tests documented in docs/memory.md)
