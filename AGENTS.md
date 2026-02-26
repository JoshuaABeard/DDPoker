# AGENTS.md

**IMPORTANT: Do not modify AGENTS.md without explicit user consent.**

## 1. Project Overview

**DD Poker** is a Texas Hold'em poker simulator with a Java Swing desktop client and online multiplayer server. Community-maintained fork of DD Poker by Doug Donohoe (2003-2017).

See `.claude/DDPOKER-OVERVIEW.md` for full tech stack, module structure, and configuration details. Check `docs/memory.md` for known gotchas before starting non-trivial work.

### Quick Reference

```bash
# All commands run from code/
mvn test                       # Build + run all tests
mvn test -P dev                # Fast: unit tests only, skip slow/integration, 4 threads
mvn test -P fast               # Skip coverage, integration, javadoc
mvn test -pl <module> -Dtest=Class  # Single test class in one module
mvn verify -P coverage         # Full coverage aggregation
mvn clean package -DskipTests  # Build only (no dev control server)
mvn clean package -DskipTests -P dev  # Build with dev GameControlServer included in fat JAR
```

### Key Entry Points

- Desktop: `com.donohoedigital.games.poker.PokerMain`
- Server: `com.donohoedigital.games.poker.server.PokerServerMain`
- REST API: `com.donohoedigital.poker.api.ApiApplication`

### Dev Control Server (automated testing via HTTP API)

The `-P dev` Maven profile adds `src/dev/java` to the build, which includes `GameControlServer` — a lightweight HTTP API for driving the game without UI interaction.

**Build and run:**
```bash
# From code/
mvn clean package -DskipTests -P dev   # Build fat JAR with GameControlServer
java -jar poker/target/DDPokerCE-3.3.0.jar > /tmp/game.log 2>&1 &
```

**Control server endpoints** (auth via `X-Control-Key` header from `~/.ddpoker/control-server.key`):
```bash
PORT=$(cat ~/.ddpoker/control-server.port)
KEY=$(cat ~/.ddpoker/control-server.key)
H="X-Control-Key: $KEY"

curl -s -H "$H" http://localhost:$PORT/health                          # {"status":"ok"}
curl -s -H "$H" http://localhost:$PORT/state                           # full game state JSON
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"numPlayers":3}' http://localhost:$PORT/game/start           # start practice game
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"type":"CALL"}' http://localhost:$PORT/action                # submit player action
```

**Polling for human turn:**
```bash
# Wait until isHumanTurn is true, then act
until curl -s -H "$H" http://localhost:$PORT/state | grep -q '"isHumanTurn":true'; do sleep 1; done
curl -s -H "$H" -X POST -H "Content-Type: application/json" -d '{"type":"CALL"}' http://localhost:$PORT/action
```

**Key game state fields:**
- `inputMode`: `NONE` | `QUITSAVE` (AI turn) | `CHECK_BET` | `CHECK_RAISE` | `CALL_RAISE` (human turn) | `DEAL` | `CONTINUE`
- `currentAction.isHumanTurn`: `true` when human action is needed
- `currentAction.availableActions`: list of valid action types for the current mode

See `.claude/guides/desktop-client-testing.md` for the full reference: all endpoints, input mode table, polling patterns, enabling debug logging, and common failure modes.

## 2. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### When Principles Conflict

Priorities (highest to lowest):
1. **Privacy** — Never commit private data (see `.claude/SECURITY.md`)
2. **Correctness** — Code must work and be tested
3. **Completeness** — Finish what you start, no TODOs or stubs
4. **Simplicity** — Prefer simple over complex
5. **Surgical** — Touch only what's needed
6. **Speed** — Fast delivery, but not at expense of above

**If genuinely stuck:** Stop and ask. Don't guess.

**When things break:** Use `superpowers:systematic-debugging`. Never suppress warnings, skip checks, or work around failures silently. If you can't complete the work, stop and explain why.

## 3. Simplicity & Surgical Changes

**Simple, complete solutions. Touch only what you must.**

### Complete, Not Minimal

**"Simplicity" means simple-and-complete, not incomplete.**

- Implement requested features fully. No TODOs. No stubs. No "// implement later" comments.
- If you can't complete something, STOP and ask. Never stub it out.
- "Minimum code" means no unnecessary abstraction — NOT partial implementation.
- Simple-but-complete beats clever-but-partial every time.

**If tempted to leave a TODO:** Stop. Either implement it now, or ask the user whether to defer it.

### No Over-Engineering

**But don't add features, complexity, or "flexibility" that wasn't requested.**

- No features beyond what was asked. No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

### Surgical Precision

**Change only what's needed to solve the problem.**

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently. Spotless auto-formats Java code on compile — don't manually format.
- If you notice unrelated dead code, mention it — don't delete it.

**When your changes create orphans:**
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

**Note:** Comprehensive testing is NOT over-engineering. Write minimal production code, but test it thoroughly.

## 4. Testing

See `.claude/guides/testing-guide.md` for project-specific frameworks, commands, and coverage thresholds.

## 5. Decisions

Capture non-obvious technical decisions in the relevant plan under `docs/plans/` (and keep them when moved to `docs/plans/completed/`).

If a decision creates a long-lived implementation gotcha, add a concise entry to `docs/memory.md`.

## 6. Plans

Plans live in `docs/plans/`. Use `superpowers:brainstorming` for design exploration and `superpowers:writing-plans` for implementation plans.

Completed plans are moved to `docs/plans/completed/` and marked with `**Status:** COMPLETED (<date>)`. Keep completed plans for historical reference — don't delete them.

## 7. Git Workflow

Handled by superpowers: `using-git-worktrees`, `finishing-a-development-branch`.

## 8. Code Reviews

Handled by superpowers: `requesting-code-review`, `receiving-code-review`.

## 9. Privacy & Security

**ALWAYS review files for private information before committing to the public repository.**

See `.claude/SECURITY.md` for the full checklist and commit workflow.

## 10. Copyright & Licensing

**DD Poker is a dual-copyright project: original work by Doug Donohoe (2003-2026) and community contributions (2026 onwards).**

When creating or modifying files:
- **New files you create**: Use community copyright (Template 3)
- **Substantially modified files**: Use dual copyright (Template 2)
- **Minor changes/bug fixes**: Keep original copyright (Template 1)

See `.claude/guides/copyright-licensing-guide.md` for:
- Complete copyright header templates
- File-by-file decision guide
- GPL-3.0 compliance requirements
- Attribution best practices

**Key principle:** Doug Donohoe retains copyright on original files (2003-2026). Community gets copyright on new/substantially modified files (2026 onwards). All code licensed GPL-3.0. Trademarks/logos remain CC BY-NC-ND 4.0.
