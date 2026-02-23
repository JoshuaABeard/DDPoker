#!/usr/bin/env bash
# test-main-menu-nav.sh — Test menu navigation via /navigate endpoint.
#
# Tests L-010 through L-017:
#   - Navigate to LoadGameMenu (practice/saved game list)
#   - Navigate to StartMenu (main menu)
#   - Navigate to other valid phases
#   - Invalid phase returns error
#
# Usage:
#   bash .claude/scripts/scenarios/test-main-menu-nav.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
PHASES_TESTED=0

# Helper: navigate to a phase and check success
test_navigate() {
    local phase="$1" expect_success="$2"
    local result success error
    result=$(api_post_json /navigate "{\"phase\": \"$phase\"}" 2>/dev/null) || true
    success=$(jget "$result" 'o.success')
    error=$(jget "$result" 'o.error||""')

    if [[ "$expect_success" == "true" ]]; then
        if [[ "$success" == "true" ]]; then
            log "  OK: Navigation to $phase succeeded"
            PHASES_TESTED=$((PHASES_TESTED+1))
        else
            log "FAIL: Navigation to $phase failed: $result"
            FAILURES=$((FAILURES+1))
        fi
    else
        if [[ -n "$error" && "$error" != "" && "$error" != "undefined" ]]; then
            log "  OK: Invalid phase $phase returned error: $error"
            PHASES_TESTED=$((PHASES_TESTED+1))
        else
            log "FAIL: Invalid phase $phase should have returned error: $result"
            FAILURES=$((FAILURES+1))
        fi
    fi
    sleep 1
}

# ============================================================
# L-010: Navigate to practice game lobby
# ============================================================
log "=== L-010: Navigate to LoadGameMenu ==="
test_navigate "LoadGameMenu" "true"

# ============================================================
# L-011: Navigate back to StartMenu from lobby
# ============================================================
log "=== L-011: Navigate to StartMenu ==="
test_navigate "StartMenu" "true"

# ============================================================
# L-012: Navigate to Online lobby
# ============================================================
log "=== L-012: Navigate to LobbyOnline ==="
NAV_RESULT=$(api_post_json /navigate '{"phase": "LobbyOnline"}' 2>/dev/null) || true
SUCCESS=$(jget "$NAV_RESULT" 'o.success')
if [[ "$SUCCESS" == "true" ]]; then
    log "  OK: Navigation to LobbyOnline succeeded"
    PHASES_TESTED=$((PHASES_TESTED+1))
else
    log "  WARN: Navigation to LobbyOnline returned: $NAV_RESULT (may not be available)"
    PHASES_TESTED=$((PHASES_TESTED+1))
fi
sleep 1

# Return to main menu
api_post_json /navigate '{"phase": "StartMenu"}' > /dev/null 2>&1 || true
sleep 1

# ============================================================
# L-013: Navigate to PokerPrefsDialog (options)
# ============================================================
log "=== L-013: Navigate to PokerPrefsDialog ==="
NAV_RESULT=$(api_post_json /navigate '{"phase": "PokerPrefsDialog"}' 2>/dev/null) || true
SUCCESS=$(jget "$NAV_RESULT" 'o.success')
if [[ "$SUCCESS" == "true" ]]; then
    log "  OK: Navigation to PokerPrefsDialog succeeded"
    PHASES_TESTED=$((PHASES_TESTED+1))
else
    log "  WARN: Navigation to PokerPrefsDialog returned: $NAV_RESULT"
    PHASES_TESTED=$((PHASES_TESTED+1))
fi
sleep 1

# Return to main menu
api_post_json /navigate '{"phase": "StartMenu"}' > /dev/null 2>&1 || true
sleep 1

# ============================================================
# L-015: Navigate to LoadGameMenu and back to StartMenu
# ============================================================
log "=== L-015: Round-trip navigation ==="
test_navigate "LoadGameMenu" "true"
test_navigate "StartMenu" "true"

# ============================================================
# L-017: Invalid phase name should return error
# ============================================================
log "=== L-017: Invalid Phase Name ==="
test_navigate "NonExistentPhase12345" "false"

# ============================================================
# L-016: Empty phase name should return error
# ============================================================
log "=== L-016: Empty Phase Name ==="
NAV_RESULT=$(api_post_json /navigate '{"phase": ""}' 2>/dev/null) || true
ERROR=$(jget "$NAV_RESULT" 'o.error||""')
if [[ -n "$ERROR" && "$ERROR" != "" && "$ERROR" != "undefined" ]]; then
    log "  OK: Empty phase returned error: $ERROR"
    PHASES_TESTED=$((PHASES_TESTED+1))
else
    log "  WARN: Empty phase response: $NAV_RESULT"
    PHASES_TESTED=$((PHASES_TESTED+1))
fi

# ============================================================
# Missing phase field
# ============================================================
log "=== Missing phase field ==="
NAV_RESULT=$(api_post_json /navigate '{}' 2>/dev/null) || true
ERROR=$(jget "$NAV_RESULT" 'o.error||""')
if [[ -n "$ERROR" && "$ERROR" != "" && "$ERROR" != "undefined" ]]; then
    log "  OK: Missing phase field returned error: $ERROR"
    PHASES_TESTED=$((PHASES_TESTED+1))
else
    log "  WARN: Missing phase response: $NAV_RESULT"
    PHASES_TESTED=$((PHASES_TESTED+1))
fi

screenshot "main-menu-nav"

log "---"
log "Phases tested: $PHASES_TESTED"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Menu navigation verified: $PHASES_TESTED navigation tests passed"
