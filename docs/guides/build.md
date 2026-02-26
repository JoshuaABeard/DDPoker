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

### Multi-Platform Installers

DD Poker supports creating native installers for Windows, macOS, and Linux. Each installer includes a bundled Java runtime, so end users don't need to install Java separately.

The build uses Maven profiles that automatically activate based on the operating system:
- **Windows**: Builds MSI installer (auto-activated on Windows)
- **macOS**: Builds DMG installer (auto-activated on macOS)
- **Linux**: Builds DEB installer (auto-activated on Linux), RPM via manual profile activation

#### Unified Build Command (All Platforms)

```shell
cd C:\Repos\DDPoker\code\poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
```

This command works on all platforms and produces the appropriate installer for your OS.

---

#### Windows Installer (MSI)

**Prerequisites:**
- Windows 10/11
- Java 25 JDK
- Maven 3.6+
- **WiX Toolset v3.14+** (required for creating MSI installers)

**Installing WiX Toolset:**

```powershell
# Using winget (Recommended)
winget install WiXToolset.WiXToolset

# Or download from: https://wixtoolset.org/releases/
```

After installation, restart your terminal for PATH changes to take effect.

**Verify Installation:**
```powershell
candle.exe -?
light.exe -?
```

**Build:**
```shell
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
```

**Output:**
- **File**: `target/dist/DDPokerCE-3.3.0.msi`
- **Size**: ~98 MB (includes Java 25 runtime)

**Installation:**
1. Double-click the .msi file
2. Windows SmartScreen will warn (unsigned installer) - click "More info" → "Run anyway"
3. Follow installation wizard
4. Launch from Start Menu: **DD Poker Community Edition**

**Uninstall:** Windows Settings → Apps → DDPokerCE → Uninstall

---

#### macOS Installer (DMG)

**Prerequisites:**
- macOS 11.0 (Big Sur) or later
- Java 25 JDK
- Maven 3.6+
- Xcode Command Line Tools (usually pre-installed)

**Build:**
```shell
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
```

**Output:**
- **File**: `target/dist/DDPokerCE-3.3.0.dmg`
- **Size**: ~98 MB (includes Java 25 runtime)
- **Architecture**: Apple Silicon (aarch64) - Intel Mac users should use the universal JAR

**Installation:**
1. Open the .dmg file
2. Drag **DD Poker CE** to the Applications folder
3. **Important**: Right-click the app and select "Open" (bypasses Gatekeeper for unsigned apps)
4. Confirm when macOS asks "Are you sure you want to open it?"

**Note**: This app is not signed with an Apple Developer certificate. Always use "Right-click → Open" on first launch.

**Uninstall:** Drag **DD Poker CE** from Applications to Trash

---

#### Linux Installer (DEB - Debian/Ubuntu)

**Prerequisites:**
- Linux (Debian, Ubuntu, Mint, or derivatives)
- Java 25 JDK
- Maven 3.6+
- `fakeroot` package

**Install Prerequisites:**
```shell
sudo apt-get update
sudo apt-get install -y fakeroot
```

**Build:**
```shell
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
```

**Output:**
- **File**: `target/dist/ddpoker-ce_3.3.0-1_amd64.deb`
- **Size**: ~98 MB (includes Java 25 runtime)

**Installation:**
```shell
sudo dpkg -i target/dist/ddpoker-ce_3.3.0-1_amd64.deb
sudo apt-get install -f  # If dependencies needed
```

**Launch:**
- Applications menu → Games → DD Poker CE
- Or run: `ddpoker-ce` in terminal

**Uninstall:**
```shell
sudo apt-get remove ddpoker-ce
```

---

#### Linux Installer (RPM - Fedora/RHEL)

**Prerequisites:**
- Linux (Fedora, RHEL, CentOS, or derivatives)
- Java 25 JDK
- Maven 3.6+
- `rpm` package

**Install Prerequisites:**
```shell
sudo dnf install rpm-build
```

**Build:**
```shell
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
mvn jpackage:jpackage -Pinstaller-linux-rpm -DskipTests
```

**Note**: RPM build requires explicit profile activation (`-Pinstaller-linux-rpm`) because only one Linux installer type can auto-activate.

**Output:**
- **File**: `target/dist/ddpoker-ce-3.3.0-1.x86_64.rpm`
- **Size**: ~98 MB (includes Java 25 runtime)

**Installation:**
```shell
sudo dnf install target/dist/ddpoker-ce-3.3.0-1.x86_64.rpm
# Or
sudo rpm -i target/dist/ddpoker-ce-3.3.0-1.x86_64.rpm
```

**Launch:**
- Applications menu → Games → DD Poker CE
- Or run: `ddpoker-ce` in terminal

**Uninstall:**
```shell
sudo dnf remove ddpoker-ce
```

---

#### CI/CD - Automated Multi-Platform Builds

GitHub Actions automatically builds all platform installers on every version tag push (e.g., `v3.3.0`):

**Trigger a build:**
```shell
git tag v3.3.0
git push origin v3.3.0
```

The workflow:
1. Builds on 3 platforms in parallel (Windows, macOS, Linux)
2. Creates DEB and RPM on Linux
3. Uploads all installers as GitHub Release artifacts
4. Includes universal JAR for manual Java installations

**Download pre-built installers:**
```shell
gh release download v3.3.0 -D downloads/
```

Or visit: https://github.com/YOUR_ORG/DDPoker/releases

---

#### Note on Code Signing

All installers are **unsigned**:
- **Windows**: SmartScreen warning - click "More info" → "Run anyway"
- **macOS**: Gatekeeper warning - use "Right-click → Open" to bypass
- **Linux**: No warnings (DEB/RPM don't require signing for installation)

For production distribution, consider obtaining code signing certificates (~$75-400/year depending on platform).

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
