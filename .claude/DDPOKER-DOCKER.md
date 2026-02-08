# DD Poker - Docker Deployment

## Overview

DDPoker now runs in a **single Docker container** with an embedded H2 database, making deployment simple and self-contained. No external database required.

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

### Prerequisites

- Docker Desktop installed and running
- Java 25+ and Maven 3.9+ (for building)
- DDPoker source code

### Build and Run

```bash
# 1. Build the Java project
cd C:\Repos\DDPoker\code
mvn clean install -DskipTests

# 2. Build and start Docker container
cd C:\Repos\DDPoker
docker compose up -d

# 3. View logs
docker compose logs -f

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
3. **Future solution**: Convert chat from UDP to TCP (see `.claude/CHAT-TCP-CONVERSION.md`)

This is a Docker Desktop limitation, not a DDPoker issue. The game server itself works correctly.

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

## Database Configuration

### H2 Embedded (Default)

By default, the container uses H2 database in MySQL compatibility mode:

```yaml
environment:
  DB_DRIVER: org.h2.Driver
  DB_URL: "jdbc:h2:file:/data/poker;MODE=MySQL;AUTO_SERVER=TRUE"
  DB_USER: sa
  DB_PASSWORD: ""
```

**Features:**
- No external database needed
- Automatic schema initialization on first run
- Data persists in Docker volume `/data`
- MySQL-compatible SQL mode

### Switching to MySQL

To use an external MySQL database instead:

1. Edit `docker-compose.yml` and uncomment the environment section:

```yaml
services:
  ddpoker:
    # ... existing config ...
    environment:
      DB_DRIVER: com.mysql.cj.jdbc.Driver
      DB_URL: "jdbc:mysql://your-mysql-host:3306/poker?useUnicode=true&characterEncoding=UTF8&useSSL=false"
      DB_USER: poker
      DB_PASSWORD: "p0k3rdb!"
```

2. Ensure MySQL is accessible from Docker
3. Create the database schema using `tools/db/create_tables.sql`
4. Restart: `docker compose up -d`

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

### Client Can't Connect

**Verify server is listening:**
```bash
docker compose logs | grep -E "Listening on port|Binding:"
```

**Expected output:**
```
Listening on port(s) 8877
Binding: 172.18.0.2:8877
```

**Solutions:**
- Check client configuration (see `CLIENT-CONFIGURATION.md`)
- Test TCP connection: `telnet localhost 8877`
- Check UDP ports: `nc -u localhost 11886`
- Verify firewall allows traffic

### Database Issues

**Check database files:**
```bash
docker compose exec ddpoker ls -lh /data/
```

**Expected:**
```
poker.mv.db      (H2 database file)
poker.lock.db    (H2 lock file)
```

**Reset database:**
```bash
# WARNING: Deletes all data
docker compose down -v
docker compose up -d
```

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

### Line Ending Issues

If you modify shell scripts on Windows:

```bash
# Convert to Unix line endings
dos2unix docker/entrypoint.sh

