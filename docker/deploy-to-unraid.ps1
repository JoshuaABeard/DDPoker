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
    [string]$ContainerName = "DDPoker",

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
                Write-Host "  ⚠ Operation failed: $_" -ForegroundColor Yellow
                Write-Host "  Press ENTER to retry or Ctrl+C to abort..." -ForegroundColor Yellow
                Read-Host
                Write-Host ""
            }
            else {
                Write-Host ""
                Write-Host "  ❌ $Description failed after $MaxAttempts attempts" -ForegroundColor Red
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
    $validationErrors += "❌ UnraidHost is required but was not provided"
}
elseif ($UnraidHost -notmatch '^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}|[a-zA-Z0-9][a-zA-Z0-9\-\.]+)$') {
    $validationErrors += "❌ UnraidHost '$UnraidHost' is not a valid IP address or hostname"
}

# Validate Docker is installed and running (needed for local build)
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    $validationErrors += "❌ Docker is not installed or not in PATH"
    $validationErrors += "   Docker is required locally to build the image before deploying to Unraid"
    $validationErrors += "   Install Docker Desktop, Podman Desktop, or Docker in WSL2"
}
else {
    # Check if Docker daemon is running
    try {
        $null = docker ps 2>&1
        if ($LASTEXITCODE -ne 0) {
            $validationErrors += "❌ Docker is installed but not running locally"
            $validationErrors += "   Please start your Docker engine (needed to build image before deploying to Unraid)"
        }
    }
    catch {
        $validationErrors += "❌ Docker is installed but not running locally"
        $validationErrors += "   Please start your Docker engine (needed to build image before deploying to Unraid)"
    }
}

# Validate SSH is available
$sshCmd = Get-Command ssh -ErrorAction SilentlyContinue
if (-not $sshCmd) {
    $validationErrors += "❌ SSH client is not installed or not in PATH"
    $validationErrors += "   SSH should be built into Windows 10+. Try: Add-WindowsCapability -Online -Name OpenSSH.Client"
}

# Validate SCP is available
$scpCmd = Get-Command scp -ErrorAction SilentlyContinue
$pscpCmd = Get-Command pscp -ErrorAction SilentlyContinue
if (-not $scpCmd -and -not $pscpCmd) {
    $validationErrors += "❌ Neither scp nor pscp is available"
    $validationErrors += "   SCP should be built into Windows 10+, or install PuTTY: winget install PuTTY.PuTTY"
}

# Validate Maven and Java if not skipping build
if (-not $SkipBuild -and -not $SkipMaven) {
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvnCmd) {
        $validationErrors += "❌ Maven (mvn) is not installed or not in PATH"
        $validationErrors += "   Either install Maven, or use -SkipBuild to skip the Maven build step"
    }

    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if (-not $javaCmd) {
        $validationErrors += "❌ Java is not installed or not in PATH"
        $validationErrors += "   Either install Java JDK, or use -SkipBuild to skip the Maven build step"
    }
}

# Validate Docker image exists if skipping build
if ($SkipBuild) {
    $imageExists = docker images -q "$ImageName`:$ImageTag" 2>$null
    if ([string]::IsNullOrWhiteSpace($imageExists)) {
        $validationErrors += "❌ Docker image '$ImageName`:$ImageTag' not found locally"
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

Write-Host "✓ All prerequisites validated successfully" -ForegroundColor Green
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
    Write-Host "✓ Maven build complete" -ForegroundColor Green
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
    }
    finally {
        Pop-Location
    }
    Write-Host "✓ Docker image built: $ImageName`:$ImageTag" -ForegroundColor Green
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
    param($ImageName, $ImageTag, $TarPath)
    & docker save -o $TarPath "$ImageName`:$ImageTag"
    return $LASTEXITCODE
} -ArgumentList $ImageName, $ImageTag, $LOCAL_TAR_PATH

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
Write-Host "✓ Image saved: $LOCAL_TAR_PATH ($([math]::Round($tarSize, 2)) MB)" -ForegroundColor Green
Write-Host ""

# ============================================================
# Step 4: Create Target Directory on Unraid
# ============================================================

