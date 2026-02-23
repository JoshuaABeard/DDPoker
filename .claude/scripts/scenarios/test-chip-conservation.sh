#!/usr/bin/env bash
# test-chip-conservation.sh — Verify chip conservation holds after every hand.
#
# Plays N hands with the CALL strategy and calls GET /validate after each
# BETWEEN_HANDS phase. Fails immediately if any table reports invalid chip
# conservation (playerChips + inPot != buyinPerPlayer * numPlayers).
#
# Usage:
#   bash .claude/scripts/scenarios/test-chip-conservation.sh [options]
#
# Options:
#   --hands N         Number of hands to verify (default: 10)
#   --skip-build      Skip mvn build
#   --skip-launch     Reuse a running game (skips build + launch)
#   --ai-delay-ms N   AI delay in ms (default: 0)

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

HANDS_TARGET=10
lib_parse_args "$@"

# Extra option parsing
for i in "$@"; do
    case "$i" in --hands) HANDS_TARGET="$2"; shift 2 ;; esac
done

lib_launch
# Use enough chips so no player is eliminated during the test (avoids tournament ending early).
lib_start_game 3 '"buyinChips": 50000'

log "Playing $HANDS_TARGET hands with CALL strategy, validating after each hand..."
HANDS_VERIFIED=0
PREV_PHASE=""
PREV_DEALER="-1"
LAST_PHASE_CHANGE=$(date +%s)

validate_now() {
    local VRESULT CC_VALID IM_VALID WARNS
    VRESULT=$(api GET /validate 2>/dev/null) || { log "WARN: /validate call failed"; return; }
    CC_VALID=$(jget "$VRESULT" 'o.chipConservation && o.chipConservation.valid')
    IM_VALID=$(jget "$VRESULT" 'o.inputModeConsistent')
    WARNS=$(jget "$VRESULT" '(o.warnings||[]).join("; ")')

    if [[ "$CC_VALID" != "true" || "$IM_VALID" != "true" ]]; then
        log "VALIDATE FAILED after hand $HANDS_VERIFIED:"
        log "  chipConservation.valid = $CC_VALID"
        log "  inputModeConsistent    = $IM_VALID"
        [[ -n "$WARNS" ]] && log "  warnings: $WARNS"
        screenshot "validation-failure-hand${HANDS_VERIFIED}"
        die "Chip conservation violated after hand $HANDS_VERIFIED"
    fi
    HANDS_VERIFIED=$((HANDS_VERIFIED + 1))
    log "  Hand $HANDS_VERIFIED validated OK"
}

while [[ $HANDS_VERIFIED -lt $HANDS_TARGET ]]; do
    STATE=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
    PHASE=$(jget "$STATE" 'o.gamePhase || "NONE"')
    REMAINING=$(jget "$STATE" 'o.tournament && o.tournament.playersRemaining || 99')

    # Detect hand completion by watching for a new PRE_FLOP starting.
    #
    # The embedded server auto-deals hands without DEAL mode, and BETWEEN_HANDS
    # is too brief (milliseconds) to reliably catch at 0.15s polling intervals.
    #
    # Two signals for "new hand just started":
    #   1. Phase transitioned FROM a non-PRE_FLOP hand phase TO PRE_FLOP
    #      (hand went to flop/turn/river, then a new hand started)
    #   2. Phase stayed PRE_FLOP but dealer seat changed
    #      (previous hand ended during preflop — everyone folded)
    if [[ "$PHASE" == "PRE_FLOP" ]]; then
        DEALER=$(jget "$STATE" 'o.tables&&o.tables[0]&&o.tables[0].dealerSeat||0')
        if [[ "$PREV_PHASE" != "PRE_FLOP" && "$PREV_PHASE" != "" && "$PREV_PHASE" != "NONE" ]]; then
            # Came from flop/turn/river/showdown/between-hands — validate
            validate_now
        elif [[ "$PREV_PHASE" == "PRE_FLOP" && "$DEALER" != "$PREV_DEALER" ]]; then
            # Dealer rotated — previous hand ended during preflop (everyone folded)
            validate_now
        fi
        PREV_DEALER="$DEALER"
    fi

    if [[ "$PHASE" != "$PREV_PHASE" ]]; then
        LAST_PHASE_CHANGE=$(date +%s)
    fi
    PREV_PHASE="$PHASE"

    # Game over — exit cleanly if tournament ended
    if [[ "$REMAINING" =~ ^[0-9]+$ && "$REMAINING" -le 1 ]]; then
        log "Game ended after $HANDS_VERIFIED hands (${REMAINING} players remaining)"
        break
    fi

    case "$MODE" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            IS_HUMAN=$(jget "$STATE" 'o.currentAction && o.currentAction.isHumanTurn || false')
            if [[ "$IS_HUMAN" == "true" ]]; then
                AVAIL=$(jget "$STATE" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
                if echo "$AVAIL" | grep -q "CHECK"; then
                    api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
                else
                    api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
                fi
                LAST_PHASE_CHANGE=$(date +%s)
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
    esac

    # Stuck detection: game phase hasn't changed for STUCK_TIMEOUT seconds
    [[ $(($(date +%s) - LAST_PHASE_CHANGE)) -gt $STUCK_TIMEOUT ]] && die "Stuck in mode $MODE / phase $PHASE (no phase change for ${STUCK_TIMEOUT}s)"

    sleep 0.15
done

pass "Chip conservation valid after $HANDS_VERIFIED hands"
