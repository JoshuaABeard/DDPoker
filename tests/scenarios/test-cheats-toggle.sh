#!/usr/bin/env bash
# test-cheats-toggle.sh — Toggle each cheat option and verify effects.
#
# Tests CH-010 through CH-020:
#   - Enable/disable each cheat via /options
#   - Verify observable effects in /state where possible
#   - aifaceup: assert opponent hole cards visible in /state (hard FAIL)
#   - Verify all cheats can be disabled mid-game
#
# Usage:
#   bash tests/scenarios/test-cheats-toggle.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0
CHEATS_TESTED=0

# Helper: set an option and verify it took effect
test_cheat() {
    local key="$1" value="$2" expected_val="$3"
    local body="{\"$key\": $value}"
    api_post_json /options "$body" > /dev/null 2>&1 || { log "FAIL: could not set $key=$value"; FAILURES=$((FAILURES+1)); return; }

    # Read back options to verify
    local options opt_val
    options=$(api GET /options 2>/dev/null) || { log "FAIL: could not read /options"; FAILURES=$((FAILURES+1)); return; }

    # Parse the nested key (e.g., cheat.aifaceup → o.cheat.aifaceup)
    opt_val=$(jget "$options" "o.$key")

    if [[ "$opt_val" == "$expected_val" ]]; then
        log "  OK: $key = $opt_val"
        CHEATS_TESTED=$((CHEATS_TESTED+1))
    else
        log "FAIL: $key — expected $expected_val, got '$opt_val'"
        FAILURES=$((FAILURES+1))
    fi
}

# Wait for first human turn so the game is in progress (hand dealt)
state=$(wait_human_turn 60) || die "Timed out waiting for human turn"

# ============================================================
# CH-010: Show AI Cards Face-Up
# ============================================================
log "=== CH-010: cheat.aifaceup ==="
test_cheat "cheat.aifaceup" "true" "true"

# With aifaceup and a hand in progress, AI hole cards must be visible in /state.
sleep 0.5
state=$(api GET /state 2>/dev/null) || die "Could not read /state"

# Check each non-human player for non-empty holeCards
AIFACEUP_OK=false
player_count=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).length||0')
for idx in $(seq 0 $((player_count - 1))); do
    is_human=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[])[$idx]?.isHuman||false")
    if [[ "$is_human" == "true" ]]; then continue; fi
    ai_cards=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[])[$idx]?.holeCards?.join(',')||''")
    if [[ -n "$ai_cards" && "$ai_cards" != "" ]]; then
        log "  OK: AI player[$idx] hole cards visible with aifaceup: $ai_cards"
        AIFACEUP_OK=true
        break
    fi
done

if [[ "$AIFACEUP_OK" != "true" ]]; then
    log "FAIL: CH-010 — aifaceup=true but no AI hole cards visible in /state (hand must be in progress)"
    FAILURES=$((FAILURES+1))
fi

# Disable aifaceup and verify cards are hidden
test_cheat "cheat.aifaceup" "false" "false"
sleep 0.5
state=$(api GET /state 2>/dev/null) || die "Could not read /state after aifaceup=false"

for idx in $(seq 0 $((player_count - 1))); do
    is_human=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[])[$idx]?.isHuman||false")
    if [[ "$is_human" == "true" ]]; then continue; fi
    ai_cards=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[])[$idx]?.holeCards?.join(',')||''")
    if [[ -n "$ai_cards" && "$ai_cards" != "" ]]; then
        log "FAIL: CH-010 — aifaceup=false but AI player[$idx] hole cards still visible: $ai_cards"
        FAILURES=$((FAILURES+1))
    fi
done
log "  OK: AI hole cards hidden with aifaceup=false"

# ============================================================
# CH-011: Show Folded Hands
# ============================================================
log "=== CH-011: cheat.showfold ==="
test_cheat "cheat.showfold" "true" "true"
test_cheat "cheat.showfold" "false" "false"

# ============================================================
# CH-012: Show Mucked Cards
# ============================================================
log "=== CH-012: cheat.showmuck ==="
test_cheat "cheat.showmuck" "true" "true"
test_cheat "cheat.showmuck" "false" "false"

# ============================================================
# CH-013: Cheat Popup
# ============================================================
log "=== CH-013: cheat.popups ==="
test_cheat "cheat.popups" "true" "true"
test_cheat "cheat.popups" "false" "false"

# ============================================================
# CH-014: Mouseover Hand Strength
# ============================================================
log "=== CH-014: cheat.mouseover ==="
test_cheat "cheat.mouseover" "true" "true"
test_cheat "cheat.mouseover" "false" "false"

# ============================================================
# CH-015: Show Winning Hand at Showdown
# ============================================================
log "=== CH-015: cheat.showdown ==="
test_cheat "cheat.showdown" "true" "true"
test_cheat "cheat.showdown" "false" "false"

# ============================================================
# CH-016: Never Broke
# ============================================================
log "=== CH-016: cheat.neverbroke ==="
test_cheat "cheat.neverbroke" "true" "true"
test_cheat "cheat.neverbroke" "false" "false"

# ============================================================
# CH-017: Pause Before Cards
# ============================================================
log "=== CH-017: cheat.pausecards ==="
test_cheat "cheat.pausecards" "true" "true"
test_cheat "cheat.pausecards" "false" "false"

# ============================================================
# CH-020: Disable all cheats and verify
# ============================================================
log "=== CH-020: Disable All Cheats ==="
api_post_json /options '{
    "cheat.aifaceup": false,
    "cheat.showfold": false,
    "cheat.showmuck": false,
    "cheat.showdown": false,
    "cheat.popups": false,
    "cheat.mouseover": false,
    "cheat.neverbroke": false,
    "cheat.pausecards": false
}' > /dev/null 2>&1 || { log "FAIL: could not disable all cheats"; FAILURES=$((FAILURES+1)); }

options=$(api GET /options 2>/dev/null) || true
all_false=true
for key in aifaceup showfold showmuck showdown popups mouseover neverbroke pausecards; do
    val=$(jget "$options" "o.cheat&&o.cheat.$key")
    if [[ "$val" != "false" ]]; then
        log "FAIL: cheat.$key still $val after disable-all"
        all_false=false
        FAILURES=$((FAILURES+1))
    fi
done
[[ "$all_false" == "true" ]] && log "  OK: All cheats disabled"

screenshot "cheats-all-disabled"

# Validate game still healthy
vresult=$(api GET /validate 2>/dev/null) || true
im_valid=$(jget "$vresult" 'o.inputModeConsistent')
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
warns=$(jget "$vresult" '(o.warnings||[]).join("; ")')

if [[ "$im_valid" == "true" ]]; then
    log "  OK: inputModeConsistent=true"
else
    log "FAIL: inputModeConsistent=$im_valid"
    FAILURES=$((FAILURES+1))
fi

if [[ "$cc_valid" == "true" ]]; then
    log "  OK: chipConservation.valid=true"
else
    log "FAIL: chipConservation.valid=$cc_valid"
    [[ -n "$warns" ]] && log "  warnings: $warns"
    FAILURES=$((FAILURES+1))
fi

log "---"
log "Cheats tested: $CHEATS_TESTED"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "All cheat toggles verified: enable/disable for 8 cheats + aifaceup visibility + bulk disable"
