# Agent Learnings

Persistent knowledge discovered during development sessions. Read this at the start of non-trivial tasks to avoid rediscovering known issues.

**Format:** `- [module/area] Finding (discovered YYYY-MM-DD)`

**Rules:**
- Add entries when you discover something non-obvious that cost you time
- Remove entries when they become obsolete (e.g., bug was fixed, dependency changed)
- Keep entries concise — one line if possible
- Don't duplicate what's already in guides or DDPOKER-OVERVIEW.md

---

## Build & Maven

- [build] `mvn test -P dev` is the fast path — skips slow/integration tests, runs 4 threads (2026-02-12)
- [build] CI uses `-P dev` profile, not the full test suite (2026-02-12)
- [coverage] Coverage threshold is 65% enforced by JaCoCo; use `mvn verify -P coverage` to check (2026-02-12)
- [format] Spotless auto-formats Java code on compile — don't manually format, just run `mvn compile` (2026-02-12)

## Testing

- [pokerengine] AIStrategyNode tests depend on PropertyConfig state; tests must be resilient to initialization order (2026-02-12)
- [db] ResultSet must be explicitly closed in ResultMap to prevent resource leaks (2026-02-12)

## Configuration

- [config] PropertyConfig is a global singleton — tests that modify it can affect other tests running in the same JVM (2026-02-12)

## Git & Workflow

- [worktree] Always create worktrees from the main worktree root, not from inside another worktree (2026-02-12)
- [ci] CI runs on push to main and on PRs to main (2026-02-12)
- [hooks] Post-commit hook reminds to update learnings.md after every commit (2026-02-12)
