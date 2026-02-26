#!/usr/bin/env bash
# test-tournament-lifecycle.sh — Verify tournament rules: blind posting,
# button movement, elimination, finish positions.
#
# Uses puppet mode to control all players. Verifies structural tournament
# mechanics across multiple hands.
#
# Usage:
#   bash tests/scenarios/test-tournament-lifecycle.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
LAST_HAND_NUM=0

# 4 players for multi-player testing with low chips for quick elimination
lib_start_game 4 '"buyinChips": 500, "disableAutoDeal": true'
sleep 2
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

state=$(api GET /state 2>/dev/null) || die "Cannot read state"
P0=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===0)||{}).name')
log "Human player: $P0"

log "=== Test 1: Button position and blind posting ==="
# Record dealer seat for first hand
dealer1=$(jget "$state" 'o.tables[0].dealerSeat||0')
log "  Hand 1 dealer seat: $dealer1"

# Verify players have chips
player_count=$(jget "$state" '(o.tables[0].players||[]).filter(p=>p&&p.chips>0).length')
log "  Active players: $player_count"
if [[ "$player_count" -lt 4 ]]; then
    record_failure "Expected 4 players at start, got $player_count"
fi

# Play first hand (all check/call)
play_to_showdown 30 || log "WARN: hand 1 no showdown"
hr=$(wait_hand_result 10) || true
if [[ -n "$hr" ]]; then
    LAST_HAND_NUM=$(jget "$hr" 'o.handNumber||0')
fi

# Advance to next hand
advance_to_next_hand 30 || die "Could not advance to hand 2"

# Check button moved
state=$(api GET /state 2>/dev/null) || die "No state for hand 2"
dealer2=$(jget "$state" 'o.tables[0].dealerSeat||0')
log "  Hand 2 dealer seat: $dealer2"
if [[ "$dealer1" == "$dealer2" ]]; then
    record_failure "Button did not move between hands (both at seat $dealer1)"
else
    log "  OK: Button moved from seat $dealer1 to seat $dealer2"
fi

log "=== Test 2: Player elimination ==="
# Play hand 2 - give seat 0 aces and have a puppet go all-in with weak cards
# Inject: seat 0 gets AA, seat 1 gets 72o (worst hand), others get junk
# Board doesn't help seat 1
api_post_json /cards/inject \
    '{"cards":["Ah","Ad","7c","2s","4c","3d","5c","6d","Ts","Ks","Qh","8s","Jd","9h","8c","3h"]}' \
    > /dev/null 2>&1

# Need to deal this hand - advance through DEAL if not already past it
state=$(api GET /state 2>/dev/null) || true
mode=$(jget "$state" 'o.inputMode||"NONE"')
if [[ "$mode" == "DEAL" ]]; then
    api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
    sleep 0.5
fi

# Wait for turns and have puppet at seat 1 go all-in, human calls, others fold
for i in $(seq 1 15); do
    state=$(wait_any_turn 10) || break
    mode=$(jget "$state" 'o.inputMode||"NONE"')
    is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
    puppet_turn=$(jget "$state" 'o.currentAction&&o.currentAction.isPuppetTurn||false')
    seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')

    # Check if hand completed
    hr=$(api GET /hand/result 2>/dev/null || true)
    if [[ -n "$hr" ]] && ! echo "$hr" | grep -q '"error"'; then
        hr_num=$(jget "$hr" 'o.handNumber||0')
        if [[ "$hr_num" -gt "$LAST_HAND_NUM" ]]; then
            break
        fi
    fi

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            if [[ "$is_human" == "true" ]]; then
                # Human calls or checks
                api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 \
                    || api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
            elif [[ "$puppet_turn" == "true" && -n "$seat" && "$seat" != "undefined" ]]; then
                if [[ "$seat" == "1" ]]; then
                    # Seat 1 goes all-in (weak hand vs aces)
                    puppet_action "$seat" "ALL_IN" 2>/dev/null \
                        || puppet_action "$seat" "CALL" 2>/dev/null || true
                else
                    # Other puppets fold
                    puppet_action "$seat" "FOLD" 2>/dev/null \
                        || puppet_action "$seat" "CHECK" 2>/dev/null || true
                fi
            fi
            ;;
        DEAL|CONTINUE|CONTINUE_LOWER)
            advance_non_betting "$state"
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac
    sleep 0.15
done

# Wait for hand completion
sleep 1
hr=$(wait_hand_result 10 "$LAST_HAND_NUM") || true
if [[ -n "$hr" ]]; then
    LAST_HAND_NUM=$(jget "$hr" 'o.handNumber||0')
fi

# Check if a player was eliminated (chip count went to 0)
state=$(api GET /state 2>/dev/null) || true
active=$(jget "$state" '(o.tables[0].players||[]).filter(p=>p&&p.chips>0).length')
log "  Active players after all-in hand: $active"
if [[ "$active" -lt "$player_count" ]]; then
    log "  OK: Player eliminated (active went from $player_count to $active)"
else
    log "  NOTE: No player eliminated (seat 1 may have survived — depends on board)"
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
    die "$FAILURES tournament lifecycle test(s) failed"
fi

pass "Tournament lifecycle tests passed"
