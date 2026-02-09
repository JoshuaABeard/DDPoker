# Remove Legacy Licensing and Activation System

## Context

DD Poker was originally a commercial product (2003-2017) with a licensing system that used retail activation keys, key validation, banned key tracking, and activation dialogs. The product is now fully open source under GPL-3.0, making this licensing infrastructure obsolete and confusing for users.

**Why this change is needed:**
- **Open source** - GPL-3.0 license means no activation keys needed
- **User confusion** - Activation dialogs and key validation are misleading
- **Code complexity** - 1000+ lines of licensing code serving no purpose
- **Maintenance burden** - Dead code that needs to be understood and maintained
- **Poor UX** - Users shouldn't see activation prompts in open source software

**Current state:**
- Legacy retail key validation (FFNN-NNNN-NNAA-AAAA format with SHA hash)
- GUID-based keys (KEY-nn-UUID format)
- Demo mode keys ("DEMO-GUID")
- Banned key tracking
- Activation UI dialogs
- License key stored in Java Preferences and database
- **Player UUID system** - Already in place for online profiles (should keep)

**Desired state:**
- **Remove all licensing/activation code completely**
- **Keep UUID system** - Use proper UUID v4 format for player identification
- Generate UUID automatically on first run (no user input)
- Use UUID as player identifier in online games
- Remove activation UI dialogs
- Clean up database schema (remove license key columns)
- No Java Preferences storage for keys

## Implementation Approach

### 1. UUID System (Keep and Improve)

**Current Implementation:**
- `OnlineProfile.getUuid()` - Profile UUID stored in database
- Used as session key for online game communication
- Manually assigned in some cases

**New Implementation:**
- **Auto-generate UUID v4** on first run or profile creation
- Store in local config file: `%APPDATA%\ddpoker\player.json`
- Use as player identifier everywhere (replace key references)
- Database `wpr_uuid` column becomes primary identifier

**Player Config File Structure:**
```json
{
  "playerId": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "Player1",
  "createdAt": "2026-02-09T12:00:00Z"
}
```

**UUID Generation:**
```java
import java.util.UUID;

public class PlayerIdentity {
    public static String generatePlayerId() {
        return UUID.randomUUID().toString();
    }
}
```

### 2. Files to Delete Completely

These files serve no purpose in open source version:

1. **`code/common/src/main/java/com/donohoedigital/config/Activation.java`**
   - Entire file (320 lines) - key validation logic

2. **`code/common/src/test/java/com/donohoedigital/config/ActivationTest.java`**
   - Unit tests for removed code

3. **`code/gameengine/src/main/java/com/donohoedigital/games/engine/Activate.java`**
   - Activation dialog UI (400+ lines)

4. **`code/gameengine/src/main/java/com/donohoedigital/games/engine/License.java`**
   - License display dialog

5. **`code/gameengine/src/main/java/com/donohoedigital/games/engine/VerifyActivationKey.java`**
   - Key verification dialog

6. **`code/gameserver/src/main/java/com/donohoedigital/games/server/model/Registration.java`**
   - Registration tracking (if only used for licensing)

### 3. Major Code Modifications

#### A. GameEngine.java - Remove License Management

**File:** `code/gameengine/src/main/java/com/donohoedigital/games/engine/GameEngine.java`

**Remove these methods (lines 489-748):**
- `getRealLicenseKey()`
- `getPublicUseKey()`
- `getDemoLicenseKey()`
- `getHeadlessLicenseKey()`
- `setLicenseKey(String sKey)`
- `resetLicenseKey()`
- `banLicenseKey()`
- `isBannedLicenseKey(String sKey)`
- `addBannedLicenseKey(String sKey)`
- `getLastLicenseKey()`
- `setLastLicenseKey(String sKey)`
- `isActivationNeeded()`
- `keyValidated(boolean bPatch)`

**Replace with:**
```java
private String playerId;

public String getPlayerId() {
    if (playerId == null) {
        playerId = PlayerIdentity.loadOrCreate();
    }
    return playerId;
}

public void setPlayerId(String id) {
    this.playerId = id;
    PlayerIdentity.save(id);
}
```

**Remove constants:**
- All `Activation.REGKEY`, `OLDKEY`, `BANKEY`, `DEMOKEY` references
- Hardcoded banned keys (lines 673-675)

#### B. PokerPlayer.java - Replace Key with UUID

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java`

**Change:**
```java
// OLD
private String sKey_;  // Line 88

public PokerPlayer(String sKey, int id, String sName, boolean bHuman)
public String getKey()
public void setKey(String sKey)
```

**To:**
```java
// NEW
private String playerId_;

