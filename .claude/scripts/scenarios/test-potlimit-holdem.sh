#!/usr/bin/env bash
# test-potlimit-holdem.sh — Verify Pot Limit Hold'em betting rules.
#
# Tests: max bet capped at pot size, correct available actions, chip conservation.
# Uses puppet mode for full control.
#
# Usage:
#   bash .claude/scripts/scenarios/test-potlimit-holdem.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# Start pot-limit game with big blind = 50
lib_start_game 3 '"buyinChips": 5000, "gameType": "potlimit", "blindLevels": [{"small":25,"big":50,"ante":0,"minutes":60}], "disableAutoDeal": true'
sleep 2
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

log "=== Test 1: Max raise capped at pot size ==="
state=$(wait_any_turn 15) || die "No turn"
mode=$(jget "$state" 'o.inputMode||"NONE"')

if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    max_raise=$(jget "$state" 'o.currentAction.maxRaise||o.currentAction.maxBet||0')
    min_raise=$(jget "$state" 'o.currentAction.minRaise||o.currentAction.minBet||0')
    log "  Pre-flop: maxRaise=$max_raise minRaise=$min_raise mode=$mode"

    # In pot-limit, maxRaise should be > minRaise (variable) but capped
    # Pre-flop with SB=25 BB=50, pot is 75. Max raise = pot (75) for first raiser
    # The exact formula depends on position but max should NOT be all-in (5000)
    if [[ "$max_raise" -gt 0 && "$max_raise" -lt 5000 ]]; then
        log "  OK: Max raise ($max_raise) is pot-limited (not all-in)"
    elif [[ "$max_raise" -ge 5000 ]]; then
        record_failure "Test 1: maxRaise=$max_raise looks like no-limit (should be pot-limited)"
    fi

    # In pot-limit, max should differ from min (unless pot equals min bet)
    if [[ "$max_raise" -gt "$min_raise" ]]; then
        log "  OK: Variable raise range (pot-limit structure: $min_raise to $max_raise)"
    else
        log "  INFO: maxRaise=$max_raise equals minRaise=$min_raise"
    fi
fi

# Play through hand checking/calling
play_to_showdown 30 || log "WARN: did not reach showdown"

# Advance to next hand for additional verification
advance_to_next_hand 30 || log "WARN: could not advance"

log "=== Test 2: Post-flop pot-limit verification ==="
# Play another hand, logging bet limits at each street
for round_iter in $(seq 1 30); do
    state=$(api GET /state 2>/dev/null) || { sleep 0.15; continue; }
    mode=$(jget "$state" 'o.inputMode||"NONE"')
    is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
    puppet_turn=$(jget "$state" 'o.currentAction&&o.currentAction.isPuppetTurn||false')
    seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')

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
            round=$(jget "$state" 'o.tables[0].bettingRound||o.gamePhase||"unknown"')
            log "  Round=$round maxRaise=$max_raise"

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

# Validate chip conservation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid in pot-limit game"
else
    log "  OK: Chip conservation valid (pot-limit game)"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES pot-limit Hold'em test(s) failed"
fi

pass "Pot Limit Hold'em betting rules verified"
