# DD Poker Testing Guide

## Overview

DD Poker Community Edition uses modern testing practices with JUnit 5 (Jupiter), AssertJ for fluent assertions, and Spring Test for integration testing. All tests follow consistent naming conventions and patterns to ensure maintainability and clarity.

## Test Framework Stack

### Core Dependencies

- **JUnit 5 (Jupiter) 5.11.0** - Modern testing framework with improved parameterized testing and lifecycle management
- **AssertJ 3.27.0** - Fluent assertion library for readable test code
- **Spring Test** - Integration testing with Spring context
- **Mockito 5.11.0** - Mocking framework for unit tests (available but prefer real objects when practical)
- **H2 Database** - In-memory database for integration tests (MySQL compatibility mode)

### Maven Configuration

All test-enabled modules have JUnit 5 and AssertJ dependencies:

```xml
<!-- JUnit 5 (Jupiter) for modern tests -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>

<!-- AssertJ for fluent assertions -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.27.0</version>
    <scope>test</scope>
</dependency>
```

## Test Naming Conventions

### Pattern: `should_ExpectedBehavior_When_Condition`

All test methods follow the BDD-style naming convention to clearly document what is being tested:

```java
@Test
void should_ReturnNull_When_KeyNotFound() {
    // Test implementation
}

@Test
void should_CreateUUID_When_FirstRunDetected() {
    // Test implementation
}

@Test
void should_ThrowException_When_ConfigCorrupted() {
    // Test implementation
}

@Test
void should_PersistSettings_When_FlushCalled() {
    // Test implementation
}
```

### Benefits

- **Self-documenting** - Test name explains intent without reading implementation
- **Easy scanning** - Quickly understand test coverage from test names
- **Clear failures** - Test failures immediately show what behavior broke
- **Consistent** - Same pattern across entire codebase

## Writing Tests

### Basic Test Structure

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UserServiceTest {

    @Test
    void should_CreateUser_When_ValidDataProvided() {
        // Arrange
        UserService service = new UserService();
        String username = "testuser";

        // Act
        User result = service.createUser(username);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
    }
}
```

### AssertJ Assertions

Use AssertJ's fluent assertion API for all assertions:

```java
// Basic assertions
assertThat(actual).isEqualTo(expected);
assertThat(condition).isTrue();
assertThat(condition).isFalse();
assertThat(value).isNull();
assertThat(value).isNotNull();

// String assertions
assertThat(text).isEmpty();
assertThat(text).contains("substring");
assertThat(text).startsWith("prefix");
assertThat(text).matches("regex.*");

// Collection assertions
assertThat(list).hasSize(5);
assertThat(list).isEmpty();
assertThat(list).contains("item1", "item2");
assertThat(list).containsExactly("item1", "item2");

// Exception assertions
assertThatThrownBy(() -> methodThatThrows())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("Expected error message");

// Numeric assertions
assertThat(value).isGreaterThan(5);
assertThat(value).isLessThan(10);
assertThat(value).isBetween(5, 10);
```

### Test Lifecycle

```java
import org.junit.jupiter.api.*;

class LifecycleTest {

    @BeforeAll
    static void setupAll() {
        // Runs once before all tests in this class
        // Use for expensive setup (database initialization, etc.)
    }

    @BeforeEach
    void setup() {
        // Runs before each test method
        // Use for test-specific setup
    }

    @Test
    void should_DoSomething_When_ConditionMet() {
        // Test code
    }

    @AfterEach
    void tearDown() {
        // Runs after each test method
        // Use for cleanup
    }

    @AfterAll
    static void tearDownAll() {
        // Runs once after all tests in this class
        // Use for expensive cleanup
    }
}
```

### Parameterized Tests

Use `@ParameterizedTest` for testing multiple inputs:

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class ParameterizedTests {

    @ParameterizedTest
    @ValueSource(strings = {"value1", "value2", "value3"})
    void should_HandleMultipleInputs_When_Parameterized(String input) {
        assertThat(process(input)).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({
        "input1, expected1",
        "input2, expected2",
        "input3, expected3"
    })
    void should_MapInputToOutput_When_GivenMultipleCases(
        String input, String expected) {
        assertThat(process(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void should_HandleComplexInputs_When_UsingMethodSource(
        TestData data) {
        assertThat(process(data)).matches(data.getExpectedPattern());
    }

    static Stream<TestData> provideTestData() {
        return Stream.of(
            new TestData("case1", "pattern1"),
            new TestData("case2", "pattern2")
        );
    }
}
```

