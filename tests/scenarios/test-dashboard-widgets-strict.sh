#!/usr/bin/env bash
# test-dashboard-widgets-strict.sh — Strict semantic dashboard widget checks.
#
# Covers D-001..D-013 using /ui/dashboard/widgets plus /state cross-checks.
#
# Usage:
#   bash tests/scenarios/test-dashboard-widgets-strict.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch
lib_start_game 4

# Force key dashboard options for deterministic assertions.
api_post_json /options '{"advisor.enabled":true,"cheat.aifaceup":true}' > /dev/null 2>&1 || true

# Ensure all relevant widgets are displayed, independent of persisted user prefs.
for item in clock playerstyle advisor simulator handstrength potodds improveodds myhand mytable rank next cheat debug; do
    api_post_json /ui/dashboard "{\"action\":\"SET_DISPLAYED\",\"name\":\"$item\",\"displayed\":true}" > /dev/null 2>&1 || true
done

state=$(wait_human_turn 60) || die "Timed out waiting for human turn"

FAILURES=0

WIDGETS=$(wait_for_widget_state 'o.present === true && o.widgets && o.widgets.clock && o.widgets.clock.present === true' 20 "dashboard widgets snapshot present") || die "Dashboard widgets snapshot not present"

check_true() {
    local cond="$1" msg="$2"
    if [[ "$cond" == "true" ]]; then
        log "  OK: $msg"
    else
        log "FAIL: $msg"
        FAILURES=$((FAILURES + 1))
    fi
}

check_eq() {
    local actual="$1" expected="$2" msg="$3"
    if [[ "$actual" == "$expected" ]]; then
        log "  OK: $msg = $actual"
    else
        log "FAIL: $msg (expected '$expected', got '$actual')"
        FAILURES=$((FAILURES + 1))
    fi
}

check_number_ge() {
    local actual="$1" minimum="$2" msg="$3"
    if [[ "$actual" =~ ^-?[0-9]+$ ]] && [[ "$actual" -ge "$minimum" ]]; then
        log "  OK: $msg = $actual"
    else
        log "FAIL: $msg expected >= $minimum, got '$actual'"
        FAILURES=$((FAILURES + 1))
    fi
}

choose_progress_action() {
    local st="$1"
    local has_check has_call has_allin has_fold
    has_check=$(jget "$st" '(o.currentAction&&o.currentAction.availableActions||[]).includes("CHECK")')
    has_call=$(jget "$st" '(o.currentAction&&o.currentAction.availableActions||[]).includes("CALL")')
    has_allin=$(jget "$st" '(o.currentAction&&o.currentAction.availableActions||[]).includes("ALL_IN")')
    has_fold=$(jget "$st" '(o.currentAction&&o.currentAction.availableActions||[]).includes("FOLD")')

    if [[ "$has_check" == "true" ]]; then
        printf '%s' "CHECK"
    elif [[ "$has_call" == "true" ]]; then
        printf '%s' "CALL"
    elif [[ "$has_fold" == "true" ]]; then
        printf '%s' "FOLD"
    elif [[ "$has_allin" == "true" ]]; then
        printf '%s' "ALL_IN"
    else
        printf '%s' ""
    fi
}

drive_until_round() {
    local target_round="$1" timeout="${2:-180}"
    local start now state round is_human folded mode action action_resp accepted

    start=$(date +%s)
    while true; do
        state=$(api GET /state 2>/dev/null || true)
        if [[ -z "$state" ]]; then
            sleep 0.2
            continue
        fi

        round=$(jget "$state" 'o.tables&&o.tables[0]&&o.tables[0].round||"NONE"')
        is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
        folded=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.isFolded||false')
        mode=$(jget "$state" 'o.inputMode || "NONE"')

        if [[ "$round" == "$target_round" && "$is_human" == "true" && "$folded" != "true" ]]; then
            printf '%s' "$state"
            return 0
        fi

        if [[ "$is_human" == "true" ]]; then
            action=$(choose_progress_action "$state")
            if [[ -n "$action" ]]; then
                action_resp=$(api_post_json /action "{\"type\":\"$action\"}" 2>/dev/null || true)
                accepted=$(jget "$action_resp" 'o.accepted || false')
                if [[ "$accepted" != "true" ]]; then
                    log "  NOTE: progress action $action not accepted: $action_resp"
                fi
            fi
        else
            case "$mode" in
                DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                    advance_non_betting "$state" > /dev/null 2>&1 || true
                    ;;
                *)
                    close_visible_dialog_if_any "drive-to-$target_round" > /dev/null 2>&1 || true
                    ;;
            esac
        fi

        now=$(date +%s)
        if [[ $((now - start)) -gt $timeout ]]; then
            return 1
        fi

        sleep 0.2
    done
}

