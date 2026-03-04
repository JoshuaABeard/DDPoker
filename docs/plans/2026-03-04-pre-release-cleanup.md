# Pre-Release Cleanup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove all legacy/dead code and bring code quality up to modern standards before the Community Edition initial release.

**Architecture:** Three sequential branches — Branch 1 (HSQLDB→H2), Branch 2 (dead code deletion), Branch 3 (code quality). Each branch builds from main; work is verified with `mvn test -P dev` before committing.

**Tech Stack:** Java 25, Maven 3.9, JUnit 5 Jupiter, Log4j2, H2 2.3.232, Spring 6.2.15, Hibernate 6.6.42

---

## Key Files Reference

| File | Role |
|------|------|
| `code/poker/src/main/java/com/donohoedigital/games/poker/PokerDatabase.java` | Client-side local stats DB — HSQLDB driver |
| `code/poker/pom.xml:177-180` | HSQLDB dependency |
| `code/gameserver/pom.xml:73-77` | Dead HSQLDB dependency |
| `code/gameserver/src/main/resources/app-context-gameserver.xml:67-98` | Commented-out HSQLDB/MySQL blocks |
| `code/gameserver/src/main/java/com/donohoedigital/games/server/EngineServer.java` | Entire gameserver module — 6 files to delete |
| `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java` | Extends EngineServer — needs inline |
| `code/poker/src/main/resources/config/poker/gamedef.xml:716-800` | Phase XML entries to remove |
| `code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/` | 9 JUnit 4 test files to migrate |
| `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/` | 28 JUnit 4 test files to migrate |
| `code/gamecommon/src/main/java/com/donohoedigital/games/config/Hide.java` | StringBuffer API to update |
| `code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java` | System.out.println to replace |
| `code/poker/src/main/java/com/donohoedigital/games/poker/PokerStats.java` | System.out.println to replace |

---

## BRANCH 1: HSQLDB → H2

Branch off main: `git checkout -b cleanup/hsqldb-to-h2`

---

### Task 1: Migrate PokerDatabase from HSQLDB to H2

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerDatabase.java:72-73,1661`

**Step 1: Read the current driver constants and initDatabase method**

Read `PokerDatabase.java` lines 60–90 and around line 1640 to understand the connection setup and the HSQLDB-specific property call.

**Step 2: Update the driver and URL constants**

Change lines 72–73:
```java
// BEFORE
private static final String DATABASE_DRIVER_CLASS = "org.hsqldb.jdbcDriver";
private static final String DATABASE_DRIVER_URL_PREFIX = "jdbc:hsqldb:file:";

// AFTER
private static final String DATABASE_DRIVER_CLASS = "org.h2.Driver";
private static final String DATABASE_DRIVER_URL_PREFIX = "jdbc:h2:file:";
```

**Step 3: Remove the HSQLDB-specific cache property call**

Find and delete the line around line 1661:
```java
stmt.executeUpdate("SET PROPERTY \"hsqldb.cache_scale\" 10");
```
H2 does not support this property — remove the entire `stmt.executeUpdate(...)` call. If the surrounding try-with-resources block becomes empty, remove it too.

**Step 4: Verify the connection URL produces a valid H2 path**

Read the `initDatabase` method (around line 1635–1650) and confirm `DATABASE_DRIVER_URL_PREFIX + clientPath.getAbsolutePath()` produces a valid H2 file URL like `jdbc:h2:file:/Users/foo/.ddpoker/db/poker`. H2 file mode appends `.mv.db` automatically — no code change needed.

**Step 5: Update PokerDatabaseTest**

Read `code/poker/src/test/java/com/donohoedigital/games/poker/PokerDatabaseTest.java`. The test likely passes in a temp directory. Verify it doesn't hardcode `hsqldb` anywhere. If it does, apply the same driver/URL replacements. Also check for any `SET PROPERTY` statements in test setup and remove them.

**Step 6: Run the PokerDatabase tests**

```bash
cd code
mvn test -pl poker -Dtest=PokerDatabaseTest -P dev
```
Expected: BUILD SUCCESS, all PokerDatabaseTest tests pass.

If H2 rejects any SQL syntax: H2 is MySQL-compatible by default in server mode but not in embedded mode. Add `;MODE=MySQL` to the URL if needed: `jdbc:h2:file:/path/poker;MODE=MySQL`. Check the schema DDL in `initSchema()` for any HSQLDB-specific syntax (e.g., `CACHED TABLE` → plain `TABLE`).

**Step 7: Commit**

```bash
cd code
git add poker/src/main/java/com/donohoedigital/games/poker/PokerDatabase.java \
        poker/src/test/java/com/donohoedigital/games/poker/PokerDatabaseTest.java
