# ADR-010: Spotless for Code Formatting

**Status:** Accepted
**Date:** 2026-02-12

## Decision
Use Spotless Maven plugin for automatic Java code formatting on compile. Auto-applies formatting during `process-sources` phase.

## Alternatives Considered
- **Checkstyle** — Only validates, doesn't auto-fix.
- **Google Java Format** — Too opinionated, would require massive reformatting.
- **Manual formatting** — Inconsistent across agent sessions.

## Consequences
- 115 existing files need formatting — will be fixed gradually as files are touched.
- Formatting is deterministic — no style debates between agent sessions.
- Eclipse JDT formatter (4.21) matches existing codebase style.
- POM formatting disabled (too noisy for existing files).
- Run `mvn spotless:apply` to format all files at once, or let it happen incrementally.
