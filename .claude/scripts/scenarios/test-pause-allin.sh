#!/usr/bin/env bash
# test-pause-allin.sh — Verify "Pause on All-In" produces PAUSE_ALLIN inputMode.
#
# Tests PA-001 through PA-005:
#   - Enable gameplay.pauseAllin=true via /options
#   - Start a 3-player game and play hands, taking CALL at every human turn
#   - Poll /state watching for inputMode == "PAUSE_ALLIN" (up to 120s)
#   - When PAUSE_ALLIN appears, send CONTINUE and assert game resumes
#
# The PAUSE_ALLIN mode fires when an AI player goes all-in. By always calling,
# we maximize the chance that an AI is forced all-in. 120s is ample time for
# this to occur in a 3-player game.
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
# PA-002: Poll for PAUSE_ALLIN mode while CALLing every human turn
# ============================================================
log "=== PA-002: Polling for PAUSE_ALLIN (timeout=120s) ==="

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
        PAUSE_ALLIN)
            log "  PAUSE_ALLIN detected — all-in pause fired"
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
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$MODE\"}" > /dev/null 2>&1 || true
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
        log "FAIL: PA-002 — PAUSE_ALLIN never appeared after ${POLL_TIMEOUT}s ($HUMAN_ACTIONS human actions taken)"
        FAILURES=$((FAILURES+1))
        break
    fi
    sleep 0.2
done

# ============================================================
# PA-003: Assert PAUSE_ALLIN appeared
# ============================================================
log "=== PA-003: Assert PAUSE_ALLIN appeared ==="
if [[ "$PAUSE_ALLIN_APPEARED" == "true" ]]; then
    log "  OK: PAUSE_ALLIN appeared after $HUMAN_ACTIONS human action(s)"
    screenshot "pause-allin-detected"
else
    log "  (already failed above)"
fi

# ============================================================
# PA-004: Send CONTINUE and assert mode transitions
# ============================================================
if [[ "$PAUSE_ALLIN_APPEARED" == "true" ]]; then
    log "=== PA-004: Send CONTINUE to advance past PAUSE_ALLIN ==="
    CRESP=$(api_post_json /action '{"type":"CONTINUE"}' 2>/dev/null) || true
    ACCEPTED=$(jget "$CRESP" 'o.accepted||false')
    if [[ "$ACCEPTED" == "true" ]]; then
        log "  OK: CONTINUE accepted"
    else
        log "FAIL: PA-004 — CONTINUE not accepted: $CRESP"
        FAILURES=$((FAILURES+1))
    fi

    # ============================================================
    # PA-005: Assert mode transitioned away from PAUSE_ALLIN
    # ============================================================
    log "=== PA-005: Assert mode left PAUSE_ALLIN ==="
    sleep 1
    STATE2=$(api GET /state 2>/dev/null) || true
    MODE2=$(jget "$STATE2" 'o.inputMode || "NONE"')
    if [[ "$MODE2" != "PAUSE_ALLIN" ]]; then
        log "  OK: Mode transitioned to $MODE2 after CONTINUE"
        screenshot "pause-allin-after-continue"
    else
        log "FAIL: PA-005 — Still in PAUSE_ALLIN after CONTINUE (mode=$MODE2)"
        FAILURES=$((FAILURES+1))
    fi
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Pause-on-All-In verified: PAUSE_ALLIN appeared, CONTINUE accepted, game resumed"
