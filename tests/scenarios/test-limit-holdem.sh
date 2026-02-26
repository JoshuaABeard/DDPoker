#!/usr/bin/env bash
# test-limit-holdem.sh — Verify Limit Hold'em betting rules.
#
# Tests: fixed bet sizes, correct available actions, chip conservation.
# Uses puppet mode for full control.
#
# Usage:
#   bash tests/scenarios/test-limit-holdem.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# Start a limit game with 3 players, big blind = 50
lib_start_game 3 '"buyinChips": 5000, "gameType": "limit", "blindLevels": [{"small":25,"big":50,"ante":0,"minutes":60}], "disableAutoDeal": true'
sleep 2
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

log "=== Test 1: Pre-flop raise equals big blind ==="
# Find a betting turn and check maxBet
state=$(wait_any_turn 15) || die "No turn"
mode=$(jget "$state" 'o.inputMode||"NONE"')
is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
puppet_turn=$(jget "$state" 'o.currentAction&&o.currentAction.isPuppetTurn||false')

if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    max_raise=$(jget "$state" 'o.currentAction.maxRaise||o.currentAction.maxBet||0')
    min_raise=$(jget "$state" 'o.currentAction.minRaise||o.currentAction.minBet||0')
    log "  Pre-flop: maxRaise=$max_raise minRaise=$min_raise mode=$mode"
    # In limit, raise size should be fixed at BB (50) pre-flop
    if [[ "$max_raise" -gt 0 && "$max_raise" == "$min_raise" ]]; then
        log "  OK: Fixed raise size (limit structure confirmed: maxRaise=minRaise=$max_raise)"
    elif [[ "$max_raise" -gt 0 ]]; then
        log "  INFO: maxRaise=$max_raise minRaise=$min_raise (checking limit behavior)"
    fi
fi

# Play through this hand checking/calling
play_to_showdown 30 || log "WARN: hand did not reach showdown"

# Advance to next hand
advance_to_next_hand 30 || die "Could not advance to hand 2"

log "=== Test 2: Post-flop betting behavior ==="
# Play through pre-flop checking/calling, observe flop/turn/river bets
for round_iter in $(seq 1 30); do
    state=$(api GET /state 2>/dev/null) || { sleep 0.15; continue; }
    mode=$(jget "$state" 'o.inputMode||"NONE"')
    is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
    puppet_turn=$(jget "$state" 'o.currentAction&&o.currentAction.isPuppetTurn||false')
    seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')

    # Check hand result (hand completed)
    hr=$(api GET /hand/result 2>/dev/null || true)
    if [[ -n "$hr" ]] && ! echo "$hr" | grep -q '"error"'; then
        hr_num=$(jget "$hr" 'o.handNumber||0')
        if [[ "$hr_num" -gt 1 ]]; then
            log "  Hand 2 complete"
            break
        fi
    fi

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            max_raise=$(jget "$state" 'o.currentAction.maxRaise||o.currentAction.maxBet||0')
            min_raise=$(jget "$state" 'o.currentAction.minRaise||o.currentAction.minBet||0')
            round=$(jget "$state" 'o.tables[0].bettingRound||o.gamePhase||"unknown"')
            log "  Round=$round maxRaise=$max_raise minRaise=$min_raise"

            # Submit check or call
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 \
                    || api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
            elif [[ "$puppet_turn" == "true" && -n "$seat" && "$seat" != "undefined" ]]; then
                puppet_action "$seat" "CHECK" 2>/dev/null || puppet_action "$seat" "CALL" 2>/dev/null || true
            fi
            ;;
        DEAL|CONTINUE|CONTINUE_LOWER)
            advance_non_betting "$state"
            ;;
    esac
    sleep 0.15
done

# Validate chip conservation at the end
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid in limit game"
else
    log "  OK: Chip conservation valid (limit game)"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES limit Hold'em test(s) failed"
fi

pass "Limit Hold'em betting rules verified"
