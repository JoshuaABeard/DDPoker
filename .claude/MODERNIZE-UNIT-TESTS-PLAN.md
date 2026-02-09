# Modernize Unit Tests and Achieve Full Test Suite Pass

## Context

DD Poker Community Edition has 56 test files with mixed testing frameworks and patterns. The test suite needs modernization to ensure consistency, maintainability, and full pass rate across all modules.

**Current state:**
- **56 total test files** across all modules
- **8 files** using JUnit 5 (Jupiter) with AssertJ (modern, recent additions)
- **18 files** using JUnit 4 (legacy, needs migration)
- **30 files** using mixed or unclear testing patterns
- **Common module:** 37 tests passing (100% pass rate) ✅
- **Other modules:** Blocked by compilation errors (jsp module issues)
- **Testing infrastructure:** JUnit 5, AssertJ, and Mockito already added to common/pom.xml

**Why this change is needed:**
- **Inconsistent patterns** - Mix of JUnit 4 and JUnit 5 creates confusion
- **Modern best practices** - JUnit 5 offers better parameterized testing, test lifecycle, and assertions
- **Compilation blockers** - jsp module prevents full test suite from running
- **TDD readiness** - Establish patterns for file-based config and licensing removal work
- **Maintainability** - Consistent test patterns make codebase easier to understand
- **CI/CD preparation** - Full test suite pass needed for automated builds

**Desired state:**
- All tests migrated to JUnit 5 (Jupiter)
- Consistent use of AssertJ for fluent assertions
- Clear test naming conventions (should_Do_When_Condition)
- All tests passing (`mvn clean test` = 100% success)
- Compilation issues resolved
- Test coverage maintained or improved (65% minimum)

## Implementation Approach

### 1. Fix Compilation Blockers (jsp Module)

**Problem:** jsp module has compilation errors preventing test execution:
```
cannot find symbol: class DefaultRuntimeDirectory
cannot find symbol: variable ConfigManager
cannot find symbol: class ApplicationError
```

**Root cause analysis needed:**
- Missing dependencies in jsp/pom.xml
- Incorrect import statements
- Classes moved or deleted in refactoring

**Resolution strategy:**
1. **Investigate missing classes** - Find where DefaultRuntimeDirectory, ApplicationError live
2. **Add missing dependencies** - Update jsp/pom.xml with correct module dependencies
3. **Fix import statements** - Correct package paths
4. **Alternative:** If jsp module is unused, consider excluding from build

**Files to investigate:**
- `code/jsp/pom.xml` - Check dependencies
- `code/jsp/src/main/java/com/donohoedigital/jsp/JspEmail.java` - Fix imports
- `code/jsp/src/main/java/com/donohoedigital/jsp/JspFile.java` - Fix imports
- `code/jsp/src/main/java/com/donohoedigital/jsp/EmbeddedServletContext.java` - Fix imports

### 2. Migrate JUnit 4 Tests to JUnit 5

**18 files need migration from JUnit 4 to JUnit 5:**

#### Migration Pattern

**JUnit 4 (old):**
```java
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

public class SampleTest {

    @Before
    public void setUp() {
        // setup code
    }

    @Test
    public void testSomething() {
        assertEquals(expected, actual);
    }

    @After
    public void tearDown() {
        // cleanup code
    }
}
```

**JUnit 5 (new):**
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

class SampleTest {  // Note: no need for public

    @BeforeEach
    void setUp() {  // Note: no need for public
        // setup code
    }

    @Test
    void should_DoSomething_When_ConditionMet() {  // Descriptive name
        assertThat(actual).isEqualTo(expected);  // AssertJ fluent assertion
    }

    @AfterEach
    void tearDown() {
        // cleanup code
    }
}
```

#### Key Changes

**Annotations:**
- `@Test` → `@org.junit.jupiter.api.Test`
- `@Before` → `@BeforeEach`
- `@After` → `@AfterEach`
- `@BeforeClass` → `@BeforeAll`
- `@AfterClass` → `@AfterAll`
- `@Ignore` → `@Disabled`

**Assertions (migrate to AssertJ):**
```java
// JUnit 4 (old)
assertEquals(expected, actual);
assertTrue(condition);
assertFalse(condition);
assertNull(value);
assertNotNull(value);

