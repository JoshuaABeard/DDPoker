#!/usr/bin/env bash
# test-pot-distribution.sh — Verify correct pot distribution at showdown.
#
# Tests: simple win, equal split, uncontested pot.
# Uses puppet mode for full control and /hand/result for verification.
#
# Usage:
#   bash tests/scenarios/test-pot-distribution.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
LAST_HAND_NUM=0

# Test 1 cards: Simple win — seat 0 (Computer 1) wins with pair of aces
# Seat 0: Ah Ad, Seat 1: 3c 5s, Seat 2: 2c 4s
# Board: 9h 8c Kd Td Qh
# Seat 0: pair of aces (AA, Q-K-T kickers)
# Seat 1: high card (Q-K-T-9-8)
# Seat 2: high card (Q-K-T-9-8)
T1='{"cards":["Ah","Ad","3c","5s","2c","4s","Js","9h","8c","Kd","6d","Td","7h","Qh"]}'

# Test 2 cards: Equal split — both seats 0 and 1 make identical broadway straights
# Seat 0: Ah Kh, Seat 1: Ad Kd, Seat 2: 2c 3c
# Board: Qc Jc Ts 4h 9h
# Seat 0: A-K-Q-J-T straight; Seat 1: same straight; Seat 2: nothing
T2='{"cards":["Ah","Kh","Ad","Kd","2c","3c","8s","Qc","Jc","Ts","5d","4h","6s","9h"]}'

lib_start_game 3 '"buyinChips": 1000'
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

state=$(api GET /state 2>/dev/null) || die "Cannot read state"
P0=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===0)||{}).name')
P1=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===1)||{}).name')
P2=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===2)||{}).name')
log "Players: seat0=$P0 seat1=$P1 seat2=$P2"

# Inject T1 for hand 2 (while hand 1 is dealt and in progress).
# Play warmup hand (hand 1 with random cards) to establish state.
log "--- Warmup hand ---"
api_post_json /cards/inject "$T1" > /dev/null 2>&1 || die "T1 injection failed"
play_to_showdown 30 0 || die "Warmup failed"
LAST_HAND_NUM=$(jget "$(wait_hand_result 5)" 'o.handNumber||0')

# === Test 1: Simple pot — one winner gets all ===
log "--- Test 1: Simple pot ---"
# Inject T2 for hand 3 (while hand 2 plays with T1 cards)
api_post_json /cards/inject "$T2" > /dev/null 2>&1 || true
play_to_showdown 30 "$LAST_HAND_NUM" || record_failure "Test 1: no showdown"
result=$(wait_hand_result 10 "$LAST_HAND_NUM") || record_failure "Test 1: no result"
if [[ -n "$result" ]]; then
    LAST_HAND_NUM=$(jget "$result" 'o.handNumber||0')
    assert_winner "$result" 0 "$P0"
    assert_winner_count "$result" 0 1
    pot_chips=$(jget "$result" '(o.pots[0]||{}).chipCount||0')
    if [[ "$pot_chips" -gt 0 ]]; then
        log "  OK: Pot has $pot_chips chips"
    else
        record_failure "Test 1: pot chipCount should be > 0, got $pot_chips"
    fi
fi

# === Test 2: Equal split — two identical hands ===
log "--- Test 2: Equal split ---"
# No injection needed for test 3 (uncontested uses no specific cards)
play_to_showdown 30 "$LAST_HAND_NUM" || record_failure "Test 2: no showdown"
result=$(wait_hand_result 10 "$LAST_HAND_NUM") || record_failure "Test 2: no result"
if [[ -n "$result" ]]; then
    LAST_HAND_NUM=$(jget "$result" 'o.handNumber||0')
    assert_winner_count "$result" 0 2
    w0=$(jget "$result" '(o.pots[0].winners[0]||{}).totalWin||0')
    w1=$(jget "$result" '(o.pots[0].winners[1]||{}).totalWin||0')
    if [[ "$w0" != "$w1" ]]; then
        record_failure "Test 2: split not equal: $w0 vs $w1"
    else
        log "  OK: Equal split of $w0 chips each"
    fi
fi

# === Test 3: Uncontested — all fold ===
log "--- Test 3: Uncontested ---"
# One player raises, all others fold. Result should be isUncontested=true.
raised=false
for i in $(seq 1 15); do
    state=$(wait_any_turn 10) || break
    mode=$(jget "$state" 'o.inputMode||"NONE"')

    # Check if this hand already completed
    hr=$(api GET /hand/result 2>/dev/null || true)
    if [[ -n "$hr" ]] && ! echo "$hr" | grep -q '"error"'; then
        hr_num=$(jget "$hr" 'o.handNumber||0')
        if [[ "$hr_num" -gt "$LAST_HAND_NUM" ]]; then
            log "  T3 iter $i: hand result found (hand #$hr_num)"
            break
        fi
    fi

    # Handle non-betting modes first
    case "$mode" in
        DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
            log "  T3 iter $i: advancing past $mode"
            advance_non_betting "$state"
            sleep 0.2
            continue
            ;;
    esac

    is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
    puppet_turn=$(jget "$state" 'o.currentAction&&o.currentAction.isPuppetTurn||false')
    seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')

    if [[ "$raised" != "true" ]]; then
        # First betting turn: raise
        if [[ "$is_human" == "true" ]]; then
            log "  T3 iter $i: human RAISE seat=$seat mode=$mode"
            resp=$(api_post_json /action '{"type":"RAISE","amount":200}' 2>&1 || true)
            log "  T3 raise response: $resp"
        elif [[ "$puppet_turn" == "true" && -n "$seat" && "$seat" != "undefined" ]]; then
            log "  T3 iter $i: puppet RAISE seat=$seat mode=$mode"
            puppet_action "$seat" "RAISE" 200 || true
        else
            log "  T3 iter $i: SKIPPED (no human/puppet) mode=$mode seat=$seat human=$is_human puppet=$puppet_turn"
            continue
        fi
        raised=true
    else
        # Subsequent turns: fold
        if [[ "$is_human" == "true" ]]; then
            log "  T3 iter $i: human FOLD seat=$seat mode=$mode"
            api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
        elif [[ "$puppet_turn" == "true" && -n "$seat" && "$seat" != "undefined" ]]; then
            log "  T3 iter $i: puppet FOLD seat=$seat mode=$mode"
            puppet_action "$seat" "FOLD" || true
        else
            log "  T3 iter $i: SKIPPED (no human/puppet for fold) mode=$mode"
            continue
        fi
    fi
    sleep 0.2
done

result=$(wait_hand_result 10 "$LAST_HAND_NUM") || record_failure "Test 3: no result"
if [[ -n "$result" ]]; then
    LAST_HAND_NUM=$(jget "$result" 'o.handNumber||0')
    uncontested=$(jget "$result" 'o.isUncontested||false')
    if [[ "$uncontested" != "true" ]]; then
        record_failure "Test 3: expected isUncontested=true, got $uncontested"
    else
        log "  OK: Hand is uncontested"
    fi
fi

# Validate chip conservation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid"
else
    log "  OK: Chip conservation valid"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES pot distribution test(s) failed"
fi

pass "Pot distribution tests passed (3/3)"