git commit -m "refactor(db): migrate PokerDatabase from HSQLDB to H2"
```

---

### Task 2: Remove HSQLDB dependencies from POMs

**Files:**
- Modify: `code/poker/pom.xml:177-180`
- Modify: `code/gameserver/pom.xml:73-77`

**Step 1: Remove from poker/pom.xml**

Delete the `hsqldb` dependency block (around lines 177-180):
```xml
<dependency>
  <groupId>hsqldb</groupId>
  <artifactId>hsqldb</artifactId>
  <version>1.8.0.10</version>
</dependency>
```

H2 is already declared in `poker/pom.xml` (around line 139) — no new dep needed.

**Step 2: Remove from gameserver/pom.xml**

Delete the `hsqldb` dependency block (lines 73-77):
```xml
<dependency>
  <groupId>hsqldb</groupId>
  <artifactId>hsqldb</artifactId>
  <version>1.8.0.10</version>
  <scope>runtime</scope>
</dependency>
```

**Step 3: Verify the build still compiles**

```bash
cd code
mvn clean compile -P dev -q
```
Expected: BUILD SUCCESS with no `hsqldb` in classpath.

**Step 4: Commit**

```bash
git add poker/pom.xml gameserver/pom.xml
git commit -m "chore(deps): remove HSQLDB 1.8.0.10 from all POMs"
```

---

### Task 3: Remove commented dead config from app-context-gameserver.xml

**Files:**
- Modify: `code/gameserver/src/main/resources/app-context-gameserver.xml:67-98`

**Step 1: Read the file**

Read `app-context-gameserver.xml`. You will find two commented-out datasource blocks:
1. A MySQL `DriverManagerDataSource` block (lines ~67-72)
2. An HSQLDB design note comment + commented HSQLDB `driverClass`/`jdbcUrl` properties inside the active `ComboPooledDataSource` bean (lines ~74-98)

**Step 2: Remove the commented blocks**

Delete:
- The entire MySQL comment block (`<!-- single connection data source --> ... </bean>`)
- The HSQLDB design note comment block (`<!-- DESIGN note: ... -->`)
- The commented-out HSQLDB properties inside the `dataSource` bean (the `<!--<property name="driverClass" value="org.hsqldb..."/>-->` lines)

Keep the active `ComboPooledDataSource` bean with its `${db.driver}` / `${db.url}` property references intact.

**Step 3: Verify the XML is still valid**

```bash
cd code
mvn validate -pl gameserver -q
```
Expected: BUILD SUCCESS (Maven parses the POM/resources without error).

**Step 4: Commit**

```bash
git add gameserver/src/main/resources/app-context-gameserver.xml
git commit -m "chore: remove commented-out HSQLDB/MySQL config from app-context-gameserver.xml"
```

---

### Task 4: Full test run and merge Branch 1

**Step 1: Run full test suite**

```bash
cd code
mvn test -P dev
```
Expected: BUILD SUCCESS. All tests pass.

**Step 2: Merge to main**

```bash
git checkout main
git merge --no-ff cleanup/hsqldb-to-h2 -m "feat: migrate HSQLDB to H2, remove all HSQLDB references"
```

---

## BRANCH 2: Dead Code Removal

Branch off main: `git checkout -b cleanup/dead-code`

---

### Task 5: Inline EngineServer into PokerServer

`EngineServer` (in the `gameserver` module) is 9 lines — it extends `GameServer` and calls `postalService.destroy()` on shutdown. `PokerServer` extends it. The fix is to have `PokerServer` extend `GameServer` directly.

**Files:**
- Modify: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java`

**Step 1: Read PokerServer.java and EngineServer.java**

Read both files in full to understand what EngineServer provides.

**Step 2: Update PokerServer to extend GameServer directly**

Changes to `PokerServer.java`:
- Change `import com.donohoedigital.games.server.EngineServer;` → `import com.donohoedigital.server.GameServer;`
- Add `import com.donohoedigital.mail.DDPostalService;`
- Change `public class PokerServer extends EngineServer` → `public class PokerServer extends GameServer`
- Add field: `@Autowired private DDPostalService postalService;`
- In the `shutdown()` override, add `postalService.destroy();` before `super.shutdown(immediate);`

