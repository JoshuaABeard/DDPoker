#!/bin/bash
# Pre-commit hook for Claude Code
# Runs before git commit commands to enforce branch policy and scan for secrets.
# Exit 0 = allow, Exit 2 = block (stderr shown to agent)

INPUT=$(cat)
# Parse command from JSON without jq dependency
COMMAND=$(echo "$INPUT" | grep -oE '"command"\s*:\s*"[^"]*"' | head -1 | sed 's/"command"\s*:\s*"//;s/"$//')

# Only intercept git commit commands
if ! echo "$COMMAND" | grep -qE '^\s*git\s+commit'; then
    exit 0
fi

ERRORS=""

# --- Check 1: Block commits to main ---
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ]; then
    STAGED=$(git diff --cached --name-only 2>/dev/null)
    NON_CLAUDE=$(echo "$STAGED" | grep -v '^\.claude/' | grep -v '^$')
    if [ -n "$NON_CLAUDE" ]; then
        ERRORS="${ERRORS}BLOCKED: Committing non-.claude/ files to $BRANCH. Use a worktree.\n"
    fi
fi

# --- Check 2: Scan staged files for secrets ---
STAGED_DIFF=$(git diff --cached 2>/dev/null)

if [ -n "$STAGED_DIFF" ]; then
    # Private IPs (192.168.x.x, 10.x.x.x)
    ADDED_LINES=$(echo "$STAGED_DIFF" | grep -E '^\+[^+]')

    IP_MATCHES=$(echo "$ADDED_LINES" | grep -oE '192\.168\.[0-9]+\.[0-9]+' | head -5)
    if [ -z "$IP_MATCHES" ]; then
        IP_MATCHES=$(echo "$ADDED_LINES" | grep -oE '10\.[0-9]+\.[0-9]+\.[0-9]+' | head -5)
    fi
    if [ -n "$IP_MATCHES" ]; then
        ERRORS="${ERRORS}SECRET: Private IP addresses found in staged changes: ${IP_MATCHES}\n"
    fi

    # API keys / tokens
    KEY_MATCHES=$(echo "$ADDED_LINES" | grep -iE 'api[_-]?(key|secret)\s*[:=]\s*"?[a-zA-Z0-9]{16,}' | head -3)
    if [ -z "$KEY_MATCHES" ]; then
        KEY_MATCHES=$(echo "$ADDED_LINES" | grep -iE 'access[_-]?token\s*[:=]\s*"?[a-zA-Z0-9]{16,}' | head -3)
    fi
    if [ -n "$KEY_MATCHES" ]; then
        ERRORS="${ERRORS}SECRET: Possible API keys/tokens in staged changes\n"
    fi

    # Passwords in code
    PW_MATCHES=$(echo "$ADDED_LINES" | grep -iE '(password|passwd|pwd)\s*[:=]\s*"[^"]{4,}"' | grep -vi 'placeholder\|example\|YOUR_' | head -3)
    if [ -n "$PW_MATCHES" ]; then
        ERRORS="${ERRORS}SECRET: Possible hardcoded passwords in staged changes\n"
    fi

    # File paths with usernames
    PATH_MATCHES=$(echo "$ADDED_LINES" | grep -iE '(C:\\\\Users\\\\[a-zA-Z]+|/home/[a-zA-Z]+|/Users/[a-zA-Z]+)' | head -3)
    if [ -n "$PATH_MATCHES" ]; then
        ERRORS="${ERRORS}SECRET: User-specific file paths found in staged changes\n"
    fi

    # Certificate/key files being committed
    CERT_FILES=$(git diff --cached --name-only 2>/dev/null | grep -iE '\.(pem|key|pfx|p12|cert|crt|jks|keystore)$' | head -5)
    if [ -n "$CERT_FILES" ]; then
        ERRORS="${ERRORS}SECRET: Certificate/key files staged: ${CERT_FILES}\n"
    fi

    # .env files
    ENV_FILES=$(git diff --cached --name-only 2>/dev/null | grep -iE '\.env$' | head -5)
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
