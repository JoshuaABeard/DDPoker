# Local Features Strict Coverage Matrix

Status: active baseline
Date: 2026-02-23
Source plan: `.claude/test-plans/local-features-test-plan.md`

## Related Plans

- **[Test Realism Overhaul](../plans/DESKTOP-CLIENT-TEST-REALISM-OVERHAUL.md):** Phases 1-4 drive classification promotions tracked here.
- **[Bug Cleanup](../plans/DESKTOP-SERVER-BUG-CLEANUP-PLAN.md):** Bug fixes should be paired with test hardening — when a bug is fixed, promote the corresponding row.

## Classification Legend

- `Strict-Real`: hard fail assertions with realistic game flow.
- `Strict-API`: hard fail assertions, but primarily API contract/round-trip checks.
- `Smoke-Hard`: has real assertions (field checks, state transitions) but incomplete coverage — e.g., uses WARN on some checks, or covers 3 of 5 behaviors.
- `Smoke-Soft`: essentially a connectivity or API-acceptance test — checks HTTP 200 or field presence without meaningful correctness assertions.
- `Blocked`: cannot be validated strictly with current control API visibility.
- `Manual`: no effective automation coverage today.

## Coverage Matrix

| IDs | Current Class | Evidence | Notes / Next Action |
|---|---|---|---|
| L-001, L-002 | Strict-API | `test-app-launch.sh` | Health/version checks are strict; UI look/resize persistence still manual. |
| L-003..L-006 | Manual | — | Window behavior and persistence are not asserted by API. |
| L-010..L-017 | Smoke-Soft | `test-navigate.sh`, `test-main-menu-nav.sh` | `test-navigate.sh` now uses hard-fail + `/navigate/status` request-id confirmation; promote classification after next full-gate stability pass. |
| L-020..L-023 | Manual | — | First-launch wizard/profile UI flow not covered by scenario scripts. |
| PP-001..PP-009 | Smoke-Hard | `test-profile-management.sh` | CRUD is covered, but several warnings and weak default-profile assertions remain. |
| P-001..P-007 | Smoke-Hard | `test-tournament-profiles.sh` | Profile list/create/delete checks exist; built-in protections are not strict in all branches. |
| P-010..P-017 | Smoke-Hard | `test-tournament-profile-editor.sh` | General settings: field coverage exists for name, players, seats, game type; many checks soft/warn-based. |
| P-020..P-029 | Smoke-Hard | `test-tournament-profile-editor.sh` | Blind structure: level add/edit/delete/reorder, time vs hands mode; assertions are presence-only. |
| P-030..P-037 | Smoke-Hard | `test-tournament-profile-editor.sh` | Rebuys & add-ons: enable/disable, limits, chip amounts; toggles checked but gameplay effects not verified. |
| P-040..P-048 | Smoke-Hard | `test-tournament-profile-editor.sh` | Payouts: payout spots, percentages, satellite mode, house cut; validation checks are soft. |
| P-050..P-058 | Smoke-Hard | `test-tournament-profile-editor.sh` | Advanced options: late reg, timeouts, bounty, chip race, max raises, persistence; round-trip only. |
| G-001..G-006 | Strict-Real | `test-game-start-params.sh` | Strong start-parameter and state verification. |
| G-010..G-019 | Smoke-Hard | `test-hand-flow.sh` | Street progression is good; hole-card visibility and showdown/winner assertions are soft. |
| G-020..G-028 | Strict-Real | `test-all-actions.sh` | Strong action acceptance + invariant checks. |
| G-029, G-030 | Manual | — | Check/fold checkbox behavior not strictly automated. |
| G-035..G-046 | Smoke-Soft | `test-keyboard-shortcuts.sh` | D-key path is best-effort; needs deterministic strict path. |
| G-050..G-052 | Smoke-Hard | `test-blind-posting.sh` | Script now enforces strict blind/ante/short-stack outcomes; remaining gap is broader profile/time-control matrix coverage. |
| C-001..C-005 | Smoke-Hard | `test-clock-state.sh` | State-field assertions are strict with `/validate`; TIME-mode tick-down semantics now verified in-script (remaining gap: long-duration drift/tolerance). |
| C-010, C-014, C-015 | Smoke-Hard | `test-level-advance.sh` | Strict escalation and final-level-repeat checks now enforced; still cheat-driven rather than real timer transitions. |
| C-020..C-024 | Smoke-Hard | `test-color-up.sh` | Strict level-jump and post-jump invariants now enforced; still cheat-driven and lacks natural timer-driven color-up transitions. |
| C-030..C-032 | Smoke-Hard | `test-clock-state.sh` | TIME-mode tick-down semantics are strict; HANDS-mode progression is still partially asserted. |
| C-040, C-041 | Smoke-Hard | `test-clock-pause.sh` | Pause/resume and freeze/resume tick behavior are now strict; remaining gap is long-duration drift tolerance. |
| MT-001, MT-003, MT-006 | Smoke-Soft | `test-multi-table.sh` | Uses `tournament.remote`/director snapshot, but still does not prove true multi-table balancing in all runs. |
| MT-002, MT-004, MT-005, MT-007 | Blocked | `test-multi-table.sh` | Full table balancing/final-table assertions remain blocked when remote snapshots expose only the current/localized table view. |
| GI-001..GI-012 | Smoke-Hard | `test-game-info-data.sh` | Script now strict with handResult checks and fixed `tournament.handNumber` path; promote classification after next full-gate stability pass. |
| GI-020..GI-026 | Manual | — | Game-over dialog and play-again/quit UI paths not strictly automated. |
| RB-001..RB-003 | Strict-Real (partial) | `test-rebuy-dialog.sh` | Natural bust + decline/accept paths are exercised with durable `tournament.rebuyOutcome` assertions; remaining gap is explicit max-rebuy/limit policy assertions. |
| RB-004..RB-012 | Smoke-Hard | `test-rebuy-addon.sh`, `test-rebuy-dialog.sh` | Rebuy/add-on invariants are now stricter (`/validate` across hands, outcome-state proof); remaining gap is explicit prize-pool delta accounting and full boundary matrix. |
| A-001..A-006 | Smoke-Soft | `test-ai-types.sh` | Type listing checks exist; in-game type visibility still weak. |
| A-010..A-017 | Manual | — | Behavioral style assertions (tight/aggressive over many hands) not automated. |
| AD-001..AD-004 | Smoke-Hard | `test-advisor-detail.sh`, `test-advisor-do-it.sh` | Advisor coverage exists, but action-effect proof still proxy-based in places. |
| AD-010..AD-014 | Manual | — | Advisor detail dialog semantics not validated end-to-end. |
| CH-001..CH-020 | Smoke-Soft | `test-cheats-toggle.sh` | Mostly option toggles; gameplay-visible cheat effects only partially asserted. |
| D-001..D-013 | Smoke-Hard | `test-dashboard-data.sh`, `test-dashboard-panels.sh`, `test-dashboard-widgets-strict.sh` | Strict semantic coverage now consumes `/ui/dashboard/widgets` with hard widget/state assertions, source-sequence transition timing checks, explicit non-human advisor branch checks, FLOP/TURN/RIVER branch assertions for handStrength + improveOdds, and a game-over rank consistency branch. Remaining gap is exhaustive variant coverage (all game-over UI phase variants and optional widget-hidden permutations). |
| D-020..D-024 | Smoke-Hard | `test-dashboard-panels.sh`, `test-preferences-full.sh`, `test-dashboard-customization.sh` | Layout visibility is strict via `/ui/state.layout` + `/state.ui.layout`; customization flow now asserts hide/show, reorder, and restart persistence via `/ui/dashboard`. |
| S-001, S-003, S-007, S-008, S-010 | Strict-API | `test-simulator.sh` | Strong simulator API assertions for key paths. |
| S-002, S-004..S-006, S-009, S-011, S-012 | Manual | — | UI simulator workflows (cancel/progress/matrix visuals) not automated. |
| AV-001 | Smoke-Soft | `test-hand-history.sh` | Analysis entry path checked indirectly. |
| AV-002..AV-003 | Manual | — | Tournament selection/detail UI behavior not strict. |
| HH-001..HH-005 | Smoke-Soft | `test-hand-history.sh` | Basic history presence checks exist; transcript-level correctness not strict. |
| HH-010..HH-015 | Manual | — | Export workflows are not covered by strict automation. |
| HH-020..HH-022 | Manual | — | Import/corrupt-file handling not automated in scenarios. |
| HH-030..HH-034 | Manual | — | Stats-calculation assertions remain manual. |
| O-001..O-007 | Smoke-Hard | `test-options-expanded.sh`, `test-preferences-full.sh` | Round-trip option checks are good; visual behavior checks are not strict. |
| O-010..O-018 | Manual | — | Audio behavior requires runtime audio validation not covered by API. |
| O-020..O-027 | Manual | — | Chat display mode and UI formatting not strictly automated. |
| O-030..O-042 | Smoke-Hard | `test-options-expanded.sh` | Option persistence/toggle checks only; gameplay behavior mostly unverified. |
| O-050..O-053 | Smoke-Hard | `test-options-expanded.sh`, `test-clock-state.sh` | Clock option flags checked; not full behavior verification. |
| O-060..O-063 | Smoke-Hard | `test-options-expanded.sh`, `test-save-load-extended.sh` | Config checks exist; screenshot sound/visual UX still manual. |
| O-070..O-071 | Smoke-Hard | `test-options-expanded.sh`, `test-preferences-full.sh` | Persistence checks partial; restart-level proof inconsistent. |
| TD-001..TD-014 | Manual | — | Table/deck customization flows are UI-visual and not API-driven today. |
| PN-001..PN-032 | Smoke-Soft / Blocked | `test-poker-night.sh` | Script is warn-heavy; full poker-night management semantics not strictly observable. |
| HG-001..HG-005 | Strict-API | `test-hand-groups.sh` | CRUD path is strong for list/create/verify/delete. |
| HG-006..HG-010 | Manual | — | Grid interaction/keyboard navigation/stat UI behaviors are not automated. |
| SL-001, SL-005 | Strict-API | `test-save-load.sh` | Save/load acceptance plus snapshot-identity restore checks and strict `/validate` post-load assertions. |
| SL-002..SL-004, SL-006..SL-009 | Smoke-Hard | `test-save-load-extended.sh` | Mid-run save/load restore checks now strict; remaining gap is richer multi-hand continuation fidelity and metadata/history verification. |
| SP-001..SP-005 | Smoke-Soft | `test-system-info.sh` | API metadata checks are covered; support dialog UX remains manual. |
| HL-001..HL-012 | Smoke-Soft | `test-system-info.sh` | Topic existence checks only; content and context-sensitive help not strict. |
| E-001 | Strict-Real | `test-heads-up.sh` | Heads-up baseline is reasonably strong. |
| E-002 | Smoke-Hard | `test-large-field.sh` | Now requires true tournament completion (no partial-progress pass path). |
| E-003, E-004 | Strict-Real | `test-fold-every-hand.sh`, `test-gameover-ranks.sh` | Elimination/game-over paths are exercised with hard outcomes. |
| E-005..E-013 | Smoke-Hard | `test-edge-cases.sh`, `test-allin-side-pot.sh` | Edge-case assertions are now strict with explicit cheat-exception handling (`setChips`), but still rely on cheat mutation for setup. |
| E-014 | Smoke-Hard | `test-split-pot.sh` | Strict split allocation checks now include winner multiplicity, odd-chip tolerance, and payout mapping; still card-injection-driven setup. |
| E-015, E-016 | Manual | — | Timeout auto-action and full heads-up game-type matrix remain manual. |

