# Plan: Dashboard Strict Widget Coverage

Status: active
Created: 2026-02-24
Scope: `.claude/scripts/scenarios`, `.claude/test-plans`, `.claude/guides`, `code/poker/src/dev/java/com/donohoedigital/games/poker/control`

---

## Goal

Upgrade dashboard coverage from broad smoke checks to strict, widget-by-widget assertions that prove:

- each widget shows the right data,
- at the right game phase/timing,
- with stable behavior across transitions and restart.

---

## Success Criteria

1. `D-001..D-013` are validated with hard assertions (no WARN-based pass path).
2. Each widget has explicit phase visibility rules and value contracts.
3. Widget snapshots include freshness metadata (`updatedAtMs`, `sourceHandNumber`, `sourceRound`, `sourceStateSeq` or equivalent).
4. Scenario checks assert both correctness and timing bounds after key transitions.
5. Coverage matrix can promote dashboard rows from `Smoke-Hard` to `Strict-API` or `Strict-Real` with evidence.

---

## Non-Goals

- Pixel-perfect visual testing.
- Replacing existing gameplay realism work outside dashboard scope.

---

## Workstreams

### WS1 - Widget Contract Spec (D-001..D-013)

Create a machine-checkable contract table per widget:

- expected presence by phase (`PRE_FLOP/FLOP/TURN/RIVER/BETWEEN_HANDS/REBUY_CHECK/GAME_OVER`),
- required fields and allowed null states,
- value invariants and source-of-truth mapping to `/state`.

Deliverables:

- Add contract section to `.claude/guides/desktop-client-testing.md`.
- Add contract mapping notes to `.claude/test-plans/local-features-strict-coverage-matrix.md`.

### WS2 - Observability Expansion

Add normalized semantic endpoint for strict assertions:

- `GET /ui/dashboard/widgets`
  - returns per-widget structured payloads (clock, advisor, strength/odds, pot odds, improve odds, hand/table/rank/up-next, cheat/debug),
  - includes freshness/timestamp metadata,
  - includes `visibility.expected`, `visibility.actual`, and optional `reason` when hidden.

Keep `GET /ui/dashboard` for layout/customization model and add summary mirror in `/state.ui.dashboardWidgets` where useful.

### WS3 - Strict Scenario Suite

Implement or split strict scenario scripts by widget domain:

- `test-dashboard-clock-strict.sh`
- `test-dashboard-advisor-strict.sh`
- `test-dashboard-odds-strict.sh`
- `test-dashboard-player-table-rank-strict.sh`
- `test-dashboard-cheat-debug-strict.sh`

Rules:

- hard fail on contract violations,
- no silent `|| true` on critical checks,
- assert phase transitions then assert widget state within bounded time window.

### WS4 - Timing Correctness Harness

Add shared helpers in `.claude/scripts/scenarios/lib.sh`:

- `wait_for_widget_state(widget, predicate, timeout)`
- `assert_widget_fresh(widget, max_age_ms)`
- `assert_widget_matches_state(widget, mapper)`

Require each strict script to validate update timeliness after:

- street changes,
- human-turn transitions,
- hand completion/start,
- rebuy offer/apply boundaries (where applicable).

### WS5 - Promotion + Evidence

- Replace/retire old dashboard WARN-style checks.
- Update matrix classifications and rationale.
- Document exact verification commands and expected runtime in the guide.

---

## Phased Execution

### Phase A - Contracts First

- Lock per-widget contracts and phase rules before coding endpoint fields.

Exit criteria:

- every D-001..D-013 has a signed-off contract line item.

### Phase B - Endpoint + Metadata

- Implement `/ui/dashboard/widgets` and optional `/state` mirror.
- Add focused unit tests in `GameControlServerTest` for no-game + basic schema behavior.

Exit criteria:

- endpoint returns stable schema + freshness metadata.

### Phase C - Strict Scripts

- Add strict widget scripts and migrate/trim existing dashboard scripts.

Exit criteria:

- strict scripts fail on injected/forced contract violations.

### Phase D - Promotion

- Reclassify dashboard rows in coverage matrix with evidence links.

Exit criteria:

- `D-001..D-013` no longer depends on WARN paths.

---

## Verification Commands

