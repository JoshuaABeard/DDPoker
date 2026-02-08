# DD Poker - Project Overview

## About

DD Poker is a full-featured Texas Hold'em poker simulator originally developed by Donohoe Digital LLC (2003-2017), now open-sourced under GPLv3. It consists of three main components:

1. **DD Poker Game** - Java Swing desktop application (client)
2. **Poker Server** - Backend API + chat server (Spring-based, MySQL)
3. **Poker Web** - Apache Wicket website / "Online Portal" (deployed as `.war` on Tomcat)

## Tech Stack

| Component         | Technology                          | Version        |
|-------------------|-------------------------------------|----------------|
| Language          | Java (OpenJDK)                      | 25             |
| Build             | Apache Maven                        | 3.9.12         |
| Desktop UI        | Java Swing                          | (JDK built-in) |
| Web Framework     | Apache Wicket                       | 10.8.0         |
| ORM               | Hibernate                           | 6.6.42.Final   |
| App Server        | Embedded Jetty (Docker) / Tomcat    | 12.1.6 / 11.0.18 |
| Database          | H2 (default) / MySQL                | 2.3.232 / 9.0  |
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

### Database Options

1. **H2 (Default for Docker)** - Embedded database, no setup required
   - Automatic schema initialization
   - MySQL compatibility mode
   - File-based storage in Docker volume
   - Perfect for development and single-server deployments

2. **MySQL (Optional)** - External database for advanced deployments
   - DB scripts in `tools/db/`
   - Configurable via environment variables
   - Supports clustering and replication

### Other Dependencies

- **SMTP** - For sending profile registration emails (optional). Configured in `poker/server.properties`.

## Configuration System

The project uses a layered properties system:

- **LoggingConfig** - `log4j2.[apptype].properties` files loaded by app type (client/webapp/server/cmdline)
- **PropertyConfig** - `[appname]/[apptype].properties` files, with per-user overrides via `[username].properties`
- **Debug Settings** - Controlled via `settings.debug.*` entries in user-specific properties files
- **Database Configuration** - Environment variables for flexible database selection:
  - `DB_DRIVER` - JDBC driver class (default: `org.h2.Driver`)
  - `DB_URL` - JDBC connection URL
  - `DB_USER` - Database username
  - `DB_PASSWORD` - Database password
  - `DB_HOST` - MySQL host (legacy, for MySQL-only setups)

## Repository Layout

```
ddpoker/
  code/                    # All Java source (Maven multi-module)
    pom.xml                # Parent POM
    common/                # Through proto/ (22 modules)
  docs/                    # Documentation (AI whitepaper, etc.)
  images/                  # Screenshots, logos
  installer/               # install4j configuration
  runtime/                 # Runtime files (messages, logs)
  tools/
    bin/                   # Shell scripts (poker, pokerserver, pokerweb, runjava, etc.)
    db/                    # Database scripts (create_dbs.sql, reset_dbs.sh, etc.)
  guidelines/              # Project guidelines and documentation
  docker/                  # Docker-related files
    entrypoint.sh          # Container entrypoint (dual-process manager)
  Dockerfile               # Primary Dockerfile (pokerserver + pokerweb + H2)
  docker-compose.yml       # Docker Compose configuration
  Dockerfile.pokerweb.docker   # DEPRECATED - Use main Dockerfile
  Dockerfile.ubuntu.docker     # Ubuntu dev/test container (optional)
  Dockerfile.act               # GitHub Actions local testing (optional)
  ddpoker.rc               # Environment setup script (aliases, PATH, JAVA_HOME)
```

## Known Quirks

- **Password handling** - Server stores reversibly-encrypted passwords and can email them in plaintext (legacy design).
- **HSQLDB** - Pinned at 1.8.0.10 (latest is 2.7.4) to avoid database migration.
- **Test coverage** - Acknowledged as lacking, especially in core poker logic.
- **DB passwords in git** - `p0k3rdb!` for local dev databases, `d@t@b@s3` for MySQL root. Acceptable for local dev.
