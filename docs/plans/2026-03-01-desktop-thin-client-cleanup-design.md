# Desktop Thin-Client Cleanup Design

**Status:** APPROVED
**Goal:** Remove all poker logic from the desktop client. The client becomes pure UI + WebSocket communication — zero poker math, zero game rules, zero AI support code.

## Motivation

The AI consolidation plan (completed 2026-03-01) removed high-level AI classes (`PokerAI`, `V1Player`, `V2Player`, `RuleEngine`) but left behind:
- ~2,500 lines of dead code (zero production references)
- ~560 lines of duplicate classes already in `pokergamecore`
- ~4,800 lines of poker math libraries still used by UI panels
- ~700 lines of AI support code (`PocketWeights`) for dashboard visualization

The server already has `AdvisorService` (hand eval + equity + pot odds via WebSocket `ADVISOR_UPDATE`) and `PokerSimulationService` (Monte Carlo equity via REST). The desktop client should consume these instead of computing locally.

## Architecture

```
Desktop Client (After Cleanup)          Embedded Server (Spring Boot)
────────────────────────────────────────────────────────────────────

Swing UI Panels                         AdvisorService (extended)
  ImproveOdds dashboard ──────────────── ADVISOR_UPDATE.improvementOdds
  PokerStatsPanel ────────────────────── ADVISOR_UPDATE.handPotential
  HandStrengthDash ───────────────────── ADVISOR_UPDATE.equity (existing)
  PokerSimulatorPanel ────── REST ─────► POST /api/v1/poker/simulate
  PokerShowdownPanel ─────── REST ─────► POST /api/v1/poker/simulate
  WeightGridPanel ────────── REMOVED

Zero poker calculation code on client.
```

## Three-Phase Approach

### Phase 1: Dead Code + Duplicate Removal (~3,060 lines)

Low risk. Delete unreferenced code and their tests. No behavior changes.

**Production files to delete:**

| File | Lines | Reason |
|------|-------|--------|
| `HandStrength.java` | 289 | Zero references in production code |
| `HoldemExpert.java` | 245 | Only referenced in comments |
| `OtherTables.java` | 433 | Zero references |
| `HandLadder.java` | 339 | Created in PokerStatsPanel but never rendered |
| `ai/AIOutcome.java` | 366 | Old AI decision logic — callers removed |
| `ai/BetRange.java` | 270 | Old bet sizing — callers removed |
| `ai/BooleanTracker.java` | 215 | Old tracking — callers removed |
| `ai/FloatTracker.java` | 197 | Old tracking — callers removed |
| `ai/PocketRanks.java` | 186 | No production usage; duplicate exists in pokergamecore |
| `ai/PocketScores.java` | 164 | No production usage; duplicate exists in pokergamecore |
| `ai/HandProbabilityMatrix.java` | 119 | Mostly commented-out; zero callers |

**Test files to delete:**

| File | Reason |
|------|--------|
| `HandStrengthTest.java` | Tests dead class |
| `ai/AIOutcomeIntegrationTest.java` | Tests dead class |
| `ai/BetRangeTest.java` | Tests dead class |
| `ai/BooleanTrackerTest.java` | Tests dead class |
| `ai/FloatTrackerTest.java` | Tests dead class |
| `ai/PocketRanksTest.java` | Tests dead class |
| `ai/PocketScoresTest.java` | Tests dead class |
| `ai/HandProbabilityMatrixTest.java` | Tests dead class |

**Cleanup:** Remove any imports or commented-out references to deleted classes in remaining files. Remove `HandLadder` usage from `PokerStatsPanel`.

### Phase 2: Extend Server APIs

Add the missing server-side capabilities that desktop UI features currently compute locally. No new endpoints — extend existing services.

**2a. Improvement odds in ADVISOR_UPDATE**

Extend `AdvisorResult` to include improvement odds (replaces client-side `HandFutures`):
```json
{
  "advisor": {
    "equity": 72.5,
    "improvementOdds": {
      "FLUSH": 19.1,
      "STRAIGHT": 16.5,
      "FULL_HOUSE": 8.7,
      "TRIPS": 4.3
    }
  }
}
```
Port `HandFutures` draw-detection logic into `AdvisorService`. Compute only on flop/turn (meaningless on river). Include in existing `ADVISOR_UPDATE` messages.

