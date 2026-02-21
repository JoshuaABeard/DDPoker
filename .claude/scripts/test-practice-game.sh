#!/usr/bin/env bash
# test-practice-game.sh — Automated practice game integration test
#
# Drives the desktop client via the Game Control Server HTTP API.
# No Swing/UI interaction required. Uses jq for JSON parsing.
#
# Usage:
#   bash .claude/scripts/test-practice-game.sh [options]
#
# Options:
#   --skip-build      Skip mvn build (use existing JAR)
#   --players N       Number of players (default: 3)
#   --games N         Number of consecutive games to run (default: 1)
#   --strategy FOLD   Action strategy: FOLD or CALL (default: FOLD)
#   --stuck-timeout N Seconds before declaring stuck (default: 15)
#   --log-dir DIR     Where to write logs and screenshots (default: /tmp/ddpoker-test)
#
# Dependencies: node (for JSON parsing), curl, mvn (unless --skip-build)

set -euo pipefail

# ── Defaults ────────────────────────────────────────────────────────────────
SKIP_BUILD=false
NUM_PLAYERS=3
NUM_GAMES=1
STRATEGY="FOLD"
STUCK_TIMEOUT=45
LOG_DIR="/tmp/ddpoker-test"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CODE_DIR="$REPO_ROOT/code"
JAR="$CODE_DIR/poker/target/DDPokerCE-3.3.0.jar"
PORT_FILE="$HOME/.ddpoker/control-server.port"
KEY_FILE="$HOME/.ddpoker/control-server.key"
JAVA_PID=""

# ── Parse args ───────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build)   SKIP_BUILD=true ;;
    --players)      NUM_PLAYERS="$2"; shift ;;
    --games)        NUM_GAMES="$2"; shift ;;
    --strategy)     STRATEGY="$2"; shift ;;
    --stuck-timeout) STUCK_TIMEOUT="$2"; shift ;;
    --log-dir)      LOG_DIR="$2"; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
  shift
done

mkdir -p "$LOG_DIR"
GAME_LOG="$LOG_DIR/game.log"
STUCK_DIR="$LOG_DIR/stuck"
mkdir -p "$STUCK_DIR"

# ── Helpers ──────────────────────────────────────────────────────────────────
log() { echo "[$(date '+%H:%M:%S')] $*"; }
die() { log "ERROR: $*"; cleanup; exit 1; }

# JSON field extractor using Node.js (no jq required)
# Usage: jget "$json" '.field.nested // "default"'
jget() {
  local json="$1" expr="$2"
  node -e "try { const o=JSON.parse(process.argv[1]); const r=($expr); process.stdout.write(String(r==null?'':r)); } catch(e) { process.stdout.write(''); }" -- "$json" 2>/dev/null
}

cleanup() {
  if [[ -n "$JAVA_PID" ]] && kill -0 "$JAVA_PID" 2>/dev/null; then
    log "Killing Java process $JAVA_PID"
    kill "$JAVA_PID" 2>/dev/null || true
    sleep 1
    kill -9 "$JAVA_PID" 2>/dev/null || true
  fi
  # Clean up H2 lock files
  rm -f "$HOME/.ddpoker/games.mv.db" "$HOME/.ddpoker/games.trace.db" 2>/dev/null || true
}
trap cleanup EXIT

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

# ── Step 1: Kill stale Java processes and clean H2 locks ────────────────────
log "Killing any stale Java processes..."
if command -v taskkill &>/dev/null; then
  taskkill //F //IM java.exe 2>/dev/null || true
elif command -v pkill &>/dev/null; then
  pkill -f "DDPokerCE" 2>/dev/null || true
fi
sleep 2
rm -f "$HOME/.ddpoker/games.mv.db" "$HOME/.ddpoker/games.trace.db" 2>/dev/null || true

# ── Step 2: Build (unless skipped) ──────────────────────────────────────────
if [[ "$SKIP_BUILD" == false ]]; then
  log "Building desktop client with -P dev..."
  mvn clean package -DskipTests -P dev -f "$CODE_DIR/pom.xml" \
    > "$LOG_DIR/build.log" 2>&1 \
    || die "Build failed — see $LOG_DIR/build.log"
  log "Build complete: $JAR"
else
  log "Skipping build (--skip-build)"
  [[ -f "$JAR" ]] || die "JAR not found: $JAR"
fi

# ── Step 3: Delete stale port/key files, then launch the client ─────────────
rm -f "$PORT_FILE" "$KEY_FILE"
log "Launching DDPoker with debug logging..."
java \
  -Dlogging.level.com.donohoedigital.games.poker.online=DEBUG \
  -Dlogging.level.com.donohoedigital.games.poker.gameserver=DEBUG \
  -Djava.awt.headless=false \
  -jar "$JAR" \
  > "$GAME_LOG" 2>&1 &
