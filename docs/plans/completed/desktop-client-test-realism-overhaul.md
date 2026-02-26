# Plan: Desktop Client Test Realism Overhaul

**Status:** COMPLETED
Created: 2026-02-23
Scope: `.claude/test-plans`, `.claude/scripts/scenarios`, `code/poker/src/dev/java/com/donohoedigital/games/poker/control`

---

## Goal

Make desktop-client testing expose real bugs instead of passing around them, while keeping scenarios aligned with real poker table behavior.

---

## Why This Plan Exists

Current coverage breadth is good, but a material portion of scripts still:

- treat known defects as WARN instead of FAIL,
- rely on cheat/state mutation paths that bypass realistic gameplay,
- validate API acceptance rather than poker correctness,
- or claim coverage that the control API cannot actually observe.

This plan converts those weak points into strict, realistic assertions and identifies required control-server gaps.

---

## Current Baseline (Reviewed)

Strong foundation scripts to keep as regression anchors:

- `test-chip-conservation.sh`
- `test-all-actions.sh`
- `test-hand-flow.sh`
- `test-split-pot.sh`

Priority scripts requiring redesign or hardening:

- `test-allin-side-pot.sh`
- `test-blind-posting.sh`
- `test-rebuy-addon.sh`
- `test-keyboard-shortcuts.sh`
- `test-multi-table.sh`
- `test-large-field.sh`
- `test-poker-night.sh`
- `test-navigate.sh`
- `test-save-load-extended.sh`
- `test-game-info-data.sh`

---

## Related Plans

- **[Desktop + Server Bug Cleanup](DESKTOP-SERVER-BUG-CLEANUP-PLAN.md):** Bug fixes and test hardening should happen together. When a bug is fixed in the cleanup plan, the corresponding scenario test should be promoted from Smoke to Strict simultaneously. Phase 2 crash/correctness fixes feed directly into Phase 1 credibility work here.
- **[Coverage Matrix](../test-plans/local-features-strict-coverage-matrix.md):** Quantitative tracking of classification promotions across phases.

---

## Execution Phases

### Phase 1 - Credibility Pass (No New API Required)

- Replace WARN-only pass-throughs on P0/P1 gameplay checks with hard FAIL or explicit SKIP with blocker reason.
- Fix incorrect field-path assertions:
  - `/validate` shape usage in save/load extended scenarios.
  - hand number source (`tournament.handNumber`).
- Add strict helper utilities in `lib.sh`:
  - `assert_json_field`
  - `assert_action_accepted`
  - `assert_mode_transition`
- Remove silent `|| true` for critical action paths where failure should fail the test.

Exit criteria:

- No critical gameplay script passes when expected assertions fail.
- Every non-fatal skip includes `blocked_by_api:<id>`.

### Phase 1.5 - Hand Result API (Parallel with Phase 2)

Implement `API-HR-01` — the hand-result payload endpoint — ahead of the other API gap closures. This is the single highest-value API addition because it unblocks poker-correctness assertions in Phase 2 rewrites: without knowing who won, what hand they held, and what they were paid, tests cannot validate poker rules.

- Add hand-result payload to `/state` or a new `/hand-result` endpoint:
  - Winners (seat, hand class, hand description).
  - Side-pot allocation and payout deltas.
  - Pot distribution breakdown.

Exit criteria:

- Phase 2 scripts can assert hand outcomes, not just chip conservation.

### Phase 2 - Realism Pass (Gameplay-Native Scenarios)

- Rewrite cheat-heavy cases to use natural poker flows first:
  - blind posting and partial-blind behavior,
  - rebuy/add-on windows,
  - level/clock transitions,
  - heads-up behavior.
- Require poker-rule assertions, not only state existence:
  - legal action ordering,
  - posted amounts,
  - side-pot eligibility,
  - result and chip deltas.

Exit criteria:

- Updated scripts reflect real table behavior and fail on rule violations.

### Phase 3 - Remaining Control API Gap Closure

Add or extend control-server endpoints/state fields for remaining unobservable behavior (API-HR-01 was pulled into Phase 1.5):

