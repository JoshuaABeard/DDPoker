<#
.SYNOPSIS
    Builds and deploys DD Poker Docker container to Unraid server.

.DESCRIPTION
    This script automates the deployment of DD Poker to an Unraid server by:
    1. Building the Maven project (optional)
    2. Building the Docker image (optional)
    3. Saving the Docker image to a tar file (with progress indicator)
    4. Transferring the tar file to Unraid via SSH/SCP (with upload progress)
    5. Loading and running the container on Unraid

    The script includes retry logic for SSH operations and shows progress during
    long-running operations like Docker save and file transfer.

.PARAMETER UnraidHost
    Required. The IP address or hostname of your Unraid server.
    Example: 192.168.1.100

.PARAMETER UnraidUser
    Optional. The SSH username for Unraid server. Default: "root"
    Most Unraid servers use root, but you can specify a different user if needed.

.PARAMETER ImageName
    Optional. The name to give the Docker image. Default: "ddpoker"
    This is the local image name, not related to Docker Hub.

.PARAMETER ImageTag
    Optional. The tag for the Docker image. Default: "latest"
    Use this to create versioned deployments if needed.

.PARAMETER ContainerName
    Optional. The name for the Docker container on Unraid. Default: "DDPoker"
    This is the name you'll use with docker commands on Unraid.

.PARAMETER SkipBuild
    Optional. Skip both Maven and Docker build steps and use existing local image.
    Use this when you've already built the image and just want to redeploy.
    This saves significant time (5-10 minutes) by skipping the build process.

.PARAMETER SkipMaven
    Optional. Skip only the Maven build step, but still build Docker image.
    Useful if you've just built the Maven project and want to rebuild the Docker image.

.PARAMETER KeepTar
    Optional. Keep the local tar file after deployment instead of deleting it.
    By default, the tar file is deleted locally after successful transfer to save disk space.
    The tar file remains on the Unraid server regardless.

.EXAMPLE
    .\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100

    Standard deployment: Builds everything and deploys to Unraid at 192.168.1.100
    Uses default user (root) and creates container named "DDPoker"

.EXAMPLE
    .\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 -SkipBuild

    Quick redeployment: Skips build steps and uses existing local Docker image.
    Perfect for when you've already built and just need to deploy again.

.EXAMPLE
    .\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 -UnraidUser admin

    Deploy using a different SSH user (admin instead of root)

.EXAMPLE
    .\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 -KeepTar

    Deploy and keep the local tar file for backup or reuse

.EXAMPLE
    .\deploy-to-unraid.ps1 -UnraidHost 192.168.1.100 -ImageTag v3.3.0 -ContainerName ddpoker-test

    Create a test deployment with custom tag and container name

.NOTES
    Prerequisites:
    - Docker Desktop must be installed and running
    - SSH client (built into Windows 10+)
    - Maven and Java (if not using -SkipBuild)
    - SSH access configured to your Unraid server
    - Optional: PuTTY (pscp) for better upload progress - install with: winget install PuTTY.PuTTY

    The script will prompt for SSH password and allows 3 retry attempts.

    File: deploy-to-unraid.ps1
    Author: DD Poker Team
    Version: 2.0

.LINK
    https://github.com/yourusername/DDPoker
#>
param(
    [Parameter(Mandatory=$true)]
    [string]$UnraidHost,

    [Parameter(Mandatory=$false)]
    [string]$UnraidUser = "root",

    [Parameter(Mandatory=$false)]
    [string]$ImageName = "ddpoker",

    [Parameter(Mandatory=$false)]
    [string]$ImageTag = "latest",

    [Parameter(Mandatory=$false)]
    [string]$ContainerName = "DDPoker-Local",

    [Parameter(Mandatory=$false)]
    [switch]$SkipBuild,

    [Parameter(Mandatory=$false)]
    [switch]$SkipMaven,

    [Parameter(Mandatory=$false)]
    [switch]$KeepTar
)

$ErrorActionPreference = "Stop"

# ============================================================
# Helper Functions
# ============================================================

