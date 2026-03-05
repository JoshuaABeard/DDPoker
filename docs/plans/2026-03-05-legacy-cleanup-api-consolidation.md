# Legacy Cleanup + API Consolidation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove dead legacy code, modernize pokerserver to Spring Boot, and consolidate the standalone `api` module into the unified server process.

**Architecture:** Three workstreams executed in order: (A) dead code cleanup, (B) Spring Boot modernization of `pokerserver`, (C) merge `api` controllers into `pokergameserver` and update all clients. Single deployable Spring Boot JAR replaces the current dual-process (pokerserver + api) deployment.

**Tech Stack:** Java 25, Spring Boot 3.5.x, Spring 6.x, Hibernate 6.x, H2, Maven, Next.js (web client)

**Design doc:** `docs/plans/2026-03-05-legacy-cleanup-api-consolidation-design.md`

---

## Workstream A: Dead Code Cleanup

### Task 1: Delete orphaned gameserver/target

**Files:**
- Delete: `code/gameserver/target/` (entire directory)

**Step 1: Delete the orphaned directory**

```bash
rm -rf code/gameserver/target
# If the parent directory is now empty, remove it too
rmdir code/gameserver 2>/dev/null || true
```

**Step 2: Verify no references remain**

```bash
grep -r "gameserver" code/pom.xml
```

Expected: No `<module>gameserver</module>` entry (already removed in earlier cleanup).

**Step 3: Build to verify nothing breaks**

```bash
cd code && mvn compile -P fast -q
```

Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add -A code/gameserver
git commit -m "chore: delete orphaned gameserver/target artifacts"
```

---

### Task 2: Update DDPOKER-OVERVIEW.md

**Files:**
- Modify: `docs/architecture/DDPOKER-OVERVIEW.md`

The current overview lists 19 modules but `code/pom.xml` has 14:

```
common, mail, gui, db, server, gamecommon, gameengine, gametools,
pokerengine, pokergamecore, pokergameserver, poker, pokerserver, api
```

**Step 1: Read the current file**

Read `docs/architecture/DDPOKER-OVERVIEW.md` fully.

**Step 2: Update the module table**

Replace the module table with the actual 14 modules. Remove deleted modules: `jsp`, `udp`, `tools`, `ddpoker`, `pokernetwork`, `gameserver`. Add missing: `pokergamecore`. Update count from 19 to 14.

Updated table:

| Module           | Description                                        | Artifact |
|------------------|----------------------------------------------------|----------|
| `common`         | Core config, logging, XML, properties, utils       | jar      |
| `mail`           | Email sending tools                                | jar      |
| `gui`            | GUI infrastructure extending Java Swing            | jar      |
| `db`             | Database infrastructure extending Hibernate        | jar      |
| `server`         | Core server functionality                          | jar      |
| `gamecommon`     | Core game utilities (shared client/server)         | jar      |
| `gameengine`     | Core game engine (Swing UI framework)              | jar      |
| `gametools`      | Game building tools (border/territory managers)    | jar      |
| `pokerengine`    | Core poker utilities (shared client/server)        | jar      |
| `pokergamecore`  | Server-side game engine (pure logic, no Swing)     | jar      |
| `pokergameserver`| Game server Spring Boot auto-configuration         | jar      |
| `poker`          | DD Poker UI / desktop client                       | jar      |
| `pokerserver`    | DD Poker backend server                            | jar      |
| `api`            | REST API (Spring Boot)                             | jar      |

Also fix the "19 modules" text to "14 modules".

**Step 3: Verify the doc reads correctly**

Re-read the file and confirm accuracy.

**Step 4: Commit**

```bash
git add docs/architecture/DDPOKER-OVERVIEW.md
git commit -m "docs: update DDPOKER-OVERVIEW.md module list to match current 14-module build"
```

---

### Task 3: Investigate CheckEndHand reachability

**Files:**
- Read: `code/poker/src/main/java/com/donohoedigital/games/poker/CheckEndHand.java`
- Read: `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/TournamentEngine.java`
- Read: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirector.java`
- Read: `code/poker/src/main/resources/config/poker/gamedef.xml` (TD.* phase registrations)

