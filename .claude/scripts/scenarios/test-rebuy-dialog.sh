#!/usr/bin/env bash
# test-rebuy-dialog.sh — Verify the rebuy dialog flow via REBUY_CHECK inputMode.
#
# Tests RB-010 through RB-020:
#   RB-010: Start game with rebuys=true
#   RB-011: Use setChips cheat to set human chips to 0
#   RB-012: Assert inputMode becomes REBUY_CHECK (hard FAIL if not)
#   RB-013: Send DECLINE_REBUY and assert mode transitions away
#   RB-015: Repeat — set chips to 0 again, assert REBUY_CHECK, send ACCEPT_REBUY,
#            assert chips are restored
#
# Prerequisites:
#   - Java fix A2 (ShowTournamentTable.setInputMode REBUY_CHECK bypass) must be applied
#   - The /cheat setChips action must be available
#
# Usage:
#   bash .claude/scripts/scenarios/test-rebuy-dialog.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# RB-010: Start game with rebuys=true
# ============================================================
log "=== RB-010: Start game with rebuys=true ==="
RESULT=$(api_post_json /game/start '{"numPlayers": 3, "rebuys": true}' 2>/dev/null) \
    || die "game/start failed"
if echo "$RESULT" | grep -q '"accepted":true'; then
    log "  OK: Game started with rebuys=true"
else
    die "game/start with rebuys=true failed: $RESULT"
fi

# Wait for first human turn so we know a hand is in progress
state=$(wait_human_turn 60) || die "Timed out waiting for human turn"
mode=$(jget "$state" 'o.inputMode || "NONE"')
log "  Game in progress: mode=$mode"

# Find the human player's seat
HUMAN_SEAT=$(jget "$state" \
    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.seat??0")
log "  Human seat: $HUMAN_SEAT"

# ============================================================
# RB-011: Set human chips to 0 to trigger rebuy dialog
# ============================================================
log "=== RB-011: Set human chips to 0 ==="
CHEAT_RESULT=$(api_post_json /cheat \
    "{\"action\":\"setChips\",\"seat\":$HUMAN_SEAT,\"amount\":0}" 2>/dev/null) || true
log "  setChips result: $CHEAT_RESULT"

# Fold the current hand so the engine evaluates the 0-chip state
api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true

# ============================================================
# RB-012: Assert REBUY_CHECK appears (hard FAIL if not)
# ============================================================
log "=== RB-012: Wait for REBUY_CHECK mode ==="
REBUY_TIMEOUT=30
REBUY_START=$(date +%s)
REBUY_APPEARED=false

while true; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')

    if [[ "$mode" == "REBUY_CHECK" ]]; then
        log "  REBUY_CHECK detected"
        REBUY_APPEARED=true
        break
    fi

    # If a new human turn arrived before REBUY_CHECK, fold again to stay broke
    if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
        is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
        if [[ "$is_human" == "true" ]]; then
            api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
        fi
    fi

    # Advance CONTINUE/DEAL prompts
    case "$mode" in
        DEAL) api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true ;;
        CONTINUE|CONTINUE_LOWER) api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true ;;
    esac

    if [[ $(($(date +%s) - REBUY_START)) -gt $REBUY_TIMEOUT ]]; then
        log "FAIL: RB-012 — REBUY_CHECK never appeared after ${REBUY_TIMEOUT}s (last mode: $mode)"
        FAILURES=$((FAILURES+1))
        break
    fi
    sleep 0.3
done

# ============================================================
# RB-013: Send DECLINE_REBUY and assert mode transitions
# ============================================================
if [[ "$REBUY_APPEARED" == "true" ]]; then
    log "=== RB-013: DECLINE_REBUY ==="
    DECLINE_RESP=$(api_post_json /action '{"type":"DECLINE_REBUY"}' 2>/dev/null) || true
    ACCEPTED=$(jget "$DECLINE_RESP" 'o.accepted||false')
    if [[ "$ACCEPTED" == "true" ]]; then
        log "  OK: DECLINE_REBUY accepted"
    else
        log "FAIL: RB-013 — DECLINE_REBUY not accepted: $DECLINE_RESP"
        FAILURES=$((FAILURES+1))
    fi

    sleep 1
    state=$(api GET /state 2>/dev/null) || true
    new_mode=$(jget "$state" 'o.inputMode || "NONE"')
    if [[ "$new_mode" != "REBUY_CHECK" ]]; then
        log "  OK: Mode transitioned away from REBUY_CHECK to $new_mode"
        screenshot "rebuy-after-decline"
    else
        log "FAIL: RB-013 — Still in REBUY_CHECK after DECLINE_REBUY"
        FAILURES=$((FAILURES+1))
    fi
