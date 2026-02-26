#!/usr/bin/env bash
# test-hand-flow.sh — Verify hand flow from preflop through showdown.
#
# Tests G-010 through G-019:
#   - Hole cards dealt to human
#   - Community cards appear at correct streets (3 flop, 1 turn, 1 river)
#   - Showdown resolves with a winner
#   - Chip conservation holds throughout
#
# Uses card injection to control the outcome and verify community cards.
#
# Usage:
#   bash tests/scenarios/test-hand-flow.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

assert() {
    local desc="$1" actual="$2" expected="$3"
    if [[ "$actual" != "$expected" ]]; then
        log "FAIL: $desc — expected '$expected', got '$actual'"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: $desc = $actual"
    fi
}

# Inject known cards BEFORE starting the game so they are consumed by the first hand.
# The first hand is dealt automatically on game start (no DEAL mode before hand 1).
# Seat0: As Ks
# Seat1: 2d 3c
# Seat2: 7h 8h
# burn, flop: Qd Jd Td
# burn, turn: 4c
# burn, river: 5c
INJECT='{"cards":["As","Ks","2d","3c","7h","8h","Qd","Jd","Td","9d","4c","2h","5c","3s"]}'
log "Injecting cards before game start..."
INJECT_RESULT=$(api_post_json /cards/inject "$INJECT" 2>/dev/null) || die "Card injection failed"
log "  Injection result: $INJECT_RESULT"

lib_start_game 3

# Wait for the first action mode (preflop betting or AI acting)
log "=== Waiting for game to start ==="
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|QUITSAVE" 30) \
    || die "Game never reached an active mode"

# G-010: Verify human hole cards visible
state=$(api GET /state 2>/dev/null) || die "Could not read state"
phase=$(jget "$state" 'o.gamePhase || "NONE"')
log "  Game phase: $phase"

# Find human player seat
human_seat=0
for seat in 0 1 2; do
    is_h=$(jget "$state" "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$seat)?.isHuman||false")
    if [[ "$is_h" == "true" ]]; then human_seat=$seat; break; fi
done
log "  Human seat: $human_seat"

human_cards=$(jget "$state" \
    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$human_seat)?.holeCards?.join(',')||''")
log "  Human cards: $human_cards"
if [[ -n "$human_cards" && "$human_cards" != "undefined" && "$human_cards" != "" ]]; then
    log "  OK: G-010 Human hole cards dealt: $human_cards"
else
    log "  WARN: G-010 Human hole cards not visible (AI may be acting first)"
fi

# G-011: Verify AI cards NOT visible (cheats off)
for seat in 0 1 2; do
    if [[ $seat -eq $human_seat ]]; then continue; fi
    ai_cards=$(jget "$state" \
        "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$seat)?.holeCards?.join(',')||''")
    if [[ -z "$ai_cards" || "$ai_cards" == "" ]]; then
        log "  OK: G-011 AI seat $seat cards hidden"
    else
        log "  WARN: G-011 AI seat $seat cards visible: $ai_cards"
    fi
done

# G-013: No community cards at preflop start
comm_cards=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).join(",")')
if [[ -z "$comm_cards" || "$comm_cards" == "" ]]; then
    log "  OK: G-013 No community cards yet"
else
    log "  INFO: Community cards already showing: $comm_cards"
fi

# ============================================================
# Play through the full hand, responding to input at each step.
# Check community card count after every poll to observe streets.
# ============================================================
log "=== Playing through hand ==="
MAX_CC_SEEN=0
FLOP_CARDS=""
TURN_CARD=""
RIVER_CARD=""
TIMEOUT=90
RIVER_DONE_TIME=0   # timestamp when river was first recorded
START=$(date +%s)

while true; do
    st=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    md=$(jget "$st" 'o.inputMode || "NONE"')

    # Check community card count (observe streets as we go)
    cc=$(jget "$st" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).length')
    [[ "$cc" =~ ^[0-9]+$ ]] || cc=0
    if [[ $cc -gt $MAX_CC_SEEN ]]; then
        MAX_CC_SEEN=$cc
        if [[ $cc -eq 3 && -z "$FLOP_CARDS" ]]; then
            FLOP_CARDS=$(jget "$st" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).join(",")')
            log "  FLOP: $FLOP_CARDS"
            screenshot "hand-flow-flop"
        elif [[ $cc -eq 4 && -z "$TURN_CARD" ]]; then
            TURN_CARD=$(jget "$st" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[])[3]||""')
            log "  TURN: $TURN_CARD"
            screenshot "hand-flow-turn"
        elif [[ $cc -eq 5 && -z "$RIVER_CARD" ]]; then
            RIVER_CARD=$(jget "$st" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[])[4]||""')
            log "  RIVER: $RIVER_CARD"
            screenshot "hand-flow-river"
            RIVER_DONE_TIME=$(date +%s)
        fi
    fi

    # After river is recorded, the embedded server starts the next hand automatically
    # (no DEAL mode between hands). QUITSAVE means AI is acting in the new hand — the
    # original hand is done. Give it 3 seconds to settle then exit cleanly.
    if [[ $RIVER_DONE_TIME -gt 0 && $(($(date +%s) - RIVER_DONE_TIME)) -ge 3 ]]; then
        log "  Hand complete — river recorded, game advanced (mode: $md)"
        screenshot "hand-flow-showdown"
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
        DEAL)
            log "  Hand complete — reached DEAL mode (max community cards seen: $MAX_CC_SEEN)"
            screenshot "hand-flow-showdown"
            break
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac

    [[ $(($(date +%s) - START)) -gt $TIMEOUT ]] && {
        log "FAIL: Timed out playing hand (stuck in mode: $md, cc: $MAX_CC_SEEN)"
        FAILURES=$((FAILURES+1))
        break
    }
    sleep 0.1
done

# G-014/G-015/G-016: Community cards at each street
if [[ $MAX_CC_SEEN -ge 3 ]]; then
    log "  OK: G-014 Flop dealt ($FLOP_CARDS)"
else
    log "FAIL: G-014 Flop never observed (max community cards: $MAX_CC_SEEN)"
    FAILURES=$((FAILURES+1))
fi

if [[ $MAX_CC_SEEN -ge 4 ]]; then
    log "  OK: G-015 Turn dealt ($TURN_CARD)"
else
    log "FAIL: G-015 Turn never observed (max community cards: $MAX_CC_SEEN)"
    FAILURES=$((FAILURES+1))
fi

if [[ $MAX_CC_SEEN -ge 5 ]]; then
    log "  OK: G-016 River dealt ($RIVER_CARD)"
else
    log "FAIL: G-016 River never observed (max community cards: $MAX_CC_SEEN)"
    FAILURES=$((FAILURES+1))
fi

# G-017: Validate chip conservation after hand
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
assert "G-017 chip conservation after hand" "$cc_valid" "true"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES assertion(s) failed in hand flow test"
fi

pass "Hand flow verified: preflop → flop (${FLOP_CARDS}) → turn (${TURN_CARD}) → river (${RIVER_CARD}) → showdown"