wait_for_playable_human_turn() {
    local timeout="${1:-90}"
    local start state mode is_human

    start=$(date +%s)
    while true; do
        state=$(api GET /state 2>/dev/null || true)
        if [[ -n "$state" ]]; then
            mode=$(jget "$state" 'o.inputMode || "NONE"')
            is_human=$(jget "$state" 'o.currentAction && o.currentAction.isHumanTurn || false')

            if [[ "$is_human" == "true" ]] && [[ "$mode" =~ ^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$ ]]; then
                printf '%s' "$state"
                return 0
            fi

            case "$mode" in
                DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                    advance_non_betting "$state" > /dev/null 2>&1 || true
                    ;;
                QUITSAVE|NONE)
                    close_visible_dialog_if_any "wait-human-turn" > /dev/null 2>&1 || true
                    ;;
            esac
        fi

        if [[ $(( $(date +%s) - start )) -gt $timeout ]]; then
            return 1
        fi
        sleep 0.2
    done
}

log "=== D-001: Clock ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.clock && o.widgets.clock.present || false')" "clock widget present"
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.clock && o.widgets.clock.displayed || false')" "clock widget displayed"
check_eq "$(jget "$WIDGETS" 'o.widgets.clock.data.level || 0')" "$(jget "$state" 'o.tournament && o.tournament.level || 0')" "clock level matches tournament"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.clock.data.smallBlind || 0' 'o.tournament && o.tournament.smallBlind || 0' "clock small blind matches state"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.clock.data.bigBlind || 0' 'o.tournament && o.tournament.bigBlind || 0' "clock big blind matches state"
check_number_ge "$(jget "$WIDGETS" 'o.widgets.clock.data.secondsRemaining || -1')" 0 "clock secondsRemaining"