// AssertJ (new)
assertThat(actual).isEqualTo(expected);
assertThat(condition).isTrue();
assertThat(condition).isFalse();
assertThat(value).isNull();
assertThat(value).isNotNull();
```

**Exception testing:**
```java
// JUnit 4 (old)
@Test(expected = IllegalArgumentException.class)
public void testException() {
    methodThatThrows();
}

// JUnit 5 (new)
@Test
void should_ThrowException_When_InvalidInput() {
    assertThatThrownBy(() -> methodThatThrows())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Expected error message");
}
```

**Parameterized tests:**
```java
// JUnit 5 has much better parameterized testing
@ParameterizedTest
@ValueSource(strings = {"value1", "value2", "value3"})
void should_HandleMultipleInputs_When_Parameterized(String input) {
    assertThat(processInput(input)).isNotNull();
}

@ParameterizedTest
@CsvSource({
    "input1, expected1",
    "input2, expected2",
    "input3, expected3"
})
void should_MapInputToOutput_When_GivenMultipleCases(String input, String expected) {
    assertThat(processInput(input)).isEqualTo(expected);
}
```

#### Files to Migrate (Priority Order)

**High Priority (Core functionality):**
1. `code/poker/src/test/java/com/donohoedigital/games/poker/HandInfoTest.java`
2. `code/poker/src/test/java/com/donohoedigital/games/poker/PokerDatabaseTest.java`
3. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineProfileServiceTest.java`
4. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineGameServiceTest.java`

**Medium Priority (Server components):**
5. `code/gameserver/src/test/java/com/donohoedigital/games/server/BannedKeyServiceTest.java`
6. `code/gameserver/src/test/java/com/donohoedigital/games/server/RegistrationServiceTest.java`
7. `code/gameserver/src/test/java/com/donohoedigital/games/server/BannedKeyTest.java`
8. `code/gameserver/src/test/java/com/donohoedigital/games/server/RegistrationTest.java`
9. `code/gameserver/src/test/java/com/donohoedigital/games/server/UpgradedKeyTest.java`

**Low Priority (To be deleted with licensing removal):**
10. These tests will be removed as part of licensing removal plan, so migration may not be needed

**Remaining files:**
11-18. Other test files in pokerserver, jsp, and wicket modules

### 3. Standardize Test Naming Conventions

**Current naming:** Mixed patterns (testSomething, testSomethingElse, should_DoX)

**New standard:** BDD-style naming with clear intent

**Pattern:**
```
should_<ExpectedBehavior>_When_<Condition>
should_<ExpectedBehavior>_Given_<Precondition>
```

**Examples:**
```java
@Test
void should_ReturnNull_When_KeyNotFound() { }

@Test
void should_CreateUUID_When_FirstRunDetected() { }

@Test
void should_ThrowException_When_ConfigCorrupted() { }

@Test
void should_PersistSettings_When_FlushCalled() { }

@Test
void should_ReturnDefault_When_ValueNotSet() { }
```

**Benefits:**
- Immediately clear what the test does
- Easy to scan test results
- Self-documenting test suite
- Matches patterns in new file-based config and licensing removal plans

### 4. Add Missing Test Coverage

**Areas needing tests (discovered during planning):**

#### A. PlayerIdentity (Licensing Removal Prep)
File doesn't exist yet, but plan calls for comprehensive tests:
- UUID v4 generation
- Platform-specific directory detection
- Save/load operations
- Corruption recovery

**Status:** Will be created as part of licensing removal TDD work

#### B. GameEngine Player ID Methods (Licensing Removal Prep)
After refactoring removes license methods:
- `getPlayerId()` - Should return valid UUID
- `setPlayerId()` - Should persist to file
- Verification that old license methods removed

**Status:** Will be created as part of licensing removal TDD work

#### C. Database Migration Tests
For both file-based config and licensing removal:
- Schema changes
- Column additions/removals
- Constraint enforcement
- Data preservation

**Status:** New test category needed

### 5. Resolve Module-Specific Issues

#### jsp Module (Blocking compilation)
**Problem:** Missing class imports preventing build
**Solution options:**
1. **Fix dependencies** - Add missing modules to jsp/pom.xml
2. **Refactor imports** - Update to correct package paths
3. **Mark as optional** - Exclude jsp from default build if unused

**Decision needed:** Is jsp module still used? Check:
- `JspEmail.java` - Email rendering functionality
- `JspFile.java` - File rendering functionality
- Are these used in pokerwicket or pokerserver?

#### gameserver Module (Legacy licensing tests)
**Status:** Has 6 test files related to licensing (BannedKey, Registration, UpgradedKey)
**Action:** These will be deleted as part of licensing removal plan
**Timeline:** No need to modernize if deleting soon

#### pokerserver Module
**Status:** Has 7 test files, some related to licensing
**Action:**
- Keep and modernize: OnlineGameServiceTest, TournamentHistoryTest
- Delete with licensing: Parts of OnlineProfileServiceTest (license key logic)

### 6. Update POMs for JUnit 5 Support

**Already done in common/pom.xml:**
```xml
<!-- JUnit 5 (Jupiter) -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>

