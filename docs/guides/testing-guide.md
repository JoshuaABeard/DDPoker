# Testing Guide

## When to Write Tests First

- **New features** — Design the interface via tests
- **Bug fixes** — Reproduce bug with test, then fix
- **Refactoring** — Ensure behavior doesn't change
- **Complex logic** — Tests help you think through edge cases

## When Tests Can Wait

- **Spiking/exploring** — Throwaway code to learn (delete after)
- **Trivial code** — Simple DTOs, getters/setters
- **Proof-of-concept** — Show feasibility first, then rewrite with tests

## Test Types

- **Example tests** — Document specific behaviors with concrete inputs/outputs
- **Property tests** — Find edge cases via randomization
- **Component tests** — UI formatting/rendering logic
- **E2E tests** — Critical user flows only

## Frameworks

| Framework | Use For |
|-----------|---------|
| JUnit 5 (preferred) | Unit and integration tests. Legacy JUnit 4 tests exist but new tests should use JUnit 5. |
| Mockito | Mocking dependencies |
| AssertJ | Fluent assertions |
| AssertJ Swing | Desktop UI tests — see [ui-testing.md](ui-testing.md) for full reference |

## Running Tests

All commands run from `code/`.

| Command | When to Use |
|---------|-------------|
| `mvn test -P dev` | **During development** — Unit tests only, skips slow/integration, 4 threads |
| `mvn test` | **Before requesting review** — Full test suite |
| `mvn verify -P coverage` | **When coverage matters** — Full suite + JaCoCo aggregation |
| `mvn test -pl <module>` | **Single module** — Only run tests in one module (e.g., `-pl pokerserver`) |
| `mvn test -pl <module> -Dtest=Class` | **Single test class** — One class in one module |

Use `-P dev` for fast feedback while iterating. Use `mvn test` (no profile) for final verification before review.

## Intent Testing Standards

Tests should verify product behavior, not code mechanics. Ask: "Would this test catch a real bug that affects users?"

### Per-Layer Guidelines

| Layer | Test Should Verify | Anti-Pattern |
|-------|-------------------|--------------|
| **Engine** (pokerengine) | Poker rules (hand rankings, blind structure, payouts) | Getter/setter on model objects |
| **Server Game** (pokergameserver core) | Game outcomes (correct winner, pot amounts, state transitions) | Data structure operations |
| **Controller** (pokergameserver controllers) | API contracts (parameters passed to service, error codes, auth) | Mock returns X, assert X (tautological) |
| **Client** (poker online) | User-facing behavior (reconnection, display updates) | Internal counters, scheduler state |
| **E2E** (poker e2e) | End-to-end journeys with poker invariants | JSON field existence without value checks |

### Checklist for New Tests

- Test name describes a poker rule or user behavior, not a method name
- Assertions verify outcomes, not intermediate state
- If using mocks, `verify()` that correct parameters were passed
- Would this test fail if there were a real poker bug?

## Coverage

- Global JaCoCo threshold in the parent POM is intentionally `0.00`
- Real enforcement is module-specific baselines in each module `pom.xml`
- Run `mvn verify -P coverage` for full coverage aggregation and module checks
- Coverage reports are generated in each module's `target/site/jacoco/`
