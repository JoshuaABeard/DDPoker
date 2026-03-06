# Remove Legacy Server Module — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the dead `code/server/` module and all its consumers, replacing the one useful piece (admin profile initialization) with a standalone Spring component.

**Architecture:** The `server` module is a legacy custom socket-based HTTP server framework (2003) fully replaced by Spring Boot. Only two classes are referenced externally: `GameServer` (extended by `PokerServer`) and `ServerSecurityProvider` (used by 3 gametools utilities). `PokerServer`'s only useful logic is admin profile init, which moves to a new `AdminProfileInitializer` component. The `PostalServiceConfig` `@Bean` gains a `destroyMethod` to replace the lifecycle management previously done by `PokerServer.destroy()`.

**Tech Stack:** Java 25, Spring Boot 3.5, Maven, JUnit 5, Mockito, AssertJ

**Design doc:** `docs/plans/2026-03-05-remove-legacy-server-module-design.md`

---

### Task 1: Create AdminProfileInitializer with tests (TDD)

**Files:**
- Create: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/AdminProfileInitializer.java`
- Test: `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/AdminProfileInitializerTest.java`

**Step 1: Write the test class**

Copy the existing `PokerServerTest.java` as the basis. Replace `PokerServer` with `AdminProfileInitializer`. Remove the reflection hack for dependency injection — use constructor injection instead.

```java
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.server.config.TestConfig;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@Tag("slow")
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@Transactional
class AdminProfileInitializerTest {
    @Autowired
    private OnlineProfileService profileService;

    @AfterEach
    void cleanup() {
        System.clearProperty("settings.admin.user");
        System.clearProperty("settings.admin.password");
    }

    private AdminProfileInitializer createInitializer() {
        return new AdminProfileInitializer(profileService);
    }