function Invoke-SSHWithRetry {
    param(
        [Parameter(Mandatory=$true)]
        [string]$Description,

        [Parameter(Mandatory=$true)]
        [scriptblock]$Command,

        [int]$MaxAttempts = 3
    )

    $attempt = 1
    while ($attempt -le $MaxAttempts) {
        Write-Host "  Attempt $attempt of $MaxAttempts..." -ForegroundColor Gray

        try {
            & $Command
            if ($LASTEXITCODE -eq 0) {
                return $true
            }
            throw "Command failed with exit code $LASTEXITCODE"
        }
        catch {
            if ($attempt -lt $MaxAttempts) {
                Write-Host ""
                Write-Host "  WARNING Operation failed: $_" -ForegroundColor Yellow
                Write-Host "  Press ENTER to retry or Ctrl+C to abort..." -ForegroundColor Yellow
                Read-Host
                Write-Host ""
            }
            else {
                Write-Host ""
                Write-Host "  ERROR $Description failed after $MaxAttempts attempts" -ForegroundColor Red
                throw $_
            }
        }

        $attempt++
    }
}

# ============================================================
# Configuration
# ============================================================

$REPO_ROOT = Split-Path -Parent $PSScriptRoot
$DOCKER_DIR = "$PSScriptRoot"
$TAR_FILE = "$ImageName-$ImageTag.tar"
$LOCAL_TAR_PATH = "$DOCKER_DIR\$TAR_FILE"
$UNRAID_TAR_PATH = "/mnt/user/appdata/ddpoker/$TAR_FILE"

# ============================================================
# Input Validation
# ============================================================

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DD Poker - Unraid Deployment Script" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Validating inputs and prerequisites..." -ForegroundColor Yellow
Write-Host ""

$validationErrors = @()

# Validate UnraidHost format (IP or hostname)
if ([string]::IsNullOrWhiteSpace($UnraidHost)) {
    $validationErrors += "ERROR UnraidHost is required but was not provided"
}
elseif ($UnraidHost -notmatch '^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}|[a-zA-Z0-9][a-zA-Z0-9\-\.]+)$') {
    $validationErrors += "ERROR UnraidHost '$UnraidHost' is not a valid IP address or hostname"
}

# Validate Docker is installed and running (needed for local build)
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    $validationErrors += "ERROR Docker is not installed or not in PATH"
    $validationErrors += "   Docker is required locally to build the image before deploying to Unraid"
    $validationErrors += "   Install Docker Desktop, Podman Desktop, or Docker in WSL2"
}
else {
    # Check if Docker daemon is running
    try {
        $null = docker ps 2>&1
        if ($LASTEXITCODE -ne 0) {
            $validationErrors += "ERROR Docker is installed but not running locally"
            $validationErrors += "   Please start your Docker engine (needed to build image before deploying to Unraid)"
        }
    }
    catch {
        $validationErrors += "ERROR Docker is installed but not running locally"
        $validationErrors += "   Please start your Docker engine (needed to build image before deploying to Unraid)"
    }
}

# Validate SSH is available
$sshCmd = Get-Command ssh -ErrorAction SilentlyContinue
if (-not $sshCmd) {
    $validationErrors += "ERROR SSH client is not installed or not in PATH"
    $validationErrors += "   SSH should be built into Windows 10+. Try: Add-WindowsCapability -Online -Name OpenSSH.Client"
}

# Validate SCP is available
$scpCmd = Get-Command scp -ErrorAction SilentlyContinue
$pscpCmd = Get-Command pscp -ErrorAction SilentlyContinue
if (-not $scpCmd -and -not $pscpCmd) {
    $validationErrors += "ERROR Neither scp nor pscp is available"
    $validationErrors += "   SCP should be built into Windows 10+, or install PuTTY: winget install PuTTY.PuTTY"
}

