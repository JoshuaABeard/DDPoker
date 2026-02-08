# DD Poker Docker Deployment Plan (Final)

## Implementation Status

**Phase 1: COMPLETE** ✅ — All H2 migration code changes done and Maven build verified.
**Phase 2: COMPLETE** ✅ — Docker image built and container running successfully.
**Phase 3: COMPLETE** ✅ — End-to-end verification passed (services running, website accessible, data persists).
**Phase 4: IN PROGRESS** — Documentation update.

### What was done:

**Phase 1 (H2 Migration):**
- H2 dependency added to `code/gameserver/pom.xml` (after HSQLDB, line ~93)
- Datasource parameterized in `app-context-gameserver.xml` (uses `${db.driver}`, `${db.url}`, `${db.user}`, `${db.password}`)
- H2 defaults added to `application.properties` and `pokerserver.properties`
- H2 schema init script created at `code/gameserver/src/main/resources/h2-init.sql`
- SQL aliases fixed in 3 DAO files (`'alias'` → `AS alias`)
- Also fixed additional quoted aliases in TournamentHistoryImplJpa: `'rank1'` → `AS rank1`, `'roi'` → `AS roi`
- Full Maven build (all 22 modules) verified passing

**Phase 2 (Docker Build):**
- PokerJetty.java updated: configurable war path (`-Dpokerweb.war.path`), port (`-Dpokerweb.port`), daemon mode (`-Dpokerweb.daemon=true`)
- `docker/entrypoint.sh` created with dual-process management, signal handling, and WORK directory setup
- `Dockerfile` created (at repo root) — copies compiled classes + dependency JARs, excludes project JARs to avoid conflicts
- `docker-compose.yml` created with all 4 ports mapped and named volume
- Maven dependency plugin configured in `pokerwicket/pom.xml` to copy test-scoped dependencies (Jetty)
- Fixed line endings issue (Windows CRLF → Unix LF) for shell scripts
- Runtime messages files copied to container
- Docker image built successfully with eclipse-temurin:25-jre base

**Phase 3 (Verification):**
- Container starts both pokerserver and pokerweb processes successfully
- PokerServer listening on TCP:8877, UDP:11886, UDP:11889
- Pokerweb (Jetty) running on port 8080 with Wicket 10.8.0 in DEPLOYMENT mode
- Website accessible at http://localhost:8080/online
- H2 database initialized and persisting data in Docker volume
- Data persistence verified across container restarts
- Client configuration documentation created (`guidelines/CLIENT-CONFIGURATION.md`)

### To resume implementation:
1. Ensure Docker Desktop is running
2. Set PATH: `export PATH="/c/Program Files/Docker/Docker/resources/bin:/c/Tools/apache-maven-3.9.12/bin:$PATH"`
3. Set JAVA_HOME: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.2.10-hotspot"`
4. Run: `cd "C:/Repos/DDPoker" && docker compose build`
5. Run: `docker compose up` to test
6. Proceed to Phase 3 verification, then Phase 4 documentation

### Key file paths for dependency copy (needed before Docker build):
```shell
cd "C:/Repos/DDPoker/code"
mvn install -DskipTests=true
mvn dependency:copy-dependencies -pl pokerserver,pokerwicket -DoutputDirectory=target/dependency -DincludeScope=runtime
```

---

## Context

We want a **single Docker container** that runs the entire DD Poker server stack — no external database, no multi-container compose. The long-term goal is publishing this as an **Unraid community app**.

Two major changes from the original codebase:
1. **Replace MySQL with H2 embedded database** (default, MySQL still available via env vars)
2. **Bundle pokerserver + pokerweb in one container** (two Java processes, shell entrypoint)

## Architecture

```
  Desktop Client (native Windows)
       │ HTTP :8877, UDP :11886/:11889
       ▼
┌──────────────────────────────────────────────┐
│  Single Docker Container                     │
│                                              │
│  ┌──────────────┐     ┌────────────────┐     │
│  │ pokerserver  │     │   pokerweb     │     │
│  │ :8877 (HTTP) │     │ :8080 (Jetty)  │     │
│  │ :11886 (UDP) │     │ Wicket web UI  │     │
│  │ :11889 (UDP) │     │                │     │
│  └──────┬───────┘     └───────┬────────┘     │
│         │                     │              │
│         ▼                     ▼              │
│  ┌────────────────────────────────────┐      │
│  │   H2 Embedded Database (default)   │      │
│  │   file:/data/poker                 │      │
│  └────────────────────────────────────┘      │
└──────────────────────────────────────────────┘
         │
         ▼ (Docker volume)
    /data/poker.mv.db  (persistent)
```