- `API-MT-01`: full multi-table visibility or table summary aggregation,
- `API-NAV-01`: durable navigation confirmation fields (target phase/screen reached),
- `API-UI-01`: dashboard/prefs editor observability hooks for layout/customization assertions,
- deterministic event stream for street transitions and action sequencing.

Exit criteria:

- previously blocked tests move from SKIP to executable assertions.

### Phase 4 - Suite Rationalization

- Merge duplicate or overlapping scripts (`navigate` variants, option variants).
- Publish one coverage matrix mapping `local-features-test-plan.md` IDs to:
  - automated strict,
  - automated smoke (hard / soft),
  - manual only,
  - blocked by API.
- Create `run-release-gate.sh` — an automated runner that executes the 12 release-gate scripts sequentially, collects exit codes, and prints a pass/fail summary with non-zero exit on any failure.
- Define the "release gate" subset as a concrete, executable artifact (not just a list).

Exit criteria:

- One authoritative automation map with clear ownership.
- `run-release-gate.sh` passes end-to-end on a clean build.

---

## New High-Value Test Cases to Add

1. Heads-up blind/button rotation correctness over multiple hands.
2. Side-pot eligibility and distribution correctness (not only conservation).
3. Split-pot odd-chip handling.
4. Min-raise reopen rule after short all-in raise.
5. Rebuy boundary exactly at last-rebuy-level transition.
6. Save/load fidelity mid-hand (pot, acting seat, legal actions).
7. Tournament payout and profile-stat reconciliation at game over.
8. Invalid-action immutability checks (409 + no state drift).

---

## Plan Hygiene Resolution

Completed/no-longer-active plan housekeeping performed as part of this effort:

- Archive completed control-server plan:
  - moved `.claude/plans/control-server-improvements.md`
  - to `.claude/plans/completed/control-server-improvements.md`
  - status normalized to `completed`.
- Normalize plan status vocabulary to protocol (`draft|active|paused|completed`) in active plan set.

---

## Recommended Execution Order (Across Both Plans)

Optimal sequencing when working on both this plan and the [bug cleanup plan](DESKTOP-SERVER-BUG-CLEANUP-PLAN.md):

1. **Quick wins:** Create `run-release-gate.sh`. Document undocumented control-server endpoints. Fix bug ledger gaps.
2. **Phase 1 (credibility pass):** WARN→FAIL conversion, fix field paths, add strict helpers — delivers immediate credibility.
3. **Phase 1.5 (API-HR-01):** Implement hand-result payload — unblocks the most valuable Phase 2 rewrites.
4. **Bug cleanup Phase 2 + Realism Phase 2 in parallel:** Fix crash/correctness bugs and harden their scenario tests together.
5. **Remaining phases** flow naturally after that foundation.

---

## Verification and Reporting

- Each phase ends with a script-level change log (what was hardened, removed, or blocked).
- Final output is a runbook listing:
  - strict pass/fail scripts,
  - expected runtime,
  - known blockers with API issue IDs,
  - manual follow-ups.

---

## Execution Record

### 2026-02-23 — Build Session 1 (active)

- Implemented API-HR-01 as `handResult` in `/state` with winners, `handClass`, `handDescription`, pot breakdown, and payout deltas.
- Added hand-result capture/logging pipeline in the WebSocket desktop path to persist last completed hand per table.
- Added strict scenario helper functions in `lib.sh` (`assert_json_field`, `assert_action_accepted`, `assert_mode_transition`).
- Hardened immediate Phase 1 target scripts to strict failures:
  - `test-save-load-extended.sh`
  - `test-game-info-data.sh`
  - `test-navigate.sh`
  - `test-blind-posting.sh`
  - `test-allin-side-pot.sh`
- Added release-gate executable artifact: `.claude/scripts/scenarios/run-release-gate.sh`.
- Updated docs for hand-number path correction (`tournament.handNumber`) and `handResult` payload usage.

### 2026-02-24 — Build Session 2 (active)

