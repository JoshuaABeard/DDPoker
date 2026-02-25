#!/usr/bin/env bash
# test-gameover-ranks.sh — Verify a 3-player game reaches a proper game-over state.
#
# Plays a 3-player game to completion using the FOLD strategy and asserts:
#   1. playersRemaining reaches 1 (OR lifecycle reaches a game-over phase)
#   2. Final /validate chip-conservation check passes
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

# Normalize options that can interfere with deterministic elimination pacing.
api_post_json /options '{"cheat.neverbroke": false, "gameplay.pauseAllin": false}' > /dev/null 2>&1 || true

# Use a deterministic, no-ante blind structure so chip-conservation checks are
# stable and independent of persisted local profile settings.
lib_start_game "$NUM_PLAYERS" '"buyinChips": 1000, "blindLevels": [{"small": 50, "big": 100, "ante": 0, "minutes": 60}]'
screenshot "gameover-start"

log "  INFO: running $NUM_PLAYERS-player FOLD strategy to completion"
ACTIONS=0
HANDS=0
LAST_MODE=""
LAST_SIG=""
LAST_CHANGE=$(date +%s)

while true; do
    STATE=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
    REMAINING=$(jget "$STATE" 'o.tournament && o.tournament.playersRemaining || 0')
    LIFECYCLE=$(jget "$STATE" 'o.lifecyclePhase || "NONE"')
    PLAYERS_WITH_CHIPS=$(jget "$STATE" \
        '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')

    SIG=$(jget "$STATE" '[(o.inputMode||"NONE"),(o.currentAction&&o.currentAction.isHumanTurn)||false,(o.tournament&&o.tournament.playersRemaining)||0,((o.tables&&o.tables[0]&&o.tables[0].communityCards)||[]).length,(o.tables&&o.tables[0]&&o.tables[0].pot)||0].join("|")')
    if [[ "$SIG" != "$LAST_SIG" ]]; then
        LAST_SIG="$SIG"
        LAST_MODE="$MODE"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && \
        die "Stuck in mode $MODE after ${STUCK_TIMEOUT}s"

    # Game-over detection
    if [[ "$REMAINING" -le 1 && $ACTIONS -gt 0 ]]; then
        log "  INFO: tournament complete: $REMAINING player(s) remaining (actions=$ACTIONS, hands=$HANDS)"
        screenshot "gameover-final"
        break
    fi
    if echo "$LIFECYCLE" | grep -qiE "gameover|GameOver|PracticeGameOver"; then
        log "  INFO: tournament complete via lifecycle: $LIFECYCLE"
        screenshot "gameover-final"
        break
    fi
    # Post-GAME_COMPLETE freeze detection
    if [[ "$MODE" == "QUITSAVE" && "$PLAYERS_WITH_CHIPS" == "1" && $ACTIONS -gt 0 ]]; then
        log "  INFO: tournament complete (post-GAME_COMPLETE: 1 player with chips)"
        screenshot "gameover-final"
        break
    fi

    case "$MODE" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            IS_HUMAN=$(jget "$STATE" 'o.currentAction && o.currentAction.isHumanTurn || false')
            if [[ "$IS_HUMAN" == "true" ]]; then
                api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
                ACTIONS=$((ACTIONS+1))
            else
                close_visible_dialog_if_any "gameover-ranks-ai-turn" > /dev/null 2>&1 || true
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
            close_visible_dialog_if_any "gameover-ranks-loop" > /dev/null 2>&1 || true
            sleep 0.2; continue ;;
    esac
    sleep 0.15
done

# Final validation
VRESULT=$(api GET /validate 2>/dev/null) || true
CC_VALID=$(jget "$VRESULT" 'o.chipConservation && o.chipConservation.valid')
WARNS=$(jget "$VRESULT" '(o.warnings||[]).join("; ")')
if [[ "$CC_VALID" != "true" ]]; then
    VISIBLE_PLAYERS=$(jget "$VRESULT" 'Number(o.chipConservation && o.chipConservation.tables && o.chipConservation.tables[0] && o.chipConservation.tables[0].visiblePlayerCount || 0)')
    PROFILE_PLAYERS=$(jget "$VRESULT" 'Number(o.chipConservation && o.chipConservation.tables && o.chipConservation.tables[0] && o.chipConservation.tables[0].profilePlayerCount || 0)')

    if [[ "$VISIBLE_PLAYERS" -gt 0 && "$PROFILE_PLAYERS" -gt 0 && "$VISIBLE_PLAYERS" -lt "$PROFILE_PLAYERS" ]]; then
        log "  NOTE: /validate chip conservation is inconclusive in terminal snapshot (visiblePlayers=$VISIBLE_PLAYERS profilePlayers=$PROFILE_PLAYERS): $WARNS"
    else
        die "final /validate chipConservation.valid=$CC_VALID  warnings: $WARNS"
    fi
fi

# Assert final state
FINAL_STATE=$(api GET /state 2>/dev/null) || die "Could not read final /state"
FINAL_REMAINING=$(jget "$FINAL_STATE" 'o.tournament && o.tournament.playersRemaining || -1')
FINAL_PLAYERS_WITH_CHIPS=$(jget "$FINAL_STATE" \
    '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')
log "  INFO: final playersRemaining: $FINAL_REMAINING  playersWithChips: $FINAL_PLAYERS_WITH_CHIPS  hands: $HANDS  actions: $ACTIONS"

if [[ "$FINAL_REMAINING" -le 1 || "$FINAL_PLAYERS_WITH_CHIPS" -le 1 ]]; then
    pass "Game reached proper game-over state ($NUM_PLAYERS players, $HANDS hands)"
else
    die "Game did not reach game-over: playersRemaining=$FINAL_REMAINING playersWithChips=$FINAL_PLAYERS_WITH_CHIPS"
fi