**Step 1: Trace the execution path**

Determine whether `CheckEndHand.java` (the client-side ChainPhase) actually executes when practice games go through the embedded server via `WebSocketTournamentDirector`.

Key questions:
1. Does `WebSocketTournamentDirector` ever invoke the phase chain that triggers `TD.CheckEndHand`?
2. Does `TournamentEngine` (pokergamecore) return the phase name `"TD.CheckEndHand"` — and if so, does the server-side `ServerTournamentDirector` handle it without the client?
3. Are there any code paths that still use the old `TournamentDirector` (non-WebSocket) for practice games?

**Step 2: Document findings**

If CheckEndHand client-side class is unreachable:
- Delete `code/poker/src/main/java/com/donohoedigital/games/poker/CheckEndHand.java`
- Delete `code/poker/src/test/java/com/donohoedigital/games/poker/CheckEndHandTest.java`
- Remove `TD.CheckEndHand` from `gamedef.xml`
- Commit: `refactor: remove unreachable client-side CheckEndHand phase`

If CheckEndHand is still reachable (some edge case):
- Document why in `docs/memory.md`
- Leave it in place
- Commit: `docs: document CheckEndHand reachability analysis`

**Step 3: Repeat for other TD.* phases**

Audit these TD.* phases the same way:
- `TD.Bet` (class: `Bet.java`)
- `TD.NewLevelActions` (class: `NewLevelActions.java`)

Memory note says: "TD.NewLevelActions, TD.Bet are dead code as phases — never triggered by TournamentEngine." Verify this is still true and remove if confirmed dead.

**Step 4: Commit**

Commit all removals together or document findings.

---

### Task 4: Remove dead Pot methods

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/Pot.java`
- Modify: test files if they test these methods

**Step 1: Verify no callers**

Grep for each method across the entire codebase:

```bash
# From code/ directory
grep -rn "\.reset()" --include="*.java" | grep -i pot
grep -rn "\.advanceRound()" --include="*.java" | grep -i pot
grep -rn "\.setSideBet(" --include="*.java"
```

Expected: Only definitions and possibly test callers, no production callers.

**Step 2: Remove dead methods**

Remove `Pot.reset()`, `Pot.advanceRound()`, and `Pot.setSideBet()` from `Pot.java`.

**Step 3: Remove test coverage for deleted methods**

If any tests exclusively test these methods, remove them.

**Step 4: Build and test**

```bash
cd code && mvn test -P dev -pl poker
```

Expected: BUILD SUCCESS, all tests pass.

**Step 5: Commit**

```bash
git add code/poker/
git commit -m "refactor: remove dead Pot methods (reset, advanceRound, setSideBet)"
```

---

### Task 5: Audit HoldemHand/PokerTable game-driving methods

This is an investigation task. Do NOT remove methods that are called by `PokerStats`, `ServerGameTable`, or tests — only remove methods with zero callers.

**Files:**
- Read: `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java`
- Read: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerTable.java`

**Step 1: For each public state-modifying method in HoldemHand, grep for callers**

Methods to check: `ante()`, `smallblind()`, `bigblind()`, `fold()`, `check()`, `checkraise()`, `call()`, `bet()`, `raise()`, `wins()`, `overbet()`, `lose()`, `deal()`, `advanceRound()`, `preResolve()`, `resolve()`

For each method, run:
```bash
grep -rn "methodName(" --include="*.java" code/
```

**Step 2: Categorize each method**

- **Keep**: Called by PokerStats, ServerGameTable, pokergamecore, or tests
- **Remove**: Zero production or test callers
- **Document**: Called only by display code (acceptable legacy)

**Step 3: Remove any methods with zero callers**

**Step 4: Build and test**

