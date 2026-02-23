#!/usr/bin/env bash
# test-neverbroke.sh — Verify the Never Go Broke offer appears when the human busts.
#
# Procedure:
#   1. Enable cheat.neverbroke=true via /options
#   2. Start a 3-player game
#   3. Verify the option is active
#   4. Wait for a human betting turn, then use /cheat to reduce the human's
#      chips to just 1 (so the next all-in bet will bust them)
#   5. Go ALL_IN (or CALL) — the human risks all remaining chips
#   6. Wait for game to process the outcome
#   7. Assert that either:
#      (a) inputMode becomes REBUY_CHECK (Never Go Broke offer appeared), or
#      (b) human chips were restored above 0 (they won the hand — retry)
#
# Note: Because we can't control the card outcome, the test may need
# multiple attempts for the human to actually lose. It retries up to
# MAX_ATTEMPTS times before failing.
#
# Usage:
#   bash .claude/scripts/scenarios/test-neverbroke.sh [options]
#
# Options:
#   --skip-build   Skip mvn build
#   --skip-launch  Reuse a running game

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

MAX_ATTEMPTS=10

log "Setting cheat.neverbroke=true..."
api_post_json /options '{"cheat.neverbroke": true}' > /dev/null

# Verify option took effect
OPTIONS=$(api GET /options 2>/dev/null) || die "Could not read /options"
NB=$(jget "$OPTIONS" 'o.cheat && o.cheat.neverbroke')
[[ "$NB" == "true" ]] || die "cheat.neverbroke did not activate (got: $NB)"
log "cheat.neverbroke is active"

lib_start_game 3
screenshot "neverbroke-start"

ATTEMPT=0
while [[ $ATTEMPT -lt $MAX_ATTEMPTS ]]; do
    ATTEMPT=$((ATTEMPT+1))
    log "Attempt $ATTEMPT / $MAX_ATTEMPTS: waiting for human turn..."

    # Wait for a human betting turn (may need to advance through DEAL/CONTINUE first)
    while true; do
        STATE=$(wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE|DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK" 60) \
            || die "Timed out"
        MODE=$(jget "$STATE" 'o.inputMode || "NONE"')

        case "$MODE" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                IS_HUMAN=$(jget "$STATE" 'o.currentAction && o.currentAction.isHumanTurn || false')
                [[ "$IS_HUMAN" == "true" ]] && break
                sleep 0.3
                ;;
            DEAL|CONTINUE|CONTINUE_LOWER)
                api_post_json /action "{\"type\":\"$MODE\"}" > /dev/null 2>&1 || true
                sleep 0.2
                ;;
            REBUY_CHECK)
                pass "REBUY_CHECK appeared — Never Go Broke offer triggered on attempt $ATTEMPT"
                exit 0
                ;;
            *)
                sleep 0.3
                ;;
        esac
    done

    # Find human seat and current chip count
    HUMAN_SEAT=$(jget "$STATE" 'o.currentAction && o.currentAction.humanSeat || 0')
    HUMAN_CHIPS=$(jget "$STATE" \
        "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$HUMAN_SEAT)?.chips || 1000")
    log "Human at seat $HUMAN_SEAT with $HUMAN_CHIPS chips — draining to 1 chip via /cheat..."

    # Drain human to 1 chip (will be all-in for the minimum)
    api_post_json /cheat "{\"action\":\"setChips\",\"seat\":$HUMAN_SEAT,\"amount\":1}" \
        > /dev/null 2>&1 || log "  WARN: /cheat setChips failed (maybe no game running yet)"
    sleep 0.3

    # Go all-in for 1 chip
    ACTION="ALL_IN"
    RESP=$(api_post_json /action "{\"type\":\"$ACTION\"}" 2>/dev/null) || true
    if ! echo "$RESP" | grep -q '"accepted":true'; then
        # Fallback: try CALL or FOLD if ALL_IN rejected
        AVAIL=$(jget "$STATE" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
        if echo "$AVAIL" | grep -q "CALL"; then
            api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
        else
            api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
            log "  Could not bet, folded this hand"
            continue
        fi
    fi
    log "  All-in action sent"
    screenshot "neverbroke-allin-attempt${ATTEMPT}"

    # Wait for outcome — either REBUY_CHECK or back to DEAL (human survived)
    OUTCOME_TIMEOUT=30
    OSTART=$(date +%s)
    while [[ $(($(date +%s) - OSTART)) -lt $OUTCOME_TIMEOUT ]]; do
        OSTATE=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
        OMODE=$(jget "$OSTATE" 'o.inputMode || "NONE"')
        case "$OMODE" in
            REBUY_CHECK)
                pass "Never Go Broke offer appeared on attempt $ATTEMPT"
                exit 0
                ;;
            DEAL)
                # Human survived — check chips
                CHIPS_NOW=$(jget "$OSTATE" \
                    "(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.seat===$HUMAN_SEAT)?.chips || 0")
                log "  Human survived with $CHIPS_NOW chips — retrying"
                break
                ;;
            CONTINUE|CONTINUE_LOWER)
                api_post_json /action "{\"type\":\"$OMODE\"}" > /dev/null 2>&1 || true
                ;;
        esac
        sleep 0.3
    done
done

die "Never Go Broke offer did not appear in $MAX_ATTEMPTS attempts. Check neverbroke implementation."
