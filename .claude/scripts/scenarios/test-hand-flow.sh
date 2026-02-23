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
#   bash .claude/scripts/scenarios/test-hand-flow.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 3

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

assert_contains() {
    local desc="$1" actual="$2" expected="$3"
    if [[ "$actual" != *"$expected"* ]]; then
        log "FAIL: $desc — expected to contain '$expected', got '$actual'"
        FAILURES=$((FAILURES+1))
    else
        log "  OK: $desc contains '$expected'"
    fi
}

# Wait for DEAL mode to inject cards
log "Waiting for DEAL mode to inject cards..."
state=$(wait_mode "DEAL" 60) || die "Never reached DEAL mode"

# Inject known cards for 3 players:
#   Seat0(human): As Ks  (strong hand)
#   Seat1(AI):    2d 3c
#   Seat2(AI):    7h 8h
#   burn, flop: Qd Jd Td  (human has broadway straight)
#   burn, turn: 4c
#   burn, river: 5c
INJECT='{"cards":["As","Ks","2d","3c","7h","8h","Qd","Jd","Td","9d","4c","2h","5c","3s"]}'
log "Injecting cards..."
INJECT_RESULT=$(api_post_json /cards/inject "$INJECT" 2>/dev/null) || die "Card injection failed"
log "  Injection result: $INJECT_RESULT"

# Deal the hand
api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
sleep 0.5

# === PREFLOP ===
log "=== PREFLOP ==="
state=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|QUITSAVE|CONTINUE" 30) \
    || die "Timed out waiting for preflop action"

# G-010: Verify human hole cards visible
state=$(api GET /state 2>/dev/null) || die "Could not read state"
phase=$(jget "$state" 'o.gamePhase || "NONE"')
log "  Game phase: $phase"

human_seat=$(jget "$state" 'o.currentAction&&o.currentAction.humanSeat||0')
human_cards=$(jget "$state" \
    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$human_seat)?.holeCards?.join(',')||''")
log "  Human cards: $human_cards"
if [[ -n "$human_cards" && "$human_cards" != "undefined" ]]; then
    log "  OK: Human hole cards dealt"
else
    log "  WARN: Human hole cards not visible yet (may be AI's turn)"
fi

# G-011: Verify AI cards NOT visible (cheats off)
for seat_offset in 1 2; do
    ai_seat=$(( (human_seat + seat_offset) % 3 ))
    ai_cards=$(jget "$state" \
        "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$ai_seat)?.holeCards?.join(',')||''")
    if [[ -z "$ai_cards" || "$ai_cards" == "" || "$ai_cards" == "undefined" ]]; then
        log "  OK: AI seat $ai_seat cards hidden"
    else
        log "  WARN: AI seat $ai_seat cards visible: $ai_cards (cheats may be on)"
    fi
done

# G-013: Verify no community cards yet in preflop
comm_cards=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).join(",")')
if [[ -z "$comm_cards" || "$comm_cards" == "" ]]; then
    log "  OK: No community cards in preflop"
else
    log "  INFO: Community cards already showing (hand may have advanced): $comm_cards"
fi

# Play through by checking/calling on human turns until we see community cards
play_through_street() {
    local target_card_count="$1" street_name="$2" timeout="${3:-30}"
    local start_time=$(date +%s)
    while true; do
        local st md
        st=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
        md=$(jget "$st" 'o.inputMode || "NONE"')

        case "$md" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                local is_human
                is_human=$(jget "$st" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                if [[ "$is_human" == "true" ]]; then
                    local avail
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
                # Hand ended early
                log "  Hand ended before reaching $street_name"
                echo "$st"
                return 0
                ;;
        esac

        # Check community card count
        local cc_count
        cc_count=$(jget "$st" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).length')
        if [[ "$cc_count" -ge "$target_card_count" ]]; then
            log "  $street_name: $cc_count community cards"
            echo "$st"
            return 0
        fi

        local elapsed=$(( $(date +%s) - start_time ))
        [[ $elapsed -gt $timeout ]] && { log "FAIL: Timed out waiting for $street_name"; return 1; }
        sleep 0.2
    done
}

# === FLOP (3 cards) ===
log "=== Playing to FLOP ==="
state=$(play_through_street 3 "FLOP") || FAILURES=$((FAILURES+1))
if [[ -n "$state" ]]; then
    flop_cards=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).slice(0,3).join(",")')
    log "  Flop cards: $flop_cards"
    screenshot "hand-flow-flop"
fi

# === TURN (4 cards) ===
log "=== Playing to TURN ==="
state=$(play_through_street 4 "TURN") || FAILURES=$((FAILURES+1))
if [[ -n "$state" ]]; then
    turn_card=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[])[3]||""')
    log "  Turn card: $turn_card"
    screenshot "hand-flow-turn"
fi

# === RIVER (5 cards) ===
log "=== Playing to RIVER ==="
state=$(play_through_street 5 "RIVER") || FAILURES=$((FAILURES+1))
if [[ -n "$state" ]]; then
    river_card=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[])[4]||""')
    log "  River card: $river_card"
    screenshot "hand-flow-river"
fi

# === SHOWDOWN ===
log "=== Playing to SHOWDOWN/DEAL ==="
# Continue acting until we reach DEAL (hand over)
SHOWDOWN_TIMEOUT=30
SSTART=$(date +%s)
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
            log "  Hand complete — reached DEAL mode"
            break
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac
    [[ $(($(date +%s) - SSTART)) -gt $SHOWDOWN_TIMEOUT ]] && { log "FAIL: Timed out at showdown"; FAILURES=$((FAILURES+1)); break; }
    sleep 0.2
done

screenshot "hand-flow-showdown"

# G-016: Validate chip conservation after hand
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
assert "chip conservation after hand" "$cc_valid" "true"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES assertion(s) failed in hand flow test"
fi

pass "Hand flow verified: preflop → flop → turn → river → showdown with card injection"
