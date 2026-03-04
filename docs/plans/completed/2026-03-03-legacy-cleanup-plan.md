# Legacy Cleanup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Status:** COMPLETED (2026-03-04)

**Goal:** Remove 8 dead Maven modules and replace the legacy c3p0 connection pool with Spring Boot's built-in HikariCP before the Community Edition initial release.

**Architecture:** Pure deletion + one datasource config swap. No new code is introduced. Each task is independently verifiable with a compile check. The legacy `pokerserver` Spring XML context and `GameServer` infrastructure are explicitly out of scope (deferred to a later plan).

**Design doc:** `docs/plans/2026-03-03-legacy-cleanup-design.md`

**Tech Stack:** Java 25, Maven 3.9, Spring Boot 3.5.8, HikariCP (already on classpath via `spring-boot-starter-data-jpa`)

---

## Test commands reference

```bash
# Full build + unit tests (fast)
cd code && mvn test -P dev

# Single module compile check
cd code && mvn test-compile -pl <module> -P dev

# Api module tests only
cd code && mvn test -pl api -P dev
```

---

## Task 1: Delete empty directories not in the parent POM

`pokerwicket/` and `wicket/` are leftover directories with no source and no `pom.xml`. They are not declared in the parent POM so no POM changes are needed — just delete the directories.

**Files:**
- Delete directory: `code/pokerwicket/`
- Delete directory: `code/wicket/`

**Step 1: Confirm neither directory has a pom.xml**

```bash
ls code/pokerwicket/ && ls code/wicket/
```

Expected: only a `target/` subdirectory in each (no `src/`, no `pom.xml`).

**Step 2: Delete both directories**

```bash
rm -rf code/pokerwicket code/wicket
```

**Step 3: Verify parent POM has no reference to them**

```bash
grep -n "pokerwicket\|<module>wicket" code/pom.xml
```

Expected: no output.

**Step 4: Build check**

```bash
cd code && mvn test-compile -P dev 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

**Step 5: Commit**

```bash
git add -A code/pokerwicket code/wicket
git commit -m "chore: delete empty pokerwicket and wicket artifact directories"
```

---

## Task 2: Remove proto, ddpoker, tools, pokernetwork from parent POM and delete

These four modules have no production consumers. They are declared in the parent POM but nothing in the active build imports their classes.

**Files:**
- Modify: `code/pom.xml` — remove 4 `<module>` entries
- Delete directory: `code/proto/`
- Delete directory: `code/ddpoker/`
- Delete directory: `code/tools/`
- Delete directory: `code/pokernetwork/`

**Step 1: Verify nothing imports from these modules**

```bash
grep -r "com\.donohoedigital\.proto\|com\.ddpoker\|com\.donohoedigital\.tools\|com\.donohoedigital\.games\.poker\.network" \
  code/common code/mail code/gui code/db code/server code/udp \
  code/gamecommon code/gameengine code/gameserver code/gametools \
  code/pokerengine code/pokergamecore code/pokergameserver \
  code/poker code/pokerserver code/api \
  --include="*.java" -l
```

Expected: no output (no files found).

**Step 2: Remove the 4 module entries from `code/pom.xml`**

Remove these four lines from the `<modules>` block:

```xml
<module>tools</module>
<module>ddpoker</module>
<module>pokernetwork</module>
```

And from the proto area (search for `proto` — it is not currently in the modules list, so this is a no-op; skip if not found).

The exact lines to remove from `code/pom.xml`:

```xml
    <module>tools</module>
```
```xml
    <module>ddpoker</module>
```
```xml
    <module>pokernetwork</module>
```

**Step 3: Also remove the proto Spotless exclusion from `code/pom.xml`**

Find and remove this line in the `<spotless-maven-plugin>` `<excludes>` block:

```xml
              <exclude>**/proto/**/*.java</exclude>
```

**Step 4: Delete the four directories**

```bash
rm -rf code/proto code/ddpoker code/tools code/pokernetwork
```

**Step 5: Build check**

```bash
cd code && mvn test-compile -P dev 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

**Step 6: Commit**

