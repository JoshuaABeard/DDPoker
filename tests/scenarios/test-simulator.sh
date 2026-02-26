#!/usr/bin/env bash
# test-simulator.sh — Verify hand equity simulator via /simulator endpoint.
#
# Tests S-001 through S-012:
#   - Run simulation with pocket aces
#   - Run simulation with community cards
#   - Verify error handling for invalid cards (hard FAIL)
#
# Usage:
#   bash tests/scenarios/test-simulator.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# S-001: Basic simulation — pocket aces
# ============================================================
log "=== S-001: Pocket Aces Simulation ==="
RESULT=$(api_post_json /simulator '{"holeCards": ["As", "Ah"], "numSimulations": 5000}' 2>/dev/null) \
    || die "Simulator request failed"
COMPLETED=$(jget "$RESULT" 'o.completed')
log "  Result: completed=$COMPLETED"
if [[ "$COMPLETED" == "true" ]]; then
    log "  OK: Simulation completed"
else
    log "FAIL: Simulation did not complete"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# S-003: Simulation with community cards
# ============================================================
log "=== S-003: Simulation with Community Cards ==="
RESULT=$(api_post_json /simulator '{"holeCards": ["Ks", "Qs"], "community": ["Js", "Ts", "2d"], "numSimulations": 5000}' 2>/dev/null) \
    || die "Simulator request failed"
COMPLETED=$(jget "$RESULT" 'o.completed')
HOLE=$(jget "$RESULT" 'o.holeCards')
COMM=$(jget "$RESULT" 'o.community')
log "  holeCards=$HOLE, community=$COMM, completed=$COMPLETED"
if [[ "$COMPLETED" == "true" ]]; then
    log "  OK: Simulation with board completed"
else
    log "FAIL: Simulation with board did not complete"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# S-007: Invalid cards — must be rejected with BadRequest
# ============================================================
log "=== S-007: Invalid Card Error ==="
RESULT=$(api_post_json /simulator '{"holeCards": ["Zz", "Ah"]}' 2>/dev/null) || true
ERROR=$(jget "$RESULT" 'o.error||""')
if [[ "$ERROR" == "BadRequest" ]]; then
    log "  OK: Invalid card rejected with BadRequest"
else
    log "FAIL: S-007 — invalid card 'Zz' should return BadRequest, got: $RESULT"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# S-008: Wrong card count — single card must be rejected
# ============================================================
log "=== S-008: Wrong Card Count ==="
RESULT=$(api_post_json /simulator '{"holeCards": ["As"]}' 2>/dev/null) || true
ERROR=$(jget "$RESULT" 'o.error||""')
if [[ "$ERROR" == "BadRequest" ]]; then
    log "  OK: Single card rejected with BadRequest"
else
    log "FAIL: S-008 — single holeCard should return BadRequest, got: $RESULT"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# S-010: Missing holeCards — must be rejected
# ============================================================
log "=== S-010: Missing Hole Cards ==="
RESULT=$(api_post_json /simulator '{"community": ["As", "Ks", "Qs"]}' 2>/dev/null) || true
ERROR=$(jget "$RESULT" 'o.error||""')
if [[ "$ERROR" == "BadRequest" ]]; then
    log "  OK: Missing holeCards rejected with BadRequest"
else
    log "FAIL: S-010 — missing holeCards should return BadRequest, got: $RESULT"
    FAILURES=$((FAILURES+1))
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Simulator verified: pocket aces, with board, invalid card handling"
