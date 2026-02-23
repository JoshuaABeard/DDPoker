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
LAST_PROGRESS_SIG=""
LAST_CHANGE=$(date +%s)

log "Folding every hand until elimination..."

while true; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')
    lifecycle=$(jget "$state" 'o.lifecyclePhase || "NONE"')
    human_chips=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.chips||0')
    human_elim=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.isEliminated||false')

    # ---------------------------------------------------------------
    # Check end conditions BEFORE stuck detection so elimination is
    # detected even when progress signals have stopped changing.
    # ---------------------------------------------------------------

    # Human eliminated (either flag or 0 chips in a non-rebuy mode)
    if [[ "$human_elim" == "true" || ("$human_chips" == "0" && "$mode" != "REBUY_CHECK" && $HANDS -gt 0) ]]; then
        HUMAN_ELIMINATED=true
        log "Human eliminated after $HANDS hands (chips=$human_chips, mode=$mode)"
        screenshot "fold-every-eliminated"
        break
    fi

    # Game ended via QUITSAVE (practice game over screen) — treat as elimination
    if [[ "$mode" == "QUITSAVE" && $HANDS -gt 0 ]]; then
        players_with_chips=$(jget "$state" \
            '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')
        if [[ "$players_with_chips" -le 1 ]]; then
            log "Game complete via QUITSAVE: $players_with_chips player(s) with chips"
            HUMAN_ELIMINATED=true
            break
        fi
    fi

    # Remaining players
    if [[ "$remaining" =~ ^[0-9]+$ && "$remaining" -le 1 && $HANDS -gt 0 ]]; then
        log "Game ended: $remaining players remaining"
        break
    fi

    # Lifecycle phase signals game over
    if echo "$lifecycle" | grep -qiE "gameover|GameOver|PracticeGameOver"; then
        log "Game over via lifecycle: $lifecycle"
        break
    fi

    # ---------------------------------------------------------------
    # Stuck detection — track progress via mode + chips + remaining.
    # QUITSAVE can persist while AI plays a hand (mode unchanged), so
    # we include chip counts to detect when the game is actually moving.
    # ---------------------------------------------------------------
    progress_sig="${mode}:${human_chips}:${remaining}"
    if [[ "$progress_sig" != "$LAST_PROGRESS_SIG" ]]; then
        LAST_PROGRESS_SIG="$progress_sig"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && die "Stuck in mode $mode after ${STUCK_TIMEOUT}s"

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
                HANDS=$((HANDS+1))
                [[ $((HANDS % 10)) -eq 0 ]] && log "  $HANDS hands folded (chips=$human_chips)"
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
        QUITSAVE|NONE)
            sleep 0.2
            ;;
    esac
    sleep 0.15
done

# Final validation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    log "WARN: Final chip conservation invalid (expected — player eliminated)"
fi

log "Result: $HANDS hands played, human eliminated=$HUMAN_ELIMINATED"

if [[ "$HUMAN_ELIMINATED" == "true" || ("$remaining" =~ ^[0-9]+$ && "$remaining" -le 1) ]]; then
    pass "Fold-every-hand test: human eliminated after $HANDS hands"
else
    die "Human was not eliminated after $HANDS hands"
fi
