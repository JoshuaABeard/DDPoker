# Server-Side AI Consolidation Design

**Status:** COMPLETED (2026-03-01)
**Goal:** Remove all game AI and poker business logic from clients. The server is the single source of truth for AI decisions, advisor calculations, and equity simulations. Clients become pure UI.

## Motivation

Game AI currently lives in three places:
- `pokergamecore` — core algorithms (Swing-free, shared)
- `pokerserver` — server-side AI integration
- `poker` (desktop client) — client-side AI wrappers for practice games

This creates maintenance burden, divergence risk, and puts business logic on the client. The web client also computes hand evaluation and equity client-side.

**After this change:** All poker logic lives server-side. Both clients (desktop Swing, web React) are pure UI that render server-provided state.

## Architecture

### 1. Desktop Practice Games: Embedded Server Engine

**Current:** Practice games call `Bet.doAI()` → `PokerAI.getHandAction()` synchronously per AI player.

**New:** Practice games embed `pokergameserver`'s `ServerTournamentDirector` in-process via `PracticeGameLauncher`. All players go through `ServerPlayerActionProvider`:
- AI players: `ServerAIProvider` computes actions using core algorithms
- Human player: embedded server calls back to Swing UI for input (same `CompletableFuture` pattern as online play)

**Changes:**
- `Bet.doAI()` becomes no-op or removed — AI actions arrive from embedded server
- `poker` module depends on `pokergameserver`
- Practice and online play share the same code path through `PokerDirector`

**Removed from `poker` module (client-side AI):**
- `PokerAI` (abstract base extending `EngineGameAI`)
- `V1Player`, `V2Player` (AI implementations)
- `RuleEngine` (desktop version of `PureRuleEngine` with Swing deps)
- `ClientV2AIContext`, `ClientStrategyProvider` (desktop adapters)
- `OpponentModel` (server has `ServerOpponentTracker`)
- `PlayerType` (server has `ServerStrategyProvider` + `StrategyDataLoader`)

### 2. ServerOpponentTracker Parity Fixes

Before removing client AI, `MutableOpponentStats` needs two fixes to match `OpponentModel` quality:

**Fix 1: Position tracking (4 → 6 categories)**
- Current: `blind=0, early=1, middle=2, late=3`
- New: `early=0, middle=1, late=2, small_blind=3, big_blind=4, overall=5`
- Blind play is fundamentally different — the AI needs this distinction

**Fix 2: Limp detection**
- Current: all pre-flop calls counted as limps
- New: only count calls when pot is unraised (limp = call BB with no prior raise)
- `recordAction()` needs to know whether there's been a raise in the current round

No interface changes needed — these are internal fixes to `MutableOpponentStats`.

### 3. Advisor Data via WebSocket (No Round-Trip)

**Current:** Web client computes advisor data client-side in `AdvisorPanel.tsx`.

**New:** Server computes advisor data and includes it in WebSocket game state messages. Sent with:
- `HOLE_CARDS` — preflop hand category + initial equity
- `COMMUNITY_CARDS_DEALT` — updated hand evaluation + equity
- `PLAYER_ACTED` — updated pot odds + recommendation

**Advisor payload:**
```json
{
  "advisor": {
    "handRank": 1,
    "handDescription": "One Pair, Aces",
    "equity": 72.5,
    "potOdds": 15.3,
    "recommendation": "Raise or Call",
    "startingHandCategory": "premium",
    "startingHandNotation": "AA"
  }
}
```

**Server-side:**
- New `AdvisorService` in `pokergameserver`:
  - `HandInfoFaster` for hand evaluation
  - Monte Carlo equity (2000 iterations)
  - Pot odds from game state
  - Starting hand categorization from `ServerStrategyProvider` hand strength tables
- Called by `GameEventBroadcaster` when building player-specific messages

**Removed from web client:**
- `lib/poker/handEvaluator.ts`
- `lib/poker/equityCalculator.ts`
- `lib/poker/startingHands.ts`
- `lib/poker/deck.ts`
- Computation logic in `AdvisorPanel.tsx` (becomes pure display)

**Desktop client:** Same advisor data arrives via embedded server events.

### 4. Simulator REST Endpoint

**Current:** `Simulator.tsx` runs Monte Carlo simulations client-side (10,000 iterations).

**New:** REST endpoint handles simulation. Simulator component becomes pure UI.

**Endpoint:** `POST /api/v1/poker/simulate`

**Request:**
```json
{
  "holeCards": ["Ah", "Kh"],
  "communityCards": ["2h", "3h", "4h"],
  "numOpponents": 3,
  "knownOpponentHands": [["7d", "2s"]],
  "iterations": 10000
}
```

**Response:**
```json
{
  "win": 85.2,
  "tie": 1.9,
  "loss": 12.9,
  "iterations": 10000,
  "opponentResults": [
    { "win": 75.3, "tie": 1.2, "loss": 23.5 },
    { "win": 68.1, "tie": 0.8, "loss": 31.1 },
    { "win": 12.5, "tie": 0.0, "loss": 87.5 }
  ]
}
```

**Server-side:**
- `PokerSimulationController` + `PokerSimulationService` in REST API module
- Monte Carlo logic ported from TypeScript (or reuse Java hand evaluation)
- Input validation: card format, no duplicates, opponent count 1-9, iteration cap 100,000
- Rate limiting

**Web client:** `Simulator.tsx` calls REST endpoint, shows loading state, no longer imports poker calculation libs.

## Files Affected

### Removed (Desktop Client AI)
- `poker/src/main/java/.../ai/PokerAI.java`
- `poker/src/main/java/.../ai/V1Player.java`
- `poker/src/main/java/.../ai/V2Player.java`
- `poker/src/main/java/.../ai/RuleEngine.java`
- `poker/src/main/java/.../ai/ClientV2AIContext.java`
- `poker/src/main/java/.../ai/ClientStrategyProvider.java`
- `poker/src/main/java/.../ai/OpponentModel.java`
- `poker/src/main/java/.../ai/PlayerType.java`

### Removed (Web Client Poker Logic)
- `web/lib/poker/handEvaluator.ts`
- `web/lib/poker/equityCalculator.ts`
- `web/lib/poker/startingHands.ts`
- `web/lib/poker/deck.ts`

### Modified (Desktop Game Loop)
- `poker/src/main/java/.../Bet.java` — remove `doAI()` local AI call path
- `poker/src/main/java/.../PokerPlayer.java` — remove `getPokerAI()` usage
- `poker/pom.xml` — add `pokergameserver` dependency

### Modified (Server)
- `ServerOpponentTracker.java` / `MutableOpponentStats` — position + limp fixes
- `GameEventBroadcaster.java` — include advisor data in player messages

### New (Server)
- `AdvisorService.java` — computes advisor data for WebSocket events
- `PokerSimulationController.java` — REST endpoint for simulator
- `PokerSimulationService.java` — Monte Carlo simulation logic

### Modified (Web Client)
- `AdvisorPanel.tsx` — pure display, reads advisor data from game state
- `Simulator.tsx` — calls REST endpoint instead of local calculation

## Decisions

- **Embedded server over local server process:** Avoids network overhead for practice games while sharing the same code path as online play.
- **Advisor via WebSocket, Simulator via REST:** Advisor needs real-time data (no round-trip), Simulator is user-initiated (REST is fine).
- **Fix ServerOpponentTracker before removal:** Ensures AI quality doesn't regress when switching from client to server opponent modeling.
- **Card format conversion:** Server accepts string format ("Ah", "Kd") in REST endpoints and converts internally to `Card` objects. WebSocket messages already use string card representations.
