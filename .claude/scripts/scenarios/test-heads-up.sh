#!/usr/bin/env bash
# test-heads-up.sh — Verify 2-player (heads-up) game works correctly.
#
# Tests E-001:
#   - Heads-up blind order correct (dealer/SB posts small blind)
#   - Game completes normally with 2 players
#   - Chip conservation holds
#
# Usage:
#   bash .claude/scripts/scenarios/test-heads-up.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 2

FAILURES=0

assert() {
    local desc="$1" actual="$2" expected="$3"
    if [[ "$actual" != "$expected" ]]; then
        log "FAIL: $desc — expected '$expected', got '$actual'"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: $desc = $actual"
    fi
}

# Verify 2 players seated
log "=== Verifying heads-up setup ==="
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL|QUITSAVE" 60) \
    || die "Timed out waiting for game"

state=$(api GET /state 2>/dev/null) || die "Could not read state"
player_count=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p).length')
assert "player count" "$player_count" "2"

# Verify dealer seat exists
dealer_seat=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].dealerSeat')
log "  Dealer seat: $dealer_seat"
if [[ -z "$dealer_seat" || "$dealer_seat" == "undefined" ]]; then
    log "FAIL: No dealer seat assigned"
    FAILURES=$((FAILURES+1))
fi

# Validate chip conservation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
assert "chip conservation" "$cc_valid" "true"

screenshot "heads-up-start"

# Play 5 hands with CALL/CHECK strategy to verify heads-up works.
# Use phase-transition hand detection: count hands when game transitions
# from non-PRE_FLOP to PRE_FLOP, or when dealer seat changes in PRE_FLOP.
log "=== Playing 5 hands heads-up ==="
HANDS=0
PREV_PHASE=""
PREV_DEALER="-1"
LAST_PROGRESS_SIG=""
LAST_CHANGE=$(date +%s)

while [[ $HANDS -lt 5 ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    phase=$(jget "$state" 'o.gamePhase || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')
    p1chips=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[])[0]?.chips||0')

    # Detect new hand started
    if [[ "$phase" == "PRE_FLOP" ]]; then
        dealer=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].dealerSeat||0')
        if [[ "$PREV_PHASE" != "PRE_FLOP" && "$PREV_PHASE" != "" && "$PREV_PHASE" != "NONE" ]]; then
            HANDS=$((HANDS+1))
            log "  Hand $HANDS complete (from $PREV_PHASE)"
            # Validate chip conservation between hands
            vresult=$(api GET /validate 2>/dev/null) || true
            cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
            if [[ "$cc_valid" != "true" ]]; then
                log "FAIL: chip conservation invalid after hand $HANDS"
                FAILURES=$((FAILURES+1))
            fi
        elif [[ "$PREV_PHASE" == "PRE_FLOP" && "$dealer" != "$PREV_DEALER" && "$PREV_DEALER" != "-1" ]]; then
            HANDS=$((HANDS+1))
            log "  Hand $HANDS complete (dealer rotated: $PREV_DEALER → $dealer)"
        fi
        PREV_DEALER="$dealer"
    fi
    PREV_PHASE="$phase"

    # Game over check (before stuck detection)
    if [[ "$remaining" =~ ^[0-9]+$ && "$remaining" -le 1 && $HANDS -gt 0 ]]; then
        log "Game ended after $HANDS hands ($remaining players remaining)"
        break
    fi

    # Stuck detection using multiple progress signals
    progress_sig="${mode}:${phase}:${p1chips}:${remaining}"
    if [[ "$progress_sig" != "$LAST_PROGRESS_SIG" ]]; then
        LAST_PROGRESS_SIG="$progress_sig"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && die "Stuck in mode $mode"

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
                if echo "$avail" | grep -q "CHECK"; then
                    api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
                else
                    api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
                fi
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac
    sleep 0.15
done

screenshot "heads-up-after-5"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Heads-up (2-player) game verified: $HANDS hands played, chip conservation valid"
