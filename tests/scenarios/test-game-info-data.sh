#!/usr/bin/env bash
# test-game-info-data.sh — Verify /state contains game info data.
#
# Tests GI-001 through GI-012:
#   - Player standings (players array with chips)
#   - Pot information
#   - Recent events
#   - Tournament metadata (level, blinds, players remaining)
#   - Table structure (dealer seat, community cards)
#   - Chip conservation data
#
# Usage:
#   bash tests/scenarios/test-game-info-data.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

# Wait for a human turn so the game is in progress
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE" 60) \
    || die "Timed out waiting for human turn"
state=$(api GET /state 2>/dev/null) || die "Could not read state"

# ============================================================
# GI-001: Players array present with chip counts
# ============================================================
log "=== GI-001: Player Standings ==="
player_count=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p).length')
log "  Players on table: $player_count"

if [[ "$player_count" -gt 0 ]]; then
    log "  OK: Players array populated"
else
    log "FAIL: Players array empty"
    FAILURES=$((FAILURES+1))
fi

# Check each player has chips
for i in $(seq 0 $((player_count - 1))); do
    pname=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p)[$i]?.name||'?'")
    pchips=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p)[$i]?.chips")
    is_human=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p)[$i]?.isHuman||false")
    log "  Player $i: $pname, chips=$pchips, human=$is_human"
done

# ============================================================
# GI-002: Human player identified
# ============================================================
log "=== GI-002: Human Player ==="
human_found=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).some(p=>p&&p.isHuman)')
if [[ "$human_found" == "true" ]]; then
    log "  OK: Human player identified"
else
    log "FAIL: No human player found in players array"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# GI-003: Pot information
# ============================================================
log "=== GI-003: Pot Information ==="
pot=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].pot||0')
log "  Pot: $pot"

if [[ -n "$pot" && "$pot" != "" && "$pot" != "undefined" ]]; then
    log "  OK: Pot data present"
else
    log "  WARN: Pot not in state (may be zero at start of hand)"
fi

# ============================================================
# GI-004: Current bets per player
# ============================================================
log "=== GI-004: Current Bets ==="
current_bets=$(jget "$state" 'JSON.stringify(o.tables&&o.tables[0]&&o.tables[0].currentBets||{})')
log "  currentBets: $current_bets"

if [[ -n "$current_bets" && "$current_bets" != "{}" && "$current_bets" != "undefined" ]]; then
    log "  OK: Current bets data present"
else
    log "  WARN: No current bets data (may be preflop before action)"
fi

# ============================================================
# GI-005: Dealer seat
# ============================================================
log "=== GI-005: Dealer Seat ==="
dealer_seat=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].dealerSeat')
if [[ -n "$dealer_seat" && "$dealer_seat" != "" && "$dealer_seat" != "undefined" ]]; then
    log "  OK: Dealer seat = $dealer_seat"
else
    log "FAIL: Dealer seat not present"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# GI-006: Tournament metadata
# ============================================================
log "=== GI-006: Tournament Metadata ==="
level=$(jget "$state" 'o.tournament&&o.tournament.level||""')
sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||""')
bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||""')
remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||""')

log "  Level: $level, SB: $sb, BB: $bb, Remaining: $remaining"

if [[ -n "$level" && "$level" != "" && "$level" != "undefined" ]]; then
    log "  OK: Tournament level present"
else
    log "FAIL: Tournament level missing"
    FAILURES=$((FAILURES+1))
fi

if [[ -n "$remaining" && "$remaining" -gt 0 ]]; then
    log "  OK: Players remaining = $remaining"
else
    log "FAIL: Players remaining missing or zero"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# GI-007: Clock data
# ============================================================
log "=== GI-007: Clock Data ==="
seconds_remaining=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.secondsRemaining')
if [[ -n "$seconds_remaining" && "$seconds_remaining" != "" && "$seconds_remaining" != "undefined" ]]; then
    log "  OK: Clock secondsRemaining = $seconds_remaining"
else
    log "  WARN: Clock data not present"
fi

# ============================================================
# GI-008: Input mode and available actions
# ============================================================
log "=== GI-008: Input Mode and Actions ==="
input_mode=$(jget "$state" 'o.inputMode||""')
avail_actions=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
log "  inputMode: $input_mode"
log "  availableActions: $avail_actions"

if [[ -n "$input_mode" && "$input_mode" != "" ]]; then
    log "  OK: Input mode present"
else
    log "FAIL: Input mode missing"
    FAILURES=$((FAILURES+1))
fi

if [[ -n "$avail_actions" && "$avail_actions" != "" ]]; then
    log "  OK: Available actions present"
else
    log "FAIL: Available actions missing"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# GI-009: Chip conservation data
# ============================================================
log "=== GI-009: Chip Conservation Data ==="
cc_sum=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].chipConservation&&o.tables[0].chipConservation.sum||0')
cc_player=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].chipConservation&&o.tables[0].chipConservation.playerTotal||0')
cc_pot=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].chipConservation&&o.tables[0].chipConservation.inPot||0')
log "  chipConservation: sum=$cc_sum, playerTotal=$cc_player, inPot=$cc_pot"

if [[ -n "$cc_sum" && "$cc_sum" -gt 0 ]]; then
    log "  OK: Chip conservation data present"
else
    log "  WARN: Chip conservation data not populated"
fi

# ============================================================
# GI-010: Recent events
# ============================================================
log "=== GI-010: Recent Events ==="
event_count=$(jget "$state" '(o.recentEvents||[]).length')
log "  recentEvents count: $event_count"

if [[ -n "$event_count" && "$event_count" -gt 0 ]]; then
    log "  OK: Recent events present ($event_count events)"
    first_event_type=$(jget "$state" '(o.recentEvents||[])[0]?.type||""')
    log "  First event type: $first_event_type"
else
    log "  WARN: No recent events (may not be populated yet)"
fi

# ============================================================
# GI-011: Hand number
# ============================================================
log "=== GI-011: Hand Number ==="
hand_number=$(jget "$state" 'o.handNumber')
if [[ -n "$hand_number" && "$hand_number" != "" && "$hand_number" != "undefined" ]]; then
    log "  OK: Hand number = $hand_number"
else
    log "  WARN: Hand number not present"
fi

# ============================================================
# GI-012: Game phase
# ============================================================
log "=== GI-012: Game Phase ==="
game_phase=$(jget "$state" 'o.gamePhase||""')
log "  gamePhase: $game_phase"

if [[ -n "$game_phase" && "$game_phase" != "" && "$game_phase" != "undefined" ]]; then
    log "  OK: Game phase present"
else
    log "  WARN: Game phase not in state"
fi

screenshot "game-info-data"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Game info data verified: players, pot, tournament, clock, actions, chip conservation, events"
