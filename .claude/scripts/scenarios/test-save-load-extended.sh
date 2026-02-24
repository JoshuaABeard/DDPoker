#!/usr/bin/env bash
# test-save-load-extended.sh — Extended strict save/load tests.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game"

log "=== SL-002: List Saves (before) ==="
SAVES_BEFORE=$(api GET /game/saves) || die "Could not list saves before"
COUNT_BEFORE=$(jget "$SAVES_BEFORE" 'o.count||0')
SAVE_DIR=$(jget "$SAVES_BEFORE" 'o.saveDir||""')
assert_json_field "$SAVES_BEFORE" 'o.saveDir||""' "save directory"
log "  INFO: saves before: $COUNT_BEFORE"
log "  INFO: save directory: $SAVE_DIR"

log "=== SL-003: Save During Gameplay ==="
SAVE_RESULT=$(api_post_json /game/save '{}') || die "Save request failed"
assert_action_accepted "$SAVE_RESULT" "initial save"
sleep 1

log "=== SL-004: Verify Save File ==="
SAVES_AFTER=$(api GET /game/saves) || die "Could not list saves after"
COUNT_AFTER=$(jget "$SAVES_AFTER" 'o.count||0')
if [[ "$COUNT_AFTER" -lt 1 ]]; then
    log "  INFO: save listing count remained $COUNT_AFTER (load verification will enforce persistence)"
else
    log "  OK: save count after first save = $COUNT_AFTER"
fi
if [[ "$COUNT_AFTER" -lt "$COUNT_BEFORE" ]]; then
    record_failure "save count regressed ($COUNT_BEFORE -> $COUNT_AFTER)"
fi

log "=== SL-005: Play and Save Again ==="
for _ in 1 2 3 4 5; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.2; continue; }
    advance_non_betting "$state"
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    if [[ "$mode" =~ ^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$ ]]; then
        is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
        if [[ "$is_human" == "true" ]]; then
            resp=$(api_post_json /action '{"type":"CALL"}' 2>/dev/null || true)
            assert_action_accepted "$resp" "progress CALL"
        fi
    fi
    sleep 0.2
done

SAVE2_RESULT=$(api_post_json /game/save '{}') || die "Second save request failed"
assert_action_accepted "$SAVE2_RESULT" "second save"
sleep 1

log "=== SL-006: Load Saved Game ==="
LOAD_RESULT=$(api_post_json /game/load '{}') || die "Load request failed"
assert_action_accepted "$LOAD_RESULT" "load"
sleep 2

log "=== SL-007: Verify Post-Load State ==="
state=$(api GET /state) || die "Could not read state after load"
mode=$(jget "$state" 'o.inputMode || "NONE"')
phase=$(jget "$state" 'o.gamePhase || "NONE"')
if [[ "$mode" == "NONE" && "$phase" == "NONE" ]]; then
    record_failure "post-load state is NONE/NONE"
else
    log "  OK: post-load state mode=$mode phase=$phase"
fi

log "=== SL-008: Validate After Load ==="
VALIDATE=$(api GET /validate) || die "Could not call /validate"
CC_VALID=$(jget "$VALIDATE" 'o.chipConservation&&o.chipConservation.valid')
IM_VALID=$(jget "$VALIDATE" 'o.inputModeConsistent')
if [[ "$CC_VALID" != "true" ]]; then
    record_failure "chipConservation.valid expected true, got $CC_VALID"
else
    log "  OK: chipConservation.valid=true"
fi
if [[ "$IM_VALID" != "true" ]]; then
    record_failure "inputModeConsistent expected true, got $IM_VALID"
else
    log "  OK: inputModeConsistent=true"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Extended save/load strict checks passed"
