# Poker Correctness Test Suite — Design

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Status:** approved
**Created:** 2026-02-25

---

## Goal

Build a comprehensive, fast, deterministic test suite that verifies DDPoker plays correct Texas Hold'em across all three betting structures (No Limit, Pot Limit, Limit) and mixed-format tournaments. Tests verify poker rules, not just that code doesn't crash.

## Architecture

Three API additions enable fully deterministic multi-player control:

1. **`/hand/result` endpoint** — exposes winner, hand ranking, pot breakdown after each hand
2. **Puppet mode** — intercepts AI decision point so tests control all players' actions
3. **`gameType` parameter** — allows starting games in any betting structure

Tests use one app launch per script with many hands per session for speed. Puppet mode eliminates non-determinism — every test controls all players' actions and injects specific cards.

---

## API Additions

### 1. `GET /hand/result`

Returns the most recently completed hand's result. Available after `resolve()` runs (round == SHOWDOWN and endDate set). Returns 409 if no hand result available.

```json
{
  "handNumber": 3,
  "communityCards": ["Qs", "Js", "Ts", "2d", "7h"],
  "isUncontested": false,
  "isAllInShowdown": false,
  "pots": [
    {
      "potNumber": 0,
      "chipCount": 600,
      "isOverbet": false,
      "players": ["Player 1", "AI-Aggressive"],
      "winners": [
        {
          "name": "Player 1",
          "chips": 600,
          "handType": "Flush",
          "handDescription": "Flush, Ace high",
          "bestHand": ["As", "Qs", "Js", "Ts", "8s"],
          "holeCards": ["As", "8s"],
          "score": 6000014
        }
      ],
      "losers": [
        {
          "name": "AI-Aggressive",
          "handType": "Pair",
          "handDescription": "Pair, Queens",
          "bestHand": ["Qs", "Qd", "Js", "Ts", "7h"],
          "holeCards": ["Qd", "3c"],
          "score": 2000012
        }
      ]
    }
  ],
  "playerResults": [
    {"name": "Player 1", "totalWin": 600, "totalOverbet": 0, "chipsBefore": 700, "chipsAfter": 1300},
    {"name": "AI-Aggressive", "totalWin": 0, "totalOverbet": 0, "chipsBefore": 300, "chipsAfter": 0}
  ]
}
```

**Data sources:** `HoldemHand`, `Pot.getWinners()`, `HandInfo.getHandTypeDesc()`, `HandInfoFast.toString()`, `HandInfo.getBest()`, `HandInfo.getScore()`. All existing classes — no new game logic.

**Lifecycle:** Result persists until next hand starts. Consumed by `GET` (not cleared).

### 2. Puppet Mode

**`POST /players/{seat}/puppet`** — enable/disable puppet mode for an AI seat.

Request: `{"enabled": true}` or `{"enabled": false}`

**`POST /players/{seat}/action`** — submit action for a puppeted player.

Request: `{"type": "CALL"}` or `{"type": "RAISE", "amount": 200}`

**Intercept point:** `ServerPlayerActionProvider.getAction()` in `code/pokergameserver`. When a puppeted AI player's turn comes, route through the existing `getHumanAction()` CompletableFuture mechanism instead of `aiProvider.getAction()`. Reuses proven timeout/validation logic.

**State changes:**
- `/state` adds `puppetedSeats: [1, 3]` listing puppeted seat numbers
- `currentAction` adds `currentPlayerSeat` and `currentPlayerName` for any player's turn (not just human)

### 3. Game Type in `/game/start`

Add `gameType` field:

```json
{
  "numPlayers": 6,
  "buyinChips": 1500,
  "gameType": "potlimit"
}
```

Values: `"nolimit"` (default), `"potlimit"`, `"limit"`.

Per-level override via `blindLevels[].gameType` for mixed format testing.

---

## Test Scripts

### New Scripts

#### `test-hand-rankings.sh`

One game, 3 players (human + 2 puppets). Each hand: inject cards, all check/call to showdown, verify `/hand/result`.

