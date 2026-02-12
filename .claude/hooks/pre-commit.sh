#!/bin/bash
# Pre-commit hook for Claude Code
# Runs before git commit commands to enforce branch policy and scan for secrets.
# Exit 0 = allow, Exit 2 = block (stderr shown to agent)

# Ensure we don't fail on any unexpected error
set +e

INPUT=$(cat 2>/dev/null || true)

# Parse command from JSON without jq dependency
COMMAND=$(echo "$INPUT" | grep -oE '"command"\s*:\s*"[^"]*"' 2>/dev/null | head -1 | sed 's/"command"\s*:\s*"//;s/"$//' 2>/dev/null || true)

# Only intercept git commit commands
if ! echo "$COMMAND" | grep -qE '^\s*git\s+commit' 2>/dev/null; then
    exit 0
fi

ERRORS=""

# --- Check 1: Block code commits to main ---
# Policy: .claude/ files, .gitignore, *.md, and other non-code files are OK on main.
# Only block commits that include code/test files (java, xml, properties, etc.)
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)
if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ]; then
    STAGED=$(git diff --cached --name-only 2>/dev/null || true)
    # Filter out allowed paths: .claude/, .gitignore, .gitattributes, markdown, LICENSE, README
    CODE_FILES=$(echo "$STAGED" | grep -vE '^\.claude/|^\.git(ignore|attributes)$|\.md$|^LICENSE|^README|^SECURITY' | grep -v '^$' || true)
    if [ -n "$CODE_FILES" ]; then
        ERRORS="${ERRORS}BLOCKED: Committing code files to $BRANCH. Use a worktree.\n"
    fi
fi

# --- Check 2: Scan staged files for secrets ---
STAGED_DIFF=$(git diff --cached 2>/dev/null || true)

if [ -n "$STAGED_DIFF" ]; then
    ADDED_LINES=$(echo "$STAGED_DIFF" | grep -E '^\+[^+]' || true)

    # Private IPs (192.168.x.x)
    IP_MATCHES=$(echo "$ADDED_LINES" | grep -oE '192\.168\.[0-9]+\.[0-9]+' 2>/dev/null | head -5 || true)
    # 10.x.x.x — exclude version-like patterns (10.0.x, 10.1.x) and common config IPs
    if [ -z "$IP_MATCHES" ]; then
        IP_MATCHES=$(echo "$ADDED_LINES" | grep -oE '10\.[0-9]+\.[0-9]+\.[0-9]+' 2>/dev/null | \
            grep -vE '^10\.[0-2]\.[0-9]+\.[0-9]+$' | head -5 || true)
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

    # Passwords in code (exclude test/mock/fake/example patterns)
    PW_MATCHES=$(echo "$ADDED_LINES" | grep -iE '(password|passwd|pwd)\s*[:=]\s*"[^"]{4,}"' 2>/dev/null | \
        grep -viE 'placeholder|example|YOUR_|test|mock|fake|dummy|sample' | head -3 || true)
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
