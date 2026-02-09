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

## Files

- **docker-compose.yml** - Docker Compose configuration with service definitions, ports, volumes, and environment variables
- **Dockerfile** - Container image definition for building the DD Poker server
- **entrypoint.sh** - Container startup script that manages both pokerserver and pokerweb processes
- **DOCKER-HUB-PUBLISHING.md** - Guide for publishing Docker images to Docker Hub

## Configuration

All configuration is managed through environment variables in `docker-compose.yml`. See the main documentation for details:

- [DDPOKER-DOCKER.md](../.claude/DDPOKER-DOCKER.md) - Complete Docker deployment guide
- [EMAIL-CONFIGURATION.md](../.claude/EMAIL-CONFIGURATION.md) - Email/SMTP setup
- [CLIENT-CONFIGURATION.md](../.claude/CLIENT-CONFIGURATION.md) - Client connection setup

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