| # | Scenario | Expected winner | Expected hand type |
|---|----------|----------------|-------------------|
| 1 | High card vs high card (kicker) | Better kicker | High Card |
| 2 | Pair vs high card | Pair holder | Pair |
| 3 | Two pair vs pair | Two pair holder | Two Pair |
| 4 | Three of a kind vs two pair | Trips holder | Three of a Kind |
| 5 | Straight vs three of a kind | Straight holder | Straight |
| 6 | Flush vs straight | Flush holder | Flush |
| 7 | Full house vs flush | Full house holder | Full House |
| 8 | Four of a kind vs full house | Quads holder | Four of a Kind |
| 9 | Straight flush vs four of a kind | Straight flush holder | Straight Flush |
| 10 | Royal flush vs straight flush | Royal flush holder | Royal Flush |
| 11 | Split — identical straights | Both | Straight |
| 12 | Kicker — same pair, different kicker | Better kicker | Pair |
| 13 | Board plays — both use community | Both | (board hand) |
| 14 | Three-way — best among three | Best hand | (varies) |
| 15 | Flush completes — suited vs unsuited | Flush holder | Flush |

#### `test-pot-distribution.sh`

3 players, all puppet.

| # | Scenario | Verification |
|---|----------|-------------|
| 1 | Simple pot — one winner | Winner gets full pot |
| 2 | Equal split — two identical hands | Each gets exactly half |
| 3 | Three-way split | Each gets exactly 1/3 |
| 4 | Side pot — short all-in | Main pot to best of all; side pot to best of remaining |
| 5 | Multiple side pots | Correct distribution per pot tier |
| 6 | Overbet return | Excess returned to sole remaining player |
| 7 | Odd chip to button | Button-nearest player gets remainder chip |
| 8 | Uncontested — all fold | Winner gets pot, no showdown |

#### `test-limit-holdem.sh`

Start with `gameType: "limit"`, 3 players, all puppet.

| # | Scenario | Verification |
|---|----------|-------------|
| 1 | Pre-flop/flop bet = big blind | `maxBet` matches BB |
| 2 | Turn/river bet = 2x big blind | `maxBet` matches 2xBB |
| 3 | Raise cap (3 raises) | RAISE absent from `availableActions` |
| 4 | Raise cap ignored heads-up | RAISE present after 3 raises when heads-up |
| 5 | Min raise = previous raise | `minRaise` in state correct |
| 6 | All-in for less than limit | Action accepted |
| 7 | Call amount after capped raises | `callAmount` correct |
| 8 | `availableActions` excludes RAISE at cap | Actions list verified |
| 9 | Check-raise within limit | Raise accepted after check |
| 10 | Big blind option | CHECK available when no raises |

#### `test-potlimit-holdem.sh`

Start with `gameType: "potlimit"`, 3 players, all puppet.

| # | Scenario | Verification |
|---|----------|-------------|
| 1 | Max bet = pot size | `maxBet` matches pot |
| 2 | Max raise = pot + call | `maxRaise` correct |
| 3 | After bet, raiser max = pot + call + bet | Chained raise max correct |
| 4 | Cannot bet more than pot | Over-pot bet rejected or capped |
| 5 | All-in for less than pot | Action accepted |
| 6 | Pre-flop pot includes blinds | `maxBet` includes SB+BB |
| 7 | `minBet`/`maxBet` in state match rules | State fields verified |
| 8 | Pot-limit with side pots | Correct distribution |

#### `test-mixed-game.sh`

Start with per-level game types, hands-per-level advancement.

| # | Scenario | Verification |
|---|----------|-------------|
| 1 | Level 1 = limit | Limit rules enforced |
| 2 | Level 2 = pot-limit | Pot-limit rules enforced |
| 3 | Level 3 = no-limit | No-limit rules enforced |
| 4 | Blind escalation across types | Blinds correct per level |
| 5 | Level advance by hand count | Level increments at correct hand |
| 6 | State reflects current type | `gameType` field matches level config |

#### `test-tournament-lifecycle.sh`

4 players, all puppet.

