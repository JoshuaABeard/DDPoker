#!/usr/bin/env bash
# lib.sh — Shared library for DDPoker scenario test scripts.
#
# Usage: source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
#
# Provides:
#   - Common variables: REPO_ROOT, CODE_DIR, JAR, PORT, KEY
#   - Functions: log, die, cleanup, api, api_post_json, jget,
#                wait_health, launch_game, wait_mode, wait_human_turn
#
# After sourcing, call:
#   lib_parse_args "$@"          # parse --skip-build, --skip-launch, --ai-delay-ms
#   lib_launch                   # build + start the game, sets PORT/KEY
#   lib_start_game [numPlayers]  # POST /game/start

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
CODE_DIR="$REPO_ROOT/code"
JAR="$CODE_DIR/poker/target/DDPokerCE-3.3.0.jar"
# On Windows, DDPoker stores config in %APPDATA%\ddpoker; on Unix, ~/.ddpoker
if [[ -n "${APPDATA:-}" ]]; then
    _DDPOKER_DIR="$(cygpath -u "$APPDATA")/ddpoker"
else
    _DDPOKER_DIR="$HOME/.ddpoker"
fi
PORT_FILE="$_DDPOKER_DIR/control-server.port"
KEY_FILE="$_DDPOKER_DIR/control-server.key"
LOG_DIR="${LOG_DIR:-/tmp/ddpoker-scenario}"
GAME_LOG="$LOG_DIR/game.log"
JAVA_PID=""
PORT=""
KEY=""

# Defaults (may be overridden before calling lib_parse_args)
SKIP_BUILD="${SKIP_BUILD:-false}"
SKIP_LAUNCH="${SKIP_LAUNCH:-false}"
AI_DELAY_MS="${AI_DELAY_MS:-0}"
STUCK_TIMEOUT="${STUCK_TIMEOUT:-30}"

mkdir -p "$LOG_DIR"

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
die()  { log "FAIL: $*"; cleanup; exit 1; }
pass() { log "PASS: $*"; }

record_failure() {
    local msg="$1"
    log "FAIL: $msg"
    if [[ -n "${FAILURES+x}" ]]; then
        FAILURES=$((FAILURES + 1))
    fi
}

assert_json_field() {
    local json="$1" expr="$2" desc="$3"
    local val
    val=$(jget "$json" "$expr")
    if [[ -z "$val" || "$val" == "undefined" || "$val" == "null" ]]; then
        record_failure "$desc missing ($expr)"
        return
    fi
    log "  OK: $desc = $val"
}

assert_action_accepted() {
    local response="$1" desc="${2:-action}"
    local accepted
    accepted=$(jget "$response" 'o.accepted||false')
    if [[ "$accepted" != "true" ]]; then
        record_failure "$desc not accepted: $response"
        return
    fi
    log "  OK: $desc accepted"
}

assert_mode_transition() {
    local expected_modes="$1" timeout="${2:-$STUCK_TIMEOUT}" desc="${3:-mode transition}"
    local state mode
    if state=$(wait_mode "$expected_modes" "$timeout" 2>/dev/null); then
        mode=$(jget "$state" 'o.inputMode || "NONE"')
        log "  OK: $desc -> $mode"
        printf '%s' "$state"
    else
        record_failure "$desc timed out waiting for [$expected_modes]"
    fi
}

now_ms() {
    node -e "process.stdout.write(String(Date.now()))" 2>/dev/null
}