Write-Host "[4/6] Creating target directory on Unraid..." -ForegroundColor Green
Invoke-SSHWithRetry -Description "Create directory on Unraid" -Command {
    ssh "$UnraidUser@$UnraidHost" "mkdir -p /mnt/user/appdata/ddpoker"
}
Write-Host "✓ Target directory ready" -ForegroundColor Green
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

if ($usePscp) {
    Write-Host "  Using pscp for transfer with progress..." -ForegroundColor Gray
    Invoke-SSHWithRetry -Description "Transfer file to Unraid" -Command {
        pscp -batch $LOCAL_TAR_PATH "$UnraidUser@$UnraidHost`:$UNRAID_TAR_PATH"
    }
} else {
    Write-Host "  Using scp for transfer..." -ForegroundColor Gray
    Write-Host "  (install PuTTY pscp for progress indicator: winget install PuTTY.PuTTY)" -ForegroundColor DarkGray
    Invoke-SSHWithRetry -Description "Transfer file to Unraid" -Command {
        scp $LOCAL_TAR_PATH "$UnraidUser@$UnraidHost`:$UNRAID_TAR_PATH"
    }
}

Write-Host ""
Write-Host "✓ Transfer complete" -ForegroundColor Green
Write-Host ""

# ============================================================
# Step 6: Deploy on Unraid
# ============================================================

Write-Host "[6/6] Deploying container on Unraid..." -ForegroundColor Green

# Create deployment script to run on Unraid
$deployScript = @"
#!/bin/bash
set -e

echo ""
echo "Loading Docker image..."
docker load -i $UNRAID_TAR_PATH

echo ""
echo "Stopping existing container (if running)..."
docker stop $ContainerName 2>/dev/null || echo "  (container not running)"

echo ""
echo "Removing existing container (if exists)..."
docker rm $ContainerName 2>/dev/null || echo "  (container does not exist)"

echo ""
echo "Starting new container..."
docker run -d \
  --name $ContainerName \
  -p 8080:8080 \
  -p 8877:8877 \
  -p 11886:11886/udp \
  -p 11889:11889/udp \
  -v ddpoker_data:/data \
  --restart unless-stopped \
  $ImageName`:$ImageTag

echo ""
echo "Waiting for container to start..."
sleep 3

echo ""
echo "Container status:"
docker ps --filter name=$ContainerName --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "Recent logs:"
docker logs --tail=20 $ContainerName

echo ""
echo "✓ Deployment complete!"
echo ""
echo "Access the server at:"
echo "  Web UI:     http://$UnraidHost:8080/online"
echo "  Game Server: $UnraidHost:8877"
echo ""
echo "To view live logs:"
echo "  docker logs -f $ContainerName"
echo ""
"@

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
Write-Host "Your DDPoker server is now running on Unraid at:" -ForegroundColor Yellow
Write-Host "  Web Interface: http://$UnraidHost:8080/online" -ForegroundColor White
Write-Host "  Game Server:   $UnraidHost:8877" -ForegroundColor White
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Configure your client to connect to $UnraidHost:8877" -ForegroundColor White
Write-Host "  2. View live logs: ssh $UnraidUser@$UnraidHost 'docker logs -f $ContainerName'" -ForegroundColor White
Write-Host "  3. Monitor status: ssh $UnraidUser@$UnraidHost 'docker ps'" -ForegroundColor White
Write-Host ""

# ============================================================
# Cleanup
# ============================================================

if (-not $KeepTar) {
    Write-Host "Cleaning up local tar file..." -ForegroundColor Gray
    Remove-Item $LOCAL_TAR_PATH -Force
    Write-Host "✓ Local cleanup complete" -ForegroundColor Green
    Write-Host ""
    Write-Host "Note: Tar file kept on Unraid at: $UNRAID_TAR_PATH" -ForegroundColor Gray
    Write-Host "      (delete manually if needed)" -ForegroundColor Gray
} else {
    Write-Host "Keeping local tar file: $LOCAL_TAR_PATH" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Done! 🎉" -ForegroundColor Green