    @Test
    @Rollback
    void should_CreateAdminProfile_When_ItDoesNotExist() {
        System.setProperty("settings.admin.user", "newadmin");
        System.setProperty("settings.admin.password", "testpass123");

        createInitializer().initializeAdminProfile();

        OnlineProfile profile = profileService.getOnlineProfileByName("newadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("newadmin");
        assertThat(profile.isRetired()).isFalse();
        assertThat(profile.getEmail()).isEqualTo("admin@localhost");

        OnlineProfile authProfile = new OnlineProfile();
        authProfile.setName("newadmin");
        authProfile.setPassword("testpass123");
        assertThat(profileService.authenticateOnlineProfile(authProfile)).isNotNull();
    }

    @Test
    @Rollback
    void should_UpdateAdminProfile_When_ItAlreadyExists() {
        OnlineProfile existing = PokerTestData.createOnlineProfile("existingadmin");
        existing.setRetired(true);
        profileService.saveOnlineProfile(existing);

        System.setProperty("settings.admin.user", "existingadmin");
        System.setProperty("settings.admin.password", "newpass456");

        createInitializer().initializeAdminProfile();

        OnlineProfile profile = profileService.getOnlineProfileByName("existingadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.isRetired()).isFalse();

        OnlineProfile authProfile = new OnlineProfile();
        authProfile.setName("existingadmin");
        authProfile.setPassword("newpass456");
        assertThat(profileService.authenticateOnlineProfile(authProfile)).isNotNull();

        authProfile.setPassword("password");
        assertThat(profileService.authenticateOnlineProfile(authProfile)).isNull();
    }

    @Test
    @Rollback
    void should_GeneratePassword_When_NotProvided() {
        System.setProperty("settings.admin.user", "autogenadmin");

        createInitializer().initializeAdminProfile();

        OnlineProfile profile = profileService.getOnlineProfileByName("autogenadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("autogenadmin");
        assertThat(profile.getPasswordHash()).isNotNull();
        assertThat(profile.getPasswordHash()).startsWith("$2a$");
    }

    @Test
    @Rollback
    void should_CreateDefaultAdmin_When_AdminUsernameNotSet() {
        System.clearProperty("settings.admin.user");
        System.clearProperty("settings.admin.password");

        createInitializer().initializeAdminProfile();

        OnlineProfile profile = profileService.getOnlineProfileByName("admin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("admin");
        assertThat(profile.isRetired()).isFalse();
        assertThat(profile.getPasswordHash()).isNotNull();
        assertThat(profile.getPasswordHash()).startsWith("$2a$");
    }

    @Test
    @Rollback
    void should_KeepExistingPassword_When_ProfileExistsAndPasswordNotProvided() {
        OnlineProfile existing = PokerTestData.createOnlineProfile("keepadmin");
        profileService.hashAndSetPassword(existing, "existingpass123");
        existing.setRetired(false);
        profileService.saveOnlineProfile(existing);

        String workDir = System.getenv("WORK");
        if (workDir == null) workDir = "/data";
        try {
            java.nio.file.Path dirPath = java.nio.file.Paths.get(workDir);
            java.nio.file.Files.createDirectories(dirPath);
            java.nio.file.Path filePath = dirPath.resolve("admin-password.txt");
            java.nio.file.Files.writeString(filePath, "existingpass123", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write password file for test", e);
        }

        System.setProperty("settings.admin.user", "keepadmin");
        System.clearProperty("settings.admin.password");

        createInitializer().initializeAdminProfile();

        OnlineProfile profile = profileService.getOnlineProfileByName("keepadmin");
        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("keepadmin");
        assertThat(profile.isRetired()).isFalse();

        OnlineProfile authProfile = new OnlineProfile();
        authProfile.setName("keepadmin");
        authProfile.setPassword("existingpass123");
        assertThat(profileService.authenticateOnlineProfile(authProfile)).isNotNull();
    }
}
```

**Step 2: Write AdminProfileInitializer**

Move the admin profile logic from `PokerServer.java` into a standalone Spring component with constructor injection.

```java
package com.donohoedigital.games.poker.server;

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class AdminProfileInitializer {
    private static final Logger logger = LogManager.getLogger(AdminProfileInitializer.class);
    private static final String ADMIN_PASSWORD_FILE = "admin-password.txt";

    private final OnlineProfileService onlineProfileService;

    public AdminProfileInitializer(OnlineProfileService onlineProfileService) {
        this.onlineProfileService = onlineProfileService;
    }

    @PostConstruct
    public void init() {
        initializeAdminProfile();
    }

    void initializeAdminProfile() {
        // Exact same logic as PokerServer.initializeAdminProfile()
        // (copy lines 92-158 from PokerServer.java)
    }

    private void writeAdminPasswordFile(String password) {
        // Exact same logic as PokerServer.writeAdminPasswordFile()
        // (copy lines 163-176 from PokerServer.java)
    }

    private String readAdminPasswordFile() {
        // Exact same logic as PokerServer.readAdminPasswordFile()
        // (copy lines 181-196 from PokerServer.java)
    }
}
```

Note: Copy the method bodies exactly from `PokerServer.java` lines 92-196. Use the community copyright header (Template 3).

**Step 3: Run the tests**

Run: `cd code && mvn test -pl pokerserver -Dtest=AdminProfileInitializerTest`
Expected: All 5 tests PASS

**Step 4: Commit**

```bash
git add code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/AdminProfileInitializer.java
git add code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/AdminProfileInitializerTest.java
git commit -m "feat: add AdminProfileInitializer to replace PokerServer admin logic"
```

---

### Task 2: Fix DDPostalService lifecycle

**Files:**
- Modify: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/config/PostalServiceConfig.java:35`

**Step 1: Add destroyMethod to @Bean**

In `PostalServiceConfig.java`, change line 35:

```java
// Before:
@Bean
public DDPostalService postalService() {

// After:
@Bean(destroyMethod = "destroy")
public DDPostalService postalService() {
```

This ensures Spring calls `DDPostalServiceImpl.destroy()` on shutdown, replacing the explicit call in `PokerServer.destroy()`.

**Step 2: Run pokerserver tests to verify no breakage**

Run: `cd code && mvn test -pl pokerserver -P dev`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/config/PostalServiceConfig.java
git commit -m "fix: register DDPostalService destroy method with Spring"
```

---

### Task 3: Delete PokerServer and its test

**Files:**
- Delete: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java`
- Delete: `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/PokerServerTest.java`
- Modify: `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/config/TestConfig.java:45`

**Step 1: Delete PokerServer.java and PokerServerTest.java**

```bash
rm code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java
rm code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/PokerServerTest.java
```

**Step 2: Update TestConfig.java**

Remove the `PokerServer.class` import and exclusion filter. The `AdminProfileInitializer` should also be excluded from test context (same reason — tests don't need it to run on startup).

```java
// Before (line 20-21):
import com.donohoedigital.games.poker.server.PokerServer;
import com.donohoedigital.games.poker.server.PokerServerMain;

// After:
import com.donohoedigital.games.poker.server.AdminProfileInitializer;
import com.donohoedigital.games.poker.server.PokerServerMain;

// Before (line 45-46):
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {PokerServer.class,
                PostalServiceConfig.class, PokerServerMain.class})})

// After:
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {AdminProfileInitializer.class,
                PostalServiceConfig.class, PokerServerMain.class})})
```

**Step 3: Run pokerserver tests**

Run: `cd code && mvn test -pl pokerserver -P dev`
Expected: All tests PASS (AdminProfileInitializerTest still passes, PokerServerTest is gone)

**Step 4: Commit**

```bash
git add -u code/pokerserver/
git commit -m "refactor: remove PokerServer, update TestConfig for AdminProfileInitializer"
```

---

### Task 4: Delete gametools utilities

**Files:**
- Delete: `code/gametools/src/main/java/com/donohoedigital/games/tools/GenerateKey.java`
- Delete: `code/gametools/src/main/java/com/donohoedigital/games/tools/EncryptString.java`
- Delete: `code/gametools/src/main/java/com/donohoedigital/games/tools/EncryptKey.java`

**Step 1: Delete the 3 files**

```bash
rm code/gametools/src/main/java/com/donohoedigital/games/tools/GenerateKey.java
rm code/gametools/src/main/java/com/donohoedigital/games/tools/EncryptString.java
rm code/gametools/src/main/java/com/donohoedigital/games/tools/EncryptKey.java
```

**Step 2: Run gametools tests**

Run: `cd code && mvn test -pl gametools -P dev`
Expected: All tests PASS (these files had no tests)

**Step 3: Commit**

```bash
git add -u code/gametools/
git commit -m "chore: remove unused encryption key utilities"
```

---

### Task 5: Remove server dependency from POMs

**Files:**
- Modify: `code/pokerserver/pom.xml:84-88` — remove server dependency
- Modify: `code/gamecommon/pom.xml:52-56` — remove server dependency

**Step 1: Edit pokerserver/pom.xml**

Remove these lines (84-88):

```xml
    <dependency>
      <groupId>com.donohoedigital</groupId>
      <artifactId>server</artifactId>
      <version>3.3.0-CommunityEdition</version>
    </dependency>
```

**Step 2: Edit gamecommon/pom.xml**

Remove these lines (52-56):

```xml
    <dependency>
      <groupId>com.donohoedigital</groupId>
      <artifactId>server</artifactId>
      <version>3.3.0-CommunityEdition</version>
    </dependency>
```

**Step 3: Build pokerserver and gamecommon to verify**

Run: `cd code && mvn compile -pl gamecommon,pokerserver`
Expected: BUILD SUCCESS (no code references server module)

**Step 4: Commit**

```bash
git add code/pokerserver/pom.xml code/gamecommon/pom.xml
git commit -m "build: remove server dependency from pokerserver and gamecommon"
```

---

### Task 6: Delete server module

**Files:**
- Delete: `code/server/` (entire directory)
- Modify: `code/pom.xml:50` — remove `<module>server</module>`

**Step 1: Remove module from parent POM**

In `code/pom.xml`, remove line 50:

```xml
    <module>server</module>
```

**Step 2: Delete the server directory**

```bash
rm -rf code/server/
```

**Step 3: Run full build**

Run: `cd code && mvn test -P dev`
Expected: BUILD SUCCESS, all tests pass

**Step 4: Commit**

```bash
git add -u code/server/ code/pom.xml
git commit -m "chore: remove legacy server module (replaced by Spring Boot)"
```

---

### Task 7: Clean up dead server properties

**Files:**
- Modify: `code/pokerserver/src/main/resources/config/poker/server.properties:81-103` — remove `settings.server.*` block
- Modify: `code/pokerserver/src/main/resources/config/poker/cmdline.properties:44-45` — remove `settings.server.*` lines

**Step 1: Remove legacy server properties from server.properties**

Remove lines 81-103 (the "Games Server" block with `settings.server.*` properties). Keep the rest of the file intact (UDP settings, other configs).

**Step 2: Remove legacy server properties from cmdline.properties**

Remove lines 44-45:

```
settings.server.readtimeout.millis=     10000
settings.server.readwait.millis=        100
```

**Step 3: Verify these properties are not referenced anywhere**

Run: `grep -r "settings\.server\." code/ --include="*.java"` — should return zero results.

**Step 4: Run full build**

Run: `cd code && mvn test -P dev`
Expected: BUILD SUCCESS, all tests pass

**Step 5: Commit**

```bash
git add code/pokerserver/src/main/resources/config/poker/server.properties
git add code/pokerserver/src/main/resources/config/poker/cmdline.properties
git commit -m "chore: remove dead legacy server properties"
```

---

### Task 8: Final verification and docs

**Step 1: Run full test suite with coverage**

Run: `cd code && mvn verify -P coverage`
Expected: BUILD SUCCESS, all coverage thresholds met

**Step 2: Verify no references to deleted code remain**

Run these searches — all should return zero results:
- `grep -r "com.donohoedigital.server" code/ --include="*.java"`
- `grep -r "GameServer" code/ --include="*.java"` (should only match pokergameserver's `GameServer*` classes, not the deleted one)
- `grep -r "ServerSecurityProvider" code/ --include="*.java"`
- `grep -r "PokerServer" code/ --include="*.java"` (should only match `PokerServerMain` and test references)

**Step 3: Update docs/memory.md if needed**

Add entry noting the server module removal if relevant context exists there.

**Step 4: Commit any doc updates**

```bash
git commit -m "docs: update memory for legacy server module removal"
```
