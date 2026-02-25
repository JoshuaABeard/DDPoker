#!/usr/bin/env bash
# test-dashboard-customization.sh — Verify dashboard customization actions and persistence.
#
# Asserts end-to-end dashboard customization via /ui/dashboard:
#   - hide/show an item
#   - reorder an item
#   - verify changed layout survives app restart
#
# Usage:
#   bash .claude/scripts/scenarios/test-dashboard-customization.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

log "Starting game for dashboard customization checks..."
lib_start_game 4
wait_human_turn 60 >/dev/null || die "Timed out waiting for human turn"

SNAP0=""
PRESENT0="false"
for _ in {1..30}; do
    SNAP0=$(api GET /ui/dashboard 2>/dev/null || true)
    PRESENT0=$(jget "$SNAP0" 'o.present || false')
    if [[ "$PRESENT0" == "true" ]]; then
        break
    fi
    sleep 0.25
done
[[ "$PRESENT0" == "true" ]] || die "Dashboard snapshot not present"

MOVE_NAME=$(jget "$SNAP0" '(o.items||[]).find(i=>i&&i.displayed&&Number.isInteger(i.position)&&i.position>0)?.name||""')
[[ -n "$MOVE_NAME" ]] || die "Could not find movable displayed dashboard item"

ORIG_POS=$(jget "$SNAP0" "(o.items||[]).find(i=>i&&i.name==='${MOVE_NAME}')?.position")
ORIG_DISPLAYED=$(jget "$SNAP0" "(o.items||[]).find(i=>i&&i.name==='${MOVE_NAME}')?.displayed")
DISPLAYED_COUNT0=$(jget "$SNAP0" 'o.displayedCount || 0')

if ! [[ "$ORIG_POS" =~ ^[0-9]+$ ]]; then
    die "Invalid original position for $MOVE_NAME: $ORIG_POS"
fi

log "Using dashboard item '$MOVE_NAME' (original position=$ORIG_POS)"

log "Hiding item via /ui/dashboard..."
HIDE_RESP=$(api_post_json /ui/dashboard "{\"action\":\"SET_DISPLAYED\",\"name\":\"$MOVE_NAME\",\"displayed\":false}" 2>/dev/null || true)
assert_action_accepted "$HIDE_RESP" "hide dashboard item"

SNAP1=$(api GET /ui/dashboard 2>/dev/null || true)
DISPLAYED1=$(jget "$SNAP1" "(o.items||[]).find(i=>i&&i.name==='${MOVE_NAME}')?.displayed")
[[ "$DISPLAYED1" == "false" ]] || die "Expected $MOVE_NAME to be hidden"

DISPLAYED_COUNT1=$(jget "$SNAP1" 'o.displayedCount || 0')
if [[ "$ORIG_DISPLAYED" == "true" ]] && [[ "$DISPLAYED_COUNT0" =~ ^[0-9]+$ ]] && [[ "$DISPLAYED_COUNT1" =~ ^[0-9]+$ ]]; then
    EXPECTED_COUNT1=$((DISPLAYED_COUNT0 - 1))
    [[ "$DISPLAYED_COUNT1" -eq "$EXPECTED_COUNT1" ]] || die "Displayed count did not decrease after hide"
fi

log "Showing item via /ui/dashboard..."
SHOW_RESP=$(api_post_json /ui/dashboard "{\"action\":\"SET_DISPLAYED\",\"name\":\"$MOVE_NAME\",\"displayed\":true}" 2>/dev/null || true)
assert_action_accepted "$SHOW_RESP" "show dashboard item"

SNAP2=$(api GET /ui/dashboard 2>/dev/null || true)
DISPLAYED2=$(jget "$SNAP2" "(o.items||[]).find(i=>i&&i.name==='${MOVE_NAME}')?.displayed")
[[ "$DISPLAYED2" == "true" ]] || die "Expected $MOVE_NAME to be visible"