# Validate Maven and Java if not skipping build
if (-not $SkipBuild -and -not $SkipMaven) {
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvnCmd) {
        $validationErrors += "ERROR Maven (mvn) is not installed or not in PATH"
        $validationErrors += "   Either install Maven, or use -SkipBuild to skip the Maven build step"
    }

    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if (-not $javaCmd) {
        $validationErrors += "ERROR Java is not installed or not in PATH"
        $validationErrors += "   Either install Java JDK, or use -SkipBuild to skip the Maven build step"
    }
}

# Validate Docker image exists if skipping build
if ($SkipBuild) {
    $imageExists = docker images -q "$ImageName`:$ImageTag" 2>$null
    if ([string]::IsNullOrWhiteSpace($imageExists)) {
        $validationErrors += "ERROR Docker image '$ImageName`:$ImageTag' not found locally"
        $validationErrors += "   Cannot use -SkipBuild without an existing image. Either:"
        $validationErrors += "   - Remove -SkipBuild to build the image"
        $validationErrors += "   - Build the image first, then use -SkipBuild"
    }
}

# Display validation results
if ($validationErrors.Count -gt 0) {
    Write-Host "Validation Failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "The following issues were found:" -ForegroundColor Red
    Write-Host ""
    foreach ($error in $validationErrors) {
        Write-Host "  $error" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Please fix the above issues and try again." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "For help, run: Get-Help .\deploy-to-unraid.ps1 -Detailed" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

Write-Host "OK All prerequisites validated successfully" -ForegroundColor Green
Write-Host ""

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Unraid Host:    $UnraidHost" -ForegroundColor White
Write-Host "  Unraid User:    $UnraidUser" -ForegroundColor White
Write-Host "  Image Name:     $ImageName`:$ImageTag" -ForegroundColor White
Write-Host "  Container Name: $ContainerName" -ForegroundColor White
Write-Host "  Skip Build:     $SkipBuild" -ForegroundColor White
Write-Host "  Skip Maven:     $SkipMaven" -ForegroundColor White
Write-Host ""

# ============================================================
# Step 1: Build Maven Project (optional)
# ============================================================

if (-not $SkipBuild -and -not $SkipMaven) {
    Write-Host "[1/6] Building Maven project..." -ForegroundColor Green
    Push-Location "$REPO_ROOT\code"
    try {
        & mvn clean install -DskipTests
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed"
        }
    }
    finally {
        Pop-Location
    }
    Write-Host "OK Maven build complete" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "[1/6] Skipping Maven build" -ForegroundColor Yellow
    Write-Host ""
}

# ============================================================
# Step 2: Build Docker Image (optional)
# ============================================================

if (-not $SkipBuild) {
    Write-Host "[2/6] Building Docker image..." -ForegroundColor Green
    Push-Location $REPO_ROOT
    try {
        & docker compose -f docker/docker-compose.yml build
        if ($LASTEXITCODE -ne 0) {
            throw "Docker build failed"
        }

        # Tag the image with our desired name
        $COMPOSE_IMAGE = "joshuaabeard/ddpoker:3.3.0-CommunityEdition"
        & docker tag $COMPOSE_IMAGE "$ImageName`:$ImageTag"
        if ($LASTEXITCODE -ne 0) {
            throw "Docker tag failed"
        }

        # Tag for local development (used by Unraid)
        & docker tag "$ImageName`:$ImageTag" "joshuaabeard/ddpoker:local-dev"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  Warning: Failed to tag as local-dev" -ForegroundColor Yellow
        }
    }
    finally {
        Pop-Location
    }
    Write-Host "OK Docker image built: $ImageName`:$ImageTag" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "[2/6] Skipping Docker build" -ForegroundColor Yellow
    Write-Host ""
}

# ============================================================
# Step 3: Save Docker Image to Tar
# ============================================================

Write-Host "[3/6] Saving Docker image to tar file..." -ForegroundColor Green
Write-Host "  This may take 1-2 minutes..." -ForegroundColor Gray

# Remove old tar file if exists
if (Test-Path $LOCAL_TAR_PATH) {
    Remove-Item $LOCAL_TAR_PATH -Force
}