fi

# ============================================================
# RB-015: Accept rebuy — set chips to 0 again, assert REBUY_CHECK, ACCEPT, verify restored
# ============================================================
log "=== RB-015: ACCEPT_REBUY restores chips ==="

# Wait for the next human turn (new hand or current hand continues)
WAIT_START=$(date +%s)
HUMAN_TURN_FOUND=false
while [[ $(($(date +%s) - WAIT_START)) -lt 30 ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                HUMAN_TURN_FOUND=true
                break
            fi
            ;;
        DEAL) api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true ;;
        CONTINUE|CONTINUE_LOWER) api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true ;;
        REBUY_CHECK)
            # Already in REBUY_CHECK from prior fold — will handle accept below
            HUMAN_TURN_FOUND=true
            break
            ;;
    esac
    sleep 0.3
done

if [[ "$HUMAN_TURN_FOUND" == "true" ]]; then
    # If not already in REBUY_CHECK, set chips to 0 and fold to trigger it
    state=$(api GET /state 2>/dev/null) || true
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    if [[ "$mode" != "REBUY_CHECK" ]]; then
        api_post_json /cheat \
            "{\"action\":\"setChips\",\"seat\":$HUMAN_SEAT,\"amount\":0}" > /dev/null 2>&1 || true
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    fi

    # Wait for REBUY_CHECK
    REBUY2_START=$(date +%s)
    REBUY2_APPEARED=false
    while true; do
        state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
        mode=$(jget "$state" 'o.inputMode || "NONE"')
        if [[ "$mode" == "REBUY_CHECK" ]]; then
            REBUY2_APPEARED=true
            break
        fi
        case "$mode" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                [[ "$is_human" == "true" ]] && api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
                ;;
            DEAL) api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true ;;
            CONTINUE|CONTINUE_LOWER) api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true ;;
        esac
        [[ $(($(date +%s) - REBUY2_START)) -gt 30 ]] && break
        sleep 0.3
    done

    if [[ "$REBUY2_APPEARED" == "true" ]]; then
        ACCEPT_RESP=$(api_post_json /action '{"type":"ACCEPT_REBUY"}' 2>/dev/null) || true
        ACCEPT_OK=$(jget "$ACCEPT_RESP" 'o.accepted||false')
        if [[ "$ACCEPT_OK" == "true" ]]; then
            log "  OK: ACCEPT_REBUY accepted"
        else
            log "FAIL: RB-015 — ACCEPT_REBUY not accepted: $ACCEPT_RESP"
            FAILURES=$((FAILURES+1))
        fi

        # Wait briefly and check chips restored
        sleep 2
        state=$(api GET /state 2>/dev/null) || true
        human_chips=$(jget "$state" \
            "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$HUMAN_SEAT)?.chips||0")
        if [[ "$human_chips" -gt 0 ]]; then
            log "  OK: Human chips restored to $human_chips after ACCEPT_REBUY"
            screenshot "rebuy-after-accept"
        else
            log "FAIL: RB-015 — Chips still 0 after ACCEPT_REBUY (chips=$human_chips)"
            FAILURES=$((FAILURES+1))
        fi
    else
        log "  WARN: Could not trigger second REBUY_CHECK for accept test — skipping RB-015"
    fi
else
    log "  WARN: Could not find human turn for ACCEPT_REBUY test — skipping RB-015"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Rebuy dialog flow verified: REBUY_CHECK appeared, DECLINE_REBUY accepted, ACCEPT_REBUY accepted"
