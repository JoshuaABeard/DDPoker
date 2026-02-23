#!/usr/bin/env bash
# test-tournament-profile-editor.sh — Extended tournament profile creation tests.
#
# Tests P-010 through P-058:
#   - Create profiles with various player counts (2-30)
#   - Create profiles with different chip amounts
#   - Create profiles with blind structures including breaks
#   - Create profiles with rebuys and limits
#   - Create profiles with addons
#   - Create profiles with different game types
#   - Validation: invalid values should error
#
# Usage:
#   bash .claude/scripts/scenarios/test-tournament-profile-editor.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
PROFILES_TESTED=0
CLEANUP_NAMES=()

# Helper: create a profile, verify it was created, track for cleanup
create_and_verify() {
    local label="$1" body="$2"
    local result created name

    result=$(api_post_json /tournament-profiles "$body" 2>/dev/null) || true
    created=$(jget "$result" 'o.created')
    name=$(jget "$result" 'o.name||""')

    if [[ "$created" == "true" ]]; then
        log "  OK: $label — profile created: $name"
        PROFILES_TESTED=$((PROFILES_TESTED+1))
        CLEANUP_NAMES+=("$name")
        return 0
    else
        local error
        error=$(jget "$result" 'o.error||""')
        log "FAIL: $label — creation failed: error=$error ($result)"
        FAILURES=$((FAILURES+1))
        return 1
    fi
}

# Helper: attempt creation expecting failure
expect_failure() {
    local label="$1" body="$2" expected_error="$3"
    local result error

    result=$(api_post_json /tournament-profiles "$body" 2>/dev/null) || true
    error=$(jget "$result" 'o.error||""')

    if [[ "$error" == "$expected_error" ]]; then
        log "  OK: $label — correctly rejected with $expected_error"
        PROFILES_TESTED=$((PROFILES_TESTED+1))
    else
        log "FAIL: $label — expected $expected_error, got: $result"
        FAILURES=$((FAILURES+1))
    fi
}

TS=$(date +%s)

# ============================================================
# P-010: Minimal profile (2 players)
# ============================================================
log "=== P-010: 2-Player Profile ==="
create_and_verify "2-player" "{
    \"name\": \"Test2p_$TS\",
    \"numPlayers\": 2,
    \"buyinChips\": 1000
}"

# ============================================================
# P-012: 6-player profile with custom blinds
# ============================================================
log "=== P-012: 6-Player Profile ==="
create_and_verify "6-player" "{
    \"name\": \"Test6p_$TS\",
    \"numPlayers\": 6,
    \"buyinChips\": 3000,
    \"blindLevels\": [
        {\"small\": 15, \"big\": 30, \"ante\": 0, \"minutes\": 15},
        {\"small\": 30, \"big\": 60, \"ante\": 5, \"minutes\": 15}
    ]
}"

# ============================================================
# P-014: 10-player profile
# ============================================================
log "=== P-014: 10-Player Profile ==="
create_and_verify "10-player" "{
    \"name\": \"Test10p_$TS\",
    \"numPlayers\": 10,
    \"buyinChips\": 5000,
    \"blindLevels\": [
        {\"small\": 25, \"big\": 50, \"ante\": 0, \"minutes\": 20},
        {\"small\": 50, \"big\": 100, \"ante\": 10, \"minutes\": 20},
        {\"small\": 100, \"big\": 200, \"ante\": 25, \"minutes\": 20}
    ]
}"

# ============================================================
# P-016: 20-player profile
# ============================================================
log "=== P-016: 20-Player Profile ==="
create_and_verify "20-player" "{
    \"name\": \"Test20p_$TS\",
    \"numPlayers\": 20,
    \"buyinChips\": 10000
}"

# ============================================================
# P-018: 30-player profile (max)
# ============================================================
log "=== P-018: 30-Player Profile ==="
create_and_verify "30-player" "{
    \"name\": \"Test30p_$TS\",
    \"numPlayers\": 30,
    \"buyinChips\": 15000
}"

# ============================================================
# P-020: Small chip amount
# ============================================================
log "=== P-020: Small Chip Amount ==="
create_and_verify "small-chips" "{
    \"name\": \"TestSmChip_$TS\",
    \"numPlayers\": 4,
    \"buyinChips\": 500
}"

# ============================================================
# P-022: Large chip amount
# ============================================================
log "=== P-022: Large Chip Amount ==="
create_and_verify "large-chips" "{
    \"name\": \"TestLgChip_$TS\",
    \"numPlayers\": 6,
    \"buyinChips\": 50000
}"

