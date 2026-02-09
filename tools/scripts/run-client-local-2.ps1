# DD Poker Client #2 - Run Locally on Windows for Development Testing
# This runs a second client instance using a separate user profile
# Use this to test multiplayer with two clients on the same machine

Write-Host ""
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  DD Poker Client #2 (Local Development)" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow
Write-Host ""

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

# Set WORK directory
$env:WORK = Join-Path $repoRoot "runtime"
Write-Host "Working directory: $env:WORK"

# Use a different user.home to avoid database conflicts
$userHome2 = Join-Path $env:USERPROFILE ".dd-poker3-client2"
New-Item -ItemType Directory -Force -Path $userHome2 | Out-Null
Write-Host "User profile directory: $userHome2"
Write-Host ""

# Create runtime directories
$logDir = Join-Path $env:WORK "ddpoker\runtime\log"
$msgDir = Join-Path $env:WORK "ddpoker\runtime\messages"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
New-Item -ItemType Directory -Force -Path $msgDir | Out-Null

# Copy messages if needed
$pokerHtml = Join-Path $msgDir "poker.html"
if (-not (Test-Path $pokerHtml)) {
    Write-Host "Copying runtime messages..."
    Copy-Item -Path "runtime\messages\*" -Destination $msgDir -Recurse -Force
}

# Build classpath for client
Write-Host "Building classpath..."
$modules = @(
    "common", "mail", "gui", "installer", "db", "wicket", "jsp", "server",
    "udp", "gamecommon", "gameengine", "ddpoker", "pokerengine", "pokernetwork",
    "poker"  # client module
)

$cp = @()
foreach ($module in $modules) {
    $classesPath = "code\$module\target\classes"
    if (Test-Path $classesPath) {
        $cp += $classesPath
    }
}

# Add third-party dependencies (from poker module)
$depPath = "code\poker\target\dependency"
if (Test-Path $depPath) {
    Get-ChildItem "$depPath\*.jar" | ForEach-Object {
        # Skip our own module JARs
        if ($_.Name -notmatch "^(common|mail|gui|installer|db|wicket|jsp|server|udp|gamecommon|gameengine|ddpoker|pokerengine|pokernetwork|poker)-\d") {
            $cp += $_.FullName
        }
    }
}

$classpath = $cp -join ";"

Write-Host "Starting DD Poker client #2..."
Write-Host ""
Write-Host "NOTE: This is CLIENT #2 with a separate user profile"
Write-Host "      Use run-client-local.ps1 for the first client"
Write-Host ""
Write-Host "NOTE: If chat doesn't connect with localhost, configure the client to use"
Write-Host "your actual IP address instead (e.g., 192.168.1.240:11886 for chat)"
Write-Host ""

# Run the client with a different user.home
$javaArgs = @(
    '-client'
    '-Xms64m'
    '-Xmx256m'
    '-Dfile.encoding=UTF-8'
    "-Duser.home=$userHome2"
    '-cp'
    $classpath
    'com.donohoedigital.games.poker.PokerMain'
)

& java $javaArgs

Write-Host ""
Write-Host "Client #2 closed."
