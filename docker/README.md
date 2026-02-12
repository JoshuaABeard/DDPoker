# DD Poker Docker Deployment

This directory contains all Docker-related files for DD Poker.

## Quick Start

**Using Pre-built Image from Docker Hub:**

```bash
docker run -d \
  --name ddpoker \
  -p 8080:8080 \
  -p 8877:8877 \
  -p 11886:11886/udp \
  -p 11889:11889/udp \
  -v ddpoker_data:/data \
  joshuaabeard/ddpoker:3.3.0-CommunityEdition
```

**Or with Docker Compose:**

From this directory (`docker/`):
```bash
docker compose up -d
```

From the repository root:
```bash
docker compose -f docker/docker-compose.yml up -d
```

Docker Compose will automatically pull the image from Docker Hub if not available locally, or build it from source if you've cloned the repository.

## Client Downloads

The Docker container serves client downloads at `http://localhost:8080/downloads/`:

### Available Files

1. **DDPokerCE-3.3.0.jar** (~21 MB)
   - Universal JAR file (works on all platforms)
   - Requires Java 25 to be installed separately
   - Run with: `java -jar DDPokerCE-3.3.0.jar`
   - Built automatically inside Docker container

2. **DDPokerCE-3.3.0.msi** (~98 MB)
   - Windows installer with bundled Java runtime
   - No Java installation required
   - Built separately on Windows machine
   - **Required before building Docker image** - must be placed in `docker/downloads/`

## Building Locally with Installers

The Docker image includes platform-specific installers (Windows MSI, macOS DMG, Linux DEB/RPM) that users can download from the web interface. Use the build script to handle installer management automatically.

### Quick Build (Automated)

```bash
# From repository root
./docker/build-with-installers.sh
```

**What it does:**
1. Checks if installers exist in `docker/downloads/`
2. Downloads missing installers from the latest GitHub Release
3. Builds the Maven project
4. Builds the Docker image

**Prerequisites:**
- GitHub CLI (`gh`) installed and authenticated
- Docker and Docker Compose installed

### Manual Build

If you want to build installers locally before building the Docker image:

**On each platform:**
```bash
# Windows (MSI):
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
cp target/dist/DDPokerCE-3.3.0.msi ../../docker/downloads/

# macOS (DMG):
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
cp target/dist/DDPokerCE-3.3.0.dmg ../../docker/downloads/

# Linux (DEB + RPM):
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
mvn jpackage:jpackage -Pinstaller-linux-rpm -DskipTests
cp target/dist/*.deb target/dist/*.rpm ../../docker/downloads/
```

**Then build Docker image:**
```bash
# From repository root
docker compose -f docker/docker-compose.yml build
```

**Accessing Downloads:**
Once the container is running, access installers at:
- **Web Browser**: http://localhost:8080/downloads/
- **Direct URLs**:
  - JAR: http://localhost:8080/downloads/DDPokerCE-3.3.0.jar
  - MSI: http://localhost:8080/downloads/DDPokerCE-3.3.0.msi
  - DMG: http://localhost:8080/downloads/DDPokerCE-3.3.0.dmg
  - DEB: http://localhost:8080/downloads/ddpoker-ce_3.3.0-1_amd64.deb
  - RPM: http://localhost:8080/downloads/ddpoker-ce-3.3.0-1.x86_64.rpm

## Files

- **docker-compose.yml** - Docker Compose configuration with service definitions, ports, volumes, and environment variables
- **Dockerfile** - Container image definition for building the DD Poker server
- **entrypoint.sh** - Container startup script that manages both pokerserver and pokerweb processes
- **DOCKER-HUB-PUBLISHING.md** - Guide for publishing Docker images to Docker Hub

## Configuration

All configuration is managed through environment variables in `docker-compose.yml`. See the main documentation for details:

- [DEPLOYMENT.md](./DEPLOYMENT.md) - Complete Docker deployment guide (includes client configuration)
- [EMAIL-CONFIGURATION.md](./EMAIL-CONFIGURATION.md) - Email/SMTP setup
- [DOCKER-HUB-PUBLISHING.md](./DOCKER-HUB-PUBLISHING.md) - Publishing to Docker Hub

## Local Overrides

For local development with secrets (SMTP passwords, etc.), create a `docker-compose.override.yml` file in the **repository root** (not in this directory). This file is gitignored and will be automatically merged with the main configuration.

Example `../docker-compose.override.yml`:
```yaml
services:
  ddpoker:
    environment:
      - SMTP_HOST=smtp.gmail.com
      - SMTP_PORT=587
      - SMTP_USER=your-email@gmail.com
      - SMTP_PASSWORD=your-app-password
      - SMTP_AUTH=true
      - SMTP_STARTTLS_ENABLE=true
      - SMTP_FROM=your-email@gmail.com
```

Then run from the repository root:
```bash
docker compose -f docker/docker-compose.yml up -d
```