**2b. Hand potential in ADVISOR_UPDATE**

Extend `AdvisorResult` to include hand potential data (replaces client-side `HandPotential`):
```json
{
  "advisor": {
    "handPotential": {
      "positivePercent": 45.2,
      "negativePercent": 12.1,
      "handTypeBreakdown": [
        {"type": "ONE_PAIR", "percent": 32.1},
        {"type": "TWO_PAIR", "percent": 18.4}
      ]
    }
  }
}
```
Port `HandPotential` analysis logic into `AdvisorService`. Compute on flop/turn only.

**2c. Multi-hand showdown simulation**

Extend `PokerSimulationService` to support:
- Multiple known player hands (for showdown equity panel)
- Group-by-hand-category mode (for simulator panel's statistical breakdown)
- Exhaustive iteration option (for small remaining deck sizes)

The REST endpoint `POST /api/v1/poker/simulate` already exists. Extend the request/response to handle these modes.

### Phase 3: Migrate UI Panels + Delete Client Math (~5,400 lines)

Switch each desktop UI panel to consume server-provided data, then delete the client-side poker math libraries.

**UI panel migration:**

| Panel | Currently Uses | Replace With |
|-------|---------------|-------------|
| `ImproveOdds` (dashboard) | `new HandFutures(...)` | Read `ADVISOR_UPDATE.improvementOdds` from game state |
| `PokerStatsPanel` | `new HandPotential(...)` | Read `ADVISOR_UPDATE.handPotential` from game state |
| `PokerStatsPanel` (strength) | `new HandStrength()` | Read `ADVISOR_UPDATE.equity` (already available) |
| `PokerSimulatorPanel` | `HoldemSimulator.simulate()` | REST call to `POST /api/v1/poker/simulate` |
| `PokerShowdownPanel` | `HoldemSimulator.simulate/iterate()` | REST call to `POST /api/v1/poker/simulate` |
| `HandStrengthDash` | `player.getHandStrength()` | Already reads cached value — no change |
| `WeightGridPanel` | `PocketWeights.getInstance()` | **Remove feature** (AI debugging tool, not user-facing) |

**Production files to delete after migration:**

| File | Lines |
|------|-------|
| `HoldemSimulator.java` | 820 |
| `HandPotential.java` | 778 |
| `HandFutures.java` | 321 |
| `HandInfo.java` | 1,039 |
| `HandInfoFast.java` | 835 |
| `ai/PocketWeights.java` | 711 |
| `ai/PocketOdds.java` | 208 |
| `ai/SimpleBias.java` | 219 |
| `ai/PocketMatrixByte.java` | ~100 |
| `ai/PocketMatrixFloat.java` | ~100 |
| `ai/PocketMatrixInt.java` | ~100 |
| `ai/PocketMatrixShort.java` | ~100 |
| `ai/PocketMatrixString.java` | ~100 |

**Test files to delete:** Corresponding test classes for all of the above.

**WeightGridPanel removal:** `WeightGridPanel` in `ai/gui/` is the sole consumer of `PocketWeights`. It's an AI debugging visualization (shows inferred opponent hand distributions). Remove the panel and its `gamedef.xml` registration. The `ai/gui/` package has other panels (`PlayerTypeDialog`, `HandSelectionPanel`, etc.) that are UI-only config editors — those stay.

## Decisions

1. **PocketWeights removed, not migrated.** It's a development/debugging tool, not a user feature. Migrating it would require the server to maintain per-player hand inference state and expose it via WebSocket — significant complexity for no user value.

2. **HandInfo/HandInfoFast deleted from client.** The server's `AdvisorService` already uses `HandInfoFaster` (from `pokerengine` module, shared). The client never needs to evaluate hands directly.

3. **No new REST endpoints.** Extend existing `POST /api/v1/poker/simulate` rather than creating separate endpoints. The `ADVISOR_UPDATE` WebSocket message is the primary vehicle for real-time data.

4. **Phase order is mandatory.** Phase 1 has zero risk. Phase 2 creates the server APIs. Phase 3 depends on Phase 2 being complete before client math can be removed.

## Verification

After each phase:
- `cd code && mvn test -P dev` passes
- Desktop practice game works (AI makes decisions, human can play)
- Dashboard displays correct data
- Simulator/Showdown panels function correctly (Phase 3)