## Classification Summary

| Classification | Count | Percentage |
|---|---|---|
| Strict-Real | 4 rows | Core gameplay anchors |
| Strict-Real (partial) | 1 row | Needs boundary checks |
| Strict-API | 4 rows | API contract validation |
| Smoke-Hard | 30 rows | Real assertions, incomplete — promote via remaining realism gaps |
| Smoke-Soft | 10 rows | Connectivity-only or observability-light paths |
| Blocked | 2 rows | Awaiting API visibility |
| Manual | 17 rows | No automation |

**Key insight:** Smoke-Hard scripts (29 rows) are the fastest to promote — most WARN→FAIL conversion is done, so remaining work is realism depth and boundary assertions. Smoke-Soft scripts (10 rows) still need stronger observability and assertion design.

## Blockers To Resolve for Strict Coverage

1. `API-MT-01`: expose full multi-table tournament visibility/invariants in control API (current remote snapshot can still collapse to single-table visibility in some runs).
2. `API-UI-02`: complete semantic dashboard widget observability adoption — endpoint exists, but strict scripts + full per-widget timing assertions still pending.

Resolved blockers (2026-02-24):

- `API-HR-01`: hand-result payload added to `/state.handResult`.
- `API-NAV-01`: durable navigation confirmation added via `/navigate/status` and `/state.navigation`.
- `API-UI-01` (expanded): `/ui/state.layout` and `/state.ui.layout` now expose dashboard item layout and GamePrefs tab layout for strict script assertions.

