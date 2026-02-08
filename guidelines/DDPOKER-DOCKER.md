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

The repository includes older Dockerfiles that are preserved but not used by the current implementation:

| File | Purpose | Status |
|------|---------|--------|
| `Dockerfile.pokerweb.docker` | Tomcat-based pokerweb | **Deprecated** - Use main `Dockerfile` |
| `Dockerfile.ubuntu.docker` | X11 dev container | **Optional** - For GUI testing |
| `Dockerfile.act` | GitHub Actions testing | **Optional** - For CI/CD testing |

The main `Dockerfile` (at repo root) replaces `Dockerfile.pokerweb.docker` and adds pokerserver support.

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
