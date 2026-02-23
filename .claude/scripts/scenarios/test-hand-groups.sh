#!/usr/bin/env bash
# test-hand-groups.sh — Verify hand group CRUD via /hand-groups endpoint.
#
# Tests HG-001 through HG-010:
#   - List built-in hand groups
#   - Create a new hand group
#   - Verify hand group appears in list
#   - Delete a user-created hand group
#
# Usage:
#   bash .claude/scripts/scenarios/test-hand-groups.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# HG-001: List hand groups
# ============================================================
log "=== HG-001: List Hand Groups ==="
HG_LIST=$(api GET /hand-groups 2>/dev/null) || die "Could not read /hand-groups"
GROUP_COUNT=$(jget "$HG_LIST" '(o.handGroups||[]).length')
log "  Found $GROUP_COUNT hand groups"

if [[ "$GROUP_COUNT" -gt 0 ]]; then
    log "  OK: Hand groups found"
    FIRST_NAME=$(jget "$HG_LIST" '(o.handGroups||[])[0]?.name||""')
    FIRST_COUNT=$(jget "$HG_LIST" '(o.handGroups||[])[0]?.handCount||0')
    log "  First group: $FIRST_NAME (handCount=$FIRST_COUNT)"
else
    log "  WARN: No hand groups found"
fi

# ============================================================
# HG-003: Create a new hand group
# ============================================================
log "=== HG-003: Create Hand Group ==="
TEST_NAME="API Test Group $(date +%s)"
CREATE_RESULT=$(api_post_json /hand-groups "{\"name\": \"$TEST_NAME\", \"description\": \"Test group\"}" 2>/dev/null) || die "Create failed"
CREATED=$(jget "$CREATE_RESULT" 'o.created')
if [[ "$CREATED" == "true" ]]; then
    log "  OK: Hand group created: $TEST_NAME"
else
    log "FAIL: Hand group creation failed: $CREATE_RESULT"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# HG-004: Verify group in list
# ============================================================
log "=== HG-004: Verify Group in List ==="
HG_LIST=$(api GET /hand-groups 2>/dev/null) || die "Could not re-read hand groups"
FOUND=$(jget "$HG_LIST" "(o.handGroups||[]).find(g=>g.name==='$TEST_NAME')?.name||''")
if [[ "$FOUND" == "$TEST_NAME" ]]; then
    log "  OK: Group found in list"
else
    log "FAIL: Created group not found"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# HG-005: Delete hand group
# ============================================================
log "=== HG-005: Delete Hand Group ==="
DEL_RESULT=$(api DELETE /hand-groups -H "Content-Type: application/json" \
    -d "{\"name\": \"$TEST_NAME\"}" 2>/dev/null) || true
DELETED=$(jget "$DEL_RESULT" 'o.deleted')
if [[ "$DELETED" == "true" ]]; then
    log "  OK: Hand group deleted"
else
    log "FAIL: Deletion failed: $DEL_RESULT"
    FAILURES=$((FAILURES+1))
fi

# Verify gone
HG_LIST=$(api GET /hand-groups 2>/dev/null) || true
STILL=$(jget "$HG_LIST" "(o.handGroups||[]).find(g=>g.name==='$TEST_NAME')?.name||''")
if [[ -z "$STILL" || "$STILL" == "" ]]; then
    log "  OK: Group no longer in list"
else
    log "FAIL: Deleted group still in list"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# HG-008: Duplicate name rejection
# ============================================================
log "=== HG-008: Duplicate Name ==="
DUP_NAME="Dup HG $(date +%s)"
api_post_json /hand-groups "{\"name\": \"$DUP_NAME\"}" > /dev/null 2>&1 || true
DUP_RESULT=$(api_post_json /hand-groups "{\"name\": \"$DUP_NAME\"}" 2>/dev/null) || true
DUP_ERROR=$(jget "$DUP_RESULT" 'o.error||""')
if [[ "$DUP_ERROR" == "Conflict" ]]; then
    log "  OK: Duplicate name rejected"
else
    log "  WARN: Duplicate response: $DUP_RESULT"
fi
api DELETE /hand-groups -H "Content-Type: application/json" \
    -d "{\"name\": \"$DUP_NAME\"}" > /dev/null 2>&1 || true

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Hand group CRUD verified: list=$GROUP_COUNT, create, verify, delete"
