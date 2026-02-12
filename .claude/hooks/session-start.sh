#!/bin/bash
# SessionStart hook for Claude Code
# Shows context at the start of each session.
# Exit 0 = allow (always), stderr shown to agent

echo "" >&2
echo "=== Session Context ===" >&2
echo "" >&2

# Current branch
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
echo "Branch: $BRANCH" >&2

# Recent commits (last 3)
echo "" >&2
echo "Recent commits:" >&2
git log --oneline -3 2>/dev/null | sed 's/^/  /' >&2

# Active plans (status: active or in progress)
echo "" >&2
ACTIVE_PLANS=$(find .claude/plans -maxdepth 1 -name "*.md" -type f 2>/dev/null | while read plan; do
    if grep -q "Status.*active\|Status.*in.*progress" "$plan" 2>/dev/null; then
        basename "$plan"
    fi
done)

if [ -n "$ACTIVE_PLANS" ]; then
    echo "Active plans:" >&2
    echo "$ACTIVE_PLANS" | sed 's/^/  /' >&2
else
    echo "No active plans" >&2
fi

# Untracked/modified files count
echo "" >&2
MODIFIED=$(git status --short 2>/dev/null | wc -l)
if [ "$MODIFIED" -gt 0 ]; then
    echo "Working tree: $MODIFIED modified/untracked files" >&2
else
    echo "Working tree: clean" >&2
fi

# Check for failing tests (look for recent test failures in target/)
echo "" >&2
FAILED_TESTS=$(find . -path "*/target/surefire-reports/*.txt" -newer .git/HEAD -type f 2>/dev/null | \
    xargs grep -l "FAILURE\|ERROR" 2>/dev/null | wc -l)
if [ "$FAILED_TESTS" -gt 0 ]; then
    echo "⚠ Warning: $FAILED_TESTS test failure(s) detected in recent runs" >&2
else
    echo "Tests: No recent failures detected" >&2
fi

echo "" >&2
echo "Tip: Check .claude/learnings.md for known gotchas" >&2
echo "======================" >&2
echo "" >&2

exit 0
