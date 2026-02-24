#!/usr/bin/env bash
# test-blind-posting.sh — Strict blind/ante/partial-blind assertions.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

FAILURES=0

restart_runtime() {
    SKIP_BUILD=true
    cleanup
    JAVA_PID=""
    PORT=""
    KEY=""
    lib_launch
}

wait_for_new_betting_hand() {
    local previous_hand="$1" timeout="${2:-45}"
    local start state mode hand
    start=$(date +%s)
    while [[ $(($(date +%s) - start)) -lt "$timeout" ]]; do
        state=$(api GET /state 2>/dev/null) || { sleep 0.2; continue; }
        mode=$(jget "$state" 'o.inputMode||"NONE"')
        hand=$(jget "$state" 'o.tournament&&o.tournament.handNumber||0')
        if [[ "$hand" -gt "$previous_hand" && "$mode" =~ ^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$ ]]; then
            printf '%s' "$state"
            return 0
        fi
        case "$mode" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                if [[ "$is_human" == "true" ]]; then
                    resp=$(api_post_json /action '{"type":"CALL"}' 2>/dev/null || true)
                    accepted=$(jget "$resp" 'o.accepted||false')
                    if [[ "$accepted" != "true" ]]; then
                        resp=$(api_post_json /action '{"type":"FOLD"}' 2>/dev/null || true)
                        assert_action_accepted "$resp" "fallback FOLD while waiting for next hand"
                    fi
                fi
                ;;
            DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                advance_non_betting "$state"
                ;;
        esac
        sleep 0.2
    done
    return 1
}

wait_for_short_stack_resolution() {
    local bb="$1" timeout="${2:-120}"
    local start state mode is_human remaining live_chips
    local saw_short=false
    start=$(date +%s)
    while [[ $(($(date +%s) - start)) -lt "$timeout" ]]; do
        state=$(api GET /state 2>/dev/null) || { sleep 0.2; continue; }
        mode=$(jget "$state" 'o.inputMode||"NONE"')
        remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')

        live_chips=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.chips||0')
        if [[ -n "$live_chips" && "$live_chips" != "undefined" && "$live_chips" != "null" ]]; then
            if [[ "$live_chips" -lt "$bb" ]]; then
                saw_short=true
            fi
        fi

        if [[ "$saw_short" == "true" && "$remaining" -le 1 ]]; then
            printf '%s' "$state"
            return 0
        fi

        if [[ "$mode" == "NONE" && "$remaining" -le 1 ]]; then
            return 1
        fi

        case "$mode" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                if [[ "$is_human" == "true" ]]; then
                    api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
                fi
                ;;
            DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                advance_non_betting "$state"
                ;;
        esac
        sleep 0.2
    done
    return 1
}

log "=== G-050: Small and Big Blind Posting ==="
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE" 60) || die "Timed out waiting for preflop"

sb=$(jget "$state" 'o.tournament&&o.tournament.smallBlind||0')
bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')
pot=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].pot||0')
bets_json=$(jget "$state" 'JSON.stringify(o.tables&&o.tables[0]&&o.tables[0].currentBets||{})')
has_sb=$(jget "$state" "Object.values(o.tables&&o.tables[0]&&o.tables[0].currentBets||{}).includes($sb)")
has_bb=$(jget "$state" "Object.values(o.tables&&o.tables[0]&&o.tables[0].currentBets||{}).includes($bb)")

if [[ "$has_sb" != "true" || "$has_bb" != "true" ]]; then
    record_failure "blind amounts not present in currentBets (SB=$sb BB=$bb currentBets=$bets_json)"
else
    log "  OK: currentBets includes SB=$sb and BB=$bb"
fi

min_pot=$((sb + bb))
if [[ "$pot" -lt "$min_pot" ]]; then
    record_failure "pot too small after blinds: pot=$pot expected>=$min_pot"
else
    log "  OK: pot after blinds = $pot"
fi

validate=$(api GET /validate) || die "Could not call /validate"
cc_valid=$(jget "$validate" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "chipConservation invalid during blind posting"
else
    log "  OK: chip conservation valid during blind posting"
fi

screenshot "blind-posting-preflop"

log "=== G-051: Ante Posting ==="
restart_runtime
lib_start_game 3 '"buyinChips":1500,"blindLevels":[{"small":25,"big":50,"ante":25,"minutes":60}]'
ante_state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE" 60) || {
    record_failure "timed out waiting for ante-enabled hand"
    ante_state=""
}
if [[ -n "$ante_state" ]]; then
    ante=$(jget "$ante_state" 'o.tournament&&o.tournament.ante||0')
    players=$(jget "$ante_state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p).length')
    pot=$(jget "$ante_state" 'o.tables&&o.tables[0]&&o.tables[0].pot||0')
    sb=$(jget "$ante_state" 'o.tournament&&o.tournament.smallBlind||0')
    bb=$(jget "$ante_state" 'o.tournament&&o.tournament.bigBlind||0')
    if [[ "$ante" -le 0 ]]; then
        record_failure "ante not enabled in custom blind structure"
    else
        log "  OK: ante reached $ante"
        expected_min=$((sb + bb + (ante * players)))
        if [[ "$pot" -lt "$expected_min" ]]; then
            record_failure "ante hand pot too small: pot=$pot expected>=$expected_min (players=$players ante=$ante)"
        else
            log "  OK: ante contribution reflected in pot (pot=$pot, min=$expected_min)"
        fi
    fi
fi

log "=== G-052: Partial Blind (cannot cover BB) ==="
restart_runtime
lib_start_game 2 '"buyinChips":40,"blindLevels":[{"small":25,"big":50,"ante":0,"minutes":60}]'
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE" 60) || die "Timed out waiting for partial-blind setup hand"
bb=$(jget "$state" 'o.tournament&&o.tournament.bigBlind||0')

next_state=$(wait_for_short_stack_resolution "$bb" 120) || record_failure "timed out waiting for short-stack blind resolution"
if [[ -n "${next_state:-}" ]]; then
    rem=$(jget "$next_state" 'o.tournament&&o.tournament.playersRemaining||0')
    live_chips=$(jget "$next_state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.chips||""')
    if [[ "$rem" -le 1 ]]; then
        log "  OK: short-stack blind sequence resolved to game completion"
    else
        record_failure "expected game completion after short-stack blinds, playersRemaining=$rem"
    fi

    if [[ -n "$live_chips" && "$live_chips" != "undefined" && "$live_chips" != "null" && "$live_chips" -ge "$bb" ]]; then
        record_failure "expected human short stack below BB at some point (current chips=$live_chips BB=$bb)"
    fi
fi

validate=$(api GET /validate) || die "Could not call /validate after partial blind"
cc_valid=$(jget "$validate" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "chipConservation invalid after short-stack scenario"
else
    log "  OK: chipConservation.valid=true after short-stack scenario"
fi

screenshot "blind-posting-partial"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Blind posting strict checks passed (blinds, antes, partial blind)"
