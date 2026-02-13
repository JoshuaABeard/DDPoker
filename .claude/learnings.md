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
- [pokerengine] NEVER call setValue() on static Card constants (SPADES_A, etc.) in tests — they are shared singletons and modifications pollute all other tests. Create new Card instances instead (2026-02-13)
- [db] ResultSet must be explicitly closed in ResultMap to prevent resource leaks (2026-02-12)

## Configuration

- [config] PropertyConfig is a global singleton — tests that modify it can affect other tests running in the same JVM (2026-02-12)

## Git & Workflow

- [worktree] Always create worktrees from the main worktree root, not from inside another worktree (2026-02-12)
- [ci] CI runs on push to main and on PRs to main (2026-02-12)
- [hooks] Claude Code `PostToolUse` hooks cause persistent "hook error" messages on Windows — even with a no-op `exit 0` script. Avoid using PostToolUse hooks entirely (2026-02-12)
- [hooks] Claude Code `PreToolUse` hooks are unreliable on Windows — sometimes work, sometimes error. Don't use for git hooks (2026-02-12)
- [hooks] Use git native hooks via `core.hooksPath = .claude/hooks` instead of Claude Code hooks for pre-commit/post-commit — works reliably across all worktrees (2026-02-12)
- [hooks] Claude Code `SessionStart` hooks work on Windows when using PowerShell (`pwsh -NoProfile -File script.ps1`) instead of bash (2026-02-12)
- [hooks] The `find` command in bash scripts on Windows behaves differently than Unix — use PowerShell `Get-ChildItem` for Windows-compatible hooks (2026-02-12)
