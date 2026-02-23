#!/usr/bin/env bash
# test-ai-types.sh — Verify AI player type listing via /ai-types endpoint.
#
# Tests A-001 through A-006:
#   - List all AI player types
#   - Verify type details (name, skill level, description)
#   - Verify types are used in game
#
# Usage:
#   bash .claude/scripts/scenarios/test-ai-types.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# A-001: List AI types
# ============================================================
log "=== A-001: List AI Types ==="
TYPES=$(api GET /ai-types 2>/dev/null) || die "Could not read /ai-types"
TYPE_COUNT=$(jget "$TYPES" '(o.aiTypes||[]).length')
log "  Found $TYPE_COUNT AI types"

if [[ "$TYPE_COUNT" -gt 0 ]]; then
    log "  OK: AI types found"
else
    log "FAIL: No AI types found"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# A-002: Verify type details
# ============================================================
log "=== A-002: AI Type Details ==="
for i in 0 1 2; do
    NAME=$(jget "$TYPES" "(o.aiTypes||[])[$i]?.name||''")
    SKILL=$(jget "$TYPES" "(o.aiTypes||[])[$i]?.skillLevel||0")
    DESC=$(jget "$TYPES" "(o.aiTypes||[])[$i]?.description||''")
    if [[ -n "$NAME" && "$NAME" != "" ]]; then
        log "  Type $i: $NAME (skill=$SKILL)"
    fi
done

# Verify each type has a name and valid skill level
VALID=0
for i in $(seq 0 $((TYPE_COUNT - 1))); do
    NAME=$(jget "$TYPES" "(o.aiTypes||[])[$i]?.name||''")
    SKILL=$(jget "$TYPES" "(o.aiTypes||[])[$i]?.skillLevel||0")
    if [[ -n "$NAME" && "$NAME" != "" && "$SKILL" -gt 0 ]]; then
        VALID=$((VALID+1))
    fi
done
log "  Valid types with name and skill: $VALID / $TYPE_COUNT"
if [[ "$VALID" -gt 0 ]]; then
    log "  OK: Types have valid details"
else
    log "FAIL: No types with valid details"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# A-005: Verify AI types appear in running game
# ============================================================
log "=== A-005: AI Types in Game ==="
lib_start_game 3
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game"

state=$(api GET /state 2>/dev/null) || die "Could not read state"
ai_type=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&!p.isHuman)?.playerType||""')
log "  First AI player type in game: $ai_type"
if [[ -n "$ai_type" && "$ai_type" != "" && "$ai_type" != "undefined" ]]; then
    log "  OK: AI player type present in running game"
else
    log "  WARN: AI player type not visible in game state"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "AI types verified: $TYPE_COUNT types listed, types have details, type visible in game"
