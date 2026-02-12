# CLAUDE.md

**IMPORTANT: Do not modify EXAMPLES.md or CLAUDE.md without explicit user consent.**

## 1. Project Overview

**DD Poker** is a Texas Hold'em poker simulator with a Java Swing desktop client and online multiplayer via a Wicket web portal. Community-maintained fork of DD Poker by Doug Donohoe (2003-2017).

### Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 25 |
| Build | Maven 3.9.12 |
| Web | Apache Wicket 10.8, Jetty 12.1 |
| Database | H2 (embedded), Hibernate 6.6 |
| DI | Spring 6.2 |
| Tests | JUnit 4/5, Mockito, AssertJ |
| UI Tests | AssertJ Swing, Playwright |
| Coverage | JaCoCo 0.8.13 (65% minimum) |

### Build & Test Commands

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

### Project Structure

```
code/                    # 21 Maven modules (parent POM here)
  pokerengine/           # Core game logic (hand evaluation, rules)
  poker/                 # Desktop client (Swing UI, main app)
  pokerserver/           # Game API server
  pokernetwork/          # Networking layer
  pokerwicket/           # Web UI (Wicket) + Playwright tests
  common/                # Shared utilities
  db/                    # Database layer
  gameengine/            # Generic game engine
  gui/                   # GUI utilities
  server/                # Server base
  udp/                   # UDP chat server
docker/                  # Docker deployment
tools/scripts/           # PowerShell dev scripts
.claude/plans/           # Task plans
```

### Key Entry Points

- Desktop: `com.donohoedigital.games.poker.PokerMain`
- Server: `com.donohoedigital.games.poker.server.PokerServerMain`
- Web: `com.donohoedigital.poker.web.PokerJetty`

## 2. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### When Principles Conflict

Priorities (highest to lowest):
1. **Privacy (Section 10)** - Never commit private data
2. **Correctness** - Code must work and be tested
3. **Simplicity (Section 3)** - Prefer simple over complex
4. **Surgical (Section 4)** - Touch only what's needed
5. **Speed** - Fast delivery matters, but not at expense of above

**If genuinely stuck:** Stop and ask. Don't guess.

## 3. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

**Note:** Comprehensive testing (Section 6) is NOT over-engineering. Write minimal production code, but test it thoroughly.

## 4. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

## 5. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals with explicit checks:
- "Add feature X" → Define expected behavior, write tests, implement, verify tests pass
- "Fix bug Y" → Reproduce with a test, fix, verify test passes
- "Refactor Z" → Verify all tests pass before AND after

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 6. Testing

**Write tests BEFORE implementation whenever possible. Thorough testing is NOT over-engineering.**

### When to Write Tests First
- **New features** - Design the interface via tests
- **Bug fixes** - Reproduce bug with test, then fix
- **Refactoring** - Ensure behavior doesn't change
- **Complex logic** - Tests help you think through edge cases

### When Tests Can Wait
- **Spiking/exploring** - Throwaway code to learn (delete after)
- **Trivial code** - Simple DTOs, getters/setters
- **Proof-of-concept** - Show feasibility first, then rewrite with tests

### Test Types to Consider
- **Property tests** - Find edge cases via randomization
- **Example tests** - Document specific behaviors
- **Component tests** - UI formatting/rendering logic
- **E2E tests** - Critical user flows only (Playwright for web UI)

### Verification
After implementing:
- All new tests pass
- All existing tests still pass
- Code coverage meets thresholds (65% minimum)
- Build completes with zero warnings

## 7. Plan Backlog Process

**Plans live in `.claude/plans/`. Update them as you go.**

### When to Create a Plan

**Always create a plan for:**
- New features (multi-file changes)
- Significant refactoring
- Complex bug fixes requiring investigation
- Anything spanning > 3 files or > 200 lines

**Skip plans for:**
- Trivial bug fixes (one-liners)
- Documentation-only changes
- Configuration tweaks
- Dependency updates only

**If unsure:** Propose a plan. User can say "just do it" if too simple.

### Plan Management

- Plans are stored in `.claude/plans/` as Markdown files.
- When working a plan, update it as progress is made — check off completed steps, note decisions, and record any deviations from the original approach.
- Upon completion, document a summary of changes made in the plan.
- After user approval, move the plan to `.claude/plans/completed/` right before the final commit of the feature.

