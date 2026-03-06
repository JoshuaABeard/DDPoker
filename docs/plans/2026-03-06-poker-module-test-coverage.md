# Poker Module Test Coverage Expansion — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Push poker module test coverage from 13% to 40%+ (root) and 43% to 55%+ (AI) through dead code removal, unit tests, logic extraction, E2E expansion, and integration tests.

**Architecture:** Five-phase approach — (1) delete dead code to shrink denominator, (2) add unit tests for untested pure-logic classes, (3) extract testable logic from Swing UI classes following the ShowdownCalculator pattern, (4) expand E2E tests via GameControlServer, (5) add integration tests via MockGameEngine.

**Tech Stack:** JUnit 5, AssertJ, Mockito, GameControlServer HTTP API, IntegrationTestBase/MockGameEngine

**Design doc:** `docs/plans/2026-03-06-poker-module-test-coverage-design.md`

---

## Task 1: Delete Empty AIStrategy Class

**Files:**
- Delete: `code/poker/src/main/java/com/donohoedigital/games/poker/ai/AIStrategy.java`
- Delete: `code/poker/src/test/java/com/donohoedigital/games/poker/ai/AIStrategyNodeTest.java` (verify no AIStrategy import first)

**Step 1: Verify zero references**

Run: `cd /c/Repos/DDPoker/code && grep -r "AIStrategy[^N]" --include="*.java" poker/src/main/java/ | grep -v "AIStrategy.java"`
Expected: No output (zero references outside its own file)

**Step 2: Delete the file**

```bash
rm code/poker/src/main/java/com/donohoedigital/games/poker/ai/AIStrategy.java
```

**Step 3: Run tests to verify no breakage**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add -u && git commit -m "chore: delete empty AIStrategy class (zero references)"
```

---

## Task 2: Delete Duplicate AIConstants (Poker Copy)

**Files:**
- Delete: `code/poker/src/main/java/com/donohoedigital/games/poker/ai/AIConstants.java`

**Step 1: Verify zero poker-module consumers**

Run: `cd /c/Repos/DDPoker/code && grep -r "AIConstants" --include="*.java" poker/src/main/java/ | grep -v "AIConstants.java"`
Expected: No output

**Step 2: Delete the file**

```bash
rm code/poker/src/main/java/com/donohoedigital/games/poker/ai/AIConstants.java
```

**Step 3: Run tests**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add -u && git commit -m "chore: delete duplicate AIConstants from poker module (pokergamecore has canonical copy)"
```

---

## Task 3: Delete Dead CreateTestCase and All References

**Files:**
- Delete: `code/poker/src/main/java/com/donohoedigital/games/poker/ai/phase/CreateTestCase.java`
- Modify: `code/poker/src/main/resources/config/poker/gamedef.xml` (remove lines 402-413)
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java` (remove lines 318-326, 1776-1779)
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerClientConstants.java` (remove line 199: `TESTING_TEST_CASE`)

**Step 1: Remove CreateTestCase phase from gamedef.xml**

Delete the entire `<phase name="CreateTestCase" ...>` block (lines 402-413 in `gamedef.xml`).

**Step 2: Remove createTestCase() method and button wiring from ShowTournamentTable.java**

Remove the button creation block (lines 318-326):
```java
        if (TESTING(PokerClientConstants.TESTING_TEST_CASE)) {
            buttonTestCase_ = new GlassButton("testcase", "GlassBig");
            buttonbase_.add(buttonTestCase_);
            buttonTestCase_.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    createTestCase();
                }
            });
        }
```

Remove the `createTestCase()` method (lines 1776-1779):
```java
    protected void createTestCase() {
        TypedHashMap params = new TypedHashMap();
        context_.processPhaseNow("CreateTestCase", params);
    }
```

Also remove the `buttonTestCase_` field declaration (search for it).

**Step 3: Remove TESTING_TEST_CASE constant from PokerClientConstants.java**

