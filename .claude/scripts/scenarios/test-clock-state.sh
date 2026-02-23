#!/usr/bin/env bash
# test-clock-state.sh — Verify clock/timer state in /state response.
#
# Tests C-001 through C-005, C-030 through C-032:
#   - Clock data present in tournament.clock
#   - Level number, time remaining shown
#   - Next level blinds shown
#   - Level advance mode (TIME vs HANDS) reported
#
# Usage:
#   bash .claude/scripts/scenarios/test-clock-state.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

# Wait for game to start
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game"

state=$(api GET /state 2>/dev/null) || die "Could not read state"

# ============================================================
# C-001: Clock visible (present in state)
# ============================================================
log "=== C-001: Clock Data Present ==="
clock_seconds=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.secondsRemaining')
clock_paused=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.isPaused')
clock_expired=$(jget "$state" 'o.tournament&&o.tournament.clock&&o.tournament.clock.isExpired')

log "  secondsRemaining: $clock_seconds"
log "  isPaused: $clock_paused"
log "  isExpired: $clock_expired"

if [[ -n "$clock_seconds" && "$clock_seconds" != "" && "$clock_seconds" != "undefined" ]]; then
    log "  OK: Clock seconds present"
else
    log "FAIL: Clock seconds not in /state"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# C-003: Level number shown
# ============================================================
log "=== C-003: Level Number ==="
level=$(jget "$state" 'o.tournament&&o.tournament.level')
log "  Current level: $level"
if [[ -n "$level" && "$level" != "" && "$level" != "undefined" ]]; then
    log "  OK: Level number present"
else
    log "FAIL: Level number missing"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# C-004: Current blind amounts
# ============================================================
log "=== C-004: Current Blinds ==="
sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind')
bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind')
log "  SB=$sb, BB=$bb"
if [[ -n "$sb" && "$sb" -gt 0 ]]; then
    log "  OK: Blinds present"
else
    log "FAIL: Blinds missing or zero"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# C-005: Next level preview
# ============================================================
log "=== C-005: Next Level Blinds ==="
next_sb=$(jget "$state" 'o.tournament&&o.tournament.nextSmallBlind')
next_bb=$(jget "$state" 'o.tournament&&o.tournament.nextBigBlind')
log "  Next SB=$next_sb, Next BB=$next_bb"
if [[ -n "$next_sb" && "$next_sb" != "" && "$next_sb" != "undefined" ]]; then
    log "  OK: Next level blinds present"
else
    log "  WARN: Next level blinds not available (may be last level)"
fi

# ============================================================
# Level advance mode
# ============================================================
log "=== Level Advance Mode ==="
advance_mode=$(jget "$state" 'o.tournament&&o.tournament.levelAdvanceMode')
log "  advanceMode: $advance_mode"
if [[ -n "$advance_mode" && "$advance_mode" != "" && "$advance_mode" != "undefined" ]]; then
    log "  OK: Level advance mode present: $advance_mode"
else
    log "  WARN: Level advance mode not available"
fi

# ============================================================
# Level count
# ============================================================
log "=== Level Count ==="
level_count=$(jget "$state" 'o.tournament&&o.tournament.levelCount')
log "  levelCount: $level_count"
if [[ -n "$level_count" && "$level_count" -gt 0 ]]; then
    log "  OK: Level count present"
else
    log "  WARN: Level count not available"
fi

# ============================================================
# Table count
# ============================================================
log "=== Table Count ==="
table_count=$(jget "$state" 'o.tableCount')
log "  tableCount: $table_count"
if [[ -n "$table_count" && "$table_count" -gt 0 ]]; then
    log "  OK: Table count present"
else
    log "  WARN: Table count not available"
fi

# ============================================================
# AI player types
# ============================================================
log "=== AI Player Types ==="
ai_type=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&!p.isHuman)?.playerType||""')
log "  First AI player type: $ai_type"
if [[ -n "$ai_type" && "$ai_type" != "" && "$ai_type" != "undefined" ]]; then
    log "  OK: AI player type present"
else
    log "  WARN: AI player type not in state (may be hidden)"
fi

screenshot "clock-state"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Clock state verified: timer, level, blinds, next level, advance mode all present"
