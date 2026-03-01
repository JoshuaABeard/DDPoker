# Test Cleanup & Improvement Design

**Date:** 2026-02-28
**Status:** DRAFT

## Context

All gameplay now runs through the server engine (`pokergameserver` module: `ServerHand`, `ServerGameTable`, `ServerTournamentDirector`). The legacy desktop engine classes (`HoldemHand`, `PokerTable`, `PokerPlayer`, `PokerGame`) survive only as view-model shells for the Swing UI (`RemoteHoldemHand`, `RemotePokerTable`). Zero poker logic executes through them at runtime.

The desktop AI stack (V2Player, PokerAI, RuleEngine) remains live until a separate "port AI to server" milestone. The `pokerserver` module's `ServerAIProvider` scaffold (unwired in production) is kept as prep for that milestone.

## Goals

1. Remove dead production code and dead tests (~2,700 lines)
2. Build comprehensive new tests against the live server engine
3. Optimize shell script execution speed

## Round 1: Dead Code & Dead Test Removal

### Production Code to Delete

| Item | Location | Reason |
|---|---|---|
| `PreFlopBias.java` | `poker/src/main/java/.../poker/PreFlopBias.java` | Zero production callers (only commented-out reference in `PocketWeights.java` line 544) |
| `PokerPlayer.processAction(HandAction)` method | `poker/src/main/java/.../poker/PokerPlayer.java` | Zero production callers — only called from `AllAITournamentSimulationTest` |
| Dead block in `HoldemHand.dealCards()` | `poker/src/main/java/.../poker/HoldemHand.java` | `sDealPlayableHands_` is hardcoded to `null` — the `if (sDealPlayableHands_ != null)` block (~20 lines) is statically unreachable |

### Tests to Delete

**Game-logic tests on dead code paths** (all extend `IntegrationTestBase`):

| File | Lines | Tests dead code path |
|---|---|---|
| `AllAITournamentSimulationTest` | 397 | Legacy HoldemHand game loop |
| `HoldemHandPotDistributionTest` | 378 | Legacy pot resolution |
| `HoldemHandPotCalculationTest` | 270 | Legacy pot math |
| `PokerPlayerBettingTest` | 249 | Legacy PokerPlayer betting |
| `PokerGameChipPoolTest` | 236 | Legacy chip pool |
| `PokerPlayerChipTest` | ~200 | Legacy chip operations |
| `PokerGameIntegrationTest` | 93 | Legacy `initTournament()` |
| `PokerTableIntegrationTest` | 150 | Legacy table init |

**Phase integration tests** (test the old game loop phase chain, not the WebSocket flow):

| File | Lines |
|---|---|
| `BasePhaseIntegrationTest` | 343 |
| `ChainPhaseIntegrationTest` | 143 |
| `PhaseContractsIntegrationTest` | 295 |
| `PreviousPhaseIntegrationTest` | 152 |

**Content-free stubs:**

| File | Notes |
|---|---|
| `V2AlgorithmIntegrationTest` (in `ai/`) | Three tests with `assertThat(true).isTrue()` — tests nothing |

### Tests to KEEP

- **All AI tests** — V2Player/PokerAI are live until the AI port milestone
- **`IntegrationTestBase` + `MockGameEngine` + `MockPokerMain`** — 5 AI integration tests depend on them
- **`PlayerProfileIntegrationTest` and `TournamentSetupIntegrationTest`** — test setup/config code still live for desktop
- **`V2AlgorithmParityTest`** (pokerserver) — tests server AI scaffold we're keeping
- **All `pokergameserver` tests** — test the live engine
- **All dev control server tests** — test the live HTTP API
- **All shell scenario scripts** — test the live E2E pipeline
- **`HeadlessGameRunnerTest`** (pokerserver) — tests `TournamentEngine` from pokergamecore

## Round 2: New Server Engine Tests

All new tests go in `pokergameserver/src/test/java/`.

### 2A: Port Valuable Scenarios from Deleted Tests

Most deleted scenarios already have server-side equivalents. The gaps:

| Scenario | New test location | What to add |
|---|---|---|
| Split pot with identical hands | `ServerHandTest` | Inject identical hole cards → verify even split |
| 4+ player pot distribution | `ServerHandTest` | Expand all-in section with 4+ player scenarios |
| Dead money in side pots | `ServerHandTest` | Player folds after contributing to pot, verify pot distribution |

~3-5 new test methods in `ServerHandTest`.

### 2B: Comprehensive Random Scenario Fuzzer

New test class: `ServerHandFuzzTest.java`

**Infrastructure:**
- `RandomGameGenerator` — generates random-but-legal setups (2-10 players, stacks 100-50000, varied blinds/antes)
- `RandomActionPlayer` — generates random-but-legal actions given current state
- Invariant assertions after every action and every hand

