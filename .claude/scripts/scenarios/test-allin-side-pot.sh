#!/usr/bin/env bash
# test-allin-side-pot.sh — Strict all-in/side-pot verification using handResult.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

restart_runtime() {
    SKIP_BUILD=true
    cleanup
    JAVA_PID=""
    PORT=""
    KEY=""
    lib_launch
}

progress_until_hand_result() {
    local target_hand="$1" timeout="${2:-75}"
    local start state mode
    start=$(date +%s)
    while [[ $(($(date +%s) - start)) -lt "$timeout" ]]; do
        state=$(api GET /state 2>/dev/null) || { sleep 0.2; continue; }
        mode=$(jget "$state" 'o.inputMode||"NONE"')
        case "$mode" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                if [[ "$is_human" == "true" ]]; then
                    resp=$(api_post_json /action '{"type":"CALL"}' 2>/dev/null || true)
                    accepted=$(jget "$resp" 'o.accepted||false')
                    if [[ "$accepted" != "true" ]]; then
                        resp=$(api_post_json /action '{"type":"FOLD"}' 2>/dev/null || true)
                        assert_action_accepted "$resp" "fallback FOLD while progressing"
                    fi
                fi
                ;;
            DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                advance_non_betting "$state"
                ;;
        esac

        hr_hand=$(jget "$state" 'o.handResult&&o.handResult.handNumber||0')
        if [[ "$hr_hand" -ge "$target_hand" ]]; then
            printf '%s' "$state"
            return 0
        fi
        sleep 0.2
    done
    return 1
}

screenshot "side-pot-start"

side_pot_found=false
max_attempts=20
result_state=""

for attempt in $(seq 1 "$max_attempts"); do
    log "=== Attempt $attempt/$max_attempts: force all-in hand ==="
    restart_runtime
    lib_start_game 3 '"buyinChips":500,"blindLevels":[{"small":25,"big":50,"ante":0,"minutes":60}]'

    state=$(wait_human_turn 60) || {
        log "  INFO: timed out waiting for human turn; retrying"
        continue
    }
    hand_number=$(jget "$state" 'o.tournament&&o.tournament.handNumber||0')

    human_seat=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.seat||0')
    ai_seats=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p&&!p.isHuman).map(p=>p.seat).join(",")')
    ai_short=$(printf '%s' "$ai_seats" | cut -d',' -f1)
    ai_deep=$(printf '%s' "$ai_seats" | cut -d',' -f2)
    if [[ -z "$ai_short" || -z "$ai_deep" ]]; then
        log "  INFO: could not identify AI seats; retrying"
        continue
    fi

    set_human=$(api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$human_seat,\"amount\":80}" 2>/dev/null || true)
    set_short=$(api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$ai_short,\"amount\":60}" 2>/dev/null || true)
    set_deep=$(api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$ai_deep,\"amount\":500}" 2>/dev/null || true)
    if [[ "$(jget "$set_human" 'o.accepted||false')" != "true" || "$(jget "$set_short" 'o.accepted||false')" != "true" || "$(jget "$set_deep" 'o.accepted||false')" != "true" ]]; then
        log "  INFO: failed to set attempt stacks; retrying"
        continue
    fi

    resp=$(api_post_json /action '{"type":"ALL_IN"}' 2>/dev/null || true)
    accepted=$(jget "$resp" 'o.accepted||false')
    if [[ "$accepted" != "true" ]]; then
        resp=$(api_post_json /action '{"type":"CALL"}' 2>/dev/null || true)
        assert_action_accepted "$resp" "fallback CALL"
    else
        log "  OK: ALL_IN accepted"
    fi

    target=$((hand_number + 1))
    state=$(progress_until_hand_result "$target" 75) || {
        log "  INFO: timed out waiting for completed handResult >= $target; retrying"
        continue
    }

    hr_pots=$(jget "$state" '(o.handResult&&o.handResult.potBreakdown||[]).length')
    hr_hand=$(jget "$state" 'o.handResult&&o.handResult.handNumber||0')
    if [[ "$hr_hand" -lt "$target" ]]; then
        log "  INFO: handResult did not advance to target hand (expected >=$target got $hr_hand)"
        continue
    fi

    if [[ "$hr_pots" -ge 2 ]]; then
        side_pot_found=true
        result_state="$state"
        log "  OK: side pot observed (potBreakdown count=$hr_pots)"
        break
    fi

    log "  INFO: no side pot this attempt (potBreakdown count=$hr_pots), retrying"
done

if [[ "$side_pot_found" != "true" ]]; then
    record_failure "could not produce a side-pot hand within $max_attempts attempts"
fi

state="$result_state"
if [[ -z "$state" ]]; then
    state=$(api GET /state) || die "Could not read /state for final assertions"
fi

log "=== Strict handResult assertions ==="
hr_exists=$(jget "$state" '!!o.handResult')
if [[ "$hr_exists" != "true" ]]; then
    record_failure "handResult missing from /state"
fi

winners_count=$(jget "$state" '(o.handResult&&o.handResult.winners||[]).length')
if [[ "$winners_count" -le 0 ]]; then
    record_failure "handResult.winners empty"
else
    log "  OK: handResult winners count = $winners_count"
fi

if [[ "$hr_exists" == "true" ]]; then
    winner_meta_ok=$(jget "$state" '(o.handResult&&o.handResult.winners||[]).every(w=>w&&((((w.cards||[]).length===0)&&(!w.handClass||w.handClass.length===0||w.handClass==="UNKNOWN")&&(!w.handDescription||w.handDescription.length===0||w.handDescription==="Unknown"))||((w.handClass&&w.handClass.length>0)&&(w.handDescription&&w.handDescription.length>0))))')
    if [[ "$winner_meta_ok" != "true" ]]; then
        record_failure "winner handClass/handDescription missing"
    else
        log "  OK: winner handClass + handDescription present"
    fi

    pot_winner_amounts_ok=$(jget "$state" '(o.handResult&&o.handResult.potBreakdown||[]).every(p=>p&&Array.isArray(p.winners)&&p.winners.length>0&&p.winners.every(w=>w.amount>0))')
    if [[ "$pot_winner_amounts_ok" != "true" ]]; then
        record_failure "potBreakdown winners/amounts invalid"
    else
        log "  OK: each pot has winner allocations"
    fi

    pot_total_sum=$(jget "$state" '(o.handResult&&o.handResult.potBreakdown||[]).reduce((s,p)=>s+(p.totalAmount||0),0)')
    pot_award_sum=$(jget "$state" '(o.handResult&&o.handResult.potBreakdown||[]).reduce((s,p)=>s+((p.winners||[]).reduce((ps,w)=>ps+(w.amount||0),0)),0)')
    if [[ "$pot_total_sum" -ne "$pot_award_sum" ]]; then
        record_failure "pot total mismatch: totals=$pot_total_sum awards=$pot_award_sum"
    else
        log "  OK: pot totals match awarded amounts ($pot_total_sum)"
    fi

    payout_count=$(jget "$state" '(o.handResult&&o.handResult.payoutDeltas||[]).length')
    payout_consistent=$(jget "$state" '(o.handResult&&o.handResult.payoutDeltas||[]).every(d=>d&&(d.endChips-d.startChips)===(d.delta||0))')
    if [[ "$payout_count" -le 0 ]]; then
        record_failure "payoutDeltas missing"
    elif [[ "$payout_consistent" != "true" ]]; then
        record_failure "payoutDeltas are internally inconsistent"
    else
        log "  OK: payoutDeltas present and internally consistent"
    fi
fi

screenshot "side-pot-resolved"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "All-in side-pot strict checks passed"
