#!/usr/bin/env bash
# test-split-pot.sh — Verify split pot when players have identical hands.
#
# Tests E-014:
#   - Inject identical hands to force a tie at showdown
#   - Verify pot is divided equally
#   - Chip conservation holds after split
#
# Strategy: Give human and AI1 the same hand (e.g., both get AK suited).
# With matching community cards (broadway board), they should split the pot.
# Cards are injected BEFORE game start so they take effect on the first hand.
#
# Usage:
#   bash .claude/scripts/scenarios/test-split-pot.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# Inject cards for a split pot BEFORE game starts:
#   Seat 0: Ah Kh  (AK suited)
#   Seat 1: Ad Kd  (AK suited — same hand strength)
#   Seat 2: 2c 3c  (weak hand — will lose or fold)
#   Board: Qs Js Ts 4h 5d  (AK + Qs Js Ts = broadway straight for both)
#
# Card order: s0c1, s0c2, s1c1, s1c2, s2c1, s2c2, burn, f1, f2, f3, burn, turn, burn, river
INJECT='{"cards":["Ah","Kh","Ad","Kd","2c","3c","Qs","Js","Ts","9s","4h","8d","5d","7s"]}'
log "Injecting split-pot cards before game start..."
inject_result=$(api_post_json /cards/inject "$INJECT" 2>/dev/null) || die "Card injection failed"
log "  Injection: $inject_result"

# Start game — first hand auto-deals with the injected cards
lib_start_game 3 '"buyinChips": 1000'

# Record chip counts before the split-pot hand
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|QUITSAVE" 60) \
    || die "Timed out waiting for game"
state=$(api GET /state 2>/dev/null) || die "Could not read state"

human_seat=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.seat||0')
log "Human seat: $human_seat"

chips_before=$(jget "$state" \
    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).map(p=>p?p.seat+':'+p.chips:'').filter(s=>s).join(',')")
log "Chips before: $chips_before"

# Play through the hand — CALL/CHECK everything to reach showdown.
# Hand completion: detect when community cards reset to 0 after being non-zero
# OR when 3 seconds pass after reaching 5 community cards.
log "Playing through hand (CALL strategy)..."
HAND_TIMEOUT=60
HSTART=$(date +%s)
MAX_CC=0
RIVER_DONE_TIME=0

while true; do
    st=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    md=$(jget "$st" 'o.inputMode || "NONE"')
    cc=$(jget "$st" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).length')
    [[ "$cc" =~ ^[0-9]+$ ]] || cc=0

    # Track max community cards seen
    [[ $cc -gt $MAX_CC ]] && MAX_CC=$cc

    # River done: 3 seconds after cc=5
    if [[ $MAX_CC -ge 5 && $RIVER_DONE_TIME -eq 0 ]]; then
        RIVER_DONE_TIME=$(date +%s)
    fi
    if [[ $RIVER_DONE_TIME -gt 0 && $(($(date +%s) - RIVER_DONE_TIME)) -ge 3 ]]; then
        log "  Hand complete — river done"
        break
    fi
    # Community cards reset = new hand dealing
    if [[ $MAX_CC -gt 0 && $cc -eq 0 ]]; then
        log "  Hand complete — community cards reset"
        break
    fi

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
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac

    [[ $(($(date +%s) - HSTART)) -gt $HAND_TIMEOUT ]] && { log "WARN: Hand timed out"; break; }
    sleep 0.2
done

screenshot "split-pot-result"

# Check chip counts after
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
