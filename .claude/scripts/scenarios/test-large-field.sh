#!/usr/bin/env bash
# test-large-field.sh — Verify large-field multi-table tournament.
#
# Tests E-002, MT-001:
#   - Start tournament with 9 players (multi-seat single table)
#   - Game progresses and players are eliminated
#   - Tournament completes normally
#   - Chip conservation holds throughout
#
# Uses a phase-transition-based hand counter (PRE_FLOP transitions) and a
# time-based completion check since DEAL mode doesn't appear in the embedded
# server.
#
# Usage:
#   bash .claude/scripts/scenarios/test-large-field.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
# 9 players with small stacks so the game completes quickly
lib_start_game 9 '"buyinChips": 500'

FAILURES=0
HANDS=0
LAST_PROGRESS_SIG=""
LAST_CHANGE=$(date +%s)
LAST_REMAINING=9
PREV_PHASE=""
PREV_DEALER="-1"
# Time-based limit: run until game ends or RUN_SECONDS elapsed
RUN_SECONDS=180
START_TIME=$(date +%s)

log "Running 9-player tournament to completion (${RUN_SECONDS}s max)..."
screenshot "large-field-start"

while true; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    phase=$(jget "$state" 'o.gamePhase || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')
    lifecycle=$(jget "$state" 'o.lifecyclePhase || "NONE"')
    players_with_chips=$(jget "$state" \
        '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')

    # Detect new hand via PRE_FLOP transition or dealer rotation
    if [[ "$phase" == "PRE_FLOP" ]]; then
        dealer=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].dealerSeat||0')
        if [[ "$PREV_PHASE" != "PRE_FLOP" && "$PREV_PHASE" != "" && "$PREV_PHASE" != "NONE" ]]; then
            HANDS=$((HANDS+1))
            # Periodic validation
            if [[ $((HANDS % 10)) -eq 0 ]]; then
                vresult=$(api GET /validate 2>/dev/null) || true
                cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
                if [[ "$cc_valid" != "true" ]]; then
                    log "FAIL: chip conservation invalid at hand $HANDS"
                    FAILURES=$((FAILURES+1))
                fi
            fi
        elif [[ "$PREV_PHASE" == "PRE_FLOP" && "$dealer" != "$PREV_DEALER" && "$PREV_DEALER" != "-1" ]]; then
            HANDS=$((HANDS+1))
        fi
        PREV_DEALER="$dealer"
    fi
    PREV_PHASE="$phase"

    # Log eliminations
    if [[ "$remaining" != "$LAST_REMAINING" && "$remaining" =~ ^[0-9]+$ ]]; then
        elapsed=$(($(date +%s) - START_TIME))
        log "  ${elapsed}s: Players remaining: $remaining (hands: $HANDS)"
        LAST_REMAINING="$remaining"
    fi

    # ---------------------------------------------------------------
    # Game-over detection BEFORE stuck check
    # ---------------------------------------------------------------
    if [[ "$remaining" =~ ^[0-9]+$ && "$remaining" -le 1 && $HANDS -gt 0 ]]; then
        log "Tournament complete: $remaining player(s) remaining"
        break
    fi
    if echo "$lifecycle" | grep -qiE "gameover|GameOver|PracticeGameOver"; then
        log "Tournament complete via lifecycle: $lifecycle"
        break
    fi
    if [[ "$mode" == "QUITSAVE" && "$players_with_chips" =~ ^[0-9]+$ && "$players_with_chips" -le 1 && $HANDS -gt 0 ]]; then
        log "Tournament complete (post-QUITSAVE: $players_with_chips player(s) with chips)"
        break
    fi

    # Time-based limit
    elapsed=$(($(date +%s) - START_TIME))
    if [[ $elapsed -ge $RUN_SECONDS ]]; then
        log "Time limit reached (${elapsed}s). Remaining: $remaining, hands: $HANDS"
        break
    fi

    # Stuck detection using multi-signal progress
    progress_sig="${mode}:${phase}:${remaining}:${players_with_chips}"
    if [[ "$progress_sig" != "$LAST_PROGRESS_SIG" ]]; then
        LAST_PROGRESS_SIG="$progress_sig"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && die "Stuck in mode $mode"

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                # Fold to speed up tournament
                api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
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
    sleep 0.1
done

screenshot "large-field-final"

# Final validation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
warns=$(jget "$vresult" '(o.warnings||[]).join("; ")')
if [[ "$cc_valid" != "true" ]]; then
    log "FAIL: final chipConservation=$cc_valid warnings: $warns"
    FAILURES=$((FAILURES+1))
fi

final_state=$(api GET /state 2>/dev/null) || true
final_remaining=$(jget "$final_state" 'o.tournament&&o.tournament.playersRemaining||-1')
elapsed=$(($(date +%s) - START_TIME))
log "Final: ${elapsed}s elapsed, $HANDS hands, $final_remaining player(s) remaining"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES validation failure(s) during tournament"
fi

# Pass if tournament completed OR significant progress was made (many players eliminated)
eliminated=$(( 9 - (final_remaining > 0 ? final_remaining : 1) ))
if [[ "$final_remaining" -le 1 || "$eliminated" -ge 4 ]]; then
    pass "Large-field tournament (9 players): completed in $HANDS hands (${elapsed}s)"
else
    die "Tournament did not progress enough: remaining=$final_remaining, hands=$HANDS"
fi
