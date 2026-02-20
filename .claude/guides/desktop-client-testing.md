# Desktop Client Manual Testing Guide

How to run the desktop client with the dev GameControlServer and drive it via HTTP
for manual and automated testing — without touching the Swing UI.

## Overview

The `-P dev` Maven profile adds `src/dev/java` to the build. This includes
`GameControlServer`, a lightweight HTTP API that lets you:

- Query full game state (input mode, current action, player stacks, etc.)
- Submit player actions (FOLD, CHECK, CALL, BET, RAISE, DEAL, CONTINUE, …)
- Start practice games programmatically

The embedded game server (Spring Boot, WebSocket) and the dev control server both
run inside the same JVM as the desktop client.

---

## Build

From `code/`:

```bash
# With dev control server (needed for HTTP-driven testing)
mvn clean package -DskipTests -P dev

# Without dev control server (normal user build)
mvn clean package -DskipTests
```

Output JAR: `poker/target/DDPokerCE-3.3.0.jar`

---

## Run

```bash
java -jar poker/target/DDPokerCE-3.3.0.jar > /tmp/game.log 2>&1 &
```

On first launch the app creates `~/.ddpoker/` and writes:

| File | Contents |
|------|----------|
| `~/.ddpoker/control-server.port` | TCP port the control server listens on (random) |
| `~/.ddpoker/control-server.key` | API key required on every request |

Read them once:

```bash
PORT=$(cat ~/.ddpoker/control-server.port)
KEY=$(cat ~/.ddpoker/control-server.key)
H="X-Control-Key: $KEY"
```

---

## Control Server Endpoints

### Health

```bash
curl -s -H "$H" http://localhost:$PORT/health
# {"status":"ok"}
```

### Game State

```bash
curl -s -H "$H" http://localhost:$PORT/state | jq .
```

Key fields:

| Field | Meaning |
|-------|---------|
| `inputMode` | Current UI mode (see table below) |
| `currentAction.isHumanTurn` | `true` when the human player must act |
| `currentAction.availableActions` | Valid action types for the current mode |
| `players[].chips` | Each player's chip count |
| `players[].isFolded` | Whether folded this hand |
| `players[].isAllIn` | Whether all-in this hand |
| `handNumber` | Current hand index |

#### Input Modes

| `inputMode` | Meaning | Valid actions |
|-------------|---------|---------------|
| `NONE` | Game not started or between states | — |
| `QUITSAVE` | AI is acting (wait) | — |
| `CHECK_BET` | Human to act, no bet yet | FOLD, CHECK, BET, ALL_IN |
| `CHECK_RAISE` | Human to act, can raise | FOLD, CHECK, RAISE, ALL_IN |
| `CALL_RAISE` | Human to act, must call or raise | FOLD, CALL, RAISE, ALL_IN |
| `DEAL` | Between hands, ready to deal | DEAL |
| `CONTINUE` | Street complete, advance | CONTINUE |
| `CONTINUE_LOWER` | Street complete (lower button) | CONTINUE_LOWER |
| `REBUY_CHECK` | Rebuy/addon prompt | REBUY, ADDON, DECLINE_REBUY |

### Start a Practice Game

```bash
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"numPlayers": 3}' \
     http://localhost:$PORT/game/start
```

Optional body fields:

| Field | Default | Meaning |
|-------|---------|---------|
| `numPlayers` | 3 | Total seats (1 human + N-1 AI) |
| `startingChips` | 1500 | Chips per player |
| `smallBlind` | 10 | Small blind amount |

### Submit a Player Action

```bash
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"type": "FOLD"}' \
     http://localhost:$PORT/action

curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"type": "RAISE", "amount": 400}' \
     http://localhost:$PORT/action
```

Returns `{"accepted": true, "type": "FOLD"}` on success, or
`{"error": "Conflict", "inputMode": "...", "availableActions": [...]}` (409)
if the action doesn't match the current mode.

---

## Polling for Human Turn

The game runs asynchronously. Poll `/state` until it's the human's turn:

