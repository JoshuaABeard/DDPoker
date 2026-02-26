#!/usr/bin/env bash
# test-hand-rankings.sh — Verify correct hand ranking at showdown.
#
# Injects known cards, all players check/call to showdown, verifies the
# correct player wins with the correct hand type via /hand/result.
#
# Card deal order for 3 players (14 cards total):
#   s0c1, s0c2, s1c1, s1c2, s2c1, s2c2, burn, f1, f2, f3, burn, turn, burn, river
#
# Requires puppet mode and hand result API.
#
# Usage:
#   bash tests/scenarios/test-hand-rankings.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
LAST_HAND_NUM=0

# Define all test hands upfront.
# Cards are injected BEFORE playing the prior hand — the game auto-deals the
# next hand immediately after showdown, so injections must be staged while the
# current hand is still in progress (they sit in the CardInjectionRegistry
# until the next call to startNewHand → takeDeck).

# Hand 1: Pair of Aces beats High Card
# Seat 0: Ah Kh -> pairs Aces with board As
# Seat 1: 3d 7s -> high card
# Seat 2: 2c 4c -> high card
# Board: As 8d 6s Td Qd
HAND1='{"cards":["Ah","Kh","3d","7s","2c","4c","9s","As","8d","6s","Jc","Td","5h","Qd"]}'

# Hand 2: Two Pair (Aces + Kings)
# Seat 0: Ah Kh -> Two Pair with board As Kd
# Seat 1: 8d 4d -> high card
# Seat 2: 2c 5c -> high card
# Board: As Kd 7s 3c 9h
HAND2='{"cards":["Ah","Kh","8d","4d","2c","5c","Ts","As","Kd","7s","Jc","3c","6h","9h"]}'

# Hand 3: Three of a Kind (Aces) beats Two Pair (8s+6s)
# Seat 0: Ah Ad -> Trips with board As
# Seat 1: 8h 6h -> Two Pair with board 8d 6s
# Seat 2: 2c 4c -> nothing
# Board: As 8d 6s 3c 7h
HAND3='{"cards":["Ah","Ad","8h","6h","2c","4c","Ts","As","8d","6s","Jc","3c","Qs","7h"]}'

# Hand 4: Straight (T-9-8-7-6) beats Three of a Kind (Aces)
# Seat 0: Th 9h -> Straight with board 8s 7d 6s
# Seat 1: Ah Ac -> Trips with board Ad
# Seat 2: 3c 4c -> nothing
# Board: 8s 7d 6s Ad 2h
HAND4='{"cards":["Th","9h","Ah","Ac","3c","4c","Ks","8s","7d","6s","Jc","Ad","5c","2h"]}'

# Hand 5: Flush (hearts) beats Straight (Q-J-T-9-8)
# Seat 0: Ah Kh -> Heart Flush (Ah Kh Jh 7h 3h)
# Seat 1: 8d Qd -> Straight Q-J-T-9-8
# Seat 2: 2c 4c -> nothing
# Board: 3h 7h Jh Ts 9s
HAND5='{"cards":["Ah","Kh","8d","Qd","2c","4c","6s","3h","7h","Jh","5c","Ts","As","9s"]}'

# Hand 6: Full House (AAA+99) beats Flush (spades: Ks As 9s 5s 3s)
# Seat 0: Ah Ad -> Full House with board As+99
# Seat 1: Ks Qh -> Spade Flush (Ks As 9s 5s 3s)
# Seat 2: 2c 4c -> nothing
# Board: As 9s 9d 5s 3s
HAND6='{"cards":["Ah","Ad","Ks","Qh","2c","4c","Ts","As","9s","9d","Jc","5s","6h","3s"]}'

# Hand 7: Four of a Kind (Aces) beats Full House (999+AA)
# Seat 0: Ah Ad -> Quads with board As Ac
# Seat 1: 9h Kd -> Full House 999+AA (9h 9s 9d + As Ac)
# Seat 2: 2c 4c -> nothing
# Board: As Ac 9s 9d 3h
HAND7='{"cards":["Ah","Ad","9h","Kd","2c","4c","Ts","As","Ac","9s","Jc","9d","6h","3h"]}'

