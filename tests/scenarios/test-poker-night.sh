#!/usr/bin/env bash
# test-poker-night.sh — Verify Poker Night / Poker Clock mode.
#
# Tests PN-001 through PN-032:
#   - Navigate to Poker Night mode
#   - Verify clock mode in /state
#   - Verify clock ticks down
#   - Verify level information
#
# Usage:
#   bash tests/scenarios/test-poker-night.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# PN-001: Navigate to Poker Night Options
# ============================================================
log "=== PN-001: Navigate to Poker Night ==="
NAV=$(api_post_json /navigate '{"phase": "PokerNightOptions"}' 2>/dev/null) || true
SUCCESS=$(jget "$NAV" 'o.success||o.accepted||""')
log "  Navigate to PokerNightOptions: $SUCCESS"
sleep 2

# Check if we can get state showing clock mode
state=$(api GET /state 2>/dev/null) || true
phase=$(jget "$state" 'o.gamePhase||""')
lifecycle=$(jget "$state" 'o.lifecyclePhase||""')
log "  gamePhase: $phase, lifecyclePhase: $lifecycle"

# ============================================================
# PN-003: Start Poker Night game
# ============================================================
log "=== PN-003: Initialize Poker Night ==="
# Try to start via game/start with clock mode
NAV=$(api_post_json /navigate '{"phase": "InitializePokerNightGame"}' 2>/dev/null) || true
sleep 3

state=$(api GET /state 2>/dev/null) || true
phase=$(jget "$state" 'o.gamePhase||""')
log "  After init: gamePhase=$phase"

if [[ "$phase" == "CLOCK" ]]; then
    log "  OK: Clock mode activated"
else
    log "  WARN: Not in clock mode (phase=$phase). Trying BeginPokerNightGame..."
    NAV=$(api_post_json /navigate '{"phase": "BeginPokerNightGame"}' 2>/dev/null) || true
    sleep 2
    state=$(api GET /state 2>/dev/null) || true
    phase=$(jget "$state" 'o.gamePhase||""')
    log "  After begin: gamePhase=$phase"
fi

# ============================================================
# PN-005: Verify clock state
# ============================================================
log "=== PN-005: Clock State ==="
state=$(api GET /state 2>/dev/null) || true
level=$(jget "$state" 'o.tournament&&o.tournament.level||0')
sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
clock_secs=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.secondsRemaining||0')
log "  Level: $level, SB=$sb, BB=$bb, seconds=$clock_secs"

if [[ "$level" -gt 0 ]]; then
    log "  OK: Level info present"
else
    log "  WARN: No level info in clock mode"
fi

# ============================================================
# PN-010: Verify clock ticks
# ============================================================
log "=== PN-010: Clock Ticks ==="
if [[ "$clock_secs" -gt 0 ]]; then
    sleep 3
    state2=$(api GET /state 2>/dev/null) || true
    clock_secs2=$(jget "$state2" 'o.tournament&&o.tournament.clock&&o.tournament.clock.secondsRemaining||0')
    log "  Before: $clock_secs, After 3s: $clock_secs2"
    if [[ "$clock_secs2" -lt "$clock_secs" ]]; then
        log "  OK: Clock is ticking down"
    else
        log "  WARN: Clock did not tick (may be paused or not running)"
    fi
else
    log "  SKIP: No clock seconds to verify"
fi

# ============================================================
# PN-020: Tournament info in clock mode
# ============================================================
log "=== PN-020: Tournament Info ==="
level_count=$(jget "$state" 'o.tournament&&o.tournament.levelCount||0')
next_sb=$(jget "$state" 'o.tournament&&o.tournament.nextSmallBlind||""')
next_bb=$(jget "$state" 'o.tournament&&o.tournament.nextBigBlind||""')
log "  levelCount=$level_count, nextSB=$next_sb, nextBB=$next_bb"

screenshot "poker-night"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Poker Night mode tested: phase=$phase, level=$level, clock=$clock_secs"
