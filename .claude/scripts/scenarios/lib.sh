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
PORT_FILE="$HOME/.ddpoker/control-server.port"
KEY_FILE="$HOME/.ddpoker/control-server.key"
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

cleanup() {
    if [[ -n "$JAVA_PID" ]] && kill -0 "$JAVA_PID" 2>/dev/null; then
        log "Stopping Java process $JAVA_PID"
        kill "$JAVA_PID" 2>/dev/null || true
        sleep 1
        kill -9 "$JAVA_PID" 2>/dev/null || true
    fi
    rm -f "$HOME/.ddpoker/games.mv.db" "$HOME/.ddpoker/games.trace.db" 2>/dev/null || true
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
    node -e "try{const o=JSON.parse(process.argv[1]);const r=($expr);process.stdout.write(String(r==null?'':r));}catch(e){process.stdout.write('');}" -- "$json" 2>/dev/null
}

api() {
    local method="$1" path="$2"
    shift 2
    curl -s -f -H "X-Control-Key: $KEY" \
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

    # Kill stale processes
    if command -v taskkill &>/dev/null; then
        taskkill //F //IM java.exe 2>/dev/null || true
    elif command -v pkill &>/dev/null; then
        pkill -f "DDPokerCE" 2>/dev/null || true
    fi
    sleep 2
    rm -f "$HOME/.ddpoker/games.mv.db" "$HOME/.ddpoker/games.trace.db" \
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
    until [[ -f "$PORT_FILE" && -f "$KEY_FILE" ]]; do
        sleep 0.5
        [[ $(($(date +%s) - start)) -gt 60 ]] && die "Timed out waiting for port/key files"
        kill -0 "$JAVA_PID" 2>/dev/null || die "Java process died — see $GAME_LOG"
    done
    PORT=$(cat "$PORT_FILE")
    KEY=$(cat "$KEY_FILE")
    log "Control server on port $PORT"

    until api GET /health 2>/dev/null | grep -q '"ok"'; do
        sleep 0.5
        [[ $(($(date +%s) - start)) -gt 60 ]] && die "Timed out waiting for /health"
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