# Save with progress indicator
Write-Host "  Saving image (progress indicator will appear below)..." -ForegroundColor Gray
$saveJob = Start-Job -ScriptBlock {
    param($TarPath)
    & docker save -o $TarPath "joshuaabeard/ddpoker:local-dev"
    return $LASTEXITCODE
} -ArgumentList $LOCAL_TAR_PATH

# Show progress dots while saving
$dotCount = 0
while ($saveJob.State -eq 'Running') {
    Write-Host "." -NoNewline -ForegroundColor Cyan
    Start-Sleep -Seconds 2
    $dotCount++
    if ($dotCount % 30 -eq 0) {
        Write-Host ""
        Write-Host "  Still saving" -NoNewline -ForegroundColor Gray
    }
}
Write-Host ""

$exitCode = Receive-Job $saveJob
Remove-Job $saveJob

if ($exitCode -ne 0) {
    throw "Docker save failed"
}

$tarSize = (Get-Item $LOCAL_TAR_PATH).Length / 1MB
Write-Host "OK Image saved: $LOCAL_TAR_PATH ($([math]::Round($tarSize, 2)) MB)" -ForegroundColor Green
Write-Host ""

# ============================================================
# Step 4: Create Target Directory on Unraid
# ============================================================

Write-Host "[4/6] Creating target directory on Unraid..." -ForegroundColor Green
Invoke-SSHWithRetry -Description "Create directory on Unraid" -Command {
    ssh "$UnraidUser@$UnraidHost" "mkdir -p /mnt/user/appdata/ddpoker"
}
Write-Host "OK Target directory ready" -ForegroundColor Green
Write-Host ""

# ============================================================
# Step 5: Transfer Tar to Unraid
# ============================================================

Write-Host "[5/6] Transferring image to Unraid..." -ForegroundColor Green
Write-Host "  Target: $UnraidUser@$UnraidHost`:$UNRAID_TAR_PATH" -ForegroundColor Gray
Write-Host "  Size: $([math]::Round($tarSize, 2)) MB" -ForegroundColor Gray
Write-Host "  This may take 2-5 minutes depending on network speed..." -ForegroundColor Gray
Write-Host ""

# Check if pscp (PuTTY SCP) is available for better progress
$usePscp = Get-Command pscp -ErrorAction SilentlyContinue

Write-Host "  Using scp for transfer..." -ForegroundColor Gray
Invoke-SSHWithRetry -Description "Transfer file to Unraid" -Command {
    scp $LOCAL_TAR_PATH "$UnraidUser@$UnraidHost`:$UNRAID_TAR_PATH"
}

Write-Host ""
Write-Host "OK Transfer complete" -ForegroundColor Green
Write-Host ""

# ============================================================
# Step 6: Deploy on Unraid
# ============================================================

Write-Host "[6/6] Deploying container on Unraid..." -ForegroundColor Green

# Create deployment script to run on Unraid
$deployScript = @'
#!/bin/bash
set -e

echo ""
echo "Loading Docker image..."
docker load -i UNRAID_TAR_PATH_PLACEHOLDER

echo ""
echo "Checking for existing containers to copy settings..."

# Determine source container for settings
SOURCE_CONTAINER=""
if docker inspect CONTAINER_NAME_PLACEHOLDER &>/dev/null; then
    echo "  Found CONTAINER_NAME_PLACEHOLDER - will preserve its settings"
    SOURCE_CONTAINER="CONTAINER_NAME_PLACEHOLDER"
elif docker inspect DDPoker &>/dev/null; then
    echo "  Found DDPoker - will migrate settings to CONTAINER_NAME_PLACEHOLDER"
    SOURCE_CONTAINER="DDPoker"
fi

