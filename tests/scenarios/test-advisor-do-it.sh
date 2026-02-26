#!/usr/bin/env bash
# test-advisor-do-it.sh — Verify ADVISOR_DO_IT action executes the AI recommendation.
#
# Starts a game with cheat.aifaceup=true (so the advisor has full information),
# waits for the first human-turn betting mode, then POSTs ADVISOR_DO_IT.
# Asserts that:
#   1. /action accepts ADVISOR_DO_IT
#   2. The human's hole cards change (a new hand was dealt, proving the game advanced)
#
# Usage:
#   bash tests/scenarios/test-advisor-do-it.sh [options]
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
# Capture human's hole cards — unique per hand (probability of identical consecutive hands ≈ 0).
# handNumber is not used because getHandNum() is always 0 in the embedded desktop game
# (setHandNum is only called from the online game server).
CARDS_BEFORE=$(jget "$STATE" '(function(){var t=(o.tables||[])[0],h=t&&(t.players||[]).find(function(p){return p.isHuman});return JSON.stringify(h&&h.holeCards||[])})()')
log "Human turn: mode=$MODE  advisorTitle='$TITLE'  advice='$ADVICE'  cards=$CARDS_BEFORE"

# Submit ADVISOR_DO_IT
RESP=$(api_post_json /action '{"type":"ADVISOR_DO_IT"}')
echo "$RESP" | grep -q '"accepted":true' || die "ADVISOR_DO_IT rejected: $RESP"
log "ADVISOR_DO_IT accepted"

# Verify the human's hole cards change (game dealt a new hand).
# With ai-delay-ms=0, inputMode cycles back to CALL_RAISE within milliseconds of
# the action completing — too fast to catch a mode transition. Hole cards change
# when a new hand is dealt; they are unique per hand and stable within a hand,
# making them the reliable indicator that ADVISOR_DO_IT executed and the game advanced.
# NOTE: screenshot must NOT be called before this poll — it blocks ~25s while the game
# processes the action, causing the poll to start before new cards are dealt.
CARDS_AFTER=""
for i in $(seq 1 30); do   # 30 × ~800ms per iteration (200ms sleep + curl + jget)
    sleep 0.2
    POLL_STATE=$(api GET /state 2>/dev/null) || continue
    POLL_CARDS=$(jget "$POLL_STATE" '(function(){var t=(o.tables||[])[0],h=t&&(t.players||[]).find(function(p){return p.isHuman});return JSON.stringify(h&&h.holeCards||[])})()')
    if [[ "$POLL_CARDS" != "$CARDS_BEFORE" ]]; then
        CARDS_AFTER="$POLL_CARDS"
        break
    fi
done

if [[ -z "$CARDS_AFTER" ]]; then
    die "Hole cards unchanged after ADVISOR_DO_IT (still $CARDS_BEFORE) — action may not have executed"
fi

screenshot "advisor-after-action"
pass "ADVISOR_DO_IT executed OK: cards $CARDS_BEFORE → $CARDS_AFTER  (advice was: '${TITLE}')"
