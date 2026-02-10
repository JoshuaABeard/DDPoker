# DD Poker - Docker Deployment Guide

## Overview

DDPoker runs in a **single Docker container** with an embedded H2 database, making deployment simple and self-contained. No external database required.

## Architecture

```
  Desktop Client (Windows/Mac/Linux)
       │ HTTP :8877, UDP :11886/:11889
       ▼
┌──────────────────────────────────────────────┐
│  Single Docker Container                     │
│                                              │
│  ┌──────────────┐     ┌────────────────┐     │
│  │ pokerserver  │     │   pokerweb     │     │
│  │ :8877 (HTTP) │     │ :8080 (Jetty)  │     │
│  │ :11886 (UDP) │     │ Wicket web UI  │     │
│  │ :11889 (UDP) │     │                │     │
│  └──────┬───────┘     └───────┬────────┘     │
│         │                     │              │
│         ▼                     ▼              │
│  ┌────────────────────────────────────┐      │
│  │   H2 Embedded Database (default)   │      │
│  │   file:/data/poker                 │      │
│  └────────────────────────────────────┘      │
└──────────────────────────────────────────────┘
         │
         ▼ (Docker volume)
    /data/poker.mv.db  (persistent)
```

## Quick Start

### Option 1: Use Pre-Built Image (Recommended)

**Prerequisites:** Docker Desktop installed and running

```bash
# Run the latest version from Docker Hub
docker run -d \
  --name ddpoker \
  -p 8080:8080 \
  -p 8877:8877 \
  -p 11886:11886/udp \
  -p 11889:11889/udp \
  -v ddpoker_data:/data \
  -v ddpoker_installers:/app/downloads \
  joshuaabeard/ddpoker:latest

# View logs
docker logs -f ddpoker

# Access the web interface
# Open browser to: http://localhost:8080/online
```

### Option 2: Build From Source

**Prerequisites:**
- Docker Desktop installed and running
- Java 25+ and Maven 3.9+
- DDPoker source code

```bash
# 1. Build the Java project
cd code
mvn clean install -DskipTests

# 2. Build and start Docker container
cd ..
docker compose -f docker/docker-compose.yml up -d

# 3. View logs
docker compose -f docker/docker-compose.yml logs -f

# 4. Access the web interface
# Open browser to: http://localhost:8080/online
```

That's it! The server is now running with:
- Game server API on port 8877
- Web interface on port 8080
- Chat server on UDP port 11886
- Connection test on UDP port 11889

## Ports Reference

| Port | Protocol | Service | Purpose |
|------|----------|---------|---------|
| 8877 | TCP | pokerserver | Game API — desktop client connects here |
| 8080 | TCP | pokerweb | Website — browse to http://localhost:8080/online |
| 11886 | UDP | pokerserver | Chat server — client chat lobby |
| 11889 | UDP | pokerserver | Connection test — client verifies connectivity |

### Known Limitation: UDP on Docker Desktop for Windows

**Important**: Docker Desktop for Windows has known issues with UDP port mapping. While TCP ports (8877, 8080) work fine, UDP ports (11886, 11889) may not forward correctly from the Windows host to the Linux container.

**Impact**: Chat functionality may timeout when connecting from a Windows client to the Docker container.

**Workarounds**:
1. **Production deployments on Linux**: UDP works fine - no issues
2. **Windows development testing**:
   - Use WSL2 backend for Docker Desktop (better UDP support)
   - Run client from inside WSL2 or a Linux VM
   - Test chat functionality on CI/CD (Linux)
3. **Future solution**: Convert chat from UDP to TCP

This is a Docker Desktop limitation, not a DDPoker issue. The game server itself works correctly.

## Client Configuration

This section explains how to configure the DD Poker desktop client to connect to your Dockerized server.

### Prerequisites

- Docker container running (via `docker compose up`)
- DD Poker desktop client installed and built
- Both services running on your local machine

### Server Endpoints

Your Docker container exposes the following services:

