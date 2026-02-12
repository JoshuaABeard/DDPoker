#!/bin/bash
# Post-commit hook for Claude Code
# Reminds agents to update learnings.md after commits.
# Exit 0 = allow (always), stderr shown to agent

# Only suppress stderr during input reading, not during the reminder output
exec 3>&2
exec 2>/dev/null

# Ensure we don't fail on any unexpected error
set +e

# Try to read input - if this fails for any reason, just exit silently
INPUT=""
read -r -t 0.1 INPUT || exit 0

# Restore stderr for reminder messages
exec 2>&3

# Exit silently if no input
[ -z "$INPUT" ] && exit 0

# Parse command from JSON without jq dependency
COMMAND=$(echo "$INPUT" | grep -oE '"command"\s*:\s*"[^"]*"' | head -1 | sed 's/"command"\s*:\s*"//;s/"$//' 2>/dev/null || true)

# Exit silently if command cannot be parsed
[ -z "$COMMAND" ] && exit 0

# Only remind after git commit commands
if echo "$COMMAND" | grep -qE '^\s*git\s+commit' 2>/dev/null; then
    echo "" >&2
    echo "Commit successful." >&2
    echo "" >&2
    echo "Reminder: If you encountered non-obvious issues during this work," >&2
    echo "consider updating .claude/learnings.md to help future sessions." >&2
    echo "" >&2
fi

exit 0