- Tightened chip-conservation handling in release-gate scenarios:
  - converted soft warning paths to hard failures in non-cheat flows,
  - enforced final `/validate` chip-conservation checks broadly,
  - kept cheat chip-mutation as the explicit exception.
- Hardened release-gate scenarios for deterministic failure behavior:
  - stricter `test-game-start-params.sh` startup/dealer-seat validation with per-case runtime isolation,
  - stricter `test-rebuy-dialog.sh` (no skip-style pass-through on RB-015 path),
  - stricter `test-all-actions.sh` hand-completion enforcement and all-in opportunity handling,
  - stricter `test-allin-side-pot.sh` side-pot expectation and retry envelope.
- Standardized scenario log semantics to improve triage quality:
  - `FAIL:` = failing assertion,
  - `OK:` = satisfied assertion,
  - `INFO:` = non-fatal diagnostic context.
- Created focused commit for scenario strictness work:
  - `bd75edbc` — `test: tighten release-gate scenario assertions`.

### 2026-02-24 — Checkpoint before context reset

- Remaining near-term items for this plan:
  1. rerun full release-gate after latest stability edits to confirm repeatable green.

### 2026-02-24 — Build Session 3 (active)

- Completed strict hardening for the remaining Phase 1 immediate targets:
  - `test-game-info-data.sh`
  - `test-navigate.sh`
- Root cause found for intermittent startup hangs in scenario runs:
  - stale persistent profile history DB (`%APPDATA%/ddpoker/save/db/poker-*`) could stall the EDT at startup,
  - this blocked `/game/start` progression and left `/state` at `StartMenu` + `inputMode=NONE`.
- Mitigation added to scenario harness:
  - `lib.sh` now clears stale profile DB files on launch/cleanup alongside existing control DB cleanup.
- Verified targeted scripts pass after mitigation:
  - `test-game-info-data.sh --skip-build`
  - `test-navigate.sh --skip-build`

### 2026-02-24 — Build Session 4 (active)

- Continued Phase 2 realism hardening without full gate rerun:
  - `test-large-field.sh` now requires true tournament completion (removed partial-progress pass path).
  - `test-color-up.sh` now enforces strict level-advance and validation checks with deterministic state driving.
  - `test-rebuy-addon.sh` now validates invariants across multiple hands and tightens add-on/rebuy assertions.
  - `test-edge-cases.sh` now uses strict assertions with explicit `setChips` cheat-exception handling (input-mode invariants still required).
- Targeted verification completed:
  - `test-large-field.sh --skip-build`
  - `test-color-up.sh --skip-build`
  - `test-rebuy-addon.sh --skip-build`
  - `test-edge-cases.sh --skip-build`

### 2026-02-24 — Build Session 5 (active)

- Hardened clock/level scenario cluster:
  - `test-clock-state.sh`: removed soft warning paths for core fields and added strict `/validate` invariant checks.
  - `test-level-advance.sh`: strict `setLevel` acceptance, per-stage invariant validation, and explicit beyond-final-level blind-repeat verification.
  - `test-clock-pause.sh`: strict pause/resume action validation, strict freeze check while paused, strict tick-down check after resume, and strict invalid-action response verification.
- Targeted verification completed:
  - `test-clock-state.sh --skip-build`
  - `test-level-advance.sh --skip-build`
  - `test-clock-pause.sh --skip-build`

### 2026-02-24 — Build Session 6 (active)

- Added non-cheat clock progression semantics in `test-clock-state.sh`:
  - in `TIME` mode, script now requires observed `secondsRemaining` decrease within a bounded window,
  - this closes the prior gap where only clock-field presence was asserted.
- Targeted verification completed:
  - `test-clock-state.sh --skip-build`

### 2026-02-24 — Build Session 7 (active)

