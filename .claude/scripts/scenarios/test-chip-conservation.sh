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
lib_start_game 3

log "Playing $HANDS_TARGET hands with CALL strategy, validating after each hand..."
HANDS_VERIFIED=0
LAST_MODE=""
LAST_CHANGE=$(date +%s)

while [[ $HANDS_VERIFIED -lt $HANDS_TARGET ]]; do
    STATE=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
    PHASE=$(jget "$STATE" 'o.gamePhase || "NONE"')
    REMAINING=$(jget "$STATE" 'o.tournament && o.tournament.playersRemaining || 0')

    if [[ "$MODE" != "$LAST_MODE" ]]; then
        LAST_MODE="$MODE"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && die "Stuck in mode $MODE"

    # Game over before target hands?
    if [[ "$REMAINING" -le 1 && $HANDS_VERIFIED -gt 0 ]]; then
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
            # After BETWEEN_HANDS, run validation once before the next DEAL prompt
            if [[ "$PHASE" == "BETWEEN_HANDS" ]] && [[ $HANDS_VERIFIED -gt 0 ]]; then
                : # handled below via PHASE check
            fi
            sleep 0.2
            ;;
    esac

    # Run /validate after each hand completes (BETWEEN_HANDS + DEAL mode)
    if [[ "$MODE" == "DEAL" ]]; then
        VRESULT=$(api GET /validate 2>/dev/null) || { log "WARN: /validate call failed"; sleep 0.3; continue; }
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
        log "  Hand $HANDS_VERIFIED validated OK (chipConservation valid, inputModeConsistent)"
    fi

    sleep 0.15
done

pass "Chip conservation valid after $HANDS_VERIFIED hands"