### Temporary Files and Directories

Use JUnit 5's `@TempDir` for file system tests:

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class FileTest {

    @Test
    void should_CreateFile_When_DirectoryExists(@TempDir Path tempDir) {
        Path testFile = tempDir.resolve("test.txt");

        // Test file operations
        Files.writeString(testFile, "content");

        assertThat(testFile).exists();
        assertThat(Files.readString(testFile)).isEqualTo("content");

        // No cleanup needed - @TempDir handles it automatically
    }
}
```

## Spring Integration Tests

### Basic Spring Test

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

@SpringJUnitConfig(locations = {"/app-context-test.xml"})
@Transactional
class ServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    @Rollback
    void should_PersistUser_When_SaveCalled() {
        User user = new User("testuser");
        userService.save(user);

        assertThat(user.getId()).isNotNull();

        User fetched = userService.findById(user.getId());
        assertThat(fetched.getUsername()).isEqualTo("testuser");
    }
}
```

### Database Testing

All database integration tests use H2 in-memory database with MySQL compatibility mode:

```java
@SpringJUnitConfig(locations = {"/app-context-test.xml"})
@Transactional
class DatabaseTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @Rollback  // Automatically rolls back after test
    void should_StoreAndRetrieveData_When_DatabaseOperationsExecuted() {
        // Test database operations
        // No cleanup needed - @Rollback handles it
    }
}
```

**Database Configuration:**
- Tests use H2 in MySQL compatibility mode
- Connection URL: `jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1`
- Automatic schema creation via Hibernate: `hibernate.hbm2ddl.auto=create-drop`
- No external database required

## Running Tests

### Run All Tests

```bash
# From project root
cd code
mvn clean test

# With coverage report
mvn clean test jacoco:report
```

### Run Tests for Specific Module

```bash
# Run tests for a single module
cd code/pokerserver
mvn test

# Run specific test class
mvn test -Dtest=OnlineProfileTest

# Run specific test method
mvn test -Dtest=OnlineProfileTest#should_PersistAndUpdate_When_ProfileSaved
```

### Test Execution Tips

- **Fast feedback** - Run tests frequently during development
- **Module isolation** - Test individual modules to identify issues quickly
- **Parallel execution** - Maven Surefire runs tests in parallel by default
- **CI readiness** - All tests must pass before committing

## Test Organization

### Module Structure

```
code/
├── common/           # 122 tests - Core utilities, config, prefs
├── poker/            # 3 tests - Game engine, hand evaluation
├── pokerserver/      # 34 tests - Server services, DAOs
├── jsp/              # 2 tests - JSP rendering
├── wicket/           # 1 test - Wicket utilities
└── gameserver/       # 6 tests - (will be removed with licensing)
```

### Test File Location

Tests mirror the production code structure:

```
code/pokerserver/
├── src/
│   ├── main/java/
│   │   └── com/donohoedigital/games/poker/server/
│   │       └── OnlineProfileService.java
│   └── test/java/
│       └── com/donohoedigital/games/poker/server/
│           └── OnlineProfileServiceTest.java
```

## Test Coverage

### Current Coverage

- **Total Tests**: 168 test methods across all modules
- **Pass Rate**: 99.4% (167/168 passing)
- **Code Coverage**: Minimum 65% required (use `mvn jacoco:report` to verify)

### Coverage by Module

| Module | Tests | Status |
|--------|-------|--------|
| common | 122 | ✅ 100% passing |
| pokerserver | 34 | ✅ 100% passing |
| poker | 3 | ✅ ~99% passing (1 cleanup issue) |
| jsp | 2 | ✅ 100% passing |
| wicket | 1 | ✅ 100% passing |
| gui | 1 | ✅ 100% passing |
| gameserver | 6 | ⚠️ Low priority (licensing removal) |

### Viewing Coverage Reports

```bash
# Generate coverage report
mvn clean test jacoco:report

# Open report (varies by OS)
# Windows
start code/target/site/jacoco/index.html

# macOS
open code/target/site/jacoco/index.html

# Linux
xdg-open code/target/site/jacoco/index.html
```