close_visible_dialog_if_any() {
    local reason="${1:-progress}"
    local dialogs count dialog_id title close_resp accepted control_resp control_ok

    dialogs=$(api GET /ui/dialogs 2>/dev/null || true)
    if [[ -z "$dialogs" ]]; then
        return 1
    fi

    count=$(jget "$dialogs" 'Number(o && o.dialogCount || 0)')
    if ! [[ "$count" =~ ^[0-9]+$ ]] || [[ "$count" -le 0 ]]; then
        return 1
    fi

    dialog_id=$(jget "$dialogs" '(o.dialogs && o.dialogs[0] && o.dialogs[0].id) || ""')
    title=$(jget "$dialogs" '(o.dialogs && o.dialogs[0] && o.dialogs[0].title) || ""')

    if [[ "$title" == *"Tournament Over"* || "$title" == *"Game Over"* ]]; then
        control_resp=$(api_post_json /control '{"action":"CLOSE_TOURNAMENT_OVER"}' 2>/dev/null || true)
        control_ok=$(jget "$control_resp" 'o.accepted || false')
        if [[ "$control_ok" == "true" ]]; then
            log "  INFO: $reason closed dialog via /control fallback (title='$title')"
            return 0
        fi

        log "  NOTE: $reason could not close tournament/game-over dialog safely (title='$title'): $control_resp"
        return 1
    fi

    if [[ -n "$dialog_id" ]]; then
        close_resp=$(api_post_json /ui/dialogs "{\"action\":\"CLOSE\",\"dialog\":\"$dialog_id\"}" 2>/dev/null || true)
    else
        close_resp=$(api_post_json /ui/dialogs '{"action":"CLOSE"}' 2>/dev/null || true)
    fi

    accepted=$(jget "$close_resp" 'o.accepted || false')
    if [[ "$accepted" == "true" ]]; then
        log "  INFO: $reason closed dialog via /ui/dialogs (title='$title')"
        return 0
    fi

    log "  NOTE: $reason could not close dialog (title='$title'): $close_resp"
    return 1
}

drain_visible_dialogs() {
    local reason="${1:-progress}" max_attempts="${2:-5}" delay_s="${3:-0.2}"
    local i
    for ((i=0; i<max_attempts; i++)); do
        close_visible_dialog_if_any "$reason" || return 0
        sleep "$delay_s"
    done
    return 0
}

wait_for_widget_state() {
    local expr="$1" timeout="${2:-$STUCK_TIMEOUT}" desc="${3:-widget state}"
    local start=$(date +%s)
    local snap=""
    while true; do
        snap=$(api GET /ui/dashboard/widgets 2>/dev/null || true)
        if [[ -n "$snap" ]]; then
            local ok
            ok=$(jget "$snap" "$expr")
            if [[ "$ok" == "true" || "$ok" == "1" ]]; then
                log "  OK: $desc" >&2
                printf '%s' "$snap"
                return 0
            fi
        fi

        local elapsed=$(( $(date +%s) - start ))
        if [[ $elapsed -gt $timeout ]]; then
            log "Timed out waiting for $desc" >&2
            if [[ -n "$snap" ]]; then
                node -e "try{process.stdout.write(JSON.stringify(JSON.parse(process.argv[1]),null,2))}catch(e){}" -- "$snap" >> "$LOG_DIR/last-widgets.json" || true
            fi
            return 1
        fi
        sleep 0.2
    done
}

assert_widget_fresh() {
    local widgets_json="$1" widget_key="$2" max_age_ms="$3" desc="${4:-$2 freshness}"
    local updated
    updated=$(jget "$widgets_json" "Number(o.widgets && o.widgets.${widget_key} && o.widgets.${widget_key}.freshness && o.widgets.${widget_key}.freshness.updatedAtMs || 0)")
    if ! [[ "$updated" =~ ^[0-9]+$ ]] || [[ "$updated" -le 0 ]]; then
        record_failure "$desc missing updatedAtMs"
        return
    fi

    local now age
    now=$(now_ms)
    if ! [[ "$now" =~ ^[0-9]+$ ]]; then
        record_failure "$desc could not compute current time"
        return
    fi
    age=$((now - updated))
    if [[ "$age" -lt 0 || "$age" -gt "$max_age_ms" ]]; then
        record_failure "$desc stale (age=${age}ms, max=${max_age_ms}ms)"
        return
    fi
    log "  OK: $desc age=${age}ms"
}

assert_widget_matches_state() {
    local widgets_json="$1" state_json="$2" widget_expr="$3" state_expr="$4" desc="$5"
    local wv sv
    wv=$(jget "$widgets_json" "$widget_expr")
    sv=$(jget "$state_json" "$state_expr")
    if [[ "$wv" != "$sv" ]]; then
        record_failure "$desc mismatch (widget='$wv' state='$sv')"
        return
    fi
    log "  OK: $desc = $wv"
}