**Test methods (~1000+ iterations):**

| Test | Iterations | Generates |
|---|---|---|
| `fuzz_randomGames_chipConservation` | 200 | Random player count/stacks/blinds, 5 hands each |
| `fuzz_headsUp_allVariations` | 200 | 2-player, varied stack ratios (equal, 10:1, 100:1) |
| `fuzz_allSameStack_allIns` | 100 | Equal stacks, forced all-in scenarios |
| `fuzz_microStacks_vs_deepStacks` | 100 | Mix of 1-2 BB stacks and 100+ BB stacks |
| `fuzz_rapidBlindEscalation` | 100 | Blinds increase every hand |
| `fuzz_everyHandHeadsUp` | 100 | 2 players, play to bust |
| `fuzz_maxPlayers_fullTable` | 100 | 9-10 players, complex multi-way pots |
| `fuzz_antesWithBlinds` | 100 | Antes + blinds at every configuration |
| `fuzz_allCheckToShowdown` | 100 | Force check/call only — stress pot distribution |

**Invariants checked:**
- Chip conservation (total chips constant)
- No negative chip counts
- Winner(s) receive exactly the pot amount
- All-in players can't win more than they're eligible for
- Hand terminates (no infinite loops)
- State consistent after resolution (no leftover pot)

### 2C: Action Validation Tests

New test class: `ServerHandActionValidationTest.java`

Parameterized tests for illegal actions:

| Test | Expected |
|---|---|
| Bet exceeds stack | Clamps to all-in or rejects |
| Raise below minimum | Rejects or adjusts to minimum |
| Check when facing bet | Rejects — must call/raise/fold |
| Action from wrong player | Rejects |
| Action after folded | Rejects |
| Double action same round | Rejects |
| Fold when can check | Allowed (legal) |
| Negative bet amount | Rejects |
| Zero bet | Rejects |

### 2D: Heads-Up Edge Case Tests

New test methods in `ServerHandTest`:

| Test | Validates |
|---|---|
| Heads-up blind posting | Button/SB posts small blind, opponent posts big blind |
| Heads-up preflop action order | Button/SB acts first preflop |
| Heads-up postflop action order | BB acts first postflop |
| Heads-up partial blind | Short stack posts partial SB — verify pot math |
| Heads-up all-in preflop from SB | SB all-in, BB calls — verify resolution |

### 2E: Timeout/Disconnection Tests

New test methods in `ServerPlayerActionProviderTest` or `ServerTournamentDirectorTest`:

| Test | Validates |
|---|---|
| Player times out → auto-fold | Hand continues, pot awarded to remaining players |
| All-in player skipped | All-in player not asked for action |
| Timeout during heads-up | Other player wins the pot |

### 2F: Port Shell Scenarios to JUnit

New test class: `ServerHandScenarioTest.java` — deterministic scenarios mirroring highest-value shell scripts:

| Shell script | JUnit equivalent | Speed improvement |
|---|---|---|
| `test-chip-conservation.sh` | N hands with call-everything, assert chip conservation | <100ms vs ~30s |
| `test-all-actions.sh` | Exercise all action types in one hand | Same |
| `test-hand-flow.sh` | Card injection → verify community cards at each street | Same |
| `test-blind-posting.sh` | Verify blind/ante deductions at various player counts | Same |
| `test-allin-side-pot.sh` | Staggered stacks → multi-way all-in → side pots | Same |

These run without tags (included in `-P dev` fast feedback).

## Round 3: Shell Script Optimization

### 3A: Single-Launch Release Gate

Modify `run-release-gate.sh` to:
1. Build once at the top
2. Launch game JVM once
3. Run each script with `--skip-build --skip-launch`
4. Restart game between scripts via `POST /game/start` (not full JVM restart)
5. Only restart JVM if a script crashes it

**Estimated speedup:** ~50% (eliminate 15 redundant JVM startups).

### 3B: Review Shell/JUnit Overlap

Shell scripts stay in the release gate even with JUnit equivalents — they validate the full HTTP API + client-server pipeline (different purpose than unit-testing the engine). The JUnit tests provide fast inner-loop feedback; shell scripts provide E2E confidence.

### What NOT to change

- Don't refactor the 54 shell scripts themselves
- Don't change `lib.sh` internals beyond single-launch support
- Don't add new shell scripts — new scenarios go in JUnit

## Decisions

- Desktop AI code stays until "port AI to server" milestone (separate work)
- `ServerAIProvider` scaffold in `pokerserver` stays as prep for AI port
- `IntegrationTestBase` + mocks stay because AI integration tests depend on them
- Shell scripts stay for E2E validation; JUnit tests handle fast feedback
- New fuzz tests run without tags (always run, even in `-P dev`)
