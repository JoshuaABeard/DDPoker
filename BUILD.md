# DD Poker - Build & Environment Setup

## Prerequisites (Windows)

All tools installed and verified on Windows 11 (2026-02-08).

| Tool              | Version       | Location                                                         | Install Method |
|-------------------|---------------|------------------------------------------------------------------|----------------|
| Eclipse Temurin   | JDK 25.0.2    | `C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot`       | `winget install EclipseAdoptium.Temurin.25.JDK` |
| Apache Maven      | 3.9.12        | `C:\Tools\apache-maven-3.9.12`                                  | Manual download from maven.apache.org |
| Docker Desktop    | 4.59.0        | Standard install                                                 | `winget install Docker.DockerDesktop` |

## Environment Variables

Set permanently for the current user:

```
JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot
PATH += C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin
PATH += C:\Tools\apache-maven-3.9.12\bin
```

**Note:** Open a new terminal after installation for these to take effect.

## Verification

```shell
java -version
# openjdk version "25.0.2" 2026-01-20 LTS
# OpenJDK Runtime Environment Temurin-25.0.2+10

mvn -version
# Apache Maven 3.9.12
# Java version: 25.0.2, vendor: Eclipse Adoptium

docker --version
# Docker Desktop 4.59.0
```

## Building the Project

### Quick Build (skip tests)

```shell
cd C:\Repos\DDPoker\code
mvn package -DskipTests=true
```

Build time: ~60 seconds. All 21 modules should report SUCCESS.

### Build with Tests

Tests use embedded H2 database:

```shell
cd C:\Repos\DDPoker\code
mvn test
```

### Install to Local Maven Repo

```shell
cd C:\Repos\DDPoker\code
mvn install -DskipTests=true
```

## Running Components (Windows)

For Windows development, use the PowerShell scripts in `tools/scripts/`:

### Poker Game (Desktop Client)

```powershell
.\tools\scripts\run-client-local.ps1
```

### Poker Server (Backend API)

Uses embedded H2 database (automatic, no setup needed):

```powershell
.\tools\scripts\run-server-local.ps1
```

Includes both pokerserver and pokerweb. Access web interface at: http://localhost:8080/online

### Second Client (for multiplayer testing)

```powershell
.\tools\scripts\run-client-local-2.ps1
```

## Docker Deployment

### Quick Start

The easiest way to run DDPoker server is using Docker:

```shell
# 1. Build the Java project
cd C:\Repos\DDPoker\code
mvn clean install -DskipTests

# 2. Build and start Docker container
cd C:\Repos\DDPoker
docker compose -f docker/docker-compose.yml up -d

# 3. View logs
docker compose -f docker/docker-compose.yml logs -f

# 4. Access web interface
# Open browser to: http://localhost:8080/online
```

This runs both pokerserver and pokerweb in a single container with an embedded H2 database (no external database required).

### Exposed Ports

| Port | Protocol | Service |
|------|----------|---------|
| 8877 | TCP | Game server API |
| 8080 | TCP | Web interface |
| 11886 | UDP | Chat server |
| 11889 | UDP | Connection test |

### Rebuilding After Changes

```shell
# 1. Make code changes
# 2. Rebuild Java project
cd code
mvn clean install -DskipTests

# 3. Rebuild Docker image
cd ..
docker compose -f docker/docker-compose.yml build

# 4. Restart container (keeps data)
docker compose -f docker/docker-compose.yml up -d

# 5. Verify
docker compose -f docker/docker-compose.yml logs --tail=100
```

For complete Docker documentation, see **[docker/DEPLOYMENT.md](docker/DEPLOYMENT.md)**.

## Database (Local Development)

DD Poker uses embedded H2 database - no setup required. Database files are automatically created in:
- Server: `runtime/poker.mv.db`
- Tests: `target/` directories (temporary)

Database is automatically initialized on first run.

## Windows-Specific Notes

- The `ddpoker.rc` script uses bash syntax. Use Git Bash, WSL, or MSYS2.
- The `source ddpoker.rc` command sets `$WORK` and creates Maven aliases.
- Docker Desktop may require a reboot after first install and must be launched before using `docker` commands.
- Maven was not available via winget; manual download was required.

## Local Development

For detailed instructions on running the server and client natively on Windows (outside Docker), see **[LOCAL-DEVELOPMENT.md](LOCAL-DEVELOPMENT.md)**.
