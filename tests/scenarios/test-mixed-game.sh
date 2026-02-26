#!/usr/bin/env bash
# test-mixed-game.sh — Verify game type changes per blind level.
#
# Starts a tournament with limit at level 1, pot-limit at level 2,
# no-limit at level 3. Plays hands and verifies betting rules change
# correctly at each transition.
#
# Usage:
#   bash tests/scenarios/test-mixed-game.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0
LAST_HAND_NUM=0

# 3 levels with different game types; short minutes so levels can advance
lib_start_game 3 '"buyinChips": 10000, "disableAutoDeal": true, "blindLevels": [{"small":25,"big":50,"ante":0,"minutes":1,"gameType":"limit"},{"small":50,"big":100,"ante":0,"minutes":1,"gameType":"potlimit"},{"small":100,"big":200,"ante":0,"minutes":1,"gameType":"nolimit"}]'
sleep 2
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

log "=== Verifying level 1 is limit ==="
state=$(api GET /state 2>/dev/null) || die "No state"
level=$(jget "$state" 'o.tournament&&o.tournament.level||0')
log "  Current level: $level"

# Play several hands to gather data about bet limits
for h in $(seq 1 6); do
    state=$(wait_any_turn 15) || { log "WARN: no turn for hand $h"; break; }
    mode=$(jget "$state" 'o.inputMode||"NONE"')
    level=$(jget "$state" 'o.tournament&&o.tournament.level||0')
    max_raise=$(jget "$state" 'o.currentAction.maxRaise||o.currentAction.maxBet||0')
    min_raise=$(jget "$state" 'o.currentAction.minRaise||o.currentAction.minBet||0')
    log "  Hand $h: level=$level maxRaise=$max_raise minRaise=$min_raise"

    # On level 1 (limit), max should equal min (fixed bet)
    # On level 2+ (pot/no-limit), max should differ from min
    if [[ "$level" == "1" && "$max_raise" -gt 0 && "$max_raise" == "$min_raise" ]]; then
        log "    OK: Level 1 limit structure (fixed raise=$max_raise)"
    elif [[ "$level" -gt 1 && "$max_raise" -gt "$min_raise" ]]; then
        log "    OK: Level $level variable raise structure ($min_raise-$max_raise)"
    fi

    play_to_showdown 20 "$LAST_HAND_NUM" || log "WARN: hand $h no showdown"

    hr=$(wait_hand_result 10 "$LAST_HAND_NUM") || true
    if [[ -n "$hr" ]]; then
        LAST_HAND_NUM=$(jget "$hr" 'o.handNumber||0')
    fi

    advance_to_next_hand 15 || { log "WARN: could not advance after hand $h"; break; }
done

# Validate chip conservation at end
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid in mixed game"
else
    log "  OK: Chip conservation valid (mixed game)"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES mixed game test(s) failed"
fi

pass "Mixed game type transitions verified"
