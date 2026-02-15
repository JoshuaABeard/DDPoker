# DD Poker - Project Overview

## About

DD Poker is a full-featured Texas Hold'em poker simulator originally developed by Donohoe Digital LLC (2003-2017), now open-sourced under GPLv3. It consists of three main components:

1. **DD Poker Game** - Java Swing desktop application (client)
2. **Poker Server** - Backend game server + chat server (Spring-based, embedded H2 database)
3. **REST API** - Spring Boot REST API for web/external access

## Tech Stack

| Component         | Technology                          | Version        |
|-------------------|-------------------------------------|----------------|
| Language          | Java (OpenJDK)                      | 25             |
| Build             | Apache Maven                        | 3.9.12         |
| Desktop UI        | Java Swing                          | (JDK built-in) |
| REST API          | Spring Boot                         | 3.5.8          |
| ORM               | Hibernate                           | 6.6.42.Final   |
| Database          | H2 Embedded                         | 2.3.232        |
| Logging           | Log4j2                              | 2.25.3         |
| Spring Framework  | Spring                              | 6.2.15         |
| Tests             | JUnit 4 + JUnit 5 (Jupiter)         | 4.13.2 / 5.x   |
| Servlet API       | Jakarta Servlet                     | 6.1.0          |
| Container         | Docker (eclipse-temurin:25-jre)     | Latest         |

## Module Structure

The project is a Maven multi-module build with 19 modules. Build order (later modules depend on earlier ones):

| Module           | Description                                        | Artifact |
|------------------|----------------------------------------------------|----------|
| `common`         | Core config, logging, XML, properties, utils       | jar      |
| `mail`           | Email sending tools                                | jar      |
| `gui`            | GUI infrastructure extending Java Swing            | jar      |
| `db`             | Database infrastructure extending Hibernate        | jar      |
| `jsp`            | JSP-based email/file generation                    | jar      |
| `server`         | Core server functionality                          | jar      |
| `udp`            | Core UDP networking                                | jar      |
| `tools`          | Misc business tools                                | jar      |
| `gamecommon`     | Core game utilities (shared client/server)         | jar      |
| `gameengine`     | Core game engine                                   | jar      |
| `gameserver`     | Core game server                                   | jar      |
| `gametools`      | Game building tools (border/territory managers)    | jar      |
| `ddpoker`        | Classes in `com.ddpoker` package                   | jar      |
| `pokerengine`    | Core poker utilities (shared client/server)        | jar      |
| `pokergamecore`  | Server-side game engine (extracted from client)    | jar      |
| `pokernetwork`   | Poker networking infrastructure (shared)           | jar      |
| `poker`          | DD Poker UI / desktop client                       | jar      |
| `pokerserver`    | DD Poker backend server                            | jar      |
| `api`            | REST API (Spring Boot)                             | jar      |

## Key Entry Points

| Component     | Main Class         | Script        |
|---------------|--------------------|---------------|
| Poker Game    | `PokerMain`        | `poker`       |
| Poker Server  | `PokerServerMain`  | `pokerserver` |
| REST API      | `ApiApplication`   | —             |

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
  .claude/                 # Project documentation and guides
  code/                    # All Java source (Maven multi-module, 19 modules)
    pom.xml                # Parent POM
  docs/                    # Technical documentation (AI whitepaper, etc.)
  images/                  # Screenshots, logos (README assets)
  runtime/                 # Runtime files (messages, logs) - gitignored
  tools/
    scripts/               # Windows PowerShell development scripts
  docker/                  # Docker deployment files
    docker-compose.yml     # Docker Compose configuration
    Dockerfile             # Container image definition (pokerserver + api + H2)
    entrypoint.sh          # Container startup script (dual-process manager)
    README.md              # Docker usage instructions
  unraid/                  # Unraid Community App template
  ddpoker.rc               # Environment setup script (Linux/Mac only)
```

## Known Quirks

- **Password handling** - Server uses bcrypt hashing (migrated from legacy DES encryption). Forgot-password generates a new password and emails it.
- **Test coverage** - 65% minimum enforced by JaCoCo. Core poker logic coverage is improving but still has gaps.
- **Embedded H2 database** - No passwords needed, automatic setup.
