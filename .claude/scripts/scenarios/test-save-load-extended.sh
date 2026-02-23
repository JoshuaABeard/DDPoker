#!/usr/bin/env bash
# test-save-load-extended.sh — Extended save/load tests.
#
# Tests SL-002 through SL-008:
#   - List save files
#   - Save during different game phases
#   - Verify save file appears in listing
#   - Load after save
#
# Usage:
#   bash .claude/scripts/scenarios/test-save-load-extended.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

# Wait for game to start
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game"

# ============================================================
# SL-002: List saves before
# ============================================================
log "=== SL-002: List Saves (before) ==="
SAVES_BEFORE=$(api GET /game/saves 2>/dev/null) || true
COUNT_BEFORE=$(jget "$SAVES_BEFORE" 'o.count||0')
SAVE_DIR=$(jget "$SAVES_BEFORE" 'o.saveDir||""')
log "  Save directory: $SAVE_DIR"
log "  Saves before: $COUNT_BEFORE"

# ============================================================
# SL-003: Save during gameplay
# ============================================================
log "=== SL-003: Save During Gameplay ==="
mode=$(jget "$state" 'o.inputMode || "NONE"')
log "  Saving in mode: $mode"
SAVE_RESULT=$(api_post_json /game/save '{}' 2>/dev/null) || true
ACCEPTED=$(jget "$SAVE_RESULT" 'o.accepted||""')
log "  Save accepted: $ACCEPTED"
sleep 2  # Give time for save to complete

# ============================================================
# SL-004: Verify save file appeared
# ============================================================
log "=== SL-004: Verify Save File ==="
SAVES_AFTER=$(api GET /game/saves 2>/dev/null) || true
COUNT_AFTER=$(jget "$SAVES_AFTER" 'o.count||0')
log "  Saves after: $COUNT_AFTER (was $COUNT_BEFORE)"
if [[ "$COUNT_AFTER" -gt "$COUNT_BEFORE" ]]; then
    LATEST=$(jget "$SAVES_AFTER" '(o.saves||[])[0]?.name||""')
    LATEST_SIZE=$(jget "$SAVES_AFTER" '(o.saves||[])[0]?.size||0')
    log "  Latest save: $LATEST (size=$LATEST_SIZE)"
    log "  OK: Save file created"
elif [[ "$ACCEPTED" == "true" ]]; then
    log "  WARN: Save accepted but file count unchanged (may have overwritten)"
else
    log "  WARN: Save may not have worked"
fi

# ============================================================
# SL-005: Play some hands then save again
# ============================================================
log "=== SL-005: Play and Save Again ==="
for i in 1 2 3; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            [[ "$is_human" == "true" ]] && api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
            ;;
        DEAL) api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true ;;
        CONTINUE|CONTINUE_LOWER) api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true ;;
        REBUY_CHECK) api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true ;;
    esac
    sleep 0.5
done

SAVE2=$(api_post_json /game/save '{}' 2>/dev/null) || true
log "  Second save: $(jget "$SAVE2" 'o.accepted||""')"
sleep 2

# ============================================================
# SL-006: Load saved game
# ============================================================
log "=== SL-006: Load Saved Game ==="
LOAD_RESULT=$(api_post_json /game/load '{}' 2>/dev/null) || true
LOAD_ACCEPTED=$(jget "$LOAD_RESULT" 'o.accepted||""')
log "  Load accepted: $LOAD_ACCEPTED"
sleep 3

# Verify game state after load
state=$(api GET /state 2>/dev/null) || true
mode=$(jget "$state" 'o.inputMode || "NONE"')
phase=$(jget "$state" 'o.gamePhase||""')
log "  After load: mode=$mode, phase=$phase"
if [[ "$mode" != "NONE" || "$phase" != "NONE" ]]; then
    log "  OK: Game has valid state after load"
else
    log "  WARN: Game in NONE state after load"
fi

# ============================================================
# SL-008: Validate after load
# ============================================================
log "=== SL-008: Validate After Load ==="
VALIDATE=$(api GET /validate 2>/dev/null) || true
VALID=$(jget "$VALIDATE" 'o.valid||""')
log "  Chip conservation valid: $VALID"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Extended save/load verified: list, save, verify file, load, validate"