The result:
```java
import com.donohoedigital.mail.DDPostalService;
import com.donohoedigital.server.GameServer;

public class PokerServer extends GameServer {
    @Autowired
    private DDPostalService postalService;

    // ... existing init(), initializeAdminProfile(), etc. unchanged ...

    @Override
    protected void shutdown(boolean immediate) {
        postalService.destroy();
        super.shutdown(immediate);
    }
}
```

**Step 3: Verify it compiles**

```bash
cd code
mvn compile -pl pokerserver -am -P dev -q
```
Expected: BUILD SUCCESS.

**Step 4: Commit**

```bash
git add pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java
git commit -m "refactor: inline EngineServer into PokerServer (prep for gameserver module removal)"
```

---

### Task 6: Remove the gameserver module

**Files:**
- Delete: `code/gameserver/` (entire directory)
- Modify: `code/pom.xml` (remove `<module>gameserver</module>`)
- Modify: `code/pokerserver/pom.xml` (remove gameserver dependency)
- Modify: `code/gametools/pom.xml` (remove gameserver dependency)

**Step 1: Verify no remaining references**

```bash
cd code
grep -r "com.donohoedigital.games.server" --include="*.java" --include="*.xml" \
     --exclude-dir=gameserver --exclude-dir=target
```
Expected: zero hits. If any hits appear, address them before deleting.

**Step 2: Remove gameserver from parent pom.xml**

In `code/pom.xml`, delete the line:
```xml
<module>gameserver</module>
```

**Step 3: Remove gameserver dependency from pokerserver/pom.xml**

Find and delete the dependency block referencing `gameserver`:
```xml
<dependency>
  <groupId>com.donohoedigital</groupId>
  <artifactId>gameserver</artifactId>
  <version>3.3.0-CommunityEdition</version>
</dependency>
```

**Step 4: Remove gameserver dependency from gametools/pom.xml**

Same pattern — find and delete the `gameserver` dependency block.

**Step 5: Delete the gameserver directory**

```bash
cd code
rm -rf gameserver/src
rm gameserver/pom.xml
rmdir gameserver 2>/dev/null || true
```

Then tell git:
```bash
git rm -r gameserver/src gameserver/pom.xml
```

**Step 6: Verify the build**

```bash
cd code
mvn clean compile -P dev -q
```
Expected: BUILD SUCCESS. No references to `gameserver` module remain.

**Step 7: Commit**

```bash
git add -A
git commit -m "refactor: delete gameserver module (superseded by pokergameserver)"
```

---

### Task 7: Audit dead phase classes before deletion

Before deleting the 10 dead phase Java files, verify they are truly unreachable.

**Files:**
- Read: `code/poker/src/main/resources/config/poker/gamedef.xml` (around lines 710-800)

**Step 1: Confirm the phase classes are only referenced from gamedef.xml**

```bash
cd code
for cls in Bet ButtonDisplay WaitForDeal DealButton PreShowdown CheckEndHand \
           DisplayTableMoves NewLevelActions ColorUp ColorUpFinish; do
    echo "=== $cls ==="
    grep -rn "$cls" --include="*.java" --exclude-dir=target \
         poker/src/main/java | grep -v "class $cls\|import.*$cls"
done
```

**Expected:** Only cross-references between the phase classes themselves (e.g., `CheckEndHand` calling `NewLevelActions.rebuy()`). No calls from `WebSocketTournamentDirector`, `ShowTournamentTable`, or any other active class.

If anything unexpected appears — stop and report before proceeding.

**Step 2: Verify WebSocketTournamentDirector never instantiates phases**

```bash
grep -n "runPhase\|ChainPhase\|getPhase\|phase\.start" \
     code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
```
Expected: zero hits for phase instantiation.

**Step 3: Document the outcome**

If all checks pass, proceed to Task 8. If any active caller is found, do not delete that file — assess separately.

---

### Task 8: Remove dead phase XML entries and Java files

**Files:**
- Modify: `code/poker/src/main/resources/config/poker/gamedef.xml:716-800`
- Delete: 10 Java files in `code/poker/src/main/java/com/donohoedigital/games/poker/`

**Step 1: Read the gamedef.xml phase section**

Read lines 700–810 of `gamedef.xml` to understand the full scope of what needs removing.

**Step 2: Remove dead phase entries from gamedef.xml**

