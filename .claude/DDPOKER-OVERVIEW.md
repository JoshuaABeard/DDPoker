# DD Poker - Project Overview

## About

DD Poker is a full-featured Texas Hold'em poker simulator originally developed by Donohoe Digital LLC (2003-2017), now open-sourced under GPLv3. It consists of three main components:

1. **DD Poker Game** - Java Swing desktop application (client)
2. **Poker Server** - Backend API + chat server (Spring-based, embedded H2 database)
3. **Poker Web** - Apache Wicket website / "Online Portal" (embedded Jetty server)

## Tech Stack

| Component         | Technology                          | Version        |
|-------------------|-------------------------------------|----------------|
| Language          | Java (OpenJDK)                      | 25             |
| Build             | Apache Maven                        | 3.9.12         |
| Desktop UI        | Java Swing                          | (JDK built-in) |
| Web Framework     | Apache Wicket                       | 10.8.0         |
| ORM               | Hibernate                           | 6.6.42.Final   |
| App Server        | Embedded Jetty                      | 12.1.6         |
| Database          | H2 Embedded                         | 2.3.232        |
| Logging           | Log4j2                              | 2.25.3         |
| Spring Framework  | Spring                              | 6.2.15         |
| Tests             | JUnit                               | 4.13.2         |
| Servlet API       | Jakarta Servlet                     | 6.1.0          |
| Container         | Docker (eclipse-temurin:25-jre)     | Latest         |

## Module Structure

The project is a Maven multi-module build with 22 modules. Build order (later modules depend on earlier ones):

| Module           | Description                                        | Artifact |
|------------------|----------------------------------------------------|----------|
| `common`         | Core config, logging, XML, properties, utils       | jar      |
| `mail`           | Email sending tools                                | jar      |
| `gui`            | GUI infrastructure extending Java Swing            | jar      |
| `installer`      | Custom installer logic                             | jar      |
| `db`             | Database infrastructure extending Hibernate        | jar      |
| `wicket`         | Web infrastructure extending Apache Wicket         | jar      |
| `jsp`            | JSP-based email/file generation                    | jar      |
| `server`         | Core server functionality                          | jar      |
| `udp`            | Core UDP networking                                | jar      |
| `gamecommon`     | Core game utilities (shared client/server)         | jar      |
| `gameengine`     | Core game engine                                   | jar      |
| `ddpoker`        | Classes in `com.ddpoker` package                   | jar      |
| `pokerengine`    | Core poker utilities (shared client/server)        | jar      |
| `pokernetwork`   | Poker networking infrastructure (shared)           | jar      |
| `poker`          | DD Poker UI / desktop client                       | jar      |
| `tools`          | Misc business tools                                | jar      |
| `gameserver`     | Core game server                                   | jar      |
| `pokerserver`    | DD Poker backend server                            | jar      |
| `gametools`      | Game building tools (border/territory managers)    | jar      |
| `pokerwicket`    | DD Poker website and Online Portal                 | war      |
| `proto`          | Prototype / experiment code                        | jar      |

## Key Entry Points

| Component     | Main Class         | Script        |
|---------------|--------------------|---------------|
| Poker Game    | `PokerMain`        | `poker`       |
| Poker Server  | `PokerServerMain`  | `pokerserver` |
| Poker Website | `PokerJetty`       | `pokerweb`    |

## Server Dependencies

### Database

**H2 Embedded** - File-based database, no setup required
- Automatic schema initialization
- MySQL compatibility mode for SQL queries
- File-based storage in Docker volume (`/data/poker.mv.db`)
- Zero configuration required

### Other Dependencies

- **SMTP** - For sending profile registration emails (optional). Configured in `poker/server.properties`.

## Configuration System

The project uses a layered properties system:

- **LoggingConfig** - `log4j2.[apptype].properties` files loaded by app type (client/webapp/server/cmdline)
- **PropertyConfig** - `[appname]/[apptype].properties` files, with per-user overrides via `[username].properties`
- **Debug Settings** - Controlled via `settings.debug.*` entries in user-specific properties files
- **Database Configuration** - Environment variables (H2 defaults are automatic):
  - `DB_DRIVER` - JDBC driver class (default: `org.h2.Driver`)
  - `DB_URL` - JDBC connection URL (default: `jdbc:h2:file:/data/poker`)
  - `DB_USER` - Database username (default: `sa`)
  - `DB_PASSWORD` - Database password (default: empty)

## Repository Layout

```
ddpoker/
  code/                    # All Java source (Maven multi-module)
    pom.xml                # Parent POM
    common/                # Through proto/ (22 modules)
  docs/                    # Documentation (AI whitepaper, etc.)
  images/                  # Screenshots, logos
  runtime/                 # Runtime files (messages, logs)
  tools/
    bin/                   # Shell scripts (poker, pokerserver, pokerweb, runjava, etc.)
    db/                    # Database scripts (create_dbs.sql, reset_dbs.sh, etc.)
  guidelines/              # Project guidelines and documentation
  docker/                  # Docker-related files
    docker-compose.yml     # Docker Compose configuration
    Dockerfile             # Container image definition (pokerserver + pokerweb + H2)
    entrypoint.sh          # Container startup script (dual-process manager)
  ddpoker.rc               # Environment setup script (aliases, PATH, JAVA_HOME)
```

## Known Quirks

- **Password handling** - Server stores reversibly-encrypted passwords and can email them in plaintext (legacy design).
- **HSQLDB** - Pinned at 1.8.0.10 (latest is 2.7.4) to avoid database migration.
- **Test coverage** - Acknowledged as lacking, especially in core poker logic.
- **Embedded H2 database** - No passwords needed, automatic setup.
