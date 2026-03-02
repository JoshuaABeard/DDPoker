# Plan: Desktop + Server Bug Cleanup

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Status:** COMPLETED (2026-03-02) — all bugs fixed across feature/desktop-thin-client-cleanup work
**Created:** 2026-02-23
**Scope:** `code/poker` (desktop client) and `code/pokerserver` (server)

---

## Goal

Reduce real user-facing bugs in the Java desktop client while tightening server reliability where server behavior contributes to client breakage, reconnect pain, or state inconsistencies.

This plan prioritizes correctness and stability first, then UX polish and maintainability.

Phase 1 bug ledger: `docs/plans/2026-02-23-desktop-server-bug-ledger.md`

---

## Initial Concern Inventory (from code review)

### High-risk / high-value

1. **Threading and EDT safety in desktop UI paths**
   - Multiple ad-hoc background threads and mixed UI-thread handoffs (`SwingUtilities.invokeLater(...)` used in many call paths).
   - Legacy pattern of `invokeLater(new Thread(...))` appears in UI components and should be normalized.
   - Targets: `PokerStatsPanel`, `PokerSimulatorPanel`, `ShowTournamentTable`, `Lobby`, `OnlineConfiguration`, `GamePrefsPanel`.

2. **Connection/reconnect lifecycle edge cases**
   - WebSocket and REST clients are mostly resilient but still rely on broad exception handling and reconnect scheduling that can hide root causes.
   - Reconnect scheduler lifecycle should be verified for leaks and duplicate reconnect attempts.
   - Targets: `online/LobbyChatWebSocketClient`, `online/WebSocketGameClient`, `online/RestAuthClient`, `online/RestGameClient`.

3. **Hidden failures due to broad catches (`Exception`/`Throwable`)**
   - Several catch-all blocks convert actionable errors into generic warnings, making production bugs hard to diagnose.
   - Targets: `DashboardManager`, `PokerMain`, online clients in `code/poker`, purger tools in `code/pokerserver`.

### Medium-risk

5. **State-machine/input-mode transitions**
   - Tournament/table input mode transitions are complex and historically bug-prone.
   - Verify mode transitions in human-turn and rebuy flows through control-server automation.
   - Primary target: `ShowTournamentTable` and related action dispatch.

6. **Legacy utility/serialization resilience**
   - Preference/state demarshal paths currently tolerate malformed state but may mask data corruption and carry stale state.
   - Targets: dashboard preference load/save and profile/load persistence surfaces.

---

## Execution Plan

### Phase 1 - Baseline and Triage

- [ ] Create a reproducible bug ledger (issue list) grouped by severity: crash, incorrect game logic, UI/UX regression, networking/reconnect.
- [ ] Run baseline test suites and capture failures/flakes:
  - `mvn test -P dev`
  - targeted reruns for `poker` and `pokerserver` when needed
- [ ] Add/refresh automated repro tests for currently known client issues before production fixes.

**Exit criteria:** every selected bug has a reproducible case (test or scripted scenario), owner, and severity.

### Phase 2 - Crash/Correctness Fix Wave (Client-first)

- [ ] Fix deterministic crashers and invalid-state errors in desktop action flows.
- [ ] Fix incorrect game-state transitions that block user action (turn handling, rebuy/check/call/raise modes, action availability).
- [ ] Validate chip-conservation and hand-state invariants for affected flows.

**Exit criteria:** no P0/P1 crashers remain in selected flows; key invariants covered by tests.

### Phase 3 - Networking and Reconnect Hardening

- [ ] Harden WebSocket reconnect behavior (single reconnect loop, clean disconnect semantics, no leaked scheduled tasks).
- [ ] Improve REST client error mapping so user-visible failures are specific and diagnosable.
- [ ] Ensure client/server state synchronization remains correct after reconnect.

**Exit criteria:** reconnect scenarios pass scripted tests; no duplicate reconnect loop behavior observed.

### Phase 4 - Concurrency and EDT Cleanup

- [ ] Replace/normalize legacy `invokeLater(new Thread(...))` and other thread-hand-off patterns.
- [ ] Ensure all Swing component updates occur on EDT and heavy computations stay off EDT.
- [ ] Add regression tests around async UI updates where practical.

**Exit criteria:** audited hotspots have explicit EDT-safe paths and no new threading regressions.

### Phase 5 - Server Reliability Touch-ups (supporting client stability)

- [ ] Tighten error handling in server-side tools/components where broad catches currently hide failures.
- [ ] Improve observability around error paths that impact clients (connection, auth, game state API).
- [ ] Verify server behavior remains stable under client reconnect and edge-case action timing.

**Exit criteria:** server logs provide actionable diagnostics; no silent-failure paths in touched components.

---

## Out of Scope (per current direction)

- No AI strategy or AI randomness changes in this cleanup wave.
- Specifically excluded: `ai/ClientStrategyProvider`, `ai/AIOutcome`, `ai/BetRange`, and related behavior tuning.

---

## Test Strategy

- Unit tests for each fixed bug where feasible.
- Integration tests for client/server interaction points (REST + WebSocket).
- Control-server scenario tests for turn state, action availability, and hand progression.
- Full verification pass before completion:
  - `mvn test -P dev`
  - module-targeted reruns for changed modules

---

## Prioritization Rules

1. Fix bugs that lose/corrupt game state before cosmetic issues.
2. Fix user-visible blockers before low-frequency edge cases.
3. Prefer surgical fixes with tests over broad refactors.
4. If a bug cannot be reproduced, do not patch blindly; first add instrumentation/repro.

---

## Deliverables

- Bug ledger with repro steps and severity.
- Incremental fix PR-sized batches (client and server), each with tests.
- Updated learnings for newly discovered gotchas.
- Review handoff document when cleanup wave completes.
