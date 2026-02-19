try {
    $claudeDir = Join-Path $env:USERPROFILE '.claude'
    $flagFile = Join-Path $claudeDir 'telegram_enabled'
    if ((Test-Path $flagFile) -and $env:TELEGRAM_BOT_TOKEN -and $env:TELEGRAM_CHAT_ID) {
        if (-not (Test-Path $claudeDir)) { New-Item -ItemType Directory -Path $claudeDir -Force | Out-Null }
        $bashStartFile = Join-Path $claudeDir 'bash_start.tmp'
        $timestamp = [DateTimeOffset]::Now.ToUnixTimeSeconds()
        Set-Content -Path $bashStartFile -Value $timestamp -NoNewline -Force
    }
} catch {}
exit 0
