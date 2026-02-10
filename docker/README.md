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
  joshuaabeard/ddpoker:3.2.0-community
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

1. **DDPokerCommunityEdition-3.3.0.jar** (~21 MB)
   - Universal JAR file (works on all platforms)
   - Requires Java 25 to be installed separately
   - Run with: `java -jar DDPokerCommunityEdition-3.3.0.jar`
   - Built automatically inside Docker container

2. **DDPokerCommunityEdition-3.3.0.msi** (~98 MB)
   - Windows installer with bundled Java runtime
   - No Java installation required
   - Built separately on Windows machine
   - **Required before building Docker image** - must be placed in `docker/downloads/`

### Building the Windows Installer

The Windows installer must be built on a Windows machine with WiX Toolset installed:

**Prerequisites:**
- Windows 10/11
- Java 25 JDK
- Maven 3.6+
- WiX Toolset v3.14+ (`winget install WiXToolset.WiXToolset`)

**Build Steps:**
```bash
# 1. Build the installer (from repository root)
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests

# 2. Copy to Docker downloads folder
cp target/dist/DDPokerCommunityEdition-3.3.0.msi ../../docker/downloads/

# 3. Rebuild Docker image (from repository root)
cd ../..
docker compose -f docker/docker-compose.yml build

# 4. Restart container
docker compose -f docker/docker-compose.yml up -d
```

**Accessing Downloads:**
- **Web Browser**: http://localhost:8080/downloads/
- **Direct URLs**:
  - JAR: http://localhost:8080/downloads/DDPokerCommunityEdition-3.3.0.jar
  - MSI: http://localhost:8080/downloads/DDPokerCommunityEdition-3.3.0.msi

**Important**: The MSI file **must** be present in `docker/downloads/` before building the Docker image. The Docker build will fail if the MSI is missing. Follow the build steps above to create the MSI first.

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
