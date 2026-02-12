#!/bin/bash
# Post-commit hook for Claude Code
# Reminds agents to update learnings.md after commits.
# Exit 0 = allow (always), stderr shown to agent

INPUT=$(cat)
# Parse command from JSON without jq dependency
COMMAND=$(echo "$INPUT" | grep -oE '"command"\s*:\s*"[^"]*"' | head -1 | sed 's/"command"\s*:\s*"//;s/"$//')

# Only remind after git commit commands
if echo "$COMMAND" | grep -qE '^\s*git\s+commit'; then
    echo "" >&2
    echo "✓ Commit successful!" >&2
    echo "" >&2
    echo "Reminder: If you encountered non-obvious issues during this work," >&2
    echo "consider updating .claude/learnings.md to help future sessions." >&2
    echo "" >&2
fi

exit 0