```bash
# Bash one-liner
until curl -s -H "$H" http://localhost:$PORT/state | grep -q '"isHumanTurn":true'; do
  sleep 0.5
done
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"type": "CALL"}' http://localhost:$PORT/action
```

Or use `jq` for richer conditions:

```bash
state=$(curl -s -H "$H" http://localhost:$PORT/state)
mode=$(echo "$state" | jq -r '.inputMode')
case "$mode" in
  CHECK_BET|CHECK_RAISE|CALL_RAISE) action="FOLD" ;;
  DEAL)     action="DEAL" ;;
  CONTINUE) action="CONTINUE" ;;
  *) echo "Waiting ($mode)"; sleep 1; continue ;;
esac
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d "{\"type\": \"$action\"}" http://localhost:$PORT/action
```

---

## Enabling Debug Logging

The key log namespaces for tracing the action pipeline:

| Logger | What it traces |
|--------|----------------|
| `WebSocketGameClient` | WebSocket send path: `[WS-SEND]` messages with type, seq, JSON |
| `WebSocketTournamentDirector` | Game state updates, EDT dispatch, table sync |
| `InboundMessageRouter` | Server-side routing: `[ROUTER]` messages for each action |
| `ServerPlayerActionProvider` | `[ACTION-HUMAN]` messages: pending futures, submission, validation |
| `GameInstance` | Hand lifecycle, player join/leave, action dispatch |

### Enable for a single run (system property)

```bash
java -Dlogging.level.com.donohoedigital.games.poker.online=DEBUG \
     -Dlogging.level.com.donohoedigital.games.poker.gameserver=DEBUG \
     -jar poker/target/DDPokerCE-3.3.0.jar
```

### Enable in `application.properties` (persistent)

Add to `code/poker/src/main/resources/application.properties` or
`application-embedded.properties`:

```properties
logging.level.com.donohoedigital.games.poker.online=DEBUG
logging.level.com.donohoedigital.games.poker.gameserver=DEBUG
```

---

## Common Problems

### Game stuck after action submitted

**Symptom:** `/state` shows `isHumanTurn: true` indefinitely after POST to `/action`
returned 200.

**Checklist:**
1. Check server log for `RATE_LIMITED` — the rate limiter (default 1000ms) rejected
   the action. The fix (`actionRateLimiter.removePlayer()` in `InboundMessageRouter`)
   clears the entry after each valid action, so this should not happen in normal play.
2. Check for `OUT_OF_ORDER` — sequence numbers must strictly increase per connection.
   The `WebSocketGameClient` uses an `AtomicLong` counter; reconnects reset the server
   side (`PlayerConnection.lastSequenceNumber = 0`) but the client counter keeps
   incrementing, so this should self-correct.
3. Check that `inputMode` is a betting mode before sending a betting action — sending
   FOLD in `DEAL` mode returns 409 and the server keeps waiting.

### Wrong action sent (e.g. FOLD sent as CHECK)

**Root cause:** `PokerGame.ACTION_FOLD = 1` equals `HandAction.ACTION_CHECK = 1`.
The `PlayerActionListener` in `WebSocketTournamentDirector` uses
`mapPokerGameActionToWsString()` (not `mapActionToWsString()`) to avoid this collision.
If you see an unexpected action, grep for which mapper is being called.

### H2 database locked on restart

The embedded server uses H2. If a previous Java process didn't exit cleanly:

```bash
# Windows — kill all Java processes
powershell -Command "Get-Process java | Stop-Process -Force"

# Then delete the lock files
rm ~/.ddpoker/games.mv.db ~/.ddpoker/games.trace.db 2>/dev/null
```

### "Another copy already running"

Same as H2 lock above — the previous instance holds the database file lock.

---

## Full Automated Test Loop (Node.js example)

See `test-practice.js` at the repo root for a complete example that:
1. Starts a 3-player practice game
2. Polls until human turn
3. Submits FOLD (or CHECK/CALL) until the game ends or a stuck timeout fires

Run it:
```bash
node test-practice.js $PORT $KEY
```

The script is gitignored (`test-*.js`) since it's a dev-only artifact.
