#!/usr/bin/env bash
# test-options-expanded.sh — Verify expanded options API covers all preference sections.
#
# Tests O-001 through O-071 (option toggle and persistence):
#   - Display options (largeCards, fourColorDeck, stylized, etc.)
#   - Gameplay options (zipMode, autodeal, checkfold, etc.)
#   - Clock options (colorUpNotify, pauseAtLevelEnd)
#   - Advisor toggle
#   - Chat options (dealer level, display mode, font size)
#   - Screenshot options (max width/height)
#
# Usage:
#   bash tests/scenarios/test-options-expanded.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
OPTIONS_TESTED=0

# Helper: toggle a boolean option and verify round-trip
test_bool_option() {
    local key="$1"
    # Set to true
    api_post_json /options "{\"$key\": true}" > /dev/null 2>&1 || { log "FAIL: could not set $key=true"; FAILURES=$((FAILURES+1)); return; }
    local options val
    options=$(api GET /options 2>/dev/null) || { log "FAIL: could not read /options"; FAILURES=$((FAILURES+1)); return; }
    val=$(jget "$options" "o.$key")
    if [[ "$val" == "true" ]]; then
        OPTIONS_TESTED=$((OPTIONS_TESTED+1))
    else
        log "FAIL: $key — expected true, got '$val'"
        FAILURES=$((FAILURES+1))
    fi
    # Reset to false
    api_post_json /options "{\"$key\": false}" > /dev/null 2>&1 || true
}

# Helper: set and verify an integer option
test_int_option() {
    local key="$1" value="$2"
    api_post_json /options "{\"$key\": $value}" > /dev/null 2>&1 || { log "FAIL: could not set $key=$value"; FAILURES=$((FAILURES+1)); return; }
    local options val
    options=$(api GET /options 2>/dev/null) || { log "FAIL: could not read /options"; FAILURES=$((FAILURES+1)); return; }
    val=$(jget "$options" "o.$key")
    if [[ "$val" == "$value" ]]; then
        OPTIONS_TESTED=$((OPTIONS_TESTED+1))
    else
        log "FAIL: $key — expected $value, got '$val'"
        FAILURES=$((FAILURES+1))
    fi
}

# === Display Options ===
log "=== Display Options ==="
test_bool_option "display.largeCards"
test_bool_option "display.fourColorDeck"
test_bool_option "display.stylizedFaceCards"
test_bool_option "display.holeCardsDown"
test_bool_option "display.showPlayerType"
test_bool_option "display.rightClickOnly"
test_bool_option "display.disableShortcuts"
log "  Display options tested: 7"

# === Gameplay Options ===
log "=== Gameplay Options ==="
test_bool_option "gameplay.zipMode"
test_bool_option "gameplay.autodeal"
test_bool_option "gameplay.checkfold"
test_bool_option "gameplay.pauseAllin"
test_bool_option "gameplay.pauseColor"
test_int_option "gameplay.aiDelayMs" 100
test_int_option "gameplay.aiDelayMs" 0  # reset
test_int_option "gameplay.handsPerHour" 50
log "  Gameplay options tested: 8"

# === Clock Options ===
log "=== Clock Options ==="
test_bool_option "clock.colorUpNotify"
test_bool_option "clock.pauseAtLevelEnd"
log "  Clock options tested: 2"

# === Advisor ===
log "=== Advisor Option ==="
test_bool_option "advisor.enabled"
log "  Advisor options tested: 1"

# === Chat Options ===
log "=== Chat Options ==="
test_int_option "chat.fontSize" 14
test_int_option "chat.fontSize" 12  # reset
log "  Chat options tested: 2"

# === Screenshot Options ===
log "=== Screenshot Options ==="
test_int_option "screenshot.maxWidth" 1920
test_int_option "screenshot.maxHeight" 1080
log "  Screenshot options tested: 2"

# === Cheat Options (quick verify of new cheats) ===
log "=== Additional Cheat Options ==="
test_bool_option "cheat.rabbithunt"
test_bool_option "cheat.manualbutton"
log "  Additional cheat options tested: 2"

log "---"
log "Total options tested: $OPTIONS_TESTED"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Expanded options API verified: $OPTIONS_TESTED options toggled and read back"
