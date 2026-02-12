# CLAUDE.md

**IMPORTANT: Do not modify CLAUDE.md without explicit user consent.**

## 1. Project Overview

**DD Poker** is a Texas Hold'em poker simulator with a Java Swing desktop client and online multiplayer via a Wicket web portal. Community-maintained fork of DD Poker by Doug Donohoe (2003-2017).

See `DDPOKER-OVERVIEW.md` for full tech stack, module structure, and configuration details.

### Quick Reference

```bash
# All commands run from code/
mvn test                       # Build + run all tests
mvn test -P dev                # Fast: unit tests only, skip slow/integration, 4 threads
mvn test -P fast               # Skip coverage, integration, javadoc
mvn verify -P coverage         # Full coverage aggregation
mvn clean package -DskipTests  # Build only

# Playwright E2E (from code/pokerwicket/)
npm test                       # Run all E2E tests (server must be running)
```

### Key Entry Points

- Desktop: `com.donohoedigital.games.poker.PokerMain`
- Server: `com.donohoedigital.games.poker.server.PokerServerMain`
- Web: `com.donohoedigital.poker.web.PokerJetty`

## 2. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### When Principles Conflict

Priorities (highest to lowest):
1. **Privacy** — Never commit private data (see `SECURITY.md`)
2. **Correctness** — Code must work and be tested
3. **Simplicity** — Prefer simple over complex
4. **Surgical** — Touch only what's needed
5. **Speed** — Fast delivery, but not at expense of above

**If genuinely stuck:** Stop and ask. Don't guess.

### When Things Break

- Build fails: Read the error. Fix the root cause, don't suppress warnings or skip checks.
- Tests fail: Investigate before re-running. If flaky, note it — don't ignore it.
- Blocked by environment/dependency: State what's broken and what you tried. Don't work around it silently.

## 3. Simplicity & Surgical Changes

**Minimum code that solves the problem. Touch only what you must.**

- No features beyond what was asked. No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

**Note:** Comprehensive testing is NOT over-engineering. Write minimal production code, but test it thoroughly.

## 4. Testing

**Write tests BEFORE implementation whenever possible.**

After implementing, verify:
- All new tests pass
- All existing tests still pass
- Code coverage meets thresholds (see testing guide)
- Build completes with zero warnings

See `.claude/guides/testing-guide.md` for test-first practices, test types, and when tests can wait.

## 5. Plans

**Plans live in `.claude/plans/`. Always create a plan for features, significant refactoring, or anything spanning > 3 files or > 200 lines.**

Skip plans for trivial fixes, docs-only changes, config tweaks, dependency updates. If unsure, propose one — user can say "just do it."

See `.claude/guides/plan-protocol.md` for plan lifecycle and management.

## 6. Git Workflow

**NEVER work directly on main. All development happens in worktrees.**

Use a worktree for anything touching code or tests. Main is OK for small (< 10 lines) non-code changes: `.claude/` files, plans, .gitignore, README typos. If unsure, use a worktree.

```bash
# From the main worktree root
git worktree add -b feature-<description> ../DDPoker-feature-<description>
```

**Naming:** `feature-*`, `fix-*`, `refactor-*`

See `.claude/guides/worktree-workflow.md` for the full create/merge/cleanup workflow.

### Commit Message Format

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

```
feat: Add player hand evaluation logic
Plan: .claude/plans/hand-evaluation.md

Chose lookup-table approach over brute-force for performance.

Co-Authored-By: Claude Opus 4.6
```

- First line: `<type>: <summary>` (50 chars max)
- Second line: `Plan: <path>` (if a plan was used, omit otherwise)
- Body: Context, reasoning, or tradeoffs (omit if self-explanatory)
- Last line: `Co-Authored-By: <model name>` (e.g., "Claude Sonnet 4.5" or "Claude Opus 4.6")

## 7. Code Reviews

**Reviews fully automated: dev agent spawns review agent (Opus).**

When work is complete, create a review handoff at `.claude/reviews/BRANCH-NAME.md` (use `.claude/reviews/TEMPLATE.md`) and spawn a review subagent.

See `.claude/guides/review-protocol.md` for the full review process.

## 8. Privacy & Security

**ALWAYS review files for private information before committing to the public repository.**

See `SECURITY.md` for the full checklist and commit workflow.