- Closed plan bookkeeping loop by refreshing coverage-matrix classifications/notes for recently hardened scripts (blind-posting, large-field, color-up, clock cluster, save/load, rebuy, split-pot).
- Continued P0/P1 strict-hardening work:
  - `test-rebuy-dialog.sh`: added boundary checks (reject REBUY outside `REBUY_CHECK`, reject immediate second REBUY) and stricter post-action invariants.
  - `test-save-load.sh`: added snapshot-identity restore assertions and strict post-load validation.
  - `test-save-load-extended.sh`: added post-save mutation/restore identity checks and stricter continuity assertions.
  - `test-allin-side-pot.sh`: strengthened side-pot and payout mapping assertions (with current payload-semantics caveat on aggregate delta sum).
  - `test-split-pot.sh`: rewritten as retry-based strict split-allocation verifier (winner multiplicity, odd-chip tolerance, payout mapping).
- Targeted verification completed:
  - `test-rebuy-dialog.sh --skip-build`
  - `test-save-load.sh --skip-build`
  - `test-save-load-extended.sh --skip-build`
  - `test-allin-side-pot.sh --skip-build`
  - `test-split-pot.sh --skip-build`

### 2026-02-24 — Build Session 8 (active)

- Completed control-API observability additions needed for stricter scenario promotion:
  - durable navigation status via `GET /navigate/status` and `/state.navigation`,
  - UI snapshot endpoint `GET /ui/state`,
  - director remote snapshot exposure via `/state.director` and `/state.tournament.remote`,
  - new rebuy lifecycle signal `tournament.rebuyOutcome` (`OFFERED`, `ACCEPT_SENT`, `DECLINE_SENT`, `APPLIED`).
- Updated strict scenarios to consume new observability:
  - `test-navigate.sh` now validates request-id application using `/navigate/status`,
  - `test-rebuy-dialog.sh` now validates rebuy outcome sequences/states from `/state`,
  - `test-multi-table.sh` now uses remote tournament snapshot fields for strict checks.
- Added/extended tests and docs for new APIs:
  - `GameControlServerTest` coverage for `/navigate/status` and `/ui/state`,
  - `WebSocketTournamentDirectorTest` coverage for rebuy outcome state signaling,
  - `.claude/guides/desktop-client-testing.md` updated with endpoint details and `tournament.rebuyOutcome` schema.
- Targeted verification completed:
  - `mvn -pl poker -P dev -Dtest=GameControlServerTest,WebSocketTournamentDirectorTest test -f code/pom.xml`
  - `test-navigate.sh --skip-build`
  - `test-multi-table.sh --skip-build`
  - `test-rebuy-dialog.sh --skip-build`

### 2026-02-24 — Build Session 9 (active)

- Continued Phase 3 `API-UI-01` depth work for layout assertions:
  - expanded `/ui/state` with `layout.dashboard` (panel/item/open-state/title/bounds snapshot),
  - expanded `/ui/state` with `layout.preferences` (GamePrefs tab count, selected tab, tab titles/bounds),
  - mirrored compact layout summary in `/state.ui.layout` for scripts already polling `/state`.
- Updated strict scenarios to consume new UI layout observability:
  - `test-dashboard-panels.sh` now hard-fails when dashboard layout snapshot is absent or structurally empty,
  - `test-preferences-full.sh` now navigates to `GamePrefs` and asserts preferences layout visibility/tab structure from `/ui/state`.
- Updated matrix/docs to reflect the API-UI unblock:
  - D-020..D-024 moved from `Blocked` to `Smoke-Hard` (with remaining customization-interaction gap tracked in notes),
  - desktop testing guide updated with new `layout` payload fields and `/state.ui.layout` summary.

### 2026-02-24 — Build Session 10 (active)

- Closed the remaining dashboard customization-interaction gap under `API-UI-01`:
  - added `GET/POST /ui/dashboard` for strict dashboard item model assertions and controlled customization actions,
  - supported actions: `SET_DISPLAYED`, `SET_OPEN`, `MOVE` (+ persisted preference save after mutations).
- Added strict scenario coverage for customization + persistence:
  - new `test-dashboard-customization.sh` validates hide/show, reorder, and restart persistence using `/ui/dashboard`.
- Extended docs/matrix tracking for the new endpoint and scenario coverage.
