#!/usr/bin/env bash
# test-app-launch.sh — Verify app launches and basic endpoints respond.
#
# Tests L-001, L-002:
#   - Health endpoint responds with "ok"
#   - Version in /state matches /system-info
#
# Usage:
#   bash .claude/scripts/scenarios/test-app-launch.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# L-001: Health endpoint responds
# ============================================================
log "=== L-001: Health Endpoint ==="
HEALTH=$(api GET /health 2>/dev/null) || die "Health endpoint unreachable"
STATUS=$(jget "$HEALTH" 'o.status||""')
if [[ "$STATUS" == "ok" ]]; then
    log "  OK: Health status is 'ok'"
else
    log "FAIL: Health status is '$STATUS', expected 'ok'"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# L-002: Version consistency between /state and /system-info
# ============================================================
log "=== L-002: Version Consistency ==="
INFO=$(api GET /system-info 2>/dev/null) || die "Could not read /system-info"
INFO_VERSION=$(jget "$INFO" 'o.version||""')
log "  /system-info version: $INFO_VERSION"

if [[ -z "$INFO_VERSION" || "$INFO_VERSION" == "" || "$INFO_VERSION" == "undefined" ]]; then
    log "FAIL: /system-info version is missing"
    FAILURES=$((FAILURES+1))
else
    log "  OK: /system-info version present"
fi

STATE=$(api GET /state 2>/dev/null) || die "Could not read /state"
STATE_VERSION=$(jget "$STATE" 'o.version||""')
log "  /state version: $STATE_VERSION"

if [[ -z "$STATE_VERSION" || "$STATE_VERSION" == "" || "$STATE_VERSION" == "undefined" ]]; then
    log "FAIL: /state version is missing"
    FAILURES=$((FAILURES+1))
else
    log "  OK: /state version present"
fi

if [[ "$INFO_VERSION" == "$STATE_VERSION" ]]; then
    log "  OK: Versions match ($INFO_VERSION)"
else
    log "FAIL: Version mismatch: /system-info=$INFO_VERSION, /state=$STATE_VERSION"
    FAILURES=$((FAILURES+1))
fi

# Verify additional system-info fields are present
CONFIG_DIR=$(jget "$INFO" 'o.configDir||""')
JAVA_VER=$(jget "$INFO" 'o.javaVersion||""')
OS_NAME=$(jget "$INFO" 'o.osName||""')

log "  configDir: $CONFIG_DIR"
log "  javaVersion: $JAVA_VER"
log "  osName: $OS_NAME"

if [[ -n "$CONFIG_DIR" && "$CONFIG_DIR" != "" ]]; then
    log "  OK: configDir present"
else
    log "FAIL: configDir missing from /system-info"
    FAILURES=$((FAILURES+1))
fi

if [[ -n "$JAVA_VER" && "$JAVA_VER" != "" ]]; then
    log "  OK: javaVersion present"
else
    log "FAIL: javaVersion missing from /system-info"
    FAILURES=$((FAILURES+1))
fi

screenshot "app-launch"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "App launch verified: health=ok, version=$INFO_VERSION matches across endpoints"
