# DD Poker Client Configuration for Docker Server

## Overview

This guide explains how to configure the DD Poker desktop client to connect to your Dockerized server running on localhost.

## Prerequisites

- Docker container running (via `docker compose up`)
- DD Poker desktop client installed and built
- Both services running on your local machine

## Server Endpoints

Your Docker container exposes the following services:

| Service | Port | Protocol | Purpose |
|---------|------|----------|---------|
| Game Server API | 8877 | TCP | Desktop client connects here for game data |
| Web Interface | 8080 | TCP | Browse to http://localhost:8080/online |
| Chat Server | 11886 | UDP | In-game chat communication |
| Connection Test | 11889 | UDP | Client verifies connectivity |

## Client Configuration Steps

### 1. Launch the Desktop Client

```bash
# From your DDPoker repository root
source ddpoker.rc
poker
```

### 2. Configure Online Server Settings

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

### 3. Create an Online Profile

1. From the Main Menu, go to **Online** → **Create Profile**
2. Fill in the profile details:
   - **Profile Name**: Your desired username
   - **Email**: Your email address
   - **Password**: Choose a secure password
3. Click **Create Profile**

**Note**: Email verification is not required in the Docker setup since SMTP is not configured by default.

### 4. Access the Online Game Portal

You can view the web interface in your browser:

1. Open http://localhost:8080/online
2. You should see:
   - List of available games
   - List of online players
   - Game statistics

## Verification Steps

### Test Connection
1. In the client, go to **Options** → **Online** → **Public Online Servers**
2. Click **Test** button
3. Should show "Connection Successful"

### Test Profile Login
1. Go to **Online** → **Login**
2. Enter your profile credentials
3. You should be connected to the server

### View in Web Browser
1. Open http://localhost:8080/online
2. Your profile should appear in the "Online Players" list when logged in

## Troubleshooting

### Connection Refused
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

### Chat Not Working
**Problem**: Can't send/receive chat messages

**Solutions**:
- Verify UDP port 11886 is accessible
- Check Windows Firewall isn't blocking UDP
- Ensure Docker is exposing UDP ports correctly

### Profile Creation Fails
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

## Advanced Configuration

### Using a Remote Server

If your Docker server is running on a different machine:

1. Replace `localhost` with the server's IP address or hostname
2. Ensure firewall allows:
   - TCP port 8877 (inbound)
   - UDP ports 11886 and 11889 (inbound)
3. Configure client:
   - **Online Server**: `<server-ip>:8877`
   - **Chat Server**: `<server-ip>:11886`

### MySQL Instead of H2

If you've configured MySQL instead of the default H2 database:

1. Ensure MySQL is accessible from Docker
2. Update `docker/docker-compose.yml` environment variables:
   ```yaml
   environment:
     DB_DRIVER: com.mysql.cj.jdbc.Driver
     DB_URL: "jdbc:mysql://your-mysql-host/poker?useUnicode=true&characterEncoding=UTF8&useSSL=false"
     DB_USER: poker
     DB_PASSWORD: "p0k3rdb!"
   ```
3. Restart container: `docker compose up -d`

## Data Persistence

Your profile and game data is stored in a Docker volume:

```bash
# View data location
docker volume inspect ddpoker_ddpoker_data

# Backup data
docker compose down
docker run --rm -v ddpoker_ddpoker_data:/data -v $(pwd):/backup alpine tar czf /backup/ddpoker-backup.tar.gz -C /data .

# Restore data
docker volume create ddpoker_ddpoker_data
docker run --rm -v ddpoker_ddpoker_data:/data -v $(pwd):/backup alpine tar xzf /backup/ddpoker-backup.tar.gz -C /data
```

## Next Steps

- Create or join games through the client
- Invite other players using the web interface
- Monitor game activity at http://localhost:8080/online
- Check server logs: `docker compose logs -f`
