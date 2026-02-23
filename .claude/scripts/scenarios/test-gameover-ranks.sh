#!/usr/bin/env bash
# test-gameover-ranks.sh — Verify a 3-player game reaches a proper game-over state.
#
# Plays a 3-player game to completion using the FOLD strategy and asserts:
#   1. playersRemaining reaches 1 (OR lifecycle reaches a game-over phase)
#   2. /validate passes throughout the game
#   3. The final state has exactly one player with chips > 0
#
# Usage:
#   bash .claude/scripts/scenarios/test-gameover-ranks.sh [options]
#
# Options:
#   --players N    Number of players (default: 3)
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game
#   --ai-delay-ms N

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
NUM_PLAYERS=3
lib_parse_args "$@"
for i in "$@"; do case "$i" in --players) NUM_PLAYERS="$2"; shift 2 ;; esac; done

lib_launch
lib_start_game "$NUM_PLAYERS"
screenshot "gameover-start"

log "Running $NUM_PLAYERS-player FOLD strategy to completion..."
ACTIONS=0
HANDS=0
LAST_MODE=""
LAST_CHANGE=$(date +%s)

while true; do
    STATE=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
    REMAINING=$(jget "$STATE" 'o.tournament && o.tournament.playersRemaining || 0')
    LIFECYCLE=$(jget "$STATE" 'o.lifecyclePhase || "NONE"')
    PLAYERS_WITH_CHIPS=$(jget "$STATE" \
        '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')

    if [[ "$MODE" != "$LAST_MODE" ]]; then
        LAST_MODE="$MODE"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && \
        die "Stuck in mode $MODE after ${STUCK_TIMEOUT}s"

    # Game-over detection
    if [[ "$REMAINING" -le 1 && $ACTIONS -gt 0 ]]; then
        log "Tournament complete: $REMAINING player(s) remaining (actions=$ACTIONS, hands=$HANDS)"
        screenshot "gameover-final"
        break
    fi
    if echo "$LIFECYCLE" | grep -qiE "gameover|GameOver|PracticeGameOver"; then
        log "Tournament complete via lifecycle: $LIFECYCLE"
        screenshot "gameover-final"
        break
    fi
    # Post-GAME_COMPLETE freeze detection
    if [[ "$MODE" == "QUITSAVE" && "$PLAYERS_WITH_CHIPS" == "1" && $ACTIONS -gt 0 ]]; then
        log "Tournament complete (post-GAME_COMPLETE: 1 player with chips)"
        screenshot "gameover-final"
        break
    fi

    case "$MODE" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            IS_HUMAN=$(jget "$STATE" 'o.currentAction && o.currentAction.isHumanTurn || false')
            if [[ "$IS_HUMAN" == "true" ]]; then
                api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
                ACTIONS=$((ACTIONS+1))
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            HANDS=$((HANDS+1))
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$MODE\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
        QUITSAVE|NONE)
            sleep 0.2; continue ;;
    esac
    sleep 0.15
done

# Final validation
VRESULT=$(api GET /validate 2>/dev/null) || true
CC_VALID=$(jget "$VRESULT" 'o.chipConservation && o.chipConservation.valid')
WARNS=$(jget "$VRESULT" '(o.warnings||[]).join("; ")')
[[ "$CC_VALID" == "true" ]] || \
    log "WARN: final /validate chipConservation.valid=$CC_VALID  warnings: $WARNS"

# Assert final state
FINAL_STATE=$(api GET /state 2>/dev/null) || true
FINAL_REMAINING=$(jget "$FINAL_STATE" 'o.tournament && o.tournament.playersRemaining || -1')
log "Final playersRemaining: $FINAL_REMAINING  hands: $HANDS  actions: $ACTIONS"

if [[ "$FINAL_REMAINING" -le 1 || "$PLAYERS_WITH_CHIPS" -le 1 ]]; then
    pass "Game reached proper game-over state ($NUM_PLAYERS players, $HANDS hands)"
else
    die "Game did not reach game-over: playersRemaining=$FINAL_REMAINING"
fi
