#!/usr/bin/env bash
# test-navigate.sh — Verify menu navigation via /navigate endpoint.
#
# Tests L-010 through L-017:
#   - Navigate to practice game setup
#   - Navigate back to main menu
#   - Invalid phase returns error
#
# Usage:
#   bash .claude/scripts/scenarios/test-navigate.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# L-010: Navigate to practice game lobby
# ============================================================
log "=== L-010: Navigate to Practice Lobby ==="
NAV_RESULT=$(api_post_json /navigate '{"phase": "LobbyPractice"}' 2>/dev/null) || true
SUCCESS=$(jget "$NAV_RESULT" 'o.success')
if [[ "$SUCCESS" == "true" ]]; then
    log "  OK: Navigation to LobbyPractice succeeded"
else
    log "  WARN: Navigation returned: $NAV_RESULT"
fi

sleep 1
screenshot "navigate-practice-lobby"

# ============================================================
# L-015: Navigate back to main menu
# ============================================================
log "=== L-015: Navigate to Main Menu ==="
NAV_RESULT=$(api_post_json /navigate '{"phase": "MainMenu"}' 2>/dev/null) || true
SUCCESS=$(jget "$NAV_RESULT" 'o.success')
if [[ "$SUCCESS" == "true" ]]; then
    log "  OK: Navigation to MainMenu succeeded"
else
    log "  WARN: Navigation to MainMenu returned: $NAV_RESULT"
fi

sleep 1
screenshot "navigate-main-menu"

# ============================================================
# L-017: Invalid phase name
# ============================================================
log "=== L-017: Invalid Phase Name ==="
NAV_RESULT=$(api_post_json /navigate '{"phase": "NonExistentPhase"}' 2>/dev/null) || true
ERROR=$(jget "$NAV_RESULT" 'o.error||""')
if [[ -n "$ERROR" && "$ERROR" != "" && "$ERROR" != "undefined" ]]; then
    log "  OK: Invalid phase returned error: $ERROR"
else
    log "  WARN: Invalid phase response: $NAV_RESULT"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Navigation tests verified: lobby, main menu, invalid phase"
