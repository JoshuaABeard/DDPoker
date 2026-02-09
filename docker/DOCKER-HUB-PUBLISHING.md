# Docker Hub Publishing Guide

This guide covers publishing DD Poker Docker images to Docker Hub.

## Prerequisites

1. Docker Hub account: https://hub.docker.com/
2. Docker CLI logged in: `docker login`
3. Repository created: `joshuaabeard/ddpoker`

## Tagging Strategy

We use semantic versioning with the `-community` suffix:

- **Full version**: `3.2.0-community` (specific release)
- **Minor version**: `3.2` (latest patch in 3.2.x)
- **Variant**: `community` (latest community version)
- **Latest**: `latest` (latest stable release)

## Publishing Process

### 1. Build the Image Locally

```bash
cd DDPoker
mvn clean package -DskipTests -f code/pom.xml
docker compose -f docker/docker-compose.yml build
```

### 2. Tag the Image

```bash
# Tag with full version
docker tag joshuaabeard/ddpoker:3.2.0-community joshuaabeard/ddpoker:3.2.0-community

# Tag with minor version
docker tag joshuaabeard/ddpoker:3.2.0-community joshuaabeard/ddpoker:3.2

# Tag as community variant
docker tag joshuaabeard/ddpoker:3.2.0-community joshuaabeard/ddpoker:community

# Tag as latest
docker tag joshuaabeard/ddpoker:3.2.0-community joshuaabeard/ddpoker:latest
```

### 3. Push to Docker Hub

```bash
# Push all tags
docker push joshuaabeard/ddpoker:3.2.0-community
docker push joshuaabeard/ddpoker:3.2
docker push joshuaabeard/ddpoker:community
docker push joshuaabeard/ddpoker:latest
```

## One-Liner Script

```bash
# Build, tag, and push all versions
cd DDPoker && \
mvn clean package -DskipTests -f code/pom.xml && \
docker compose -f docker/docker-compose.yml build && \
docker tag joshuaabeard/ddpoker:3.2.0-community joshuaabeard/ddpoker:3.2 && \
docker tag joshuaabeard/ddpoker:3.2.0-community joshuaabeard/ddpoker:community && \
docker tag joshuaabeard/ddpoker:3.2.0-community joshuaabeard/ddpoker:latest && \
docker push joshuaabeard/ddpoker:3.2.0-community && \
docker push joshuaabeard/ddpoker:3.2 && \
docker push joshuaabeard/ddpoker:community && \
docker push joshuaabeard/ddpoker:latest
```

## Verifying the Push

Check Docker Hub: https://hub.docker.com/r/joshuaabeard/ddpoker/tags

Test pulling:
```bash
docker pull joshuaabeard/ddpoker:3.2.0-community
docker pull joshuaabeard/ddpoker:latest
```

## Version Bumping Workflow

When releasing a new version (e.g., 3.3.0-community):

1. Update version in all POMs and PokerConstants.java
2. Update docker-compose.yml image tag
3. Build and test locally
4. Tag and push with new version numbers
5. Update `latest` tag to point to new version

## Docker Hub Repository Settings

**Repository:** `joshuaabeard/ddpoker`
**Visibility:** Public
**Description:** DD Poker - Texas Hold'em tournament software with online multiplayer (Community Fork)

**Overview/README:** Link to https://github.com/JoshuaABeard/DDPoker

**Deprecated Tags:**
- `v3.0` - Original version, use `3.2.0-community` or later
