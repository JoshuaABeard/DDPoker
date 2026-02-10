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

### Build Windows Installer

Creates a self-contained Windows installer with bundled Java runtime (no Java installation required for end users).

#### Prerequisites
- Windows 10/11
- Java 25 JDK (not JRE)
- Maven 3.6+
- **WiX Toolset v3.14+** (required for creating EXE/MSI installers)

#### Installing WiX Toolset

WiX is required for jpackage to create Windows installers. Install using one of these methods:

**Option 1: Using winget (Recommended)**
```powershell
# Run PowerShell as Administrator
winget install WiXToolset.WiXToolset
```

**Option 2: Manual Download**
1. Visit: https://wixtoolset.org/releases/
2. Download WiX Toolset 3.14.1 (or latest v3.x)
3. Run installer as Administrator
4. Restart terminal for PATH changes to take effect

**Verify Installation**:
```powershell
# Should display WiX version
candle.exe -?
light.exe -?
```

#### Build Steps

```shell
cd C:\Repos\DDPoker\code\poker
mvn clean package assembly:single jpackage:jpackage
```

**Note**: WiX tools must be in your PATH. If you get a "Can not find WiX tools" error, restart your terminal after installing WiX.

#### Output

- **File**: `target/dist/DDPokerCommunityEdition-3.3.0.msi`
- **Size**: ~98MB (includes Java runtime)
- **Installation**: Double-click to install, creates Start Menu shortcut

**Branding**: The installer is branded as "DDPoker Community Edition" to clearly distinguish it from the original DD Poker releases.

#### Testing the Installer

1. **Locate the installer**:
   ```
   C:\Repos\DDPoker\code\poker\target\dist\DDPoker-3.3.0-community.msi
   ```

2. **Install**:
   - Double-click the .msi file
   - Windows SmartScreen will warn (unsigned installer - this is expected)
   - Click "More info" → "Run anyway"
   - Follow installer wizard
   - Choose installation directory (default: `C:\Program Files\DDPoker`)

3. **Launch**:
   - Start Menu → All Apps → DD Poker → DDPoker
   - Or use desktop shortcut

4. **Verify**:
   - Application launches without Java installation
   - Database created at: `%APPDATA%\ddpoker\poker.mv.db`
   - Config created at: `%APPDATA%\ddpoker\config.json`

#### Note on SmartScreen Warning

The installer is unsigned, so Windows will show a security warning. This is expected and safe to bypass for development/personal use. To remove the warning for production distribution, a code signing certificate is needed (estimated cost: $75-300/year).

#### Uninstall

Windows Settings → Apps → DDPoker → Uninstall

**Note**: User data (database, config) is preserved in `%APPDATA%\ddpoker\` after uninstall.

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
