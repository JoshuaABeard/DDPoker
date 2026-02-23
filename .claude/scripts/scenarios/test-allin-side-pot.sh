#!/usr/bin/env bash
# test-allin-side-pot.sh — Verify chip conservation holds with side pots.
#
# Creates a staggered all-in scenario:
#   - Seat 0 (human):  small stack (100 chips) — goes all-in
#   - Seat 1 (AI):     medium stack (500 chips) — set via /cheat
#   - Seat 2 (AI):     full stack  (1500 chips)
#
# The human goes all-in with a short stack, creating a side pot between
# seats 1 and 2. After the hand resolves, GET /validate must report
# chipConservation.valid=true.
#
# Note: The AI players' decisions cannot be controlled. The test verifies
# that however the engine distributes the pot(s), chips are conserved.
# Card injection (if needed) can be added via POST /cards/inject.
#
# Usage:
#   bash .claude/scripts/scenarios/test-allin-side-pot.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

lib_start_game 3
screenshot "side-pot-start"

# Wait for first human turn, then set up staggered stack sizes
log "Waiting for first human turn to set up stack sizes..."
STATE=$(wait_human_turn 60) || die "Timed out waiting for human turn"
HUMAN_SEAT=$(jget "$STATE" 'o.currentAction && o.currentAction.humanSeat || 0')
log "Human is at seat $HUMAN_SEAT"

# Set staggered stacks: human=100, AI1=500, leave AI2 at default ~1500
# Find AI seats (not human)
AI1_SEAT=$(( (HUMAN_SEAT + 1) % 3 ))
AI2_SEAT=$(( (HUMAN_SEAT + 2) % 3 ))
log "Setting stacks: seat$HUMAN_SEAT=100, seat$AI1_SEAT=500, seat$AI2_SEAT=unchanged"

api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$HUMAN_SEAT,\"amount\":100}" > /dev/null 2>&1 || \
    log "WARN: setChips for human failed"
api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$AI1_SEAT,\"amount\":500}" > /dev/null 2>&1 || \
    log "WARN: setChips for AI1 failed"
sleep 0.3

# Validate before all-in
BEFORE=$(api GET /validate 2>/dev/null) || die "Could not read /validate before all-in"
CC_BEFORE=$(jget "$BEFORE" 'o.chipConservation && o.chipConservation.valid')
[[ "$CC_BEFORE" == "true" ]] || log "WARN: chipConservation already invalid before all-in: $CC_BEFORE"

# Re-read state after chip adjustment
STATE2=$(api GET /state 2>/dev/null) || die "Could not read state"
MODE2=$(jget "$STATE2" 'o.inputMode || "NONE"')
log "Mode after chip adjustment: $MODE2"

# Submit ALL_IN from human
if echo "$MODE2" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    IS_HUMAN=$(jget "$STATE2" 'o.currentAction && o.currentAction.isHumanTurn || false')
    if [[ "$IS_HUMAN" == "true" ]]; then
        RESP=$(api_post_json /action '{"type":"ALL_IN"}')
        echo "$RESP" | grep -q '"accepted":true' || die "ALL_IN rejected: $RESP"
        log "Human went ALL_IN with 100 chips (short stack)"
    fi
fi
screenshot "side-pot-after-allin"

# Wait for hand to resolve (DEAL mode means hand is over)
log "Waiting for hand to resolve..."
RESOLVE_TIMEOUT=60
RSTART=$(date +%s)
while [[ $(($(date +%s) - RSTART)) -lt $RESOLVE_TIMEOUT ]]; do
    RSTATE=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    RMODE=$(jget "$RSTATE" 'o.inputMode || "NONE"')
    case "$RMODE" in
        DEAL)
            log "Hand resolved — now in DEAL mode"
            break
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$RMODE\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
        QUITSAVE|NONE)
            sleep 0.3 ;;
        *) sleep 0.3 ;;
    esac
    sleep 0.2
done

screenshot "side-pot-resolved"

# Validate chip conservation after side pot resolution
VRESULT=$(api GET /validate 2>/dev/null) || die "Could not read /validate after hand"
CC_VALID=$(jget "$VRESULT" 'o.chipConservation && o.chipConservation.valid')
IM_VALID=$(jget "$VRESULT" 'o.inputModeConsistent')
WARNS=$(jget "$VRESULT" '(o.warnings||[]).join("; ")')

log "Post-hand /validate:"
log "  chipConservation.valid = $CC_VALID"
log "  inputModeConsistent    = $IM_VALID"
[[ -n "$WARNS" ]] && log "  warnings: $WARNS"

# Show per-table breakdown
TABLES=$(node -e "
  const v = JSON.parse(process.argv[1]);
  const t = (v.chipConservation && v.chipConservation.tables) || [];
  t.forEach(tb => console.log('  table ' + tb.id + ': playerChips=' + tb.playerChips
    + ' inPot=' + tb.inPot + ' total=' + tb.total + ' expected=' + tb.expectedTotal));
" -- "$VRESULT" 2>/dev/null) || true
[[ -n "$TABLES" ]] && log "$TABLES"

if [[ "$CC_VALID" != "true" ]]; then
    screenshot "side-pot-validation-fail"
    die "Side pot chip conservation violated! warnings: $WARNS"
fi

pass "Side pot scenario: chip conservation valid after staggered all-in"