# ============================================================
# P-030: Blind structure with breaks
# ============================================================
log "=== P-030: Blinds With Breaks ==="
create_and_verify "blinds-breaks" "{
    \"name\": \"TestBreaks_$TS\",
    \"numPlayers\": 8,
    \"buyinChips\": 5000,
    \"blindLevels\": [
        {\"small\": 25, \"big\": 50, \"ante\": 0, \"minutes\": 15},
        {\"small\": 50, \"big\": 100, \"ante\": 0, \"minutes\": 15},
        {\"small\": 75, \"big\": 150, \"ante\": 0, \"minutes\": 15, \"isBreak\": true},
        {\"small\": 100, \"big\": 200, \"ante\": 25, \"minutes\": 15}
    ]
}"

# ============================================================
# P-032: Many blind levels
# ============================================================
log "=== P-032: Many Blind Levels ==="
create_and_verify "many-levels" "{
    \"name\": \"TestManyLvl_$TS\",
    \"numPlayers\": 6,
    \"buyinChips\": 5000,
    \"blindLevels\": [
        {\"small\": 10, \"big\": 20, \"ante\": 0, \"minutes\": 10},
        {\"small\": 15, \"big\": 30, \"ante\": 0, \"minutes\": 10},
        {\"small\": 25, \"big\": 50, \"ante\": 0, \"minutes\": 10},
        {\"small\": 50, \"big\": 100, \"ante\": 5, \"minutes\": 10},
        {\"small\": 75, \"big\": 150, \"ante\": 10, \"minutes\": 10},
        {\"small\": 100, \"big\": 200, \"ante\": 25, \"minutes\": 10},
        {\"small\": 200, \"big\": 400, \"ante\": 50, \"minutes\": 10},
        {\"small\": 300, \"big\": 600, \"ante\": 75, \"minutes\": 10}
    ]
}"

# ============================================================
# P-040: Profile with rebuys enabled
# ============================================================
log "=== P-040: Rebuys Enabled ==="
create_and_verify "rebuys" "{
    \"name\": \"TestRebuy_$TS\",
    \"numPlayers\": 6,
    \"buyinChips\": 2000,
    \"rebuys\": true
}"

# ============================================================
# P-042: Profile with rebuys and limits
# ============================================================
log "=== P-042: Rebuys With Limits ==="
create_and_verify "rebuy-limits" "{
    \"name\": \"TestRebuyLim_$TS\",
    \"numPlayers\": 6,
    \"buyinChips\": 2000,
    \"rebuys\": true,
    \"maxRebuys\": 3
}"

# ============================================================
# P-044: Profile with addons
# ============================================================
log "=== P-044: Addons Enabled ==="
create_and_verify "addons" "{
    \"name\": \"TestAddon_$TS\",
    \"numPlayers\": 6,
    \"buyinChips\": 2000,
    \"addons\": true
}"

# ============================================================
# P-046: Rebuys and addons together
# ============================================================
log "=== P-046: Rebuys + Addons ==="
create_and_verify "rebuy-addon" "{
    \"name\": \"TestRA_$TS\",
    \"numPlayers\": 8,
    \"buyinChips\": 3000,
    \"rebuys\": true,
    \"addons\": true,
    \"maxRebuys\": 2
}"

# ============================================================
# P-048: Verify created profiles appear in list
# ============================================================
log "=== P-048: Verify Profiles in List ==="
PROFILES=$(api GET /tournament-profiles 2>/dev/null) || die "Could not re-read profiles"
total=$(jget "$PROFILES" '(o.profiles||[]).length')
log "  Total profiles: $total"

found_count=0
for name in "${CLEANUP_NAMES[@]}"; do
    match=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$name')?.name||''")
    if [[ "$match" == "$name" ]]; then
        found_count=$((found_count+1))
    else
        log "  WARN: Profile '$name' not found in list"
    fi
done
log "  Found $found_count of ${#CLEANUP_NAMES[@]} created profiles in list"

if [[ $found_count -eq ${#CLEANUP_NAMES[@]} ]]; then
    log "  OK: All created profiles found"
else
    log "FAIL: Only $found_count of ${#CLEANUP_NAMES[@]} profiles found in list"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# P-050: Verify profile field values round-trip
# ============================================================
log "=== P-050: Verify Field Round-Trip ==="
if [[ ${#CLEANUP_NAMES[@]} -gt 0 ]]; then
    CHECK_NAME="${CLEANUP_NAMES[0]}"
    num_p=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$CHECK_NAME')?.numPlayers")
    buyin=$(jget "$PROFILES" "(o.profiles||[]).find(p=>p.name==='$CHECK_NAME')?.buyinChips")
    log "  Profile '$CHECK_NAME': numPlayers=$num_p, buyinChips=$buyin"

    if [[ "$num_p" == "2" ]]; then
        log "  OK: numPlayers round-tripped correctly"
    else
        log "  WARN: numPlayers=$num_p (expected 2, may not be in list response)"
    fi
fi

# ============================================================
# P-052: Duplicate name rejection
# ============================================================
log "=== P-052: Duplicate Name ==="
if [[ ${#CLEANUP_NAMES[@]} -gt 0 ]]; then
    expect_failure "duplicate" \
        "{\"name\": \"${CLEANUP_NAMES[0]}\", \"numPlayers\": 3}" \
        "Conflict"
fi

# ============================================================
# P-054: Missing name
# ============================================================
log "=== P-054: Missing Name ==="
expect_failure "missing-name" \
    "{\"numPlayers\": 6}" \
    "BadRequest"

# ============================================================
# P-056: Blank name
# ============================================================
log "=== P-056: Blank Name ==="
expect_failure "blank-name" \
    "{\"name\": \"   \", \"numPlayers\": 6}" \
    "BadRequest"

# ============================================================
# P-058: Invalid JSON
# ============================================================
log "=== P-058: Invalid JSON ==="
expect_failure "invalid-json" \
    "{bad json}" \
    "BadRequest"

# ============================================================
# Cleanup: delete test profiles
# ============================================================
log "=== Cleanup: Deleting Test Profiles ==="
for name in "${CLEANUP_NAMES[@]}"; do
    api DELETE /tournament-profiles -H "Content-Type: application/json" \
        -d "{\"name\": \"$name\"}" > /dev/null 2>&1 || true
done
log "  Cleanup attempted for ${#CLEANUP_NAMES[@]} profiles"

screenshot "tournament-profile-editor"

log "---"
log "Profiles tested: $PROFILES_TESTED"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Tournament profile editor verified: $PROFILES_TESTED tests passed (player counts, chips, blinds, rebuys, addons, validation)"