# Capture settings if source exists
if [[ -n "$SOURCE_CONTAINER" ]]; then
    echo "  Capturing settings from $SOURCE_CONTAINER..."

    # Environment variables
    EXISTING_ENV=$(docker inspect $SOURCE_CONTAINER --format '{{range .Config.Env}}-e "{{.}}" {{end}}')
    if [[ $? -ne 0 ]]; then
        echo "ERROR: Failed to capture environment from $SOURCE_CONTAINER"
        exit 1
    fi

    # Port mappings - use jq for reliable parsing
    EXISTING_PORTS=$(docker inspect $SOURCE_CONTAINER --format '{{json .HostConfig.PortBindings}}' | jq -r 'to_entries | map(.key as $container | .value[] | "-p \(.HostPort):\($container)") | join(" ")' 2>/dev/null)
    if [[ $? -ne 0 ]]; then
        echo "ERROR: Failed to capture ports from $SOURCE_CONTAINER"
        exit 1
    fi
    # If ports are empty, use defaults
    if [[ -z "$EXISTING_PORTS" ]]; then
        echo "  No ports configured in $SOURCE_CONTAINER, using defaults"
        EXISTING_PORTS="-p 8080:8080 -p 8877:8877 -p 11886:11886/udp -p 11889:11889/udp"
    else
        echo "  Captured ports: $EXISTING_PORTS"
    fi

    # Volume mounts
    EXISTING_VOLUMES=$(docker inspect $SOURCE_CONTAINER --format '{{range .Mounts}}-v {{if eq .Type "volume"}}{{.Name}}{{else}}{{.Source}}{{end}}:{{.Destination}}{{if not .RW}}:ro{{end}} {{end}}')
    if [[ $? -ne 0 ]]; then
        echo "ERROR: Failed to capture volumes from $SOURCE_CONTAINER"
        exit 1
    fi

    # Network
    EXISTING_NETWORK=$(docker inspect $SOURCE_CONTAINER --format '{{.HostConfig.NetworkMode}}')
    if [[ $? -ne 0 ]]; then
        echo "ERROR: Failed to capture network from $SOURCE_CONTAINER"
        exit 1
    fi

    # Static IP address (if set)
    EXISTING_IP=$(docker inspect $SOURCE_CONTAINER --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
    if [[ -n "$EXISTING_IP" ]]; then
        EXISTING_IP_FLAG="--ip $EXISTING_IP"
        echo "  Captured static IP: $EXISTING_IP"
    else
        EXISTING_IP_FLAG=""
    fi

    # MAC address (if set)
    EXISTING_MAC=$(docker inspect $SOURCE_CONTAINER --format '{{.NetworkSettings.MacAddress}}')
    if [[ -n "$EXISTING_MAC" ]]; then
        EXISTING_MAC_FLAG="--mac-address $EXISTING_MAC"
        echo "  Captured MAC address: $EXISTING_MAC"
    else
        EXISTING_MAC_FLAG=""
    fi

    # Restart policy
    EXISTING_RESTART=$(docker inspect $SOURCE_CONTAINER --format '{{.HostConfig.RestartPolicy.Name}}')
    if [[ $? -ne 0 ]]; then
        echo "ERROR: Failed to capture restart policy from $SOURCE_CONTAINER"
        exit 1
    fi

    echo "  OK Settings captured successfully"
else
    echo "  No existing container found - using defaults"
    EXISTING_ENV=""
    EXISTING_PORTS="-p 8080:8080 -p 8877:8877 -p 11886:11886/udp -p 11889:11889/udp"
    EXISTING_VOLUMES="-v ddpoker_data:/data"
    EXISTING_NETWORK="bridge"
    EXISTING_RESTART="unless-stopped"
    EXISTING_IP_FLAG=""
    EXISTING_MAC_FLAG=""
fi

echo ""
echo "Stopping and removing existing containers..."

# Stop and remove DDPoker-Local if it exists
if docker inspect CONTAINER_NAME_PLACEHOLDER &>/dev/null; then
    echo "  Stopping CONTAINER_NAME_PLACEHOLDER..."
    docker stop CONTAINER_NAME_PLACEHOLDER 2>/dev/null || echo "    (already stopped)"
    echo "  Removing CONTAINER_NAME_PLACEHOLDER..."
    docker rm CONTAINER_NAME_PLACEHOLDER
fi

# Stop production DDPoker (avoid port conflicts, keep it stopped)
if docker inspect DDPoker &>/dev/null; then
    echo "  Stopping production DDPoker container..."
    docker stop DDPoker 2>/dev/null || echo "    (already stopped)"
    echo "    Production container will remain stopped"
fi

echo ""
echo "Starting CONTAINER_NAME_PLACEHOLDER container..."
docker run -d \
  --name CONTAINER_NAME_PLACEHOLDER \
  --net=$EXISTING_NETWORK \
  $EXISTING_IP_FLAG \
  $EXISTING_MAC_FLAG \
  $EXISTING_ENV \
  $EXISTING_PORTS \
  $EXISTING_VOLUMES \
  --restart=$EXISTING_RESTART \
  --label net.unraid.docker.managed=dockerman \
  --label net.unraid.docker.icon="https://raw.githubusercontent.com/JoshuaABeard/DDPoker/main/unraid/icons/ddpoker.png" \
  --label net.unraid.docker.webui="http://[IP]:[PORT:8080]" \
  joshuaabeard/ddpoker:local-dev

echo ""
echo "Waiting for container to start..."
sleep 3

echo ""
echo "Container status:"
docker ps --filter name=CONTAINER_NAME_PLACEHOLDER --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "Production container status:"
docker ps -a --filter name=DDPoker --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "Recent logs:"
docker logs --tail=20 CONTAINER_NAME_PLACEHOLDER

echo ""
echo "OK Deployment complete!"
echo ""
echo "  CONTAINER_NAME_PLACEHOLDER: Running with joshuaabeard/ddpoker:local-dev"
echo "  DDPoker: Stopped (production container)"
echo ""
echo "Access the server at:"
echo "  Web UI:     http://UNRAID_HOST_PLACEHOLDER:8080/online"
echo "  Game Server: UNRAID_HOST_PLACEHOLDER:8877"
echo ""
echo "To view live logs:"
echo "  docker logs -f CONTAINER_NAME_PLACEHOLDER"
echo ""
'@

# Substitute PowerShell variables into bash script
$deployScript = $deployScript -replace 'CONTAINER_NAME_PLACEHOLDER', $ContainerName
$deployScript = $deployScript -replace 'UNRAID_TAR_PATH_PLACEHOLDER', $UNRAID_TAR_PATH
$deployScript = $deployScript -replace 'UNRAID_HOST_PLACEHOLDER', $UnraidHost

# Execute deployment script on Unraid
$deployScript | & ssh "$UnraidUser@$UnraidHost" "bash -s"
if ($LASTEXITCODE -ne 0) {
    throw "Deployment on Unraid failed"
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Your DDPoker LOCAL DEV server is now running on Unraid:" -ForegroundColor Yellow
Write-Host "  Container:     $ContainerName" -ForegroundColor White
Write-Host "  Image:         joshuaabeard/ddpoker:local-dev" -ForegroundColor White
Write-Host "  Web Interface: http://${UnraidHost}:8080/online" -ForegroundColor White
Write-Host "  Game Server:   ${UnraidHost}:8877" -ForegroundColor White
Write-Host ""
Write-Host "Production container (DDPoker) is STOPPED" -ForegroundColor Yellow
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Configure your client to connect to ${UnraidHost}:8877" -ForegroundColor White
Write-Host "  2. View live logs: ssh $UnraidUser@${UnraidHost} 'docker logs -f $ContainerName'" -ForegroundColor White
Write-Host "  3. Monitor status: ssh $UnraidUser@${UnraidHost} 'docker ps'" -ForegroundColor White
Write-Host ""

# ============================================================
# Cleanup
# ============================================================

if (-not $KeepTar) {
    Write-Host "Cleaning up local tar file..." -ForegroundColor Gray
    Remove-Item $LOCAL_TAR_PATH -Force
    Write-Host "OK Local cleanup complete" -ForegroundColor Green
    Write-Host ""
    Write-Host "Note: Tar file kept on Unraid at: $UNRAID_TAR_PATH" -ForegroundColor Gray
    Write-Host "      (delete manually if needed)" -ForegroundColor Gray
} else {
    Write-Host "Keeping local tar file: $LOCAL_TAR_PATH" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Done! 🎉" -ForegroundColor Green