```bash
cd code && mvn test -P dev
```

**Step 5: Commit findings**

```bash
git commit -m "refactor: remove dead game-driving methods from HoldemHand/PokerTable"
```

If nothing was removed, commit documentation instead:
```bash
git commit -m "docs: audit HoldemHand/PokerTable methods — all have active callers"
```

---

### Task 6: Extract PokerStats simulation to pokergamecore

**Files:**
- Read: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerStats.java`
- Create: `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/simulation/HandSimulator.java`
- Create: `code/pokergamecore/src/test/java/com/donohoedigital/games/poker/core/simulation/HandSimulatorTest.java`

**Step 1: Read PokerStats and understand its simulation dependencies**

PokerStats uses:
- `PokerGame` — creates game with profile (line 76)
- `PokerTable` — creates table (line 80)
- `HoldemHand` — `deal()`, `advanceRound()`, `preResolve()`, `resolve()`, `getWin()`
- `PokerPlayer` — `setChipCount()`, `fold()`, `betTest()`, `getHandSorted()`, `isFolded()`

**Step 2: Design the extraction**

Create a `HandSimulator` in `pokergamecore` that encapsulates the Monte Carlo simulation without depending on `PokerGame`/`PokerTable`/`HoldemHand` from the `poker` module. It should use `pokergamecore` interfaces and `pokerengine` hand evaluation.

This is a significant refactor — the simulation currently depends on the full game engine stack. The approach:
1. Identify the minimal subset of `HoldemHand` behavior needed for simulation
2. Reimplement that subset using `pokergamecore` primitives
3. Update `PokerStats` to use the new `HandSimulator`

**Step 3: Write failing tests for HandSimulator**

Test that the simulator can:
- Deal hands to N players
- Evaluate hand strengths
- Run Monte Carlo trials and report win percentages

**Step 4: Implement HandSimulator**

Use `pokerengine` hand evaluation (HandInfo, HandInfoFast) which is already in the shared module.

**Step 5: Update PokerStats to use HandSimulator**

Replace direct `HoldemHand.deal()` usage with `HandSimulator`.

**Step 6: Build and test**

```bash
cd code && mvn test -P dev -pl pokergamecore,poker
```

**Step 7: Remove orphaned methods if any**

Check if any `HoldemHand`/`PokerTable` methods lost their last caller after this extraction.

**Step 8: Commit**

```bash
git add code/pokergamecore/ code/poker/
git commit -m "refactor: extract Monte Carlo simulation to pokergamecore HandSimulator"
```

---

## Workstream B: Spring Boot Modernization

### Task 7: Create Spring Boot configuration class for datasource

**Files:**
- Create: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/config/DataSourceConfig.java`
- Read: `code/pokerserver/src/main/resources/app-context-gameserver.xml` (source of truth for current config)

**Step 1: Read current XML datasource config**

`app-context-gameserver.xml` defines:
- `DriverManagerDataSource` with `${db.driver}`, `${db.url}`, `${db.user}`, `${db.password}`
- `LocalContainerEntityManagerFactoryBean` with Hibernate JPA
- `JpaTransactionManager`
- Component scanning (excluding poker.api and pokergameserver packages)
- Annotation-driven transaction management

**Step 2: Write the Spring Boot equivalent**

```java
package com.donohoedigital.games.poker.server.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = {
    "com.donohoedigital.games.poker.model",
    "com.donohoedigital.games.poker.gameserver.persistence"
})
@EnableJpaRepositories(basePackages = {
    "com.donohoedigital.games.poker.dao"
})
public class DataSourceConfig {
    // Spring Boot auto-configures DataSource, EntityManagerFactory,
    // and TransactionManager from application.properties
}
```

**Step 3: Migrate datasource properties to application.properties**

Map from `pokerserver.properties` to Spring Boot conventions:

```properties
spring.datasource.driver-class-name=${DB_DRIVER:org.h2.Driver}
spring.datasource.url=${DB_URL:jdbc:h2:file:./data/poker;MODE=MySQL;AUTO_SERVER=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-init.sql'}
spring.datasource.username=${DB_USER:sa}
spring.datasource.password=${DB_PASSWORD:}

spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.jpa.open-in-view=false
```

**Step 4: Write a test to verify datasource wires up**

```java
@SpringBootTest
class DataSourceConfigTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void dataSourceIsConfigured() {
        assertNotNull(dataSource);
    }
}
```

**Step 5: Commit**

```bash
git add code/pokerserver/
git commit -m "feat: add Spring Boot DataSourceConfig to replace XML datasource config"
```

---

### Task 8: Convert PokerServerMain to Spring Boot application

**Files:**
- Modify: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServerMain.java`
- Modify: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java`
- Modify: `code/pokerserver/src/main/resources/application.properties`
- Modify: `code/pokerserver/pom.xml` (add spring-boot-starter-parent or spring-boot dependency)

**Step 1: Read current PokerServerMain and PokerServer**

- `PokerServerMain` creates `ClassPathXmlApplicationContext("app-context-pokerserver.xml")`
- `PokerServer` extends `GameServer` (Thread-based server), has `init()` that calls `super.init()`, `initializeAdminProfile()`, `start()`

**Step 2: Add Spring Boot dependencies to pokerserver/pom.xml**

Add `spring-boot-starter`, `spring-boot-starter-data-jpa`, `spring-boot-starter-web` (or reuse from pokergameserver transitive). Check what's already available transitively.

**Step 3: Convert PokerServerMain**

```java
@SpringBootApplication(scanBasePackages = {
    "com.donohoedigital.games.poker.server",
    "com.donohoedigital.games.poker.service",
    "com.donohoedigital.games.poker.dao"
})
@ImportAutoConfiguration(GameServerAutoConfiguration.class)
public class PokerServerMain {
    public static void main(String[] args) {
        // Preserve existing LoggingConfig initialization
        LoggingConfig.init("poker", LoggingConfig.SERVER);
        SpringApplication.run(PokerServerMain.class, args);
    }
}
```

**Step 4: Convert PokerServer init to ApplicationRunner**

```java
@Component
public class PokerServer implements ApplicationRunner {
    private final OnlineProfileService profileService;
    private final DDPostalService postalService;

    @Override
    public void run(ApplicationArguments args) {
        initializeAdminProfile();
        // postalService lifecycle managed by Spring
    }
    // ... keep initializeAdminProfile() logic
}
```

Remove the `extends GameServer` and `extends Thread` chain. The `GameServer` base class provided legacy socket handling that's now replaced by Spring Boot's embedded Tomcat + `pokergameserver`'s WebSocket support.

**Step 5: Verify ConfigManager initialization**

`ConfigManager` is currently created as a Spring bean in XML. It needs to be preserved — create a `@Bean` method or `@Configuration` class:

```java
@Configuration
public class ConfigManagerConfig {
    @Bean
    public ConfigManager configManager() {
        return new ConfigManager("poker", ApplicationType.SERVER);
    }
}
```

**Step 6: Build and test**

```bash
cd code && mvn test -P dev -pl pokerserver
```

**Step 7: Commit**

```bash
git add code/pokerserver/
git commit -m "refactor: convert PokerServerMain to Spring Boot application"
```

---

### Task 9: Remove XML config files and unify Spring contexts

**Files:**
- Delete: `code/pokerserver/src/main/resources/app-context-pokerserver.xml`
- Delete: `code/pokerserver/src/main/resources/app-context-gameserver.xml`
- Delete: `code/pokerserver/src/main/resources/pokerserver.properties`
- Delete: `code/pokerserver/src/main/resources/META-INF/persistence-pokerserver.xml`
- Modify: `code/pokerserver/src/main/resources/application.properties` (consolidate all properties)

**Step 1: Verify all XML-defined beans are covered by annotations/config classes**