| Service | Port | Protocol | Purpose |
|---------|------|----------|---------|
| Game Server API | 8877 | TCP | Desktop client connects here for game data |
| Web Interface | 8080 | TCP | Browse to http://localhost:8080/online |
| Chat Server | 11886 | UDP | In-game chat communication |
| Connection Test | 11889 | UDP | Client verifies connectivity |

### Client Configuration Steps

#### 1. Launch the Desktop Client

```bash
# From your DDPoker repository root
source ddpoker.rc
poker
```

#### 2. Configure Online Server Settings

1. In the game client, go to **Main Menu** → **Options**
2. Navigate to the **Online** tab
3. Select **Public Online Servers** section
4. Configure the following settings:

   - **Enable**: ☑ (Check this box)
   - **Online Server**: `localhost:8877`
   - **Chat Server**: `localhost:11886`

5. Click **Test Connection** to verify connectivity
   - You should see a success message
   - If it fails, ensure Docker container is running: `docker compose ps`

6. Click **OK** to save settings

#### 3. Create an Online Profile

1. From the Main Menu, go to **Online** → **Create Profile**
2. Fill in the profile details:
   - **Profile Name**: Your desired username
   - **Email**: Your email address
   - **Password**: Choose a secure password
3. Click **Create Profile**

**Note**: Email verification is not required in the Docker setup since SMTP is not configured by default.

#### 4. Access the Online Game Portal

You can view the web interface in your browser:

1. Open http://localhost:8080/online
2. You should see:
   - List of available games
   - List of online players
   - Game statistics

### Verification Steps

#### Test Connection
1. In the client, go to **Options** → **Online** → **Public Online Servers**
2. Click **Test** button
3. Should show "Connection Successful"

#### Test Profile Login
1. Go to **Online** → **Login**
2. Enter your profile credentials
3. You should be connected to the server

#### View in Web Browser
1. Open http://localhost:8080/online
2. Your profile should appear in the "Online Players" list when logged in

### Troubleshooting Client Connection

#### Connection Refused
**Problem**: Client can't connect to server

**Solutions**:
```bash
# Check if container is running
docker compose ps

# Check container logs
docker compose logs

# Restart container
docker compose restart
```

#### Chat Not Working
**Problem**: Can't send/receive chat messages

**Solutions**:
- Verify UDP port 11886 is accessible
- Check Windows Firewall isn't blocking UDP
- Ensure Docker is exposing UDP ports correctly (see Known Limitation above)

#### Profile Creation Fails
**Problem**: Can't create profile

**Solutions**:
```bash
# Check database is initialized
docker compose exec ddpoker ls -la /data/

# Check for errors in logs
docker compose logs | grep -i error

# Reset database (WARNING: deletes all data)
docker compose down -v
docker compose up
```

### Using a Remote Server

If your Docker server is running on a different machine:

1. Replace `localhost` with the server's IP address or hostname
2. Ensure firewall allows:
   - TCP port 8877 (inbound)
   - UDP ports 11886 and 11889 (inbound)
3. Configure client:
   - **Online Server**: `<server-ip>:8877`
   - **Chat Server**: `<server-ip>:11886`

## Data Management

### Persistent Storage

Data is stored in a Docker named volume:

```bash
# View volume details
docker volume inspect ddpoker_ddpoker_data

# List volume contents
docker compose exec ddpoker ls -lh /data/
```

### Backup Database

```bash
# Stop container
docker compose down

# Backup
docker run --rm \
  -v ddpoker_ddpoker_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/ddpoker-backup-$(date +%Y%m%d).tar.gz -C /data .

# Restart
docker compose up -d
```

### Restore Database

```bash
# Stop and remove container
docker compose down

# Remove old volume (WARNING: destroys data)
docker volume rm ddpoker_ddpoker_data

# Create new volume
docker volume create ddpoker_ddpoker_data

# Restore
docker run --rm \
  -v ddpoker_ddpoker_data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/ddpoker-backup-YYYYMMDD.tar.gz -C /data

# Start
docker compose up -d
```

### Reset Database

```bash
# WARNING: This deletes all data!
docker compose down -v
docker compose up -d
```

## Container Management

### Common Commands

