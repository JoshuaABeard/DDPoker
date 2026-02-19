try {
    $claudeDir = Join-Path $env:USERPROFILE '.claude'
    $flagFile = Join-Path $claudeDir 'telegram_enabled'
    if ((Test-Path $flagFile) -and $env:TELEGRAM_BOT_TOKEN -and $env:TELEGRAM_CHAT_ID) {
        $startFile = Join-Path $claudeDir 'session_start.tmp'
        $durationText = 'Unknown'
        if (Test-Path $startFile) {
            try {
                $startTime = [int64](Get-Content $startFile -Raw)
                $endTime = [DateTimeOffset]::Now.ToUnixTimeSeconds()
                $duration = $endTime - $startTime
                $minutes = [math]::Floor($duration / 60)
                $seconds = $duration % 60
                $durationText = "${minutes}m ${seconds}s"
            } catch {
                $durationText = 'Error'
            }
            Remove-Item $startFile -Force -ErrorAction SilentlyContinue
        }
        $projectDir = 'DDPoker'
        try { $projectDir = Split-Path -Leaf (Get-Location) } catch {}
        $now = Get-Date
        $message = '✅ Claude Code Session Completed - Project: ' + $projectDir + ' - Duration: ' + $durationText + ' - Time: ' + $now.ToString('HH:mm:ss')
        try {
            Invoke-RestMethod -Uri "https://api.telegram.org/bot$env:TELEGRAM_BOT_TOKEN/sendMessage" -Method Post -Body @{chat_id=$env:TELEGRAM_CHAT_ID; text=$message} -ErrorAction SilentlyContinue | Out-Null
        } catch {}
    }
} catch {}
exit 0