Cross-reference each bean in the XML files against the new `@Configuration` classes and `@Component`-scanned classes.

**Step 2: Consolidate properties**

Move all properties from `pokerserver.properties` into `application.properties` using Spring Boot conventions:

```properties
# App identity
app.name=poker
app.type=SERVER

# Database (Spring Boot auto-config)
spring.datasource.driver-class-name=${DB_DRIVER:org.h2.Driver}
spring.datasource.url=${DB_URL:jdbc:h2:file:./data/poker;MODE=MySQL;AUTO_SERVER=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-init.sql'}
spring.datasource.username=${DB_USER:sa}
spring.datasource.password=${DB_PASSWORD:}

# JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false

# Game server (from pokergameserver auto-config)
game.server.enabled=true

# Server port
server.port=${PORT:8877}
```

**Step 3: Remove persistence-pokerserver.xml**

Spring Boot auto-discovers `@Entity` classes via `@EntityScan`. The explicit persistence unit XML is no longer needed.

**Step 4: Delete XML files**

```bash
rm code/pokerserver/src/main/resources/app-context-pokerserver.xml
rm code/pokerserver/src/main/resources/app-context-gameserver.xml
rm code/pokerserver/src/main/resources/pokerserver.properties
rm code/pokerserver/src/main/resources/META-INF/persistence-pokerserver.xml
```

**Step 5: Update any code that references these files**

Search for `app-context-pokerserver.xml`, `app-context-gameserver.xml`, `pokerserver.properties`, `persistence-pokerserver.xml` across the codebase and update/remove references.

Check test code — some tests may load XML contexts.

**Step 6: Build and test**

```bash
cd code && mvn test -P dev -pl pokerserver
```

**Step 7: Commit**

```bash
git add code/pokerserver/
git commit -m "refactor: remove legacy XML Spring config, consolidate to application.properties"
```

---

### Task 10: Verify full build with unified Spring Boot server

**Files:**
- No new files — integration verification

**Step 1: Full build**

```bash
cd code && mvn clean test -P dev
```

**Step 2: Start the server and verify it boots**

```bash
cd code && mvn clean package -DskipTests -pl pokerserver -am
java -jar pokerserver/target/pokerserver-*.jar &
# Wait for startup
sleep 10
curl -s http://localhost:8877/api/v1/games?pageSize=1
kill %1
```

**Step 3: Verify pokergameserver auto-configuration activates**

Check startup logs for:
- `GameServerAutoConfiguration` loading
- `GameInstanceManager` bean created
- WebSocket handlers registered

**Step 4: Commit any fixes**

```bash
git commit -m "fix: resolve Spring Boot migration issues found during integration test"
```

---

## Workstream C: API Consolidation

### Task 11: Analyze API controller feature overlap

**Files:**
- Read: ALL controllers in `code/api/src/main/java/com/donohoedigital/poker/api/controller/`
- Read: ALL controllers in `code/pokergameserver/src/main/java/.../controller/`

**Step 1: Create a feature matrix**

For each endpoint in the `api` module, document:
1. HTTP method + path
2. What it does
3. Whether an equivalent already exists in `pokergameserver`
4. If equivalent exists, which implementation is more complete
5. What services/DAOs it depends on

**Step 2: Document the merge plan**

For each `api` endpoint, record one of:
- **MERGE**: Add to existing pokergameserver controller (specify which)
- **NEW**: Create new controller in pokergameserver
- **DROP**: Functionality already exists, no action needed
- **MOVE**: Move as-is to pokergameserver

**Step 3: Document in the plan**

Write findings as a checklist that Tasks 12-17 will execute against.

**Step 4: Commit analysis**

```bash
git commit -m "docs: API controller feature overlap analysis for consolidation"
```

---

### Task 12: Merge profile endpoints into existing ProfileController

**Files:**
- Modify: `code/pokergameserver/src/main/java/.../controller/ProfileController.java`
- Read: `code/api/src/main/java/.../controller/ProfileController.java`
- Create/modify: DTOs as needed

