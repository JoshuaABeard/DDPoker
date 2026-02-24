#!/usr/bin/env bash
# test-all-actions.sh — Exercise every player action type and verify acceptance.
#
# Tests G-020 through G-028:
#   - FOLD, CHECK, CALL, BET, RAISE, ALL_IN
#   - Verifies each action is accepted and game advances
#   - Validates chip conservation after each hand
#
# Strategy: Start multiple hands, use each action type once, verify the
# game state transitions correctly.
#
# Usage:
#   bash .claude/scripts/scenarios/test-all-actions.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
# 3 players with large stacks so no one gets eliminated during the test.
lib_start_game 3 '"buyinChips": 50000'

FAILURES=0
ACTIONS_TESTED=0

assert_action() {
    local action_type="$1" amount_json="${2:-}"
    local body="{\"type\":\"$action_type\"${amount_json:+, $amount_json}}"
    local resp
    resp=$(api_post_json /action "$body" 2>/dev/null) || { log "FAIL: $action_type — request failed"; FAILURES=$((FAILURES+1)); return 1; }
    if echo "$resp" | grep -q '"accepted":true'; then
        log "  OK: $action_type accepted"
        ACTIONS_TESTED=$((ACTIONS_TESTED+1))
        return 0
    else
        log "FAIL: $action_type rejected — $resp"
        FAILURES=$((FAILURES+1))
        return 1
    fi
}

require_finish_hand() {
    local label="$1"
    if ! finish_hand; then
        log "FAIL: hand did not finish cleanly after $label"
        FAILURES=$((FAILURES+1))
    fi
}

# Helper: advance through non-human-turn states until human betting turn
advance_to_human_turn() {
    local timeout="${1:-60}"
    local start_time=$(date +%s)
    while true; do
        local st md
        st=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
        md=$(jget "$st" 'o.inputMode || "NONE"')

        case "$md" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                local is_human
                is_human=$(jget "$st" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                if [[ "$is_human" == "true" ]]; then
                    echo "$st"
                    return 0
                fi
                ;;
            DEAL)
                # Between-hand DEAL mode (some configurations); advance it
                api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
                ;;
            CONTINUE|CONTINUE_LOWER)
                api_post_json /action "{\"type\":\"$md\"}" > /dev/null 2>&1 || true
                ;;
            REBUY_CHECK)
                api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
                ;;
            QUITSAVE|NONE)
                # AI acting or between states — just wait
                ;;
        esac


        local elapsed=$(( $(date +%s) - start_time ))
        [[ $elapsed -gt $timeout ]] && { log "  INFO: timed out waiting for human turn"; return 1; }
        sleep 0.2
    done
}

# Helper: play through rest of hand after our action (AI + continues)
# The embedded server auto-deals the next hand without showing DEAL mode,
# so we detect "hand over" when the game advances to a new hand (cc drops
# back to 0 from a non-zero value, or QUITSAVE persists 3s after cc>=5).
finish_hand() {
    local timeout="${1:-60}"
    local start_time=$(date +%s)
    local max_cc=0 river_done_time=0
    while true; do
        local st md
        st=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
        md=$(jget "$st" 'o.inputMode || "NONE"')

        local cc
        cc=$(jget "$st" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).length')
        [[ "$cc" =~ ^[0-9]+$ ]] || cc=0

        # Detect hand completion:
        # 1) cc drops back to 0 after being non-zero (new hand started)
        # 2) River (cc=5) seen and 3s have passed
        if [[ $max_cc -ge 5 && $river_done_time -eq 0 ]]; then
            river_done_time=$(date +%s)
        fi
        if [[ $river_done_time -gt 0 && $(( $(date +%s) - river_done_time )) -ge 3 ]]; then
            return 0
        fi
        if [[ $max_cc -gt 0 && $cc -eq 0 ]]; then
            return 0  # cc reset — new hand dealing
        fi
        [[ $cc -gt $max_cc ]] && max_cc=$cc

        case "$md" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                local is_human
                is_human=$(jget "$st" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                if [[ "$is_human" == "true" ]]; then
                    # Just check/call to keep hand going
                    local avail
                    avail=$(jget "$st" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
                    if echo "$avail" | grep -q "CHECK"; then
                        api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
                    else
                        api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
                    fi
                fi
                ;;
            CONTINUE|CONTINUE_LOWER)
                api_post_json /action "{\"type\":\"$md\"}" > /dev/null 2>&1 || true
                ;;
            DEAL)
                return 0  # Hand is over (between-hand DEAL mode)
                ;;
            REBUY_CHECK)
                api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
                ;;
        esac

        local elapsed=$(( $(date +%s) - start_time ))
        [[ $elapsed -gt $timeout ]] && { log "  INFO: timed out finishing hand"; return 1; }
        sleep 0.2
    done
}

validate_hand() {
    local label="$1"
    local vresult cc_valid
    vresult=$(api GET /validate 2>/dev/null) || true
    cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
    if [[ "$cc_valid" != "true" ]]; then
        log "FAIL: chip conservation invalid after $label"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: chip conservation valid after $label"
    fi
}

