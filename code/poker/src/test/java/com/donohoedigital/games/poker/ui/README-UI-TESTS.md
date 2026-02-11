# DD Poker Testing Strategy

## Overview

DD Poker uses a **two-tier testing approach**:

1. **Integration Tests** - Fast, headless tests for business logic (preferred)
2. **UI Smoke Tests** - Minimal manual verification before releases

This approach provides comprehensive coverage while keeping tests fast and CI/CD-friendly.

---

## Integration Tests (Preferred)

**Location:** `src/test/java/com/donohoedigital/games/poker/integration/`

### Benefits
✅ **Fast** - No UI initialization overhead (runs in ~4-5 seconds for 167 tests)
✅ **Reliable** - No timing/threading issues
✅ **Headless** - Runs in CI/CD without display
✅ **Focused** - Tests business logic directly
✅ **Maintainable** - Less brittle than UI tests

### Running Integration Tests

```bash
# Run all integration tests
cd code/poker
mvn test -Dtest="*IntegrationTest,FirstTimeWizardFlowTest"

# Run specific integration test class
mvn test -Dtest=PlayerProfileIntegrationTest
mvn test -Dtest=TournamentSetupIntegrationTest
mvn test -Dtest=FirstTimeWizardFlowTest
```

### Integration Test Coverage

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `FirstTimeWizardFlowTest` | 8 | Wizard state, preferences, profile creation |
| `PlayerProfileIntegrationTest` | 7 | Profile CRUD, listing, sorting |
| `TournamentSetupIntegrationTest` | 7 | Tournament configuration, payouts, blind levels |
| Other existing integration tests | 145+ | Game logic, AI, phases, etc. |

**Total: 167 integration tests**

---

## UI Smoke Tests (Manual)

**Location:** `src/test/java/com/donohoedigital/games/poker/ui/CriticalUISmokeTest.java`

### Purpose
Minimal manual verification before releases to catch critical UI regressions.

### Requirements

**IMPORTANT:** These tests require a **graphical display environment** to run.

- ✅ **Works on:** Developer workstations with displays (Windows, Mac, Linux with X11)
- ❌ **Does NOT work on:** Headless CI/CD environments

### Running UI Smoke Tests

```bash
# Run all critical smoke tests (3 tests)
cd code/poker
mvn test -Dtest=CriticalUISmokeTest

# Run specific smoke test
mvn test -Dtest=CriticalUISmokeTest#smoke_ApplicationLaunches
```

### Smoke Test Coverage

| Test | Purpose |
|------|---------|
| `smoke_ApplicationLaunches` | Verify main window appears |
| `smoke_CanNavigateToPracticeMode` | Verify navigation works |
| `smoke_CanReturnToMainMenu` | Verify back navigation works |

**Total: 3 critical smoke tests**

Screenshots are saved to `target/assertj-swing-screenshots/` for visual verification.

---

## Test Infrastructure

### Base Classes & Helpers

- **PokerUITestBase** - Base class for UI tests (JUnit 5 compatible)
- **TestProfileHelper** - Profile/preference management for test isolation
- **PokerMatchers** - Custom AssertJ Swing matchers for DD Poker components
- **@EnabledIfDisplay** - Annotation to skip tests in headless environments

### Using TestProfileHelper

```java
@BeforeEach
void setUp() throws Exception {
    // Initialize config for headless testing
    new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

    // Clean up test data
    TestProfileHelper.clearAllProfiles();
    TestProfileHelper.clearWizardPreferences();
}
```

---

## CI/CD Configuration

### Recommended: Run Integration Tests Only

```yaml
# GitHub Actions example
- name: Run Tests
  run: |
    cd code/poker
    mvn test -Dtest="*IntegrationTest,FirstTimeWizardFlowTest"
```

### Optional: Skip UI Tests from Full Suite

```yaml
# Run all tests except UI smoke tests
- name: Run Tests
  run: mvn test -Dgroups='!ui' -pl poker
```

### Advanced: Run UI Tests with Virtual Display (Linux)

```yaml
# GitHub Actions with Xvfb
- name: Run UI Tests
  run: |
    Xvfb :99 -screen 0 1024x768x24 &
    export DISPLAY=:99
    cd code/poker
    mvn test -Dtest=CriticalUISmokeTest
```

---

## Development Workflow

### For New Features

1. **Write integration tests first** - Test business logic without UI
2. **Implement the feature** - Make tests pass
3. **Run full test suite** - Verify no regressions
4. **Manual smoke test** - Run `CriticalUISmokeTest` before committing

### Before Releases

```bash
# 1. Run all integration tests
mvn test -Dtest="*IntegrationTest,FirstTimeWizardFlowTest"

# 2. Run UI smoke tests manually
mvn test -Dtest=CriticalUISmokeTest

# 3. Check screenshots in target/assertj-swing-screenshots/
```

---

## Troubleshooting

### Integration Tests

**Issue:** `ConfigManager already initialized` warnings
**Cause:** Normal - multiple tests initialize config
**Solution:** Ignore - these are warnings, not errors

**Issue:** Profile cleanup not working
**Cause:** Profile files persist between tests
**Solution:** Use `TestProfileHelper.clearAllProfiles()` in `@BeforeEach`

### UI Smoke Tests

**Issue:** Tests hang or timeout
**Cause:** No display environment available
**Solution:** Run on a machine with a display, or configure Xvfb (Linux)

**Issue:** `ComponentLookupException`
**Cause:** Component names don't match actual UI
**Solution:** Use `printComponentHierarchy()` helper to debug:

```java
@Test
void debug_ShowComponentTree() {
    robot().waitForIdle();
    printComponentHierarchy();
}
```

**Issue:** Wizard blocks tests
**Cause:** No profile exists, FirstTimeWizard appears
**Solution:** `PokerUITestBase` calls `TestProfileHelper.setupForNonWizardTests()` automatically

---

## Migration from UI Tests

This project originally planned comprehensive AssertJ Swing UI tests but pivoted to integration tests for better reliability and speed.

**Archived approach:** Full UI test suite with page objects
**Current approach:** Integration tests + minimal UI smoke tests

### Why Integration Tests?

- AssertJ Swing tests were slow (~30s+ for a few tests)
- Thread timing issues caused flakiness
- Hard to run in headless CI/CD environments
- Integration tests provide same coverage with better maintainability

### What We Kept

- ✅ `CriticalUISmokeTest` - 3 critical manual verification tests
- ✅ `PokerUITestBase` - Infrastructure for smoke tests
- ✅ `TestProfileHelper` - Useful for both UI and integration tests
- ✅ `@EnabledIfDisplay` - Automatically skips UI tests in headless mode

### What We Removed

- ❌ Full UI test suite (35+ tests) - replaced with integration tests
- ❌ Page Object classes - not needed for integration tests
- ❌ Complex UI interaction tests - business logic tested at integration level

---

## Summary

**For Day-to-Day Development:**
- Write and run integration tests (`*IntegrationTest.java`)
- Fast, reliable, runs in any environment

**Before Releases:**
- Run integration tests: `mvn test -Dtest="*IntegrationTest,FirstTimeWizardFlowTest"`
- Run smoke tests: `mvn test -Dtest=CriticalUISmokeTest`
- Verify screenshots look correct

**For CI/CD:**
- Run integration tests only (headless-compatible)
- Skip UI smoke tests or run on runners with displays
