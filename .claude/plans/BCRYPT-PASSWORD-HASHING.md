# Password Security: Migrate from DES Encryption to Bcrypt Hashing

## Context

Passwords are currently stored using reversible DES encryption with a hardcoded key in `OnlineProfile`. This means anyone with the source code (it's open source) can decrypt every password in the database. The plaintext password is also displayed in the admin UI, emailed in "forgot password" flows, and logged in test mode. This plan replaces the DES encryption with bcrypt hashing so passwords are never recoverable from the database.

No data migration is needed -- this is a new product version with no existing users.

## Requirements Recap

1. **Hash passwords** with bcrypt before storing in DB
2. **Log generated password** for admin when auto-generated (at generation time, before hashing)
3. **Email password** to user on first sign-in (the generated plaintext, before hashing)
4. **Reset password** when a user forgets it (generate new, hash, email plaintext)
5. **Authenticate** by comparing user-provided plaintext against stored bcrypt hash
6. **TDD approach** throughout

---

## Step 1: Add jBCrypt Dependency

**File:** `code/pokerserver/pom.xml`

Add `org.mindrot:jbcrypt:0.4` dependency. Zero transitive dependencies, standalone bcrypt library.

---

## Step 2: TDD -- PasswordHashingService

### RED: Write tests first

**Create:** `code/pokerserver/src/test/java/com/donohoedigital/games/poker/service/PasswordHashingServiceTest.java`

Tests (unit tests, no Spring context):
- `hashPassword` returns a bcrypt hash (starts with `$2a$`, not equal to plaintext)
- `checkPassword` returns true for correct password
- `checkPassword` returns false for wrong password
- Same password hashed twice produces different hashes (different salts)
- `checkPassword` returns false for null plaintext or null hash

### GREEN: Implement

**Create:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/service/PasswordHashingService.java`

```java
public interface PasswordHashingService {
    String hashPassword(String plaintext);
    boolean checkPassword(String plaintext, String hash);
}
```

**Create:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/service/impl/PasswordHashingServiceImpl.java`

- `hashPassword`: `BCrypt.hashpw(plaintext, BCrypt.gensalt())`
- `checkPassword`: null-safe wrapper around `BCrypt.checkpw(plaintext, hash)`

**Verify:** tests pass.

---

## Step 3: Simplify OnlineProfile Entity

**File:** `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/OnlineProfile.java`

### Remove all DES encryption code:
- Delete `SECURITY_KEY` byte array (line 91-92)
- Delete `k()` method (line 294-307)
- Delete `encryptInternal()` / `decryptInternal()` (lines 312-323)
- Delete `encryptToDatabase()` / `decryptFromDatabase()` (lines 328-357)
- Delete `@PostLoad onLoad()` callback (lines 276-280)

### Rename password field to clarify it stores a hash:
- Replace `PROFILE_PASSWORD_IN_DB` constant with `PROFILE_PASSWORD_HASH`
- Rename `getPasswordInDatabase()` → `getPasswordHash()` (still mapped to `@Column(name = "wpr_password")`)
- Rename `setPasswordInDatabase()` → `setPasswordHash()` — stores the bcrypt hash directly, no encryption

### Keep transient password for transport:
- `getPassword()` stays `@Transient` — returns `data_.getString(PROFILE_PASSWORD)` directly (no decryption)
- `setPassword(String s)` stays — stores plaintext in `data_` under `PROFILE_PASSWORD` for client↔server message transport only. **No longer calls** `setPasswordHash()`. Hashing is the service layer's job.

### Update existing tests:
- **File:** `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/OnlineProfileStubTest.java` — update for renamed methods

---

## Step 4: Update OnlineProfileService Interface & Implementation

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/service/OnlineProfileService.java`

Add: `void hashAndSetPassword(OnlineProfile profile, String plaintext);`

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/service/impl/OnlineProfileServiceImpl.java`

### TDD: Update auth tests first (RED), then implement (GREEN)

- `@Autowired PasswordHashingService passwordHashingService`
- New `hashAndSetPassword()` method: calls `passwordHashingService.hashPassword(plaintext)` then `profile.setPasswordHash(hash)`
- Modify `authenticateOnlineProfile()` (line 170):
  - Before: `lookup.getPassword().equals(profile.getPassword())`
  - After: `passwordHashingService.checkPassword(profile.getPassword(), lookup.getPasswordHash())`
- Modify `retire()` (line 125):
  - Before: `profile.setPassword("__retired__")`
  - After: `profile.setPasswordHash(passwordHashingService.hashPassword("__retired__"))`

---

## Step 5: Update PokerServer Admin Profile Initialization

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java`

`initializeAdminProfile()` (lines 82-147) has 3 code paths that need updating:

### 5a. Profile doesn't exist + no explicit password (line 100-107)
- Generates password and logs it — **still works**. Just hash before saving:
  ```
  Before: profile.setPassword(adminPassword);
  After:  onlineProfileService.hashAndSetPassword(profile, adminPassword);
  ```
- The generated plaintext is logged before hashing, so admin can still see it in logs.

### 5b. Profile doesn't exist + explicit password set
- Same change: hash before saving via `hashAndSetPassword()`

### 5c. Profile exists + explicit password set (line 130-137)
- Same change: `onlineProfileService.hashAndSetPassword(profile, adminPassword)` then update

### 5d. Profile exists + no explicit password (line 139-145) — **KEY EDGE CASE**
- Currently logs `profile.getPassword()` — this returns plaintext from DES decryption
- **After bcrypt, the plaintext is not recoverable.** Cannot log the existing password.
- **Solution:** Save the generated plaintext to a file on disk (`/data/admin-password.txt`) at first creation. On subsequent restarts, read the plaintext from the file and log it. The password stays the same across restarts since `/data` is a Docker volume.
- Updated logic:
  ```java
  // Profile exists, no explicit password - read from persisted file
  String savedPassword = readAdminPasswordFile();
  if (savedPassword != null) {
      logger.warn("Admin credentials (from {}):", ADMIN_PASSWORD_FILE);
      logger.warn("  Username: {}", adminUsername);
      logger.warn("  Password: {}", savedPassword);
      logger.warn("  Set ADMIN_PASSWORD env var to customize");
  } else {
      // File missing (e.g., volume was wiped) - regenerate
      adminPassword = onlineProfileService.generatePassword();
      onlineProfileService.hashAndSetPassword(profile, adminPassword);
      onlineProfileService.updateOnlineProfile(profile);
      writeAdminPasswordFile(adminPassword);
      logger.warn("Admin credentials (regenerated, file was missing):");
      logger.warn("  Username: {}", adminUsername);
      logger.warn("  Password: {}", adminPassword);
  }
  ```

### 5e. Admin password file helper methods
- Add `writeAdminPasswordFile(String password)` — writes plaintext to `$WORK/admin-password.txt` (or `/data/admin-password.txt`)
- Add `readAdminPasswordFile()` — reads plaintext from the file, returns null if missing
- Use existing `WORK` environment variable (already set in `entrypoint.sh` to `/data/work`) to locate the file
- Also write the file in the "profile doesn't exist" path (5a/5b) so it's always available on restart

### Update existing tests:
- **File:** `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/PokerServerTest.java` — update for new hashing behavior

---

## Step 6: Update PokerServlet (Server API Endpoints)

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServlet.java`

### 6a. `addOnlineProfile()` (line 729-731)
- After: `onlineProfileService.hashAndSetPassword(profile, generatedPassword)`
- `generatedPassword` plaintext still passed to `sendProfileEmail()` for emailing to user

### 6b. `resetOnlineProfile()` (line 816-818)
- After: `onlineProfileService.hashAndSetPassword(profileToUpdate, generatedPassword)`

### 6c. `changeOnlineProfilePassword()` (line 961)
- After: `onlineProfileService.hashAndSetPassword(profileToUpdate, newpassword.getPassword())`

### 6d. `sendOnlineProfilePassword()` (line 996) — convert to reset flow
- Before: retrieves stored plaintext and emails it
- After: generate new password, hash & store it, update profile, email the new plaintext
- This is necessary because bcrypt hashes are irreversible

### 6e. `sendProfileEmail()` (line 1019, 1026) — no change needed
- Already logs the generated plaintext (before hashing), which is the desired admin debug behavior

---

## Step 7: Update Wicket Web Pages

### 7a. LoginUtils.java — bcrypt auth

**File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/util/LoginUtils.java`

- Line 160: Replace `profile.getPassword().equals(password)` with `hashingService.checkPassword(password, profile.getPasswordHash())`
- Get `PasswordHashingService` from `PokerWicketApplication`

**File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/PokerWicketApplication.java`

- Add `getPasswordHashingService()` method (same pattern as existing `getProfileService()`)

### 7b. ForgotPassword.java — convert to password reset

**File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/online/ForgotPassword.java`

- Before: `profile.getPassword()` → email existing password
- After: generate new password via `profileService.generatePassword()`, hash & store via `profileService.hashAndSetPassword()`, update profile, email the new plaintext
- Inject `PasswordHashingService` or use `profileService.hashAndSetPassword()` (which already handles it)
- Update user-facing messages: "Your password has been reset and sent to..."

**File:** `code/pokerwicket/src/main/resources/.../ForgotPassword.html` (or equivalent) — update description text

### 7c. MyProfile.java — password change

**File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/online/MyProfile.java`

- Line 183: Replace `profile.setPassword(data.getNu())` with `profileService.hashAndSetPassword(profile, data.getNu())`
- `authenticateOnlineProfile(auth)` already uses bcrypt internally (changed in Step 4)

### 7d. OnlineProfileSearch.java — remove password display, add reset button

**File:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/admin/pages/OnlineProfileSearch.java`

- Line 259: Remove `row.add(new StringLabel("password"))`
- Add a "Reset Password" button/link that generates a new password, hashes & stores it, and emails the new password to the user

**File:** `code/pokerwicket/src/main/resources/.../OnlineProfileSearch.html` — replace password `<span>` with reset button markup

---

## Step 8: Update Test Infrastructure

**File:** `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/PokerTestData.java`

- Update `createOnlineProfile()`: use `profile.setPasswordHash(BCrypt.hashpw("password", BCrypt.gensalt()))` instead of `profile.setPassword("password")`

**File:** `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/OnlineProfileTest.java`

- Update assertions: check `getPasswordHash()` starts with `$2a$` instead of asserting plaintext round-trip

**File:** Spring test context XML(s) — ensure `PasswordHashingServiceImpl` is available as a bean

---

## Files Modified (Summary)

| File | Change |
|------|--------|
| `code/pokerserver/pom.xml` | Add jBCrypt dependency |
| `code/pokerengine/.../model/OnlineProfile.java` | Remove DES encryption, add hash-based storage |
| `code/pokerserver/.../service/PasswordHashingService.java` | **New** — interface |
| `code/pokerserver/.../service/impl/PasswordHashingServiceImpl.java` | **New** — bcrypt implementation |
| `code/pokerserver/.../service/OnlineProfileService.java` | Add `hashAndSetPassword()` |
| `code/pokerserver/.../service/impl/OnlineProfileServiceImpl.java` | Bcrypt auth, inject hashing service |
| `code/pokerserver/.../server/PokerServer.java` | Hash admin password, regenerate on restart if not configured |
| `code/pokerserver/.../server/PokerServlet.java` | Hash passwords before storing, convert send-password to reset |
| `code/pokerwicket/.../util/LoginUtils.java` | Bcrypt comparison for web login |
| `code/pokerwicket/.../PokerWicketApplication.java` | Expose `PasswordHashingService` |
| `code/pokerwicket/.../pages/online/ForgotPassword.java` + `.html` | Convert to password reset flow |
| `code/pokerwicket/.../pages/online/MyProfile.java` | Use `hashAndSetPassword()` |
| `code/pokerwicket/.../admin/pages/OnlineProfileSearch.java` + `.html` | Replace password display with Reset button |
| `code/pokerserver/.../server/PokerTestData.java` | Use bcrypt in test fixtures |
| `code/pokerserver/.../server/OnlineProfileTest.java` | Update assertions |
| `code/pokerserver/.../server/PokerServerTest.java` | Update for admin password hashing |
| `code/pokerengine/.../model/OnlineProfileStubTest.java` | Update for renamed methods |
| `code/pokerserver/.../service/PasswordHashingServiceTest.java` | **New** — unit tests |

## Verification

1. `mvn test -pl pokerserver` — all service & servlet tests pass
2. `mvn test -pl pokerengine` — entity tests pass
3. `mvn test -pl pokerwicket` — wicket page tests pass
4. `mvn package` — full build succeeds with zero warnings
5. Manual: start server, create profile → password emailed, login works, forgot password resets & emails new password, admin UI shows Reset button instead of password