## Immediate Strict-Hardening Targets (Phase 1)

1. `test-save-load-extended.sh` — completed
2. `test-game-info-data.sh` — completed
3. `test-navigate.sh` — completed
4. `test-blind-posting.sh` — completed
5. `test-allin-side-pot.sh` — completed

## Release-Risk Sorted View

### P0 - Release Blockers (must be Strict before release)

| ID Bucket | Why Release-Critical | Current State | Gate Requirement |
|---|---|---|---|
| L-001, L-002 | App must launch and show valid build identity | Strict-API | Keep strict pass required |
| G-001..G-006 | Wrong setup invalidates all gameplay tests | Strict-Real | Keep strict pass required |
| G-020..G-028 | Core player actions; regressions are user-visible and severe | Strict-Real | Keep strict pass required |
| G-050..G-052 | Blind/ante/partial-blind are rule-critical | Smoke-Hard | Promote to Strict-Real |
| GI-010..GI-012 | Side-pot correctness is core poker integrity | Smoke-Hard | Promote to Strict-Real |
| RB-001..RB-003 | Rebuy accept/decline is elimination-critical | Strict-Real (partial) | Keep strict; add boundary checks |
| SL-001, SL-003..SL-005 | Save/load continuation correctness | Strict-API + Smoke-Hard | Promote full flow to Strict-Real |
| E-001, E-003, E-004, E-013, E-014 | Heads-up, elimination, all-in, split-pot correctness | Mixed strict/smoke | Promote E-013/E-014 to Strict-Real |