The `api` ProfileController has endpoints not in `pokergameserver`:
- GET `/api/profile` (query by name) → add as GET `/api/v1/profiles?name={name}`
- GET `/api/profile/aliases` → add as GET `/api/v1/profiles/aliases`
- POST `/api/profile/retire` → add as POST `/api/v1/profiles/{id}/retire`

**Step 1: Read both ProfileControllers fully**

**Step 2: Add missing endpoints to pokergameserver ProfileController**

Map old paths to new `/api/v1/profiles/*` convention. Add service dependencies as constructor injection.

**Step 3: Write tests for new endpoints**

**Step 4: Build and test**

```bash
cd code && mvn test -P dev -pl pokergameserver
```

**Step 5: Commit**

```bash
git add code/pokergameserver/
git commit -m "feat: merge api profile endpoints into pokergameserver ProfileController"
```

---

### Task 13: Create new controllers in pokergameserver (history, leaderboard, search, templates)

**Files:**
- Create: `code/pokergameserver/src/main/java/.../controller/HistoryController.java`
- Create: `code/pokergameserver/src/main/java/.../controller/LeaderboardController.java`
- Create: `code/pokergameserver/src/main/java/.../controller/SearchController.java`
- Create: `code/pokergameserver/src/main/java/.../controller/TemplateController.java`
- Move: DTOs from `api` module as needed

**Step 1: For each controller, read the api module version**

**Step 2: Create the pokergameserver version at `/api/v1/{resource}`**

Path mapping:
- `/api/history` → `/api/v1/history`
- `/api/tournaments/{id}` → `/api/v1/tournaments/{id}`
- `/api/leaderboard` → `/api/v1/leaderboard`
- `/api/leaderboard/player/{name}` → `/api/v1/leaderboard/player/{name}`
- `/api/search` → `/api/v1/search`
- `/api/profile/templates` → `/api/v1/profiles/templates`

**Step 3: Move DTOs**

Move `SimulationRequest`, `TemplateRequest`, `TemplateResponse` from `api` to `pokergameserver` DTO package.

**Step 4: Write tests for each new controller**

**Step 5: Build and test**

```bash
cd code && mvn test -P dev -pl pokergameserver
```

**Step 6: Commit**

```bash
git add code/pokergameserver/
git commit -m "feat: add history, leaderboard, search, template controllers to pokergameserver"
```

---

### Task 14: Create admin, download, RSS, health controllers in pokergameserver

**Files:**
- Create: `code/pokergameserver/src/main/java/.../controller/AdminController.java`
- Create: `code/pokergameserver/src/main/java/.../controller/DownloadController.java`
- Create: `code/pokergameserver/src/main/java/.../controller/RssController.java`
- Modify: existing `DevController.java` (merge verify-user into AdminController, potentially remove DevController)

**Step 1: Read api module controllers**

**Step 2: Create pokergameserver versions**

Path mapping:
- `/api/admin/profiles` → `/api/v1/admin/profiles`
- `/api/admin/bans` → `/api/v1/admin/bans`
- `/api/admin/profiles/{id}/verify` → `/api/v1/admin/profiles/{id}/verify`
- `/api/admin/profiles/{id}/unlock` → `/api/v1/admin/profiles/{id}/unlock`
- `/api/admin/profiles/{id}/resend-verification` → `/api/v1/admin/profiles/{id}/resend-verification`
- `/api/downloads/{filename}` → `/api/v1/downloads/{filename}`
- `/api/rss/{mode}` → `/api/v1/rss/{mode}`
- `/api/health` → Spring Boot Actuator `/actuator/health` or `/api/v1/health`

**Step 3: Merge DevController.verify-user into AdminController**

The existing `DevController` at `/api/v1/dev/verify-user` should be consolidated into the new `AdminController`.

**Step 4: Add hosts endpoint**