# Rebuild
docker compose build
```

The `.gitattributes` file should prevent this, but if you edit directly, use `dos2unix`.

## Build Process Details

### Maven Build

The Docker build requires pre-compiled classes:

```bash
cd code
mvn clean install -DskipTests
```

This produces:
- `target/classes/` - Compiled classes for each module
- `target/dependency/` - All dependency JARs
- `target/test-classes/` - Test classes (needed for PokerJetty)

### Dockerfile Layers

1. **Base image** - Eclipse Temurin JRE 25
2. **Directory setup** - Create `/app`, `/data`
3. **Copy classes** - All 22 modules' compiled classes → `/app/classes/`
4. **Copy webapp** - Wicket webapp files → `/app/webapp/`
5. **Copy runtime messages** - Message files → `/app/runtime/messages/`
6. **Copy dependencies** - All JARs → `/app/lib/`
7. **Remove duplicates** - Delete project JARs (we use classes/)
8. **Copy entrypoint** - Shell script → `/app/entrypoint.sh`
9. **Set environment** - Database defaults
10. **Expose ports** - 8877, 8080, 11886, 11889

### Classpath Construction

The entrypoint builds the classpath dynamically:

```bash
CLASSPATH="/app/classes"
for jar in /app/lib/*.jar; do
  CLASSPATH="$CLASSPATH:$jar"
done
```

This ensures:
- Compiled classes take precedence (avoiding conflicts)
- All dependencies are available
- Both processes share the same classpath

## Advanced Configuration

### Environment Variables

All configuration can be overridden via environment variables in `docker-compose.yml`:

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

To use different ports, edit `docker-compose.yml`:

```yaml
ports:
  - "9877:8877"  # Game server
  - "9080:8080"  # Web interface
  - "21886:11886/udp"  # Chat
  - "21889:11889/udp"  # Connection test
```

**Note:** Internal ports (right side) should not change. Only map external ports (left side).

### Resource Limits

Add resource constraints in `docker-compose.yml`:

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

## Legacy Dockerfiles

The main `Dockerfile` (at repo root) provides the complete production deployment with both pokerserver and pokerweb using embedded Jetty.

## Client Installer Distribution

The Docker container can automatically build and serve pre-configured client installers, making deployment seamless for your players.

### Overview

When the container starts, it can optionally build native installers (Windows `.exe`, Mac `.dmg`, Linux `.sh`) that are:
- **Pre-configured** with your server URL
- **Self-contained** with embedded Java runtime
- **Single-file** executables - just download and run
- **Cached** for fast restarts

### How It Works

```
Container Startup
    │
    ▼
Check for cached installers
    │
    ├─► Found (same SERVER_HOST) ──► Use cached installers
    │                                 (instant startup)
    │
    └─► Not found / HOST changed ──► Build new installers
                                      (5-10 minutes first time)
                                      │
                                      ▼
                                   Cache for future use
                                      │
                                      ▼
                                   Start server
```

### Configuration

Set the `SERVER_HOST` environment variable to your server's public hostname:

```bash
# For localhost testing
docker compose up

# For production server
SERVER_HOST=poker.myserver.com docker compose up

# Or in .env file
echo "SERVER_HOST=poker.myserver.com" > .env
docker compose up
```

### Download Page

Players visit `http://your-server:8080/download` to see:
- Windows installer (`.exe`) - ~100-120 MB
- macOS installer (`.dmg`) - ~100-120 MB
- Linux installer (`.sh`) - ~100-120 MB
- Cross-platform JAR (`.jar`) - ~20 MB (requires Java 25)

All installers are **pre-configured** to connect to your server automatically!

### Installer Build Process

The build happens automatically at container startup if needed:

1. **Configure URLs**: Updates `client.properties` and `gamedef.xml` with your server URL
2. **Build Client**: Compiles Java code with Maven
3. **Generate Installers**: Uses Install4j to create native installers (if available)
4. **Fallback**: Creates fat JAR if Install4j not present
5. **Cache**: Saves to `/app/downloads` volume for reuse

### Install4j Support

The project has an **open source Install4j license** for building native installers.

**To enable Install4j in Docker** (future enhancement):
```dockerfile
# Add to Dockerfile
RUN wget https://download.ej-technologies.com/install4j/install4j_linux_10_0_9.tar.gz && \
    tar -xzf install4j_linux_10_0_9.tar.gz -C /opt/ && \
    ln -s /opt/install4j10 /opt/install4j
```

**Without Install4j**: Container automatically falls back to fat JAR distribution (20 MB, requires Java 25).

### Viewing Available Downloads

```bash
# List installers in running container
docker compose exec ddpoker ls -lh /app/downloads/

# Browse via web
open http://localhost:8080/downloads/
```

### Forcing Rebuild

To force installers to rebuild (e.g., after changing SERVER_HOST):

```bash
# Option 1: Remove cache volume
docker compose down
docker volume rm ddpoker_installer_cache
docker compose up

# Option 2: Delete cache file in container
docker compose exec ddpoker rm -f /app/downloads/.installer-cache-info
docker compose restart
```

### Build Time

| Scenario | Time | Notes |
|----------|------|-------|
| **First startup** (building installers) | 5-10 min | Maven + Install4j build |
| **Subsequent restarts** (cached) | <10 sec | Uses cached installers |
| **SERVER_HOST changed** | 5-10 min | Rebuilds with new URL |
| **Fat JAR fallback** (no Install4j) | ~2 min | Maven build only |

### Volume Management

The `installer_cache` volume persists installers across restarts:

```yaml
volumes:
  - installer_cache:/app/downloads  # Persistent cache
```

**Benefits:**
- ✅ Fast restarts (no rebuild needed)
- ✅ Disk space efficient (share across restarts)
- ✅ Automatic cache invalidation (when SERVER_HOST changes)

### Multi-Host Deployment

Perfect for scenarios where multiple people host their own servers:

```bash
# Host 1
SERVER_HOST=poker1.example.com docker compose up
# Players download from http://poker1.example.com:8080/download
# Clients connect to poker1.example.com automatically

# Host 2
SERVER_HOST=poker2.example.com docker compose up
# Players download from http://poker2.example.com:8080/download
# Clients connect to poker2.example.com automatically
```

Each host's container builds installers configured for their specific server!

### Scripts Reference

The installer system consists of three scripts in `/docker`:

1. **`configure-server-url.sh`** - Updates client configuration files
   ```bash
   ./docker/configure-server-url.sh <server_host> [server_port] [chat_port] [web_port]
   ```

2. **`build-installers.sh`** - Builds installers with configured URLs
   ```bash
   ./docker/build-installers.sh <server_host> [server_port] [chat_port] [web_port] [output_dir]
   ```

3. **`entrypoint-with-installers.sh`** - Container startup with installer build support
   - Checks cache
   - Builds if needed
   - Starts server

### Troubleshooting

**Installers not appearing:**
```bash
# Check if build ran
docker compose logs | grep installer

# Check download directory
docker compose exec ddpoker ls -l /app/downloads/

# Force rebuild
docker volume rm ddpoker_installer_cache
docker compose up --force-recreate
```

**Build taking too long:**
- First build takes 5-10 minutes (normal)
- Subsequent restarts use cached installers (<10 sec)
- Check logs: `docker compose logs -f`

**Fat JAR instead of native installers:**
- Install4j not present in container (expected currently)
- Fat JAR fallback works but requires Java 25
- Future: Add Install4j to Dockerfile for native installers

## Future Plans

### Unraid Community App

The long-term goal is publishing DDPoker as an Unraid community application. This will require:

1. **Multi-architecture builds** - Support amd64 and arm64
2. **Container registry** - Publish to Docker Hub or GHCR
3. **Unraid template** - XML template for Unraid app store
4. **Icon/logo** - Container branding
5. **Documentation** - User guide for Unraid users

### Possible Enhancements

- **Health checks** - Docker HEALTHCHECK instruction
- **Metrics** - Prometheus/Grafana integration
- **Nginx reverse proxy** - HTTPS support
- **Separate containers** - Split pokerserver and pokerweb (if needed)
- **Init containers** - Database migration automation

## Client Configuration

For detailed instructions on configuring the desktop client to connect to your Docker server, see:

**[CLIENT-CONFIGURATION.md](./CLIENT-CONFIGURATION.md)**

Includes:
- Step-by-step client setup
- Connection testing
- Profile creation
- Troubleshooting
- Remote server configuration

## Getting Help

If you encounter issues:

1. Check logs: `docker compose logs`
2. Review troubleshooting section above
3. Verify prerequisites are met
4. Check Docker Desktop is running
5. Ensure ports aren't blocked by firewall

## Summary

The DDPoker Docker deployment provides:

✅ Single container for entire stack
✅ Embedded H2 database (no external DB needed)
✅ Automatic schema initialization
✅ Persistent data storage
✅ Simple `docker compose up` deployment
✅ Easy backup and restore
✅ MySQL fallback option
✅ Production-ready with Jetty + Wicket

Perfect for development, testing, and production deployment!