### P1 - Ship-Critical (strict preferred; temporary smoke allowed with tracked issue)

| ID Bucket | Why High Risk | Current State | Promotion Trigger |
|---|---|---|---|
| C-001..C-005, C-010..C-015, C-040..C-041 | Clock/level bugs materially affect tournaments | Smoke-Hard | Remove warn paths; verify real timing transitions |
| MT-001..MT-007 | Large field/multi-table is a major advertised capability | Smoke-Soft/Blocked | Add API-MT-01 visibility, then harden |
| GI-001..GI-005, GI-020..GI-026 | In-game info and game-over integrity | Smoke-Hard/Manual | Harden state + payout/profile assertions |
| RB-004..RB-012 | Rebuy/add-on economics and boundaries | Smoke-Hard | Add strict pool and limit checks |
| CH-010..CH-020 | Cheats are explicit product options in practice mode | Smoke-Soft | Verify gameplay-visible effects, not just toggles |
| O-030..O-042, O-050..O-053 | Practice pacing and pause behavior drive UX | Smoke-Hard | Promote key behavior paths to strict |

### P2 - Medium Risk (smoke acceptable for now; strict backlog)

| ID Bucket | Current State | Notes |
|---|---|---|
| PP-001..PP-009 | Smoke-Hard | Profile CRUD covered; harden default assertions |
| P-001..P-007 | Smoke-Hard | Profile list covered; harden built-in protection branches |
| P-010..P-058 | Smoke-Hard | Editor fields covered broadly; promote after P0/P1 |
| A-001..A-006, AD-001..AD-004 | Smoke-Soft / Smoke-Hard | Baseline covered; move to strict after P0/P1 |
| D-001..D-013, SP-001..SP-005, HL-001..HL-012 | Smoke-Hard / Smoke-Soft | Presence checks mostly sufficient for non-blocking release |
| HG-001..HG-005, S-001/S-003/S-007/S-008/S-010 | Strict-API | Keep as-is; not primary gameplay blockers |

### P3 - Low Risk / Manual-Heavy (not release-gating today)

| ID Bucket | Current State | Notes |
|---|---|---|
| L-003..L-006, L-020..L-023 | Manual | Window/first-run UX |
| A-010..A-017, AD-010..AD-014 | Manual | Behavioral quality and advisor detail UX |
| D-020..D-024, TD-001..TD-014 | Blocked/Manual | Requires stronger UI observability |
| HH export/import/stat detail ranges | Manual | Valuable but not immediate ship blockers |
| PN-001..PN-032 | Smoke-Soft/Blocked | Keep out of release gate until API coverage improves |

## Recommended Release Gate Execution Order

1. `test-app-launch.sh`
2. `test-game-start-params.sh`
3. `test-all-actions.sh`
4. `test-chip-conservation.sh`
5. `test-blind-posting.sh` (after strict hardening)
6. `test-allin-side-pot.sh` (after strict hardening)
7. `test-rebuy-dialog.sh`
8. `test-save-load.sh`
9. `test-save-load-extended.sh` (after strict hardening)
10. `test-heads-up.sh`
11. `test-fold-every-hand.sh`
12. `test-gameover-ranks.sh`