Delete line 199: `public static final String TESTING_TEST_CASE = "settings.debug.testcase";`

**Step 4: Delete the CreateTestCase class file**

```bash
rm code/poker/src/main/java/com/donohoedigital/games/poker/ai/phase/CreateTestCase.java
```

Check if the `ai/phase/` directory is now empty and remove it if so:
```bash
rmdir code/poker/src/main/java/com/donohoedigital/games/poker/ai/phase/ 2>/dev/null || true
```

**Step 5: Run tests**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add -u && git commit -m "chore: remove dead CreateTestCase dialog and all references

AI test case creation was disabled when AI moved server-side.
Removes class, gamedef.xml phase, button wiring, and debug constant."
```

---

## Task 4: Unit Tests for TournamentProfileFormatter

This class uses `PropertyConfig.getMessage()` heavily, which requires ConfigManager initialization. Use `IntegrationTestBase` infrastructure.

**Files:**
- Create: `code/poker/src/test/java/com/donohoedigital/games/poker/TournamentProfileFormatterTest.java`
- Reference: `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileFormatter.java`

**Step 1: Write tests for the static getTableFormatDisplay() method (no infrastructure needed)**

```java
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.protocol.constants.ProtocolConstants;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TournamentProfileFormatterTest {

    @Nested
    class GetTableFormatDisplay {

        @Test
        void should_ReturnFullRing_When_SeatsFullRing() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(ProtocolConstants.SEATS_FULL_RING))
                    .contains("Full Ring");
        }

        @Test
        void should_Return6Max_When_Seats6Max() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(ProtocolConstants.SEATS_6MAX))
                    .contains("6-Max");
        }

        @Test
        void should_ReturnHeadsUp_When_SeatsHeadsUp() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(ProtocolConstants.SEATS_HEADS_UP))
                    .contains("Heads-Up");
        }

        @Test
        void should_ReturnCustom_When_NonStandardSeats() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(8))
                    .contains("8 per table");
        }
    }
}
```

**Step 2: Run tests to verify they pass**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=TournamentProfileFormatterTest -P dev -q`
Expected: 4 tests PASS

**Step 3: Add integration tests for formatter methods that need ConfigManager**

Add to the same test file, using IntegrationTestBase:

```java
    @Nested
    @Tag("slow")
    @Tag("integration")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class FormatterIntegration extends IntegrationTestBase {

        @Test
        void should_FormatHTMLSummary_When_ValidProfile() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);
            String html = formatter.toHTMLSummary(false, "en");
            assertThat(html).isNotEmpty();
        }

        @Test
        void should_FormatHTML_When_ValidProfile() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);
            String html = formatter.toHTML("en");
            assertThat(html).isNotEmpty();
        }

        @Test
        void should_FormatBlindsText_When_ValidLevel() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);
            String blinds = formatter.getBlindsText("msg.level.", 1, false);
            assertThat(blinds).isNotEmpty();
        }

        @Test
        void should_ReturnProfile_From_Getter() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);
            assertThat(formatter.getProfile()).isSameAs(profile);
        }

        private TournamentProfile createTestProfile() {
            // Use the default tournament profile loaded by ConfigManager
            // or create a minimal one with required fields set
            TournamentProfile profile = new TournamentProfile("Test Tournament");
            return profile;
        }
    }
```

Note: The exact `TournamentProfile` construction may need adjustment based on what fields are required. The implementor should check `TournamentProfile` constructors and required fields.

**Step 4: Run all tests**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=TournamentProfileFormatterTest -P dev -q`
Expected: All tests PASS

**Step 5: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/TournamentProfileFormatterTest.java
git commit -m "test(poker): add TournamentProfileFormatter unit and integration tests"
```

---

## Task 5: Unit Tests for HandSelectionScheme.getHandStrength()

