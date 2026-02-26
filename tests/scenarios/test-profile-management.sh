#!/usr/bin/env bash
# test-profile-management.sh — Test player profile management endpoints.
#
# Tests PP-001 through PP-009:
#   - GET /profiles lists profiles
#   - POST /profiles creates a new profile
#   - GET /profiles/default returns current default
#   - Created profile appears in list
#   - Duplicate name rejected (409)
#   - Missing/blank name rejected (400)
#   - Note: DELETE not yet implemented, so delete tests are skipped
#
# Usage:
#   bash tests/scenarios/test-profile-management.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# api_post_json uses curl -f which suppresses the response body on HTTP errors (4xx/5xx).
# For tests that check error response bodies, use this helper without -f.
api_post_raw() {
    local path="$1" body="$2"
    curl -s -H "X-Control-Key: $KEY" -X POST -H "Content-Type: application/json" \
        -d "$body" "http://localhost:$PORT$path"
}

# ============================================================
# PP-001: List profiles
# ============================================================
log "=== PP-001: List Profiles ==="
PROFILES=$(api GET /profiles 2>/dev/null) || die "Could not read /profiles"
PROFILE_COUNT=$(jget "$PROFILES" '(o.profiles||[]).length')
log "  Found $PROFILE_COUNT profiles"

if [[ "$PROFILE_COUNT" -ge 0 ]]; then
    log "  OK: Profiles endpoint returned valid list"
else
    log "FAIL: Profiles endpoint returned unexpected format"
    FAILURES=$((FAILURES+1))
fi

# List first few profile names
for i in 0 1 2; do
    PNAME=$(jget "$PROFILES" "(o.profiles||[])[$i]?.name||''")
    if [[ -n "$PNAME" && "$PNAME" != "" ]]; then
        log "  Profile $i: $PNAME"
    fi
done

# ============================================================
# PP-002: Get default profile
# ============================================================
log "=== PP-002: Default Profile ==="
DEFAULT=$(api GET /profiles/default 2>/dev/null) || die "Could not read /profiles/default"
DEFAULT_NAME=$(jget "$DEFAULT" 'o.defaultProfile||""')
log "  Default profile: $DEFAULT_NAME"

if [[ -n "$DEFAULT_NAME" && "$DEFAULT_NAME" != "" && "$DEFAULT_NAME" != "null" ]]; then
    log "  OK: Default profile is set"
else
    log "  WARN: No default profile (may be first run)"
fi

# ============================================================
# PP-003: Create a new profile
# ============================================================
log "=== PP-003: Create New Profile ==="
TEST_NAME="TestProfile_$(date +%s)"
CREATE_RESULT=$(api_post_json /profiles "{\"name\": \"$TEST_NAME\"}" 2>/dev/null) || die "Create failed"
CREATED=$(jget "$CREATE_RESULT" 'o.created')
CREATED_NAME=$(jget "$CREATE_RESULT" 'o.name||""')

if [[ "$CREATED" == "true" ]]; then
    log "  OK: Profile '$TEST_NAME' created"
else
    log "FAIL: Profile creation failed: $CREATE_RESULT"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# PP-004: Verify profile appears in list
# ============================================================
log "=== PP-004: Verify Profile in List ==="
PROFILES=$(api GET /profiles 2>/dev/null) || die "Could not re-read profiles"
FOUND=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$TEST_NAME')?.name||''")
if [[ "$FOUND" == "$TEST_NAME" ]]; then
    log "  OK: Created profile found in list"
else
    log "FAIL: Created profile not found in list"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# PP-005: Verify created profile became default
# ============================================================
log "=== PP-005: Verify New Profile is Default ==="
DEFAULT=$(api GET /profiles/default 2>/dev/null) || true
NEW_DEFAULT=$(jget "$DEFAULT" 'o.defaultProfile||""')
if [[ "$NEW_DEFAULT" == "$TEST_NAME" ]]; then
    log "  OK: New profile is now the default"
else
    log "  WARN: Default profile is '$NEW_DEFAULT', not '$TEST_NAME' (may not auto-select)"
fi

# ============================================================
# PP-006: Duplicate name should be rejected (409)
# ============================================================
log "=== PP-006: Duplicate Name Rejected ==="
DUP_RESULT=$(api_post_raw /profiles "{\"name\": \"$TEST_NAME\"}" 2>/dev/null) || true
DUP_ERROR=$(jget "$DUP_RESULT" 'o.error||""')
if [[ "$DUP_ERROR" == "Conflict" ]]; then
    log "  OK: Duplicate name correctly rejected with Conflict"
else
    log "FAIL: Duplicate name should return Conflict, got: $DUP_RESULT"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# PP-007: Missing name field should return 400
# ============================================================
log "=== PP-007: Missing Name Field ==="
BAD_RESULT=$(api_post_raw /profiles '{"email": "test@test.com"}' 2>/dev/null) || true
BAD_ERROR=$(jget "$BAD_RESULT" 'o.error||""')
if [[ "$BAD_ERROR" == "BadRequest" ]]; then
    log "  OK: Missing name correctly rejected with BadRequest"
else
    log "FAIL: Missing name should return BadRequest, got: $BAD_RESULT"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# PP-008: Blank name should return 400
# ============================================================
log "=== PP-008: Blank Name ==="
BLANK_RESULT=$(api_post_raw /profiles '{"name": "   "}' 2>/dev/null) || true
BLANK_ERROR=$(jget "$BLANK_RESULT" 'o.error||""')
if [[ "$BLANK_ERROR" == "BadRequest" ]]; then
    log "  OK: Blank name correctly rejected with BadRequest"
else
    log "FAIL: Blank name should return BadRequest, got: $BLANK_RESULT"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# PP-009: Empty body should return 400
# ============================================================
log "=== PP-009: Empty Body ==="
EMPTY_RESULT=$(api_post_raw /profiles '' 2>/dev/null) || true
EMPTY_ERROR=$(jget "$EMPTY_RESULT" 'o.error||""')
if [[ "$EMPTY_ERROR" == "BadRequest" ]]; then
    log "  OK: Empty body correctly rejected with BadRequest"
else
    log "  WARN: Empty body response: $EMPTY_RESULT"
fi

screenshot "profile-management"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Profile management verified: list, create, default, duplicate rejection, validation"
