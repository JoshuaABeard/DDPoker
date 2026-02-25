#!/usr/bin/env bash
# test-pause-allin.sh — Verify "Pause on All-In" produces CONTINUE_LOWER inputMode.
#
# Tests PA-001 through PA-005:
#   - Enable gameplay.pauseAllin=true via /options
#   - Start a 3-player game with buyinChips=100 and blinds=50/100
#   - BB (100 chips) posts 100 = all-in (full BB, no partial posting)
#   - UTG/SB go all-in via ALL_IN action; BB checks freely (canCheck=true,
#     no chips needed) and stays in hand → 2+ players all-in with cards
#     → AllInRunoutPaused → CONTINUE_LOWER appears
#   - Send CONTINUE_LOWER, assert game resumes
#
# The pause-allin option routes through WebSocketTournamentDirector on the
# embedded server. When all remaining players are all-in, the director enters
# the AllInRunoutPaused state which surfaces as inputMode "CONTINUE_LOWER".
# With buyinChips=100 / blinds=50/100, BB posts the full big blind (all-in).
# UTG/SB go all-in by sending ALL_IN. BB gets canCheck=true (nobody raised)
# and checks to stay in the hand. With 2+ players all-in and holding cards,
# CONTINUE_LOWER appears within a few seconds.
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

lib_start_game 3 '"buyinChips": 100, "blindLevels": [{"small": 50, "big": 100, "ante": 0, "minutes": 60}]'
screenshot "pause-allin-start"

# ============================================================
# PA-002: Poll for CONTINUE_LOWER mode while acting on every human turn.
#
# Action strategy to reliably produce an all-in runout:
#   CALL_RAISE → ALL_IN: forces the player all-in (canAllIn=true when
#       calling equals going all-in with buyinChips=100, big=100)
#   CHECK_BET / CHECK_RAISE → CHECK: BB posted the full big blind and nobody
#       raised, so canCheck=true — checking keeps BB in the hand with 0 chips.
#       Sending CALL would be rejected (wrong mode), and ALL_IN → FOLD because
#       canAllIn=false for a 0-chip all-in player, both of which remove BB's
#       cards and prevent the all-in runout from firing.
# ============================================================
log "=== PA-002: Polling for CONTINUE_LOWER (timeout=60s) ==="

POLL_START=$(date +%s)
POLL_TIMEOUT=60
PAUSE_ALLIN_APPEARED=false
HUMAN_ACTIONS=0
LAST_MODE=""

while true; do
    STATE=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    close_visible_dialog_if_any "pause-allin-loop" > /dev/null 2>&1 || true
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
        CALL_RAISE)
            IS_HUMAN=$(jget "$STATE" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$IS_HUMAN" == "true" ]]; then
                api_post_json /action '{"type":"ALL_IN"}' > /dev/null 2>&1 || true
                HUMAN_ACTIONS=$((HUMAN_ACTIONS+1))
            fi
            ;;
        CHECK_BET|CHECK_RAISE)
            IS_HUMAN=$(jget "$STATE" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$IS_HUMAN" == "true" ]]; then
                api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
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
            # Usually AI acting, but if control state still reports a human turn,
            # submit a safe fallback action to avoid deadlock in QUITSAVE.
            IS_HUMAN=$(jget "$STATE" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$IS_HUMAN" == "true" ]]; then
                CAN_CHECK=$(jget "$STATE" 'Array.isArray(o.currentAction&&o.currentAction.availableActions) && o.currentAction.availableActions.includes("CHECK")')
                CAN_CALL=$(jget "$STATE" 'Array.isArray(o.currentAction&&o.currentAction.availableActions) && o.currentAction.availableActions.includes("CALL")')
                CAN_ALL_IN=$(jget "$STATE" 'Array.isArray(o.currentAction&&o.currentAction.availableActions) && o.currentAction.availableActions.includes("ALL_IN")')

                if [[ "$CAN_CHECK" == "true" ]]; then
                    api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
                    HUMAN_ACTIONS=$((HUMAN_ACTIONS+1))
                elif [[ "$CAN_ALL_IN" == "true" ]]; then
                    api_post_json /action '{"type":"ALL_IN"}' > /dev/null 2>&1 || true
                    HUMAN_ACTIONS=$((HUMAN_ACTIONS+1))
                elif [[ "$CAN_CALL" == "true" ]]; then
                    api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
                    HUMAN_ACTIONS=$((HUMAN_ACTIONS+1))
                fi
            fi

            close_visible_dialog_if_any "pause-allin-idle" > /dev/null 2>&1 || true
            ;;
    esac

    ELAPSED=$(($(date +%s) - POLL_START))
    if [[ $ELAPSED -gt $POLL_TIMEOUT ]]; then
        log "FAIL: PA-002 — CONTINUE_LOWER never appeared after ${POLL_TIMEOUT}s ($HUMAN_ACTIONS human action(s) taken); last mode=$MODE"
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
    # PA-005: Assert mode eventually leaves CONTINUE_LOWER.
    # Poll-first, send-second: wait for the current mode, then send
    # CONTINUE_LOWER only if still needed. This avoids firing a second
    # sendContinueRunout() before the server has processed the first
    # (from PA-004), which could cause a double-resolve and leave the
    # game stuck waiting for a CL that was already sent.
    # ============================================================
    log "=== PA-005: Assert mode left CONTINUE_LOWER ==="
    CL_START=$(date +%s)
    CL_TIMEOUT=30
    MODE2="CONTINUE_LOWER"
    while true; do
        sleep 0.3
        STATE2=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
        close_visible_dialog_if_any "pause-allin-continue" > /dev/null 2>&1 || true
        MODE2=$(jget "$STATE2" 'o.inputMode || "NONE"')
        if [[ "$MODE2" != "CONTINUE_LOWER" ]]; then
            break
        fi
        # Mode is still CONTINUE_LOWER — send a drain CL and keep polling.
        api_post_json /action '{"type":"CONTINUE_LOWER"}' > /dev/null 2>&1 || true
        ELAPSED2=$(($(date +%s) - CL_START))
        if [[ $ELAPSED2 -gt $CL_TIMEOUT ]]; then
            log "FAIL: PA-005 — Still in CONTINUE_LOWER after ${CL_TIMEOUT}s"
            FAILURES=$((FAILURES+1))
            break
        fi
    done
    if [[ "$MODE2" != "CONTINUE_LOWER" ]]; then
        log "  OK: Mode transitioned to $MODE2 after all CONTINUE_LOWER pauses"
        screenshot "pause-allin-after-continue"
    fi
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Pause-on-All-In verified: CONTINUE_LOWER appeared, CONTINUE_LOWER accepted, game resumed"
