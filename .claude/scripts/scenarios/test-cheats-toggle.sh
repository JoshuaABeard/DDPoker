#!/usr/bin/env bash
# test-cheats-toggle.sh — Toggle each cheat option and verify effects.
#
# Tests CH-010 through CH-020:
#   - Enable/disable each cheat via /options
#   - Verify observable effects in /state where possible
#   - Verify all cheats can be disabled mid-game
#
# Usage:
#   bash .claude/scripts/scenarios/test-cheats-toggle.sh [options]

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
    local nested_path
    nested_path=$(echo "$key" | sed 's/\./\&\&o./g; s/^/o./')
    opt_val=$(jget "$options" "$nested_path")

    if [[ "$opt_val" == "$expected_val" ]]; then
        log "  OK: $key = $opt_val"
        CHEATS_TESTED=$((CHEATS_TESTED+1))
    else
        log "FAIL: $key — expected $expected_val, got '$opt_val'"
        FAILURES=$((FAILURES+1))
    fi
}

# Wait for first human turn so the game is running
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game start"

# ============================================================
# CH-010: Show AI Cards Face-Up
# ============================================================
log "=== CH-010: cheat.aifaceup ==="
test_cheat "cheat.aifaceup" "true" "true"

# With aifaceup, AI hole cards should be visible in /state
sleep 0.5
state=$(api GET /state 2>/dev/null) || true
human_seat=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.seat||0')
ai_cards=$(jget "$state" \
    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p&&!p.isHuman).map(p=>(p.holeCards||[]).join(',')).join(';')")
if [[ -n "$ai_cards" && "$ai_cards" != "" && "$ai_cards" != ";" ]]; then
    log "  OK: AI cards visible with aifaceup: $ai_cards"
else
    log "  WARN: AI cards not visible in /state (may need hand in progress)"
fi

# Disable
test_cheat "cheat.aifaceup" "false" "false"

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
log "  inputModeConsistent: $im_valid"

log "---"
log "Cheats tested: $CHEATS_TESTED"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "All cheat toggles verified: enable/disable for 8 cheats + bulk disable"