cleanup() {
    if [[ -n "$JAVA_PID" ]] && kill -0 "$JAVA_PID" 2>/dev/null; then
        log "Stopping Java process $JAVA_PID"
        kill "$JAVA_PID" 2>/dev/null || true
        sleep 1
        kill -9 "$JAVA_PID" 2>/dev/null || true
    fi
    rm -f "$_DDPOKER_DIR/games.mv.db" "$_DDPOKER_DIR/games.trace.db" 2>/dev/null || true
}
trap cleanup EXIT

lib_parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --skip-build)    SKIP_BUILD=true ;;
            --skip-launch)   SKIP_LAUNCH=true ;;
            --ai-delay-ms)   AI_DELAY_MS="$2"; shift ;;
            --stuck-timeout) STUCK_TIMEOUT="$2"; shift ;;
            --log-dir)       LOG_DIR="$2"; GAME_LOG="$LOG_DIR/game.log"; mkdir -p "$LOG_DIR"; shift ;;
            *) echo "Unknown option: $1"; exit 1 ;;
        esac
        shift
    done
}

# Extract a field from JSON using node (no jq required).
# Usage: jget "$json" '.field // "default"'
jget() {
    local json="$1" expr="$2"
    # Pass JSON via stdin to avoid Windows CLI argument length limits on large responses.
    printf '%s' "$json" | node -e "
        let d='';
        process.stdin.on('data',c=>d+=c);
        process.stdin.on('end',()=>{
            try{const o=JSON.parse(d);const r=($expr);process.stdout.write(String(r==null?'':r));}
            catch(e){process.stdout.write('');}
        });" 2>/dev/null
}

api() {
    local method="$1" path="$2"
    shift 2
    curl -s --fail-with-body -H "X-Control-Key: $KEY" \
        ${method:+-X "$method"} \
        "$@" \
        "http://localhost:$PORT$path"
}

api_post_json() {
    local path="$1" body="$2"
    api POST "$path" -H "Content-Type: application/json" -d "$body"
}

screenshot() {
    local label="$1"
    api GET /screenshot > "$LOG_DIR/${label}.png" 2>/dev/null || true
}

lib_build() {
    if [[ "$SKIP_BUILD" == false ]]; then
        log "Building with -P dev..."
        mvn clean package -DskipTests -P dev -f "$CODE_DIR/pom.xml" \
            > "$LOG_DIR/build.log" 2>&1 \
            || die "Build failed — see $LOG_DIR/build.log"
        log "Build complete"
    else
        [[ -f "$JAR" ]] || die "JAR not found: $JAR (run without --skip-build)"
    fi
}

lib_launch() {
    lib_build

    if [[ "$SKIP_LAUNCH" == true ]]; then
        [[ -f "$PORT_FILE" && -f "$KEY_FILE" ]] || die "No running game found (missing port/key files)"
        PORT=$(cat "$PORT_FILE")
        KEY=$(cat "$KEY_FILE")
        log "Reusing running game on port $PORT"
        return
    fi

    # Kill stale Java processes (use kill -9 on all java PIDs — works on Git Bash/Windows)
    java_pids=$(ps aux 2>/dev/null | grep java | grep -v grep | awk '{print $2}' || true)
    if [[ -n "$java_pids" ]]; then
        kill -9 $java_pids 2>/dev/null || true
        sleep 3
    fi
    rm -f "$_DDPOKER_DIR/games.mv.db" "$_DDPOKER_DIR/games.trace.db" \
          "$PORT_FILE" "$KEY_FILE" 2>/dev/null || true

    log "Launching DDPoker (ai-delay-ms=$AI_DELAY_MS)..."
    java \
        -Dlogging.level.com.donohoedigital.games.poker.online=DEBUG \
        -Dlogging.level.com.donohoedigital.games.poker.gameserver=DEBUG \
        -Djava.awt.headless=false \
        -Dgame.server.ai-action-delay-ms="$AI_DELAY_MS" \
        -jar "$JAR" \
        > "$GAME_LOG" 2>&1 &
    JAVA_PID=$!
    log "Java PID: $JAVA_PID"

    local start=$(date +%s)
    local startup_timeout=120
    until [[ -f "$PORT_FILE" && -f "$KEY_FILE" ]]; do
        sleep 0.5
        [[ $(($(date +%s) - start)) -gt $startup_timeout ]] && die "Timed out waiting for port/key files"
        kill -0 "$JAVA_PID" 2>/dev/null || die "Java process died — see $GAME_LOG"
    done
    PORT=$(cat "$PORT_FILE")
    KEY=$(cat "$KEY_FILE")
    log "Control server on port $PORT"

    until api GET /health 2>/dev/null | grep -q '"ok"'; do
        sleep 0.5
        [[ $(($(date +%s) - start)) -gt $startup_timeout ]] && die "Timed out waiting for /health"
        kill -0 "$JAVA_PID" 2>/dev/null || die "Java process died — see $GAME_LOG"
    done
    log "Control server healthy"
}