```bash
git add code/pom.xml
git add -A code/proto code/ddpoker code/tools code/pokernetwork
git commit -m "chore: remove dead modules proto, ddpoker, tools, pokernetwork"
```

---

## Task 3: Remove udp module

`udp` is declared in the parent POM and `gameserver` has an explicit dependency on it. The M7 plan removed all usage, but the module and its dependency were not cleaned up.

**Files:**
- Modify: `code/pom.xml` — remove `<module>udp</module>`
- Modify: `code/gameserver/pom.xml` — remove `udp` dependency block
- Delete directory: `code/udp/`

**Step 1: Verify gameserver has no imports from udp**

```bash
grep -r "com\.donohoedigital\.udp" code/gameserver/src --include="*.java"
```

Expected: no output.

**Step 2: Remove `<module>udp</module>` from `code/pom.xml`**

Remove this line from the `<modules>` block:

```xml
    <module>udp</module>
```

**Step 3: Remove the udp dependency from `code/gameserver/pom.xml`**

Remove this block (lines 68–71):

```xml
    <dependency>
      <groupId>com.donohoedigital</groupId>
      <artifactId>udp</artifactId>
      <version>3.3.0-CommunityEdition</version>
    </dependency>
```

**Step 4: Delete the udp directory**

```bash
rm -rf code/udp
```

**Step 5: Build check**

```bash
cd code && mvn test-compile -P dev 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

**Step 6: Commit**

```bash
git add code/pom.xml code/gameserver/pom.xml
git add -A code/udp
git commit -m "chore: remove udp module and sever gameserver dependency"
```

---

## Task 4: Remove jsp module

`jsp` provides JSP template compilation for the pre-WebSocket email server. It is in the parent POM and `gameserver` depends on it, but nothing in `gameserver`'s Java source imports its classes.

**Files:**
- Modify: `code/pom.xml` — remove `<module>jsp</module>`
- Modify: `code/gameserver/pom.xml` — remove `jsp` dependency block
- Delete directory: `code/jsp/`

**Step 1: Verify gameserver has no imports from jsp**

```bash
grep -r "com\.donohoedigital\.jsp\|jasper\|JspFactory\|PageContext" code/gameserver/src --include="*.java"
```

Expected: no output.

**Step 2: Also verify pokerserver has no direct jsp imports** (it uses email templates via the mail module, not jsp directly)

```bash
grep -r "com\.donohoedigital\.jsp" code/pokerserver/src --include="*.java"
```

Expected: no output. If output appears, note the files but do not block — pokerserver is in-scope for the deferred removal plan.

**Step 3: Remove `<module>jsp</module>` from `code/pom.xml`**

Remove this line from the `<modules>` block:

```xml
    <module>jsp</module>
```

**Step 4: Remove the jsp dependency from `code/gameserver/pom.xml`**

Remove this block (lines 58–61):

```xml
    <dependency>
      <groupId>com.donohoedigital</groupId>
      <artifactId>jsp</artifactId>
      <version>3.3.0-CommunityEdition</version>
    </dependency>
```

**Step 5: Delete the jsp directory**

```bash
rm -rf code/jsp
```

**Step 6: Build check**

```bash
cd code && mvn test-compile -P dev 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

**Step 7: Commit**

```bash
git add code/pom.xml code/gameserver/pom.xml
git add -A code/jsp
git commit -m "chore: remove jsp module and sever gameserver dependency"
```

---

## Task 5: Replace c3p0 with HikariCP in the api module

`DatabaseConfig.java` manually constructs a `ComboPooledDataSource`. Since `api` is a Spring Boot application, the correct approach is to delete the `@Bean DataSource` method and let Spring Boot auto-configure HikariCP from `spring.datasource.*` properties. HikariCP is already on the classpath via `spring-boot-starter-data-jpa`. The `entityManagerFactory` and `transactionManager` beans are retained — they still load `persistence-pokerserver.xml` for entity scanning and will simply receive the auto-configured datasource.

Note: c3p0 is NOT removed from `gameserver/pom.xml` — the legacy `PokerServerMain` process still uses it via `app-context-gameserver.xml`. That cleanup is deferred to the `pokerserver` removal plan.