public PokerPlayer(String playerId, int id, String sName, boolean bHuman)
public String getPlayerId()
public void setPlayerId(String playerId)
```

**Update all constructors and usages** (23 files reference PokerPlayer with key)

#### C. OnlineProfile.java - Remove License Fields

**File:** `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/OnlineProfile.java`

**Remove fields:**
```java
// DELETE
PROFILE_LICENSE_KEY = "profilelicensekey"
PROFILE_ACTIVATED = "profileactivated"

// Methods to remove:
getLicenseKey() / setLicenseKey(String s)
isActivated() / setActivated(boolean b)
```

**Keep:**
```java
// KEEP - this is the new primary identifier
PROFILE_UUID = "profileuuid"
getUuid() / setUuid(String s)
```

**Database migration:**
```sql
-- Remove old columns
ALTER TABLE online_profile DROP COLUMN IF EXISTS wpr_license_key;
ALTER TABLE online_profile DROP COLUMN IF EXISTS wpr_is_activated;

-- Ensure UUID is unique and not null
ALTER TABLE online_profile MODIFY COLUMN wpr_uuid VARCHAR(36) NOT NULL UNIQUE;
```

#### D. PokerGame.java - Update Player Creation

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java`

**Line 1104 - AI player creation:**
```java
// OLD
PokerPlayer ai = new PokerPlayer(getPublicUseKey(), i, aiProfile, false);

// NEW
PokerPlayer ai = new PokerPlayer(engine_.getPlayerId(), i, aiProfile, false);
```

#### E. ValidateProfile.java - UUID as Session Key

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/ValidateProfile.java`

**Lines 107-132 - Already using UUID, just clean up:**
```java
// This is GOOD - already uses UUID
String uuid = validatedProfile.getUuid();
DDMessage.setDefaultRealKey(uuid);
DDMessage.setDefaultKey(uuid);

// Just ensure no license key references remain
```

#### F. PokerServlet.java - Server-Side Validation

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServlet.java`

**Remove:**
- Any license key validation logic
- Key checking on server side