---

# Phase 1: H2 Database Migration

**Goal:** Make the datasource fully configurable via environment variables, with H2 as the default. Keep MySQL as an option. Fix SQL compatibility issues.

**Milestone:** `mvn package -DskipTests` succeeds with H2 on the classpath. App can start locally against H2.

## Step 1.1 — Add H2 dependency

**File:** `code/gameserver/pom.xml`

Add H2 database dependency alongside existing MySQL connector:

```xml
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <version>2.3.232</version>
  <scope>runtime</scope>
</dependency>
```

Keep the existing `mysql-connector-j` dependency — MySQL remains available for anyone who wants it.

## Step 1.2 — Make datasource configurable via environment variables

**File:** `code/gameserver/src/main/resources/app-context-gameserver.xml`

Change the hardcoded MySQL datasource to use property placeholders that can be overridden by env vars.

**Current (hardcoded MySQL):**
```xml
<property name="driverClass" value="com.mysql.cj.jdbc.Driver"/>
<property name="jdbcUrl" value="jdbc:mysql://${db.host}/poker?useUnicode=true&amp;characterEncoding=UTF8&amp;useSSL=false"/>
<property name="user" value="poker"/>
<property name="password" value="p0k3rdb!"/>
```

**New (env-var driven, H2 default):**
```xml
<property name="driverClass" value="${db.driver}"/>
<property name="jdbcUrl" value="${db.url}"/>
<property name="user" value="${db.user}"/>
<property name="password" value="${db.password}"/>
```

**File:** `code/gameserver/src/main/resources/application.properties`

Update to set H2 defaults:
```properties
db.host=${DB_HOST:127.0.0.1}
db.driver=${DB_DRIVER:org.h2.Driver}
db.url=${DB_URL:jdbc:h2:file:./data/poker;MODE=MySQL;AUTO_SERVER=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-init.sql'}
db.user=${DB_USER:sa}
db.password=${DB_PASSWORD:}
jpa.persistence.location=classpath:META-INF/persistence-gameserver.xml
jpa.persistence.name=registration
```

**File:** `code/pokerserver/src/main/resources/pokerserver.properties`

Update to match:
```properties
app.name=poker
app.type=SERVER
db.host=${DB_HOST:127.0.0.1}
db.driver=${DB_DRIVER:org.h2.Driver}
db.url=${DB_URL:jdbc:h2:file:./data/poker;MODE=MySQL;AUTO_SERVER=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-init.sql'}
db.user=${DB_USER:sa}
db.password=${DB_PASSWORD:}
jpa.persistence.location=classpath:META-INF/persistence-pokerserver.xml
jpa.persistence.name=poker
```

**To switch back to MySQL**, users set env vars:
```
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_URL=jdbc:mysql://myhost/poker?useUnicode=true&characterEncoding=UTF8&useSSL=false
DB_USER=poker
DB_PASSWORD=p0k3rdb!
```

## Step 1.3 — Create H2-compatible schema init script

**File:** `code/gameserver/src/main/resources/h2-init.sql`

Place on the classpath so H2's `INIT=RUNSCRIPT FROM 'classpath:h2-init.sql'` can find it. Uses `IF NOT EXISTS` on every statement so it's safe to run on every startup (no-op after first time).

Adapted from `tools/db/create_tables.sql`:
- `INT UNSIGNED` → `INT`
- `SMALLINT UNSIGNED` → `SMALLINT`
- Remove `Engine = InnoDB DEFAULT CHARSET=utf8`
- Add `CREATE TABLE IF NOT EXISTS` on all tables
- Add `CREATE INDEX IF NOT EXISTS` on all indexes

Full content:

```sql
-- H2-compatible schema for DD Poker (auto-init on first run)
-- Adapted from tools/db/create_tables.sql

CREATE TABLE IF NOT EXISTS wan_profile (
    wpr_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wpr_name VARCHAR(32) NOT NULL,
    wpr_license_key VARCHAR(55) NOT NULL,
    wpr_email VARCHAR(255) NOT NULL,
    wpr_password VARCHAR(255) NOT NULL,
    wpr_is_activated BOOLEAN NOT NULL,
    wpr_is_retired BOOLEAN NOT NULL,
    wpr_create_date DATETIME NOT NULL,
    wpr_modify_date DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS wpr_name ON wan_profile(wpr_name);
CREATE INDEX IF NOT EXISTS wpr_email ON wan_profile(wpr_email);

CREATE TABLE IF NOT EXISTS wan_game (
    wgm_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wgm_license_key VARCHAR(55) NOT NULL,
    wgm_url VARCHAR(64) NOT NULL,
    wgm_host_player VARCHAR(64) NOT NULL,
    wgm_start_date DATETIME NULL,
    wgm_end_date DATETIME NULL,
    wgm_create_date DATETIME NOT NULL,
    wgm_modify_date DATETIME NOT NULL,
    wgm_mode TINYINT NOT NULL,
    wgm_tournament_data TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS wgm_license_key ON wan_game(wgm_license_key, wgm_url);
CREATE INDEX IF NOT EXISTS wgm_host_player ON wan_game(wgm_host_player);
CREATE INDEX IF NOT EXISTS wgm_modify_date ON wan_game(wgm_modify_date);
CREATE INDEX IF NOT EXISTS wgm_end_date ON wan_game(wgm_end_date);
CREATE INDEX IF NOT EXISTS wgm_create_date_mode ON wan_game(wgm_create_date, wgm_mode);
CREATE INDEX IF NOT EXISTS wgm_end_date_mode ON wan_game(wgm_end_date, wgm_mode);
CREATE INDEX IF NOT EXISTS wgm_mode ON wan_game(wgm_mode);

CREATE TABLE IF NOT EXISTS wan_history (
    whi_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    whi_game_id INT NOT NULL,
    whi_tournament_name VARCHAR(255) NOT NULL,
    whi_num_players INT NOT NULL,
    whi_is_ended BOOL NOT NULL,
    whi_profile_id INT NOT NULL,
    whi_player_name VARCHAR(32) NOT NULL,
    whi_player_type TINYINT NOT NULL,
    whi_finish_place SMALLINT NOT NULL,
    whi_prize DECIMAL NOT NULL,
    whi_buy_in DECIMAL NOT NULL,
    whi_total_rebuy DECIMAL NOT NULL,
    whi_total_add_on DECIMAL NOT NULL,
    whi_rank_1 DECIMAL(10,3) NOT NULL,
    whi_disco DECIMAL(10,0) NOT NULL,
    whi_end_date DATETIME NOT NULL,
    FOREIGN KEY (whi_game_id) REFERENCES wan_game(wgm_id),
    FOREIGN KEY (whi_profile_id) REFERENCES wan_profile(wpr_id)
);
CREATE INDEX IF NOT EXISTS whi_end_date ON wan_history(whi_end_date);
CREATE INDEX IF NOT EXISTS whi_player_type ON wan_history(whi_player_type);
CREATE INDEX IF NOT EXISTS whi_is_ended ON wan_history(whi_is_ended);

CREATE TABLE IF NOT EXISTS registration (
    reg_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reg_license_key VARCHAR(55) NOT NULL,
    reg_product_version VARCHAR(32) NOT NULL,
    reg_ip_address VARCHAR(16) NOT NULL,
    reg_host_name VARCHAR(255) NULL,
    reg_host_name_modified VARCHAR(255) NULL,
    reg_port SMALLINT NULL,
    reg_server_time DATETIME NOT NULL,
    reg_java_version VARCHAR(32) NULL,
    reg_os VARCHAR(32) NULL,
    reg_type TINYINT NOT NULL,
    reg_is_duplicate BOOL NOT NULL,
    reg_is_ban_attempt BOOL NOT NULL,
    reg_name VARCHAR(100) NULL,
    reg_email VARCHAR(255) NULL,
    reg_address VARCHAR(255) NULL,
    reg_city VARCHAR(50) NULL,
    reg_state VARCHAR(50) NULL,
    reg_postal VARCHAR(50) NULL,
    reg_country VARCHAR(120) NULL
);
CREATE INDEX IF NOT EXISTS reg_address ON registration(reg_address);
CREATE INDEX IF NOT EXISTS reg_email ON registration(reg_email);
CREATE INDEX IF NOT EXISTS reg_host_name_modified ON registration(reg_host_name_modified);
CREATE INDEX IF NOT EXISTS reg_ip_address ON registration(reg_ip_address);
CREATE INDEX IF NOT EXISTS reg_is_duplicate ON registration(reg_is_duplicate);
CREATE INDEX IF NOT EXISTS reg_name ON registration(reg_name);
CREATE INDEX IF NOT EXISTS reg_license_key ON registration(reg_license_key);
CREATE INDEX IF NOT EXISTS reg_type ON registration(reg_type);

CREATE TABLE IF NOT EXISTS banned_key (
    ban_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ban_key VARCHAR(255) NOT NULL,
    ban_until DATE NOT NULL,
    ban_comment VARCHAR(128) NULL,
    ban_create_date DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ban_key ON banned_key(ban_key);

CREATE TABLE IF NOT EXISTS upgraded_key (
    upg_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    upg_license_key VARCHAR(55) NOT NULL,
    upg_count SMALLINT NOT NULL,
    upg_create_date DATETIME NOT NULL,
    upg_modify_date DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS upg_license_key ON upgraded_key(upg_license_key);
```

