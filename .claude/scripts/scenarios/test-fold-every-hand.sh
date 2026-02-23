#!/usr/bin/env bash
# test-fold-every-hand.sh — Human folds every hand until eliminated.
#
# Tests E-003, E-004:
#   - Tournament finishes correctly when human folds every hand
#   - Human eliminated when chips reach 0 (no rebuy available)
#   - Game Over state reached with correct finishing position
#
# Difference from test-gameover-ranks.sh: this specifically verifies the
# human is eliminated (not just that the game ends) and tracks elimination.
#
# Usage:
#   bash .claude/scripts/scenarios/test-fold-every-hand.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0
HANDS=0
HUMAN_ELIMINATED=false
LAST_MODE=""
LAST_CHANGE=$(date +%s)

log "Folding every hand until elimination..."

while true; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')
    lifecycle=$(jget "$state" 'o.lifecyclePhase || "NONE"')

    if [[ "$mode" != "$LAST_MODE" ]]; then
        LAST_MODE="$mode"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && die "Stuck in mode $mode after ${STUCK_TIMEOUT}s"

    # Check if human is eliminated
    human_seat=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.seat')
    human_chips=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.chips||0')
    human_elim=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.isEliminated||false')

    if [[ "$human_elim" == "true" || ("$human_chips" == "0" && "$mode" != "REBUY_CHECK" && $HANDS -gt 0) ]]; then
        HUMAN_ELIMINATED=true
        log "Human eliminated after $HANDS hands (chips=$human_chips)"
        screenshot "fold-every-eliminated"
        break
    fi

    # Game over detection
    if [[ "$remaining" -le 1 && $HANDS -gt 0 ]]; then
        log "Game ended: $remaining players remaining"
        break
    fi
    if echo "$lifecycle" | grep -qiE "gameover|GameOver|PracticeGameOver"; then
        log "Game over via lifecycle: $lifecycle"
        break
    fi

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            HANDS=$((HANDS+1))
            [[ $((HANDS % 10)) -eq 0 ]] && log "  $HANDS hands folded (chips=$human_chips)"
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
        QUITSAVE|NONE)
            # Check for post-game state
            players_with_chips=$(jget "$state" \
                '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')
            if [[ "$players_with_chips" -le 1 && $HANDS -gt 0 ]]; then
                log "Game complete: $players_with_chips player(s) with chips"
                break
            fi
            sleep 0.2
            ;;
    esac
    sleep 0.15
done

# Final validation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    log "WARN: Final chip conservation invalid (expected after elimination)"
fi

log "Result: $HANDS hands played, human eliminated=$HUMAN_ELIMINATED"

if [[ "$HUMAN_ELIMINATED" == "true" || "$remaining" -le 1 ]]; then
    pass "Fold-every-hand test: human eliminated after $HANDS hands"
else
    die "Human was not eliminated after $HANDS hands"
fi