## Best Practices

### DO ✅

- **Use descriptive test names** - `should_Do_When_Condition` pattern
- **Use AssertJ** - Fluent assertions are more readable
- **Test one thing** - Each test should verify one behavior
- **Arrange-Act-Assert** - Clear test structure
- **Use @TempDir** - For file system tests
- **Use H2 in-memory** - For database tests
- **Use @Rollback** - For transactional tests
- **Run tests frequently** - Fast feedback loop

### DON'T ❌

- **Don't use JUnit 4** - All tests use JUnit 5 (Jupiter)
- **Don't use generic names** - Avoid `testMethod1`, `testSomething`
- **Don't make tests depend on each other** - Each test should be independent
- **Don't use Thread.sleep()** - Prefer explicit waits or test utilities
- **Don't test implementation details** - Test behavior, not internals
- **Don't skip cleanup** - Use @TempDir, @Rollback, or @AfterEach
- **Don't commit failing tests** - All tests must pass

### Code Style

```java
// ✅ GOOD - Clear, fluent, descriptive
@Test
void should_ReturnUser_When_UserExists() {
    User user = new User("test");
    userService.save(user);

    User result = userService.findByName("test");

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("test");
}

// ❌ BAD - Generic name, JUnit 4 assertions, unclear
@Test
public void testUser() {
    User u = new User("test");
    userService.save(u);
    User r = userService.findByName("test");
    assertTrue(r != null);
    assertEquals("test", r.getName());
}
```

## Test Examples

### Unit Test Example

From `code/poker/src/test/java/com/donohoedigital/games/poker/HandInfoTest.java`:

```java
@Test
void should_CalculateCorrectScores_When_EvaluatingPokerHands() {
    new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

    verify("Royal Flush (clubs)", 10000014,
        CLUBS_A, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_K);
    verify("Straight Flush K", 9000013,
        HEARTS_9, HEARTS_J, HEARTS_K, HEARTS_Q, CLUBS_Q, HEARTS_T, HEARTS_3);
    verify("Quads", 8000135,
        CLUBS_8, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7, HEARTS_7);
}

private static void verify(String name, int expected, Card... cards) {
    HandSorted hand = new HandSorted();
    for (Card card : cards) {
        if (card != null) hand.addCard(card);
    }

    HandInfo info = new HandInfo(testPlayer, hand, null);
    int fastScore = new HandInfoFast().getScore(info.getHole(), info.getCommunity());

    assertThat(fastScore)
        .as("Score for %s doesn't match expected", name)
        .isEqualTo(expected);
}
```

### Spring Integration Test Example

From `code/pokerserver/src/test/java/.../OnlineProfileTest.java`:

```java
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class OnlineProfileTest {

    @Autowired
    private OnlineProfileDao dao;

    @Test
    @Rollback
    void should_PersistAndUpdate_When_ProfileSaved() {
        String password = "foobar";
        OnlineProfile newProfile = createTestProfile("TEST");
        newProfile.setPassword(password);
        dao.save(newProfile);

        assertThat(newProfile.getId()).isNotNull();

        OnlineProfile fetch = dao.get(newProfile.getId());
        assertThat(fetch.getName()).isEqualTo(newProfile.getName());
        assertThat(fetch.getPassword()).isEqualTo(password);

        String key = "1111-1111-1111-1111";
        newProfile.setLicenseKey(key);
        dao.update(newProfile);

        OnlineProfile updated = dao.get(newProfile.getId());
        assertThat(updated.getLicenseKey()).isEqualTo(key);
    }
}
```

## Troubleshooting

### Common Issues

**Tests not running:**
```bash
# Ensure JUnit 5 dependency is in pom.xml
# Check Maven Surefire is version 3.0+
mvn dependency:tree | grep junit
```

**Compilation errors:**
```bash
# Clean and rebuild
mvn clean compile test-compile
```

**Database connection errors in tests:**
```bash
# Verify H2 is configured in test resources
# Check src/test/resources/application-test.properties
# Ensure dataSource.jdbcUrl uses H2
```

**Spring context not loading:**
```bash
# Verify test context XML exists
# Check @SpringJUnitConfig location path
# Ensure all required beans are defined
```