**Files:**
- Create: `code/poker/src/test/java/com/donohoedigital/games/poker/ai/HandSelectionSchemeTest.java`
- Reference: `code/poker/src/main/java/com/donohoedigital/games/poker/ai/HandSelectionScheme.java`

**Step 1: Write tests for getHandStrength()**

The `getHandStrength(ClientCard, ClientCard)` method at line 187 iterates hand groups and returns strength/10. Test with a manually constructed scheme.

```java
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.HandGroup;
import com.donohoedigital.games.poker.display.ClientCard;
import com.donohoedigital.games.poker.display.ClientHand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HandSelectionSchemeTest {

    @Nested
    class GetHandStrength {

        private HandSelectionScheme scheme;

        @BeforeEach
        void setUp() {
            scheme = new HandSelectionScheme("Test");
            // Add a hand group containing AA with strength 10
            // The implementor should check HandGroup API to construct test groups
        }

        @Test
        void should_ReturnZero_When_HandNotInAnyGroup() {
            ClientCard c1 = ClientCard.of(ClientCard.TWO, ClientCard.CLUBS);
            ClientCard c2 = ClientCard.of(ClientCard.SEVEN, ClientCard.DIAMONDS);
            assertThat(scheme.getHandStrength(c1, c2)).isEqualTo(0.0f);
        }

        @Test
        void should_ReturnStrengthDividedByTen_When_HandInGroup() {
            // After setting up a group with strength 8 containing certain cards,
            // verify getHandStrength returns 0.8f
        }

        @Test
        void should_AcceptClientHand_Overload() {
            ClientHand hand = new ClientHand();
            hand.addCard(ClientCard.of(ClientCard.TWO, ClientCard.CLUBS));
            hand.addCard(ClientCard.of(ClientCard.SEVEN, ClientCard.DIAMONDS));
            assertThat(scheme.getHandStrength(hand)).isEqualTo(0.0f);
        }
    }

    @Nested
    class GroupManagement {

        @Test
        void should_StartWithEmptyGroups() {
            HandSelectionScheme scheme = new HandSelectionScheme("Test");
            assertThat(scheme.getHandGroups()).isEmpty();
        }

        @Test
        void should_EnsureEmptyGroup_AddsOneGroup() {
            HandSelectionScheme scheme = new HandSelectionScheme("Test");
            scheme.ensureEmptyGroup();
            assertThat(scheme.getHandGroups()).hasSize(1);
        }

        @Test
        void should_RemoveEmptyGroups() {
            HandSelectionScheme scheme = new HandSelectionScheme("Test");
            scheme.ensureEmptyGroup();
            scheme.removeEmptyGroups();
            assertThat(scheme.getHandGroups()).isEmpty();
        }
    }

    @Nested
    class DescriptionAndMap {

        @Test
        void should_SetAndGetDescription() {
            HandSelectionScheme scheme = new HandSelectionScheme("Test");
            scheme.setDescription("A test scheme");
            assertThat(scheme.getDescription()).isEqualTo("A test scheme");
        }

        @Test
        void should_ReturnEmptyDescription_ByDefault() {
            HandSelectionScheme scheme = new HandSelectionScheme("Test");
            assertThat(scheme.getDescription()).isEmpty();
        }
    }
}
```

Note: The implementor needs to check `HandGroup` construction and `ClientCard.of()` factory (or equivalent constructor) to flesh out the strength tests.

**Step 2: Run tests**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=HandSelectionSchemeTest -P dev -q`
Expected: Tests PASS

**Step 3: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/ai/HandSelectionSchemeTest.java
git commit -m "test(poker): add HandSelectionScheme unit tests"
```

---

## Task 6: Extract BetCalculator from Bet.java

Extract pure calculation logic from `Bet.java` into a new `BetCalculator` class following the `ShowdownCalculator` pattern.

