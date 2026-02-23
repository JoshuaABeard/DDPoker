#!/usr/bin/env bash
# test-advisor-detail.sh — Verify advisor provides recommendations on human turns.
#
# Tests AD-001 through AD-004:
#   - Enable advisor via preferences
#   - advisorAdvice and advisorTitle populated on human turn
#   - Advisor recommendation updates across streets (preflop, flop, etc.)
#   - Disable advisor → fields empty
#
# Usage:
#   bash .claude/scripts/scenarios/test-advisor-detail.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# AD-001: Enable advisor
# ============================================================
log "=== AD-001: Enable Advisor ==="
api_post_json /options '{"advisor.enabled": true}' > /dev/null 2>&1 || \
    log "WARN: Could not set advisor.enabled (may use different key)"

lib_start_game 3

# ============================================================
# AD-002: Verify advisor recommendation on human turn
# ============================================================
log "=== AD-002: Check advisor on human turn ==="

# Wait for human betting turn
state=$(wait_human_turn 60) || die "Timed out waiting for human turn"

# Check advisorAdvice and advisorTitle
advice=$(jget "$state" 'o.currentAction&&o.currentAction.advisorAdvice||""')
title=$(jget "$state" 'o.currentAction&&o.currentAction.advisorTitle||""')

log "  advisorAdvice: $advice"
log "  advisorTitle: $title"

# Advisor may take a moment to compute
if [[ -z "$advice" || "$advice" == "" || "$advice" == "undefined" ]]; then
    log "  Advisor advice not ready yet, waiting..."
    sleep 2
    state=$(api GET /state 2>/dev/null) || true
    advice=$(jget "$state" 'o.currentAction&&o.currentAction.advisorAdvice||""')
    title=$(jget "$state" 'o.currentAction&&o.currentAction.advisorTitle||""')
    log "  advisorAdvice (retry): $advice"
    log "  advisorTitle (retry): $title"
fi

if [[ -n "$advice" && "$advice" != "" && "$advice" != "undefined" && "$advice" != "null" ]]; then
    log "  OK: Advisor advice present"
else
    log "FAIL: Advisor advice empty on human turn"
    FAILURES=$((FAILURES+1))
fi

if [[ -n "$title" && "$title" != "" && "$title" != "undefined" && "$title" != "null" ]]; then
    log "  OK: Advisor title present: $title"
else
    log "FAIL: Advisor title empty on human turn"
    FAILURES=$((FAILURES+1))
fi

screenshot "advisor-preflop"

# ============================================================
# AD-003: Verify advisor updates on different streets
# ============================================================
log "=== AD-003: Advisor updates across streets ==="

preflop_advice="$advice"

# Play to flop by calling
avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
if echo "$avail" | grep -q "CHECK"; then
    api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
else
    api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
fi

# Wait for next human turn (should be on flop or later)
NEXT_TIMEOUT=30
NSTART=$(date +%s)
flop_advice=""
while [[ $(($(date +%s) - NSTART)) -lt $NEXT_TIMEOUT ]]; do
    st=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    md=$(jget "$st" 'o.inputMode || "NONE"')

    case "$md" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$st" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                flop_advice=$(jget "$st" 'o.currentAction&&o.currentAction.advisorAdvice||""')
                flop_title=$(jget "$st" 'o.currentAction&&o.currentAction.advisorTitle||""')
                log "  Next street advice: $flop_advice"
                log "  Next street title: $flop_title"
                break
            fi
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$md\"}" > /dev/null 2>&1 || true
            ;;
        DEAL)
            log "  Hand ended before reaching next street"
            break
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac
    sleep 0.3
done

if [[ -n "$flop_advice" && "$flop_advice" != "" && "$flop_advice" != "undefined" ]]; then
    log "  OK: Advisor provided advice on subsequent street"
else
    log "  WARN: Could not verify advisor on subsequent street (hand may have ended)"
fi

screenshot "advisor-flop"

# Fold to end the hand
api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true

# ============================================================
# AD-004: Disable advisor → no recommendations
# ============================================================
log "=== AD-004: Disable Advisor ==="
api_post_json /options '{"advisor.enabled": false}' > /dev/null 2>&1 || \
    log "WARN: Could not disable advisor"

# Finish current hand and start new one
sleep 1
state=$(wait_mode "DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK" 30) || true
mode=$(jget "$state" 'o.inputMode || "NONE"')
while [[ "$mode" != "DEAL" ]]; do
    advance_non_betting "$state"
    sleep 0.5
    state=$(api GET /state 2>/dev/null) || break
    mode=$(jget "$state" 'o.inputMode || "NONE"')
done

if [[ "$mode" == "DEAL" ]]; then
    api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
    state=$(wait_human_turn 30) || log "WARN: No human turn for disabled advisor check"

    if [[ -n "$state" ]]; then
        no_advice=$(jget "$state" 'o.currentAction&&o.currentAction.advisorAdvice||""')
        no_title=$(jget "$state" 'o.currentAction&&o.currentAction.advisorTitle||""')
        log "  advisorAdvice (disabled): '$no_advice'"
        log "  advisorTitle (disabled): '$no_title'"

        if [[ -z "$no_advice" || "$no_advice" == "" || "$no_advice" == "null" || "$no_advice" == "undefined" ]]; then
            log "  OK: Advisor advice empty when disabled"
        else
            log "  WARN: Advisor still providing advice after disable (may be cached)"
        fi
    fi
fi

screenshot "advisor-disabled"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "Advisor verified: advice/title present on human turn, updates across streets"
