# DD Poker Server - Run Locally on Windows
# This allows UDP chat to work without Docker networking issues

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  DD Poker Server (Local Windows)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
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

# Build classpath
Write-Host "Building classpath..."
$modules = @(
    "common", "mail", "gui", "installer", "db", "wicket", "jsp", "server",
    "udp", "gamecommon", "gameengine", "ddpoker", "pokerengine", "pokernetwork",
    "tools", "gameserver", "pokerserver"
)

$cp = @()
foreach ($module in $modules) {
    $classesPath = "code\$module\target\classes"
    if (Test-Path $classesPath) {
        $cp += $classesPath
    }
}

# Add third-party dependencies (from pokerserver)
$depPath = "code\pokerserver\target\dependency"
if (Test-Path $depPath) {
    Get-ChildItem "$depPath\*.jar" | ForEach-Object {
        # Skip our own module JARs
        if ($_.Name -notmatch "^(common|mail|gui|installer|db|wicket|jsp|server|udp|gamecommon|gameengine|ddpoker|pokerengine|pokernetwork|tools|gameserver|pokerserver)-\d") {
            $cp += $_.FullName
        }
    }
}

$classpath = $cp -join ";"

Write-Host "Starting pokerserver..."
Write-Host "Press Ctrl+C to stop"
Write-Host ""

# Set H2 database path (use local directory, replace backslashes with forward slashes for H2)
$dbPath = (Join-Path $env:WORK "poker") -replace '\\', '/'
Write-Host "Database: $dbPath"
Write-Host ""

# Run the server
$javaArgs = @(
    '-server'
    '-Xms24m'
    '-Xmx96m'
    '-Dfile.encoding=UTF-8'
    "-Ddb.driver=org.h2.Driver"
    "-Ddb.url=jdbc:h2:file:$dbPath;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_ON_EXIT=FALSE"
    "-Ddb.user=sa"
    "-Ddb.password="
    # SMTP Configuration (optional - uncomment and configure for email functionality)
    # '-Dsettings.smtp.host=smtp.gmail.com'
    # '-Dsettings.smtp.port=587'
    # '-Dsettings.smtp.user=your-email@gmail.com'
    # '-Dsettings.smtp.pass=your-app-password'
    # '-Dsettings.smtp.auth=true'
    # '-Dsettings.smtp.starttls.enable=true'
    # '-Dsettings.server.profilefrom=your-email@gmail.com'
    '-cp'
    $classpath
    'com.donohoedigital.games.poker.server.PokerServerMain'
)
& java $javaArgs

Write-Host ""
Write-Host "Server stopped."
