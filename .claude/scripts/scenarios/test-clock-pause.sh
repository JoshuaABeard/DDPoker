#!/usr/bin/env bash
# test-clock-pause.sh — Verify clock pause/resume via /control endpoint.
#
# Tests C-040, C-041:
#   - Start game, verify clock is running
#   - POST /control {"action":"PAUSE"}, verify isPaused is true
#   - POST /control {"action":"RESUME"}, verify isPaused is false
#
# Usage:
#   bash .claude/scripts/scenarios/test-clock-pause.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

# Wait for game to be in progress
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game start"

# ============================================================
# C-040a: Verify clock is running (secondsRemaining present)
# ============================================================
log "=== C-040: Clock Running ==="
state=$(api GET /state 2>/dev/null) || die "Could not read state"

seconds=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.secondsRemaining')
is_paused=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.isPaused')
log "  secondsRemaining: $seconds"
log "  isPaused: $is_paused"

if [[ -n "$seconds" && "$seconds" != "" && "$seconds" != "undefined" ]]; then
    log "  OK: Clock data present"
else
    log "FAIL: Clock secondsRemaining not present"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# C-040b: Pause the clock
# ============================================================
log "=== C-040: Pause Clock ==="
pause_result=$(api_post_json /control '{"action":"PAUSE"}' 2>/dev/null) || true
accepted=$(jget "$pause_result" 'o.accepted')
action=$(jget "$pause_result" 'o.action||""')

if [[ "$accepted" == "true" ]]; then
    log "  OK: PAUSE accepted"
else
    log "FAIL: PAUSE not accepted: $pause_result"
    FAILURES=$((FAILURES+1))
fi

if [[ "$action" == "PAUSE" ]]; then
    log "  OK: Response action is PAUSE"
else
    log "  WARN: Response action: $action"
fi

# Verify isPaused is true in state
sleep 0.5
state=$(api GET /state 2>/dev/null) || die "Could not read state after pause"
paused_val=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.isPaused')
log "  isPaused after PAUSE: $paused_val"

if [[ "$paused_val" == "true" ]]; then
    log "  OK: Clock is paused"
else
    log "FAIL: Clock isPaused should be true after PAUSE, got: $paused_val"
    FAILURES=$((FAILURES+1))
fi

# Record seconds for comparison
paused_seconds=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.secondsRemaining')
log "  secondsRemaining while paused: $paused_seconds"

# Wait briefly and verify seconds haven't changed (clock is frozen)
sleep 2
state=$(api GET /state 2>/dev/null) || true
paused_seconds2=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.secondsRemaining')
log "  secondsRemaining 2s later: $paused_seconds2"

if [[ -n "$paused_seconds" && -n "$paused_seconds2" && "$paused_seconds" != "undefined" && "$paused_seconds2" != "undefined" ]]; then
    if [[ "$paused_seconds2" -ge "$paused_seconds" ]]; then
        log "  OK: Clock frozen while paused (seconds did not decrease)"
    else
        log "  WARN: Seconds decreased while paused ($paused_seconds -> $paused_seconds2)"
    fi
fi

screenshot "clock-paused"

# ============================================================
# C-041: Resume the clock
# ============================================================
log "=== C-041: Resume Clock ==="
resume_result=$(api_post_json /control '{"action":"RESUME"}' 2>/dev/null) || true
accepted=$(jget "$resume_result" 'o.accepted')
action=$(jget "$resume_result" 'o.action||""')

if [[ "$accepted" == "true" ]]; then
    log "  OK: RESUME accepted"
else
    log "FAIL: RESUME not accepted: $resume_result"
    FAILURES=$((FAILURES+1))
fi

if [[ "$action" == "RESUME" ]]; then
    log "  OK: Response action is RESUME"
else
    log "  WARN: Response action: $action"
fi

# Verify isPaused is false in state
sleep 0.5
state=$(api GET /state 2>/dev/null) || die "Could not read state after resume"
resumed_val=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.isPaused')
log "  isPaused after RESUME: $resumed_val"

if [[ "$resumed_val" == "false" ]]; then
    log "  OK: Clock is running"
else
    log "FAIL: Clock isPaused should be false after RESUME, got: $resumed_val"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# Verify invalid control action returns error
# ============================================================
log "=== Invalid Control Action ==="
bad_result=$(api_post_json /control '{"action":"EXPLODE"}' 2>/dev/null) || true
bad_error=$(jget "$bad_result" 'o.error||""')
if [[ "$bad_error" == "BadRequest" ]]; then
    log "  OK: Invalid action correctly rejected with BadRequest"
else
    log "  WARN: Invalid action response: $bad_result"
fi

screenshot "clock-resumed"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Clock pause/resume verified: PAUSE sets isPaused=true, RESUME sets isPaused=false"
