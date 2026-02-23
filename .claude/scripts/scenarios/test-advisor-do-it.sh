#!/usr/bin/env bash
# test-advisor-do-it.sh — Verify ADVISOR_DO_IT action executes the AI recommendation.
#
# Starts a game with cheat.aifaceup=true (so the advisor has full information),
# waits for the first human-turn betting mode, then POSTs ADVISOR_DO_IT.
# Asserts that:
#   1. /action accepts ADVISOR_DO_IT
#   2. The inputMode changes from a betting mode (i.e., the game advanced)
#
# Usage:
#   bash .claude/scripts/scenarios/test-advisor-do-it.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

# Enable AI-face-up so the advisor always has a recommendation
log "Setting cheat.aifaceup=true..."
api_post_json /options '{"cheat.aifaceup": true}' > /dev/null

lib_start_game 3
screenshot "advisor-start"

log "Waiting for human turn..."
STATE=$(wait_human_turn 60) || die "Timed out waiting for human turn"
MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
ADVICE=$(jget "$STATE" 'o.currentAction && o.currentAction.advisorAdvice || ""')
TITLE=$(jget "$STATE" 'o.currentAction && o.currentAction.advisorTitle || ""')
log "Human turn: mode=$MODE  advisorTitle='$TITLE'  advice='$ADVICE'"

# Submit ADVISOR_DO_IT
RESP=$(api_post_json /action '{"type":"ADVISOR_DO_IT"}')
echo "$RESP" | grep -q '"accepted":true' || die "ADVISOR_DO_IT rejected: $RESP"
log "ADVISOR_DO_IT accepted"
screenshot "advisor-after-action"

# Verify the mode changed (game advanced)
sleep 1
STATE2=$(api GET /state 2>/dev/null) || die "Could not read state after ADVISOR_DO_IT"
MODE2=$(jget "$STATE2" 'o.inputMode || "NONE"')
log "Mode after ADVISOR_DO_IT: $MODE2 (was: $MODE)"

# Mode should have changed from the betting mode (could now be QUITSAVE, CONTINUE, etc.)
# Accept any change; QUITSAVE means AI is now acting, which is correct.
if [[ "$MODE2" == "$MODE" ]]; then
    # Wait one more second in case the dispatch is slow
    sleep 2
    STATE3=$(api GET /state 2>/dev/null) || true
    MODE3=$(jget "$STATE3" 'o.inputMode || "NONE"')
    if [[ "$MODE3" == "$MODE" ]]; then
        die "Mode unchanged after ADVISOR_DO_IT: still $MODE — action may not have executed"
    fi
    MODE2="$MODE3"
fi

pass "ADVISOR_DO_IT executed OK: $MODE → $MODE2  (advice was: '${TITLE}')"
