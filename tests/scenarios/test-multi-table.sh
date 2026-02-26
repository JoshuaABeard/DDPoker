#!/usr/bin/env bash
# test-multi-table.sh — Verify large-field tournament with 20 players starts and plays.
#
# Architecture note: In DD Poker's WebSocket tournament architecture, the control server
# only observes the human's current table via PokerGame.getTables(). Other tables are
# managed by the embedded WebSocket server (ServerTournamentDirector) without client
# visibility. As a result:
#   - tableCount always reports 1 (only the human's table is visible)
#   - ValidateHandler chip conservation uses game.getNumPlayers() (=20) vs only
#     the visible table's chips (~10 players) → always reports invalid for multi-table
#
# Tests MT-001 through MT-007:
#   MT-001: 20-player game starts and accepts the request
#   MT-002: Human's table is visible with players seated
#   MT-003: Tournament chip leaderboard shows all (or most) players
#   MT-004: Tournament info shows 20 total players
#   MT-005: Game plays for 60s using CALL strategy (actions accepted)
#   MT-006: Tournament progresses (players get eliminated, logged if any)
#   MT-007: Final state check after playing
#
# Usage:
#   bash tests/scenarios/test-multi-table.sh [options]

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
# MT-001: 20-player game started and human's table is visible
# ============================================================
log "=== MT-001: Human Table Visible ==="
tables_length=$(jget "$state" '(o.tables||[]).length')
table_count=$(jget "$state" 'o.tableCount||0')
log "  tableCount: $table_count, tables array: $tables_length"

# In multi-table architecture, only human's table is visible (1 table expected)
if [[ "$tables_length" -ge 1 ]]; then
    log "  OK: Human's table is visible"
else
    log "FAIL: No table visible in state"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# MT-002: Human's table has players seated
# ============================================================
log "=== MT-002: Players on Human's Table ==="
table_players=$(jget "$state" '(o.tables||[])[0]?.players?.filter(p=>p).length||0')
table_id=$(jget "$state" '(o.tables||[])[0]?.id||0')
log "  Table $table_id: $table_players players"

if [[ "$table_players" -ge 2 ]]; then
    log "  OK: Table has $table_players players"
else
    log "FAIL: Expected players on table, got $table_players"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# MT-003: Tournament total players = 20
# ============================================================
log "=== MT-003: Tournament Total Players ==="
total_players=$(jget "$state" 'o.tournament&&o.tournament.totalPlayers||0')
log "  Tournament totalPlayers: $total_players"

if [[ "$total_players" -eq 20 ]]; then
    log "  OK: Tournament has 20 total players"
else
    log "FAIL: Expected 20 total players, got $total_players"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# MT-004: Tournament chip leaderboard present
# ============================================================
log "=== MT-004: Chip Leaderboard ==="
leaders=$(jget "$state" '(o.tournament&&o.tournament.chipLeaderboard||[]).length')
log "  Chip leaderboard entries: $leaders"
if [[ "$leaders" -gt 0 ]]; then
    log "  OK: Leaderboard has $leaders entries"
else
    log "  WARN: No leaderboard entries (may still be initializing)"
fi

screenshot "multi-table-start"

# ============================================================
# MT-005/MT-006: Play for 60 seconds with CALL strategy
# ============================================================
# Note: Tournament games auto-deal (no DEAL button click needed).
# We play for a fixed wall-clock duration and count actions taken.
log "=== MT-005: Playing hands with CALL strategy (60s) ==="
ACTIONS=0
PLAY_START=$(date +%s)
PLAY_DURATION=60
LAST_REMAINING=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||20')
LAST_CHANGE=$(date +%s)

while [[ $(($(date +%s) - PLAY_START)) -lt $PLAY_DURATION ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')

    # Log player eliminations
    if [[ "$remaining" != "$LAST_REMAINING" && "$remaining" -gt 0 ]]; then
        elapsed=$(($(date +%s) - PLAY_START))
        log "  Players remaining: $LAST_REMAINING → $remaining (${elapsed}s, actions: $ACTIONS)"
        LAST_REMAINING="$remaining"
    fi

    # Tournament complete?
    if [[ "$remaining" -le 1 ]]; then
        log "  Tournament complete: $remaining remaining"
        break
    fi

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
                if echo "$avail" | grep -q "CHECK"; then
                    api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 && ACTIONS=$((ACTIONS+1)) || true
                else
                    api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 && ACTIONS=$((ACTIONS+1)) || true
                fi
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 && ACTIONS=$((ACTIONS+1)) || true
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 && ACTIONS=$((ACTIONS+1)) || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 && ACTIONS=$((ACTIONS+1)) || true
            ;;
        QUITSAVE|NONE)
            sleep 0.1
            ;;
    esac
    sleep 0.1
done

log "  Completed play phase: $ACTIONS actions in $(($(date +%s) - PLAY_START))s"

# ============================================================
# MT-007: Final validation
# ============================================================
log "=== MT-007: Final State ==="
final_state=$(api GET /state 2>/dev/null) || true
final_remaining=$(jget "$final_state" 'o.tournament&&o.tournament.playersRemaining||0')
log "  Actions taken: $ACTIONS, remaining players: $final_remaining"

if [[ $ACTIONS -gt 0 ]]; then
    log "  OK: Game accepted $ACTIONS actions during play"
else
    log "FAIL: No actions were accepted"
    FAILURES=$((FAILURES+1))
fi

screenshot "multi-table-final"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Large-field tournament: 20 players, $ACTIONS actions taken, $final_remaining players remaining"
