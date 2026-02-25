#!/usr/bin/env bash
# test-rebuy-addon.sh — Verify rebuy and add-on game configuration.
#
# Tests RB-001, RB-004, RB-005, RB-006:
#   RB-001: Start game with rebuys=true succeeds
#   RB-004: Chip conservation holds during play with rebuy profile
#   RB-005: Neverbroke fires and restores chips (separate game, no rebuys)
#   RB-006: Start game with addons=true succeeds
#
# RB-002/RB-003 rebuy decision behavior is covered by test-rebuy-dialog.sh.
# This scenario focuses on configuration, conservation, and neverbroke fallback
# while dismissing incidental dialogs via /ui/dialogs so execution stays unblocked.
#
# Usage:
#   bash .claude/scripts/scenarios/test-rebuy-addon.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# RB-001: Start game with rebuys=true
# ============================================================
log "=== RB-001: Start game with rebuys=true ==="
RESULT=$(api_post_json /game/start '{"numPlayers":3,"rebuys":true}' 2>/dev/null) || die "game/start failed"
if echo "$RESULT" | grep -q '"accepted":true'; then
    log "  OK: Game started with rebuys=true"
else
    log "FAIL: game/start with rebuys=true failed: $RESULT"
    FAILURES=$((FAILURES+1))
fi

# Wait for hand to start and advance through setup
START_WAIT=$(date +%s)
FIRST_MODE=""
while [[ $(($(date +%s) - START_WAIT)) -lt 30 ]]; do
    sleep 0.5
    STATE=$(api GET /state 2>/dev/null) || continue
    MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
    close_visible_dialog_if_any "rebuy-start-wait" > /dev/null 2>&1 || true
    case "$MODE" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL|CONTINUE|CONTINUE_LOWER)
            FIRST_MODE="$MODE"
            break ;;
    esac
done
[[ -n "$FIRST_MODE" ]] || die "Game did not start within 30s (mode=$FIRST_MODE)"
log "  OK: Game in progress (mode=$FIRST_MODE)"
screenshot "rebuy-start"

# ============================================================
# RB-004: Chip conservation holds at start
# ============================================================
log "=== RB-004: Chip conservation with rebuy profile ==="
VRESULT=$(api GET /validate 2>/dev/null) || true
CC_VALID=$(jget "$VRESULT" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$CC_VALID" == "true" ]]; then
    log "  OK: Chip conservation valid"
else
    log "FAIL: Chip conservation invalid at rebuy profile start"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# RB-005: Neverbroke fires when broke (fallback when rebuys not in window)
# Enable neverbroke in a separate game without rebuys for deterministic behavior.
# ============================================================
log "=== RB-005: Neverbroke fallback (separate game, no rebuys) ==="

# Start a fresh game without rebuys (so neverbroke fires cleanly)
RESULT2=$(api_post_json /game/start '{"numPlayers":3}' 2>/dev/null) || die "second game/start failed"
if echo "$RESULT2" | grep -q '"accepted":true'; then
    log "  OK: Second game started (no rebuys)"
else
    log "FAIL: Second game start failed: $RESULT2"
    FAILURES=$((FAILURES+1))
fi

# Enable neverbroke
api_post_json /options '{"cheat.neverbroke": true}' > /dev/null
NB=$(jget "$(api GET /options 2>/dev/null)" 'o.cheat && o.cheat.neverbroke')
[[ "$NB" == "true" ]] || die "neverbroke did not activate"
log "  OK: neverbroke enabled"

# Wait for human turn
HUMAN_SEAT=0
while true; do
    STATE=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL|CONTINUE|CONTINUE_LOWER" 60) \
        || die "Timed out waiting for human turn"
    MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
    case "$MODE" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            IS_HUMAN=$(jget "$STATE" 'o.currentAction && o.currentAction.isHumanTurn || false')
            if [[ "$IS_HUMAN" == "true" ]]; then
                HUMAN_SEAT=$(jget "$STATE" 'o.currentAction && o.currentAction.humanSeat || 0')
                break
            fi
            sleep 0.3 ;;
        DEAL|CONTINUE|CONTINUE_LOWER)
            advance_non_betting "$STATE"
            sleep 0.2 ;;
        *)
            close_visible_dialog_if_any "rebuy-neverbroke-turn-wait" > /dev/null 2>&1 || true
            sleep 0.3 ;;
    esac
done
log "  Human at seat $HUMAN_SEAT, mode=$MODE"

# Set chips to 0 and fold
api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$HUMAN_SEAT,\"amount\":0}" > /dev/null 2>&1 || true
api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true

# Poll for chips restored (dismiss any incidental dialogs while polling)
POLL_START=$(date +%s)
HUMAN_CHIPS=0
while [[ $(($(date +%s) - POLL_START)) -lt 30 ]]; do
    sleep 0.5
    STATE=$(api GET /state 2>/dev/null) || continue
    HUMAN_CHIPS=$(jget "$STATE" \
        "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$HUMAN_SEAT)?.chips||0")
    [[ "$HUMAN_CHIPS" -gt 0 ]] && break
    close_visible_dialog_if_any "rebuy-neverbroke-poll" > /dev/null 2>&1 || true
done

drain_visible_dialogs "rebuy-neverbroke-post-check" 6 0.2

if [[ "$HUMAN_CHIPS" -gt 0 ]]; then
    log "  OK: Neverbroke restored chips to $HUMAN_CHIPS"
else
    log "FAIL: Chips still 0 after 30s — neverbroke did not fire"
    FAILURES=$((FAILURES+1))
fi
screenshot "rebuy-neverbroke"

# ============================================================
# RB-006: Start game with addons=true
# ============================================================
log "=== RB-006: Start game with addons=true ==="
RESULT3=$(api_post_json /game/start '{"numPlayers":3,"addons":true}' 2>/dev/null) || die "addons game/start failed"
if echo "$RESULT3" | grep -q '"accepted":true'; then
    log "  OK: Game started with addons=true"
else
    log "FAIL: game/start with addons=true failed: $RESULT3"
    FAILURES=$((FAILURES+1))
fi

sleep 2
STATE3=$(api GET /state 2>/dev/null) || true
MODE3=$(jget "$STATE3" 'o.inputMode || "NONE"')
drain_visible_dialogs "addons-start" 6 0.2
log "  Game state after addons start: mode=$MODE3"
screenshot "rebuy-addon-start"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Rebuy/add-on configuration verified: rebuys=true accepted, addons=true accepted, neverbroke fallback works"
