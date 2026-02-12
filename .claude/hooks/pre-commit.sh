#!/bin/bash
# Pre-commit hook for Claude Code
# Runs before git commit commands to enforce branch policy and scan for secrets.
# Exit 0 = allow, Exit 2 = block (stderr shown to agent)

# Ensure we don't fail on any unexpected error
set +e
trap 'exit 0' ERR

INPUT=$(cat 2>/dev/null || true)

# Parse command from JSON without jq dependency
COMMAND=$(echo "$INPUT" | grep -oE '"command"\s*:\s*"[^"]*"' 2>/dev/null | head -1 | sed 's/"command"\s*:\s*"//;s/"$//' 2>/dev/null || true)

# Only intercept git commit commands
if ! echo "$COMMAND" | grep -qE '^\s*git\s+commit' 2>/dev/null; then
    exit 0
fi

ERRORS=""

# --- Check 1: Block commits to main ---
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)
if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ]; then
    STAGED=$(git diff --cached --name-only 2>/dev/null || true)
    NON_CLAUDE=$(echo "$STAGED" | grep -v '^\.claude/' | grep -v '^$' || true)
    if [ -n "$NON_CLAUDE" ]; then
        ERRORS="${ERRORS}BLOCKED: Committing non-.claude/ files to $BRANCH. Use a worktree.\n"
    fi
fi

# --- Check 2: Scan staged files for secrets ---
STAGED_DIFF=$(git diff --cached 2>/dev/null || true)

if [ -n "$STAGED_DIFF" ]; then
    # Private IPs (192.168.x.x, 10.x.x.x)
    ADDED_LINES=$(echo "$STAGED_DIFF" | grep -E '^\+[^+]' || true)

    IP_MATCHES=$(echo "$ADDED_LINES" | grep -oE '192\.168\.[0-9]+\.[0-9]+' 2>/dev/null | head -5 || true)
    if [ -z "$IP_MATCHES" ]; then
        IP_MATCHES=$(echo "$ADDED_LINES" | grep -oE '10\.[0-9]+\.[0-9]+\.[0-9]+' 2>/dev/null | head -5 || true)
    fi
    if [ -n "$IP_MATCHES" ]; then
        ERRORS="${ERRORS}SECRET: Private IP addresses found in staged changes: ${IP_MATCHES}\n"
    fi

    # API keys / tokens
    KEY_MATCHES=$(echo "$ADDED_LINES" | grep -iE 'api[_-]?(key|secret)\s*[:=]\s*"?[a-zA-Z0-9]{16,}' 2>/dev/null | head -3 || true)
    if [ -z "$KEY_MATCHES" ]; then
        KEY_MATCHES=$(echo "$ADDED_LINES" | grep -iE 'access[_-]?token\s*[:=]\s*"?[a-zA-Z0-9]{16,}' 2>/dev/null | head -3 || true)
    fi
    if [ -n "$KEY_MATCHES" ]; then
        ERRORS="${ERRORS}SECRET: Possible API keys/tokens in staged changes\n"
    fi

    # Passwords in code
    PW_MATCHES=$(echo "$ADDED_LINES" | grep -iE '(password|passwd|pwd)\s*[:=]\s*"[^"]{4,}"' 2>/dev/null | grep -vi 'placeholder\|example\|YOUR_' | head -3 || true)
    if [ -n "$PW_MATCHES" ]; then
        ERRORS="${ERRORS}SECRET: Possible hardcoded passwords in staged changes\n"
    fi

    # File paths with usernames
    PATH_MATCHES=$(echo "$ADDED_LINES" | grep -iE '(C:\\\\Users\\\\[a-zA-Z]+|/home/[a-zA-Z]+|/Users/[a-zA-Z]+)' 2>/dev/null | head -3 || true)
    if [ -n "$PATH_MATCHES" ]; then
        ERRORS="${ERRORS}SECRET: User-specific file paths found in staged changes\n"
    fi

    # Certificate/key files being committed
    CERT_FILES=$(git diff --cached --name-only 2>/dev/null | grep -iE '\.(pem|key|pfx|p12|cert|crt|jks|keystore)$' | head -5 || true)
    if [ -n "$CERT_FILES" ]; then
        ERRORS="${ERRORS}SECRET: Certificate/key files staged: ${CERT_FILES}\n"
    fi

    # .env files
    ENV_FILES=$(git diff --cached --name-only 2>/dev/null | grep -iE '\.env$' | head -5 || true)
    if [ -n "$ENV_FILES" ]; then
        ERRORS="${ERRORS}SECRET: .env file staged: ${ENV_FILES}\n"
    fi
fi

# --- Report ---
if [ -n "$ERRORS" ]; then
    printf "%b" "$ERRORS" >&2
    exit 2
fi

exit 0
