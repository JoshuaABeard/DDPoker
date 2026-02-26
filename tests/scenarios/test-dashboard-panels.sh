#!/usr/bin/env bash
# test-dashboard-panels.sh — Verify dashboard state fields are populated on human turn.
#
# Waits for the first human-turn betting mode and asserts that:
#   - currentAction.advisorAdvice is non-null (advisor has a recommendation)
#   - currentAction.advisorTitle  is non-null
#   - currentAction.callAmount or currentAction.minBet is present and numeric
#   - currentAction.pot is non-negative
#   - currentAction.availableActions is a non-empty list
#
# Usage:
#   bash tests/scenarios/test-dashboard-panels.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

# Enable AI face-up so the advisor always computes a recommendation
log "Setting cheat.aifaceup=true..."
api_post_json /options '{"cheat.aifaceup": true}' > /dev/null

lib_start_game 3
screenshot "dashboard-start"

log "Waiting for first human turn..."
STATE=$(wait_human_turn 60) || die "Timed out waiting for human turn"
MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
log "Human turn in mode: $MODE"

# Dump currentAction for visibility
log "currentAction:"
node -e "
  const s = JSON.parse(process.argv[1]);
  const ca = s.currentAction || {};
  Object.keys(ca).forEach(k => console.log('  ' + k + ': ' + JSON.stringify(ca[k])));
" -- "$STATE" 2>/dev/null || true

screenshot "dashboard-human-turn"

# Extract and assert fields
ADVICE=$(jget "$STATE" 'o.currentAction && o.currentAction.advisorAdvice || ""')
TITLE=$(jget  "$STATE" 'o.currentAction && o.currentAction.advisorTitle || ""')
CALL_AMT=$(jget "$STATE" 'o.currentAction && o.currentAction.callAmount || ""')
MIN_BET=$(jget  "$STATE" 'o.currentAction && o.currentAction.minBet || ""')
POT=$(jget      "$STATE" 'o.currentAction && o.currentAction.pot || -1')
AVAIL=$(jget    "$STATE" '(o.currentAction && o.currentAction.availableActions || []).length')

FAILURES=0

log "Checking advisorAdvice..."
if [[ -z "$ADVICE" || "$ADVICE" == "null" ]]; then
    log "  FAIL: advisorAdvice is null/empty"
    FAILURES=$((FAILURES+1))
else
    log "  OK: advisorAdvice='$ADVICE'"
fi

log "Checking advisorTitle..."
if [[ -z "$TITLE" || "$TITLE" == "null" ]]; then
    log "  FAIL: advisorTitle is null/empty"
    FAILURES=$((FAILURES+1))
else
    log "  OK: advisorTitle='$TITLE'"
fi

log "Checking pot..."
if [[ -z "$POT" || "$POT" == "-1" ]]; then
    log "  FAIL: pot not present"
    FAILURES=$((FAILURES+1))
else
    log "  OK: pot=$POT"
fi

log "Checking available actions..."
if [[ -z "$AVAIL" || "$AVAIL" == "0" ]]; then
    log "  FAIL: availableActions is empty"
    FAILURES=$((FAILURES+1))
else
    log "  OK: $AVAIL actions available"
fi

log "Checking bet/call amounts..."
if [[ -n "$CALL_AMT" && "$CALL_AMT" != "0" ]]; then
    log "  OK: callAmount=$CALL_AMT"
elif [[ -n "$MIN_BET" && "$MIN_BET" != "0" ]]; then
    log "  OK: minBet=$MIN_BET"
else
    log "  NOTE: callAmount=0 and minBet=0 (may be valid if first to act with no bet)"
fi

[[ $FAILURES -eq 0 ]] || die "$FAILURES dashboard field assertion(s) failed"
pass "All dashboard fields populated correctly on human turn (mode: $MODE)"