lib_start_game() {
    local num_players="${1:-3}"
    local extra_json="${2:-}"
    local body="{\"numPlayers\": $num_players${extra_json:+, $extra_json}}"
    local result
    result=$(api_post_json /game/start "$body")
    echo "$result" | grep -q '"accepted":true' || die "Game start failed: $result"
    log "Game started ($num_players players)"
}

# Wait for a specific inputMode (or any of several modes separated by |).
# Usage: wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE" 30
wait_mode() {
    local modes="$1"
    local timeout="${2:-$STUCK_TIMEOUT}"
    local start=$(date +%s)
    while true; do
        local state mode
        state=$(api GET /state 2>/dev/null) || { sleep 0.3; continue; }
        mode=$(jget "$state" 'o.inputMode || "NONE"')
        if echo "$mode" | grep -qE "^($modes)$"; then
            echo "$state"
            return 0
        fi

        close_visible_dialog_if_any "wait_mode[$modes]" > /dev/null 2>&1 || true

        local elapsed=$(( $(date +%s) - start ))
        if [[ $elapsed -gt $timeout ]]; then
            log "Timed out waiting for mode [$modes]; current: $mode"
            node -e "try{process.stdout.write(JSON.stringify(JSON.parse(process.argv[1]),null,2))}catch(e){}" -- "$state" >> "$LOG_DIR/last-state.json" || true
            return 1
        fi
        sleep 0.3
    done
}

# Wait specifically for a human-turn betting mode.
wait_human_turn() {
    wait_mode "CHECK_BET|CHECK_RAISE|CALL_RAISE" "${1:-$STUCK_TIMEOUT}"
}

# Advance past a non-betting prompt (DEAL, CONTINUE, CONTINUE_LOWER, REBUY_CHECK).
advance_non_betting() {
    local state="$1"
    local mode
    mode=$(jget "$state" 'o.inputMode || "NONE"')
    case "$mode" in
        DEAL)            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true ;;
        CONTINUE)        api_post_json /action '{"type":"CONTINUE"}' > /dev/null 2>&1 || true ;;
        CONTINUE_LOWER)  api_post_json /action '{"type":"CONTINUE_LOWER"}' > /dev/null 2>&1 || true ;;
        REBUY_CHECK)     api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true ;;
    esac
}

# ─── Puppet mode helpers ────────────────────────────────────────────────────

# Enable puppet mode for a specific seat.
puppet_seat() {
    local seat="$1"
    local result
    result=$(api_post_json /players/puppet "{\"seat\":$seat,\"enabled\":true}") || {
        log "WARN: puppet_seat $seat failed: $result"
        return 1
    }
    local accepted
    accepted=$(jget "$result" 'o.accepted||false')
    if [[ "$accepted" != "true" ]]; then
        log "WARN: puppet_seat $seat not accepted: $result"
        return 1
    fi
    log "  OK: Puppeted seat $seat"
}