**Keep:**
- UUID validation (ensure it's a valid UUID format)
- Profile authentication via credentials

### 4. UI Changes - Remove Activation Dialogs

**Files to update:**
- Remove menu items/buttons that launch activation dialogs
- Remove "Register" or "Activate" options
- Remove license key display in About dialog
- Remove activation status indicators

**Menu items to remove:**
- "Help → Activate" or similar
- Any registration/licensing menu items

### 5. Java Preferences Cleanup

**Remove these preference nodes:**
```java
// Delete from GameEngine
Preferences prefs = Prefs.getUserPrefs(Prefs.NODE_OPTIONS + getPrefsNodeName() + "/key/" + appName_ + "-" + keyVersion_);

// Remove these keys:
prefs.remove(Activation.REGKEY);
prefs.remove(Activation.OLDKEY);
prefs.remove(Activation.BANKEY);
prefs.remove(Activation.DEMOKEY);
```

**Migration:**
Users upgrading will lose their old "license key" but that's fine - it's meaningless now. UUID will be auto-generated fresh.

### 6. DDMessage Key/Session Management

**File:** `code/common/src/main/java/com/donohoedigital/comms/DDMessage.java`

**Keep these methods** (used for session management):
```java
setDefaultKey(String key)      // Session UUID
getDefaultKey()
setDefaultRealKey(String key)  // Real UUID (not public)
```

**Rename for clarity:**
```java
// Consider renaming to avoid "key" terminology:
setDefaultSessionId(String sessionId)
getDefaultSessionId()
```

### 7. Property Files and Configuration

**Search and update:**
- `*.properties` files with activation/license text
- Help files mentioning activation
- Error messages about invalid keys
- Tooltips referencing license keys

**Files to check:**
```
code/poker/src/main/resources/config/poker/client.properties
code/poker/src/main/resources/config/poker/help/*.html
code/gameengine/src/main/resources/config/engine/*.properties
```

### 8. Player Identity Management (New Class)

**Create:** `code/common/src/main/java/com/donohoedigital/config/PlayerIdentity.java`

```java
package com.donohoedigital.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * Manages player identity using UUID v4.
 * Replaces legacy license key system.
 */
public class PlayerIdentity {
    private static final String PLAYER_FILE = "player.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Load existing player ID or create new one.
     */
    public static String loadOrCreate() {
        File playerFile = getPlayerFile();

        if (playerFile.exists()) {
            try {
                Map<String, Object> data = mapper.readValue(playerFile, Map.class);
                String playerId = (String) data.get("playerId");
                if (playerId != null && isValidUUID(playerId)) {
                    return playerId;
                }
            } catch (IOException e) {
                // File corrupted, generate new ID
            }
        }

        // Generate new UUID
        String newId = UUID.randomUUID().toString();
        save(newId);
        return newId;
    }

    /**
     * Save player ID to disk.
     */
    public static void save(String playerId) {
        try {
            File playerFile = getPlayerFile();
            playerFile.getParentFile().mkdirs();

            Map<String, Object> data = new HashMap<>();
            data.put("playerId", playerId);
            data.put("createdAt", System.currentTimeMillis());

            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(playerFile, data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save player ID", e);
        }
    }

    /**
     * Get player config file location.
     */
    private static File getPlayerFile() {
        String configDir = getConfigDirectory();
        return new File(configDir, PLAYER_FILE);
    }

    /**
     * Platform-specific config directory.
     */
    private static String getConfigDirectory() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return System.getenv("APPDATA") + File.separator + "ddpoker";
        } else if (os.contains("mac")) {
            return System.getProperty("user.home") + "/Library/Application Support/ddpoker";
        } else {
            return System.getProperty("user.home") + "/.ddpoker";
        }
    }

    /**
     * Validate UUID format.
     */
    private static boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

## Critical Files Summary

### Files to Delete (6 files):
1. `code/common/src/main/java/com/donohoedigital/config/Activation.java`
2. `code/common/src/test/java/com/donohoedigital/config/ActivationTest.java`
3. `code/gameengine/src/main/java/com/donohoedigital/games/engine/Activate.java`
4. `code/gameengine/src/main/java/com/donohoedigital/games/engine/License.java`
5. `code/gameengine/src/main/java/com/donohoedigital/games/engine/VerifyActivationKey.java`
6. `code/gameserver/src/main/java/com/donohoedigital/games/server/model/Registration.java` (if only used for licensing)

### Files to Create (1 file):
1. `code/common/src/main/java/com/donohoedigital/config/PlayerIdentity.java` - New UUID management

### Files to Heavily Modify (10+ files):
1. `code/gameengine/src/main/java/com/donohoedigital/games/engine/GameEngine.java` - Remove 260+ lines (methods 489-748)
2. `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java` - Rename sKey_ to playerId_
3. `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/OnlineProfile.java` - Remove license fields
4. `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java` - Update AI player creation
5. `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineManager.java` - Update player creation (2 places)
6. `code/poker/src/main/java/com/donohoedigital/games/poker/online/ListGames.java` - Update temp player creation
7. `code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineLobby.java` - Update player creation
8. `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServlet.java` - Remove key validation
9. Plus 15+ other files with key/license references

### Database Migration:
- H2 SQL script to remove `wpr_license_key` and `wpr_is_activated` columns
- Ensure `wpr_uuid` is NOT NULL and UNIQUE

## Verification Steps

### 1. Code Compilation
```bash
mvn clean compile -f code/pom.xml
```
**Expected:** No compilation errors after refactoring

### 2. Grep for Removed Concepts
```bash
# Should return 0 results after cleanup:
grep -r "Activation\." code/
grep -r "license.*key" code/ -i
grep -r "activate" code/ -i
grep -r "REGKEY\|OLDKEY\|BANKEY\|DEMOKEY" code/
grep -r "isBannedLicenseKey" code/
grep -r "keyValidated" code/
```

### 3. Player Identity Creation
```bash
# Run application
java -jar DDPoker.jar

# Verify player.json created:
# Windows: dir "%APPDATA%\ddpoker\player.json"
# macOS: ls ~/Library/Application\ Support/ddpoker/player.json
# Linux: ls ~/.ddpoker/player.json
```

**Expected JSON content:**
```json
{
  "playerId": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": 1707523200000
}
```

### 4. Online Game Flow
1. **Create Profile** - No license key required
2. **Login** - Authenticate with credentials only
3. **Join Game** - UUID used as player identifier
4. **Play Round** - No activation checks during gameplay

**Expected:** Seamless experience with no activation prompts

### 5. Database Verification
```sql
-- Check schema
DESCRIBE online_profile;

-- Should NOT have:
-- wpr_license_key
-- wpr_is_activated

-- Should have:
-- wpr_uuid VARCHAR(36) NOT NULL UNIQUE
```

### 6. UI Verification
- [ ] No "Activate" menu items
- [ ] No "Register" dialogs
- [ ] No license key display in About screen
- [ ] No activation prompts on startup
- [ ] No "invalid key" error messages

### 7. Regression Testing
- [ ] Single player practice works
- [ ] AI opponents function correctly
- [ ] Online multiplayer works (UUID-based)
- [ ] Profile creation/login works
- [ ] Settings persist across sessions
- [ ] No functionality broken by license removal

## Expected Outcomes

### 1. **Clean Open Source Experience**
- No misleading activation dialogs
- No license key confusion
- Professional open source UX
- Welcoming to new users

### 2. **Simplified Codebase**
- ~1500+ lines of code removed
- No legacy retail logic
- Clear UUID-based identification
- Easier to understand and maintain

### 3. **Modern Player Identity**
- UUID v4 standard (industry best practice)
- Auto-generated on first run
- Portable (stored in config file)
- No user friction

### 4. **Database Cleanup**
- Removed unused columns
- UUID as primary identifier
- Cleaner schema
- No legacy artifacts

## Risks and Mitigation

### Risk 1: Breaking Online Multiplayer
**Mitigation:**
- UUID system already in place and working
- ValidateProfile.java already uses UUID as session key
- Just removing parallel license key system
- Test online games thoroughly after changes

### Risk 2: Lost Player Identity
**Mitigation:**
- Old users will get new UUID (acceptable for open source transition)
- No critical data tied to old license keys
- Profile credentials still work (username/password)
- UUID stored in accessible config file (user can backup)

### Risk 3: Server Compatibility
**Mitigation:**
- Server already handles UUID-based sessions
- Remove server-side license validation carefully
- Test client-server communication after changes
- Ensure backwards compatibility if needed

### Risk 4: Missed References
**Mitigation:**
- Comprehensive grep search performed
- 29+ files identified with key references
- Compile after each major change
- Thorough testing of all game modes

## TDD Approach

This feature will be developed using **Test-Driven Development (TDD)** with modern testing frameworks.

### Testing Framework: JUnit 5 (Jupiter)

**Why JUnit 5:**
- Modern annotations (`@Test`, `@BeforeEach`, `@AfterEach`, `@TempDir`)
- Better parameterized testing for cross-platform scenarios (`@ParameterizedTest`)
- Improved assertions and test organization
- Compatible with existing JUnit 4 tests (can coexist in same project)

**Additional Testing Tools:**
- **AssertJ** - Fluent assertions for better readability
- **Mockito** - Mocking for GameEngine and legacy code isolation
- **@TempDir** - Clean test isolation for file operations
- Project already has Jackson for JSON

**Add to `code/common/pom.xml`:**
```xml
<!-- JUnit 5 (Jupiter) -->
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

<!-- Mockito for mocking -->
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

### TDD Cycle for Each Feature

Follow **Red-Green-Refactor** for each capability:

1. **🔴 RED** - Write a failing test that defines desired behavior
2. **🟢 GREEN** - Write minimal code to make the test pass
3. **🔵 REFACTOR** - Clean up code while keeping tests green
4. **♻️ REPEAT** - Next test for next behavior

### Test-First Development Order

**Cycle 1: PlayerIdentity - UUID Generation**
```java
@Test
void should_GenerateValidUUIDv4_When_CreateCalled() {
    // RED: Test fails (PlayerIdentity doesn't exist yet)
    String playerId = PlayerIdentity.generatePlayerId();

    // GREEN: Implement generatePlayerId() using UUID.randomUUID()
    assertThat(playerId).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    // REFACTOR: Verify UUID v4 format specifically
}

@Test
void should_GenerateUniqueIds_When_CalledMultipleTimes() {
    // Ensure no collisions
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
        ids.add(PlayerIdentity.generatePlayerId());
    }
    assertThat(ids).hasSize(1000);
}
```

**Cycle 2: PlayerIdentity - Platform Detection**
```java
@ParameterizedTest
@ValueSource(strings = {"windows 10", "windows 11", "windows server 2022"})
void should_DetectWindowsConfigDirectory_When_RunningOnWindows(String osName) {
    // RED: Test fails (getConfigDirectory() not implemented)
    // Mock system property
    System.setProperty("os.name", osName);

    // GREEN: Implement getConfigDirectory() for Windows
    String configDir = PlayerIdentity.getConfigDirectory();

    // REFACTOR: Verify correct APPDATA path
    assertThat(configDir).contains("\\ddpoker");
    assertThat(configDir).doesNotStartWith(".");
}

@Test
void should_UseMacOSPath_When_RunningOnMacOS() {
    System.setProperty("os.name", "Mac OS X");

    String configDir = PlayerIdentity.getConfigDirectory();

    assertThat(configDir).contains("/Library/Application Support/ddpoker");
}

@Test
void should_UseLinuxHiddenPath_When_RunningOnLinux() {
    System.setProperty("os.name", "Linux");

    String configDir = PlayerIdentity.getConfigDirectory();

    assertThat(configDir).endsWith("/.ddpoker");
}
```

**Cycle 3: PlayerIdentity - Save/Load**
```java
@Test
void should_SavePlayerIdToFile_When_SaveCalled(@TempDir Path tempDir) {
    // RED: Test fails (save() not implemented)
    // Override config directory for testing
    PlayerIdentity.setConfigDirectory(tempDir.toString());

    String playerId = "550e8400-e29b-41d4-a716-446655440000";

    // GREEN: Implement save() with Jackson
    PlayerIdentity.save(playerId);

    // REFACTOR: Verify file exists and contains correct JSON
    Path playerFile = tempDir.resolve("player.json");
    assertThat(playerFile).exists();

    String content = Files.readString(playerFile);
    assertThat(content).contains("\"playerId\" : \"550e8400-e29b-41d4-a716-446655440000\"");
}

@Test
void should_LoadPlayerIdFromFile_When_FileExists(@TempDir Path tempDir) {
    // Setup: Create player.json
    PlayerIdentity.setConfigDirectory(tempDir.toString());
    String originalId = "550e8400-e29b-41d4-a716-446655440000";
    PlayerIdentity.save(originalId);

    // Test load
    String loadedId = PlayerIdentity.loadOrCreate();

    assertThat(loadedId).isEqualTo(originalId);
}

@Test
void should_GenerateNewId_When_FileDoesNotExist(@TempDir Path tempDir) {
    PlayerIdentity.setConfigDirectory(tempDir.toString());

    String playerId = PlayerIdentity.loadOrCreate();

    assertThat(playerId).isNotNull();
    assertThat(playerId).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    // Verify file was created
    Path playerFile = tempDir.resolve("player.json");
    assertThat(playerFile).exists();
}

@Test
void should_GenerateNewId_When_FileCorrupted(@TempDir Path tempDir) {
    // Setup: Create corrupted player.json
    PlayerIdentity.setConfigDirectory(tempDir.toString());
    Path playerFile = tempDir.resolve("player.json");
    Files.createDirectories(playerFile.getParent());
    Files.writeString(playerFile, "invalid json {{{");

    // Test recovery
    String playerId = PlayerIdentity.loadOrCreate();

    assertThat(playerId).isNotNull();
    assertThat(playerId).matches("[0-9a-f]{8}-.*");
}
```

**Cycle 4: GameEngine - Player ID Management**
```java
@Test
void should_GetPlayerId_When_Called() {
    // RED: Test fails (getPlayerId() not implemented)
    GameEngine engine = new GameEngine("test", "poker");

    // GREEN: Implement getPlayerId()
    String playerId = engine.getPlayerId();

    // REFACTOR: Verify valid UUID returned
    assertThat(playerId).isNotNull();
    assertThat(playerId).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
}

@Test
void should_PersistPlayerId_When_SetPlayerIdCalled(@TempDir Path tempDir) {
    PlayerIdentity.setConfigDirectory(tempDir.toString());
    GameEngine engine = new GameEngine("test", "poker");

    String newId = "550e8400-e29b-41d4-a716-446655440000";
    engine.setPlayerId(newId);

    // Verify saved to file
    String loadedId = PlayerIdentity.loadOrCreate();
    assertThat(loadedId).isEqualTo(newId);
}

@Test
void should_NotHaveLicenseKeyMethods_When_RefactoringComplete() {
    // Verify old methods removed
    GameEngine engine = new GameEngine("test", "poker");

    assertThatThrownBy(() -> {
        Method method = GameEngine.class.getMethod("getRealLicenseKey");
        method.invoke(engine);
    }).isInstanceOf(NoSuchMethodException.class);

    // Verify more removed methods
    assertThatThrownBy(() -> GameEngine.class.getMethod("setLicenseKey", String.class))
        .isInstanceOf(NoSuchMethodException.class);
    assertThatThrownBy(() -> GameEngine.class.getMethod("isActivationNeeded"))
        .isInstanceOf(NoSuchMethodException.class);
}
```

**Cycle 5: PokerPlayer - Player ID Field**
```java
@Test
void should_UsePlayerId_When_ConstructorCalled() {
    // RED: Test fails (playerId parameter doesn't exist)
    String playerId = "550e8400-e29b-41d4-a716-446655440000";

    // GREEN: Change constructor from (String sKey, ...) to (String playerId, ...)
    PokerPlayer player = new PokerPlayer(playerId, 1, "TestPlayer", true);

    // REFACTOR: Verify playerId stored
    assertThat(player.getPlayerId()).isEqualTo(playerId);
}

@Test
void should_NotHaveKeyMethods_When_RefactoringComplete() {
    // Verify old methods removed
    assertThatThrownBy(() -> PokerPlayer.class.getMethod("getKey"))
        .isInstanceOf(NoSuchMethodException.class);
    assertThatThrownBy(() -> PokerPlayer.class.getMethod("setKey", String.class))
        .isInstanceOf(NoSuchMethodException.class);
}

@Test
void should_HavePlayerIdMethods_When_RefactoringComplete() {
    // Verify new methods exist
    assertThatCode(() -> {
        Method getMethod = PokerPlayer.class.getMethod("getPlayerId");
        Method setMethod = PokerPlayer.class.getMethod("setPlayerId", String.class);
    }).doesNotThrowAnyException();
}
```

**Cycle 6: OnlineProfile - Remove License Fields**
```java
@Test
void should_NotHaveLicenseKeyField_When_RefactoringComplete() {
    OnlineProfile profile = new OnlineProfile();

    // Verify field removed
    assertThatThrownBy(() -> {
        Method method = OnlineProfile.class.getMethod("getLicenseKey");
        method.invoke(profile);
    }).isInstanceOf(NoSuchMethodException.class);
}

@Test
void should_HaveUUIDField_When_RefactoringComplete() {
    OnlineProfile profile = new OnlineProfile();

    String uuid = "550e8400-e29b-41d4-a716-446655440000";
    profile.setUuid(uuid);

    assertThat(profile.getUuid()).isEqualTo(uuid);
}

@Test
void should_NotHaveActivatedField_When_RefactoringComplete() {
    // Verify isActivated() and setActivated() removed
    assertThatThrownBy(() -> OnlineProfile.class.getMethod("isActivated"))
        .isInstanceOf(NoSuchMethodException.class);
}
```

**Cycle 7: Database Migration**
```java
@Test
void should_RemoveLicenseKeyColumn_When_MigrationRun() {
    // RED: Test fails (migration not created)
    // Setup in-memory H2 database
    Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");

    // Create old schema with license columns
    Statement stmt = conn.createStatement();
    stmt.execute("CREATE TABLE online_profile (" +
                 "wpr_id INT PRIMARY KEY, " +
                 "wpr_license_key VARCHAR(50), " +
                 "wpr_is_activated BOOLEAN, " +
                 "wpr_uuid VARCHAR(36))");

    // GREEN: Run migration script
    runMigrationScript(conn, "migration-remove-licensing.sql");

    // REFACTOR: Verify columns removed
    DatabaseMetaData meta = conn.getMetaData();
    ResultSet columns = meta.getColumns(null, null, "ONLINE_PROFILE", "WPR_LICENSE_KEY");
    assertThat(columns.next()).isFalse(); // Column should not exist

    columns = meta.getColumns(null, null, "ONLINE_PROFILE", "WPR_IS_ACTIVATED");
    assertThat(columns.next()).isFalse();
}

@Test
void should_MakeUUIDNotNull_When_MigrationRun() {
    Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
    setupOldSchema(conn);

    runMigrationScript(conn, "migration-remove-licensing.sql");

    // Verify UUID is NOT NULL
    ResultSet columns = conn.getMetaData().getColumns(null, null, "ONLINE_PROFILE", "WPR_UUID");
    assertThat(columns.next()).isTrue();
    assertThat(columns.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.columnNoNulls);
}

@Test
void should_EnforceUUIDUniqueness_When_MigrationRun() {
    Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
    setupOldSchema(conn);
    runMigrationScript(conn, "migration-remove-licensing.sql");

    Statement stmt = conn.createStatement();
    stmt.execute("INSERT INTO online_profile (wpr_id, wpr_uuid) VALUES (1, '550e8400-e29b-41d4-a716-446655440000')");

    // Try to insert duplicate UUID
    assertThatThrownBy(() -> {
        stmt.execute("INSERT INTO online_profile (wpr_id, wpr_uuid) VALUES (2, '550e8400-e29b-41d4-a716-446655440000')");
    }).hasMessageContaining("UNIQUE"); // Should fail on unique constraint
}
```

**Cycle 8: Integration Tests - Online Game Flow**
```java
@Test
void should_CreatePlayerWithUUID_When_JoiningOnlineGame() {
    // Integration test: Full flow from profile creation to game join

    // 1. Create profile (no license key)
    OnlineProfile profile = new OnlineProfile();
    profile.setName("TestPlayer");
    profile.setUuid(PlayerIdentity.generatePlayerId());

    // 2. Create PokerPlayer
    PokerPlayer player = new PokerPlayer(profile.getUuid(), 1, profile.getName(), true);

    // 3. Verify UUID used as identifier
    assertThat(player.getPlayerId()).isEqualTo(profile.getUuid());
    assertThat(player.getPlayerId()).matches("[0-9a-f]{8}-.*");
}

@Test
void should_UseUUIDForSessionKey_When_ValidatingProfile() {
    // Test ValidateProfile.java logic
    OnlineProfile profile = new OnlineProfile();
    profile.setUuid("550e8400-e29b-41d4-a716-446655440000");

    // Simulate validation
    String sessionKey = profile.getUuid();
    DDMessage.setDefaultKey(sessionKey);

    assertThat(DDMessage.getDefaultKey()).isEqualTo(profile.getUuid());
}
```

**Cycle 9: Code Cleanup Verification**
```java
@Test
void should_NotFindLicensingReferences_When_CleanupComplete() throws IOException {
    // Grep-style test to verify no licensing code remains
    Path codeDir = Paths.get("code");

    List<Path> javaFiles = Files.walk(codeDir)
        .filter(p -> p.toString().endsWith(".java"))
        .collect(Collectors.toList());

    List<String> violations = new ArrayList<>();

    for (Path file : javaFiles) {
        String content = Files.readString(file);

        // Check for forbidden patterns
        if (content.contains("Activation.REGKEY")) {
            violations.add(file + " contains Activation.REGKEY");
        }
        if (content.contains("getRealLicenseKey")) {
            violations.add(file + " contains getRealLicenseKey");
        }
        if (content.contains("isBannedLicenseKey")) {
            violations.add(file + " contains isBannedLicenseKey");
        }
        if (content.contains("keyValidated")) {
            violations.add(file + " contains keyValidated");
        }
    }

    assertThat(violations).isEmpty();
}

@Test
void should_NotHaveDeletedClasses_When_CleanupComplete() {
    // Verify deleted classes don't exist
    assertThatThrownBy(() -> Class.forName("com.donohoedigital.config.Activation"))
        .isInstanceOf(ClassNotFoundException.class);

    assertThatThrownBy(() -> Class.forName("com.donohoedigital.games.engine.Activate"))
        .isInstanceOf(ClassNotFoundException.class);

    assertThatThrownBy(() -> Class.forName("com.donohoedigital.games.engine.License"))
        .isInstanceOf(ClassNotFoundException.class);
}
```

### Test Organization

**PlayerIdentityTest.java** - Unit tests for UUID management
- UUID v4 generation and validation
- Platform-specific directory detection
- JSON save/load operations
- Corruption recovery

**GameEnginePlayerIdTest.java** - GameEngine refactoring tests
- Player ID getter/setter
- Verification of removed license methods
- Integration with PlayerIdentity

**PokerPlayerRefactoringTest.java** - PokerPlayer field rename tests
- Constructor parameter changes
- playerId getter/setter
- Verification of removed key methods

**OnlineProfileRefactoringTest.java** - OnlineProfile cleanup tests
- UUID field usage
- License field removal verification
- Database column mapping

**DatabaseMigrationTest.java** - Database schema tests
- License column removal
- UUID constraints (NOT NULL, UNIQUE)
- Migration script execution

**LicensingRemovalIntegrationTest.java** - End-to-end tests
- Online game flow with UUID
- Session management
- Player creation and authentication

**CodeCleanupVerificationTest.java** - Static analysis tests
- Grep-style checks for forbidden patterns
- Deleted class verification
- Import statement validation

### Test Naming Convention
```java
@Test
void should_GenerateValidUUID_When_PlayerIdentityCreated() { }

@Test
void should_RemoveLicenseKey_When_OnlineProfileRefactored() { }

@Test
void should_UsePlayerId_When_PokerPlayerConstructed() { }
```

### Mocking Strategy

**When to Mock:**
- GameEngine initialization (heavy Spring context)
- Database connections (use H2 in-memory for tests)
- File system operations (use @TempDir instead)

**Example with Mockito:**
```java
@ExtendWith(MockitoExtension.class)
class PokerGameRefactoringTest {

    @Mock
    private GameEngine mockEngine;

    @Test
    void should_UseEnginePlayerId_When_CreatingAIPlayer() {
        when(mockEngine.getPlayerId()).thenReturn("550e8400-e29b-41d4-a716-446655440000");

        PokerGame game = new PokerGame(mockEngine);
        PokerPlayer ai = game.createAIPlayer(1, "AI Player");

        verify(mockEngine).getPlayerId();
        assertThat(ai.getPlayerId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }
}
```

### TDD Benefits for This Feature

✅ **Confidence** - Every refactored line has tests proving it works
✅ **Regression Protection** - Ensure no licensing code slips through
✅ **Refactoring Safety** - Can rename fields/methods with confidence
✅ **Documentation** - Tests show how UUID system replaces licensing
✅ **Integration Validation** - Verify online game flow still works
✅ **Database Safety** - Migration scripts tested before production

### Test Coverage Goals

**Minimum Coverage:**
- PlayerIdentity.java: **95%** (core functionality)
- GameEngine player ID methods: **90%**
- PokerPlayer refactoring: **85%**
- OnlineProfile refactoring: **85%**
- Database migration: **100%** (critical)

**Overall Project Coverage:**
- Maintain existing 65% minimum threshold
- No decrease in coverage from refactoring

## Implementation Phases

### Phase 0: TDD Setup (1-2 hours)
1. **Add JUnit 5 and Mockito** to `code/common/pom.xml`
2. **Configure test runner** - Ensure Maven Surefire runs JUnit 5 tests
3. **Create test structure** - Package layout for new test files
4. **Verify setup** - Run existing tests to ensure no conflicts

### Phase 1: Core Refactoring with TDD (10-12 hours)
1. **Write tests** for `PlayerIdentity.java` (UUID generation, save/load, platform detection)
2. **Implement** `PlayerIdentity.java` to make tests pass
3. **Write tests** for GameEngine player ID methods
4. **Delete** 6 licensing files (Activation, License, etc.)
5. **Refactor** `GameEngine.java` (remove 260 lines, add player ID methods)
6. **Write tests** for PokerPlayer field changes
7. **Update** `PokerPlayer.java` (sKey_ → playerId_)
8. **Write tests** for OnlineProfile refactoring
9. **Update** `OnlineProfile.java` (remove license fields)
10. **Compile and verify** all tests pass

### Phase 2: Propagate Changes with TDD (7-9 hours)
1. **Write integration tests** for player creation flows
2. **Update** all PokerPlayer constructors (23 files)
3. **Update** player creation points (PokerGame, OnlineManager, etc.)
4. **Write tests** verifying old methods removed
5. **Remove** UI activation menus/dialogs
6. **Update** property files (remove activation text)
7. **Clean up** Java Preferences usage
8. **Compile and test** - all unit tests pass

### Phase 3: Database & Server with TDD (5-6 hours)
1. **Write tests** for database migration
2. **Create** database migration script
3. **Write tests** for server-side UUID validation
4. **Update** server-side validation (PokerServlet)
5. **Write integration tests** for online profile creation
6. **Test** online game sessions
7. **Verify** UUID uniqueness enforcement

### Phase 4: Testing & Polish (5-7 hours)
1. **Write code cleanup verification tests** (grep-style checks)
2. **Full regression testing** (single player, multiplayer)
3. **Cross-platform testing** (Windows, macOS, Linux)
4. **UUID generation/persistence testing**
5. **Help documentation updates**
6. **Final grep verification** (no license references)
7. **Code coverage review** - ensure 65% minimum maintained

**Total Effort with TDD:** 27-34 hours (includes test writing time, higher quality outcome)

## Success Criteria

The license removal is successful when:

1. **Zero licensing code** - All activation/license code removed
2. **UUID-based identity** - All players identified by UUID v4
3. **No activation UI** - No dialogs or prompts about licenses
4. **Database cleaned** - License columns removed
5. **Clean codebase** - No "key", "activation", "license" references (except UUID)
6. **Functional gameplay** - All modes work without activation
7. **Online multiplayer** - UUID-based sessions work correctly
8. **Professional UX** - Open source experience with no commercial artifacts
9. **Comprehensive test coverage** - All new code has unit tests (95%+ for PlayerIdentity, 85%+ for refactored classes)
10. **All tests passing** - Zero test failures, project builds successfully with `mvn clean test`
11. **Code cleanup verified** - Static analysis tests confirm no licensing references remain

## Notes and Considerations

### Why Keep UUID System

**UUIDs are essential for:**
- Online player identification
- Session management
- Conflict-free identifiers across servers
- Industry standard approach
- No user input required (auto-generated)

### Why Remove License Keys

**License keys are problematic because:**
- Misleading for open source users
- Complex validation logic serves no purpose
- Java Preferences storage is opaque
- Banned key tracking is meaningless
- Commercial artifact in GPL software
- Poor user experience

### Alternative: Feature Flag

**Option:** Keep licensing code but disable with feature flag
**Verdict:** **NO** - Dead code should be deleted, not disabled
- Adds maintenance burden
- Confuses new developers
- Takes up cognitive space
- No legitimate use case remains

### Backwards Compatibility

**Decision:** **No backwards compatibility for old license keys**
- New community fork, fresh start
- Old retail keys are meaningless in open source
- Users will get new UUIDs automatically
- Online profiles use credentials (username/password), not keys
- Acceptable breaking change for open source transition
