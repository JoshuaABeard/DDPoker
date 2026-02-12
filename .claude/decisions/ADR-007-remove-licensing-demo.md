# ADR-007: Delete Licensing and Demo Mode Code

**Status:** Accepted
**Date:** 2026-01
**Plan:** `.claude/plans/completed/REMOVE-LICENSING-PLAN.md`, `.claude/plans/completed/REMOVE-DEMO-MODE.md`

## Decision
Deleted 2,000+ lines of commercial licensing and demo mode code entirely. Player identification uses auto-generated UUID v4. No feature flags — dead code is deleted, not disabled.

## Alternatives Considered
- **Feature flags to disable** — Rejected. Dead code should be removed.
- **Keep demo mode for testing** — Rejected. Renamed remaining seeded randomness to "seeded mode" for clarity.

## Consequences
- Player identification via UUID in `player.json` (platform-specific config directory).
- Admin creation via `ADMIN_USERNAME`/`ADMIN_PASSWORD` environment variables.
- Wire protocol compatibility broken (acceptable for open-source transition).
- ~40 files modified across 6 modules (bottom-up dependency chain).
