# CLAUDE.md

**IMPORTANT: Do not modify EXAMPLES.md or CLAUDE.md without explicit user consent.**

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 5. Test-Driven Development

**Write tests BEFORE implementation whenever possible.**

### The Red-Green-Refactor Cycle
```
RED:    Write a failing test for the new behavior
  ↓
GREEN:  Write minimal code to make it pass
  ↓
REFACTOR: Clean up while keeping tests green
  ↓
Repeat for next behavior
```

### TDD Workflow for New Features
1. **Understand the requirement** - What should this do? What are edge cases?
2. **Write the test** - It will fail (no implementation yet)
3. **Implement minimally** - Just enough to pass the test
4. **Refactor if needed** - Simplify while tests stay green
5. **Repeat** - Next requirement, back to step 1

### When to Write Tests First
- ✅ **New features** - Design the interface via tests
- ✅ **Bug fixes** - Reproduce bug with test, then fix
- ✅ **Refactoring** - Ensure behavior doesn't change
- ✅ **Complex logic** - Tests help you think through edge cases

### When Tests Can Wait
- ❌ **Spiking/exploring** - Throwaway code to learn (delete after)
- ❌ **Trivial code** - Simple DTOs, getters/setters
- ❌ **Proof-of-concept** - Show feasibility first, then rewrite with tests

### Test Types to Consider
- **Property tests** - Find edge cases via randomization
- **Example tests** - Document specific behaviors
- **Component tests** - UI formatting/rendering logic
- **E2E tests** - Critical user flows only

### Verification
After implementing:
- ✅ All new tests pass
- ✅ All existing tests still pass
- ✅ Code coverage meets thresholds (65% minimum)
- ✅ build completes with zero warnings

## 6. Plan Backlog Process

**Plans live in `.claude/plans/`. Update them as you go.**

- Plans are stored in `.claude/plans/` as Markdown files.
- When working a plan, update it as progress is made — check off completed steps, note decisions, and record any deviations from the original approach.
- Upon completion, document a summary of changes made in the plan.
- After user approval, move the plan to `.claude/plans/completed/` right before the final commit of the feature.

## 7. Git Worktree Workflow

**NEVER work directly on main. All development happens in worktrees.**

### Creating Worktrees
```bash
# From main worktree: C:\Repos\DDPoker
git worktree add -b feature-name ../DDPoker-feature-<description>
```

**Naming:** `DDPoker-feature-*`, `DDPoker-fix-*`, `DDPoker-refactor-*`

### Workflow
1. Create worktree and work there
2. Commit and test normally
3. When complete, STOP and request code review (no PRs)
4. After approval: merge to main, push, remove worktree

### Rules
- ❌ **Never work directly on main** - Main only receives merges
- ✅ **One worktree per feature** - Sibling directories
- ✅ **Clean up after merge** - `git worktree remove` + `git branch -d`
- ✅ **Plans/backlog** - Can be managed in main

## 8. Private Information Check Before Committing

**ALWAYS review files for private information before committing to the public repository.**

### What to Check For:
Before committing any file, scan for:
- ❌ **Private IP addresses** (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
- ❌ **Specific domain names** (personal domains, company domains)
- ❌ **Server hostnames** (actual server names)
- ❌ **Credentials** (passwords, API keys, tokens, SSH keys)
- ❌ **Email addresses** (personal emails)
- ❌ **File paths with usernames** (C:\Users\John\...)
- ❌ **Database connection strings** (with real hosts/passwords)
- ❌ **Network details** (MAC addresses, specific network configs)

### Safe Alternatives:
Use placeholders and generic examples:
- ✅ `YOUR_IP_HERE`, `YOUR_DOMAIN`, `example.com`
- ✅ Container names: `DDPoker`, `swag`, `database`
- ✅ Environment variables: `${DATABASE_URL}`, `${API_KEY}`
- ✅ Generic paths: `/data`, `/config`, relative paths
- ✅ Localhost references: `localhost`, `127.0.0.1`
- ✅ Documentation IPs: `192.0.2.x` (RFC 5737), `example.com`

### Workflow:
When asked to commit files:
1. **List files** being committed
2. **Review each file** for private information
3. **Present findings** to user clearly:
   - ✅ "File X is SAFE - no private info"
   - ❌ "File Y contains: IP 192.168.1.50 on line 23"
4. **Wait for approval** if any issues found
5. **Proceed with commit** only after user confirms

### Common File Types to Review Carefully:
- Configuration files (`.conf`, `.env`, `.yaml`, `.json`)
- Scripts (`.sh`, `.ps1`, `.bat`)
- Docker files (`Dockerfile`, `docker-compose.yml`)
- Templates (Unraid `.xml`, Kubernetes manifests)
- Documentation with examples (`README.md`, setup guides)

### Example Review:
```
## Privacy Check for: swag/ddpoker.conf

✅ SAFE - Uses container name `DDPoker` instead of IP
✅ SAFE - Uses wildcard domain `ddpoker.*`
✅ SAFE - No credentials found
✅ SAFE - Standard port 8080 (generic)

Ready to commit.
```