Delete the following `<phase>` elements (and any `<param>` entries that reference them):
- Line ~716: `<param name="deal.phase" strvalue="TD.DealButton"/>` — remove this param if it's inside a dead parent phase
- `<phase name="TD.ButtonDisplay" ...>`
- `<phase name="TD.WaitForDeal" ...>`
- `<phase name="TD.DealButton" ...>`
- `<phase name="TD.Bet" ...>`
- `<phase name="TD.PreShowdown" ...>`
- `<phase name="TD.CheckEndHand" ...>`
- `<phase name="TD.DisplayTableMoves" ...>`
- `<phase name="TD.NewLevelActions" ...>`
- `<phase name="TD.ColorUp" ...>` (includes the `next-phase` param pointing to ColorUpFinish)
- `<phase name="TD.ColorUpFinish" ...>`

**Step 3: Delete the Java phase files**

```bash
cd code/poker/src/main/java/com/donohoedigital/games/poker
git rm Bet.java ButtonDisplay.java WaitForDeal.java DealButton.java \
       PreShowdown.java CheckEndHand.java DisplayTableMoves.java \
       NewLevelActions.java ColorUp.java ColorUpFinish.java
```

**Step 4: Compile to catch any missed references**

```bash
cd code
mvn compile -pl poker -am -P dev 2>&1 | grep -E "ERROR|error:|cannot find"
```
Expected: No compilation errors. If any class is referenced somewhere not found by the grep audit, the error output will show exactly where.

Fix any compilation errors before proceeding. If a reference is in test code only, the test itself is also dead (remove it).

**Step 5: Run full test suite**

```bash
cd code
mvn test -P dev
```
Expected: BUILD SUCCESS.

**Step 6: Commit**

```bash
git add -A
git commit -m "refactor: delete 10 dead client-side ChainPhase classes and gamedef.xml entries"
```

---

### Task 9: Merge Branch 2

```bash
git checkout main
git merge --no-ff cleanup/dead-code -m "refactor: remove gameserver module and dead phase chain code"
```

---

## BRANCH 3: Code Quality

Branch off main: `git checkout -b cleanup/code-quality`

---

### Task 10: Migrate pokerengine tests from JUnit 4 to JUnit 5

**Files:**
- Modify: All `.java` files in `code/pokerengine/src/test/` that import `org.junit.Test` (not `org.junit.jupiter`)
- Modify: `code/pokerengine/pom.xml`

**Step 1: Find exactly which files need migration**

```bash
grep -rl "import org.junit.Test;" \
     code/pokerengine/src/test --include="*.java"
```
Note: This is `import org.junit.Test;` (JUnit 4), NOT `import org.junit.jupiter.api.Test;` (JUnit 5). The output is your migration list.

**Step 2: For each file, apply these mechanical substitutions**

The transformation rules are:
| JUnit 4 | JUnit 5 |
|---------|---------|
| `import org.junit.Test;` | `import org.junit.jupiter.api.Test;` |
| `import org.junit.Before;` | `import org.junit.jupiter.api.BeforeEach;` |
| `import org.junit.After;` | `import org.junit.jupiter.api.AfterEach;` |
| `import org.junit.BeforeClass;` | `import org.junit.jupiter.api.BeforeAll;` |
| `import org.junit.AfterClass;` | `import org.junit.jupiter.api.AfterAll;` |
| `import org.junit.Ignore;` | `import org.junit.jupiter.api.Disabled;` |
| `import static org.junit.Assert.*;` | `import static org.junit.jupiter.api.Assertions.*;` |
| `@Before` (method annotation) | `@BeforeEach` |
| `@After` (method annotation) | `@AfterEach` |
| `@BeforeClass` | `@BeforeAll` |
| `@AfterClass` | `@AfterAll` |
| `@Ignore` | `@Disabled` |

Also: JUnit 5 `Assertions` methods take `(expected, actual)` in same order as JUnit 4, so `assertEquals(expected, actual)` is unchanged. **Do not** change assertion argument order — it will silently break tests.

**Step 3: Check for expected-exception pattern**

JUnit 4 uses `@Test(expected = SomeException.class)`. JUnit 5 requires:
```java
@Test
void myTest() {
    assertThrows(SomeException.class, () -> {
        // code that should throw
    });
}
```
Search for this pattern:
```bash
grep -n "@Test(expected" code/pokerengine/src/test -r --include="*.java"
```
Convert any hits to `assertThrows`.

**Step 4: Remove JUnit 4 and vintage-engine from pokerengine/pom.xml**

In `code/pokerengine/pom.xml`, delete:
- The `junit:junit` dependency block
- The `junit-vintage-engine` dependency block

JUnit 5 (`org.junit.jupiter:junit-jupiter`) should already be present — verify.

