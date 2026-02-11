# DD Poker - Unraid Deployment Guide

This guide explains how to deploy DD Poker to your Unraid server for local testing without using Docker Hub.

## Prerequisites

### On Your Windows Development Machine
- Docker Desktop installed and running
- Maven and Java 25+ (for building)
- SSH client (built into Windows 10+)
- Git Bash or PowerShell

### On Your Unraid Server
- SSH access enabled
- Docker installed (comes with Unraid by default)
- Network connectivity to your Windows machine

## Setup SSH Access to Unraid

### Option 1: Password Authentication (Simplest)
No additional setup needed. The script will prompt for your password when connecting.

### Option 2: SSH Key Authentication (Recommended for Frequent Deployments)

1. **Generate SSH key on Windows** (if you don't have one):
   ```powershell
   ssh-keygen -t ed25519 -C "your_email@example.com"
   # Press Enter to accept default location
   # Optionally set a passphrase
   ```

2. **Copy public key to Unraid**:
   ```powershell
   # Get your public key
   Get-Content $env:USERPROFILE\.ssh\id_ed25519.pub

   # SSH to Unraid and add the key
   ssh root@YOUR_UNRAID_IP
   mkdir -p ~/.ssh
   echo "YOUR_PUBLIC_KEY_HERE" >> ~/.ssh/authorized_keys
   chmod 700 ~/.ssh
   chmod 600 ~/.ssh/authorized_keys
   ```

3. **Test passwordless login**:
   ```powershell
   ssh root@YOUR_UNRAID_IP
   # Should connect without asking for password
   ```

## Deployment Scripts

Two scripts are provided:
- **deploy-to-unraid.ps1** - PowerShell (Windows native)
- **deploy-to-unraid.sh** - Bash (Git Bash/WSL)

Both scripts do the same thing - choose whichever you prefer.

## Usage

### PowerShell (Recommended for Windows)

```powershell
# Navigate to docker directory
cd C:\Repos\DDPoker\docker

# Full build and deploy
.\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100

# Custom username (default is 'root')
.\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 -UnraidUser admin

# Skip Maven build (if already built)
.\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 -SkipMaven

# Skip entire build (just transfer existing image)
.\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 -SkipBuild

# Keep tar file locally after deployment
.\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 -KeepTar
```

### Bash (Git Bash / WSL)

```bash
# Navigate to docker directory
cd /c/Repos/DDPoker/docker

# Make script executable (first time only)
chmod +x deploy-to-unraid.sh

# Full build and deploy
./deploy-to-unraid.sh 192.168.1.100

# Custom username
./deploy-to-unraid.sh 192.168.1.100 admin

# Skip Maven build
SKIP_MAVEN=1 ./deploy-to-unraid.sh 192.168.1.100

# Skip entire build
SKIP_BUILD=1 ./deploy-to-unraid.sh 192.168.1.100

# Keep tar file locally
KEEP_TAR=1 ./deploy-to-unraid.sh 192.168.1.100
```

## What the Script Does

The deployment script automates these steps:

1. **Build Maven Project** (optional)
   - Compiles all Java modules
   - Packages dependencies
   - Can be skipped with `-SkipMaven` or `SKIP_MAVEN=1`

2. **Build Docker Image** (optional)
   - Creates Docker image with all components
   - Tags as `ddpoker:latest`
   - Can be skipped with `-SkipBuild` or `SKIP_BUILD=1`

3. **Save Image to Tar**
   - Exports Docker image as `ddpoker-latest.tar`
   - Typically 300-500 MB

4. **Create Target Directory**
   - Creates `/mnt/user/appdata/ddpoker/` on Unraid
   - Skips if already exists

5. **Transfer to Unraid**
   - Uses SCP to copy tar file to Unraid
   - Shows progress (takes 2-5 minutes depending on network)

6. **Deploy on Unraid**
   - Loads the Docker image
   - Stops existing container (if running)
   - Removes old container
   - Starts new container with proper configuration
   - Shows container status and logs

7. **Cleanup**
   - Removes local tar file (unless `-KeepTar` specified)
   - Tar file kept on Unraid for future reference

## Container Configuration

The script starts the container with these settings:

```bash
docker run -d \
  --name ddpoker \
  -p 8080:8080 \           # Web interface
  -p 8877:8877 \           # Game server API
  -p 11886:11886/udp \     # Chat server
  -p 11889:11889/udp \     # Connection test
  -v ddpoker_data:/data \  # Persistent database
  --restart unless-stopped \
  ddpoker:latest
```

### Customizing Container Settings

To customize environment variables (email, admin credentials, etc.), you can:

1. **Modify the deployment script** - Edit the `docker run` command in either script
2. **Use Unraid's Docker UI** - After first deployment, manage via Unraid web interface
3. **Create docker-compose.yml on Unraid** - For more complex configurations

Example with environment variables:
```bash
docker run -d \
  --name ddpoker \
  -p 8080:8080 \
  -p 8877:8877 \
  -p 11886:11886/udp \
  -p 11889:11889/udp \
  -v ddpoker_data:/data \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD=your-secure-password \
  -e SMTP_HOST=smtp.gmail.com \
  -e SMTP_PORT=587 \
  -e SMTP_USER=your-email@gmail.com \
  -e SMTP_PASSWORD=your-app-password \
  --restart unless-stopped \
  ddpoker:latest
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for full list of environment variables.

## Accessing Your Server

After deployment completes:

1. **Web Interface**: http://YOUR_UNRAID_IP:8080/online
2. **Game Server**: Configure client to connect to `YOUR_UNRAID_IP:8877`

### Configure Desktop Client

1. Launch DD Poker client
2. Go to **Options** → **Online** → **Public Online Servers**
3. Set:
   - **Online Server**: `YOUR_UNRAID_IP:8877`
   - **Chat Server**: `YOUR_UNRAID_IP:11886`
4. Click **Test Connection** to verify

## Managing the Container on Unraid

### View Logs
```bash
ssh root@YOUR_UNRAID_IP
docker logs -f ddpoker
```

### Check Status
```bash
ssh root@YOUR_UNRAID_IP
docker ps --filter name=ddpoker
```

### Restart Container
```bash
ssh root@YOUR_UNRAID_IP
docker restart ddpoker
```

### Stop Container
```bash
ssh root@YOUR_UNRAID_IP
docker stop ddpoker
```

### Remove Container
```bash
ssh root@YOUR_UNRAID_IP
docker stop ddpoker
docker rm ddpoker
```

### View Data Volume
```bash
ssh root@YOUR_UNRAID_IP
docker volume inspect ddpoker_data
ls -lh /var/lib/docker/volumes/ddpoker_data/_data/
```

## Troubleshooting

### Permission Denied (SSH)
```
Permission denied (publickey,password)
```

**Solution**: Verify SSH credentials
```powershell
# Test SSH connection
ssh root@YOUR_UNRAID_IP
# If password prompt doesn't appear, check Unraid SSH settings
```

### Connection Refused (Docker)
```
Cannot connect to the Docker daemon
```

**Solution**: Ensure Docker is running on Unraid
```bash
ssh root@YOUR_UNRAID_IP
docker info
# Should show Docker version and status
```

### Port Already in Use
```
bind: address already in use
```

**Solution**: Check for conflicting containers
```bash
ssh root@YOUR_UNRAID_IP
docker ps | grep -E "8080|8877"
# Stop conflicting container or change ports
```

### Transfer is Slow
```
Transferring at 100 KB/s...
```

**Solutions**:
- Use wired connection instead of WiFi
- Check network congestion
- Consider running build directly on Unraid (see Advanced section)

### Container Exits Immediately
```bash
ssh root@YOUR_UNRAID_IP
docker logs ddpoker
# Check for error messages
```

**Common issues**:
- Missing MSI file (must be built on Windows first)
- Insufficient memory
- Port conflicts

## Advanced Usage

### Environment Variables

Customize behavior with environment variables:

**PowerShell:**
```powershell
$env:IMAGE_NAME = "my-ddpoker"
$env:IMAGE_TAG = "test-v1"
.\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100
```

**Bash:**
```bash
IMAGE_NAME=my-ddpoker IMAGE_TAG=test-v1 ./deploy-to-unraid.sh 192.168.1.100
```

Available variables:
- `IMAGE_NAME` - Docker image name (default: `ddpoker`)
- `IMAGE_TAG` - Docker image tag (default: `latest`)
- `CONTAINER_NAME` - Container name on Unraid (default: `ddpoker`)

### Deploy Multiple Versions

Run different versions side-by-side for testing:

```powershell
# Deploy version 1
.\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 `
    -ImageTag "v3.3.0" `
    -ContainerName "ddpoker-v330"

# Deploy development version
.\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 `
    -ImageTag "dev" `
    -ContainerName "ddpoker-dev"
```

**Important**: Change port mappings to avoid conflicts!

### Backup Data Before Deploy

```bash
ssh root@YOUR_UNRAID_IP

# Create backup
docker run --rm \
  -v ddpoker_data:/data \
  -v /mnt/user/backups:/backup \
  alpine tar czf /backup/ddpoker-$(date +%Y%m%d-%H%M%S).tar.gz -C /data .
```

### Schedule Automatic Deployments

Create a scheduled task to deploy nightly builds:

**Windows Task Scheduler:**
1. Open Task Scheduler
2. Create Basic Task
3. Set trigger (e.g., daily at 2 AM)
4. Action: Start a program
   - Program: `powershell.exe`
   - Arguments: `-File C:\Repos\DDPoker\docker\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100`

## Comparison with Docker Hub Approach

### Local Deployment (This Method)
**Pros:**
- ✅ No Docker Hub account needed
- ✅ Works on private networks
- ✅ No image size limits
- ✅ No public exposure
- ✅ Faster for local networks

**Cons:**
- ❌ Requires SCP/SSH access
- ❌ Slower initial setup
- ❌ Manual transfer for each deployment

### Docker Hub Approach
**Pros:**
- ✅ Simple `docker pull` on Unraid
- ✅ Easy sharing with others
- ✅ Version history

**Cons:**
- ❌ Requires Docker Hub account
- ❌ Slower upload/download
- ❌ Public by default (private requires paid account)
- ❌ Image size limits on free tier

## Next Steps

After successful deployment:

1. **Configure Email** - See [EMAIL-CONFIGURATION.md](EMAIL-CONFIGURATION.md)
2. **Set Admin Password** - See [DEPLOYMENT.md](DEPLOYMENT.md#admin-panel-configuration)
3. **Test Client Connection** - Configure desktop client
4. **Monitor Logs** - Watch for any errors
5. **Setup Backups** - Automate database backups

## Support

For issues or questions:
- Check [DEPLOYMENT.md](DEPLOYMENT.md) for general Docker documentation
- Review [../docs/ADMIN-PANEL.md](../docs/ADMIN-PANEL.md) for admin features
- File issue at https://github.com/anthropics/claude-code/issues