# ============================================================
# Test 1: FOLD (G-020)
# ============================================================
log "=== Test: FOLD ==="
state=$(advance_to_human_turn) || die "Could not reach human turn for FOLD test"
mode=$(jget "$state" 'o.inputMode || "NONE"')
log "  INFO: input mode: $mode"
assert_action "FOLD"
require_finish_hand "FOLD"
validate_hand "FOLD"

# ============================================================
# Test 2: CHECK (G-021)
# ============================================================
log "=== Test: CHECK ==="
# CHECK requires CHECK_BET or CHECK_RAISE mode.
# In a 3-player game this occurs when the human is BB (no preflop raise)
# or when the human acts first post-flop with no bet.
# Strategy: fold when CHECK isn't available; the next hand will cycle the
# dealer button so the human's position changes.
MAX_TRIES=20
CHECK_DONE=false
for i in $(seq 1 $MAX_TRIES); do
    state=$(advance_to_human_turn) || die "Could not reach human turn for CHECK test"
    avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
    if echo "$avail" | grep -q "CHECK"; then
        assert_action "CHECK"
        require_finish_hand "CHECK"
        validate_hand "CHECK"
        CHECK_DONE=true
        break
    else
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    fi
done
[[ "$CHECK_DONE" == "true" ]] || { log "FAIL: Never got CHECK opportunity in $MAX_TRIES hands"; FAILURES=$((FAILURES+1)); }

# ============================================================
# Test 3: CALL (G-022)
# ============================================================
log "=== Test: CALL ==="
CALL_DONE=false
for i in $(seq 1 $MAX_TRIES); do
    state=$(advance_to_human_turn) || die "Could not reach human turn for CALL test"
    avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
    if echo "$avail" | grep -q "CALL"; then
        assert_action "CALL"
        require_finish_hand "CALL"
        validate_hand "CALL"
        CALL_DONE=true
        break
    else
        api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || \
            api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    fi
done
[[ "$CALL_DONE" == "true" ]] || { log "FAIL: Never got CALL opportunity in $MAX_TRIES hands"; FAILURES=$((FAILURES+1)); }

# ============================================================
# Test 4: BET (G-023)
# ============================================================
log "=== Test: BET ==="
BET_DONE=false
for i in $(seq 1 $MAX_TRIES); do
    state=$(advance_to_human_turn) || die "Could not reach human turn for BET test"
    avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
    if echo "$avail" | grep -q "BET"; then
        min_bet=$(jget "$state" 'o.currentAction&&o.currentAction.minBet||20')
        assert_action "BET" "\"amount\": $min_bet"
        require_finish_hand "BET"
        validate_hand "BET"
        BET_DONE=true
        break
    else
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    fi
done
[[ "$BET_DONE" == "true" ]] || { log "FAIL: Never got BET opportunity in $MAX_TRIES hands"; FAILURES=$((FAILURES+1)); }

# ============================================================
# Test 5: RAISE (G-024)
# ============================================================
log "=== Test: RAISE ==="
RAISE_DONE=false
for i in $(seq 1 $MAX_TRIES); do
    state=$(advance_to_human_turn) || die "Could not reach human turn for RAISE test"
    avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
    if echo "$avail" | grep -q "RAISE"; then
        min_raise=$(jget "$state" 'o.currentAction&&o.currentAction.minRaise||40')
        assert_action "RAISE" "\"amount\": $min_raise"
        require_finish_hand "RAISE"
        validate_hand "RAISE"
        RAISE_DONE=true
        break
    else
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    fi
done
[[ "$RAISE_DONE" == "true" ]] || { log "FAIL: Never got RAISE opportunity in $MAX_TRIES hands"; FAILURES=$((FAILURES+1)); }

# ============================================================
# Test 6: ALL_IN (G-025)
# ============================================================
log "=== Test: ALL_IN ==="
ALL_IN_DONE=false
for i in $(seq 1 $MAX_TRIES); do
    state=$(advance_to_human_turn 45) || {
        log "  INFO: timed out waiting for ALL_IN opportunity ($i/$MAX_TRIES); restarting game"
        lib_start_game 3 '"buyinChips": 50000'
        continue
    }
    avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
    if echo "$avail" | grep -q "ALL_IN"; then
        assert_action "ALL_IN"
        require_finish_hand "ALL_IN"
        validate_hand "ALL_IN"
        ALL_IN_DONE=true
        break
    fi

    # Keep progressing hands until ALL_IN becomes available.
    if echo "$avail" | grep -q "FOLD"; then
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    elif echo "$avail" | grep -q "CHECK"; then
        api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
    else
        api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
    fi
done
[[ "$ALL_IN_DONE" == "true" ]] || { log "FAIL: Never got ALL_IN opportunity in $MAX_TRIES hands"; FAILURES=$((FAILURES+1)); }

screenshot "all-actions-complete"

log "---"
log "  INFO: actions tested: $ACTIONS_TESTED"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "All 6 action types verified: FOLD, CHECK, CALL, BET, RAISE, ALL_IN"
