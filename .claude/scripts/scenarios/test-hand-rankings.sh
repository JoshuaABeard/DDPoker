#!/usr/bin/env bash
# test-hand-rankings.sh — Verify correct hand ranking at showdown.
#
# Injects known cards, all players check/call to showdown, verifies the
# correct player wins with the correct hand type via /hand/result.
#
# Requires puppet mode and hand result API.
#
# Usage:
#   bash .claude/scripts/scenarios/test-hand-rankings.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
LAST_HAND_NUM=0

# Inject cards for the first hand BEFORE game start
# 1. Pair beats High Card
# Seat 0: Ah Kh (will pair aces with board)
# Seat 1: 3d 7s (nothing)
# Seat 2: 2c 4c (nothing)
# Board: As 8d 6s Td Qd
INJECT='{"cards":["Ah","Kh","3d","7s","2c","4c","9s","As","8d","6s","Jc","Td","5h","Qd"]}'
api_post_json /cards/inject "$INJECT" > /dev/null 2>&1 || die "First card injection failed"

# Start a 3-player game with enough chips to last all hands
lib_start_game 3 '"buyinChips": 10000, "disableAutoDeal": true'

# Wait for game to start and puppet all AI seats
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

# Get player names from state
state=$(api GET /state 2>/dev/null) || die "Cannot read state"
P0=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===0)||{}).name||"Player 1"')
P1=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===1)||{}).name||"AI-1"')
P2=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===2)||{}).name||"AI-2"')
log "Players: seat0=$P0 seat1=$P1 seat2=$P2"

# Helper: play current hand to showdown and verify result.
# Usage: verify_hand DESC EXPECTED_WINNER EXPECTED_HAND_DESC
verify_hand() {
    local desc="$1" expected_winner="$2" expected_desc="$3"
    log "--- Verifying: $desc ---"

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

# Helper: advance to next hand with injected cards.
# Usage: inject_and_advance CARDS_JSON
inject_and_advance() {
    local cards="$1"
    advance_to_next_hand 30 "{\"cards\":[$cards]}" \
        || die "Failed to advance to next hand"
}

# === Hand 1: Pair beats High Card (cards already injected) ===
verify_hand "Pair vs High Card" "$P0" "Pair"

# === Hand 2: Two Pair beats Pair ===
# Seat 0: Ah Kh -> pairs aces and kings with board
# Seat 1: 3d 4d -> pairs only 3s
# Seat 2: 2c 5c -> nothing
# Board: As Kd 3s Td Qd
inject_and_advance '"Ah","Kh","3d","4d","2c","5c","9s","As","Kd","3s","Jc","Td","7h","Qd"'
verify_hand "Two Pair vs Pair" "$P0" "Two Pair"

# === Hand 3: Three of a Kind beats Two Pair ===
# Seat 0: Ah Ad -> trips aces with board
# Seat 1: Kh Kd -> two pair K+Q
# Seat 2: 2c 3c -> nothing
# Board: As Qd 6s Td 8h
inject_and_advance '"Ah","Ad","Kh","Kd","2c","3c","9s","As","Qd","6s","Jc","Td","5h","8h"'
verify_hand "Trips vs Two Pair" "$P0" "Three of a Kind"

# === Hand 4: Straight beats Three of a Kind ===
# Seat 0: Th 9h -> straight T-6
# Seat 1: As Ad -> trips aces
# Seat 2: 2c 3c -> nothing
# Board: 8s 7d 6s Kd Qd
inject_and_advance '"Th","9h","As","Ad","2c","3c","Ks","8s","7d","6s","Ac","Kd","5h","Qd"'
verify_hand "Straight vs Trips" "$P0" "Straight"

# === Hand 5: Flush beats Straight ===
# Seat 0: Ah 9h -> flush (hearts: Ah 9h 3h 7h Jh)
# Seat 1: Td 9d -> straight T-6
# Seat 2: 2c 4c -> nothing
# Board: 3h 7h 8s 5h Jh
inject_and_advance '"Ah","9h","Td","9d","2c","4c","Ks","3h","7h","8s","Qd","5h","6s","Jh"'
verify_hand "Flush vs Straight" "$P0" "Flush"

# === Hand 6: Full House beats Flush ===
# Seat 0: Ah Ad -> full house (AAA+KK)
# Seat 1: 9h 8h -> flush (hearts: 9h 8h 3h 7h Jh) — wait, need hearts on board
# Actually, simpler: give P0 aces, board has an ace and a pair
# Seat 0: Ah Ad -> full house AAA+KK
# Seat 1: Kh 9h -> flush if board has 3 hearts... let me do it differently
# Seat 0: Ah Ad -> full house with board As Kd Ks
# Seat 1: Qh Jh -> no flush, just high cards
# Seat 2: 2c 3c -> nothing
# Board: As Kd Ks Td 8h
inject_and_advance '"Ah","Ad","Qh","Jh","2c","3c","9s","As","Kd","Ks","7d","Td","5h","8h"'
verify_hand "Full House vs Two Pair" "$P0" "Full House"

# === Hand 7: Four of a Kind beats Full House ===
# Seat 0: Ah Ad -> quads with board As Ac
# Seat 1: Kh Kd -> full house KKK+Q with board Ks
# Seat 2: 2c 3c -> nothing
# Board: As Ac Ks Qd Td
inject_and_advance '"Ah","Ad","Kh","Kd","2c","3c","9s","As","Ac","Ks","7d","Qd","5h","Td"'
verify_hand "Quads vs Full House" "$P0" "Four of a Kind"

# === Hand 8: Straight Flush beats Four of a Kind ===
# Seat 0: 9h 8h -> straight flush 5h-9h
# Seat 1: As Ad -> quads with Ac on board
# Seat 2: 2c 3c -> nothing
# Board: 7h 6h 5h Ac Kd
inject_and_advance '"9h","8h","As","Ad","2c","3c","Ks","7h","6h","5h","Td","Ac","Jd","Kd"'
verify_hand "Straight Flush vs Quads" "$P0" "Straight Flush"

# === Hand 9: Split pot — identical hands ===
# Seat 0: Ah Kh -> broadway straight
# Seat 1: Ad Kd -> broadway straight (same)
# Seat 2: 2c 3c -> nothing
# Board: Qs Js Ts 9s 4h
inject_and_advance '"Ah","Kh","Ad","Kd","2c","3c","8s","Qs","Js","Ts","4h","9s","5d","7d"'
verify_hand "Split — identical straights" "SPLIT" "Straight"

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
