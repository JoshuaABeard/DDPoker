# ADR-008: Module-Specific Coverage Baselines

**Status:** Accepted
**Date:** 2026-01
**Plan:** `.claude/plans/completed/COVERAGE-ENFORCEMENT-PROJECT-WIDE.md`

## Decision
Disabled global 65% threshold. Each module has its own JaCoCo baseline (0% to 50%) based on current state. AI package enforces 50% minimum.

## Alternatives Considered
- **Single global threshold** — Rejected. Unrealistic for UI/legacy modules with 0% coverage.
- **No enforcement** — Rejected. Coverage would regress without baselines.

## Consequences
- New code must meet the module's baseline or raise it.
- Baselines are ratcheted up as coverage improves, never lowered.
- CI uses `dev` profile (skips coverage); `verify -P coverage` for full enforcement.
