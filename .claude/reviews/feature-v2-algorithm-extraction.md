# Review: feature-v2-algorithm-extraction

**Reviewer:** Claude Opus 4.6
**Date:** 2026-02-15
**Plan:** `.claude/plans/PHASE7C-V2-EXTRACTION.md`

---

## Parity Review: V2Player vs V2Algorithm

### Status: CANNOT REVIEW -- V2Algorithm Does Not Exist

**V2Algorithm.java has not been created yet.** The file at the expected path
`code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/ai/V2Algorithm.java`
does not exist. The PHASE7C plan shows all 8 phases with unchecked boxes -- no work
has been started on the V2 extraction.

### What Exists Today

1. **V2Player.java** (original, in `poker` module) -- 1,830 lines, partially modified:
   - Has `import com.donohoedigital.games.poker.core.ai.V2Algorithm;` (line 47)
   - Has `private V2Algorithm algorithm;` field (line 57)
   - Has `getActionViaAlgorithm()` method (lines 141-159) that would delegate to V2Algorithm
   - Has `PokerPlayerAdapter` inner class (lines 202-238) wrapping PokerPlayer to GamePlayerInfo
   - Has `buildActionOptions()` and `convertToLegacyAction()` bridge methods
   - **BUT**: The `init()` method at line 100 creates `new V2Algorithm(...)` which will fail
     at compile time since V2Algorithm does not exist

2. **V1Algorithm.java** (completed extraction, in `pokergamecore`) -- 2,025 lines, fully implemented
   - This is the V1 extraction that was completed in Phase 7B
   - Provides the pattern for V2 extraction (PurePokerAI interface, AIContext abstraction)

3. **PurePokerAI.java** (interface, in `pokergamecore`) -- defines `getAction()`, `wantsRebuy()`, `wantsAddon()`

4. **PHASE7C Plan** -- detailed 8-phase plan exists but all phases are unchecked

### Build Status Concern

V2Player.java currently imports and references V2Algorithm (line 47, 57, 100), which does
not exist. This means **the project cannot compile as-is**. Either:
- These changes are on a feature branch that was merged prematurely, or
- There is a compilation error that needs to be addressed

### Method-by-Method Pre-Assessment

Since V2Algorithm does not exist, I will document what V2Player currently contains that
would need to be extracted, to serve as a reference for when the extraction is implemented.

#### 1. computeOdds() -- V2Player lines 385-618

**Status:** NOT EXTRACTED (exists only in V2Player)

Key logic to preserve during extraction:
- Fingerprint-based caching (lines 407-409): `fpPocket_`, `fpCommunity_`
- 52x52 enumeration loop (lines 457-588) with `HandInfoFaster` scoring
- Positive/negative potential calculation with weighted `phs = Math.pow(ranks.getRawHandStrength(hand), 2)`
- Raw hand strength = win / count (line 591)
- Aggregated ppot/npot calculation (lines 593-603)
- Delegation to `computeBiasedPotential()` when community < 5 cards

#### 2. getBiasedHandStrength() -- V2Player lines 743-922

**Status:** NOT EXTRACTED (exists only in V2Player)

Key logic to preserve:
- Per-opponent weighting using `SimpleBias.simpleBias_[biasIndex][x][y] / 1000.0f`
- Card rank mapping: pairs -> (ACE-rank, ACE-rank), suited -> higher rank first, offsuit -> lower rank first
- `percentPaid = opponent.getOpponentModel().handsPaid.getPercentTrue(0.30f)`
- Weight calculation: paid opponents get SimpleBias weighting, unpaid get 1.0
- Per-seat BHS accumulation weighted by `rhs = ranks.getRawHandStrength(hand)`
- Final: average across opponents (not product) at line 905

#### 3. getBiasedEffectiveHandStrength() -- V2Player lines 653-670

**Status:** NOT EXTRACTED (exists only in V2Player)

Formula (line 664-667):
```
behs_ = Math.pow(
    Math.min(bhs - rhs * getBiasedNegativePotential()
        + Math.min((1 - bhs), getBiasedPositivePotential() * (potOdds + 1)), 1.0f),
    getNumWithCards() - 1)
```

Note: Uses `potOdds` parameter (not `scaledPotOdds`). The user's description mentioned
`scaledPotOdds` but the actual code uses `potOdds` directly.

#### 4. computeBiasedPotential() -- V2Player lines 684-741

**Status:** NOT EXTRACTED (exists only in V2Player)

Key logic:
- Uses `updateFieldMatrix()` for opponent range weighting
- 52x52 enumeration weighted by `fieldMatrix.get(i,j) * ranks.getRawHandStrength(hand)`
- Biased positive/negative potential = weighted sum / total weighted

#### 5. updateFieldMatrix() -- V2Player lines 953-1071

**Status:** NOT EXTRACTED (exists only in V2Player)

Key logic:
- Three opponent categories: couldLimp, paid, unpaid
- Limp path: `(simpleBias[limpIndex] - simpleBias[raiseIndex]) / 1000.0f`
- Paid path: `simpleBias[bhsIndex] / 1000.0f` with raise adjustment
- Unpaid path: weight = 1.0f
- `wasRaisedPreFlop` adjustments (raiseIndex-1 for limpers, bhsIndex-2 for callers)
- Takes max weight across all opponents per card combination

#### 6. State Management

**Status:** NOT EXTRACTED (exists only in V2Player)

State that would need extraction:
- `dSteam_` (tilt tracking) -- updated in `endHand()` via `computeBadBeatScore()`
- `potRaised_[10]` + `maPotRaised_` -- moving average updated in `dealtFlop()`
- `stealSuspicion_` -- updated in `playerActed()` based on position and action
- `fpPocket_`, `fpCommunity_` -- fingerprint caching for computeOdds()
- `biasedHandStrength_`, `behs_` -- cached computation results
- `lastPotStatus_` -- pot status tracking

### Overall Assessment

- **Behavioral parity score:** N/A -- V2Algorithm does not exist
- **Blocking issues:**
  1. V2Algorithm.java does not exist -- cannot perform comparison
  2. V2Player.java has forward references to non-existent V2Algorithm class (compile error)
  3. All 8 phases of PHASE7C plan are unchecked -- no extraction work has begun
- **Status:** CANNOT ASSESS -- extraction not yet implemented

### Recommendations

1. **Fix compilation:** Either revert the V2Algorithm references in V2Player (lines 47, 57, 100, 155)
   or begin Phase 1 of the extraction to provide the class.
2. **Follow the plan:** The PHASE7C plan is well-structured. Execute phases 1-6 in order.
3. **Re-request this review** after Phase 6 (V2Algorithm creation) is complete.
4. **Key parity risks** to watch during extraction:
   - The `getBiasedHandStrength()` method uses a simplified SimpleBias path (just `paid` vs `unpaid`)
     compared to `updateFieldMatrix()` which uses the full 3-way categorization
     (couldLimp/paid/unpaid). These are intentionally different -- BHS is per-opponent
     while field matrix uses max-across-opponents.
   - The BEHS formula has subtle operator precedence: `bhs - rhs*npot + min(...)` where
     `rhs` is raw hand strength (the multi-player exponent version from `getRawHandStrength()`).
   - `computeBadBeatScore()` depends on `HoldemSimulator` which may have Swing dependencies.
     The plan correctly suggests deferring steam to zero for server-side initially.