JAVA_PID=$!
log "Java PID: $JAVA_PID"

# ── Step 4: Wait for control server to come up ──────────────────────────────
log "Waiting for control server..."
WAIT_START=$(date +%s)
until [[ -f "$PORT_FILE" && -f "$KEY_FILE" ]]; do
  sleep 0.5
  [[ $(($(date +%s) - WAIT_START)) -gt 60 ]] && die "Timed out waiting for port/key files"
  kill -0 "$JAVA_PID" 2>/dev/null || die "Java process died before writing port file — see $GAME_LOG"
done
PORT=$(cat "$PORT_FILE")
KEY=$(cat "$KEY_FILE")
log "Control server port: $PORT"

until api GET /health 2>/dev/null | grep -q '"ok"'; do
  sleep 0.5
  [[ $(($(date +%s) - WAIT_START)) -gt 60 ]] && die "Timed out waiting for /health"
  kill -0 "$JAVA_PID" 2>/dev/null || die "Java process died — see $GAME_LOG"
done
log "Control server healthy"

# ── Step 5: Run N games ──────────────────────────────────────────────────────
TOTAL_HANDS=0
GAME_FAILURES=0

for GAME_NUM in $(seq 1 "$NUM_GAMES"); do
  log "━━━ Game $GAME_NUM / $NUM_GAMES ━━━"

  # Start practice game
  log "Starting $NUM_PLAYERS-player practice game (strategy: $STRATEGY)..."
  RESULT=$(api_post_json /game/start "{\"numPlayers\": $NUM_PLAYERS}")
  echo "$RESULT" | grep -q '"accepted":true' \
    || die "Game start failed: $RESULT"
  log "Game started OK"

  screenshot "game${GAME_NUM}-start"

  # Poll loop
  LAST_STATE_KEY=""
  LAST_CHANGE=$(date +%s)
  HANDS_THIS_GAME=0
  GAME_OVER=false
  ACTIONS_TAKEN=0

  while true; do
    STATE=$(api GET /state 2>/dev/null) || { sleep 0.5; continue; }

    # Extract key fields using Node.js JSON parser
    INPUT_MODE=$(jget "$STATE" 'o.inputMode || "NONE"')
    PLAYERS_REMAINING=$(jget "$STATE" 'o.tournament && o.tournament.playersRemaining || 0')
    GAME_PHASE=$(jget "$STATE" 'o.gamePhase || "NONE"')
    TABLE_ROUND=$(jget "$STATE" 'o.tables && o.tables[0] && o.tables[0].round || "NONE"')
    POT=$(jget "$STATE" 'o.tables && o.tables[0] && o.tables[0].pot || 0')
    IS_HUMAN_TURN=$(jget "$STATE" 'o.currentAction && o.currentAction.isHumanTurn || false')
    LIFECYCLE=$(jget "$STATE" 'o.lifecyclePhase || "NONE"')

    # Stuck detection: track a composite state key
    STATE_KEY="${INPUT_MODE}|${GAME_PHASE}|${TABLE_ROUND}|${POT}|${LIFECYCLE}"
    if [[ "$STATE_KEY" != "$LAST_STATE_KEY" ]]; then
      LAST_STATE_KEY="$STATE_KEY"
      LAST_CHANGE=$(date +%s)
    fi

    ELAPSED=$(($(date +%s) - LAST_CHANGE))
    if [[ $ELAPSED -gt $STUCK_TIMEOUT ]]; then
      # Distinguish post-GAME_COMPLETE freeze from a genuine stuck state.
      # After GAME_COMPLETE the server sends no more updates (modal dialog blocks
      # the Swing UI), so the state freezes in QUITSAVE with exactly 1 player
      # having chips (the tournament winner).
      PLAYERS_WITH_CHIPS=$(jget "$STATE" '(o.tables||[]).reduce((s,t)=>s+(t.players||[]).filter(p=>p&&p.chips>0).length,0)')
      if [[ "$INPUT_MODE" == "QUITSAVE" && "$PLAYERS_WITH_CHIPS" == "1" && "$ACTIONS_TAKEN" -gt 0 ]]; then
        log "Game $GAME_NUM complete (post-GAME_COMPLETE freeze: 1 player with chips, state frozen ${ELAPSED}s)!"
        screenshot "game${GAME_NUM}-over"
        TOTAL_HANDS=$((TOTAL_HANDS + HANDS_THIS_GAME))
        GAME_OVER=true
        break
      fi
      STUCK_LABEL="game${GAME_NUM}-stuck-action${ACTIONS_TAKEN}"
      log "STUCK after ${ELAPSED}s in state: $STATE_KEY"
      node -e "try{process.stdout.write(JSON.stringify(JSON.parse(process.argv[1]),null,2))}catch(e){process.stdout.write(process.argv[1])}" -- "$STATE" > "$STUCK_DIR/${STUCK_LABEL}-state.json"
      screenshot "stuck/${STUCK_LABEL}"
      log "Stuck state written to $STUCK_DIR/"
      GAME_FAILURES=$((GAME_FAILURES + 1))
      break
    fi

    # Game over detection: 1 player remaining (use ACTIONS_TAKEN since auto-deal
    # games never show DEAL inputMode, so HANDS_THIS_GAME stays at 0)
    if [[ "$PLAYERS_REMAINING" -le 1 && "$ACTIONS_TAKEN" -gt 0 ]]; then
      log "Game $GAME_NUM complete! Hands played: $HANDS_THIS_GAME, Actions: $ACTIONS_TAKEN"
      screenshot "game${GAME_NUM}-over"
      TOTAL_HANDS=$((TOTAL_HANDS + HANDS_THIS_GAME))
      GAME_OVER=true
      break
    fi

    # Lifecycle-based game over detection
    if echo "$LIFECYCLE" | grep -qi "gameover\|PracticeGameOver\|GameOver"; then
      log "Game $GAME_NUM complete via lifecycle! Hands: $HANDS_THIS_GAME, Actions: $ACTIONS_TAKEN"
      screenshot "game${GAME_NUM}-over"
      TOTAL_HANDS=$((TOTAL_HANDS + HANDS_THIS_GAME))
      GAME_OVER=true
      break
    fi

    # Act based on inputMode
    case "$INPUT_MODE" in
      CHECK_BET|CHECK_RAISE|CALL_RAISE)
        if [[ "$IS_HUMAN_TURN" == "true" ]]; then
          if [[ "$STRATEGY" == "FOLD" ]]; then
            ACTION="FOLD"
          else
            # CALL strategy: use CHECK if available, else CALL
            AVAIL=$(jget "$STATE" '(o.currentAction&&o.currentAction.availableActions||[]).join("\n")')
            if echo "$AVAIL" | grep -q "^CHECK$"; then
              ACTION="CHECK"
            else
              ACTION="CALL"
            fi
          fi
          RESP=$(api_post_json /action "{\"type\":\"$ACTION\"}" 2>/dev/null)
          if echo "$RESP" | grep -q '"accepted":true'; then
            ACTIONS_TAKEN=$((ACTIONS_TAKEN + 1))
            log "  Hand $HANDS_THIS_GAME | $INPUT_MODE → $ACTION (actions: $ACTIONS_TAKEN)"
          else
            log "  WARN: action $ACTION rejected: $RESP"
          fi
        fi
        ;;

      DEAL)
        RESP=$(api_post_json /action '{"type":"DEAL"}' 2>/dev/null)
        if echo "$RESP" | grep -q '"accepted":true'; then
          HANDS_THIS_GAME=$((HANDS_THIS_GAME + 1))
          log "  Dealt hand $HANDS_THIS_GAME"
        fi
        ;;

      CONTINUE|CONTINUE_LOWER)
        RESP=$(api_post_json /action "{\"type\":\"$INPUT_MODE\"}" 2>/dev/null)
        if echo "$RESP" | grep -q '"accepted":true'; then
          log "  Continued ($INPUT_MODE)"
        fi
        ;;

      REBUY_CHECK)
        api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
        log "  Declined rebuy"
        ;;

      QUITSAVE|NONE|*)
        # AI acting or transitioning — wait
        sleep 0.2
        continue
        ;;
    esac

    sleep 0.15
  done

  if [[ "$GAME_OVER" == false && "$GAME_FAILURES" -gt 0 ]]; then
    log "Game $GAME_NUM FAILED (stuck)"
  fi

  # Brief pause between games
  if [[ $GAME_NUM -lt $NUM_GAMES ]]; then
    log "Pausing 2s before next game..."
    sleep 2
  fi
done

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
log "━━━ Results ━━━"
log "Games run:    $NUM_GAMES"
log "Games failed: $GAME_FAILURES"
log "Total hands:  $TOTAL_HANDS"
log "Logs:         $LOG_DIR"

if [[ $GAME_FAILURES -gt 0 ]]; then
  log "RESULT: FAILED — $GAME_FAILURES of $NUM_GAMES games stuck"
  log "Stuck snapshots: $STUCK_DIR/"
  log "Game log:        $GAME_LOG"
  exit 1
else
  log "RESULT: PASSED — all $NUM_GAMES games completed"
  exit 0
fi
