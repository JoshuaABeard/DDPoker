# ADR-002: File-Based JSON Configuration

**Status:** Accepted
**Date:** 2026-01
**Plan:** `.claude/plans/completed/FILE-BASED-CONFIG.md`

## Decision
Replaced Java Preferences API (Windows Registry / hidden Linux files) with file-based JSON configuration using Jackson.

## Alternatives Considered
- **YAML** — No comments support, but Jackson handles JSON natively.
- **Properties files** — Flat structure, can't represent nested config.
- **TOML** — Requires additional library.
- **XML** — Too verbose.

## Consequences
- Platform-specific paths: `%APPDATA%\ddpoker\` (Windows), `~/Library/Application Support/ddpoker/` (macOS), `~/.ddpoker/` (Linux).
- Adapter pattern (`FilePrefsAdapter`) avoids changing 50+ `DDOption` classes.
- No migration from old Preferences — clean start for community fork.
