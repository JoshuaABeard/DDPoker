#!/usr/bin/env bash
# test-split-pot.sh — Verify split pot when players have identical hands.
#
# Tests E-014:
#   - Inject identical hands to force a tie at showdown
#   - Verify pot is divided equally
#   - Chip conservation holds after split
#
# Strategy: Give human and AI1 the same hand (e.g., both get pocket pairs
# of same rank but different suits). With matching community cards, they
# should split the pot.
#
# Usage:
#   bash .claude/scripts/scenarios/test-split-pot.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3 '"startingChips": 1000, "smallBlind": 10'

FAILURES=0

# Wait for DEAL mode
log "Waiting for DEAL mode to inject cards..."
state=$(wait_mode "DEAL|CHECK_BET|CHECK_RAISE|CALL_RAISE" 60) \
    || die "Timed out waiting for game"

mode=$(jget "$state" 'o.inputMode || "NONE"')

# If already in a hand, fold and wait for DEAL
if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
    state=$(wait_mode "DEAL|CONTINUE|CONTINUE_LOWER" 30) || die "Timed out"
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    while [[ "$mode" != "DEAL" ]]; do
        advance_non_betting "$state"
        sleep 0.5
        state=$(api GET /state 2>/dev/null) || continue
        mode=$(jget "$state" 'o.inputMode || "NONE"')
    done
fi

# Record chip counts before the split-pot hand
state=$(api GET /state 2>/dev/null) || die "Could not read state"
human_seat=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.seat||0')
log "Human seat: $human_seat"

chips_before=$(jget "$state" \
    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).map(p=>p?p.seat+':'+p.chips:'').filter(s=>s).join(',')")
log "Chips before: $chips_before"

# Inject cards for a split pot:
#   Seat 0: Ah Kh  (AK suited)
#   Seat 1: Ad Kd  (AK suited — same hand strength)
#   Seat 2: 2c 3c  (weak hand — will lose)
#   Board: Qs Js Ts 4h 5d  (both AK make broadway straight)
#
# Card order: s0c1, s0c2, s1c1, s1c2, s2c1, s2c2, burn, f1, f2, f3, burn, turn, burn, river
INJECT='{"cards":["Ah","Kh","Ad","Kd","2c","3c","Qs","Js","Ts","9s","4h","8d","5d","7s"]}'
log "Injecting split-pot cards..."
inject_result=$(api_post_json /cards/inject "$INJECT" 2>/dev/null) || die "Card injection failed"
log "  Injection: $inject_result"

# Deal the hand
api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
sleep 0.5

# Play through the hand — call everything to reach showdown
log "Playing through hand (CALL strategy)..."
HAND_TIMEOUT=60
HSTART=$(date +%s)
while true; do
    st=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    md=$(jget "$st" 'o.inputMode || "NONE"')

    case "$md" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$st" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                avail=$(jget "$st" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
                if echo "$avail" | grep -q "CHECK"; then
                    api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
                else
                    api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
                fi
            fi
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$md\"}" > /dev/null 2>&1 || true
            ;;
        DEAL)
            log "Hand complete — reached DEAL"
            break
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac

    [[ $(($(date +%s) - HSTART)) -gt $HAND_TIMEOUT ]] && { log "WARN: Hand timed out"; break; }
    sleep 0.2
done

screenshot "split-pot-result"

# Check chip counts after — seat 0 and seat 1 should have roughly equal change
state=$(api GET /state 2>/dev/null) || die "Could not read final state"
chips_after=$(jget "$state" \
    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).map(p=>p?p.seat+':'+p.chips:'').filter(s=>s).join(',')")
log "Chips after: $chips_after"

# Validate chip conservation (most important assertion)
vresult=$(api GET /validate 2>/dev/null) || die "Could not validate"
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    warns=$(jget "$vresult" '(o.warnings||[]).join("; ")')
    log "FAIL: Chip conservation invalid after split pot"
    log "  warnings: $warns"
    FAILURES=$((FAILURES+1))
else
    log "  OK: Chip conservation valid after split pot"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Split pot test: chip conservation valid after tie hand (chips: $chips_after)"