## Migration from JUnit 4

All tests have been migrated from JUnit 4 to JUnit 5. If you encounter any remaining JUnit 4 tests:

### Quick Migration Guide

1. **Update imports:**
   ```java
   // OLD (JUnit 4)
   import org.junit.Test;
   import static org.junit.Assert.*;

   // NEW (JUnit 5)
   import org.junit.jupiter.api.Test;
   import static org.assertj.core.api.Assertions.*;
   ```

2. **Update Spring annotations:**
   ```java
   // OLD
   @RunWith(SpringJUnit4ClassRunner.class)
   @ContextConfiguration(locations = {"/app-context.xml"})

   // NEW
   @SpringJUnitConfig(locations = {"/app-context.xml"})
   ```

3. **Update lifecycle annotations:**
   ```java
   // OLD: @Before → NEW: @BeforeEach
   // OLD: @After → NEW: @AfterEach
   // OLD: @BeforeClass → NEW: @BeforeAll
   // OLD: @AfterClass → NEW: @AfterAll
   ```

4. **Convert assertions to AssertJ:**
   ```java
   // OLD
   assertEquals(expected, actual);

   // NEW
   assertThat(actual).isEqualTo(expected);
   ```

5. **Rename tests to BDD style:**
   ```java
   // OLD
   @Test
   public void testSave() { }

   // NEW
   @Test
   void should_PersistEntity_When_SaveCalled() { }
   ```

## Contributing

When adding new tests:

1. **Follow naming conventions** - Use `should_Do_When_Condition`
2. **Use JUnit 5** - Never add JUnit 4 tests
3. **Use AssertJ** - Fluent assertions only
4. **Add to appropriate module** - Match production code location
5. **Ensure tests pass** - Run locally before committing
6. **Maintain coverage** - Don't reduce existing coverage
7. **Document complex tests** - Add comments for non-obvious logic