log "=== D-002: Player Info ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.playerInfo && o.widgets.playerInfo.present || false')" "playerInfo widget present"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.playerInfo.data.playerName || ""' '(o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.name||""' "playerInfo player name matches human"

log "=== D-003: Advisor ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.advisor && o.widgets.advisor.present || false')" "advisor widget present"
check_true "$(jget "$WIDGETS" 'o.widgets.advisor.data.isHumanTurn || false')" "advisor indicates human turn"
ADVICE=$(jget "$WIDGETS" 'o.widgets.advisor.data.advisorAdvice || ""')
TITLE=$(jget "$WIDGETS" 'o.widgets.advisor.data.advisorTitle || ""')
if [[ -z "$ADVICE" || -z "$TITLE" ]]; then
    log "FAIL: advisor advice/title should be non-empty on human turn"
    FAILURES=$((FAILURES + 1))
else
    log "  OK: advisor advice/title present"
fi
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.advisor.data.advisorAdvice || ""' 'o.currentAction && o.currentAction.advisorAdvice || ""' "advisor advice matches /state"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.advisor.data.advisorTitle || ""' 'o.currentAction && o.currentAction.advisorTitle || ""' "advisor title matches /state"

log "=== D-004: Simulator ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.simulator && o.widgets.simulator.present || false')" "simulator widget present"
check_true "$(jget "$WIDGETS" 'o.widgets.simulator.data.available || false')" "simulator action available"
check_eq "$(jget "$WIDGETS" 'o.widgets.simulator.data.targetPhase || ""')" "CalcTool" "simulator target phase"

log "=== D-005: Hand Strength ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.handStrength && o.widgets.handStrength.present || false')" "handStrength widget present"
HS_EXPECTED=$(jget "$WIDGETS" 'o.widgets.handStrength.data.expectedStrengthValue || false')
if [[ "$HS_EXPECTED" == "true" ]]; then
    check_true "$(jget "$WIDGETS" 'Number(o.widgets.handStrength.data.handStrength) >= 0 && Number(o.widgets.handStrength.data.handStrength) <= 1')" "handStrength in [0,1]"
else
    log "  OK: handStrength not required in current round"
fi

log "=== D-006: Pot Odds ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.potOdds && o.widgets.potOdds.present || false')" "potOdds widget present"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.potOdds.data.pot || 0' 'o.currentAction && o.currentAction.pot || 0' "potOdds pot matches /state"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.potOdds.data.callAmount || 0' 'o.currentAction && o.currentAction.callAmount || 0' "potOdds callAmount matches /state"

log "=== D-007: Improve Odds ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.improveOdds && o.widgets.improveOdds.present || false')" "improveOdds widget present"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.improveOdds.data.communityCardCount || 0' '(o.tables&&o.tables[0]&&o.tables[0].communityCards||[]).length' "improveOdds community count matches /state"

log "=== D-008: My Hand ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.myHand && o.widgets.myHand.present || false')" "myHand widget present"
assert_widget_matches_state "$WIDGETS" "$state" '(o.widgets.myHand.data.holeCards||[]).length' '((o.tables&&o.tables[0]&&o.tables[0].players||[]).find(p=>p&&p.isHuman)?.holeCards||[]).length' "myHand hole-card count matches /state"

log "=== D-009: My Table ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.myTable && o.widgets.myTable.present || false')" "myTable widget present"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.myTable.data.tableId || -1' 'o.tables&&o.tables[0]&&o.tables[0].id||-1' "myTable table id matches /state"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.myTable.data.handNumber || 0' 'o.tournament&&o.tournament.handNumber||0' "myTable hand number matches /state"

log "=== D-010: Rank ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.rank && o.widgets.rank.present || false')" "rank widget present"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.rank.data.totalPlayers || 0' 'o.tournament&&o.tournament.totalPlayers||0' "rank totalPlayers matches /state"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.rank.data.playersRemaining || 0' 'o.tournament&&o.tournament.playersRemaining||0' "rank playersRemaining matches /state"

log "=== D-011: Up Next ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.upNext && o.widgets.upNext.present || false')" "upNext widget present"
assert_widget_matches_state "$WIDGETS" "$state" 'o.widgets.upNext.data.level || 0' 'o.tournament&&o.tournament.level||0' "upNext level matches /state"

log "=== D-012: Cheat ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.cheat && o.widgets.cheat.present || false')" "cheat widget present"
check_true "$(jget "$WIDGETS" 'Number(o.widgets.cheat.data.activeCount||0) >= 0')" "cheat activeCount is non-negative"

log "=== D-013: Debug ==="
check_true "$(jget "$WIDGETS" 'o.widgets && o.widgets.debug && o.widgets.debug.present || false')" "debug widget present"
check_true "$(jget "$WIDGETS" 'o.widgets.debug.data.threadDumpActionAvailable || false')" "debug thread-dump action available"

# Snapshot freshness contract
log "=== Freshness Metadata ==="
WIDGETS_FRESH=$(api GET /ui/dashboard/widgets 2>/dev/null || true)
check_true "$(jget "$WIDGETS_FRESH" 'Number(o.source && o.source.updatedAtMs || 0) > 0')" "source updatedAtMs present"
check_true "$(jget "$WIDGETS_FRESH" 'Number(o.source && o.source.sourceStateSeq || 0) >= 1')" "sourceStateSeq present"
for widget in clock playerInfo advisor simulator handStrength potOdds improveOdds myHand myTable rank upNext cheat debug; do
    assert_widget_fresh "$WIDGETS_FRESH" "$widget" 120000 "$widget freshness metadata"
done

log "=== Transition Timing ==="
SEQ0=$(jget "$WIDGETS" 'Number((o.source && o.source.sourceStateSeq) ?? 0)')
ACTIONS=$(jget "$state" 'JSON.stringify(o.currentAction && o.currentAction.availableActions || [])')
ACTION=""
if [[ "$ACTIONS" == *"CHECK"* ]]; then
    ACTION="CHECK"
elif [[ "$ACTIONS" == *"CALL"* ]]; then
    ACTION="CALL"
elif [[ "$ACTIONS" == *"FOLD"* ]]; then
    ACTION="FOLD"
fi

if [[ -z "$ACTION" ]]; then
    log "FAIL: could not choose valid transition action from $ACTIONS"
    FAILURES=$((FAILURES + 1))
else
    ACTION_RESP=$(api_post_json /action "{\"type\":\"$ACTION\"}" 2>/dev/null || true)
    ACCEPTED=$(jget "$ACTION_RESP" 'o.accepted || false')
    if [[ "$ACCEPTED" != "true" ]]; then
        log "FAIL: transition action not accepted: $ACTION_RESP"
        FAILURES=$((FAILURES + 1))
    else
        WIDGETS_AFTER=$(wait_for_widget_state "Number(o.source && o.source.sourceStateSeq || 0) > $SEQ0" 20 "widget source sequence advanced after $ACTION") || {
            log "FAIL: source sequence did not advance after $ACTION"
            FAILURES=$((FAILURES + 1))
            WIDGETS_AFTER=""
        }
        if [[ -n "$WIDGETS_AFTER" ]]; then
            assert_widget_fresh "$WIDGETS_AFTER" "clock" 2000 "clock freshness after action"
            assert_widget_fresh "$WIDGETS_AFTER" "advisor" 2000 "advisor freshness after action"
        fi
    fi
fi

log "=== Non-Human Branch ==="
AI_STATE=""
for _ in {1..180}; do
    CANDIDATE=$(api GET /state 2>/dev/null || true)
    if [[ -z "$CANDIDATE" ]]; then
        sleep 0.2
        continue
    fi

    IS_HUMAN=$(jget "$CANDIDATE" 'o.currentAction&&o.currentAction.isHumanTurn||false')
    MODE=$(jget "$CANDIDATE" 'o.inputMode || "NONE"')
    if [[ "$IS_HUMAN" == "false" && "$MODE" != "NONE" ]]; then
        AI_STATE="$CANDIDATE"
        break
    fi

    if [[ "$IS_HUMAN" == "true" ]]; then
        CAN_FOLD=$(jget "$CANDIDATE" '(o.currentAction&&o.currentAction.availableActions||[]).includes("FOLD")')
        if [[ "$CAN_FOLD" == "true" ]]; then
            AI_ACTION="FOLD"
        else
            AI_ACTION=$(choose_progress_action "$CANDIDATE")
        fi
        if [[ -n "$AI_ACTION" ]]; then
            api_post_json /action "{\"type\":\"$AI_ACTION\"}" > /dev/null 2>&1 || true
        fi
    else
        case "$MODE" in
            DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                advance_non_betting "$CANDIDATE" > /dev/null 2>&1 || true
                ;;
        esac
    fi

    sleep 0.2
done

if [[ -z "$AI_STATE" ]]; then
    log "FAIL: timed out waiting for non-human /state snapshot"
    FAILURES=$((FAILURES + 1))
    WIDGETS_AI=""
else
    WIDGETS_AI=$(wait_for_widget_state 'o.present === true && o.widgets && o.widgets.advisor && o.widgets.advisor.data && o.widgets.advisor.data.isHumanTurn === false' 20 "non-human widget snapshot") || {
        log "FAIL: timed out waiting for non-human widget snapshot"
        FAILURES=$((FAILURES + 1))
        WIDGETS_AI=""
    }
fi

if [[ -n "$WIDGETS_AI" ]]; then
    check_true "$(jget "$WIDGETS_AI" 'o.widgets.advisor.data.isHumanTurn === false')" "advisor marks non-human turn"
    check_true "$(jget "$WIDGETS_AI" 'o.widgets.advisor.data.hasAdvice === true || o.widgets.advisor.data.hasAdvice === false')" "advisor hasAdvice boolean present on non-human turn"
fi

log "=== Post-Flop Branches ==="
log "Restarting game for deterministic post-flop branch checks..."
lib_start_game 2 '"buyinChips": 5000, "blindLevels": [{"small": 25, "big": 50, "ante": 0, "minutes": 60}]'
wait_for_playable_human_turn 90 >/dev/null || {
    log "FAIL: timed out waiting for playable human turn after post-flop restart"
    FAILURES=$((FAILURES + 1))
}

for item in handstrength improveodds myhand; do
    api_post_json /ui/dashboard "{\"action\":\"SET_DISPLAYED\",\"name\":\"$item\",\"displayed\":true}" > /dev/null 2>&1 || true
done

WIDGETS_POSTFLOP=$(wait_for_widget_state 'o.present === true && o.widgets && o.widgets.handStrength && o.widgets.handStrength.present === true' 20 "post-flop game widget snapshot ready") || {
    log "FAIL: widgets did not become ready after post-flop restart"
    FAILURES=$((FAILURES + 1))
    WIDGETS_POSTFLOP=""
}

FLOP_STATE=$(drive_until_round "FLOP" 180) || {
    log "FAIL: timed out reaching FLOP on human turn"
    FAILURES=$((FAILURES + 1))
    FLOP_STATE=""
}

if [[ -n "$FLOP_STATE" ]]; then
    WIDGETS_FLOP=$(wait_for_widget_state 'o.present === true && o.source && o.source.sourceRound === "FLOP"' 30 "FLOP widget snapshot") || {
        log "FAIL: timed out waiting for FLOP widget snapshot"
        FAILURES=$((FAILURES + 1))
        WIDGETS_FLOP=""
    }

    if [[ -n "$WIDGETS_FLOP" ]]; then
        check_true "$(jget "$WIDGETS_FLOP" 'o.widgets.handStrength.data.expectedStrengthValue === true')" "FLOP handStrength expected"
        check_true "$(jget "$WIDGETS_FLOP" 'Number(o.widgets.handStrength.data.handStrength) >= 0 && Number(o.widgets.handStrength.data.handStrength) <= 1')" "FLOP handStrength in [0,1]"
        check_true "$(jget "$WIDGETS_FLOP" 'o.widgets.improveOdds.data.expectedImproveOdds === true')" "FLOP improveOdds expected"
        check_eq "$(jget "$WIDGETS_FLOP" 'o.widgets.improveOdds.data.communityCardCount || 0')" "3" "FLOP community card count"
        check_true "$(jget "$WIDGETS_FLOP" 'o.widgets.improveOdds.data.totalImprovePercent !== null && Number(o.widgets.improveOdds.data.totalImprovePercent) >= 0')" "FLOP improve odds computed"
    fi
fi

TURN_STATE=$(drive_until_round "TURN" 240) || {
    log "FAIL: timed out reaching TURN on human turn"
    FAILURES=$((FAILURES + 1))
    TURN_STATE=""
}

if [[ -n "$TURN_STATE" ]]; then
    WIDGETS_TURN=$(wait_for_widget_state 'o.present === true && o.source && o.source.sourceRound === "TURN"' 30 "TURN widget snapshot") || {
        log "FAIL: timed out waiting for TURN widget snapshot"
        FAILURES=$((FAILURES + 1))
        WIDGETS_TURN=""
    }

    if [[ -n "$WIDGETS_TURN" ]]; then
        check_true "$(jget "$WIDGETS_TURN" 'o.widgets.handStrength.data.expectedStrengthValue === true')" "TURN handStrength expected"
        check_true "$(jget "$WIDGETS_TURN" 'Number(o.widgets.handStrength.data.handStrength) >= 0 && Number(o.widgets.handStrength.data.handStrength) <= 1')" "TURN handStrength in [0,1]"
        check_true "$(jget "$WIDGETS_TURN" 'o.widgets.improveOdds.data.expectedImproveOdds === true')" "TURN improveOdds expected"
        check_eq "$(jget "$WIDGETS_TURN" 'o.widgets.improveOdds.data.communityCardCount || 0')" "4" "TURN community card count"
        check_true "$(jget "$WIDGETS_TURN" 'o.widgets.improveOdds.data.totalImprovePercent !== null && Number(o.widgets.improveOdds.data.totalImprovePercent) >= 0')" "TURN improve odds computed"
    fi
fi

RIVER_STATE=""
for attempt in 1 2 3; do
    RIVER_STATE=$(drive_until_round "RIVER" 240) || RIVER_STATE=""
    if [[ -n "$RIVER_STATE" ]]; then
        break
    fi

    if [[ "$attempt" -lt 3 ]]; then
        log "  INFO: could not reach RIVER on attempt $attempt, restarting short game"
        lib_start_game 2 '"buyinChips": 5000, "blindLevels": [{"small": 25, "big": 50, "ante": 0, "minutes": 60}]'
        wait_for_playable_human_turn 90 >/dev/null || true
        for item in handstrength improveodds myhand; do
            api_post_json /ui/dashboard "{\"action\":\"SET_DISPLAYED\",\"name\":\"$item\",\"displayed\":true}" > /dev/null 2>&1 || true
        done
    fi
done

if [[ -z "$RIVER_STATE" ]]; then
    log "FAIL: timed out reaching RIVER on human turn"
    FAILURES=$((FAILURES + 1))
fi

if [[ -n "$RIVER_STATE" ]]; then
    WIDGETS_RIVER=$(wait_for_widget_state 'o.present === true && o.source && o.source.sourceRound === "RIVER"' 30 "RIVER widget snapshot") || {
        log "FAIL: timed out waiting for RIVER widget snapshot"
        FAILURES=$((FAILURES + 1))
        WIDGETS_RIVER=""
    }

    if [[ -n "$WIDGETS_RIVER" ]]; then
        check_true "$(jget "$WIDGETS_RIVER" 'o.widgets.handStrength.data.expectedStrengthValue === true')" "RIVER handStrength expected"
        check_true "$(jget "$WIDGETS_RIVER" 'Number(o.widgets.handStrength.data.handStrength) >= 0 && Number(o.widgets.handStrength.data.handStrength) <= 1')" "RIVER handStrength in [0,1]"
        check_true "$(jget "$WIDGETS_RIVER" 'o.widgets.improveOdds.data.expectedImproveOdds === false')" "RIVER improveOdds not expected"
        check_eq "$(jget "$WIDGETS_RIVER" 'o.widgets.improveOdds.data.communityCardCount || 0')" "5" "RIVER community card count"
        check_true "$(jget "$WIDGETS_RIVER" 'o.widgets.improveOdds.data.totalImprovePercent === null')" "RIVER improve odds omitted"
    fi
fi

log "=== Game-Over Branch ==="
log "Restarting game for game-over rank branch checks..."
lib_start_game 2 '"rebuys": false'
api_post_json /cheat '{"action":"setLevel","level":7}' > /dev/null 2>&1 || true

STATE_GO=$(api GET /state 2>/dev/null || true)

GAME_OVER=false
FINAL_STATE_GO=""
for _ in {1..1500}; do
    STATE_GO=$(api GET /state 2>/dev/null || true)
    if [[ -z "$STATE_GO" ]]; then
        sleep 0.2
        continue
    fi

    REMAINING_GO=$(jget "$STATE_GO" 'o.tournament && o.tournament.playersRemaining || 0')
    LIFECYCLE_GO=$(jget "$STATE_GO" 'o.lifecyclePhase || "NONE"')
    MODE_GO=$(jget "$STATE_GO" 'o.inputMode || "NONE"')
    if [[ "$REMAINING_GO" -le 1 ]] || echo "$LIFECYCLE_GO" | grep -qiE "gameover|GameOver|PracticeGameOver"; then
        GAME_OVER=true
        FINAL_STATE_GO="$STATE_GO"
        break
    fi

    IS_HUMAN_GO=$(jget "$STATE_GO" 'o.currentAction && o.currentAction.isHumanTurn || false')
    if [[ "$IS_HUMAN_GO" == "true" ]]; then
        GO_ACTION=$(choose_progress_action "$STATE_GO")
        if [[ -n "$GO_ACTION" ]]; then
            api_post_json /action "{\"type\":\"$GO_ACTION\"}" > /dev/null 2>&1 || true
        fi
    else
        case "$MODE_GO" in
            DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                advance_non_betting "$STATE_GO" > /dev/null 2>&1 || true
                ;;
            *)
                close_visible_dialog_if_any "game-over-drive" > /dev/null 2>&1 || true
                ;;
        esac
    fi

    sleep 0.2