The web client calls `/api/games/hosts` — add to `GameController` or create dedicated endpoint at `/api/v1/games/hosts`.

**Step 5: Write tests**

**Step 6: Build and test**

```bash
cd code && mvn test -P dev -pl pokergameserver
```

**Step 7: Commit**

```bash
git add code/pokergameserver/
git commit -m "feat: add admin, download, RSS, health controllers to pokergameserver"
```

---

### Task 15: Move SimulationController to pokergameserver

**Files:**
- Read: `code/api/src/main/java/.../controller/SimulationController.java`
- Create: `code/pokergameserver/src/main/java/.../controller/SimulationController.java` (or add to existing)
- Move: `SimulationRequest` DTO

**Step 1: Read the api SimulationController**

Already at `/api/v1/poker/simulate` — path stays the same.

**Step 2: Move to pokergameserver**

Adapt to use pokergameserver's service layer. The simulation may depend on `pokerengine` hand evaluation — verify dependencies.

**Step 3: Write tests**

**Step 4: Build and test**

```bash
cd code && mvn test -P dev -pl pokergameserver
```

**Step 5: Commit**

```bash
git add code/pokergameserver/
git commit -m "feat: move SimulationController to pokergameserver"
```

---

### Task 16: Update web client API paths

**Files:**
- Modify: `code/web/lib/api.ts`
- Modify: `code/web/lib/__tests__/api-clients.test.ts`
- Modify: `code/web/lib/__tests__/api.test.ts`
- Modify: `code/web/app/download/page.tsx` (download links)
- Modify: `code/web/app/online/page.tsx` (RSS link)

**Step 1: Update api.ts**

Change all non-v1 paths to v1:

| Old Path | New Path |
|---|---|
| `/api/players/{id}` | `/api/v1/profiles/{id}` |
| `/api/players/name/{name}` | `/api/v1/profiles/name/{name}` |
| `/api/players/me` | `/api/v1/profiles/me` |
| `/api/players/me/password` | `/api/v1/profiles/me/password` |
| `/api/profile/aliases` | `/api/v1/profiles/aliases` |
| `/api/profile/templates` | `/api/v1/profiles/templates` |
| `/api/profile/templates/{id}` | `/api/v1/profiles/templates/{id}` |
| `/api/leaderboard` | `/api/v1/leaderboard` |
| `/api/leaderboard/player/{name}` | `/api/v1/leaderboard/player/{name}` |
| `/api/history` | `/api/v1/history` |
| `/api/tournaments/{id}` | `/api/v1/tournaments/{id}` |
| `/api/search` | `/api/v1/search` |
| `/api/games/hosts` | `/api/v1/games/hosts` |
| `/api/admin/profiles` | `/api/v1/admin/profiles` |
| `/api/admin/bans` | `/api/v1/admin/bans` |
| `/api/admin/bans/{key}` | `/api/v1/admin/bans/{key}` |
| `/api/admin/profiles/{id}/verify` | `/api/v1/admin/profiles/{id}/verify` |
| `/api/admin/profiles/{id}/unlock` | `/api/v1/admin/profiles/{id}/unlock` |
| `/api/admin/profiles/{id}/resend-verification` | `/api/v1/admin/profiles/{id}/resend-verification` |
| `/api/downloads/{file}` | `/api/v1/downloads/{file}` |
| `/api/rss/{mode}` | `/api/v1/rss/{mode}` |
| `/api/health` | `/api/v1/health` |

Also remove the comment distinguishing "legacy `/api/*` portal API" from `/api/v1/*` — there is now only one API.

**Step 2: Update download page links**

In `code/web/app/download/page.tsx`, update `/api/downloads/` to `/api/v1/downloads/`.

**Step 3: Update RSS link**

In `code/web/app/online/page.tsx`, update `/api/rss/` to `/api/v1/rss/`.

**Step 4: Update test assertions**

Update all path assertions in `api-clients.test.ts` and `api.test.ts` to use `/api/v1/` prefix.