## References

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Spring Test Documentation](https://docs.spring.io/spring-framework/reference/testing.html)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)

## Poker Game Testing Patterns

### IntegrationTestBase

All poker game tests extend `IntegrationTestBase` which provides complete game engine setup:

```java
import com.donohoedigital.games.poker.integration.IntegrationTestBase;

@Tag("integration")
class MyPokerTest extends IntegrationTestBase {
    // IntegrationTestBase provides:
    // - ConfigManager setup for headless testing
    // - PropertyConfig initialization
    // - ImageConfig and StylesConfig setup
    // - MockGameEngine initialization

    @Test
    void should_DoSomething_When_ConditionMet() {
        // Your test code with full game infrastructure available
    }
}
```

### Card Dealing Helpers

When testing poker hands, use these standard helper patterns:

```java
/**
 * Deal specific pocket cards to a player
 */
private void dealPocketCards(PokerPlayer p, Card c1, Card c2) {
    Hand pocket = p.getHand();
    pocket.clear();
    pocket.addCard(c1);
    pocket.addCard(c2);
}

/**
 * Deal community cards
 */
private void dealCommunity(Card... cards) {
    Hand community = hand.getCommunity();
    community.clear();
    for (Card c : cards) {
        community.addCard(c);
    }
}

// Usage example:
dealPocketCards(player,
    new Card(CardSuit.SPADES, Card.ACE),
    new Card(CardSuit.HEARTS, Card.ACE));

dealCommunity(
    new Card(CardSuit.SPADES, Card.KING),
    new Card(CardSuit.HEARTS, Card.QUEEN),
    new Card(CardSuit.DIAMONDS, Card.JACK),
    new Card(CardSuit.CLUBS, Card.TEN),
    new Card(CardSuit.SPADES, Card.NINE)
);
```

**Important:** Card classes are in `com.donohoedigital.games.poker.engine` package:
```java
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
```

### Game Setup Pattern

Standard pattern for creating game infrastructure:

```java
@BeforeEach
void setUp() {
    // Create game with profile
    game = new PokerGame(null);
    TournamentProfile profile = new TournamentProfile("test");
    profile.setBuyinChips(1500);
    game.setProfile(profile);

    // Create table
    table = new PokerTable(game, 1);
    table.setMinChip(1); // Required for betting calculations

    // Create players
    for (int i = 0; i < 3; i++) {
        players[i] = new PokerPlayer(i + 1, "Player" + i, true);
        players[i].setChipCount(1000);
        game.addPlayer(players[i]);
        table.setPlayer(players[i], i);
    }

    // Initialize hand (must call newHand() BEFORE creating HoldemHand!)
    table.setButton(0);
    for (PokerPlayer p : players) {
        p.newHand('p'); // CRITICAL: Call before creating hand
    }

    hand = new HoldemHand(table);
    table.setHoldemHand(hand);
    hand.setBigBlind(20);
    hand.setSmallBlind(10);
    hand.setPlayerOrder(false);
}
```

**Critical Timing:** Always call `player.newHand('p')` BEFORE creating `HoldemHand`. The `newHand()` method captures the chip count at the start of the hand, which is required for proper betting calculations.

### Reflection for Private Fields

Many poker classes use private fields without setters (by design). Use reflection for test setup:

```java
/**
 * Set private rebuy count for testing
 */
private void setNumRebuys(PokerPlayer player, int count) {
    try {
        Field field = PokerPlayer.class.getDeclaredField("nNumRebuy_");
        field.setAccessible(true);
        field.set(player, count);
    } catch (Exception e) {
        throw new RuntimeException("Failed to set rebuy count", e);
    }
}

/**
 * Set TournamentProfile values via internal map
 */
private void setProfileInteger(String key, int value) {
    try {
        Field mapField = TournamentProfile.class.getDeclaredField("map_");
        mapField.setAccessible(true);
        Object map = mapField.get(profile);

        map.getClass().getMethod("setInteger", String.class, Integer.class)
           .invoke(map, key, value);
    } catch (Exception e) {
        throw new RuntimeException("Failed to set profile integer", e);
    }
}

// Usage:
setNumRebuys(player, 2);
setProfileInteger("rebuychips", 1500);
setProfileInteger("addonchips", 2000);
```

### Money-Critical Testing: Chip Conservation

**CRITICAL:** Every test involving money operations must verify chip conservation:

```java
@Test
void should_DistributeExactPotAmount_When_SingleWinner() {
    // Setup cards and betting...

    // Capture total chips BEFORE resolution
    int totalChipsBefore = 0;
    for (PokerPlayer p : players) {
        totalChipsBefore += p.getChipCount();
    }
    totalChipsBefore += hand.getTotalPotChipCount();

    // Execute operation
    hand.resolve();

    // Verify total chips AFTER resolution
    int totalChipsAfter = 0;
    for (PokerPlayer p : players) {
        totalChipsAfter += p.getChipCount();
    }

    // CRITICAL: Total must be unchanged (no money created/destroyed)
    assertThat(totalChipsAfter).isEqualTo(totalChipsBefore);
}
```

**Pattern for all money operations:**
1. Capture total chips before operation
2. Execute operation
3. Verify total chips unchanged after operation
4. Optionally verify distribution details

### Betting Test Patterns

```java
@Test
void should_DeductChips_When_BetCalled() {
    int initialChips = player.getChipCount();
    int betAmount = 100;

    // Must set current player index before betting
    hand.setCurrentPlayerIndex(0);
    player.bet(betAmount, "test bet");

    assertThat(player.getChipCount()).isEqualTo(initialChips - betAmount);
}

@Test
void should_BeAllIn_When_BettingAllChips() {
    int allChips = player.getChipCount();

    hand.setCurrentPlayerIndex(0);
    player.bet(allChips, "all-in");

    assertThat(player.getChipCount()).isEqualTo(0);
    assertThat(player.isAllIn()).isTrue();
}
```

**Important:**
- Must call `hand.setCurrentPlayerIndex()` before any betting method
- Use `hand.advanceRound()` to move to next betting round
- Community cards must be dealt even for uncontested pots (resolve() evaluates hands)

### Hand Evaluation

Hand evaluation happens automatically - don't try to do it manually:

```java
// ✅ GOOD - Let resolve() handle hand evaluation
dealPocketCards(player0, ...);
dealPocketCards(player1, ...);
dealCommunity(...);

hand.resolve(); // Automatically evaluates hands and determines winner

// ❌ BAD - Don't manually call hand evaluation
players[0].getHandInfo().setHand(...); // This method doesn't exist!
```

### Pot Distribution Testing

```java
@Test
void should_SplitPotEvenly_When_TwoWayTie() {
    // Deal identical hands to create tie
    dealPocketCards(players[0],
        new Card(CardSuit.SPADES, Card.ACE),
        new Card(CardSuit.HEARTS, Card.KING));

    dealPocketCards(players[1],
        new Card(CardSuit.CLUBS, Card.ACE),
        new Card(CardSuit.DIAMONDS, Card.KING));

    // Board that creates same hand for both
    dealCommunity(
        new Card(CardSuit.SPADES, Card.QUEEN),
        new Card(CardSuit.HEARTS, Card.QUEEN),
        new Card(CardSuit.DIAMONDS, Card.JACK),
        new Card(CardSuit.CLUBS, Card.TEN),
        new Card(CardSuit.SPADES, Card.NINE)
    );

    // Create pot
    createSimplePot(100); // 200 total

    int player0Before = players[0].getChipCount();
    int player1Before = players[1].getChipCount();

    // Resolve
    hand.resolve();

    // Verify even split
    assertThat(players[0].getChipCount()).isEqualTo(player0Before + 100);
    assertThat(players[1].getChipCount()).isEqualTo(player1Before + 100);
}
```

### Test Organization for Poker Module

**Test Categories:**

1. **Unit Tests** - Pure functions, no game engine
   - Example: `HandInfoTest` - Hand scoring calculations

2. **Integration Tests** - Require game engine (extend IntegrationTestBase)
   - Example: `PokerPlayerChipTest` - Chip management operations
   - Example: `HoldemHandPotDistributionTest` - Pot distribution

3. **AI Tests** - AI decision making (extend IntegrationTestBase)
   - Example: `V2PlayerHandStrengthTest` - Hand strength calculations
   - Example: `PokerAIPositionTest` - Position query methods

**Tag Integration Tests:**
```java
@Tag("integration")
class MyIntegrationTest extends IntegrationTestBase {
    // Tests requiring full game engine setup
}
```

## Test Coverage Enforcement

### Jacoco Configuration

The poker module enforces minimum coverage thresholds to prevent regressions:

**Minimum Coverage Requirements:**
- **Main Package** (`com.donohoedigital.games.poker`): 40% instruction coverage
- **AI Package** (`com.donohoedigital.games.poker.ai`): 50% instruction coverage

**Excluded from Enforcement:**
- UI packages: `com.donohoedigital.games.poker.dashboard`, `com.donohoedigital.games.poker.ai.gui`
- Third-party code: `com.zookitec.layout`

**Running Coverage Checks:**
```bash
# Generate coverage report
cd code/poker
mvn test jacoco:report

# Check if coverage meets thresholds
mvn verify

# View HTML report
start target/site/jacoco/index.html
```

**Coverage Violations:**
If coverage falls below thresholds, the build will fail with:
```
[ERROR] Rule violated for package com.donohoedigital.games.poker:
        instructions covered ratio is 0.35, but expected minimum is 0.40
```

### Current Coverage Status

**Overall Poker Module:** ~20%
- **Main Package** (`com.donohoedigital.games.poker`): 19%
  - HoldemHand: 47.9% ✅
  - PokerGame: 45% ✅
  - PokerPlayer: 29% ⚠️
  - PokerTable: ~0% 🔴

- **AI Package** (`com.donohoedigital.games.poker.ai`): 50% ✅
  - V2Player: 40% ✅
  - PokerAI: 23% ⚠️
  - V1Player: 4% 🔴
  - RuleEngine: 5% 🔴

- **Online Package** (`com.donohoedigital.games.poker.online`): 1% 🔴

**Total Tests:** 1,292 (51 added in Phase 6)

## Status

✅ **Test modernization complete** (February 2026)
- All 16 JUnit 4 test files migrated to JUnit 5
- 1,292 test methods across all modules
- AssertJ assertions throughout
- BDD-style naming conventions
- H2 in-memory database for integration tests
- 100% test pass rate

✅ **Money-critical operations protected** (February 2026)
- All chip management operations tested (Phase 6A)
- All pot distribution operations tested (Phase 6B)
- Chip conservation verified via invariant tests
- 51 new poker game logic tests created