POS_BEFORE_MOVE=$(jget "$SNAP2" "(o.items||[]).find(i=>i&&i.name==='${MOVE_NAME}')?.position")
if ! [[ "$POS_BEFORE_MOVE" =~ ^[0-9]+$ ]]; then
    die "Invalid position before move for $MOVE_NAME: $POS_BEFORE_MOVE"
fi
[[ "$POS_BEFORE_MOVE" -gt 0 ]] || die "Chosen item is not movable upward"

log "Moving item up by one position..."
MOVE_RESP=$(api_post_json /ui/dashboard "{\"action\":\"MOVE\",\"name\":\"$MOVE_NAME\",\"direction\":\"UP\",\"steps\":1}" 2>/dev/null || true)
assert_action_accepted "$MOVE_RESP" "move dashboard item"

SNAP3=$(api GET /ui/dashboard 2>/dev/null || true)
POS_AFTER_MOVE=$(jget "$SNAP3" "(o.items||[]).find(i=>i&&i.name==='${MOVE_NAME}')?.position")
if ! [[ "$POS_AFTER_MOVE" =~ ^[0-9]+$ ]]; then
    die "Invalid position after move for $MOVE_NAME: $POS_AFTER_MOVE"
fi
EXPECTED_AFTER_MOVE=$((POS_BEFORE_MOVE - 1))
[[ "$POS_AFTER_MOVE" -eq "$EXPECTED_AFTER_MOVE" ]] || die "Move did not change position as expected"

log "Restarting app to verify dashboard persistence..."
cleanup
JAVA_PID=""
SKIP_BUILD=true
SKIP_LAUNCH=false
lib_launch
lib_start_game 4
wait_human_turn 60 >/dev/null || die "Timed out waiting for human turn after restart"

SNAP_RESTART=$(api GET /ui/dashboard 2>/dev/null || true)
PRESENT_RESTART=$(jget "$SNAP_RESTART" 'o.present || false')
[[ "$PRESENT_RESTART" == "true" ]] || die "Dashboard not present after restart"

DISPLAYED_RESTART=$(jget "$SNAP_RESTART" "(o.items||[]).find(i=>i&&i.name==='${MOVE_NAME}')?.displayed")
POS_RESTART=$(jget "$SNAP_RESTART" "(o.items||[]).find(i=>i&&i.name==='${MOVE_NAME}')?.position")

[[ "$DISPLAYED_RESTART" == "true" ]] || die "Expected $MOVE_NAME to remain visible after restart"
[[ "$POS_RESTART" =~ ^[0-9]+$ ]] || die "Invalid restart position for $MOVE_NAME: $POS_RESTART"
[[ "$POS_RESTART" -eq "$EXPECTED_AFTER_MOVE" ]] || die "Expected persisted position $EXPECTED_AFTER_MOVE, got $POS_RESTART"

log "Restoring original dashboard position/display for '$MOVE_NAME'..."
if [[ "$POS_RESTART" -gt "$ORIG_POS" ]]; then
    STEPS=$((POS_RESTART - ORIG_POS))
    api_post_json /ui/dashboard "{\"action\":\"MOVE\",\"name\":\"$MOVE_NAME\",\"direction\":\"UP\",\"steps\":$STEPS}" >/dev/null 2>&1 || true
elif [[ "$POS_RESTART" -lt "$ORIG_POS" ]]; then
    STEPS=$((ORIG_POS - POS_RESTART))
    api_post_json /ui/dashboard "{\"action\":\"MOVE\",\"name\":\"$MOVE_NAME\",\"direction\":\"DOWN\",\"steps\":$STEPS}" >/dev/null 2>&1 || true
fi

if [[ "$ORIG_DISPLAYED" == "false" ]]; then
    api_post_json /ui/dashboard "{\"action\":\"SET_DISPLAYED\",\"name\":\"$MOVE_NAME\",\"displayed\":false}" >/dev/null 2>&1 || true
fi

pass "Dashboard customization verified (hide/show, reorder, restart persistence) for '$MOVE_NAME'"