# Disable puppet mode for a specific seat.
unpuppet_seat() {
    local seat="$1"
    api_post_json /players/puppet "{\"seat\":$seat,\"enabled\":false}" > /dev/null 2>&1
}

# Submit an action for a puppeted player.
# Usage: puppet_action SEAT TYPE [AMOUNT]
puppet_action() {
    local seat="$1" type="$2" amount="${3:-0}"
    local body
    if [[ "$amount" -gt 0 ]]; then
        body="{\"seat\":$seat,\"type\":\"$type\",\"amount\":$amount}"
    else
        body="{\"seat\":$seat,\"type\":\"$type\"}"
    fi
    local result
    result=$(api_post_json /players/action "$body") || {
        log "WARN: puppet_action seat=$seat type=$type failed: $result"
        return 1
    }
    local accepted
    accepted=$(jget "$result" 'o.accepted||false')
    if [[ "$accepted" != "true" ]]; then
        log "WARN: puppet_action seat=$seat type=$type not accepted: $result"
        return 1
    fi
}

# Enable puppet mode for all AI seats (non-human players).
puppet_all() {
    local state
    state=$(api GET /state 2>/dev/null) || return 1
    local seats
    seats=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p&&!p.isHuman).map(p=>p.seat).join(",")')
    IFS=',' read -ra seat_arr <<< "$seats"
    for s in "${seat_arr[@]}"; do
        [[ -n "$s" ]] && puppet_seat "$s"
    done
}

# ─── Hand result helpers ────────────────────────────────────────────────────

# Get the hand result JSON.
get_hand_result() {
    api GET /hand/result 2>/dev/null
}

# Wait for a hand result to be available (after showdown).
wait_hand_result() {
    local timeout="${1:-$STUCK_TIMEOUT}"
    local start=$(date +%s)
    while true; do
        local result
        result=$(api GET /hand/result 2>/dev/null || true)
        if [[ -n "$result" ]]; then
            local err
            err=$(jget "$result" 'o.error||""')
            if [[ -z "$err" ]]; then
                printf '%s' "$result"
                return 0
            fi
        fi
        local elapsed=$(( $(date +%s) - start ))
        if [[ $elapsed -gt $timeout ]]; then
            log "Timed out waiting for hand result"
            return 1
        fi
        sleep 0.3
    done
}

# Assert the winner of pot N matches expected name.
# Usage: assert_winner RESULT_JSON POT_NUM EXPECTED_NAME
assert_winner() {
    local result="$1" pot_num="$2" expected="$3"
    local actual
    actual=$(jget "$result" "(o.pots[$pot_num].winners[0]||{}).name||''")
    if [[ "$actual" != "$expected" ]]; then
        record_failure "Pot $pot_num winner: expected '$expected', got '$actual'"
        return
    fi
    log "  OK: Pot $pot_num winner = $actual"
}

# Assert the hand description of the winner of pot N contains expected text.
# Usage: assert_hand_description RESULT_JSON POT_NUM EXPECTED_SUBSTR
assert_hand_description() {
    local result="$1" pot_num="$2" expected="$3"
    local actual
    actual=$(jget "$result" "(o.pots[$pot_num].winners[0]||{}).handDescription||''")
    if [[ "$actual" != *"$expected"* ]]; then
        record_failure "Pot $pot_num hand description: expected to contain '$expected', got '$actual'"
        return
    fi
    log "  OK: Pot $pot_num hand description contains '$expected' (full: '$actual')"
}

# Assert number of winners for a pot (for split pot testing).
# Usage: assert_winner_count RESULT_JSON POT_NUM EXPECTED_COUNT
assert_winner_count() {
    local result="$1" pot_num="$2" expected="$3"
    local actual
    actual=$(jget "$result" "(o.pots[$pot_num].winners||[]).length")
    if [[ "$actual" != "$expected" ]]; then
        record_failure "Pot $pot_num winner count: expected $expected, got $actual"
        return
    fi
    log "  OK: Pot $pot_num has $actual winner(s)"
}

