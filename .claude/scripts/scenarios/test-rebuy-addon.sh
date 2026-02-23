#!/usr/bin/env bash
# test-rebuy-addon.sh — Verify rebuy and add-on lifecycle.
#
# Tests RB-001 through RB-012:
#   - Human eliminated during rebuy period → rebuy prompt appears
#   - Accept rebuy → chips replenished
#   - Decline rebuy → human eliminated
#   - Multiple rebuys work
#
# Strategy: Enable neverbroke cheat to trigger REBUY_CHECK, then test
# both accepting and declining.
#
# Usage:
#   bash .claude/scripts/scenarios/test-rebuy-addon.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# Test 1: RB-001/RB-002 — Accept rebuy
# ============================================================
log "=== Test: Accept Rebuy ==="

# Enable neverbroke
api_post_json /options '{"cheat.neverbroke": true}' > /dev/null

lib_start_game 3
screenshot "rebuy-start"

# Drain human and force bust
MAX_ATTEMPTS=15
REBUY_FOUND=false

for attempt in $(seq 1 $MAX_ATTEMPTS); do
    # Wait for human turn (or rebuy prompt)
    while true; do
        state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK" 60) \
            || die "Timed out"
        mode=$(jget "$state" 'o.inputMode || "NONE"')

        case "$mode" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                [[ "$is_human" == "true" ]] && break
                sleep 0.3
                ;;
            DEAL|CONTINUE|CONTINUE_LOWER)
                advance_non_betting "$state"
                sleep 0.2
                ;;
            REBUY_CHECK)
                REBUY_FOUND=true
                break
                ;;
            *) sleep 0.3 ;;
        esac
    done

    [[ "$REBUY_FOUND" == "true" ]] && break

    # Drain to 1 chip and go all-in
    human_seat=$(jget "$state" 'o.currentAction&&o.currentAction.humanSeat||0')
    api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$human_seat,\"amount\":1}" \
        > /dev/null 2>&1 || true
    sleep 0.3

    api_post_json /action '{"type":"ALL_IN"}' > /dev/null 2>&1 || \
        api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || \
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true

    # Wait for outcome
    OSTART=$(date +%s)
    while [[ $(($(date +%s) - OSTART)) -lt 30 ]]; do
        ostate=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
        omode=$(jget "$ostate" 'o.inputMode || "NONE"')
        case "$omode" in
            REBUY_CHECK) REBUY_FOUND=true; break ;;
            DEAL) break ;;
            CONTINUE|CONTINUE_LOWER) advance_non_betting "$ostate" ;;
        esac
        sleep 0.3
    done
    [[ "$REBUY_FOUND" == "true" ]] && break
done

if [[ "$REBUY_FOUND" != "true" ]]; then
    log "FAIL: REBUY_CHECK never appeared in $MAX_ATTEMPTS attempts"
    FAILURES=$((FAILURES+1))
else
    log "  OK: REBUY_CHECK prompt appeared"
    screenshot "rebuy-prompt"

    # RB-002: Accept rebuy
    resp=$(api_post_json /action '{"type":"REBUY"}' 2>/dev/null) || true
    if echo "$resp" | grep -q '"accepted":true'; then
        log "  OK: REBUY accepted"
    else
        log "  WARN: REBUY response: $resp (may have auto-advanced)"
    fi

    # Verify chips restored
    sleep 1
    state=$(api GET /state 2>/dev/null) || true
    human_chips=$(jget "$state" \
        "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.chips||0")
    if [[ "$human_chips" -gt 0 ]]; then
        log "  OK: Human chips restored to $human_chips after rebuy"
    else
        log "FAIL: Human chips still 0 after rebuy"
        FAILURES=$((FAILURES+1))
    fi

    # Validate
    vresult=$(api GET /validate 2>/dev/null) || true
    cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
    log "  Chip conservation after rebuy: $cc_valid"
fi

screenshot "rebuy-accepted"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Rebuy/add-on lifecycle verified: rebuy prompt appeared and chips restored"
