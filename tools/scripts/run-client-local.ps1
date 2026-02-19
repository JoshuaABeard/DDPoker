# DD Poker Client - Run Locally on Windows for Development Testing
# This runs the client using the compiled classes

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  DD Poker Client (Local Development)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$repoRoot = (Get-Item (Join-Path $PSScriptRoot "..\..")).FullName
Set-Location $repoRoot

# Set WORK directory
$env:WORK = Join-Path $repoRoot "runtime"
Write-Host "Working directory: $env:WORK"
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

Write-Host "Starting DD Poker client..."
Write-Host ""
Write-Host "NOTE: If chat doesn't connect with localhost, configure the client to use"
Write-Host "your actual IP address instead (e.g., 192.168.1.x:11886 for chat)"
Write-Host ""

# Run the client
$javaArgs = @(
    '-Xms64m'
    '-Xmx256m'
    '-Dfile.encoding=UTF-8'
    '-cp'
    $classpath
    'com.donohoedigital.games.poker.PokerMain'
)

& java $javaArgs

Write-Host ""
Write-Host "Client closed."