## 8. Git Worktree Workflow

**NEVER work directly on main. All development happens in worktrees.**

### Creating Worktrees
```bash
# From the main worktree root
git worktree add -b feature-name ../DDPoker-feature-<description>
```

**Naming:** `DDPoker-feature-*`, `DDPoker-fix-*`, `DDPoker-refactor-*`

### Workflow
1. Pull main from remote
2. Create worktree from main and work there
3. Commit and test normally
4. When complete, STOP and request code review (no PRs)
5. After approval: squash merge to main, push, clean up
   - `git checkout main && git merge --squash <branch> && git commit`
   - `git worktree remove <path>` + `git branch -d <branch>`

### What Goes Where

**Use a worktree** for anything touching code or tests.

**Main is OK** for small (< 10 lines) non-code changes: `.claude/` files, plans, .gitignore, README typos.

**If unsure, use a worktree.**

### Commit Message Format

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

```
feat: Add player hand evaluation logic
Plan: .claude/plans/hand-evaluation.md

Chose lookup-table approach over brute-force for performance.

Co-Authored-By: <Claude Opus 4.6>
```

- First line: `<type>: <summary>` (50 chars max)
- Second line: `Plan: <path>` (if a plan was used, omit otherwise)
- Body: Context, reasoning, or tradeoffs (omit if self-explanatory)
- Last line: `Co-Authored-By: <model>` (use actual model name)

## 9. Code Review Process

**Reviews fully automated: dev agent spawns review agent (Opus).**

### Developer Agent: Request Review

When work is complete:

1. **Create review handoff:** `.claude/reviews/BRANCH-NAME.md`
   - Summary: 2-3 sentences of what changed and why
   - Plan: Path to plan file (if applicable, e.g., `.claude/plans/feature.md`)
   - Files changed: List with privacy check status
   - Verification: Test results, coverage %, build status
   - Context: Important decisions or tradeoffs
   - Worktree path: Absolute path to the worktree

2. **Spawn review agent:**
   - Use Task tool with `subagent_type: "general-purpose"`, `model: "opus"`
   - Task prompt **must** include: "Read `.claude/CLAUDE.md` Section 9 for the review checklist, then read the handoff file at `.claude/reviews/BRANCH-NAME.md` and perform the review."

3. **Present results:** Show user review findings from updated handoff file

### Review Agent: Perform Review

When spawned:

1. **Read** `.claude/CLAUDE.md` Section 9 and the handoff file
2. **Read the plan** (if referenced in the handoff) to understand intended scope and approach
3. **Navigate to worktree** using the path from the handoff file
4. **Run verification:** Execute tests (`mvn test`), check coverage, run build
5. **Verify against CLAUDE.md:**
   - Tests pass, coverage >= 65%, build clean (zero warnings)
   - No scope creep (Section 4)
   - No over-engineering (Section 3)
   - No private info (Section 10)
   - No security vulnerabilities
   - Implementation matches plan: correct approach, all steps completed, deviations documented
6. **Update handoff file** with findings:
   - Status: APPROVED | NOTES | CHANGES REQUIRED
   - Findings: Specific issues with file:line references
   - Blockers: Required changes (if any)
7. **Return to dev agent:** Summary of review status

## 10. Private Information Check Before Committing

**ALWAYS review files for private information before committing to the public repository.**

### What to Check For
Before committing any file, scan for:
- **Private IP addresses** (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
- **Specific domain names** (personal domains, company domains)
- **Server hostnames** (actual server names)
- **Credentials** (passwords, API keys, tokens, SSH keys)
- **Email addresses** (personal emails)
- **File paths with usernames** (C:\Users\John\...)
- **Database connection strings** (with real hosts/passwords)
- **Network details** (MAC addresses, specific network configs)

Replace any found with placeholders (`YOUR_IP_HERE`, `example.com`, `${API_KEY}`, etc.) or environment variables.

### Workflow
When asked to commit files:
1. **List files** being committed
2. **Review each file** for private information
3. **Present findings** to user clearly:
   - "File X is SAFE - no private info"
   - "File Y contains: IP 192.168.1.50 on line 23"
4. **Wait for approval** if any issues found
5. **Proceed with commit** only after user confirms
