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

All valid action types:

| Type | Mode | Notes |
|------|------|-------|
| `FOLD` | CHECK_BET / CHECK_RAISE / CALL_RAISE | |
| `CHECK` | CHECK_BET / CHECK_RAISE | No existing bet |
| `CALL` | CALL_RAISE | Match existing bet |
| `BET` | CHECK_BET | `amount` required |
| `RAISE` | CHECK_RAISE / CALL_RAISE | `amount` required |
| `ALL_IN` | any betting mode | Uses `amount=0`; engine uses full stack |
| `DEAL` | DEAL | Deal next hand |
| `CONTINUE` | CONTINUE | Advance past pause/showdown |
| `CONTINUE_LOWER` | CONTINUE_LOWER | Variant continue |
| `REBUY` | REBUY_CHECK | Accept rebuy |
| `ADDON` | REBUY_CHECK | Accept add-on |
| `DECLINE_REBUY` | REBUY_CHECK | Skip (timer expires naturally) |
| `ADVISOR_DO_IT` | any betting mode | Execute the advisor's recommended action |

### Card Injection

Stage a specific card order for the **next** hand dealt:

```bash
# Explicit card order: seat0-c1, seat0-c2, seat1-c1, seat1-c2, ...,
#   burn, flop1, flop2, flop3, burn, turn, burn, river
# For 3 players that's 14 cards total.
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"cards": ["As","Ks","2d","3c","7h","8h","Qd","Jd","Td","9d","4c","2h","3s","5c"]}' \
     http://localhost:$PORT/cards/inject

# Reproducible seeded shuffle
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"seed": 42}' \
     http://localhost:$PORT/cards/inject

# Clear pending injection
curl -s -H "$H" -X DELETE http://localhost:$PORT/cards/inject
```

The injection is consumed once (one hand) and then cleared automatically.

### Options and Cheat Toggles

```bash
# Read all current options
curl -s -H "$H" http://localhost:$PORT/options | jq .

# Set one or more options
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"cheat.neverbroke": true, "gameplay.pauseAllin": true, "gameplay.aiDelayMs": 500}' \
     http://localhost:$PORT/options
```

Boolean option keys: `cheat.neverbroke`, `cheat.aifaceup`, `cheat.showfold`,
`cheat.showmuck`, `cheat.showdown`, `cheat.popups`, `cheat.mouseover`,
`cheat.pausecards`, `gameplay.pauseAllin`, `gameplay.pauseColor`, `gameplay.zipMode`

Integer option keys: `gameplay.aiDelayMs`

### Mid-Game State Manipulation (`/cheat`)

```bash
# Set a player's chip count
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"action": "setChips", "seat": 2, "amount": 100}' \
     http://localhost:$PORT/cheat

# Advance to blind level 3 (0-based)
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"action": "setLevel", "level": 3}' \
     http://localhost:$PORT/cheat

# Move dealer button to seat 1
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"action": "setButton", "seat": 1}' \
     http://localhost:$PORT/cheat

# Mark player as eliminated
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
     -d '{"action": "eliminatePlayer", "seat": 0}' \
     http://localhost:$PORT/cheat
```

### WebSocket Log

```bash
# Last 40 WebSocket messages and last 50 game events
curl -s -H "$H" http://localhost:$PORT/ws-log | jq .
```

Response shape:
```json
{
  "messages": [{"ms": 1234567890, "direction": "OUT", "type": "PLAYER_ACTION", "payload": "BET:200"}],
  "events":   [{"ms": 1234567890, "type": "NEW_HAND", "table": 1}]
}
```

Use this to verify what WebSocket message type was actually sent for an action (e.g., confirm that
ALL_IN in CHECK_BET mode sends `BET` not `RAISE` on the wire).

### Invariant Validation

```bash
curl -s -H "$H" http://localhost:$PORT/validate | jq .
```

Response shape:
```json
{
  "chipConservation": {
    "valid": true,
    "tables": [{
      "id": 1, "playerChips": 4400, "inPot": 100, "total": 4500,
      "buyinPerPlayer": 1500, "numPlayers": 3, "expectedTotal": 4500
    }]
  },
  "inputModeConsistent": true,
  "warnings": []
}
```

`chipConservation.valid` is `false` if `playerChips + inPot != buyinPerPlayer * numPlayers` on
any table. `inputModeConsistent` is `false` if a betting input mode is active but no hand is
running. `warnings` lists human-readable violation descriptions.

