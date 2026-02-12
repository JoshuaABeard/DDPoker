# ADR-004: SecureRandom for Production Shuffles

**Status:** Accepted
**Date:** 2026-01
**Plan:** `.claude/plans/completed/IMPROVE-RANDOMNESS.md`

## Decision
Production deck shuffles use `SecureRandom` (ThreadLocal). Simulation/calculator uses `ThreadLocalRandom`. Deterministic mode (seed>0) uses `Random(seed)`.

## Alternatives Considered
- **MersenneTwisterFast everywhere** — Rejected. Not cryptographically secure for production.
- **SecureRandom everywhere** — Rejected. Too slow for simulation fast path.

## Consequences
- Split shuffle logic: seed=0 → SecureRandom (production), seed>0 → Random (demo/test).
- Removed 30+ lines of custom NEXT_SEED()/ADJUST_SEED() code.
