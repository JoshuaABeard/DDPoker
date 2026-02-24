#!/usr/bin/env bash
# test-color-up.sh — Verify color-up behavior at higher blind levels.
#
# Tests C-020 through C-024:
#   - Start 3-player game
#   - Advance to a level where color-up occurs via setLevel cheat
#   - Verify chip conservation via /validate
#   - Test gameplay.pauseColor option
#
# Usage:
#   bash .claude/scripts/scenarios/test-color-up.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

# Wait for game to start
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL" 60) \
    || die "Timed out waiting for game start"

# ============================================================
# C-020: Get baseline state at level 1
# ============================================================
log "=== C-020: Baseline State ==="
state=$(api GET /state 2>/dev/null) || die "Could not read state"
level=$(jget "$state" 'o.tournament&&o.tournament.level||1')
sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
log "  Level $level: SB=$sb, BB=$bb"

# Validate chip conservation at baseline
vresult=$(api GET /validate 2>/dev/null) || die "Could not validate"
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" == "true" ]]; then
    log "  OK: Chip conservation valid at baseline"
else
    log "FAIL: Chip conservation invalid at baseline"
    FAILURES=$((FAILURES+1))
fi

# Fold current hand if in betting mode
mode=$(jget "$state" 'o.inputMode || "NONE"')
if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
    if [[ "$is_human" == "true" ]]; then
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    fi
fi

# Wait for DEAL mode (with timeout and betting-mode fallback to avoid infinite loop)
DEAL_START=$(date +%s)
while [[ $(($(date +%s) - DEAL_START)) -lt 30 ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    case "$mode" in
        DEAL) break ;;
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            [[ "$is_human" == "true" ]] && api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
            ;;
        CONTINUE|CONTINUE_LOWER) advance_non_betting "$state" ;;
        REBUY_CHECK) api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true ;;
    esac
    sleep 0.3
done

# ============================================================
# C-021: Advance to high level for color-up
# ============================================================
log "=== C-021: Advance to Level 5 (color-up range) ==="
api_post_json /cheat '{"action":"setLevel","level":5}' > /dev/null 2>&1 \
    || log "WARN: setLevel cheat may have failed"
sleep 0.5

# Deal a hand at the new level
api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
sleep 1

state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|QUITSAVE" 30) \
    || die "Timed out after level advance"
state=$(api GET /state 2>/dev/null) || die "Could not read state"

new_level=$(jget "$state" 'o.tournament&&o.tournament.level||1')
new_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
new_bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
log "  Level $new_level: SB=$new_sb, BB=$new_bb"

if [[ "$new_sb" -gt "$sb" ]]; then
    log "  OK: Blinds increased after level advance"
else
    log "FAIL: Blinds did not increase (was $sb, now $new_sb)"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# C-022: Verify chip conservation after level advance
# ============================================================
log "=== C-022: Chip Conservation After Level Advance ==="
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
warns=$(jget "$vresult" '(o.warnings||[]).join("; ")')
if [[ "$cc_valid" == "true" ]]; then
    log "  OK: Chip conservation valid after level advance"
else
    log "FAIL: Chip conservation invalid after level advance"
    [[ -n "$warns" ]] && log "  warnings: $warns"
    FAILURES=$((FAILURES+1))
fi

screenshot "color-up-level5"

# ============================================================
# C-023: Enable pauseColor option
# ============================================================
log "=== C-023: Enable pauseColor Option ==="
api_post_json /options '{"gameplay.pauseColor": true}' > /dev/null 2>&1 \
    || { log "FAIL: Could not set pauseColor"; FAILURES=$((FAILURES+1)); }

options=$(api GET /options 2>/dev/null) || true
pause_color=$(jget "$options" 'o.gameplay&&o.gameplay.pauseColor')
if [[ "$pause_color" == "true" ]]; then
    log "  OK: pauseColor enabled"
else
    log "FAIL: pauseColor not enabled after setting"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# C-024: Advance further and play a hand
# ============================================================
log "=== C-024: Play Hand at Higher Level ==="
# Fold current hand
mode=$(jget "$state" 'o.inputMode || "NONE"')
if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
    if [[ "$is_human" == "true" ]]; then
        api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    fi
fi

# Wait for next DEAL, advancing through any continue/rebuy prompts
ADVANCE_START=$(date +%s)
while [[ $(($(date +%s) - ADVANCE_START)) -lt 30 ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    case "$mode" in
        DEAL) break ;;
        CONTINUE|CONTINUE_LOWER) advance_non_betting "$state" ;;
        REBUY_CHECK) api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true ;;
    esac
    sleep 0.3
done

# Advance to level 7
api_post_json /cheat '{"action":"setLevel","level":7}' > /dev/null 2>&1 || true
sleep 0.5

api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
sleep 1

state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|QUITSAVE|CONTINUE" 30) || true
state=$(api GET /state 2>/dev/null) || true

final_level=$(jget "$state" 'o.tournament&&o.tournament.level||1')
final_sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
log "  Level $final_level: SB=$final_sb"

# Final validation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
warns=$(jget "$vresult" '(o.warnings||[]).join("; ")')
if [[ "$cc_valid" == "true" ]]; then
    log "  OK: Final chip conservation valid"
else
    log "FAIL: Final chip conservation invalid"
    [[ -n "$warns" ]] && log "  warnings: $warns"
    FAILURES=$((FAILURES+1))
fi

# Reset pauseColor
api_post_json /options '{"gameplay.pauseColor": false}' > /dev/null 2>&1 || true

screenshot "color-up-final"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Color-up tests verified: level advance, chip conservation, pauseColor option"
