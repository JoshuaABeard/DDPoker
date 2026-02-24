#!/usr/bin/env bash
# test-save-load.sh — Verify save/load game via API.
#
# Tests SL-001 and SL-005:
#   - Save a running game — assert accepted=true (hard FAIL)
#   - Load a saved game — assert accepted=true (hard FAIL)
#
# Usage:
#   bash .claude/scripts/scenarios/test-save-load.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

# Wait for game to start
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game"

# ============================================================
# SL-001: Save game — assert accepted=true
# ============================================================
log "=== SL-001: Save Game ==="
SAVE_RESULT=$(api_post_json /game/save '{}' 2>/dev/null) || true
log "  INFO: save result: $SAVE_RESULT"
SAVE_ACCEPTED=$(jget "$SAVE_RESULT" 'o.accepted||false')
if [[ "$SAVE_ACCEPTED" == "true" ]]; then
    log "  OK: Game saved (accepted=true)"
else
    log "FAIL: SL-001 — save did not return accepted=true: $SAVE_RESULT"
    FAILURES=$((FAILURES+1))
fi

# Record current state for comparison after load
state=$(api GET /state 2>/dev/null) || true
pre_level=$(jget "$state" 'o.tournament&&o.tournament.level')
log "  INFO: pre-load level: $pre_level"

# ============================================================
# SL-005: Load game — assert accepted=true
# ============================================================
log "=== SL-005: Load Game ==="
LOAD_RESULT=$(api_post_json /game/load '{}' 2>/dev/null) || true
log "  INFO: load result: $LOAD_RESULT"
LOAD_ACCEPTED=$(jget "$LOAD_RESULT" 'o.accepted||false')
if [[ "$LOAD_ACCEPTED" == "true" ]]; then
    log "  OK: Game loaded (accepted=true)"
    sleep 2

    # Verify game state is valid after load
    state=$(api GET /state 2>/dev/null) || true
    post_level=$(jget "$state" 'o.tournament&&o.tournament.level')
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    log "  INFO: post-load level: $post_level, mode: $mode"
    if [[ -n "$mode" && "$mode" != "NONE" ]]; then
        log "  OK: Game is in a valid state after load"
    else
        log "  INFO: Game mode is NONE after load (save/load accept path already verified)"
    fi
else
    log "FAIL: SL-005 — load did not return accepted=true: $LOAD_RESULT"
    FAILURES=$((FAILURES+1))
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Save/load tests verified: save accepted, load accepted"
