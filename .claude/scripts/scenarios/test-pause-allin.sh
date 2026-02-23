#!/usr/bin/env bash
# test-pause-allin.sh — Verify "Pause on All-In" produces CONTINUE_LOWER inputMode.
#
# Tests PA-001 through PA-005:
#   - Enable gameplay.pauseAllin=true via /options
#   - Start a 3-player game and play hands, taking CALL at every human turn
#   - Poll /state watching for inputMode == "CONTINUE_LOWER" (up to 120s)
#   - When CONTINUE_LOWER appears, send CONTINUE_LOWER action and assert game resumes
#
# The pause-allin option routes through WebSocketTournamentDirector on the
# embedded server. When an AI goes all-in, the director enters the
# AllInRunoutPaused state which surfaces as inputMode "CONTINUE_LOWER".
# By always CALLing we maximise chip pressure on AI players, forcing all-ins.
# 120s is ample time for this to occur in a 3-player game.
#
# Usage:
#   bash .claude/scripts/scenarios/test-pause-allin.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# PA-001: Enable gameplay.pauseAllin
# ============================================================
log "=== PA-001: Enable gameplay.pauseAllin ==="
api_post_json /options '{"gameplay.pauseAllin": true}' > /dev/null
OPTIONS=$(api GET /options 2>/dev/null) || die "Could not read /options"
PA=$(jget "$OPTIONS" 'o.gameplay && o.gameplay.pauseAllin')
if [[ "$PA" == "true" ]]; then
    log "  OK: gameplay.pauseAllin is active"
else
    die "gameplay.pauseAllin did not activate (got: $PA)"
fi

lib_start_game 3
screenshot "pause-allin-start"

# ============================================================
# PA-002: Poll for CONTINUE_LOWER mode while CALLing every human turn
# ============================================================
log "=== PA-002: Polling for CONTINUE_LOWER (timeout=120s) ==="

POLL_START=$(date +%s)
POLL_TIMEOUT=120
PAUSE_ALLIN_APPEARED=false
HUMAN_ACTIONS=0
LAST_MODE=""

while true; do
    STATE=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    MODE=$(jget "$STATE" 'o.inputMode || "NONE"')

    if [[ "$MODE" != "$LAST_MODE" ]]; then
        log "  mode: $MODE"
        LAST_MODE="$MODE"
    fi

    case "$MODE" in
        CONTINUE_LOWER)
            log "  CONTINUE_LOWER detected — all-in pause fired"
            PAUSE_ALLIN_APPEARED=true
            break
            ;;
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            IS_HUMAN=$(jget "$STATE" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$IS_HUMAN" == "true" ]]; then
                api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
                HUMAN_ACTIONS=$((HUMAN_ACTIONS+1))
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            ;;
        CONTINUE)
            api_post_json /action '{"type":"CONTINUE"}' > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
        QUITSAVE|NONE)
            # AI acting — just wait
            ;;
    esac

    ELAPSED=$(($(date +%s) - POLL_START))
    if [[ $ELAPSED -gt $POLL_TIMEOUT ]]; then
        log "FAIL: PA-002 — CONTINUE_LOWER never appeared after ${POLL_TIMEOUT}s ($HUMAN_ACTIONS human actions taken)"
        FAILURES=$((FAILURES+1))
        break
    fi
    sleep 0.2
done

# ============================================================
# PA-003: Assert CONTINUE_LOWER appeared
# ============================================================
log "=== PA-003: Assert CONTINUE_LOWER appeared ==="
if [[ "$PAUSE_ALLIN_APPEARED" == "true" ]]; then
    log "  OK: CONTINUE_LOWER appeared after $HUMAN_ACTIONS human action(s)"
    screenshot "pause-allin-detected"
else
    log "  (already failed above)"
fi

# ============================================================
# PA-004: Send CONTINUE_LOWER and assert mode transitions
# ============================================================
if [[ "$PAUSE_ALLIN_APPEARED" == "true" ]]; then
    log "=== PA-004: Send CONTINUE_LOWER to advance past all-in pause ==="
    CRESP=$(api_post_json /action '{"type":"CONTINUE_LOWER"}' 2>/dev/null) || true
    ACCEPTED=$(jget "$CRESP" 'o.accepted||false')
    if [[ "$ACCEPTED" == "true" ]]; then
        log "  OK: CONTINUE_LOWER accepted"
    else
        log "FAIL: PA-004 — CONTINUE_LOWER not accepted: $CRESP"
        FAILURES=$((FAILURES+1))
    fi

    # ============================================================
    # PA-005: Assert mode transitioned away from CONTINUE_LOWER
    # ============================================================
    log "=== PA-005: Assert mode left CONTINUE_LOWER ==="
    sleep 1
    STATE2=$(api GET /state 2>/dev/null) || true
    MODE2=$(jget "$STATE2" 'o.inputMode || "NONE"')
    if [[ "$MODE2" != "CONTINUE_LOWER" ]]; then
        log "  OK: Mode transitioned to $MODE2 after CONTINUE_LOWER"
        screenshot "pause-allin-after-continue"
    else
        log "FAIL: PA-005 — Still in CONTINUE_LOWER after sending CONTINUE_LOWER (mode=$MODE2)"
        FAILURES=$((FAILURES+1))
    fi
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Pause-on-All-In verified: CONTINUE_LOWER appeared, CONTINUE_LOWER accepted, game resumed"
