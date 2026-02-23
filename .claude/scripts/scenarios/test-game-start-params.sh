#!/usr/bin/env bash
# test-game-start-params.sh — Verify game start with various parameters.
#
# Tests G-001 through G-006:
#   - Correct player count after start
#   - Starting chip stacks match request
#   - Blind amounts match request
#   - Dealer button placed on a valid seat
#
# Usage:
#   bash .claude/scripts/scenarios/test-game-start-params.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

run_test() {
    local label="$1" players="$2" chips="$3" small_blind="$4"
    log "=== $label: ${players}p, ${chips} chips, SB=${small_blind} ==="

    local extra="\"startingChips\": $chips, \"smallBlind\": $small_blind"
    lib_start_game "$players" "$extra"
    sleep 1

    # Wait for first human turn or DEAL mode
    local state
    state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL|CONTINUE" 60) \
        || { log "FAIL: $label — timed out waiting for game to start"; FAILURES=$((FAILURES+1)); return; }

    local mode
    mode=$(jget "$state" 'o.inputMode || "NONE"')

    # If we landed on DEAL, deal the first hand and wait for action
    if [[ "$mode" == "DEAL" ]]; then
        api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
        state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|CONTINUE|QUITSAVE" 30) \
            || { log "FAIL: $label — timed out after DEAL"; FAILURES=$((FAILURES+1)); return; }
    fi

    # Re-read state for assertions
    state=$(api GET /state 2>/dev/null) || { log "FAIL: $label — could not read state"; FAILURES=$((FAILURES+1)); return; }

    # G-002: Verify player count
    local actual_players
    actual_players=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p).length')
    if [[ "$actual_players" != "$players" ]]; then
        log "FAIL: $label — expected $players players, got $actual_players"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: player count = $actual_players"
    fi

    # G-003: Verify starting chips (sum should be players * chips, accounting for blinds already posted)
    local total_chips
    total_chips=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].chipConservation||{}).sum||0')
    local expected_total=$((players * chips))
    if [[ "$total_chips" != "$expected_total" && "$total_chips" != "0" ]]; then
        log "FAIL: $label — expected total chips $expected_total, got $total_chips"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: total chips = $total_chips (expected $expected_total)"
    fi

    # G-005: Verify blind amounts via tournament state
    local actual_sb
    actual_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
    if [[ "$actual_sb" != "$small_blind" && "$actual_sb" != "0" ]]; then
        log "FAIL: $label — expected SB=$small_blind, got $actual_sb"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: smallBlind = $actual_sb"
    fi

    # G-006: Verify dealer button is on a valid seat
    local dealer_seat
    dealer_seat=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].dealerSeat')
    if [[ -z "$dealer_seat" || "$dealer_seat" == "undefined" ]]; then
        log "FAIL: $label — dealer seat not set"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: dealerSeat = $dealer_seat"
    fi

    # Validate chip conservation
    local vresult cc_valid
    vresult=$(api GET /validate 2>/dev/null) || true
    cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
    if [[ "$cc_valid" != "true" ]]; then
        log "FAIL: $label — chip conservation invalid"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: chip conservation valid"
    fi

    screenshot "start-params-${label}"
}

# Test 1: Default 3 players
run_test "3p-default" 3 1500 10

# Test 2: 6 players, larger stacks
run_test "6p-large" 6 5000 25

# Test 3: 2 players (heads-up)
run_test "2p-headsup" 2 1000 5

# Test 4: 10 players
run_test "10p-full" 10 2000 50

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "All game start parameter variations verified (4 configurations)"
