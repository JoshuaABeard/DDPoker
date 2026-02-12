# ADR-003: Maven Build Profiles

**Status:** Accepted
**Date:** 2026-01
**Plan:** `.claude/plans/completed/BUILD-OPTIMIZATION.md`

## Decision
Created three Maven profiles: `dev` (fast unit tests, 4 threads), `fast` (skip coverage + integration), `coverage` (full JaCoCo reports in verify phase).

## Alternatives Considered
- **Parallel test execution** — Rejected due to shared singleton state (ConfigManager, PropertyConfig).
- **Gradle migration** — Too complex, not worth the effort.
- **Maven Daemon** — Too complex for incremental gain.

## Consequences
- CI uses `dev` profile for speed; `coverage` profile for full reports.
- JaCoCo moved from `test` to `verify` phase (5-10% faster builds).
- Parallel tests remain blocked until singleton state is addressed.