<!-- AssertJ -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.27.0</version>
    <scope>test</scope>
</dependency>
```

**Need to add to other modules:**
- `code/gameserver/pom.xml`
- `code/poker/pom.xml`
- `code/pokerserver/pom.xml`
- `code/wicket/pom.xml`
- `code/jsp/pom.xml` (if keeping module)

**Also ensure Maven Surefire is configured:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.2</version>
        </plugin>
    </plugins>
</build>
```

### 7. Add Mockito for Complex Dependencies

**Use cases for mocking:**
- GameEngine initialization (heavy Spring context)
- Database connections (use H2 in-memory where possible, mock where not)
- File system operations (prefer @TempDir, mock when needed)
- Network calls (mock HTTP clients)

**Already added to common/pom.xml:**
```xml
<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>

<!-- Mockito-JUnit Jupiter integration -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

**Example usage:**
```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {

    @Mock
    private DatabaseService mockDatabase;

    @InjectMocks
    private UserService userService;

    @Test
    void should_ReturnUser_When_UserExists() {
        when(mockDatabase.findUser(1L)).thenReturn(new User(1L, "Test"));

        User result = userService.getUser(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test");
        verify(mockDatabase).findUser(1L);
    }
}
```

## Critical Files

### Files to Fix (Compilation Blockers):

1. **`code/jsp/src/main/java/com/donohoedigital/jsp/JspEmail.java`**
   - Missing: DefaultRuntimeDirectory, ConfigManager, ConfigUtils, ApplicationError

2. **`code/jsp/src/main/java/com/donohoedigital/jsp/JspFile.java`**
   - Missing: DefaultRuntimeDirectory, ConfigManager, ConfigUtils, ApplicationError

3. **`code/jsp/src/main/java/com/donohoedigital/jsp/EmbeddedServletContext.java`**
   - Missing: DefaultRuntimeDirectory

4. **`code/jsp/pom.xml`**
   - Likely missing dependencies on common or gameengine modules

### Files to Migrate (JUnit 4 → JUnit 5):

**High Priority (8 files):**
1. `code/poker/src/test/java/com/donohoedigital/games/poker/HandInfoTest.java`
2. `code/poker/src/test/java/com/donohoedigital/games/poker/PokerDatabaseTest.java`
3. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineProfileServiceTest.java`
4. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineGameServiceTest.java`
5. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineProfileTest.java`
6. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineGameTest.java`
7. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/TournamentHistoryServiceTest.java`
8. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/TournamentHistoryTest.java`

**Low Priority (10 files - will delete with licensing removal):**
9. `code/gameserver/src/test/java/com/donohoedigital/games/server/BannedKeyServiceTest.java`
10. `code/gameserver/src/test/java/com/donohoedigital/games/server/BannedKeyTest.java`
11. `code/gameserver/src/test/java/com/donohoedigital/games/server/RegistrationServiceTest.java`
12. `code/gameserver/src/test/java/com/donohoedigital/games/server/RegistrationTest.java`
13. `code/gameserver/src/test/java/com/donohoedigital/games/server/SpringCreatedServiceTest.java`
14. `code/gameserver/src/test/java/com/donohoedigital/games/server/UpgradedKeyTest.java`
15. `code/jsp/src/test/java/com/donohoedigital/jsp/JspEmailTest.java`
16. `code/jsp/src/test/java/com/donohoedigital/jsp/JspFileTest.java`
17. `code/wicket/src/test/java/com/donohoedigital/wicket/annotations/MixedParamEncoderTest.java`
18. `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineProfileServiceDummyTest.java`

### POMs to Update (Add JUnit 5 + AssertJ):

1. `code/gameserver/pom.xml`
2. `code/poker/pom.xml`
3. `code/pokerserver/pom.xml`
4. `code/wicket/pom.xml`
5. `code/jsp/pom.xml` (if keeping)

### Existing Modern Tests (Reference for patterns):

**Already using JUnit 5 + AssertJ (8 files):**
1. `code/common/src/test/java/com/donohoedigital/config/FilePrefsTest.java` ⭐ **Best example**
2. `code/common/src/test/java/com/donohoedigital/config/FilePrefsAdapterTest.java`
3. `code/common/src/test/java/com/donohoedigital/config/PrefsTest.java`
4. `code/common/src/test/java/com/donohoedigital/base/UtilsTest.java`
5. `code/common/src/test/java/com/donohoedigital/base/ManagedQueueTest.java`
6. `code/common/src/test/java/com/donohoedigital/comms/VersionTest.java`
7. `code/common/src/test/java/com/donohoedigital/comms/DataMarshallerTest.java`
8. `code/common/src/test/java/com/donohoedigital/config/ActivationTest.java` (will be deleted)

## Verification Steps

### 1. Fix Compilation Issues

**Test jsp module:**
```bash
cd code/jsp
mvn clean compile
```

**Expected:** Zero compilation errors

**If errors persist:**
- Check if jsp module is actually needed
- Consider excluding from parent pom.xml build

### 2. Run Full Test Suite

**Command:**
```bash
cd code
mvn clean test
```

**Expected output:**
```
[INFO] Tests run: X, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 3. Verify Each Module

**Test each module independently:**
```bash
cd code/common && mvn test        # ✅ Already passing (37 tests)
cd code/gameserver && mvn test    # Need to verify
cd code/poker && mvn test         # Need to verify
cd code/pokerserver && mvn test   # Need to verify
cd code/wicket && mvn test        # Need to verify
```

### 4. Check Test Coverage

**Generate coverage report:**
```bash
mvn clean test jacoco:report
```

**Review coverage:**
- Open `target/site/jacoco/index.html` in browser
- Ensure 65% minimum coverage maintained
- Identify untested code paths

### 5. Verify Migration Quality

**For each migrated test file:**
- [ ] Uses JUnit 5 annotations (`@Test`, `@BeforeEach`, etc.)
- [ ] Uses AssertJ assertions (`assertThat()`)
- [ ] Uses descriptive test names (`should_Do_When_Condition`)
- [ ] Removes JUnit 4 imports
- [ ] All tests pass
- [ ] No test functionality lost

### 6. Integration Testing

**End-to-end verification:**
```bash
# Build entire project
mvn clean install

# Run application
java -jar code/poker/target/DDPoker.jar

# Verify:
# - Application starts without errors
# - Settings load correctly
# - Game functionality works
# - No regressions introduced by test changes
```

## Expected Outcomes

After implementation:

### 1. **Unified Testing Framework**
- All tests use JUnit 5 (Jupiter)
- Consistent AssertJ assertions
- Modern testing patterns throughout
- Easier to onboard new contributors

### 2. **Full Test Suite Pass**
- `mvn clean test` = 100% success
- No compilation errors
- No failing tests
- No skipped tests (unless intentionally disabled)

### 3. **Better Test Readability**
- Descriptive test names (should_Do_When_Condition)
- Clear assertion messages
- Easy to understand test intent
- Self-documenting test suite

### 4. **Improved Developer Experience**
- Faster test execution (JUnit 5 optimizations)
- Better parameterized testing
- Clearer test failure messages
- Easier debugging

### 5. **Foundation for New Work**
- Ready for file-based config TDD
- Ready for licensing removal TDD
- Patterns established for future tests
- CI/CD pipeline ready

### 6. **Maintained or Improved Coverage**
- Minimum 65% code coverage maintained
- No coverage regressions
- Clear gaps identified for future work

## Implementation Phases

### Phase 0: Investigation & Planning (2-3 hours)
1. **Investigate jsp module compilation errors**
   - Find where missing classes live (DefaultRuntimeDirectory, ApplicationError, ConfigManager)
   - Check if jsp module is actually used in codebase
   - Decide: fix, exclude, or delete jsp module
2. **Audit test suite**
   - Run tests module-by-module to identify failing tests
   - Document failure patterns
   - Identify tests that depend on licensing code (will be deleted)
3. **Create migration priority list**
   - Based on failure analysis and usage patterns

### Phase 1: Fix Compilation Blockers (2-4 hours)
1. **Option A: Fix jsp module**
   - Add missing dependencies to jsp/pom.xml
   - Fix import statements
   - Verify compilation succeeds
2. **Option B: Exclude jsp module**
   - Comment out jsp module in parent pom.xml
   - Verify rest of project compiles
   - Document reason for exclusion
3. **Verify compilation**
   - `mvn clean compile` succeeds for entire project
   - All modules accessible for testing

### Phase 2: Update POMs (1-2 hours)
1. **Add JUnit 5 dependencies** to all module POMs:
   - gameserver/pom.xml
   - poker/pom.xml
   - pokerserver/pom.xml
   - wicket/pom.xml
2. **Add AssertJ** to all module POMs
3. **Add Mockito** (if needed) to module POMs
4. **Configure Maven Surefire** plugin for JUnit 5
5. **Verify** - Run `mvn test` to ensure infrastructure works

### Phase 3: Migrate High-Priority Tests (6-8 hours)
**Migrate 8 core test files using TDD pattern:**

For each file:
1. **Read original test** - Understand what it tests
2. **Create new test file** - Start fresh with JUnit 5
3. **Copy test logic** - Migrate one test at a time
4. **Update assertions** - Convert to AssertJ
5. **Rename tests** - Use should_Do_When_Condition pattern
6. **Run tests** - Verify all pass
7. **Delete old test** - Remove JUnit 4 version

**Files:**
1. HandInfoTest.java
2. PokerDatabaseTest.java
3. OnlineProfileServiceTest.java
4. OnlineGameServiceTest.java
5. OnlineProfileTest.java
6. OnlineGameTest.java
7. TournamentHistoryServiceTest.java
8. TournamentHistoryTest.java

### Phase 4: Full Test Suite Verification (2-3 hours)
1. **Run full test suite** - `mvn clean test`
2. **Fix any failures** - Debug and resolve issues
3. **Document skipped tests** - Note any @Disabled tests and why
4. **Generate coverage report** - Verify 65% maintained
5. **Integration test** - Build and run application

### Phase 5: Documentation & Cleanup (1-2 hours)
1. **Update TESTING.md** (create if doesn't exist)
   - Document testing patterns
   - Provide examples of good tests
   - Explain naming conventions
2. **Clean up old dependencies**
   - Remove JUnit 4 from POMs (if all tests migrated)
   - Remove unused test dependencies
3. **Update README.md**
   - Add section on running tests
   - Document test structure
4. **Create test template file**
   - Example test file for contributors to copy

### Phase 6: Low-Priority Tests (OPTIONAL - 4-5 hours)
**Only if time permits OR if needed before licensing removal:**

Migrate remaining 10 JUnit 4 test files:
- 6 gameserver tests (licensing-related, will be deleted soon)
- 2 jsp tests (if module kept)
- 1 wicket test
- 1 pokerserver dummy test

**Note:** These may be deleted as part of licensing removal, so migration has low value.

**Total Effort:** 14-22 hours (13-17 hours if skipping Phase 6)

## TDD Benefits for Test Modernization

✅ **Quality Assurance** - Verify tests still work after migration
✅ **No Regressions** - Ensure no test coverage lost
✅ **Clear Patterns** - Establish consistent testing style
✅ **Documentation** - Tests serve as examples for contributors
✅ **Foundation** - Ready for file-based config and licensing removal TDD work
✅ **CI/CD Ready** - Full test suite pass enables automated builds

## Success Criteria

The test modernization is successful when:

1. **Zero compilation errors** - `mvn clean compile` succeeds
2. **Full test suite passes** - `mvn clean test` = 100% success (all passing)
3. **All core tests migrated** - 8 high-priority tests using JUnit 5 + AssertJ
4. **Consistent naming** - All tests use should_Do_When_Condition pattern
5. **No coverage regression** - Maintain 65% minimum code coverage
6. **Clear documentation** - TESTING.md explains patterns and conventions
7. **POMs updated** - All modules have JUnit 5, AssertJ, Mockito dependencies
8. **Application works** - No regressions in functionality

## Notes and Considerations

### jsp Module Decision

**Question:** Is jsp module still used?

**Investigation needed:**
- Search codebase for references to `JspEmail` and `JspFile`
- Check if pokerserver or pokerwicket use these classes
- Review git history - when was jsp module last actively developed?

**Options:**
1. **Fix and keep** - If actively used, fix dependencies
2. **Exclude from build** - If unused, exclude from parent pom
3. **Delete entirely** - If completely obsolete, remove from codebase

**Recommendation:** Investigate first, then decide. Leaning toward exclusion if not clearly used.

### Licensing-Related Tests

**10 test files will be deleted** as part of licensing removal plan:
- BannedKeyServiceTest, BannedKeyTest
- RegistrationServiceTest, RegistrationTest
- UpgradedKeyTest
- OnlineProfileServiceDummyTest
- Parts of OnlineProfileServiceTest

**Decision:** Low priority for migration since they'll be deleted soon.

**Timeline:** If licensing removal starts before test modernization completes, skip migrating these files.

### Test Coverage Goals

**Current coverage:** Unknown (need to generate report)

**Minimum threshold:** 65% (project standard)

**Strategy:**
- Maintain existing coverage during migration
- Don't add new tests in this phase (that's for TDD phases)
- Identify coverage gaps for future work

### JUnit 4 vs JUnit 5 Coexistence

**Can they coexist?** Yes, JUnit 5 supports JUnit 4 tests via vintage engine

**Should we keep JUnit 4?** No, for consistency:
- Remove JUnit 4 dependency after all tests migrated
- Use only JUnit 5 going forward
- Cleaner, more maintainable test suite

### Mockito Usage Guidelines

**When to mock:**
- Heavy Spring context initialization (GameEngine)
- External dependencies (databases, file systems, networks)
- Slow operations in unit tests

**When NOT to mock:**
- Simple POJOs (just instantiate them)
- @TempDir for file operations (JUnit 5 provides this)
- In-memory H2 for database tests (prefer real database when fast)

### Parameterized Testing Examples

**Great use cases for @ParameterizedTest:**
- Platform detection (Windows, macOS, Linux)
- Input validation (valid/invalid inputs)
- Configuration variations
- Edge cases (boundary values)

**Example from file-based config plan:**
```java
@ParameterizedTest
@ValueSource(strings = {"windows 10", "windows 11", "windows server 2022"})
void should_DetectWindowsConfigDirectory_When_RunningOnWindows(String osName) {
    String configDir = PlayerIdentity.getConfigDirectory(osName);
    assertThat(configDir).contains("\\ddpoker");
}
```

## Related Plans

This test modernization plan supports and enables:

1. **FILE-BASED-CONFIG-PLAN.md** - TDD implementation needs modern test infrastructure
2. **REMOVE-LICENSING-PLAN.md** - Large refactoring needs comprehensive test coverage
3. **Future TDD work** - Establishes patterns and infrastructure for new features

## Risk Mitigation

### Risk 1: Breaking Existing Tests
**Mitigation:**
- Migrate one test at a time
- Run tests after each migration
- Keep original test until new one passes
- Use git branches for safety

### Risk 2: Lost Test Coverage
**Mitigation:**
- Generate coverage report before migration
- Generate coverage report after migration
- Compare to ensure no regression
- Document any intentional coverage changes

### Risk 3: jsp Module Dependency Hell
**Mitigation:**
- Time-box investigation to 2 hours
- If not resolved quickly, exclude module
- Document decision and rationale
- Revisit later if needed

### Risk 4: Time Overrun
**Mitigation:**
- Prioritize high-impact tests first
- Skip low-priority licensing tests
- Break work into phases
- Can pause between phases

### Risk 5: Introducing Regressions
**Mitigation:**
- Full test suite run after each phase
- Manual smoke testing of application
- Integration tests before marking complete
- Git branches for easy rollback
