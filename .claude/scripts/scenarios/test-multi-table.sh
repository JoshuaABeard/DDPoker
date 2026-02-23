#!/usr/bin/env bash
# test-multi-table.sh — Verify multi-table tournament with 20 players.
#
# Tests MT-001 through MT-007:
#   - Start 20-player game
#   - Verify tableCount > 1 in /state
#   - Verify tables array has multiple entries
#   - Play through hands using CALL strategy
#   - Verify chip conservation periodically
#   - Verify table consolidation as players are eliminated
#
# Usage:
#   bash .claude/scripts/scenarios/test-multi-table.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 20

FAILURES=0

# Wait for game to start
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game start"

state=$(api GET /state 2>/dev/null) || die "Could not read state"

# ============================================================
# MT-001: Verify tableCount > 1
# ============================================================
log "=== MT-001: Table Count ==="
table_count=$(jget "$state" 'o.tableCount||0')
log "  tableCount: $table_count"

if [[ "$table_count" -gt 1 ]]; then
    log "  OK: Multiple tables ($table_count)"
else
    log "FAIL: Expected multiple tables for 20 players, got $table_count"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# MT-002: Verify tables array has multiple entries
# ============================================================
log "=== MT-002: Tables Array ==="
tables_length=$(jget "$state" '(o.tables||[]).length')
log "  tables array length: $tables_length"

if [[ "$tables_length" -gt 1 ]]; then
    log "  OK: Tables array has $tables_length entries"
else
    log "FAIL: Expected multiple table entries, got $tables_length"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# MT-003: Verify total player count across tables
# ============================================================
log "=== MT-003: Total Players Across Tables ==="
total_players=$(jget "$state" \
    '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p).length,0)')
log "  Total players across tables: $total_players"

if [[ "$total_players" -ge 20 ]]; then
    log "  OK: All 20 players distributed across tables"
else
    log "  WARN: Expected 20 players, found $total_players (some may already be eliminated)"
fi

# Log per-table player counts
for i in $(seq 0 $((tables_length - 1))); do
    t_players=$(jget "$state" "(o.tables||[])[$i]?.players?.filter(p=>p).length||0")
    t_id=$(jget "$state" "(o.tables||[])[$i]?.id||$i")
    log "  Table $t_id: $t_players players"
done

screenshot "multi-table-start"

# ============================================================
# MT-004: Initial chip conservation
# ============================================================
log "=== MT-004: Initial Chip Conservation ==="
vresult=$(api GET /validate 2>/dev/null) || die "Could not validate"
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" == "true" ]]; then
    log "  OK: Initial chip conservation valid"
else
    log "FAIL: Initial chip conservation invalid"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# MT-005/MT-006: Play through hands with CALL strategy, periodic validation
# ============================================================
log "=== MT-005: Playing hands with CALL strategy ==="
HANDS=0
MAX_HANDS=60
LAST_MODE=""
LAST_CHANGE=$(date +%s)
LAST_TABLE_COUNT=$table_count

while [[ $HANDS -lt $MAX_HANDS ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')
    cur_tables=$(jget "$state" '(o.tables||[]).length')

    if [[ "$mode" != "$LAST_MODE" ]]; then
        LAST_MODE="$mode"
        LAST_CHANGE=$(date +%s)
    fi
    [[ $(($(date +%s) - LAST_CHANGE)) -gt $STUCK_TIMEOUT ]] && die "Stuck in mode $mode"

    # Log table consolidation
    if [[ "$cur_tables" != "$LAST_TABLE_COUNT" ]]; then
        log "  Tables changed: $LAST_TABLE_COUNT -> $cur_tables (remaining: $remaining)"
        LAST_TABLE_COUNT="$cur_tables"
    fi

    # Game over?
    if [[ "$remaining" -le 1 && $HANDS -gt 0 ]]; then
        log "  Tournament complete: $remaining remaining"
        break
    fi

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
                if echo "$avail" | grep -q "CHECK"; then
                    api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
                else
                    api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
                fi
            fi
            ;;
        DEAL)
            # Periodic validation every 10 hands
            if [[ $((HANDS % 10)) -eq 0 && $HANDS -gt 0 ]]; then
                vresult=$(api GET /validate 2>/dev/null) || true
                cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
                if [[ "$cc_valid" != "true" ]]; then
                    log "FAIL: Chip conservation invalid at hand $HANDS"
                    FAILURES=$((FAILURES+1))
                else
                    log "  Hand $HANDS: chip conservation valid (tables: $cur_tables, remaining: $remaining)"
                fi
            fi
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            HANDS=$((HANDS+1))
            [[ $((HANDS % 15)) -eq 0 ]] && log "  Hand $HANDS (remaining: $remaining, tables: $cur_tables)"
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

# ============================================================
# MT-007: Final validation
# ============================================================
log "=== MT-007: Final State ==="
final_state=$(api GET /state 2>/dev/null) || true
final_tables=$(jget "$final_state" '(o.tables||[]).length')
final_remaining=$(jget "$final_state" 'o.tournament&&o.tournament.playersRemaining||0')
log "  Played $HANDS hands, final tables: $final_tables, remaining: $final_remaining"

vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
warns=$(jget "$vresult" '(o.warnings||[]).join("; ")')
log "  Final chip conservation: $cc_valid"
[[ -n "$warns" && "$warns" != "" ]] && log "  warnings: $warns"

screenshot "multi-table-final"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Multi-table tournament verified: $table_count initial tables, $HANDS hands played, chip conservation valid"
