#!/usr/bin/env bash
# test-rebuy-dialog.sh — Verify the rebuy dialog flow via REBUY_CHECK inputMode.
#
# Tests RB-010 through RB-020:
#   RB-010: Start game with rebuys=true, very small buyinChips + high blinds
#   RB-012: Keep folding until human goes bust; assert REBUY_CHECK appears
#   RB-013: Send DECLINE_REBUY and assert mode transitions away
#   RB-015: Repeat — bust again, assert REBUY_CHECK, send REBUY, assert chips restored
#
# Design:
#   In online practice mode the server tracks chip counts and sends REBUY_OFFERED
#   when the human player runs out of chips at the end of a hand. The client-side
#   setChips cheat is local-only and does not notify the server, so using it cannot
#   trigger REBUY_OFFERED. Instead we start the game with a very small buyin
#   (150 chips) against blinds of 50/100 so the human is forced all-in within a
#   few hands and naturally goes bust.
#
# Usage:
#   bash tests/scenarios/test-rebuy-dialog.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# RB-010: Start game with rebuys=true and tiny stack so human busts quickly
# ============================================================
log "=== RB-010: Start game with tiny stack + rebuys=true ==="
RESULT=$(api_post_json /game/start '{
    "numPlayers": 2,
    "buyinChips": 500,
    "rebuys": true,
    "blindLevels": [{"small": 50, "big": 100, "ante": 0, "minutes": 60}]
}' 2>/dev/null) || die "game/start failed"
if echo "$RESULT" | grep -q '"accepted":true'; then
    log "  OK: Game started (buyinChips=500, blinds=50/100, rebuys=true)"
else
    die "game/start failed: $RESULT"
fi

# ============================================================
# RB-012: Keep folding until REBUY_CHECK appears (human busts naturally)
# ============================================================
log "=== RB-012: Fold every hand until bust → REBUY_CHECK ==="
REBUY_TIMEOUT=120
REBUY_START=$(date +%s)
REBUY_APPEARED=false

while true; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')

    if [[ "$mode" == "REBUY_CHECK" ]]; then
        log "  INFO: REBUY_CHECK detected; human went bust"
        REBUY_APPEARED=true
        break
    fi

    # Fold every betting turn to drain chips through blinds
    if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
        is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
        if [[ "$is_human" == "true" ]]; then
            api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
        fi
    else
        case "$mode" in
            DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                advance_non_betting "$state"
                ;;
        esac
    fi

    if [[ $(($(date +%s) - REBUY_START)) -gt $REBUY_TIMEOUT ]]; then
        log "FAIL: RB-012 — REBUY_CHECK never appeared after ${REBUY_TIMEOUT}s (last mode: $mode)"
        FAILURES=$((FAILURES+1))
        break
    fi
    sleep 0.3
done

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

# ============================================================
# RB-013: Send DECLINE_REBUY and assert mode transitions
# ============================================================
log "=== RB-013: DECLINE_REBUY ==="
DECLINE_RESP=$(api_post_json /action '{"type":"DECLINE_REBUY"}' 2>/dev/null) || true
ACCEPTED=$(jget "$DECLINE_RESP" 'o.accepted||false')
if [[ "$ACCEPTED" == "true" ]]; then
    log "  OK: DECLINE_REBUY accepted"
else
    log "FAIL: RB-013 — DECLINE_REBUY not accepted: $DECLINE_RESP"
    FAILURES=$((FAILURES+1))
fi

sleep 2
state=$(api GET /state 2>/dev/null) || true
new_mode=$(jget "$state" 'o.inputMode || "NONE"')
if [[ "$new_mode" != "REBUY_CHECK" ]]; then
    log "  OK: Mode transitioned away from REBUY_CHECK to $new_mode"
    screenshot "rebuy-after-decline"
else
    log "FAIL: RB-013 — Still in REBUY_CHECK after DECLINE_REBUY"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# RB-015: Start fresh game, bust again, accept rebuy, verify chips restored
# ============================================================
log "=== RB-015: Accept rebuy — verify chips restored ==="
RESULT=$(api_post_json /game/start '{
    "numPlayers": 2,
    "buyinChips": 500,
    "rebuys": true,
    "blindLevels": [{"small": 50, "big": 100, "ante": 0, "minutes": 60}]
}' 2>/dev/null) || true

START2_ACCEPTED=$(jget "$RESULT" 'o.accepted||false')
if [[ "$START2_ACCEPTED" != "true" ]]; then
    log "FAIL: RB-015 — second game/start failed: $RESULT"
    FAILURES=$((FAILURES+1))
fi

# Wait for human seat
SEAT_STATE=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL|CONTINUE|QUITSAVE|REBUY_CHECK" 30 2>/dev/null) || true
HUMAN_SEAT=$(jget "$SEAT_STATE" \
    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.seat??-1")
log "  INFO: human seat: $HUMAN_SEAT"
if ! [[ "$HUMAN_SEAT" =~ ^[0-9]+$ ]]; then
    log "FAIL: RB-015 — invalid human seat: $HUMAN_SEAT"
    FAILURES=$((FAILURES+1))
fi

# Keep folding until bust
REBUY2_START=$(date +%s)
REBUY2_APPEARED=false
while true; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')

    if [[ "$mode" == "REBUY_CHECK" ]]; then
        REBUY2_APPEARED=true
        break
    fi

    if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
        is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
        if [[ "$is_human" == "true" ]]; then
            api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
        fi
    else
        case "$mode" in
            DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                advance_non_betting "$state"
                ;;
        esac
    fi

    [[ $(($(date +%s) - REBUY2_START)) -gt 120 ]] && break
    sleep 0.3
done

if [[ "$REBUY2_APPEARED" == "true" ]]; then
    ACCEPT_RESP=$(api_post_json /action '{"type":"REBUY"}' 2>/dev/null) || true
    ACCEPT_OK=$(jget "$ACCEPT_RESP" 'o.accepted||false')
    if [[ "$ACCEPT_OK" == "true" ]]; then
        log "  OK: REBUY accepted"
    else
        log "FAIL: RB-015 — REBUY not accepted: $ACCEPT_RESP"
        FAILURES=$((FAILURES+1))
    fi

    sleep 3
    state=$(api GET /state 2>/dev/null) || true
    human_chips=$(jget "$state" \
        "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$HUMAN_SEAT)?.chips||0")
    if [[ "$human_chips" -gt 0 ]]; then
        log "  OK: Human chips restored to $human_chips after REBUY"
        screenshot "rebuy-after-accept"
    else
        log "FAIL: RB-015 — Chips still 0 after REBUY (chips=$human_chips)"
        FAILURES=$((FAILURES+1))
    fi
else
    log "FAIL: RB-015 — could not trigger second REBUY_CHECK for accept test"
    FAILURES=$((FAILURES+1))
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Rebuy dialog flow verified: REBUY_CHECK appeared, DECLINE_REBUY accepted, REBUY accepted"