- `mvn -pl poker -P dev -Dtest=GameControlServerTest test -f code/pom.xml`
- `bash .claude/scripts/scenarios/test-dashboard-panels.sh --skip-build`
- new strict dashboard scripts (WS3)
- targeted release-gate subset run after integration

---

## Risks and Mitigations

- **Risk:** Widget values are computed asynchronously and may race.
  - **Mitigation:** add freshness metadata + bounded wait helpers; assert eventual correctness within SLA window.
- **Risk:** Some widgets may not expose enough internals for strict semantic checks.
  - **Mitigation:** prefer minimal read-only API additions over brittle UI text scraping.
- **Risk:** Increased scenario runtime.
  - **Mitigation:** keep scripts focused, parallelize where independent, and use `--skip-build` after first compile.

---

## Related Artifacts

- `.claude/plans/DESKTOP-CLIENT-TEST-REALISM-OVERHAUL.md`
- `.claude/test-plans/local-features-test-plan.md`
- `.claude/test-plans/local-features-strict-coverage-matrix.md`

---

## Execution Record

### 2026-02-24 - Build Session 1 (active)

- Started plan execution (status moved to `active`).
- Began Phase A by drafting strict dashboard widget contracts (D-001..D-013) in the desktop testing guide.
- Updated strict coverage matrix notes to call out contract completion progress and remaining semantic API gaps for strict promotion.

### 2026-02-24 - Build Session 2 (active)

- Began Phase B endpoint work:
  - added `GET /ui/dashboard/widgets` (`UiDashboardWidgetsHandler`) with semantic widget payloads and freshness metadata,
  - wired endpoint in `GameControlServer`.
- Added no-game schema tests for `/ui/dashboard/widgets` in `GameControlServerTest`.
- Updated desktop testing guide endpoint reference with `/ui/dashboard/widgets` payload intent.

### 2026-02-24 - Build Session 3 (active)

- Started WS3 strict script adoption with a new scenario:
  - added `test-dashboard-widgets-strict.sh` to assert D-001..D-013 core widget contracts using `/ui/dashboard/widgets` cross-checked against `/state`.
- Validated the new script on a live game run.
- Updated coverage/docs references to include the new strict dashboard script and current promotion status.

### 2026-02-24 - Build Session 4 (active)

- Added WS4 timing helpers in scenario library (`lib.sh`):
  - `wait_for_widget_state(...)`
  - `assert_widget_fresh(...)`
  - `assert_widget_matches_state(...)`
  - `now_ms()`
- Updated `test-dashboard-widgets-strict.sh` to use helper-driven assertions and timing checks:
  - hard cross-checks against `/state` now use `assert_widget_matches_state`,
  - transition timing now asserts `/ui/dashboard/widgets.sourceStateSeq` advances after a real human action,
  - freshness-after-action is asserted for key widgets (`clock`, `advisor`) with tight bounds.
- Improved `/ui/dashboard/widgets` state-sequence semantics so `sourceStateSeq` and `updatedAtMs` reflect observed game-state changes rather than request count.

### 2026-02-24 - Build Session 5 (active)

- Extended strict script branch coverage beyond pre-flop snapshots:
  - added active driving helpers inside `test-dashboard-widgets-strict.sh` to progress hands while minimizing fold risk,
  - added explicit FLOP and TURN branch assertions for `handStrength` and `improveOdds` contracts (`expected*` flags, card counts, computed improve odds presence).
- Re-validated strict scenario end-to-end with timing checks + post-flop branches enabled.

### 2026-02-24 - Build Session 6 (active)

- Hardened branch stability by restarting a compact 2-player game before post-flop branch checks.
- Expanded branch assertions:
  - added non-human branch checks (`advisor.data.isHumanTurn=false`),
  - added explicit RIVER branch checks for `handStrength` and `improveOdds` (`expectedImproveOdds=false`, `communityCardCount=5`, `totalImprovePercent=null`).
- Re-validated strict dashboard scenario and dashboard-panels regression.

### 2026-02-24 - Build Session 7 (active)

- Extended strict script with a game-over branch focused on rank contract consistency:
  - starts a compact 2-player no-rebuy game,
  - accelerates elimination with high blind level,
  - asserts `/ui/dashboard/widgets.rank.data.playersRemaining` matches final `/state.tournament.playersRemaining` at terminal conditions.
- Re-validated strict script + dashboard-panels regression after game-over branch addition.
