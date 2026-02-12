#!/usr/bin/env pwsh
# Status line for Claude Code
# Shows git branch, model, context usage, and block indicator

# Set UTF-8 encoding for proper emoji/unicode support
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# Read JSON input from stdin
$inputJson = [Console]::In.ReadToEnd()

try {
    # Parse JSON
    $data = $inputJson | ConvertFrom-Json

    # Extract values
    $model = if ($data.model.display_name) { $data.model.display_name } else { "Unknown" }
    $usedPct = if ($null -ne $data.context_window.used_percentage) {
        [math]::Round($data.context_window.used_percentage, 1)
    } else {
        0
    }
    $totalInput = if ($data.context_window.total_input_tokens) { $data.context_window.total_input_tokens } else { 0 }
    $totalOutput = if ($data.context_window.total_output_tokens) { $data.context_window.total_output_tokens } else { 0 }
    $contextSize = if ($data.context_window.context_window_size) { $data.context_window.context_window_size } else { 200000 }

    # Get git branch
    $branch = git rev-parse --abbrev-ref HEAD 2>$null
    if (-not $branch) { $branch = "unknown" }

    # Get git status - count changes
    $gitStatus = git -c core.fileMode=false status --short 2>$null
    $added = 0
    $deleted = 0

    if ($gitStatus) {
        $added = ($gitStatus | Where-Object { $_ -match '^(A|M|\?\?)' } | Measure-Object).Count
        $deleted = ($gitStatus | Where-Object { $_ -match '^D' } | Measure-Object).Count
    }

    # ANSI escape code
    $esc = [char]27

    # Icons for each section
    $gitIcon = "⎇"      # Git branch symbol

    # Model icon options:
    # $modelIcon = "🤖"   # Robot emoji (needs emoji support)
    # $modelIcon = "⚙"    # Gear/settings
    # $modelIcon = "◉"    # Fisheye
    # $modelIcon = "●"    # Filled circle
    # $modelIcon = "▣"    # Square with fill
    $modelIcon = "🤖"     # Default: Robot (try this first)

    # Context icon options:
    # $ctxIcon = "💭"     # Thought bubble (represents AI thinking/context)
    # $ctxIcon = "📊"     # Bar chart (represents usage/metrics)
    # $ctxIcon = "◈"      # Diamond (clean geometric)
    # $ctxIcon = "⊙"      # Circled dot (widely supported)
    # $ctxIcon = "◉"      # Fisheye (bold circle)
    # $ctxIcon = "%"      # Percent symbol (literal)
    # $ctxIcon = "▓"      # Block character (original)
    $ctxIcon = "💭"       # Default: Thought bubble

    # Color options for git section background (choose one):
    # $gitBg = "48;5;161"  # Bright pink/magenta (original)
    # $gitBg = "48;5;99"   # Purple
    # $gitBg = "48;5;75"   # Light blue
    # $gitBg = "48;5;37"   # Teal/cyan
    # $gitBg = "48;5;208"  # Orange
    # $gitBg = "48;5;71"   # Green
    $gitBg = "48;5;99"     # Default: Purple

    # Build git section with branch icon and colored background
    $gitSection = "$esc[$($gitBg)m$esc[97m $gitIcon $branch (+$added,-$deleted) $esc[0m"

    # Build model section with yellow background
    $modelSection = "$esc[43m$esc[30m $modelIcon $model $esc[0m"

    # Build context section with blue background
    $ctxSection = "$esc[44m$esc[97m $ctxIcon Ctx: $($usedPct)% $esc[0m"

    # Calculate block percentage (total tokens / context window size)
    $totalTokens = $totalInput + $totalOutput
    $blockPct = if ($contextSize -gt 0) {
        [math]::Round(($totalTokens / $contextSize) * 100, 1)
    } else {
        0
    }

    # Progress bar style options - choose one:
    $barStyle = "segmented"  # Options: "bracketed", "segmented", "simple", "gradient"

    $barWidth = 20  # Shorter bar looks cleaner

    if ($barStyle -eq "bracketed") {
        # Style 1: [████████░░░░] with brackets
        $filledLength = [math]::Floor(($blockPct / 100) * $barWidth)
        $emptyLength = $barWidth - $filledLength
        $filled = "█" * $filledLength
        $empty = "░" * $emptyLength

        # Color only the filled portion
        $barColor = if ($blockPct -lt 50) { "$esc[92m" }      # Bright green
                    elseif ($blockPct -lt 80) { "$esc[93m" }  # Bright yellow
                    else { "$esc[91m" }                        # Bright red

        $barDisplay = "[$barColor$filled$esc[0m$empty]"

    } elseif ($barStyle -eq "segmented") {
        # Style 2: ▰▰▰▰▰▱▱▱▱▱ rectangular segments
        $filledLength = [math]::Floor(($blockPct / 100) * $barWidth)
        $emptyLength = $barWidth - $filledLength
        $filled = "▰" * $filledLength
        $empty = "▱" * $emptyLength

        $barColor = if ($blockPct -lt 50) { "$esc[92m" }
                    elseif ($blockPct -lt 80) { "$esc[93m" }
                    else { "$esc[91m" }

        $barDisplay = "$barColor$filled$esc[0m$empty"

    } elseif ($barStyle -eq "simple") {
        # Style 3: Simple filled bar without empty space
        $filledLength = [math]::Floor(($blockPct / 100) * $barWidth)
        $filled = "▓" * $filledLength

        $barColor = if ($blockPct -lt 50) { "$esc[92m" }
                    elseif ($blockPct -lt 80) { "$esc[93m" }
                    else { "$esc[91m" }

        $barDisplay = $barColor + $filled.PadRight($barWidth) + $esc + "[0m"

    } else {
        # Style 4: Gradient using different density blocks
        $filledLength = [math]::Floor(($blockPct / 100) * $barWidth)
        $emptyLength = $barWidth - $filledLength
        $filled = "▓" * $filledLength
        $empty = "░" * $emptyLength

        $barColor = if ($blockPct -lt 50) { "$esc[92m" }
                    elseif ($blockPct -lt 80) { "$esc[93m" }
                    else { "$esc[91m" }

        $barDisplay = "$barColor$filled$esc[37m$empty$esc[0m"
    }

    # Build block section with progress bar
    $blockSection = " Block $barDisplay $blockPct%"

    # Output the complete status line to stdout
    [Console]::WriteLine("$gitSection $modelSection $ctxSection $blockSection")

} catch {
    # Fallback on error - output error details for debugging
    [Console]::WriteLine("Status: Error - $($_.Exception.Message)")
}