## Step 1.4 — Fix native SQL column alias syntax (3 files)

H2 in MySQL mode doesn't support the `count(x) 'alias'` syntax. Use standard `AS` syntax (also valid MySQL).

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/dao/impl/OnlineGameImplJpa.java`
- Find: `count(wgm_id) 'cnt'`
- Replace: `count(wgm_id) AS cnt`

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/dao/impl/TournamentHistoryImplJpa.java`
- Find: `count(whi_profile_id) 'gamesplayed'`
- Replace: `count(whi_profile_id) AS gamesplayed`
- Also look for other quoted aliases in the same file

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/dao/impl/OnlineProfileImplJpa.java`
- Find: `count(whi_id) 'num'`
- Replace: `count(whi_id) AS num`
- Also look for other quoted aliases in the same file

**Note:** Search all DAO impl files for the pattern `') '` or `' '` to catch any other quoted aliases.

## Step 1.5 — Rebuild and verify

```shell
cd C:\Repos\DDPoker\code
mvn package -DskipTests=true
```

All 22 modules must build successfully with H2 on the classpath.

**Phase 1 Milestone Complete:** H2 dependency added, datasource configurable, schema script created, SQL compatibility fixed, project builds.

---

# Phase 2: Single Container Docker Build

**Goal:** Create a single Dockerfile that bundles pokerserver + pokerweb + H2, runs with one command.

**Milestone:** `docker compose up --build` starts the container. Both processes run, H2 initializes, ports respond.

## Step 2.1 — Create entrypoint script

**File:** `docker/entrypoint.sh`

```bash
#!/bin/bash
set -e

# ============================================================
# DD Poker Combined Server Entrypoint
# Starts pokerserver and pokerweb as separate Java processes
# ============================================================

APP_DIR=/app
DATA_DIR=/data

# Ensure data directory exists
mkdir -p "$DATA_DIR"

