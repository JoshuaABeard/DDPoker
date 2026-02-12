# Docker Build and Publishing Workflow

This guide explains the complete workflow for building and publishing Docker images with platform-specific installers.

## Overview

DD Poker Docker images include platform-specific installers (Windows MSI, macOS DMG, Linux DEB/RPM) that users can download from the web interface. This requires a multi-step build process:

1. Build platform-specific installers on their native OS (GitHub Actions)
2. Package installers into Docker image
3. Publish Docker image to Docker Hub

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ GitHub Actions: build-installers.yml (automatic on tag)    │
│   • Windows runner → MSI                                     │
│   • macOS runner → DMG                                       │
│   • Linux runner → DEB + RPM                                 │
│   • Creates GitHub Release with all installers              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ GitHub Actions: publish-docker.yml (manual trigger)         │
│   • Downloads installers from GitHub Release                │
│   • Builds Docker image with installers                     │
│   • Publishes to Docker Hub                                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Local Development: build-with-installers.sh                 │
│   • Downloads installers from latest GitHub Release         │
│   • Builds Docker image locally for testing                 │
└─────────────────────────────────────────────────────────────┘
```

## Production Publishing Workflow

**When creating a new release:**

1. **Tag and build installers** (automatic):
   ```bash
   git tag v3.3.1
   git push origin v3.3.1
   ```
   This triggers `build-installers.yml` which:
   - Builds installers on all platforms
   - Creates a GitHub Release with all installers

2. **Publish Docker image** (manual):
   - Go to: **Actions → Build and Publish Docker Image → Run workflow**
   - Enter version: `v3.3.1`
   - Check "Push to Docker Hub"
   - Click "Run workflow"

   This:
   - Downloads installers from the v3.3.1 release
   - Builds the Docker image
   - Pushes to Docker Hub with tags:
     - `joshuaabeard/ddpoker:3.3.1-CommunityEdition`
     - `joshuaabeard/ddpoker:3.3`
     - `joshuaabeard/ddpoker:CommunityEdition`
     - `joshuaabeard/ddpoker:latest`

## Local Development Workflow

**For testing Docker images locally:**

```bash
# From repository root
./docker/build-with-installers.sh
```

This script:
1. Checks if installers exist in `docker/downloads/`
2. Downloads missing installers from latest GitHub Release
3. Builds Maven project
4. Builds Docker image locally

**Requirements:**
- GitHub CLI (`gh`) installed and authenticated
- Docker and Docker Compose installed

**To test the image:**
```bash
docker compose -f docker/docker-compose.yml up -d
```

Access downloads at: http://localhost:8080/downloads/

## Manual Local Build (Advanced)

If you need to build installers locally instead of downloading them:

**On Windows:**
```bash
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
cp target/dist/DDPokerCE-3.3.0.msi ../../docker/downloads/
```

**On macOS:**
```bash
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
cp target/dist/DDPokerCE-3.3.0.dmg ../../docker/downloads/
```

**On Linux:**
```bash
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

## GitHub Actions Secrets

For automated Docker Hub publishing, configure these repository secrets:

- `DOCKERHUB_USERNAME`: Your Docker Hub username
- `DOCKERHUB_TOKEN`: Docker Hub access token
  - Create at: https://hub.docker.com/settings/security
  - Name: "DDPoker GitHub Actions"
  - Permissions: Read, Write, Delete

## Troubleshooting

### "gh: command not found"
Install GitHub CLI:
- macOS: `brew install gh`
- Windows: `winget install GitHub.cli`
- Linux: https://github.com/cli/cli/blob/trunk/docs/install_linux.md

Then authenticate: `gh auth login`

### "Missing required files"
The specified GitHub Release doesn't have all required installers. Either:
1. Wait for the `build-installers.yml` workflow to complete
2. Manually build and upload missing installers
3. Use a different release version

### "Docker build failed"
Ensure Maven build completes successfully:
```bash
cd code
mvn clean package -DskipTests
```

### Testing without Docker Hub push
Run the GitHub Actions workflow with "Push to Docker Hub" **unchecked** to test the build without publishing.

## Files

- `.github/workflows/build-installers.yml` - Builds platform-specific installers
- `.github/workflows/publish-docker.yml` - Builds and publishes Docker image
- `docker/build-with-installers.sh` - Local build script
- `docker/Dockerfile` - Docker image definition
- `docker/docker-compose.yml` - Docker Compose configuration
- `docker/DOCKER-HUB-PUBLISHING.md` - Detailed publishing guide

## Related ADRs

- `ADR-005`: jpackage for Cross-Platform Installers
