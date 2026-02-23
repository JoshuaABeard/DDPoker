#!/usr/bin/env bash
# test-neverbroke.sh — Verify the Never Go Broke cheat restores chips when human busts.
#
# Tests NB-001 through NB-003:
#   1. Enable cheat.neverbroke=true
#   2. Start a game (no rebuys required)
#   3. Set human chips to 0 via cheat during their turn, then FOLD
#   4. At end of hand, NEVER_BROKE_ACTIVE fires: chips auto-transferred from chip leader
#      to human (chips restored BEFORE any blocking dialog)
#   5. Verify human chips > 0 via /state polling (works even while info dialog blocks EDT)
#
# Note: REBUY_CHECK mode (from GameOver phase) only fires when human is fully eliminated
# (bGameOver=true). Neverbroke sets bGameOver=false, so REBUY_CHECK never appears here.
#
# Usage:
#   bash .claude/scripts/scenarios/test-neverbroke.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# NB-001: Enable neverbroke cheat
# ============================================================
log "=== NB-001: Enable cheat.neverbroke ==="
api_post_json /options '{"cheat.neverbroke": true}' > /dev/null

OPTIONS=$(api GET /options 2>/dev/null) || die "Could not read /options"
NB=$(jget "$OPTIONS" 'o.cheat && o.cheat.neverbroke')
[[ "$NB" == "true" ]] || die "cheat.neverbroke did not activate (got: $NB)"
log "  OK: cheat.neverbroke is active"

# Start game — no rebuys needed; NEVER_BROKE_ACTIVE path auto-restores chips
lib_start_game 3
screenshot "neverbroke-start"

# ============================================================
# NB-002: Trigger neverbroke by setting chips to 0 and folding
# ============================================================
log "=== NB-002: Trigger Never Go Broke ==="

# Wait for human betting turn (advancing DEAL/CONTINUE as needed)
while true; do
    STATE=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL|CONTINUE|CONTINUE_LOWER" 60) \
        || die "Timed out waiting for human betting turn"
    MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
    case "$MODE" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            IS_HUMAN=$(jget "$STATE" 'o.currentAction && o.currentAction.isHumanTurn || false')
            [[ "$IS_HUMAN" == "true" ]] && break
            sleep 0.3
            ;;
        DEAL|CONTINUE|CONTINUE_LOWER)
            advance_non_betting "$STATE"
            sleep 0.2
            ;;
        *) sleep 0.3 ;;
    esac
done

HUMAN_SEAT=$(jget "$STATE" 'o.currentAction && o.currentAction.humanSeat || 0')
log "  Human at seat $HUMAN_SEAT, mode=$MODE"

# Set chips to 0 and fold to trigger neverbroke at end of hand
log "  Setting human chips to 0 via cheat..."
api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$HUMAN_SEAT,\"amount\":0}" \
    > /dev/null 2>&1 || log "  WARN: setChips returned non-zero exit"

log "  Folding — neverbroke will fire at end of hand..."
api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true

# ============================================================
# NB-003: Verify chips restored (neverbroke auto-transfers from chip leader)
# ============================================================
log "=== NB-003: Verify chips restored by neverbroke ==="
# NEVER_BROKE_ACTIVE sets human.setChipCount(nAdd) BEFORE showing any info dialog,
# so polling /state works even while the EDT is blocked by the info dialog.
START=$(date +%s)
HUMAN_CHIPS=0
while [[ $(($(date +%s) - START)) -lt 30 ]]; do
    sleep 0.5
    STATE=$(api GET /state 2>/dev/null) || continue
    HUMAN_CHIPS=$(jget "$STATE" \
        "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$HUMAN_SEAT)?.chips||0")
    [[ "$HUMAN_CHIPS" -gt 0 ]] && break
done

if [[ "$HUMAN_CHIPS" -gt 0 ]]; then
    log "  OK: Neverbroke restored chips to $HUMAN_CHIPS"
else
    log "FAIL: Human chips still 0 after 30s — neverbroke did not fire"
    FAILURES=$((FAILURES+1))
fi

screenshot "neverbroke-result"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Never Go Broke verified: chips restored to $HUMAN_CHIPS after human went broke"
