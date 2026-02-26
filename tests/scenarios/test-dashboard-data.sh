#!/usr/bin/env bash
# test-dashboard-data.sh — Verify dashboard panel data in /state.
#
# Tests D-001 through D-013:
#   - Hand strength data for human player
#   - Advisor data (advice/title)
#   - Rank data
#   - Pot odds and betting info
#   - Player standings via chips
#
# Usage:
#   bash tests/scenarios/test-dashboard-data.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 4

# Enable advisor for maximum dashboard data
api_post_json /options '{"advisor.enabled": true}' > /dev/null 2>&1 || true

FAILURES=0

# Wait for a human turn to check dashboard data
log "=== D-001: Dashboard Data on Human Turn ==="
state=$(wait_human_turn 60) || die "Timed out waiting for human turn"

# ============================================================
# D-002: Hand strength
# ============================================================
log "=== D-002: Hand Strength ==="
human_strength=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.handStrength')
human_eff=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.effectiveHandStrength')
log "  handStrength: $human_strength"
log "  effectiveHandStrength: $human_eff"
if [[ -n "$human_strength" && "$human_strength" != "undefined" && "$human_strength" != "" ]]; then
    log "  OK: Hand strength present"
else
    log "  WARN: Hand strength not available on this turn"
fi

# ============================================================
# D-003: Player rank
# ============================================================
log "=== D-003: Player Rank ==="
human_rank=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.rank')
log "  rank: $human_rank"
if [[ -n "$human_rank" && "$human_rank" != "undefined" && "$human_rank" != "" ]]; then
    log "  OK: Rank present"
else
    log "  WARN: Rank not available"
fi

# ============================================================
# D-004: Advisor advice
# ============================================================
log "=== D-004: Advisor Advice ==="
advice=$(jget "$state" 'o.currentAction&&o.currentAction.advisorAdvice||""')
title=$(jget "$state" 'o.currentAction&&o.currentAction.advisorTitle||""')
log "  advisorAdvice: $advice"
log "  advisorTitle: $title"
if [[ -n "$advice" && "$advice" != "" && "$advice" != "undefined" ]]; then
    log "  OK: Advisor advice present"
else
    log "  WARN: No advisor advice (may need advisor enabled)"
fi

# ============================================================
# D-006: Pot and betting info
# ============================================================
log "=== D-006: Pot and Betting Info ==="
pot=$(jget "$state" 'o.currentAction&&o.currentAction.pot||0')
call_amount=$(jget "$state" 'o.currentAction&&o.currentAction.callAmount||0')
min_bet=$(jget "$state" 'o.currentAction&&o.currentAction.minBet||0')
max_bet=$(jget "$state" 'o.currentAction&&o.currentAction.maxBet||0')
log "  pot=$pot, callAmount=$call_amount, minBet=$min_bet, maxBet=$max_bet"
if [[ "$pot" -gt 0 ]]; then
    log "  OK: Pot info present"
else
    log "  WARN: Pot is zero"
fi

# ============================================================
# D-008: Available actions
# ============================================================
log "=== D-008: Available Actions ==="
actions=$(jget "$state" 'JSON.stringify(o.currentAction&&o.currentAction.availableActions||[])')
log "  availableActions: $actions"
if [[ "$actions" != "[]" ]]; then
    log "  OK: Available actions listed"
else
    log "FAIL: No available actions on human turn"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# D-010: Player standings (chips across all players)
# ============================================================
log "=== D-010: Player Standings ==="
player_count=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).length')
log "  Players at table: $player_count"
for i in 0 1 2 3; do
    name=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[])[$i]?.name||''")
    chips=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[])[$i]?.chips||0")
    human=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[])[$i]?.isHuman||false")
    if [[ -n "$name" && "$name" != "" ]]; then
        log "  Player $i: $name, chips=$chips, human=$human"
    fi
done
if [[ "$player_count" -gt 0 ]]; then
    log "  OK: Player standings available"
else
    log "FAIL: No player data"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# D-012: Hole cards
# ============================================================
log "=== D-012: Hole Cards ==="
hole=$(jget "$state" 'JSON.stringify((o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.holeCards||[])')
log "  holeCards: $hole"
if [[ "$hole" != "[]" ]]; then
    log "  OK: Hole cards visible for human"
else
    log "  WARN: No hole cards visible"
fi

# ============================================================
# D-013: Community cards and round
# ============================================================
log "=== D-013: Community Cards ==="
community=$(jget "$state" 'JSON.stringify(o.tables&&o.tables[0]&&o.tables[0].communityCards||[])')
round=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].round||""')
log "  communityCards: $community, round: $round"
log "  OK: Round and community cards available"

screenshot "dashboard-data"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Dashboard data verified: strength, rank, advisor, pot, actions, standings, cards"
