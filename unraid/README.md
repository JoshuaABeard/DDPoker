# DD Poker for Unraid

Texas Hold'em tournament software with online multiplayer support. This container includes the game server, web portal for game management, and downloadable desktop clients.

## Installation

### Method 1: Manual Template Installation

1. Copy `DDPoker.xml` to your Unraid server
2. Place it in: `/boot/config/plugins/dockerMan/templates-user/`
3. In Unraid web UI, go to **Apps** > **Previous Apps** > **Add Container**
4. Select "DDPoker" from the template dropdown
5. Configure settings and click **Apply**

### Method 2: Template Repository URL

1. In Unraid web UI, go to **Apps** > **Settings**
2. Under "Template Repositories", add:
   ```
   https://raw.githubusercontent.com/JoshuaABeard/DDPoker/main/unraid/
   ```
3. Go to **Apps** tab
4. Search for "DD Poker" and click **Install**

## Configuration

### Required Settings

| Setting | Default | Description |
|---------|---------|-------------|
| Web UI Port | 8080 | Web interface access port |
| Game Server Port | 8877 | Game server port for desktop clients |
| Data Path | `/mnt/user/appdata/ddpoker/data` | Database and runtime files |
| Client Installers Path | `/mnt/user/appdata/ddpoker/clientInstallers` | Client download cache |
| Timezone | `America/New_York` | Container timezone |

### Advanced Settings

**UDP Ports (Optional):**
- **Chat Port (11886):** In-game lobby chat server
- **Connection Test Port (11889):** UDP connectivity verification

**Note:** UDP ports are optional. Core functionality (game server, web UI) works without them.

**Database Configuration:**
- Default: H2 embedded database (no configuration needed)
- For MySQL: Change `DB_DRIVER`, `DB_URL`, `DB_USER`, `DB_PASSWORD`

**SMTP Configuration (Optional):**
- Required only for email notifications
- Leave at defaults if not using email features

## Usage

### Access the Web Interface

1. Navigate to: `http://[UNRAID-IP]:8080`
2. Web portal provides:
   - Game hosting and management
   - Player profile administration
   - Tournament history viewing
   - Client downloads

### Download Desktop Clients

From the web interface, download clients for:
- **Windows** (.exe installer)
- **macOS** (.dmg installer)
- **Linux** (.sh installer)
- **Universal** (Java JAR, requires Java 25+)

### Connect Desktop Client to Server

1. Launch desktop client
2. Go to **Online** > **Settings**
3. Configure server connection:
   - **Server Address:** `[UNRAID-IP]:8877`
   - **Chat Address:** `[UNRAID-IP]:11886` (optional)
4. Create or log in to your online profile
5. Join or host games from the Online Lobby

## Storage

### Persistent Data

All data is stored in two Unraid shares:

**Data Directory** (`/mnt/user/appdata/ddpoker/data`):
- H2 database file: `poker.mv.db`
- Player profiles and online accounts
- Game history and tournament records
- Runtime configuration

**Client Installers** (`/mnt/user/appdata/ddpoker/clientInstallers`):
- Cached client installers (Windows, Mac, Linux)
- Universal JAR client
- Automatically generated on first container start
- Reused on container restarts for faster startup

### Backup Recommendations

**Backup the data directory** to preserve:
- Player profiles and accounts
- Tournament history
- Database contents

The client installers directory can be regenerated and doesn't need backup.

## Troubleshooting

### Container Won't Start

**Check logs:**
```bash
docker logs ddpoker
```

**Common issues:**
- Port conflicts (8080 or 8877 already in use)
- Permission issues on appdata directory
- Insufficient disk space

### Web UI Not Accessible

1. Verify container is running: **Docker** tab in Unraid
2. Check port mapping: Ensure 8080 is not used by another container
3. Check firewall rules if accessing from another machine

### Desktop Client Can't Connect

1. **Verify server address:** Use Unraid server IP, not `localhost`
2. **Check port 8877:** Ensure it's not blocked by firewall
3. **Test from web UI:** If web UI works, server is running correctly
4. **Review client logs:** Located in client's runtime log directory

### Database Issues

**Reset database (WARNING: Deletes all data):**
```bash
# Stop container
docker stop ddpoker

# Remove database
rm -rf /mnt/user/appdata/ddpoker/data/*

# Restart container (fresh database will be created)
docker start ddpoker
```

### UDP Chat Not Working

This is expected in some configurations:
- UDP port mapping can be unreliable on some networks
- Chat through the Online Lobby (before joining games) uses TCP and works reliably
- In-game chat uses UDP and may not work in all environments
- Core game functionality is not affected

## Port Reference

| Port | Protocol | Service | Required |
|------|----------|---------|----------|
| 8080 | TCP | Web Interface | Yes |
| 8877 | TCP | Game Server | Yes |
| 11886 | UDP | Lobby Chat | Optional |
| 11889 | UDP | Connection Test | Optional |

## Advanced Configuration

### Using External MySQL Database

1. Set these environment variables:
   ```
   DB_DRIVER=com.mysql.cj.jdbc.Driver
   DB_URL=jdbc:mysql://[MYSQL-HOST]:3306/ddpoker
   DB_USER=ddpoker_user
   DB_PASSWORD=your_secure_password
   ```

2. Create database and user in MySQL:
   ```sql
   CREATE DATABASE ddpoker;
   CREATE USER 'ddpoker_user'@'%' IDENTIFIED BY 'your_secure_password';
   GRANT ALL PRIVILEGES ON ddpoker.* TO 'ddpoker_user'@'%';
   FLUSH PRIVILEGES;
   ```

3. Tables will be created automatically on first startup

### SMTP Email Configuration

**Gmail Example:**
```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM=noreply@yourdomain.com
```

**Note:** Use app-specific passwords for Gmail, not your main account password.

## Support

- **Issues:** https://github.com/JoshuaABeard/DDPoker/issues
- **Documentation:** See `DDPOKER-DOCKER.md` in repository for detailed technical documentation
- **Source Code:** https://github.com/JoshuaABeard/DDPoker

## License

DD Poker is licensed under the GNU General Public License v3.0. See LICENSE.txt in the repository for full details.
