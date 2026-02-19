try {
    $claudeDir = Join-Path $env:USERPROFILE '.claude'
    $flagFile = Join-Path $claudeDir 'telegram_enabled'
    if ((Test-Path $flagFile) -and $env:TELEGRAM_BOT_TOKEN -and $env:TELEGRAM_CHAT_ID) {
        $projectDir = 'DDPoker'
        try { $projectDir = Split-Path -Leaf (Get-Location) } catch {}
        $now = Get-Date
        $message = '🔔 Claude Code Notification - Project: ' + $projectDir + ' - Time: ' + $now.ToString('HH:mm:ss') + ' - Status: Waiting for user input or permission'
        try {
            Invoke-RestMethod -Uri "https://api.telegram.org/bot$env:TELEGRAM_BOT_TOKEN/sendMessage" -Method Post -Body @{chat_id=$env:TELEGRAM_CHAT_ID; text=$message} -ErrorAction SilentlyContinue | Out-Null
        } catch {}
    }
} catch {}
exit 0
