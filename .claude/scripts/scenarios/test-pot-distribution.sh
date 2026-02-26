#!/usr/bin/env bash
# test-pot-distribution.sh — Verify correct pot distribution at showdown.
#
# Tests: simple win, equal split, uncontested pot.
# Uses puppet mode for full control and /hand/result for verification.
#
# Usage:
#   bash .claude/scripts/scenarios/test-pot-distribution.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
LAST_HAND_NUM=0

# Inject cards for the first hand BEFORE game start:
# Seat 0: Ah Ad (pair of aces — winner)
# Seat 1: 3c 7s (nothing)
# Seat 2: 2c 4d (nothing)
# Board: 9h 8d 6s Td Qd
INJECT='{"cards":["Ah","Ad","3c","7s","2c","4d","Ks","9h","8d","6s","Jc","Td","5h","Qd"]}'
api_post_json /cards/inject "$INJECT" > /dev/null 2>&1 || die "Card injection failed"

lib_start_game 3 '"buyinChips": 1000, "disableAutoDeal": true'
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

state=$(api GET /state 2>/dev/null) || die "Cannot read state"
P0=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===0)||{}).name')
P1=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===1)||{}).name')
P2=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===2)||{}).name')
log "Players: seat0=$P0 seat1=$P1 seat2=$P2"

# === Test 1: Simple pot — one winner gets all ===
log "--- Test 1: Simple pot ---"
play_to_showdown 30 || record_failure "Test 1: no showdown"
result=$(wait_hand_result 10) || record_failure "Test 1: no result"
if [[ -n "$result" ]]; then
    LAST_HAND_NUM=$(jget "$result" 'o.handNumber||0')
    assert_winner "$result" 0 "$P0"
    assert_winner_count "$result" 0 1
    # Check the pot has a positive chip count
    pot_chips=$(jget "$result" '(o.pots[0]||{}).chipCount||0')
    if [[ "$pot_chips" -gt 0 ]]; then
        log "  OK: Pot has $pot_chips chips"
    else
        record_failure "Test 1: pot chipCount should be > 0, got $pot_chips"
    fi
fi

# === Test 2: Equal split — two identical hands ===
log "--- Test 2: Equal split ---"
# Seat 0: Ah Kh -> broadway straight
# Seat 1: Ad Kd -> broadway straight (same)
# Seat 2: 2c 3c -> nothing
# Board: Qs Js Ts 9s 4h
advance_to_next_hand 30 '{"cards":["Ah","Kh","Ad","Kd","2c","3c","8s","Qs","Js","Ts","4h","9s","5d","7d"]}' \
    || record_failure "Test 2: could not advance"

play_to_showdown 30 "$LAST_HAND_NUM" || record_failure "Test 2: no showdown"
result=$(wait_hand_result 10 "$LAST_HAND_NUM") || record_failure "Test 2: no result"
if [[ -n "$result" ]]; then
    LAST_HAND_NUM=$(jget "$result" 'o.handNumber||0')
    assert_winner_count "$result" 0 2
    # Both winners should have the same win amount
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
advance_to_next_hand 30 || record_failure "Test 3: could not advance"

# Wait for first player's turn, then have them raise; others fold
state=$(wait_any_turn 15) || { record_failure "Test 3: no turn"; }
if [[ -n "$state" ]]; then
    is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
    seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')

    # First player raises
    if [[ "$is_human" == "true" ]]; then
        api_post_json /action '{"type":"RAISE","amount":200}' > /dev/null 2>&1 || true
    elif [[ -n "$seat" && "$seat" != "undefined" ]]; then
        puppet_action "$seat" "RAISE" 200 || true
    fi
    sleep 0.3

    # Remaining players fold
    for i in $(seq 1 5); do
        state=$(wait_any_turn 5) || break
        # Check if hand result is already available (everyone folded)
        hr=$(api GET /hand/result 2>/dev/null || true)
        if [[ -n "$hr" ]]; then
            hr_num=$(jget "$hr" 'o.handNumber||0')
            if [[ "$hr_num" -gt "$LAST_HAND_NUM" ]]; then
                break
            fi
        fi
        seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')
        is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
        if [[ "$is_human" == "true" ]]; then
            api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
        elif [[ -n "$seat" && "$seat" != "undefined" ]]; then
            puppet_action "$seat" "FOLD" || true
        fi
        sleep 0.2
    done

    sleep 0.5
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
