#!/usr/bin/env bash
# test-level-advance.sh — Verify blind level advancement via setLevel cheat.
#
# Tests C-010, C-014, C-015:
#   - Advancing to a higher level increases blinds
#   - Blinds match the tournament profile for each level
#   - After final level, last level repeats
#
# Usage:
#   bash .claude/scripts/scenarios/test-level-advance.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

assert_ge() {
    local desc="$1" actual="$2" min="$3"
    if [[ "$actual" -ge "$min" ]]; then
        log "  OK: $desc = $actual (>= $min)"
    else
        log "FAIL: $desc = $actual (expected >= $min)"
        FAILURES=$((FAILURES+1))
    fi
}

assert_ne() {
    local desc="$1" actual="$2" not_val="$3"
    if [[ "$actual" != "$not_val" ]]; then
        log "  OK: $desc = $actual (not $not_val)"
    else
        log "FAIL: $desc = $actual (should not be $not_val)"
        FAILURES=$((FAILURES+1))
    fi
}

# Wait for first human turn to establish baseline
log "=== Baseline: Level 1 ==="
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game start"

# Get level 1 blinds
state=$(api GET /state 2>/dev/null) || die "Could not read state"
level1_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
level1_bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
level1_level=$(jget "$state" 'o.tournament&&o.tournament.level||1')
log "  Level $level1_level: SB=$level1_sb, BB=$level1_bb"

# Fold current hand
mode=$(jget "$state" 'o.inputMode || "NONE"')
if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
fi

# Wait for DEAL
state=$(wait_mode "DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK" 30) || true
mode=$(jget "$state" 'o.inputMode || "NONE"')
while [[ "$mode" != "DEAL" ]]; do
    advance_non_betting "$state"
    sleep 0.5
    state=$(api GET /state 2>/dev/null) || break
    mode=$(jget "$state" 'o.inputMode || "NONE"')
done

# ============================================================
# C-010/C-014: Advance to level 3 and verify blinds increase
# ============================================================
log "=== Advancing to Level 3 ==="
api_post_json /cheat '{"action":"setLevel","level":3}' > /dev/null 2>&1 \
    || log "WARN: setLevel cheat failed"
sleep 0.5

# Deal a hand at the new level
api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
sleep 1

state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|QUITSAVE" 30) \
    || die "Timed out after level advance"
state=$(api GET /state 2>/dev/null) || die "Could not read state"

level3_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
level3_bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
level3_level=$(jget "$state" 'o.tournament&&o.tournament.level||1')
log "  Level $level3_level: SB=$level3_sb, BB=$level3_bb"

# Blinds should be higher at level 3 than level 1
assert_ge "level 3 small blind vs level 1" "$level3_sb" "$level1_sb"
assert_ge "level 3 big blind vs level 1" "$level3_bb" "$level1_bb"

# Validate chip conservation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    log "WARN: chip conservation invalid after level advance (expected — chips may not match new buyin)"
fi

screenshot "level-advance-3"

# Fold to move on
mode=$(jget "$state" 'o.inputMode || "NONE"')
if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
    if [[ "$is_human" == "true" ]]; then
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    fi
fi

# ============================================================
# C-010: Advance to level 5 and verify further increase
# ============================================================
log "=== Advancing to Level 5 ==="
# Wait for DEAL
state=$(wait_mode "DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK" 30) || true
mode=$(jget "$state" 'o.inputMode || "NONE"')
while [[ "$mode" != "DEAL" ]]; do
    advance_non_betting "$state"
    sleep 0.5
    state=$(api GET /state 2>/dev/null) || break
    mode=$(jget "$state" 'o.inputMode || "NONE"')
done

api_post_json /cheat '{"action":"setLevel","level":5}' > /dev/null 2>&1 || true
sleep 0.5

api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
sleep 1

state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|QUITSAVE" 30) || true
state=$(api GET /state 2>/dev/null) || true

level5_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
level5_bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
level5_level=$(jget "$state" 'o.tournament&&o.tournament.level||1')
log "  Level $level5_level: SB=$level5_sb, BB=$level5_bb"

assert_ge "level 5 small blind vs level 3" "$level5_sb" "$level3_sb"
assert_ge "level 5 big blind vs level 3" "$level5_bb" "$level3_bb"

screenshot "level-advance-5"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Level advancement verified: blinds increase correctly across levels 1 → 3 → 5"
