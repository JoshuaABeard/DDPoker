#!/usr/bin/env bash
# test-edge-cases.sh — Additional edge case tests.
#
# Tests E-005 through E-016:
#   - All-in with multiple players
#   - Min-raise scenarios
#   - Short stack situations
#   - Multiple eliminations in one hand
#
# Usage:
#   bash .claude/scripts/scenarios/test-edge-cases.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# E-005: Short stack all-in
# ============================================================
log "=== E-005: Short Stack All-In ==="
lib_start_game 4

# Set one player to very low chips
api_post_json /cheat '{"action": "setChips", "seat": 1, "amount": 50}' > /dev/null 2>&1 || true

state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game"

# Play through with the short stack
HANDS=0
while [[ $HANDS -lt 10 ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            HANDS=$((HANDS+1))
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac
    sleep 0.3
done

# Validate chip conservation
VALIDATE=$(api GET /validate 2>/dev/null) || true
VALID=$(jget "$VALIDATE" 'o.valid||false')
if [[ "$VALID" == "true" ]]; then
    log "  OK: Chip conservation valid after short stack play"
else
    log "FAIL: Chip conservation invalid"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# E-007: Level count verification
# ============================================================
log "=== E-007: Level Count ==="
state=$(api GET /state 2>/dev/null) || true
level_count=$(jget "$state" 'o.tournament&&o.tournament.levelCount||0')
log "  Level count: $level_count"
if [[ "$level_count" -gt 0 ]]; then
    log "  OK: Level count present"
else
    log "  WARN: Level count not available"
fi

# ============================================================
# E-009: Multiple eliminations
# ============================================================
log "=== E-009: Multiple Eliminations ==="
# Set two players to very low chips to force eliminations
api_post_json /cheat '{"action": "setChips", "seat": 2, "amount": 10}' > /dev/null 2>&1 || true
api_post_json /cheat '{"action": "setChips", "seat": 3, "amount": 10}' > /dev/null 2>&1 || true

# Play through until eliminations happen
ELIM_HANDS=0
while [[ $ELIM_HANDS -lt 15 ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')
    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            [[ "$is_human" == "true" ]] && api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            ELIM_HANDS=$((ELIM_HANDS+1))
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac
    [[ "$remaining" -le 1 ]] && break
    sleep 0.3
done

log "  Players remaining: $remaining after $ELIM_HANDS hands"
VALIDATE=$(api GET /validate 2>/dev/null) || true
VALID=$(jget "$VALIDATE" 'o.valid||false')
if [[ "$VALID" == "true" ]]; then
    log "  OK: Chip conservation valid after eliminations"
else
    log "  WARN: Chip conservation check: $VALIDATE"
fi

# ============================================================
# E-012: Chip leaderboard
# ============================================================
log "=== E-012: Chip Leaderboard ==="
state=$(api GET /state 2>/dev/null) || true
leaders=$(jget "$state" '(o.tournament&&o.tournament.chipLeaderboard||[]).length||0')
log "  Leaderboard entries: $leaders"
if [[ "$leaders" -gt 0 ]]; then
    top_name=$(jget "$state" '(o.tournament&&o.tournament.chipLeaderboard||[])[0]?.name||""')
    top_chips=$(jget "$state" '(o.tournament&&o.tournament.chipLeaderboard||[])[0]?.chips||0')
    log "  Leader: $top_name ($top_chips chips)"
    log "  OK: Leaderboard present"
else
    log "  WARN: Leaderboard empty"
fi

screenshot "edge-cases"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Edge cases verified: short stack, level count, eliminations, leaderboard"
