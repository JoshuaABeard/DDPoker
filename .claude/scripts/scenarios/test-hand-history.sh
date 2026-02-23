#!/usr/bin/env bash
# test-hand-history.sh — Verify tournament history via /history endpoint.
#
# Tests HH-001 through HH-005, AV-001:
#   - Query tournament history for current profile (may be empty initially)
#   - Start a game, play a few hands, use completeGame cheat to end it
#   - Wait for game-over state, then assert a new history entry was written
#
# Usage:
#   bash .claude/scripts/scenarios/test-hand-history.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# ============================================================
# HH-001: Query tournament history (may be empty initially)
# ============================================================
log "=== HH-001: Tournament History ==="
HIST=$(api GET /history 2>/dev/null) || die "Could not read /history"
PROFILE=$(jget "$HIST" 'o.profileName||""')
COUNT=$(jget "$HIST" 'o.count||0')
log "  Profile: $PROFILE, tournaments: $COUNT"

if [[ -n "$PROFILE" && "$PROFILE" != "" && "$PROFILE" != "undefined" ]]; then
    log "  OK: Profile name present"
else
    log "  WARN: No profile name in history response"
fi

# ============================================================
# Play a game and use completeGame cheat to end it cleanly
# ============================================================
log "=== Playing a game and ending it via completeGame ==="
lib_start_game 3

# Play 3 human turns (fold each time) to generate some hand history,
# then use the completeGame cheat to end the game naturally.
HANDS_FOLDED=0
TARGET_FOLDS=3
PLAY_START=$(date +%s)

while [[ $HANDS_FOLDED -lt $TARGET_FOLDS ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
                HANDS_FOLDED=$((HANDS_FOLDED+1))
                log "  Folded hand $HANDS_FOLDED of $TARGET_FOLDS"
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
    esac

    [[ $(($(date +%s) - PLAY_START)) -gt 120 ]] && die "Timed out waiting to play $TARGET_FOLDS hands"
    sleep 0.3
done

log "  Played $HANDS_FOLDED hands — sending completeGame cheat"

# ============================================================
# HH-002: Force game end via completeGame cheat
# ============================================================
log "=== HH-002: completeGame Cheat ==="
CHEAT_RESULT=$(api_post_json /cheat '{"action": "completeGame"}' 2>/dev/null) || die "completeGame cheat failed"
ACCEPTED=$(jget "$CHEAT_RESULT" 'o.accepted||false')
if [[ "$ACCEPTED" == "true" ]]; then
    log "  OK: completeGame accepted"
else
    log "FAIL: completeGame cheat not accepted: $CHEAT_RESULT"
    FAILURES=$((FAILURES+1))
fi

# Continue playing through the final hand so the game reaches game-over state.
# Keep handling actions until: playersRemaining <= 1, or lifecycle shows game over,
# or 1 player has chips (QUITSAVE with single chip-holder).
log "  Playing through final hand to reach game-over..."
GAMEOVER=false
GAMEOVER_TIMEOUT=120
GAMEOVER_START=$(date +%s)

while [[ "$GAMEOVER" == "false" ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||99')
    lifecycle=$(jget "$state" 'o.lifecyclePhase||""')
    players_with_chips=$(jget "$state" \
        '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')

    # Game-over detection (mirrors test-gameover-ranks.sh)
    if [[ "$remaining" =~ ^[0-9]+$ && "$remaining" -le 1 && "$HANDS_FOLDED" -gt 0 ]]; then
        log "  Game over: $remaining player(s) remaining"
        GAMEOVER=true
        break
    fi
    if echo "$lifecycle" | grep -qiE "gameover|PracticeGameOver"; then
        log "  Game over via lifecycle: $lifecycle"
        GAMEOVER=true
        break
    fi
    if [[ "$mode" == "QUITSAVE" && "$players_with_chips" == "1" && "$HANDS_FOLDED" -gt 0 ]]; then
        log "  Game over: QUITSAVE with 1 player holding chips"
        GAMEOVER=true
        break
    fi

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
            fi
            ;;
        DEAL)
            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
            ;;
        CONTINUE|CONTINUE_LOWER)
            api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true
            ;;
        REBUY_CHECK)
            api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
            ;;
        PAUSE_ALLIN)
            api_post_json /action '{"type":"CONTINUE"}' > /dev/null 2>&1 || true
            ;;
    esac

    [[ $(($(date +%s) - GAMEOVER_START)) -gt $GAMEOVER_TIMEOUT ]] && \
        die "Timed out waiting for game-over after completeGame (mode=$mode, remaining=$remaining)"
    sleep 0.3
done

# Give the game a moment to write history
sleep 2

# ============================================================
# HH-003: Check history after game completed
# ============================================================
log "=== HH-003: History After Game ==="
HIST2=$(api GET /history 2>/dev/null) || die "Could not read /history after game"
NEW_COUNT=$(jget "$HIST2" 'o.count||0')
log "  Tournament count now: $NEW_COUNT (was $COUNT)"

if [[ "$NEW_COUNT" -gt "$COUNT" ]]; then
    log "  OK: New history entry written after game completion"
    TNAME=$(jget "$HIST2" '(o.tournaments||[])[0]?.tournamentName||""')
    PLACE=$(jget "$HIST2" '(o.tournaments||[])[0]?.place||0')
    PLAYERS=$(jget "$HIST2" '(o.tournaments||[])[0]?.numPlayers||0')
    log "  Latest: name=$TNAME, place=$PLACE, players=$PLAYERS"
else
    log "FAIL: HH-003 — game completed but no new history entry (count still $NEW_COUNT)"
    FAILURES=$((FAILURES+1))
fi

# ============================================================
# AV-001: Save list
# ============================================================
log "=== SL: Save File List ==="
SAVES=$(api GET /game/saves 2>/dev/null) || true
SAVE_COUNT=$(jget "$SAVES" 'o.count||0')
SAVE_DIR=$(jget "$SAVES" 'o.saveDir||""')
log "  Save directory: $SAVE_DIR"
log "  Save files: $SAVE_COUNT"
if [[ -n "$SAVE_DIR" && "$SAVE_DIR" != "" ]]; then
    log "  OK: Save directory reported"
else
    log "  WARN: Save directory not available"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES test(s) failed"
fi

pass "History verified: profile=$PROFILE, completeGame used, new entry count=$NEW_COUNT, saves=$SAVE_COUNT"