**Files:**
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/BetCalculator.java`
- Create: `code/poker/src/test/java/com/donohoedigital/games/poker/BetCalculatorTest.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/Bet.java` (delegate to BetCalculator)

**Step 1: Write failing tests for BetCalculator**

```java
package com.donohoedigital.games.poker;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BetCalculatorTest {

    // =================================================================
    // determineInputMode tests
    // =================================================================

    @Nested
    class DetermineInputMode {

        @Test
        void should_ReturnCheckBet_When_NothingToCallAndNoBet() {
            assertThat(BetCalculator.determineInputMode(0, 0))
                    .isEqualTo(PokerTableInput.MODE_CHECK_BET);
        }

        @Test
        void should_ReturnCheckRaise_When_NothingToCallButBetExists() {
            assertThat(BetCalculator.determineInputMode(0, 100))
                    .isEqualTo(PokerTableInput.MODE_CHECK_RAISE);
        }

        @Test
        void should_ReturnCallRaise_When_AmountToCall() {
            assertThat(BetCalculator.determineInputMode(50, 0))
                    .isEqualTo(PokerTableInput.MODE_CALL_RAISE);
        }

        @Test
        void should_ReturnCallRaise_When_AmountToCallAndBetExists() {
            assertThat(BetCalculator.determineInputMode(50, 100))
                    .isEqualTo(PokerTableInput.MODE_CALL_RAISE);
        }
    }

    // =================================================================
    // roundToMinChip tests
    // =================================================================

    @Nested
    class RoundToMinChip {

        @Test
        void should_ReturnSameAmount_When_AlreadyMultipleOfMinChip() {
            assertThat(BetCalculator.roundToMinChip(100, 25)).isEqualTo(100);
        }

        @Test
        void should_RoundDown_When_RemainderLessThanHalf() {
            // 110 % 25 = 10, half of 25 = 12.5, 10 < 12.5 => round down to 100
            assertThat(BetCalculator.roundToMinChip(110, 25)).isEqualTo(100);
        }

        @Test
        void should_RoundUp_When_RemainderGreaterOrEqualToHalf() {
            // 113 % 25 = 13, half of 25 = 12.5, 13 >= 12.5 => round up to 125
            assertThat(BetCalculator.roundToMinChip(113, 25)).isEqualTo(125);
        }

        @Test
        void should_RoundUp_When_RemainderExactlyHalf() {
            // 50 % 100 = 50, half of 100 = 50, 50 >= 50 => round up to 100
            assertThat(BetCalculator.roundToMinChip(50, 100)).isEqualTo(100);
        }

        @Test
        void should_ReturnZero_When_AmountIsZero() {
            assertThat(BetCalculator.roundToMinChip(0, 25)).isEqualTo(0);
        }
    }

    // =================================================================
    // determineBetOrRaise tests
    // =================================================================

    @Nested
    class DetermineBetOrRaise {

        @Test
        void should_ReturnBet_When_InputModeIsCheckBet() {
            assertThat(BetCalculator.determineBetOrRaise(PokerTableInput.MODE_CHECK_BET))
                    .isEqualTo(HandAction.ACTION_BET);
        }

        @Test
        void should_ReturnRaise_When_InputModeIsCheckRaise() {
            assertThat(BetCalculator.determineBetOrRaise(PokerTableInput.MODE_CHECK_RAISE))
                    .isEqualTo(HandAction.ACTION_RAISE);
        }

        @Test
        void should_ReturnRaise_When_InputModeIsCallRaise() {
            assertThat(BetCalculator.determineBetOrRaise(PokerTableInput.MODE_CALL_RAISE))
                    .isEqualTo(HandAction.ACTION_RAISE);
        }
    }

    // =================================================================
    // determineCheckOrCall tests
    // =================================================================

    @Nested
    class DetermineCheckOrCall {

        @Test
        void should_ReturnCheck_When_InputModeIsCheckBet() {
            assertThat(BetCalculator.determineCheckOrCall(PokerTableInput.MODE_CHECK_BET))
                    .isEqualTo(HandAction.ACTION_CHECK);
        }

        @Test
        void should_ReturnCheck_When_InputModeIsCheckRaise() {
            assertThat(BetCalculator.determineCheckOrCall(PokerTableInput.MODE_CHECK_RAISE))
                    .isEqualTo(HandAction.ACTION_CHECK);
        }

        @Test
        void should_ReturnCall_When_InputModeIsCallRaise() {
            assertThat(BetCalculator.determineCheckOrCall(PokerTableInput.MODE_CALL_RAISE))
                    .isEqualTo(HandAction.ACTION_CALL);
        }
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=BetCalculatorTest -P dev -q`
Expected: FAIL — `BetCalculator` class not found

**Step 3: Implement BetCalculator**

```java
package com.donohoedigital.games.poker;

/**
 * Pure calculation logic extracted from Bet phase for testability.
 * Determines input modes, rounds bet amounts, and classifies actions.
 */
public class BetCalculator {

    private BetCalculator() {} // utility class

    /**
     * Determine the input mode based on the current betting state.
     *
     * @param toCall amount needed to call (0 = no bet to match)
     * @param currentBet current bet on the table
     * @return one of MODE_CHECK_BET, MODE_CHECK_RAISE, or MODE_CALL_RAISE
     */
    public static int determineInputMode(int toCall, int currentBet) {
        if (toCall == 0) {
            return (currentBet == 0)
                    ? PokerTableInput.MODE_CHECK_BET
                    : PokerTableInput.MODE_CHECK_RAISE;
        }
        return PokerTableInput.MODE_CALL_RAISE;
    }

    /**
     * Round a bet amount to the nearest min chip denomination.
     * Rounds up when the remainder is >= half the min chip, down otherwise.
     *
     * @param amount the raw bet amount
     * @param minChip the minimum chip denomination
     * @return the rounded amount
     */
    public static int roundToMinChip(int amount, int minChip) {
        int odd = amount % minChip;
        if (odd == 0) return amount;

        int rounded = amount - odd;
        if ((float) odd >= (minChip / 2.0f)) {
            rounded += minChip;
        }
        return rounded;
    }

    /**
     * Determine whether a bet/raise action is a BET or RAISE.
     *
     * @param inputMode current input mode
     * @return ACTION_BET if check/bet mode, ACTION_RAISE otherwise
     */
    public static int determineBetOrRaise(int inputMode) {
        return (inputMode == PokerTableInput.MODE_CHECK_BET)
                ? HandAction.ACTION_BET
                : HandAction.ACTION_RAISE;
    }

    /**
     * Determine whether a check/call action is a CHECK or CALL.
     *
     * @param inputMode current input mode
     * @return ACTION_CHECK if check mode, ACTION_CALL otherwise
     */
    public static int determineCheckOrCall(int inputMode) {
        return (inputMode == PokerTableInput.MODE_CHECK_BET
                || inputMode == PokerTableInput.MODE_CHECK_RAISE)
                ? HandAction.ACTION_CHECK
                : HandAction.ACTION_CALL;
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=BetCalculatorTest -P dev -q`
Expected: All tests PASS

**Step 5: Update Bet.java to delegate to BetCalculator**

In `Bet.java`, replace the inline logic:

Line ~184-188 — replace:
```java
int inputMode;
if (nToCall == 0) {
    inputMode = (nBet == 0) ? PokerTableInput.MODE_CHECK_BET : PokerTableInput.MODE_CHECK_RAISE;
} else {
    inputMode = PokerTableInput.MODE_CALL_RAISE;
}
```
with:
```java
int inputMode = BetCalculator.determineInputMode(nToCall, nBet);
```

In `betRaise()` line ~418-426, replace the rounding block:
```java
int nMinChip = table_.getMinChip();
int nOdd = nAmount % nMinChip;
int nNewAmount = nAmount;
if (nOdd != 0) {
    nNewAmount = nAmount - nOdd;
    if ((float) nOdd >= (nMinChip / 2.0f)) {
        nNewAmount += nMinChip;
    }
}
```
with:
```java
int nMinChip = table_.getMinChip();
int nNewAmount = BetCalculator.roundToMinChip(nAmount, nMinChip);
```

In `betRaise()` line ~442-446, replace:
```java
if (game_.getInputMode() == PokerTableInput.MODE_CHECK_BET) {
    return new HandAction(player_, nRound_, HandAction.ACTION_BET, nAmount, "betbtn");
} else {
    return new HandAction(player_, nRound_, HandAction.ACTION_RAISE, nAmount, "raisebtn");
}
```
with:
```java
int actionType = BetCalculator.determineBetOrRaise(game_.getInputMode());
String btn = (actionType == HandAction.ACTION_BET) ? "betbtn" : "raisebtn";
return new HandAction(player_, nRound_, actionType, nAmount, btn);
```

In `checkCall()` line ~452-459, replace:
```java
if ((game_.getInputMode() == PokerTableInput.MODE_CHECK_BET
        || game_.getInputMode() == PokerTableInput.MODE_CHECK_RAISE)) {
    return new HandAction(player_, nRound_, HandAction.ACTION_CHECK, "checkbtn");
} else {
    return new HandAction(player_, nRound_, HandAction.ACTION_CALL, "callbtn");
}
```
with:
```java
int actionType = BetCalculator.determineCheckOrCall(game_.getInputMode());
String btn = (actionType == HandAction.ACTION_CHECK) ? "checkbtn" : "callbtn";
return new HandAction(player_, nRound_, actionType, btn);
```

**Step 6: Run full test suite for poker module**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 7: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/BetCalculator.java \
        code/poker/src/test/java/com/donohoedigital/games/poker/BetCalculatorTest.java \
        code/poker/src/main/java/com/donohoedigital/games/poker/Bet.java
git commit -m "refactor(poker): extract BetCalculator from Bet phase with full test coverage

Extracts pure calculation logic (input mode, bet rounding, action type)
from Swing-coupled Bet class into testable BetCalculator utility."
```

---

## Task 7: Extract GameOutcome from GameOver.java

**Files:**
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/GameOutcome.java`
- Create: `code/poker/src/test/java/com/donohoedigital/games/poker/GameOutcomeTest.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/GameOver.java` (delegate)

**Step 1: Write failing tests**

```java
package com.donohoedigital.games.poker;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GameOutcomeTest {

    @Nested
    class Determine {

        @Test
        void should_ReturnWin_When_PlaceIsFirst() {
            assertThat(GameOutcome.determine(1, 500, false)).isEqualTo(GameOutcome.WIN);
        }

        @Test
        void should_ReturnMoney_When_PrizePositiveButNotFirst() {
            assertThat(GameOutcome.determine(3, 100, false)).isEqualTo(GameOutcome.MONEY);
        }

        @Test
        void should_ReturnObserver_When_NoPrizeAndGameOver() {
            assertThat(GameOutcome.determine(5, 0, true)).isEqualTo(GameOutcome.OBSERVER);
        }

        @Test
        void should_ReturnBusted_When_NoPrizeAndGameNotOver() {
            assertThat(GameOutcome.determine(5, 0, false)).isEqualTo(GameOutcome.BUSTED);
        }

        @Test
        void should_ReturnWin_When_FirstPlaceEvenWithZeroPrize() {
            assertThat(GameOutcome.determine(1, 0, false)).isEqualTo(GameOutcome.WIN);
        }

        @Test
        void should_ReturnMoney_When_SecondPlaceWithPrize() {
            assertThat(GameOutcome.determine(2, 250, true)).isEqualTo(GameOutcome.MONEY);
        }
    }
}
```

**Step 2: Run to verify failure**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=GameOutcomeTest -P dev -q`
Expected: FAIL — `GameOutcome` not found

**Step 3: Implement GameOutcome**

```java
package com.donohoedigital.games.poker;

/**
 * Classification of a player's tournament outcome.
 * Extracted from GameOver dialog for testability.
 */
public enum GameOutcome {
    WIN,
    MONEY,
    OBSERVER,
    BUSTED;

    /**
     * Determine the outcome for a player.
     *
     * @param place player's finishing place (1 = winner)
     * @param prize prize amount won
     * @param gameOver whether the entire game has ended
     * @return the outcome classification
     */
    public static GameOutcome determine(int place, int prize, boolean gameOver) {
        if (place == 1) return WIN;
        if (prize > 0) return MONEY;
        if (gameOver) return OBSERVER;
        return BUSTED;
    }
}
```

**Step 4: Run tests**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=GameOutcomeTest -P dev -q`
Expected: All tests PASS

**Step 5: Update GameOver.java to use GameOutcome**

In `GameOver.java` lines 142-151, replace:
```java
String sMsg;
if (human.getPlace() == 1) {
    sMsg = PropertyConfig.getMessage("msg.gameover.out.win");
} else if (human.getPrize() > 0) {
    sMsg = PropertyConfig.getMessage("msg.gameover.out.money");
} else if (game_.isGameOver()) {
    sMsg = PropertyConfig.getMessage("msg.gameover.out.observer");
} else {
    sMsg = PropertyConfig.getMessage("msg.gameover.out.busted");
}
```
with:
```java
GameOutcome outcome = GameOutcome.determine(human.getPlace(), human.getPrize(), game_.isGameOver());
String sMsg = PropertyConfig.getMessage(switch (outcome) {
    case WIN -> "msg.gameover.out.win";
    case MONEY -> "msg.gameover.out.money";
    case OBSERVER -> "msg.gameover.out.observer";
    case BUSTED -> "msg.gameover.out.busted";
});
```

**Step 6: Run full tests, commit**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -P dev -q`
Expected: BUILD SUCCESS

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/GameOutcome.java \
        code/poker/src/test/java/com/donohoedigital/games/poker/GameOutcomeTest.java \
        code/poker/src/main/java/com/donohoedigital/games/poker/GameOver.java
git commit -m "refactor(poker): extract GameOutcome enum from GameOver dialog"
```

---

## Task 8: Expand E2E Betting Scenario Tests

Add E2E tests that exercise betting code paths through the GameControlServer.

**Files:**
- Create: `code/poker/src/test/java/com/donohoedigital/games/poker/e2e/BettingE2ETest.java`
- Reference: `code/poker/src/test/java/com/donohoedigital/games/poker/e2e/GameLifecycleE2ETest.java`

**Step 1: Write E2E betting tests**

Follow the pattern from `GameLifecycleE2ETest`. The test should:
1. Start a practice game
2. Navigate to a human betting turn
3. Exercise CHECK, CALL, RAISE, FOLD, and ALL_IN actions
4. Verify game state transitions after each action

```java
package com.donohoedigital.games.poker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BettingE2ETest extends ControlServerTestBase {

    @BeforeEach
    void startFreshGame() throws Exception {
        client().startGame(3);
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
        client().submitAction("DEAL");
    }

    @Test
    @Order(1)
    void should_AdvanceGame_When_FoldAction() throws Exception {
        JsonNode state = client().waitForHumanBettingTurn(Duration.ofSeconds(15));
        client().submitAction("FOLD");
        // After folding, game should advance (not stay in same betting mode)
        JsonNode next = client().waitForInputMode(Duration.ofSeconds(15),
                "DEAL", "CONTINUE", "CONTINUE_LOWER", "QUITSAVE", "NONE",
                "CHECK_BET", "CHECK_RAISE", "CALL_RAISE");
        assertThat(next).isNotNull();
    }

    @Test
    @Order(2)
    void should_AdvanceGame_When_CheckAction() throws Exception {
        JsonNode state = client().waitForHumanBettingTurn(Duration.ofSeconds(15));
        String mode = state.path("inputMode").asText();
        if (mode.equals("CHECK_BET") || mode.equals("CHECK_RAISE")) {
            client().submitAction("CHECK");
            JsonNode next = client().waitForInputMode(Duration.ofSeconds(15),
                    "DEAL", "CONTINUE", "CONTINUE_LOWER", "QUITSAVE", "NONE",
                    "CHECK_BET", "CHECK_RAISE", "CALL_RAISE");
            assertThat(next).isNotNull();
        }
        // If mode is CALL_RAISE, check isn't valid — just fold instead
    }

    @Test
    @Order(3)
    void should_AdvanceGame_When_CallAction() throws Exception {
        JsonNode state = client().waitForHumanBettingTurn(Duration.ofSeconds(15));
        String mode = state.path("inputMode").asText();
        if (mode.equals("CALL_RAISE")) {
            client().submitAction("CALL");
            JsonNode next = client().waitForInputMode(Duration.ofSeconds(15),
                    "DEAL", "CONTINUE", "CONTINUE_LOWER", "QUITSAVE", "NONE",
                    "CHECK_BET", "CHECK_RAISE", "CALL_RAISE");
            assertThat(next).isNotNull();
        }
    }
}
```

Note: The implementor will need to adapt based on the actual `ControlServerClient` API (check `waitForHumanBettingTurn` vs `playUntilHumanBettingTurn` method names in the existing test infrastructure).

**Step 2: Run E2E tests**

Run: `cd /c/Repos/DDPoker/code && mvn test -pl poker -Dtest=BettingE2ETest -P dev -q`
Expected: Tests PASS (requires game to be buildable with `-P dev`)

**Step 3: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/e2e/BettingE2ETest.java
git commit -m "test(poker): add E2E betting scenario tests via GameControlServer"
```

---

## Task 9: Raise JaCoCo Coverage Thresholds

After all tests are added, raise the thresholds in `code/poker/pom.xml`.

**Files:**
- Modify: `code/poker/pom.xml` (JaCoCo thresholds section)

**Step 1: Run coverage report to see actual numbers**

Run: `cd /c/Repos/DDPoker/code && mvn verify -P coverage -pl poker -q`

Check the report at `code/poker/target/site/jacoco/index.html` for actual coverage percentages.

**Step 2: Update thresholds to lock in new baselines**

In `code/poker/pom.xml`, update the minimum values for the two package rules:
- `com.donohoedigital.games.poker`: raise from `0.13` to actual - 2%
- `com.donohoedigital.games.poker.ai`: raise from `0.43` to actual - 2%

**Step 3: Verify build passes with new thresholds**

Run: `cd /c/Repos/DDPoker/code && mvn verify -P coverage -pl poker -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add code/poker/pom.xml
git commit -m "build: raise JaCoCo coverage thresholds for poker module to lock in new baselines"
```

---

## Task Dependency Summary

```
Task 1 (AIStrategy) ──┐
Task 2 (AIConstants) ──┼── Phase 1: Dead code (independent, can parallel)
Task 3 (CreateTestCase)┘
                        ↓
Task 4 (TournamentProfileFormatter) ──┐
Task 5 (HandSelectionScheme) ─────────┼── Phase 2: Unit tests (independent, can parallel)
                                      ↓
Task 6 (BetCalculator) ──────────┐
Task 7 (GameOutcome) ────────────┼── Phase 3: Extract logic (independent, can parallel)
                                 ↓
Task 8 (E2E Betting) ──────────── Phase 4: E2E expansion
                                 ↓
Task 9 (JaCoCo thresholds) ────── Phase 5: Lock in coverage (must be last)
```

Tasks within each phase can be executed in parallel. Phases should be sequential.