| # | Scenario | Verification |
|---|----------|-------------|
| 1 | Blind posting order (3+ players) | SB left of dealer, BB left of SB via `currentBets` |
| 2 | Heads-up blind posting | Dealer=SB, other=BB |
| 3 | Button moves clockwise | `dealerSeat` increments each hand |
| 4 | Elimination at zero chips | Player eliminated after losing hand |
| 5 | Finish positions match elimination order | 1st eliminated = last place |
| 6 | Blind escalation timing | Blinds increase at correct hand count |
| 7 | Short stack partial blind | All-in blind post accepted |
| 8 | Dead button | Skip eliminated seat for button |

### Existing Script Improvements

| Script | Improvement |
|--------|------------|
| `test-split-pot.sh` | Puppet all players to call → verify `/hand/result` equal split amounts (not just conservation) |
| `test-all-actions.sh` | Puppet mode + injection → manufacture exact scenarios for each action in ~6 hands (not 50+ loops) |
| `test-gameover-ranks.sh` | Puppet losers all-in with bad cards → deterministic elimination in ~5 hands |
| `test-hand-history.sh` | Cross-check history against `/hand/result` winner/amounts |
| `test-rebuy-addon.sh` | Puppet player to lose on purpose → rebuy triggers exactly when expected |
| `test-neverbroke.sh` | Puppet human all-in with bad cards → guaranteed near-bust for never-broke trigger |

---

## lib.sh Additions

New helpers for puppet mode and result verification:

```bash
puppet_seat()        # POST /players/{seat}/puppet {"enabled":true}
unpuppet_seat()      # POST /players/{seat}/puppet {"enabled":false}
puppet_action()      # POST /players/{seat}/action {"type":"CALL"}
puppet_all()         # Enable puppet mode for all non-human AI seats
get_hand_result()    # GET /hand/result -> JSON
wait_any_turn()      # Wait for ANY player's turn (human or puppet), return state
play_to_showdown()   # All players check/call through all streets to showdown
assert_winner()      # Verify /hand/result winner name matches expected
assert_hand_type()   # Verify /hand/result hand type matches expected
assert_pot_amount()  # Verify specific pot winner received expected chips
assert_split()       # Verify equal split across N winners
assert_max_bet()     # Verify maxBet in state matches expected (for limit/pot-limit)
assert_available()   # Verify action is in availableActions
assert_unavailable() # Verify action is NOT in availableActions
```

**`play_to_showdown`** is the workhorse — loops through all betting rounds, submitting check/call for each player's turn until the hand completes. Most ranking and distribution tests use this pattern.

---

## Speed Budget

| Script | Hands | Launch | Play | Total |
|--------|-------|--------|------|-------|
| test-hand-rankings.sh | 15 | ~8s | ~5s | ~13s |
| test-pot-distribution.sh | 8 | ~8s | ~3s | ~11s |
| test-limit-holdem.sh | 10 | ~8s | ~4s | ~12s |
| test-potlimit-holdem.sh | 8 | ~8s | ~3s | ~11s |
| test-mixed-game.sh | 12 | ~8s | ~5s | ~13s |
| test-tournament-lifecycle.sh | 20 | ~8s | ~7s | ~15s |
| **Total new** | **73** | | | **~75s** |

Existing test improvements add no extra time — they replace slow non-deterministic loops with fast deterministic puppet sequences.

---

## Implementation Order

1. API additions (puppet mode, `/hand/result`, `gameType` param) — required by all tests
2. lib.sh helpers — shared foundation
3. test-hand-rankings.sh — most fundamental correctness
4. test-pot-distribution.sh — money correctness
5. test-limit-holdem.sh + test-potlimit-holdem.sh — betting structure correctness
6. test-mixed-game.sh — multi-type correctness
7. test-tournament-lifecycle.sh — tournament rules
8. Existing test improvements — leverage new infrastructure

---

## Out of Scope

- AI strategy verification (excluded per project direction)
- Multi-table tournament testing (only human's table visible)
- Network failure / reconnect simulation
- Animation timing / pixel verification
- Omaha or other non-Hold'em variants