# Build classpath from all module JARs and classes
# The Dockerfile copies everything into /app/lib/ and /app/classes/
CLASSPATH="$APP_DIR/classes"
for jar in $APP_DIR/lib/*.jar; do
  CLASSPATH="$CLASSPATH:$jar"
done

export CLASSPATH

# Common JVM options
JAVA_OPTS="-server -Dfile.encoding=UTF-8"

echo "============================================"
echo "  DD Poker Server Starting"
echo "============================================"
echo "  Data directory: $DATA_DIR"
echo "  DB Driver: ${DB_DRIVER:-org.h2.Driver}"
echo "============================================"

# Start pokerserver (background)
echo "[entrypoint] Starting pokerserver..."
java $JAVA_OPTS -Xms24m -Xmx96m \
  -cp "$CLASSPATH" \
  com.donohoedigital.games.poker.server.PokerServerMain &
SERVER_PID=$!
echo "[entrypoint] pokerserver PID: $SERVER_PID"

# Brief pause to let server initialize Spring context first
sleep 3

# Start pokerweb via embedded Jetty (background)
echo "[entrypoint] Starting pokerweb (Jetty)..."
java $JAVA_OPTS -Xms24m -Xmx96m \
  -Dwicket.configuration=deployment \
  -cp "$CLASSPATH" \
  com.donohoedigital.games.poker.wicket.PokerJetty &
WEB_PID=$!
echo "[entrypoint] pokerweb PID: $WEB_PID"

echo "[entrypoint] Both processes started. Waiting..."

# Trap signals for graceful shutdown
shutdown() {
  echo "[entrypoint] Shutting down..."
  kill $SERVER_PID $WEB_PID 2>/dev/null
  wait $SERVER_PID $WEB_PID 2>/dev/null
  echo "[entrypoint] Shutdown complete."
  exit 0
}
trap shutdown SIGTERM SIGINT

# Wait for either process to exit
# If one dies, stop the other and exit
wait -n $SERVER_PID $WEB_PID
EXIT_CODE=$?
echo "[entrypoint] A process exited with code $EXIT_CODE. Stopping remaining..."
kill $SERVER_PID $WEB_PID 2>/dev/null
wait $SERVER_PID $WEB_PID 2>/dev/null
exit $EXIT_CODE
```

## Step 2.2 — Create combined Dockerfile

**File:** `Dockerfile.ddpoker.docker`

```dockerfile
# ============================================================
# DD Poker - Combined Server Container
# Runs pokerserver + pokerweb + H2 embedded database
# ============================================================
FROM eclipse-temurin:25-jre

LABEL maintainer="DD Poker Docker"
LABEL description="DD Poker server with embedded H2 database"

# Create app and data directories
RUN mkdir -p /app/lib /app/classes /data

WORKDIR /app

# Copy compiled classes from all modules
# These are the target/classes directories from each module
COPY code/common/target/classes/ /app/classes/
COPY code/mail/target/classes/ /app/classes/
COPY code/gui/target/classes/ /app/classes/
COPY code/installer/target/classes/ /app/classes/
COPY code/db/target/classes/ /app/classes/
COPY code/wicket/target/classes/ /app/classes/
COPY code/jsp/target/classes/ /app/classes/
COPY code/server/target/classes/ /app/classes/
COPY code/udp/target/classes/ /app/classes/
COPY code/gamecommon/target/classes/ /app/classes/
COPY code/gameengine/target/classes/ /app/classes/
COPY code/ddpoker/target/classes/ /app/classes/
COPY code/pokerengine/target/classes/ /app/classes/
COPY code/pokernetwork/target/classes/ /app/classes/
COPY code/tools/target/classes/ /app/classes/
COPY code/gameserver/target/classes/ /app/classes/
COPY code/pokerserver/target/classes/ /app/classes/

# Copy pokerweb classes (both main and test — PokerJetty is in test)
COPY code/pokerwicket/target/classes/ /app/classes/
COPY code/pokerwicket/target/test-classes/ /app/classes/

# Copy the webapp directory (needed by embedded Jetty)
COPY code/pokerwicket/src/main/webapp/ /app/webapp/

# Copy all dependency JARs
# We use a helper script to copy from Maven local repo based on classpath.txt
COPY docker/copy-deps.sh /tmp/copy-deps.sh
COPY code/pokerserver/target/classpath.txt /tmp/pokerserver-classpath.txt
COPY code/pokerwicket/target/classpath.txt /tmp/pokerwicket-classpath.txt
RUN chmod +x /tmp/copy-deps.sh && /tmp/copy-deps.sh && rm -f /tmp/copy-deps.sh /tmp/*-classpath.txt

# Copy entrypoint
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Environment defaults (H2 embedded database)
ENV DB_DRIVER=org.h2.Driver
ENV DB_URL="jdbc:h2:file:/data/poker;MODE=MySQL;AUTO_SERVER=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-init.sql'"
ENV DB_USER=sa
ENV DB_PASSWORD=

# Ports
EXPOSE 8877
EXPOSE 8080
EXPOSE 11886/udp
EXPOSE 11889/udp

# Persistent data volume
VOLUME /data

ENTRYPOINT ["/app/entrypoint.sh"]
```

## Step 2.3 — Create dependency copy helper script

**File:** `docker/copy-deps.sh`

This script reads both `classpath.txt` files, deduplicates, and copies all JAR files to `/app/lib/`.

```bash
#!/bin/bash
set -e

# Merge and deduplicate JAR paths from both classpath.txt files
# classpath.txt is colon-separated list of absolute paths to JARs in ~/.m2/repository
cat /tmp/pokerserver-classpath.txt /tmp/pokerwicket-classpath.txt \
  | tr ':' '\n' \
  | sort -u \
  | grep '\.jar$' \
  | while read jar; do
    if [ -f "$jar" ]; then
      cp "$jar" /app/lib/
    fi
  done

echo "Copied $(ls /app/lib/*.jar 2>/dev/null | wc -l) JARs to /app/lib/"
```

**Important Note:** The `classpath.txt` files reference JARs in the local Maven repository (`~/.m2/repository/...`). These paths won't exist inside the Docker build context. We have two options:

**Option A (simpler):** Use `maven-dependency-plugin` to copy all runtime deps into `target/dependency/` for each module, then COPY that directory. This is the standard Docker approach.

**Option B (what we'll actually do):** Add a Maven build step that copies all deps to a staging directory before Docker build. We'll add this to the build instructions.

**Revised approach for Step 2.3:** Instead of the copy script, we'll use Maven's `dependency:copy-dependencies` goal and adjust the Dockerfile to copy from `target/dependency/`.

Add to parent `code/pom.xml` (or run as a separate Maven command):
```shell
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -DincludeScope=runtime
```

Then the Dockerfile simplifies — copy `target/dependency/*.jar` from each module to `/app/lib/`. In practice, the pokerserver and pokerwicket modules transitively include all deps, so we only need to copy from those two modules' `target/dependency/` directories.

## Step 2.4 — Adjust PokerJetty webapp path

**File:** `code/pokerwicket/src/test/java/com/donohoedigital/games/poker/wicket/PokerJetty.java`

The current code hardcodes the webapp path:
```java
bb.setWar("code/pokerwicket/src/main/webapp");
```

This won't work in the Docker container. Make it configurable:
```java
String warPath = System.getProperty("pokerweb.war.path", "code/pokerwicket/src/main/webapp");
bb.setWar(warPath);
```

In the Docker entrypoint, add:
```
-Dpokerweb.war.path=/app/webapp
```

## Step 2.5 — Create docker-compose.yml

**File:** `docker-compose.yml`

```yaml
services:
  ddpoker:
    build:
      context: .
      dockerfile: Dockerfile.ddpoker.docker
    ports:
      - "8877:8877"
      - "8080:8080"
      - "11886:11886/udp"
      - "11889:11889/udp"
    volumes:
      - ddpoker_data:/data
    # To switch to MySQL, uncomment and configure:
    # environment:
    #   DB_DRIVER: com.mysql.cj.jdbc.Driver
    #   DB_URL: "jdbc:mysql://your-mysql-host/poker?useUnicode=true&characterEncoding=UTF8&useSSL=false"
    #   DB_USER: poker
    #   DB_PASSWORD: "p0k3rdb!"

volumes:
  ddpoker_data:
```

## Step 2.6 — Build and test

```shell
# 1. Build Java project
cd C:\Repos\DDPoker
source ddpoker.rc
mvn-package-notests

# 2. Copy dependencies to target/dependency for Docker
cd code
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -DincludeScope=runtime
cd ..

# 3. Build and run container
docker compose up --build
```

**Phase 2 Milestone Complete:** Container starts, both processes running, H2 database initialized, ports responding.

---

# Phase 3: Verification & Client Connection

**Goal:** Confirm end-to-end functionality. Website loads, game client connects, chat works.

**Milestone:** Full stack operational. Client can create an online profile and see the game lobby.

## Step 3.1 — Verify container logs

After `docker compose up --build`, check for:
- `[entrypoint] Starting pokerserver...` — process launched
- `[entrypoint] Starting pokerweb (Jetty)...` — process launched
- Spring context initialization (no exceptions)
- `PokerServer` started messages
- Jetty `Started` message on port 8080
- H2 database file created at `/data/poker.mv.db`

## Step 3.2 — Verify website

Browse to http://localhost:8080/online

Expected: DD Poker Online Portal loads with empty game/player lists.

## Step 3.3 — Run and configure desktop client

```shell
source ddpoker.rc
poker
```

In the game client:
1. Go to **Options** → **Online** → **Public Online Servers**
2. Check the **Enable** checkbox
3. Set **Online Server:** `localhost:8877`
4. Set **Chat Server:** `localhost:11886`
5. Click **Test** to verify connection

## Step 3.4 — Test online profile creation

In the game client:
1. Go to **Online** → **Create Profile**
2. Enter a name and email
3. Profile should be created (note: no email sent since SMTP is disabled)

## Step 3.5 — Verify data persistence

```shell
docker compose down
docker compose up
```

Data should persist. Previously created profiles should still exist.

```shell
docker compose down -v
docker compose up
```

Data should be wiped. Fresh empty database.

**Phase 3 Milestone Complete:** Full end-to-end verification passed.

---

# Phase 4: Documentation Update

**Goal:** Update guidelines with final Docker instructions and architecture decisions.

**Milestone:** `guidelines/DDPOKER-DOCKER.md` is current and accurate.

## Step 4.1 — Update DDPOKER-DOCKER.md

Rewrite `guidelines/DDPOKER-DOCKER.md` with:
- Updated architecture diagram
- H2 database explanation and MySQL fallback instructions
- Build and run commands
- Port reference table
- Client configuration steps
- Troubleshooting section
- Unraid future plans

## Step 4.2 — Update DDPOKER-OVERVIEW.md

Add H2 to the tech stack table. Note the database abstraction layer.

## Step 4.3 — Update DDPOKER-BUILD-SETUP.md

Add Docker build steps to the build workflow section.

**Phase 4 Milestone Complete:** All documentation is current.

---

# Summary: All Files

## New Files (5-6)
| File | Phase | Purpose |
|------|-------|---------|
| `code/gameserver/src/main/resources/h2-init.sql` | 1 | H2-compatible schema with IF NOT EXISTS |
| `docker/entrypoint.sh` | 2 | Dual-process launcher with signal handling |
| `docker/copy-deps.sh` | 2 | (may not be needed if using mvn dependency:copy-dependencies) |
| `Dockerfile.ddpoker.docker` | 2 | Single combined container |
| `docker-compose.yml` | 2 | Convenience compose file |

## Modified Files (7-8)
| File | Phase | Change |
|------|-------|--------|
| `code/gameserver/pom.xml` | 1 | Add H2 dependency |
| `code/gameserver/src/main/resources/app-context-gameserver.xml` | 1 | Parameterize datasource |
| `code/gameserver/src/main/resources/application.properties` | 1 | Add db.driver, db.url, db.user, db.password defaults |
| `code/pokerserver/src/main/resources/pokerserver.properties` | 1 | Add db.driver, db.url, db.user, db.password defaults |
| `code/pokerserver/.../OnlineGameImplJpa.java` | 1 | Fix SQL alias: `'cnt'` → `AS cnt` |
| `code/pokerserver/.../TournamentHistoryImplJpa.java` | 1 | Fix SQL alias: `'gamesplayed'` → `AS gamesplayed` |
| `code/pokerserver/.../OnlineProfileImplJpa.java` | 1 | Fix SQL alias: `'num'` → `AS num` |
| `code/pokerwicket/.../PokerJetty.java` | 2 | Make webapp path configurable via system property |

## Unchanged
| File | Notes |
|------|-------|
| `Dockerfile.pokerweb.docker` | Preserved for standalone MySQL usage |
| `Dockerfile.ubuntu.docker` | Preserved for dev/X11 usage |
| `tools/db/*` | Preserved for standalone MySQL setup |
| All JPA entity classes | Standard annotations, no changes needed |

---

# Environment Variables Reference

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_DRIVER` | `org.h2.Driver` | JDBC driver class |
| `DB_URL` | `jdbc:h2:file:/data/poker;MODE=MySQL;AUTO_SERVER=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-init.sql'` | JDBC connection URL |
| `DB_USER` | `sa` | Database username |
| `DB_PASSWORD` | (empty) | Database password |
| `DB_HOST` | `127.0.0.1` | MySQL host (only used when switching to MySQL) |

---

# Ports Reference

| Port | Protocol | Service | Purpose |
|------|----------|---------|---------|
| 8877 | TCP | pokerserver | Game API — desktop client connects here |
| 8080 | TCP | pokerweb | Website — browse to http://host:8080/online |
| 11886 | UDP | pokerserver | Chat server — client chat lobby |
| 11889 | UDP | pokerserver | Connection test — client verifies connectivity |