# Hand 8: Straight Flush (5h-9h) beats Four of a Kind (Aces)
# Seat 0: 9h 8h -> Straight Flush with board 7h 6h 5h
# Seat 1: As Ad -> Quads with board Ac Ah
# Seat 2: 2c 4c -> nothing
# Board: 7h 6h 5h Ac Ah
HAND8='{"cards":["9h","8h","As","Ad","2c","4c","Ts","7h","6h","5h","Jc","Ac","3d","Ah"]}'

# Hand 9: Split pot — identical straights (T-9-8-7-6)
# Seat 0: 6h Kh -> Straight T-9-8-7-6
# Seat 1: 6d Kd -> Straight T-9-8-7-6 (same)
# Seat 2: 2c 4c -> nothing
# Board: Ts 9s 8d 7c 2h
HAND9='{"cards":["6h","Kh","6d","Kd","2c","4c","Qs","Ts","9s","8d","Jh","7c","3s","2h"]}'

lib_start_game 3 '"buyinChips": 10000'

state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

state=$(api GET /state 2>/dev/null) || die "Cannot read state"
P0=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===0)||{}).name||"Player 1"')
P1=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===1)||{}).name||"AI-1"')
P2=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===2)||{}).name||"AI-2"')
log "Players: seat0=$P0 seat1=$P1 seat2=$P2"

# verify_hand: inject next hand's cards, play current hand, verify result.
# Cards are injected BEFORE play_to_showdown so they're staged in the registry
# while the current hand plays out. When the current hand finishes and the game
# auto-deals the next hand, the registry is consumed.
# Usage: verify_hand DESC EXPECTED_WINNER EXPECTED_DESC NEXT_CARDS_JSON
verify_hand() {
    local desc="$1" expected_winner="$2" expected_desc="$3" next_cards="${4:-}"
    log "--- Verifying: $desc ---"

    # Stage next hand's cards NOW (while current hand is in progress)
    if [[ -n "$next_cards" ]]; then
        api_post_json /cards/inject "$next_cards" > /dev/null 2>&1 || true
    fi

    play_to_showdown 30 "$LAST_HAND_NUM" \
        || { record_failure "$desc: did not reach showdown"; return; }

    local result
    result=$(wait_hand_result 10 "$LAST_HAND_NUM") \
        || { record_failure "$desc: no hand result"; return; }

    LAST_HAND_NUM=$(jget "$result" 'o.handNumber||0')
    log "  Hand #$LAST_HAND_NUM completed"

    if [[ "$expected_winner" == "SPLIT" ]]; then
        assert_winner_count "$result" 0 2
    else
        assert_winner "$result" 0 "$expected_winner"
    fi
    assert_hand_description "$result" 0 "$expected_desc"
}

# Inject HAND1 now — it sits in the registry until hand 2's deal.
# Play warmup hand (hand 1 with random cards) to establish state.
log "--- Warmup hand (random cards) ---"
api_post_json /cards/inject "$HAND1" > /dev/null 2>&1 || die "HAND1 injection failed"
play_to_showdown 30 0 || die "Warmup hand failed"
LAST_HAND_NUM=$(jget "$(wait_hand_result 5)" 'o.handNumber||0')
log "  Warmup done (hand #$LAST_HAND_NUM)"

# Play all 9 test hands. Each verify_hand injects the NEXT test's cards before
# playing the current hand. When the current hand finishes, the next hand
# auto-deals with the staged cards.
verify_hand "Pair vs High Card"              "$P0"   "Pair"              "$HAND2"
verify_hand "Two Pair vs High Card"          "$P0"   "Two Pair"          "$HAND3"
verify_hand "Trips vs Two Pair"              "$P0"   "Three of a Kind"   "$HAND4"
verify_hand "Straight vs Trips"              "$P0"   "Straight"          "$HAND5"
verify_hand "Flush vs Straight"              "$P0"   "Flush"             "$HAND6"
verify_hand "Full House vs Flush"            "$P0"   "Full House"        "$HAND7"
verify_hand "Quads vs Full House"            "$P0"   "Four of a Kind"    "$HAND8"
verify_hand "Straight Flush vs Quads"        "$P0"   "Straight Flush"    "$HAND9"
verify_hand "Split — identical straights"    "SPLIT" "Straight"          ""

# Validate chip conservation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid"
else
    log "  OK: Chip conservation valid"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES hand ranking test(s) failed"
fi

pass "All 9 hand ranking tests passed"
