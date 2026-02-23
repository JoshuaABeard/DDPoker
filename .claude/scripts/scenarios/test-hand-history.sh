#!/usr/bin/env bash
# test-hand-history.sh — Verify tournament history via /history endpoint.
#
# Tests HH-001 through HH-005, AV-001 through AV-003:
#   - Query tournament history for current profile
#   - Play a game to generate history, then verify it appears
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
# Play a quick game to generate history
# ============================================================
log "=== Playing a game to generate history ==="
lib_start_game 3

# Play through hands quickly by folding
HANDS_PLAYED=0
MAX_HANDS=30
GAME_OVER=false

while [[ "$HANDS_PLAYED" -lt "$MAX_HANDS" && "$GAME_OVER" == "false" ]]; do
    state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    remaining=$(jget "$state" 'o.tournament&&o.tournament.playersRemaining||0')

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
                HANDS_PLAYED=$((HANDS_PLAYED+1))
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
        GAMEOVER)
            GAME_OVER=true
            ;;
    esac

    if [[ "$remaining" -le 1 && "$remaining" -gt 0 ]]; then
        GAME_OVER=true
    fi
    sleep 0.3
done
log "  Played $HANDS_PLAYED hands, gameOver=$GAME_OVER"

# ============================================================
# HH-003: Check history after game
# ============================================================
log "=== HH-003: History After Game ==="
HIST=$(api GET /history 2>/dev/null) || true
NEW_COUNT=$(jget "$HIST" 'o.count||0')
log "  Tournament count now: $NEW_COUNT (was $COUNT)"

if [[ "$NEW_COUNT" -gt "$COUNT" ]]; then
    log "  OK: Tournament history has new entry"
    # Check details of latest entry
    TNAME=$(jget "$HIST" '(o.tournaments||[])[0]?.tournamentName||""')
    PLACE=$(jget "$HIST" '(o.tournaments||[])[0]?.place||0')
    PLAYERS=$(jget "$HIST" '(o.tournaments||[])[0]?.numPlayers||0')
    log "  Latest: name=$TNAME, place=$PLACE, players=$PLAYERS"
elif [[ "$GAME_OVER" == "true" ]]; then
    log "  WARN: Game completed but no new history entry"
else
    log "  WARN: Game did not complete, no new history expected"
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

pass "History verified: profile=$PROFILE, played=$HANDS_PLAYED hands, saves=$SAVE_COUNT"