done

if [[ "$GAME_OVER" != "true" ]]; then
    log "FAIL: timed out reaching game-over branch"
    FAILURES=$((FAILURES + 1))
else
    for _ in {1..20}; do
        close_visible_dialog_if_any "game-over-settle" || break
        sleep 0.2
    done

    for _ in {1..200}; do
        CANDIDATE_FINAL=$(api GET /state 2>/dev/null || true)
        if [[ -z "$CANDIDATE_FINAL" ]]; then
            sleep 0.2
            continue
        fi

        REMAINING_CAND=$(jget "$CANDIDATE_FINAL" '((o.tournament && o.tournament.playersRemaining) ?? 99)')
        STANDINGS_COUNT_CAND=$(jget "$CANDIDATE_FINAL" '(o.tournament && o.tournament.standings || []).length')
        TOTAL_PLAYERS_CAND=$(jget "$CANDIDATE_FINAL" 'o.tournament && o.tournament.totalPlayers || 0')
        SETTLED_CAND=$(jget "$CANDIDATE_FINAL" '(() => { const t=o.tournament||{}; const st=t.standings||[]; const total=Number(t.totalPlayers||0); if(total<=0||st.length!==total) return false; const places=st.map(s=>Number(s&&s.place||0)); if(places.some(p=>p<=0)) return false; if(places.filter(p=>p===1).length!==1) return false; const prizesPaid=Number(t.prizesPaid||0); const prizePool=Number(t.prizePool||0); return prizesPaid>0 || prizePool===0; })()')
        if [[ "$REMAINING_CAND" -le 1 ]] && [[ "$STANDINGS_COUNT_CAND" -eq "$TOTAL_PLAYERS_CAND" ]] && [[ "$TOTAL_PLAYERS_CAND" -gt 0 ]] && [[ "$SETTLED_CAND" == "true" ]]; then
            FINAL_STATE_GO="$CANDIDATE_FINAL"
            break
        fi
        MODE_CAND=$(jget "$CANDIDATE_FINAL" 'o.inputMode || "NONE"')
        case "$MODE_CAND" in
            DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)
                advance_non_betting "$CANDIDATE_FINAL" > /dev/null 2>&1 || true
                ;;
        esac
        close_visible_dialog_if_any "game-over-finalize" > /dev/null 2>&1 || true
        sleep 0.2
    done

    WIDGETS_GO=$(api GET /ui/dashboard/widgets 2>/dev/null || true)
    REMAINING_FINAL=$(jget "$FINAL_STATE_GO" '((o.tournament && o.tournament.playersRemaining) ?? 99)')
    LIFECYCLE_FINAL=$(jget "$FINAL_STATE_GO" 'o.lifecyclePhase || "NONE"')
    PRIZE_POOL_FINAL=$(jget "$FINAL_STATE_GO" 'o.tournament && o.tournament.prizePool || 0')
    PRIZES_PAID_FINAL=$(jget "$FINAL_STATE_GO" 'o.tournament && o.tournament.prizesPaid || 0')
    STANDINGS_JSON_FINAL=$(jget "$FINAL_STATE_GO" 'JSON.stringify(o.tournament && o.tournament.standings || [])')
    log "  INFO: game-over summary: remaining=$REMAINING_FINAL lifecycle=$LIFECYCLE_FINAL prizePool=$PRIZE_POOL_FINAL prizesPaid=$PRIZES_PAID_FINAL standings=$STANDINGS_JSON_FINAL"
    check_true "$(jget "$WIDGETS_GO" "Number((o.widgets && o.widgets.rank && o.widgets.rank.data && o.widgets.rank.data.playersRemaining) ?? -1) === Number($REMAINING_FINAL)")" "rank playersRemaining matches final state"

    if [[ "$REMAINING_FINAL" -le 1 ]]; then
        check_true "$(jget "$WIDGETS_GO" 'Number((o.widgets && o.widgets.rank && o.widgets.rank.data && o.widgets.rank.data.playersRemaining) ?? 99) <= 1')" "rank playersRemaining <= 1 at game-over"
    fi

    if echo "$LIFECYCLE_FINAL" | grep -qiE "gameover|GameOver|PracticeGameOver"; then
        check_true "$(jget "$WIDGETS_GO" 'o.widgets && o.widgets.rank && o.widgets.rank.data && o.widgets.rank.data.isGameOver === true')" "rank widget reports game-over in game-over lifecycle"
    fi

    TOTAL_PLAYERS_FINAL=$(jget "$FINAL_STATE_GO" 'o.tournament && o.tournament.totalPlayers || 0')
    STANDINGS_COUNT_FINAL=$(jget "$FINAL_STATE_GO" '(o.tournament && o.tournament.standings || []).length')
    check_eq "$STANDINGS_COUNT_FINAL" "$TOTAL_PLAYERS_FINAL" "standings count matches total players"

    check_true "$(jget "$FINAL_STATE_GO" '(() => { const st=(o.tournament&&o.tournament.standings)||[]; const p=st.map(s=>Number(s&&s.place||0)).filter(v=>v>0).sort((a,b)=>a-b); if(p.length===0) return false; if(new Set(p).size!==p.length) return false; for(let i=0;i<p.length;i++){ if(p[i]!==i+1) return false; } return true; })()')" "standings places are unique and contiguous"

    WINNERS_FINAL=$(jget "$FINAL_STATE_GO" '(o.tournament && o.tournament.standings || []).filter(s => Number(s && s.place || 0) === 1).length')
    if [[ "$REMAINING_FINAL" -le 1 ]]; then
        check_eq "$WINNERS_FINAL" "1" "exactly one first-place finisher"
    fi

    SUM_PRIZES_FINAL=$(jget "$FINAL_STATE_GO" '(o.tournament && o.tournament.standings || []).reduce((s,p)=>s+Number((p&&p.prize)||0),0)')
    check_eq "$SUM_PRIZES_FINAL" "$PRIZES_PAID_FINAL" "sum(standings.prize) equals prizesPaid"
    if [[ "$PRIZE_POOL_FINAL" =~ ^[0-9]+$ ]] && [[ "$PRIZES_PAID_FINAL" =~ ^[0-9]+$ ]]; then
        if [[ "$PRIZES_PAID_FINAL" -le "$PRIZE_POOL_FINAL" ]]; then
            log "  OK: prizesPaid <= prizePool ($PRIZES_PAID_FINAL <= $PRIZE_POOL_FINAL)"
        else
            log "FAIL: prizesPaid > prizePool ($PRIZES_PAID_FINAL > $PRIZE_POOL_FINAL)"
            FAILURES=$((FAILURES + 1))
        fi
    else
        log "FAIL: invalid prizePool/prizesPaid values"
        FAILURES=$((FAILURES + 1))
    fi

    PAYOUT_COUNT_FINAL=$(jget "$FINAL_STATE_GO" '(o.tournament && o.tournament.payoutTable || []).length')
    check_eq "$PAYOUT_COUNT_FINAL" "$TOTAL_PLAYERS_FINAL" "payout table row count"

    NONZERO_PAYOUT_ROWS=$(jget "$FINAL_STATE_GO" '(o.tournament && o.tournament.payoutTable || []).filter(r => Number(r && r.payout || 0) > 0).length')
    if [[ "$TOTAL_PLAYERS_FINAL" -gt 1 ]]; then
        check_true "$(jget "$FINAL_STATE_GO" '((o.tournament && o.tournament.payoutTable) || []).some(r => Number(r && r.payout || 0) > 0)')" "payout table has non-zero rewards"
        check_true "$(jget "$FINAL_STATE_GO" 'Number((o.tournament && o.tournament.prizesPaid) ?? 0) > 0')" "prizesPaid > 0 at terminal state"
    fi

    HUMAN_PLACE_FINAL=$(jget "$FINAL_STATE_GO" '(o.tournament && o.tournament.standings || []).find(s => s && s.isHuman)?.place || 0')
    HUMAN_PRIZE_FINAL=$(jget "$FINAL_STATE_GO" '(o.tournament && o.tournament.standings || []).find(s => s && s.isHuman)?.prize || 0')
    check_eq "$(jget "$WIDGETS_GO" 'o.widgets && o.widgets.rank && o.widgets.rank.data && o.widgets.rank.data.place || 0')" "$HUMAN_PLACE_FINAL" "rank widget place matches standings"
    check_eq "$(jget "$WIDGETS_GO" 'o.widgets && o.widgets.rank && o.widgets.rank.data && o.widgets.rank.data.prize || 0')" "$HUMAN_PRIZE_FINAL" "rank widget prize matches standings"
fi

screenshot "dashboard-widgets-strict"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES strict dashboard widget assertion(s) failed"
fi

pass "Strict dashboard widget semantics verified on human turn"
