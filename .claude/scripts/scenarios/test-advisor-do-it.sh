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

# Verify the mode changed (game advanced).
# With ai-delay-ms=0, the game can cycle back to a betting mode within milliseconds,
# so we poll in a tight loop rather than checking at a fixed delay.
MODE_AFTER=""
for i in $(seq 1 60); do   # 60 × 50ms = 3 seconds
    sleep 0.05
    POLL_STATE=$(api GET /state 2>/dev/null) || continue
    POLL_MODE=$(jget "$POLL_STATE" 'o.inputMode || "NONE"')
    if [[ "$POLL_MODE" != "$MODE" ]]; then
        MODE_AFTER="$POLL_MODE"
        break
    fi
done

if [[ -z "$MODE_AFTER" ]]; then
    die "Mode unchanged after ADVISOR_DO_IT: still $MODE — action may not have executed"
fi

pass "ADVISOR_DO_IT executed OK: $MODE → $MODE_AFTER  (advice was: '${TITLE}')"