**Files:**
- Modify: `code/api/src/main/java/com/donohoedigital/poker/api/config/DatabaseConfig.java`
- Modify: `code/api/src/main/resources/application.properties`

**Step 1: Update `DatabaseConfig.java`**

Replace the entire file content with the following (removes the `@Bean DataSource` method, its `@Value` fields, and the c3p0/PropertyVetoException imports; retains `entityManagerFactory` and `transactionManager`):

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.poker.api.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for DD Poker API. Uses Spring Boot auto-configured
 * HikariCP datasource; wires the legacy persistence XML for entity scanning.
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {"com.donohoedigital.games.server.service", "com.donohoedigital.games.server.dao",
        "com.donohoedigital.games.poker.service", "com.donohoedigital.games.poker.dao"})
@EnableJpaRepositories(basePackages = "com.donohoedigital.games.poker.gameserver.persistence.repository")
public class DatabaseConfig {

    @Value("${jpa.persistence.location:classpath:META-INF/persistence-pokerserver.xml}")
    private String persistenceLocation;

    @Value("${jpa.persistence.name:poker}")
    private String persistenceName;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPersistenceXmlLocation(persistenceLocation);
        em.setPersistenceUnitName(persistenceName);

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        em.setJpaVendorAdapter(vendorAdapter);

        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "false");
        em.setJpaProperties(properties);

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
}
```

**Step 2: Update `application.properties`**

Replace the `db.*` custom keys with standard `spring.datasource.*` keys and add HikariCP pool settings. Remove the old `db.*` block and add the following in its place:

Remove:
```properties
# Database Configuration
db.driver=${DB_DRIVER:org.h2.Driver}
db.url=${DB_URL:jdbc:h2:/data/poker}
db.user=${DB_USER:sa}
db.password=${DB_PASSWORD:}
```

Add:
```properties
# Database Configuration (HikariCP — Spring Boot auto-configured)
spring.datasource.driver-class-name=${DB_DRIVER:org.h2.Driver}
spring.datasource.url=${DB_URL:jdbc:h2:/data/poker}
spring.datasource.username=${DB_USER:sa}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.idle-timeout=21600000
```

Also keep the `jpa.persistence.location` and `jpa.persistence.name` keys — they are still read by `DatabaseConfig.java`.

**Step 3: Run api tests**

```bash
cd code && mvn test -pl api -P dev 2>&1 | tail -20
```

Expected: `BUILD SUCCESS` with all api tests passing. The Spring context must load without a `NoSuchBeanDefinitionException` or `DataSourceProperties` error.

**Step 4: Run full build**

```bash
cd code && mvn test -P dev 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

**Step 5: Commit**

```bash
git add code/api/src/main/java/com/donohoedigital/poker/api/config/DatabaseConfig.java
git add code/api/src/main/resources/application.properties
git commit -m "chore(api): replace c3p0 with Spring Boot HikariCP auto-configuration"
```

---

## Task 6: Final verification

**Step 1: Full build and test**

```bash
cd code && mvn test -P dev 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`. All module tests pass.

**Step 2: Confirm deleted modules are gone**

```bash
ls code/ | sort
```

Expected output should NOT include: `pokerwicket`, `wicket`, `proto`, `ddpoker`, `tools`, `pokernetwork`, `udp`, `jsp`

**Step 3: Confirm parent POM modules list**

```bash
grep "<module>" code/pom.xml
```

Expected modules: `common`, `mail`, `gui`, `db`, `server`, `gamecommon`, `gameengine`, `gameserver`, `gametools`, `pokerengine`, `pokergamecore`, `pokergameserver`, `poker`, `pokerserver`, `api`

**Step 4: Confirm no c3p0 in api module's effective classpath**

```bash
cd code && mvn dependency:tree -pl api | grep c3p0
```

c3p0 will still appear as a transitive dependency (via `api → pokerserver → gameserver → c3p0`) — this is expected and harmless. What matters is that `DatabaseConfig.java` no longer instantiates it.

**Step 5: Commit cleanup summary if any stray files remain**

```bash
git status
```

If clean: no action needed. If any stray files remain, add and commit them.