```bash
# Start container
docker compose up -d

# Stop container
docker compose down

# View logs (all)
docker compose logs -f

# View logs (last 50 lines)
docker compose logs --tail=50

# Restart container
docker compose restart

# Rebuild after code changes
cd code && mvn clean install -DskipTests && cd ..
docker compose build
docker compose up -d

# Execute command in container
docker compose exec ddpoker sh

# Check container status
docker compose ps

# View resource usage
docker stats ddpoker-ddpoker-1
```

### Updating the Container

After making code changes:

```bash
# 1. Rebuild Java project
cd code
mvn clean install -DskipTests

# 2. Rebuild Docker image
cd ..
docker compose build

# 3. Restart with new image (keeps data)
docker compose up -d

# 4. Verify
docker compose logs --tail=100
```

## Troubleshooting

### Container Won't Start

**Check logs:**
```bash
docker compose logs
```

**Common issues:**
- Port conflicts (8080, 8877, 11886, 11889 already in use)
- Docker Desktop not running
- Insufficient memory

**Solutions:**
```bash
# Check if ports are in use
netstat -an | grep -E "8080|8877|11886|11889"

# Free up memory
docker system prune -a

# Restart Docker Desktop
```

### Website Not Loading

**Test connectivity:**
```bash
curl http://localhost:8080/online
```

**Check if Jetty started:**
```bash
docker compose logs | grep -i "started.*jetty"
```

**Solutions:**
- Ensure container is running: `docker compose ps`
- Check firewall isn't blocking port 8080
- Restart container: `docker compose restart`

### High Memory Usage

**Check memory:**
```bash
docker stats ddpoker-ddpoker-1
```

**Current JVM settings:**
- pokerserver: `-Xms24m -Xmx96m`
- pokerweb: `-Xms24m -Xmx96m`

**Adjust in `docker/entrypoint.sh` if needed:**
```bash
# Increase max heap to 256MB
-Xms24m -Xmx256m
```

## Email Configuration

See [EMAIL-CONFIGURATION.md](./EMAIL-CONFIGURATION.md) for complete SMTP setup instructions to enable user activation emails.

## Advanced Configuration

### Environment Variables

All configuration can be overridden via environment variables in `docker/docker-compose.yml`:

```yaml
environment:
  # Database
  DB_DRIVER: org.h2.Driver
  DB_URL: "jdbc:h2:file:/data/poker;MODE=MySQL"
  DB_USER: sa
  DB_PASSWORD: ""

  # Runtime directory (default: /data/work)
  WORK: /data/work
```

### Custom Ports

To use different ports, edit `docker/docker-compose.yml`:

```yaml
ports:
  - "9877:8877"  # Game server
  - "9080:8080"  # Web interface
  - "21886:11886/udp"  # Chat
  - "21889:11889/udp"  # Connection test
```

**Note:** Internal ports (right side) should not change. Only map external ports (left side).

### Resource Limits

Add resource constraints in `docker/docker-compose.yml`:

```yaml
services:
  ddpoker:
    # ... existing config ...
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

## Container Details

### Base Image

- **eclipse-temurin:25-jre** - Official Eclipse Temurin JRE 25 image
- Lightweight (~300MB) with just the runtime, no build tools

### What's Inside

The container includes:
- Compiled classes from all 22 DDPoker modules
- All runtime dependencies (JARs)
- Embedded Jetty 12.1.6 for web interface
- Apache Wicket 10.8.0 framework
- H2 database 2.3.232
- Dual-process entrypoint script

### Process Management

The container runs two Java processes:
1. **pokerserver** - Game server and chat (starts first)
2. **pokerweb** - Embedded Jetty with Wicket webapp (starts 3 seconds later)

Both processes are managed by `/app/entrypoint.sh`:
- Handles graceful shutdown on SIGTERM/SIGINT
- If one process dies, the other is stopped
- Proper exit code propagation

## Summary

The DDPoker Docker deployment provides:

✅ Single container for entire stack
✅ Embedded H2 database (no external DB needed)
✅ Automatic schema initialization
✅ Persistent data storage
✅ Simple `docker compose up` deployment
✅ Easy backup and restore
✅ Production-ready with Jetty + Wicket

Perfect for development, testing, and production deployment!