# Assert a value from the state matches expected.
# Usage: assert_state_field STATE_JSON EXPR EXPECTED DESC
assert_state_field() {
    local state="$1" expr="$2" expected="$3" desc="$4"
    local actual
    actual=$(jget "$state" "$expr")
    if [[ "$actual" != "$expected" ]]; then
        record_failure "$desc: expected '$expected', got '$actual'"
        return
    fi
    log "  OK: $desc = $actual"
}

# ─── Turn management helpers ────────────────────────────────────────────────

# Wait for any player's turn (human or puppet), return the state JSON.
wait_any_turn() {
    local timeout="${1:-$STUCK_TIMEOUT}"
    local start=$(date +%s)
    while true; do
        local state
        state=$(api GET /state 2>/dev/null) || { sleep 0.15; continue; }
        local mode
        mode=$(jget "$state" 'o.inputMode||"NONE"')

        # Human betting turn
        if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
            printf '%s' "$state"
            return 0
        fi

        # Puppet turn (detected via isPuppetTurn in currentAction)
        local puppet_turn
        puppet_turn=$(jget "$state" 'o.currentAction&&o.currentAction.isPuppetTurn||false')
        if [[ "$puppet_turn" == "true" ]]; then
            printf '%s' "$state"
            return 0
        fi

        # Non-betting modes that need advancing
        if echo "$mode" | grep -qE "^(DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)$"; then
            printf '%s' "$state"
            return 0
        fi

        close_visible_dialog_if_any "wait_any_turn" > /dev/null 2>&1 || true

        local elapsed=$(( $(date +%s) - start ))
        if [[ $elapsed -gt $timeout ]]; then
            log "Timed out waiting for any turn; current mode: $mode"
            return 1
        fi
        sleep 0.15
    done
}

# Play all players through to showdown (everyone checks/calls).
# All non-human players must be puppeted. Handles DEAL/CONTINUE prompts.
# Returns when a hand result is available from /hand/result.
# Usage: play_to_showdown [TIMEOUT]
play_to_showdown() {
    local timeout="${1:-60}"
    local start=$(date +%s)
    while true; do
        local state mode
        state=$(api GET /state 2>/dev/null) || { sleep 0.15; continue; }
        mode=$(jget "$state" 'o.inputMode||"NONE"')

        close_visible_dialog_if_any "showdown-loop" > /dev/null 2>&1 || true

        # Check if hand result is available (more reliable than checking gamePhase)
        local hr
        hr=$(api GET /hand/result 2>/dev/null || true)
        if [[ -n "$hr" ]] && ! echo "$hr" | grep -q '"error"'; then
            log "  Hand result available"
            return 0
        fi

        # Human betting turn
        local is_human
        is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
        if [[ "$is_human" == "true" ]]; then
            local avail
            avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
            if echo "$avail" | grep -q "CHECK"; then
                api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
            else
                api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
            fi
        fi

        # Puppet turn
        local puppet_turn seat avail
        puppet_turn=$(jget "$state" 'o.currentAction&&o.currentAction.isPuppetTurn||false')
        if [[ "$puppet_turn" == "true" ]]; then
            seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')
            avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')
            if [[ -n "$seat" && "$seat" != "undefined" ]]; then
                if echo "$avail" | grep -q "CHECK"; then
                    puppet_action "$seat" "CHECK" || true
                else
                    puppet_action "$seat" "CALL" || true
                fi
            fi
        fi

        # Non-betting modes
        case "$mode" in
            DEAL)            api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true ;;
            CONTINUE)        api_post_json /action '{"type":"CONTINUE"}' > /dev/null 2>&1 || true ;;
            CONTINUE_LOWER)  api_post_json /action '{"type":"CONTINUE_LOWER"}' > /dev/null 2>&1 || true ;;
            REBUY_CHECK)     api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true ;;
        esac

        local elapsed=$(( $(date +%s) - start ))
        if [[ $elapsed -gt $timeout ]]; then
            log "WARN: play_to_showdown timed out after ${timeout}s (mode=$mode)"
            return 1
        fi
        sleep 0.15
    done
}