### Richer State Fields

The `/state` response includes additional fields after D4:

| Field | Where | Meaning |
|-------|-------|---------|
| `tables[].chipConservation` | always | `{playerTotal, inPot, sum}` — live chip total for quick sanity checks |
| `tables[].currentBets` | during hand | `{"seat0": N, "seat2": M}` — per-player bets in the current round |
| `currentAction.advisorAdvice` | human turn | Advisor recommendation text (null if not computed yet) |
| `currentAction.advisorTitle` | human turn | Short advisor title (e.g., "Call") |
| `recentEvents` | always | Last 20 game events from the ring buffer |

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

---

## Overriding AI Action Delay for Fast Tests

By default the embedded game server adds a randomized 500–2000ms delay before
each AI action (base `game.server.ai-action-delay-ms=1000`, randomized in
`[base/2, base*2]`). This makes the game feel natural but makes automated tests
very slow — a 3-player game can take minutes instead of seconds.

Override it to 0 for instant AI responses:

```bash
java -Dgame.server.ai-action-delay-ms=0 \
     -jar poker/target/DDPokerCE-3.3.0.jar
```

The smoke test script (`test-practice-game.sh`) defaults to `--ai-delay-ms 0`
so automated runs are fast. Pass a non-zero value to test realistic timing:

```bash
bash .claude/scripts/test-practice-game.sh --ai-delay-ms 200
```

The `--ai-delay-ms` value is passed directly to `-Dgame.server.ai-action-delay-ms`
at JVM launch. With `ai-action-delay-ms=0` the `> 0` guard in
`ServerPlayerActionProvider` skips the sleep entirely — no randomization occurs.

### Enable in `application.properties` (persistent)

Add to `code/poker/src/main/resources/application.properties` or
`application-embedded.properties`:

```properties
logging.level.com.donohoedigital.games.poker.online=DEBUG
logging.level.com.donohoedigital.games.poker.gameserver=DEBUG
```

---

## Scenario Scripts

Ready-to-run scenario scripts in `.claude/scripts/scenarios/`. All scripts share
`.claude/scripts/scenarios/lib.sh` for common launch/cleanup/helper logic.

| Script | What It Tests | Key Assertion |
|--------|--------------|---------------|
| `test-chip-conservation.sh` | Play N hands with CALL strategy | `/validate` passes after every hand |
| `test-advisor-do-it.sh` | POST `ADVISOR_DO_IT` on human turn | inputMode changes (game advanced); advisorAdvice/Title present |
| `test-action-type-log.sh` | POST `ALL_IN` in CHECK_BET mode | `/ws-log` shows outbound `PLAYER_ACTION` payload starting with `BET:` |
| `test-gameover-ranks.sh` | FOLD strategy to completion | `playersRemaining == 1` at end; `/validate` passes |
| `test-dashboard-panels.sh` | Inspect state on human turn | `advisorAdvice`, `advisorTitle`, `pot`, `availableActions` all populated |
| `test-neverbroke.sh` | Drain human chips → all-in bust | `inputMode` becomes `REBUY_CHECK` |
| `test-pause-allin.sh` | ALL_IN with `gameplay.pauseAllin=true` | `inputMode` becomes `CONTINUE`; POST `CONTINUE` advances |
| `test-allin-side-pot.sh` | Staggered stacks → side pot | `/validate` passes after pot distribution |

Common options (all scripts):

```bash
--skip-build     # Reuse existing JAR (no mvn build)
--skip-launch    # Reuse already-running game (no launch)
--ai-delay-ms N  # AI action delay (default: 0)
--stuck-timeout N # Seconds before declaring stuck (default: 30)
--log-dir DIR    # Where to write logs + screenshots (default: /tmp/ddpoker-scenario)
```

Example usage:

```bash
# Full end-to-end chip conservation test
bash .claude/scripts/scenarios/test-chip-conservation.sh --hands 10

# Advisor test (build already done)
bash .claude/scripts/scenarios/test-advisor-do-it.sh --skip-build

# Side-pot regression (reuse a running game)
bash .claude/scripts/scenarios/test-allin-side-pot.sh --skip-build --skip-launch

# Game-over test with 4 players
bash .claude/scripts/scenarios/test-gameover-ranks.sh --players 4
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
