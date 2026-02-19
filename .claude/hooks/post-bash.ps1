try {
    $claudeDir = Join-Path $env:USERPROFILE '.claude'
    $flagFile = Join-Path $claudeDir 'telegram_enabled'
    if ((Test-Path $flagFile) -and $env:TELEGRAM_BOT_TOKEN -and $env:TELEGRAM_CHAT_ID) {
        $startFile = Join-Path $claudeDir 'bash_start.tmp'
        if (Test-Path $startFile) {
            try {
                $startTime = [int64](Get-Content $startFile -Raw)
                $endTime = [DateTimeOffset]::Now.ToUnixTimeSeconds()
                $duration = $endTime - $startTime
                Remove-Item $startFile -Force -ErrorAction SilentlyContinue
                if ($duration -gt 300) {
                    $minutes = [math]::Floor($duration / 60)
                    $seconds = $duration % 60
                    $projectDir = 'DDPoker'
                    try { $projectDir = Split-Path -Leaf (Get-Location) } catch {}
                    $now = Get-Date
                    $message = '⚠️ Long Bash Operation - Duration: ' + $minutes + 'm ' + $seconds + 's - Project: ' + $projectDir + ' - Time: ' + $now.ToString('HH:mm:ss')
                    try {
                        Invoke-RestMethod -Uri "https://api.telegram.org/bot$env:TELEGRAM_BOT_TOKEN/sendMessage" -Method Post -Body @{chat_id=$env:TELEGRAM_CHAT_ID; text=$message} -ErrorAction SilentlyContinue | Out-Null
                    } catch {}
                }
            } catch {}
        }
    }
} catch {}
exit 0
