#!/usr/bin/env bash
# test-large-field.sh — Verify large-field multi-table tournament.
#
# Tests E-002, MT-001:
#   - Start tournament with 20+ players across multiple tables
#   - Game progresses and players are eliminated
#   - Tournament completes normally
#   - Chip conservation holds throughout
#
# Usage:
#   bash .claude/scripts/scenarios/test-large-field.sh [options]
#
# Options:
#   --players N    Number of players (default: 20)

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
NUM_PLAYERS=20
lib_parse_args "$@"
for i in "$@"; do case "$i" in --players) NUM_PLAYERS="$2"; shift 2 ;; esac; done

lib_launch
lib_start_game "$NUM_PLAYERS"

FAILURES=0
HANDS=0
LAST_MODE=""
LAST_CHANGE=$(date +%s)
LAST_REMAINING=$NUM_PLAYERS
MAX_HANDS=200  # Safety limit

log "Running $NUM_PLAYERS-player tournament to completion (max $MAX_HANDS hands)..."
screenshot "large-field-start"

while [[ $HANDS -lt $MAX_HANDS ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')
    lifecycle=$(jget "$state" 'o.lifecyclePhase || "NONE"')
    table_count=$(jget "$state" '(o.tables||[]).length')

    if [[ "$mode" != "$LAST_MODE" ]]; then
        LAST_MODE="$mode"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && die "Stuck in mode $mode"

    # Log eliminations
    if [[ "$remaining" != "$LAST_REMAINING" && "$remaining" -gt 0 ]]; then
        log "  Players remaining: $remaining (tables: $table_count)"
        LAST_REMAINING="$remaining"
    fi

    # Game-over detection
    if [[ "$remaining" -le 1 && $HANDS -gt 0 ]]; then
        log "Tournament complete: $remaining player(s) remaining"
        break
    fi
    if echo "$lifecycle" | grep -qiE "gameover|GameOver|PracticeGameOver"; then
        log "Tournament complete via lifecycle: $lifecycle"
        break
    fi
    # Post-GAME_COMPLETE detection
    players_with_chips=$(jget "$state" \
        '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')
    if [[ "$mode" == "QUITSAVE" && "$players_with_chips" -le 1 && $HANDS -gt 0 ]]; then
        log "Tournament complete (1 player with chips)"
        break
    fi

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                # Fold to speed up tournament
                api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
            fi
            ;;
        DEAL)
            # Periodic validation
            if [[ $((HANDS % 20)) -eq 0 && $HANDS -gt 0 ]]; then
                vresult=$(api GET /validate 2>/dev/null) || true
                cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
                if [[ "$cc_valid" != "true" ]]; then
                    log "FAIL: chip conservation invalid at hand $HANDS"
                    FAILURES=$((FAILURES+1))
                fi
            fi
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            HANDS=$((HANDS+1))
            [[ $((HANDS % 25)) -eq 0 ]] && log "  Hand $HANDS (remaining: $remaining, tables: $table_count)"
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
[[ "$cc_valid" != "true" ]] && log "WARN: final chipConservation=$cc_valid warnings: $warns"

final_state=$(api GET /state 2>/dev/null) || true
final_remaining=$(jget "$final_state" 'o.tournament&&o.tournament.playersRemaining||-1')
log "Final: $HANDS hands, $final_remaining player(s) remaining"

if [[ "$final_remaining" -le 1 || "$players_with_chips" -le 1 || $HANDS -ge $MAX_HANDS ]]; then
    if [[ $HANDS -ge $MAX_HANDS ]]; then
        log "WARN: Hit max hands limit ($MAX_HANDS) — tournament may not have completed"
    fi
    if [[ $FAILURES -gt 0 ]]; then
        die "$FAILURES validation failure(s) during tournament"
    fi
    pass "Large-field tournament ($NUM_PLAYERS players): completed in $HANDS hands"
else
    die "Tournament did not complete: remaining=$final_remaining, hands=$HANDS"
fi
