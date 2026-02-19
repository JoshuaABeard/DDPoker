try {
    $claudeDir = Join-Path $env:USERPROFILE '.claude'
    $flagFile = Join-Path $claudeDir 'telegram_enabled'
    if ((Test-Path $flagFile) -and $env:TELEGRAM_BOT_TOKEN -and $env:TELEGRAM_CHAT_ID) {
        if (-not (Test-Path $claudeDir)) { New-Item -ItemType Directory -Path $claudeDir -Force | Out-Null }
        $startFile = Join-Path $claudeDir 'session_start.tmp'
        $startTime = [DateTimeOffset]::Now.ToUnixTimeSeconds()
        Set-Content -Path $startFile -Value $startTime -NoNewline -Force
        $projectDir = 'DDPoker'
        try { $projectDir = Split-Path -Leaf (Get-Location) } catch {}
        $now = Get-Date
        $message = '🚀 Claude Code Session Started - Project: ' + $projectDir + ' - Time: ' + $now.ToString('HH:mm:ss')
        try {
            Invoke-RestMethod -Uri "https://api.telegram.org/bot$env:TELEGRAM_BOT_TOKEN/sendMessage" -Method Post -Body @{chat_id=$env:TELEGRAM_CHAT_ID; text=$message} -ErrorAction SilentlyContinue | Out-Null
        } catch {}
    }
} catch {}
exit 0
