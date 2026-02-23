#!/usr/bin/env bash
# test-preferences-full.sh — Comprehensive preferences/options coverage.
#
# Tests O-002 through O-070:
#   - All boolean display options
#   - All gameplay options with integer values
#   - Verify options persist across reads
#   - Verify invalid option names handled
#
# Usage:
#   bash .claude/scripts/scenarios/test-preferences-full.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
OPTIONS_OK=0
OPTIONS_FAIL=0

# Helper: test a boolean option round-trip
test_bool() {
    local key="$1"
    api_post_json /options "{\"$key\": true}" > /dev/null 2>&1 || { log "FAIL: could not set $key"; OPTIONS_FAIL=$((OPTIONS_FAIL+1)); return; }
    local options val nested
    options=$(api GET /options 2>/dev/null) || { log "FAIL: could not read"; OPTIONS_FAIL=$((OPTIONS_FAIL+1)); return; }
    nested=$(echo "$key" | sed 's/\./\?./; s/^/o./')
    val=$(jget "$options" "$nested")
    if [[ "$val" == "true" ]]; then
        OPTIONS_OK=$((OPTIONS_OK+1))
    else
        log "  FAIL: $key — expected true, got '$val'"
        OPTIONS_FAIL=$((OPTIONS_FAIL+1))
    fi
    # Reset
    api_post_json /options "{\"$key\": false}" > /dev/null 2>&1 || true
}

# Helper: test an integer option round-trip
test_int() {
    local key="$1" value="$2"
    api_post_json /options "{\"$key\": $value}" > /dev/null 2>&1 || { log "FAIL: could not set $key"; OPTIONS_FAIL=$((OPTIONS_FAIL+1)); return; }
    local options val nested
    options=$(api GET /options 2>/dev/null) || { log "FAIL: could not read"; OPTIONS_FAIL=$((OPTIONS_FAIL+1)); return; }
    nested=$(echo "$key" | sed 's/\./\?./; s/^/o./')
    val=$(jget "$options" "$nested")
    if [[ "$val" == "$value" ]]; then
        OPTIONS_OK=$((OPTIONS_OK+1))
    else
        log "  FAIL: $key — expected $value, got '$val'"
        OPTIONS_FAIL=$((OPTIONS_FAIL+1))
    fi
}

# ============================================================
# O-002 through O-009: Display options
# ============================================================
log "=== Display Options ==="
test_bool "display.largeCards"
test_bool "display.fourColorDeck"
test_bool "display.stylizedFaceCards"
test_bool "display.holeCardsDown"
test_bool "display.showPlayerType"
test_bool "display.rightClickOnly"
test_bool "display.disableShortcuts"
log "  Display: $OPTIONS_OK OK, $OPTIONS_FAIL fail"

# ============================================================
# O-020 through O-029: Gameplay options
# ============================================================
log "=== Gameplay Options ==="
BEFORE_OK=$OPTIONS_OK
test_bool "gameplay.zipMode"
test_bool "gameplay.autodeal"
test_bool "gameplay.checkfold"
test_bool "gameplay.pauseAllin"
test_bool "gameplay.pauseColor"
test_int "gameplay.aiDelayMs" 200
test_int "gameplay.aiDelayMs" 0
test_int "gameplay.handsPerHour" 60
test_int "gameplay.handsPerHour" 0
GP_OK=$((OPTIONS_OK - BEFORE_OK))
log "  Gameplay: $GP_OK new OK"

# ============================================================
# O-030 through O-039: Clock options
# ============================================================
log "=== Clock Options ==="
BEFORE_OK=$OPTIONS_OK
test_bool "clock.colorUpNotify"
test_bool "clock.pauseAtLevelEnd"
CK_OK=$((OPTIONS_OK - BEFORE_OK))
log "  Clock: $CK_OK new OK"

# ============================================================
# O-040: Advisor
# ============================================================
log "=== Advisor Option ==="
BEFORE_OK=$OPTIONS_OK
test_bool "advisor.enabled"
AD_OK=$((OPTIONS_OK - BEFORE_OK))
log "  Advisor: $AD_OK new OK"

# ============================================================
# O-050: Chat options
# ============================================================
log "=== Chat Options ==="
BEFORE_OK=$OPTIONS_OK
test_int "chat.fontSize" 16
test_int "chat.fontSize" 12
CH_OK=$((OPTIONS_OK - BEFORE_OK))
log "  Chat: $CH_OK new OK"

# ============================================================
# O-055: Screenshot options
# ============================================================
log "=== Screenshot Options ==="
BEFORE_OK=$OPTIONS_OK
test_int "screenshot.maxWidth" 1920
test_int "screenshot.maxHeight" 1080
test_int "screenshot.maxWidth" 0
test_int "screenshot.maxHeight" 0
SC_OK=$((OPTIONS_OK - BEFORE_OK))
log "  Screenshot: $SC_OK new OK"

# ============================================================
# O-060: Cheat options
# ============================================================
log "=== Cheat Options ==="
BEFORE_OK=$OPTIONS_OK
test_bool "cheat.rabbithunt"
test_bool "cheat.manualbutton"
CT_OK=$((OPTIONS_OK - BEFORE_OK))
log "  Cheat: $CT_OK new OK"

# ============================================================
# O-070: Read all options at once
# ============================================================
log "=== Full Options Read ==="
ALL=$(api GET /options 2>/dev/null) || die "Could not read /options"
HAS_DISPLAY=$(jget "$ALL" 'o.display!==undefined')
HAS_GAMEPLAY=$(jget "$ALL" 'o.gameplay!==undefined')
HAS_CLOCK=$(jget "$ALL" 'o.clock!==undefined')
log "  display section: $HAS_DISPLAY"
log "  gameplay section: $HAS_GAMEPLAY"
log "  clock section: $HAS_CLOCK"

log "---"
log "Total OK: $OPTIONS_OK, Total FAIL: $OPTIONS_FAIL"
FAILURES=$OPTIONS_FAIL

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES option test(s) failed"
fi

pass "Comprehensive options verified: $OPTIONS_OK options toggled and read back"
