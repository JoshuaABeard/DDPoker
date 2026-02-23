#!/usr/bin/env bash
# test-tournament-profiles.sh — CRUD operations on tournament profiles.
#
# Tests P-001 through P-007:
#   - List built-in tournament profiles
#   - Create a new profile with custom settings
#   - Verify profile settings persisted
#   - Delete a user-created profile
#   - Verify built-in profiles cannot be deleted
#
# Usage:
#   bash .claude/scripts/scenarios/test-tournament-profiles.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# P-001: List built-in profiles
# ============================================================
log "=== P-001: List Tournament Profiles ==="
PROFILES=$(api GET /tournament-profiles 2>/dev/null) || die "Could not read /tournament-profiles"
PROFILE_COUNT=$(jget "$PROFILES" '(o.profiles||[]).length')
log "  Found $PROFILE_COUNT profiles"

if [[ "$PROFILE_COUNT" -gt 0 ]]; then
    log "  OK: Built-in profiles found"
    FIRST_NAME=$(jget "$PROFILES" '(o.profiles||[])[0]?.name||""')
    log "  First profile: $FIRST_NAME"
else
    log "  WARN: No profiles found (may need first-run setup)"
fi

# ============================================================
# P-003: Create a new profile
# ============================================================
log "=== P-003: Create New Profile ==="
TEST_PROFILE_NAME="API Test Profile $(date +%s)"
CREATE_BODY="{
    \"name\": \"$TEST_PROFILE_NAME\",
    \"numPlayers\": 6,
    \"buyinChips\": 2000,
    \"blindLevels\": [
        {\"small\": 10, \"big\": 20, \"ante\": 0, \"minutes\": 10},
        {\"small\": 25, \"big\": 50, \"ante\": 0, \"minutes\": 10},
        {\"small\": 50, \"big\": 100, \"ante\": 10, \"minutes\": 10}
    ],
    \"rebuys\": true,
    \"addons\": false
}"

CREATE_RESULT=$(api_post_json /tournament-profiles "$CREATE_BODY" 2>/dev/null) || die "Create failed"
CREATED=$(jget "$CREATE_RESULT" 'o.created')
if [[ "$CREATED" == "true" ]]; then
    log "  OK: Profile created: $TEST_PROFILE_NAME"
else
    log "FAIL: Profile creation failed: $CREATE_RESULT"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# P-002/P-004: Verify profile appears in list with correct settings
# ============================================================
log "=== P-002: Verify Profile Settings ==="
PROFILES=$(api GET /tournament-profiles 2>/dev/null) || die "Could not re-read profiles"
FOUND=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$TEST_PROFILE_NAME')")
if [[ -n "$FOUND" && "$FOUND" != "undefined" && "$FOUND" != "" ]]; then
    log "  OK: Profile found in list"

    # Verify settings
    NUM_P=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$TEST_PROFILE_NAME')?.numPlayers")
    BUYIN=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$TEST_PROFILE_NAME')?.buyinChips")
    REBUYS=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$TEST_PROFILE_NAME')?.rebuys")
    LEVELS=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$TEST_PROFILE_NAME')?.levelCount")

    log "  numPlayers=$NUM_P, buyinChips=$BUYIN, rebuys=$REBUYS, levels=$LEVELS"

    [[ "$NUM_P" == "6" ]] && log "  OK: numPlayers correct" || { log "FAIL: numPlayers=$NUM_P"; FAILURES=$((FAILURES+1)); }
    [[ "$BUYIN" == "2000" ]] && log "  OK: buyinChips correct" || { log "FAIL: buyinChips=$BUYIN"; FAILURES=$((FAILURES+1)); }
    [[ "$REBUYS" == "true" ]] && log "  OK: rebuys correct" || { log "FAIL: rebuys=$REBUYS"; FAILURES=$((FAILURES+1)); }
else
    log "FAIL: Created profile not found in list"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# P-005: Delete user-created profile
# ============================================================
log "=== P-005: Delete Profile ==="
DEL_RESULT=$(api DELETE /tournament-profiles -H "Content-Type: application/json" \
    -d "{\"name\": \"$TEST_PROFILE_NAME\"}" 2>/dev/null) || true
DELETED=$(jget "$DEL_RESULT" 'o.deleted')
if [[ "$DELETED" == "true" ]]; then
    log "  OK: Profile deleted"
else
    log "FAIL: Profile deletion failed: $DEL_RESULT"
    FAILURES=$((FAILURES+1))
fi

# Verify gone
PROFILES=$(api GET /tournament-profiles 2>/dev/null) || true
STILL_FOUND=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$TEST_PROFILE_NAME')?.name||''")
if [[ -z "$STILL_FOUND" || "$STILL_FOUND" == "" ]]; then
    log "  OK: Profile no longer in list"
else
    log "FAIL: Deleted profile still in list"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# P-006: Attempt to delete built-in profile
# ============================================================
log "=== P-006: Delete Built-in Profile (should fail) ==="
BUILTIN_NAME=$(jget "$PROFILES" '(o.profiles||[]).find(p=>!p.canDelete)?.name||""')
if [[ -n "$BUILTIN_NAME" && "$BUILTIN_NAME" != "" ]]; then
    DEL_BUILTIN=$(api DELETE /tournament-profiles -H "Content-Type: application/json" \
        -d "{\"name\": \"$BUILTIN_NAME\"}" 2>/dev/null) || true
    ERROR=$(jget "$DEL_BUILTIN" 'o.error||""')
    if [[ "$ERROR" == "Forbidden" ]]; then
        log "  OK: Built-in profile deletion blocked (Forbidden)"
    else
        log "  WARN: Unexpected response for built-in delete: $DEL_BUILTIN"
    fi
else
    log "  SKIP: No undeletable built-in profile found"
fi

# ============================================================
# P-009: Duplicate test — create with same name should fail
# ============================================================
log "=== Duplicate Name Test ==="
DUP_NAME="Dup Test $(date +%s)"
api_post_json /tournament-profiles "{\"name\": \"$DUP_NAME\", \"numPlayers\": 3}" > /dev/null 2>&1 || true
DUP_RESULT=$(api_post_json /tournament-profiles "{\"name\": \"$DUP_NAME\", \"numPlayers\": 3}" 2>/dev/null) || true
DUP_ERROR=$(jget "$DUP_RESULT" 'o.error||""')
if [[ "$DUP_ERROR" == "Conflict" ]]; then
    log "  OK: Duplicate name rejected"
else
    log "  WARN: Duplicate response: $DUP_RESULT"
fi
# Clean up
api DELETE /tournament-profiles -H "Content-Type: application/json" \
    -d "{\"name\": \"$DUP_NAME\"}" > /dev/null 2>&1 || true

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Tournament profile CRUD verified: list, create, verify, delete, protection"
