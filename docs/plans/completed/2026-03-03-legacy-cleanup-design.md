# Legacy Cleanup Design — Pre-Release

**Date:** 2026-03-03
**Status:** COMPLETED (2026-03-04)
**Goal:** Remove all dead modules and replace the legacy c3p0 connection pool with HikariCP before the initial Community Edition release.

---

## Context

The audit identified eight dead/unused Maven modules still declared in the parent POM and one live-but-legacy dependency (c3p0) in the `api` module. None of the dead modules are reachable at runtime. Removing them and modernising the datasource configuration produces a cleaner, more maintainable codebase for the first release.

The companion `pokerserver` removal (which covers the legacy Spring XML context, `GameServer`/`EngineServlet`, and the `server`/`gameserver` modules) is deferred until after the account management plan completes.

---

## Section 1: Dead Module Removals

### Modules deleted entirely (directory + parent POM entry)

| Module | Reason dead |
|---|---|
| `pokerwicket` | Empty directory — no source, no pom.xml |
| `wicket` | Empty directory — no source, no pom.xml |
| `proto` | 20 prototype/experiment classes from 2003-era dev; no production consumers; sole remaining Wicket import in the codebase |
| `ddpoker` | 2 classes (`Card`, `PlayerAction`) fully superseded by `pokerengine` equivalents; no consumers |
| `tools` | `ProxyServlet` + mail utilities; no upstream consumers |
| `pokernetwork` | No `src/` directory; was P2P networking shell post-M7; only `proto` test depends on it |
| `udp` | UDP networking library; M7 plan removed all usage but forgot to delete the module |
| `jsp` | JSP compilation support for the pre-WebSocket server; not used in any current flow |

### Dependency severing (modules kept, references removed)

- **`gameserver/pom.xml`** — remove `<dependency>` on `udp`
- **`gameserver/pom.xml`** — remove `<dependency>` on `jsp` (scope: provided)
- **`parent pom.xml`** (`code/pom.xml`) — remove all 8 `<module>` entries listed above

---

## Section 2: c3p0 → HikariCP

`DatabaseConfig.java` in the `api` module manually constructs a `ComboPooledDataSource`. Since `api` is a Spring Boot application, the correct approach is to remove the manual `@Bean DataSource` method and let Spring Boot auto-configure a HikariCP pool. HikariCP is already on the classpath via `spring-boot-starter-data-jpa`.

The `entityManagerFactory` and `transactionManager` beans in `DatabaseConfig.java` are retained — they are still required to load `persistence-pokerserver.xml` for legacy entity scanning. They receive the Spring Boot auto-configured datasource instead of c3p0.

### Changes

**`api/src/main/java/.../config/DatabaseConfig.java`**
- Delete the `@Bean DataSource` method
- Remove imports: `ComboPooledDataSource`, `PropertyVetoException`
- Remove `@Value` fields: `dbDriver`, `dbUrl`, `dbUser`, `dbPassword`

**`api/src/main/resources/application.properties`**
- Replace custom `db.*` keys with standard `spring.datasource.*` keys
- Add `spring.datasource.hikari.*` pool settings to match current c3p0 config:
  - `minimum-idle=1`, `maximum-pool-size=5`, `idle-timeout=21600000` (6 hours in ms)

**`gameserver/pom.xml`**
- Remove the c3p0 `<dependency>` block

---

## Section 3: Testing

No new tests are needed — these are pure deletions and a datasource swap.

Verification:
1. `mvn test -P dev` from `code/` — confirms clean compile and test pass with all 8 modules removed and HikariCP wired
2. `mvn test -pl api -P dev` — focused check that `DatabaseConfig` loads the Spring context correctly
3. Compiler catches any stray `<dependency>` on a deleted module automatically

---

## Out of Scope

- `pokerserver` module removal (deferred — depends on account management plan completing first)
- `gameserver` / `server` module removal (same deferral)
- Spring XML → Spring Boot `@Configuration` migration (same deferral)
- `OnlineConfiguration` / `SendMessageDialog` legacy UI cleanup (separate concern)