**Step 5: Run web tests**

```bash
cd code/web && npm test
```

**Step 6: Commit**

```bash
git add code/web/
git commit -m "feat: update web client API paths from /api/ to /api/v1/"
```

---

### Task 17: Update desktop client API paths

**Files:**
- Modify: `code/poker/src/main/java/.../online/RestGameClient.java` (if it has non-v1 paths)
- Modify: `code/poker/src/main/java/.../server/GameServerRestClient.java` (if it has non-v1 paths)
- Search: Any other HTTP client code in `code/poker/`

**Step 1: Search for non-v1 API paths in the poker module**

```bash
grep -rn '"/api/' --include="*.java" code/poker/src/main/java/ | grep -v '/api/v1/'
```

**Step 2: Update any non-v1 paths found**

**Step 3: Build and test**

```bash
cd code && mvn test -P dev -pl poker
```

**Step 4: Commit**

```bash
git add code/poker/
git commit -m "feat: update desktop client API paths to /api/v1/"
```

---

### Task 18: Update scenario test scripts

**Files:**
- Modify: files in `tests/scenarios/` that use curl with API paths

**Step 1: Search for non-v1 API paths in test scripts**

```bash
grep -rn '/api/' tests/scenarios/ | grep -v '/api/v1/'
```

**Step 2: Update paths**

**Step 3: Commit**

```bash
git add tests/scenarios/
git commit -m "test: update scenario scripts API paths to /api/v1/"
```

---

### Task 19: Delete api module

**Files:**
- Delete: `code/api/` (entire directory)
- Modify: `code/pom.xml` (remove `<module>api</module>`)
- Modify: `docker/Dockerfile` (remove api references)
- Modify: `docs/architecture/DDPOKER-OVERVIEW.md` (remove api from module table, update count to 13)

**Step 1: Remove module from parent POM**

In `code/pom.xml`, remove `<module>api</module>`.

**Step 2: Delete the module directory**

```bash
rm -rf code/api
```

**Step 3: Update Docker files**

Remove these lines from `docker/Dockerfile`:
```
COPY code/api/target/classes/ /app/classes/
COPY code/api/target/dependency/ /app/lib/
```

Update `docker/entrypoint.sh` to run single process instead of dual.

**Step 4: Update DDPOKER-OVERVIEW.md**

Remove `api` row from module table. Update count from 14 to 13.

**Step 5: Full build**

```bash
cd code && mvn clean test -P dev
```

**Step 6: Commit**

```bash
git add -A
git commit -m "refactor: delete api module — all endpoints consolidated into pokergameserver"
```

---

### Task 20: Simplify Docker deployment

**Files:**
- Modify: `docker/Dockerfile`
- Modify: `docker/entrypoint.sh`
- Modify: `docker/docker-compose.yml` (if needed)

**Step 1: Update Dockerfile**

Build single fat JAR from `pokerserver` module. Remove api module references.

**Step 2: Simplify entrypoint.sh**

Replace dual-process manager with:
```bash
exec java -jar /app/pokerserver.jar "$@"
```

**Step 3: Test Docker build**

```bash
cd docker && docker build -t ddpoker-test .
```

**Step 4: Commit**

```bash
git add docker/
git commit -m "refactor: simplify Docker to single Spring Boot process"
```

---

### Task 21: Final verification and memory update

**Step 1: Full build with coverage**

```bash
cd code && mvn clean verify -P coverage
```

**Step 2: Update docs/memory.md**

Add entries for:
- API consolidation: all endpoints now at `/api/v1/*` in pokergameserver
- pokerserver is Spring Boot (no more XML contexts)
- Single process deployment (no dual api+pokerserver)
- Remove stale entries about api module, gameserver module

**Step 3: Update DDPOKER-OVERVIEW.md if needed**

Final review of accuracy.

**Step 4: Commit**

```bash
git add docs/
git commit -m "docs: update memory and overview for API consolidation completion"
```