**Step 5: Run the pokerengine tests**

```bash
cd code
mvn test -pl pokerengine -P dev
```
Expected: BUILD SUCCESS, all tests green. If any fail, the migration has a bug — fix it. Do not skip or `@Disabled` tests that were passing before.

**Step 6: Check other modules for orphaned junit:junit deps**

```bash
grep -rl "junit:junit" code --include="pom.xml" | grep -v "target"
```
For any module that already has all tests using JUnit 5, remove `junit:junit` from that POM too.

**Step 7: Commit**

```bash
cd code
git add pokerengine/
git commit -m "test: migrate pokerengine tests from JUnit 4 to JUnit 5, remove vintage-engine"
```

---

### Task 11: StringBuffer → StringBuilder

**Files:**
- Modify: `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/ai/AIOutcome.java`
- Modify: `code/gamecommon/src/main/java/com/donohoedigital/games/config/Hide.java`
- Modify: `code/gamecommon/src/main/java/com/donohoedigital/games/config/GameState.java`
- Modify: `code/gametools/src/main/java/com/donohoedigital/games/tools/Unhide.java`
- Modify: `code/common/src/main/java/com/donohoedigital/base/ZipUtil.java`
- Modify: `code/common/src/test/java/com/donohoedigital/base/ZipUtilTest.java`
- **Skip:** `code/server/src/main/java/com/donohoedigital/server/GameServletRequest.java` — `getRequestURL()` implements `HttpServletRequest` which specifies `StringBuffer` return type. Do not change it.
- **Skip:** `code/poker/src/main/java/com/zookitec/layout/ExplicitConstraints.java` — third-party code.

**Step 1: Read each file and locate StringBuffer usage**

For each file listed above, read it and find every `StringBuffer` reference.

**Step 2: Replace StringBuffer with StringBuilder**

In non-synchronized contexts, `StringBuffer` → `StringBuilder` is safe. Rules:
- Local variables: change `new StringBuffer()` → `new StringBuilder()`
- Parameters and return types: change only if all callers are also being updated in this task
- `Hide.java` exports `obfuscate(StringBuffer)` / `deobfuscate(StringBuffer)` — change signatures to `StringBuilder` AND update callers in `GameState.java` and `Unhide.java` in the same commit
- Remove `@SuppressWarnings("StringConcatenationInsideStringBufferAppend")` from `AIOutcome.java` after changing to `StringBuilder`

**Step 3: Compile**

```bash
cd code
mvn compile -P dev -q
```
Expected: BUILD SUCCESS.

**Step 4: Run tests**

```bash
cd code
mvn test -P dev
```
Expected: BUILD SUCCESS.

**Step 5: Commit**

```bash
git add pokergamecore/src/main/java/com/donohoedigital/games/poker/core/ai/AIOutcome.java \
        gamecommon/src/main/java/com/donohoedigital/games/config/Hide.java \
        gamecommon/src/main/java/com/donohoedigital/games/config/GameState.java \
        gametools/src/main/java/com/donohoedigital/games/tools/Unhide.java \
        common/src/main/java/com/donohoedigital/base/ZipUtil.java \
        common/src/test/java/com/donohoedigital/base/ZipUtilTest.java
git commit -m "refactor: replace StringBuffer with StringBuilder in non-synchronized code"
```

---

### Task 12: Replace System.out.println with proper logging

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerStats.java`

**Step 1: Read both files**

Find every `System.out.println` or `System.err.println` call. Both files should already have a `Logger logger = LogManager.getLogger(...)` field.

**Step 2: Replace in PokerMain.java**

3 instances. Use the appropriate log level:
- Startup information → `logger.info(...)`
- Errors → `logger.error(...)`

**Step 3: Replace in PokerStats.java**

2 instances:
- Line 141: `System.out.println(stat.toString())` → `logger.info("{}", stat)`
- Line 289: `System.out.println("PokerStats [loops] ...")` — this appears to be usage/help text. Use `logger.info(...)`.

**Step 4: Compile and run tests**

```bash
cd code
mvn test -pl poker -P dev
```
Expected: BUILD SUCCESS.

**Step 5: Commit**

```bash
git add poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java \
        poker/src/main/java/com/donohoedigital/games/poker/PokerStats.java
git commit -m "refactor: replace System.out.println with Log4j2 logger in PokerMain and PokerStats"
```

---

### Task 13: TODO/FIXME comment audit and cleanup

**Files:** Multiple — guided by grep output

**Step 1: Get the full TODO list**

```bash
cd code
grep -rn "TODO\|FIXME\|HACK\|XXX" --include="*.java" \
     --exclude-dir=target | grep -v "test.*TODO\|TODO.*test" | sort
