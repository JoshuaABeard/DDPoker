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

## Implementation Phases

### Phase 1: Core Refactoring (8-10 hours)
1. Create `PlayerIdentity.java` class
2. Delete 6 licensing files (Activation, License, etc.)
3. Refactor `GameEngine.java` (remove 260 lines)
4. Update `PokerPlayer.java` (sKey_ → playerId_)
5. Update `OnlineProfile.java` (remove license fields)
6. Compile and fix immediate errors

### Phase 2: Propagate Changes (6-8 hours)
1. Update all PokerPlayer constructors (23 files)
2. Update player creation points (PokerGame, OnlineManager, etc.)
3. Remove UI activation menus/dialogs
4. Update property files (remove activation text)
5. Clean up Java Preferences usage
6. Compile and test

### Phase 3: Database & Server (4-5 hours)
1. Create database migration script
2. Update server-side validation (PokerServlet)
3. Test online profile creation
4. Test online game sessions
5. Verify UUID uniqueness enforcement

### Phase 4: Testing & Polish (5-6 hours)
1. Full regression testing (single player, multiplayer)
2. Cross-platform testing (Windows, macOS, Linux)
3. UUID generation/persistence testing
4. Help documentation updates
5. Final grep verification (no license references)

**Total Effort:** 23-29 hours

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
