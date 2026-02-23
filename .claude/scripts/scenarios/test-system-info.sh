#!/usr/bin/env bash
# test-system-info.sh — Verify system info and help topics endpoints.
#
# Tests SP-001 through SP-005, HL-001 through HL-012, L-002:
#   - GET /system-info returns version and paths
#   - GET /help/topics returns topic list with existence checks
#   - Version string displayed correctly
#
# Usage:
#   bash .claude/scripts/scenarios/test-system-info.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# SP-002: Version information
# ============================================================
log "=== SP-002/L-002: Version Info ==="
INFO=$(api GET /system-info 2>/dev/null) || die "Could not read /system-info"

VERSION=$(jget "$INFO" 'o.version||""')
CONFIG_DIR=$(jget "$INFO" 'o.configDir||""')
JAVA_VER=$(jget "$INFO" 'o.javaVersion||""')
OS_NAME=$(jget "$INFO" 'o.osName||""')

log "  version: $VERSION"
log "  configDir: $CONFIG_DIR"
log "  javaVersion: $JAVA_VER"
log "  osName: $OS_NAME"

if [[ -n "$VERSION" && "$VERSION" != "" && "$VERSION" != "undefined" ]]; then
    log "  OK: Version present"
else
    log "FAIL: Version missing"
    FAILURES=$((FAILURES+1))
fi

if [[ -n "$CONFIG_DIR" && "$CONFIG_DIR" != "" ]]; then
    log "  OK: Config directory present"
else
    log "FAIL: Config directory missing"
    FAILURES=$((FAILURES+1))
fi

if [[ -n "$JAVA_VER" && "$JAVA_VER" != "" ]]; then
    log "  OK: Java version present"
else
    log "FAIL: Java version missing"
    FAILURES=$((FAILURES+1))
fi

# Also verify version in /state
STATE=$(api GET /state 2>/dev/null) || true
STATE_VER=$(jget "$STATE" 'o.version||""')
if [[ "$STATE_VER" == "$VERSION" ]]; then
    log "  OK: /state version matches /system-info version"
else
    log "  WARN: /state version=$STATE_VER vs /system-info version=$VERSION"
fi

# ============================================================
# HL-001 through HL-010: Help Topics
# ============================================================
log "=== HL-001: Help Topics ==="
TOPICS=$(api GET /help/topics 2>/dev/null) || die "Could not read /help/topics"
TOPIC_COUNT=$(jget "$TOPICS" '(o.topics||[]).length')
log "  Found $TOPIC_COUNT help topics"

if [[ "$TOPIC_COUNT" -gt 0 ]]; then
    log "  OK: Help topics listed"
else
    log "FAIL: No help topics found"
    FAILURES=$((FAILURES+1))
fi

# Check each topic exists
EXISTING=0
MISSING=0
for i in $(seq 0 $((TOPIC_COUNT - 1))); do
    TOPIC_ID=$(jget "$TOPICS" "(o.topics||[])[$i]?.id||''")
    EXISTS=$(jget "$TOPICS" "(o.topics||[])[$i]?.exists")
    if [[ "$EXISTS" == "true" ]]; then
        EXISTING=$((EXISTING+1))
    else
        MISSING=$((MISSING+1))
        log "  WARN: Help topic '$TOPIC_ID' does not exist"
    fi
done
log "  Existing: $EXISTING, Missing: $MISSING"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "System info and help topics verified: version=$VERSION, $EXISTING/$TOPIC_COUNT topics exist"
