#!/bin/bash
# Post-commit hook for Claude Code
# Reminds agents to update learnings.md after commits.
# Exit 0 = allow (always), stderr shown to agent

# Ensure we don't fail on any unexpected error
set +e

# Read input with timeout to prevent hanging
INPUT=$(timeout 1s cat 2>/dev/null || true)

# Exit silently if no input available
if [ -z "$INPUT" ]; then
    exit 0
fi

# Parse command from JSON without jq dependency
COMMAND=$(echo "$INPUT" | grep -oE '"command"\s*:\s*"[^"]*"' 2>/dev/null | head -1 | sed 's/"command"\s*:\s*"//;s/"$//' 2>/dev/null || true)

# Exit silently if command cannot be parsed
if [ -z "$COMMAND" ]; then
    exit 0
fi

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
