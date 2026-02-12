# SessionStart hook for Claude Code
# Shows context at the start of each session

try {
    Write-Host ""
    Write-Host "=== Session Context ==="
    Write-Host ""

    # Current branch
    $branch = git rev-parse --abbrev-ref HEAD 2>$null
    Write-Host "Branch: $branch"

    # Recent commits (last 3)
    Write-Host ""
    Write-Host "Recent commits:"
    git log --oneline -3 2>$null | ForEach-Object { Write-Host "  $_" }

    # Active plans
    Write-Host ""
    $planDir = ".claude/plans"
    if (Test-Path $planDir) {
        $activePlans = Get-ChildItem "$planDir/*.md" -ErrorAction SilentlyContinue | Where-Object {
            (Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue) -match "Status.*(active|in.*progress)"
        } | ForEach-Object { $_.Name }

        if ($activePlans) {
            Write-Host "Active plans:"
            $activePlans | ForEach-Object { Write-Host "  $_" }
        } else {
            Write-Host "No active plans"
        }
    } else {
        Write-Host "No active plans"
    }

    # Modified files count
    Write-Host ""
    $modified = (git status --short 2>$null | Measure-Object -Line).Lines
    if ($modified -gt 0) {
        Write-Host "Working tree: $modified modified/untracked files"
    } else {
        Write-Host "Working tree: clean"
    }

    Write-Host ""
    Write-Host "Tip: Check .claude/learnings.md for known gotchas"
    Write-Host "======================"
    Write-Host ""
} catch {
    # Silently ignore errors
}

exit 0
