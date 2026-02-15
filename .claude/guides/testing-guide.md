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
| AssertJ Swing | Desktop UI tests |

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

## Coverage

- Minimum threshold: **65%** (enforced by JaCoCo)
- Run `mvn verify -P coverage` for full coverage aggregation
- Coverage reports in each module's `target/site/jacoco/`
