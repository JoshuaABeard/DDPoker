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
- **Property tests** (FSCheck) - Find edge cases via randomization
- **Example tests** (Theory + InlineData) - Document specific behaviors
- **Component tests** (bUnit) - UI formatting/rendering logic
- **Integration tests** - Real MCP servers, Azure OpenAI
- **E2E tests** (Playwright) - Critical user flows only

### Verification
After implementing:
- ✅ All new tests pass
- ✅ All existing tests still pass
- ✅ Code coverage meets thresholds (65% minimum)
- ✅ `dotnet build` completes with zero warnings

See [TESTING.md](TESTING.md) for comprehensive TDD best practices.