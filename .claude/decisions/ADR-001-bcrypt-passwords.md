# ADR-001: Bcrypt for Password Hashing

**Status:** Accepted
**Date:** 2026-01
**Plan:** `.claude/plans/completed/BCRYPT-PASSWORD-HASHING.md`

## Decision
Replaced reversible DES encryption with bcrypt one-way hashing for all password storage.

## Alternatives Considered
- **Keep DES with migration path** — Rejected. No legacy users to migrate (new community fork).
- **Argon2** — Rejected. Bcrypt is sufficient and has no native dependency.

## Consequences
- "Forgot password" is now a reset flow (generates new password), not recovery.
- Admin passwords persisted to disk file since they can't be decrypted from DB.
- Wire protocol compatibility broken (acceptable for open-source transition).