```

**Step 2: Categorize and act on each**

Apply these rules to each hit:

**Remove the comment** (comment is stale observation, no action needed):
- `PokerRankUpdater.java:111` — `// TODO: make sure debug off (next line)` → either the debug is already off (delete comment) or turn it off and delete the TODO
- `gametools/*.java` — war-aoi specific comments like `//war-aoi specific (TODO: config)` → replace with `// not used for poker` or delete

**Fix and remove** (trivially fixable now):
- `gui/OptionCombo.java:117` — `// TODO: this doesn't currently do anything` — read what "this" refers to. If it's genuinely a no-op, remove the entire method or leave a clear `// intentionally empty` comment instead of TODO.
- `gametools/TerritoryPointChooser.java:150` — `// TODO - fix InternalDialog.POSITION_NOT_OBSCURED` — check if this position constant exists now. If it does, use it; if not, leave a clear comment about why not.

**Convert to `// Future:` prefix** (legitimate future work, not blocking release):
- `pokergamecore/ai/PureRuleEngine.java` — 7 AI strategy TODOs. Change `// TODO:` → `// Future:` to signal intentional deferral.
- `pokergamecore/ai/V2Algorithm.java` — 2 reputation TODOs. Same treatment.
- `pokerengine/model/TournamentProfile.java` — localization TODO. Change to `// Future:`.
- `gui/DDButtonUI.java:314` — icon scaling TODO. Change to `// Future:`.
- `gui/InternalDialog.java` — 2 TODOs. Assess: if they're about JProfile tooling, delete; if about real bugs, convert to `// Future:`.
- `server/ServletDebug.java:155` — multi-line URL TODO. Convert to `// Future:`.

**Test stubs** — if a test class has a `TODO: Implement test` body, either implement a minimal test or delete the stub method entirely:
- `poker/ShowTournamentTableFocusTest.java:54` — `// TODO: Implement test for isFocusInChat()` — read the test. If the test method body is empty, delete the method. Don't leave untested stubs.

**Step 3: Run tests after each file touched**

```bash
cd code
mvn test -P dev
```

**Step 4: Commit**

```bash
git add -A
git commit -m "chore: resolve TODO/FIXME comments — fix trivial ones, convert AI strategy to Future:"
```

---

### Task 14: Full test run and merge Branch 3

**Step 1: Run full test suite including coverage**

```bash
cd code
mvn verify -P coverage
```
Expected: BUILD SUCCESS, all module JaCoCo thresholds met.

**Step 2: Verify zero HSQLDB references remain**

```bash
grep -r "hsqldb\|HSQL" code --include="*.java" --include="*.xml" \
     --include="*.properties" --exclude-dir=target
```
Expected: zero hits.

**Step 3: Verify zero JUnit 4 imports remain in pokerengine**

```bash
grep -r "import org.junit.Test;" code/pokerengine/src/test --include="*.java"
```
Expected: zero hits.

**Step 4: Verify zero System.out in production Java source**

```bash
grep -rn "System\.out\." code --include="*.java" --exclude-dir=target \
     --exclude-dir=test | grep -v "//\|tools/\|gametools/"
```
Expected: zero hits in non-tool production code.

**Step 5: Merge to main**

```bash
git checkout main
git merge --no-ff cleanup/code-quality -m "refactor: JUnit 5 migration, StringBuilder, logging, TODO cleanup"
```

---

## Final Verification Checklist

After all three branches are merged:

- [ ] `mvn test -P dev` passes from `code/`
- [ ] `mvn verify -P coverage` passes (coverage thresholds met)
- [ ] Zero `hsqldb` references: `grep -r "hsqldb" code --include="*.java" --include="*.xml" --exclude-dir=target`
- [ ] Zero JUnit 4 imports: `grep -r "import org.junit.Test;" code --include="*.java" --exclude-dir=target`
- [ ] Zero `System.out.println` in production code (outside `tools/` batch utilities)
- [ ] Zero `StringBuffer` in non-interface, non-third-party code
- [ ] `gameserver` module directory deleted and removed from parent pom.xml
- [ ] All 10 dead phase Java files deleted
- [ ] All dead `<phase>` entries removed from `gamedef.xml`
- [ ] No TODO/FIXME comments (only `// Future:` for intentional deferrals)
