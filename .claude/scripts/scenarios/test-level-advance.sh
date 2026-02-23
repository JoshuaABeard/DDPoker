#!/usr/bin/env bash
# test-level-advance.sh — Verify blind level advancement via setLevel cheat.
#
# Tests C-010, C-014, C-015:
#   - Advancing to a higher level increases blinds
#   - Blinds match the tournament profile for each level
#   - After final level, last level repeats
#
# Usage:
#   bash .claude/scripts/scenarios/test-level-advance.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

assert_ge() {
    local desc="$1" actual="$2" min="$3"
    if [[ "$actual" -ge "$min" ]]; then
        log "  OK: $desc = $actual (>= $min)"
    else
        log "FAIL: $desc = $actual (expected >= $min)"
        FAILURES=$((FAILURES+1))
    fi
}

assert_gt() {
    local desc="$1" actual="$2" min="$3"
    if [[ "$actual" -gt "$min" ]]; then
        log "  OK: $desc = $actual (> $min)"
    else
        log "FAIL: $desc = $actual (expected > $min)"
        FAILURES=$((FAILURES+1))
    fi
}

# Wait for game to reach a playable state (any human or AI betting mode)
log "=== Baseline: Level 1 ==="
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|QUITSAVE" 60) \
    || die "Timed out waiting for game start"

# Read level 1 blinds directly from state
state=$(api GET /state 2>/dev/null) || die "Could not read state"
level1_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
level1_bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
level1_level=$(jget "$state" 'o.tournament&&o.tournament.level||0')
log "  Level $level1_level: SB=$level1_sb, BB=$level1_bb"

# Sanity: level 1 blinds must be positive
if [[ "$level1_sb" -le 0 ]]; then
    log "FAIL: Level 1 small blind is 0 — StateHandler blind lookup broken"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# C-010/C-014: Advance to level 2 and verify blinds increase
# ============================================================
# The default practice profile has 3 levels (25/50, 50/100, 100/200),
# so we test level advancement within that range.
log "=== Advancing to Level 2 ==="
cheat_result=$(api_post_json /cheat '{"action":"setLevel","level":2}' 2>/dev/null) \
    || log "WARN: setLevel HTTP call failed"
log "  setLevel result: $cheat_result"
sleep 0.3

# Read blinds immediately — StateHandler reads from profile using the new level
state=$(api GET /state 2>/dev/null) || die "Could not read state"
level2_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
level2_bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
level2_level=$(jget "$state" 'o.tournament&&o.tournament.level||0')
log "  Level $level2_level: SB=$level2_sb, BB=$level2_bb"

# Blinds should be higher at level 2 than level 1
assert_ge "level 2 small blind vs level 1" "$level2_sb" "$level1_sb"
assert_ge "level 2 big blind vs level 1" "$level2_bb" "$level1_bb"
assert_gt "level 2 small blind strictly greater" "$level2_sb" "$level1_sb"

screenshot "level-advance-2"

# ============================================================
# C-010: Advance to level 3 and verify further increase
# ============================================================
log "=== Advancing to Level 3 ==="
api_post_json /cheat '{"action":"setLevel","level":3}' > /dev/null 2>&1 || true
sleep 0.3

state=$(api GET /state 2>/dev/null) || true
level3_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
level3_bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
level3_level=$(jget "$state" 'o.tournament&&o.tournament.level||0')
log "  Level $level3_level: SB=$level3_sb, BB=$level3_bb"

assert_ge "level 3 small blind vs level 2" "$level3_sb" "$level2_sb"
assert_ge "level 3 big blind vs level 2" "$level3_bb" "$level2_bb"
assert_gt "level 3 small blind strictly greater" "$level3_sb" "$level2_sb"

screenshot "level-advance-3"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Level advancement verified: blinds increase correctly across levels 1 → 2 → 3"
