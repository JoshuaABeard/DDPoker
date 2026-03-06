# Thin Client Test Coverage Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Raise Java thin client (`poker` module) test coverage by adding unit tests to key classes with testable logic that currently have 0% coverage.

**Architecture:** Tests target display models and utility classes in the poker module. All classes will undergo mechanical type-name changes when the pokergameprotocol plan executes (Card->ClientCard, BettingRound->ClientBettingRound, etc.), but the logic under test is stable. Tests use JUnit 5, Mockito, and AssertJ per project conventions.

**Tech Stack:** Java 25, JUnit 5, Mockito, AssertJ, Maven

**Note:** The pokergameprotocol migration plan (`docs/plans/2026-03-05-pokergameprotocol-implementation.md`) will later require mechanical import/type updates to these tests (Task 12 of that plan). The test _logic_ is durable.

---

## Task 1: ClientPlayer unit tests

ClientPlayer is the display-only player model (730 instructions, 0% coverage). No existing test file. Contains conditional logic for `isAllIn()`, `isInHand()`, `showFoldedHand()`, chip calculations, tournament spending, hand strength, and position names.

**Files:**
- Create: `code/poker/src/test/java/com/donohoedigital/games/poker/online/ClientPlayerTest.java`

**Step 1: Write the test file**

```java
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientPlayerTest {

    private ClientPlayer player;

    @BeforeEach
    void setUp() {
        player = new ClientPlayer(1, "Alice", true);
    }

    // ========== Constructor Tests ==========

    @Test
    void constructorSetsIdentity() {
        assertThat(player.getID()).isEqualTo(1);
        assertThat(player.getName()).isEqualTo("Alice");
        assertThat(player.isHuman()).isTrue();
    }

    @Test
    void constructorWithPlayerKey() {
        ClientPlayer p = new ClientPlayer("key-123", 2, "Bob", false);
        assertThat(p.getPlayerId()).isEqualTo("key-123");
        assertThat(p.isHuman()).isFalse();
        assertThat(p.isComputer()).isTrue();
    }

    @Test
    void defaultValuesAfterConstruction() {
        assertThat(player.getChipCount()).isEqualTo(0);
        assertThat(player.isFolded()).isFalse();
        assertThat(player.isSittingOut()).isFalse();
        assertThat(player.isDisconnected()).isFalse();
        assertThat(player.isBooted()).isFalse();
        assertThat(player.isWaiting()).isFalse();
        assertThat(player.getPlace()).isEqualTo(0);
        assertThat(player.getPrize()).isEqualTo(0);
    }

    // ========== isAllIn Tests ==========

    @Test
    void isAllIn_trueWhenNotFoldedZeroChipsAndHasCards() {
        player.setChipCount(0);
        player.setFolded(false);
        player.getHand().addCard(Card.getCard("Ah"));
        player.getHand().addCard(Card.getCard("Kd"));

        assertThat(player.isAllIn()).isTrue();
    }

    @Test
    void isAllIn_falseWhenHasChips() {
        player.setChipCount(500);
        player.getHand().addCard(Card.getCard("Ah"));

        assertThat(player.isAllIn()).isFalse();
    }

    @Test
    void isAllIn_falseWhenFolded() {
        player.setChipCount(0);
        player.setFolded(true);
        player.getHand().addCard(Card.getCard("Ah"));

        assertThat(player.isAllIn()).isFalse();
    }

    @Test
    void isAllIn_falseWhenNoCards() {
        player.setChipCount(0);
        player.setFolded(false);

        assertThat(player.isAllIn()).isFalse();
    }

    // ========== isInHand Tests ==========

    @Test
    void isInHand_trueWhenHasCardsAndNotFolded() {
        player.getHand().addCard(Card.getCard("Ah"));
        player.setFolded(false);

        assertThat(player.isInHand()).isTrue();
    }

    @Test
    void isInHand_falseWhenFolded() {
        player.getHand().addCard(Card.getCard("Ah"));
        player.setFolded(true);

        assertThat(player.isInHand()).isFalse();
    }

    @Test
    void isInHand_falseWhenNoCards() {
        player.setFolded(false);

        assertThat(player.isInHand()).isFalse();
    }

    // ========== showFoldedHand Tests ==========

    @Test
    void showFoldedHand_trueWhenFoldedAndExposed() {
        player.setFolded(true);
        player.setCardsExposed(true);

        assertThat(player.showFoldedHand()).isTrue();
    }

    @Test
    void showFoldedHand_falseWhenNotFolded() {
        player.setFolded(false);
        player.setCardsExposed(true);

        assertThat(player.showFoldedHand()).isFalse();
    }

    @Test
    void showFoldedHand_falseWhenNotExposed() {
        player.setFolded(true);
        player.setCardsExposed(false);

        assertThat(player.showFoldedHand()).isFalse();
    }

    // ========== Chip Calculation Tests ==========

    @Test
    void addChips_incrementsChipCount() {
        player.setChipCount(1000);
        player.addChips(500);

        assertThat(player.getChipCount()).isEqualTo(1500);
    }

    @Test
    void addRebuy_incrementsCountAndChips() {
        player.setChipCount(0);
        player.addRebuy(100, 1000, false);

        assertThat(player.getNumRebuys()).isEqualTo(1);
        assertThat(player.getRebuy()).isEqualTo(100);
        assertThat(player.getChipCount()).isEqualTo(1000);
    }

    @Test
    void addRebuy_accumulatesMultipleRebuys() {
        player.addRebuy(100, 1000, false);
        player.addRebuy(100, 1000, false);

        assertThat(player.getNumRebuys()).isEqualTo(2);
        assertThat(player.getRebuy()).isEqualTo(200);
        assertThat(player.getChipCount()).isEqualTo(2000);
    }

    @Test
    void addAddon_incrementsAddonAndChips() {
        player.setChipCount(500);
        player.addAddon(200, 2000);

        assertThat(player.getAddon()).isEqualTo(200);
        assertThat(player.getChipCount()).isEqualTo(2500);
    }

    @Test
    void getTotalSpent_sumsBuyinRebuyAddon() {
        player.setBuyin(500);
        player.setRebuy(200);
        player.setAddon(100);

        assertThat(player.getTotalSpent()).isEqualTo(800);
    }

    // ========== Hand Strength Tests ==========

    @Test
    void getEffectiveHandStrength_combinesStrengthAndPotential() {
        player.setHandStrength(0.5f);
        player.setHandPotential(0.3f);

        // EHS = HS + (1 - HS) * HP = 0.5 + 0.5 * 0.3 = 0.65
        assertThat(player.getEffectiveHandStrength()).isEqualTo(0.65f);
    }

    @Test
    void getEffectiveHandStrength_maxStrengthIgnoresPotential() {
        player.setHandStrength(1.0f);
        player.setHandPotential(0.5f);

        // EHS = 1.0 + 0.0 * 0.5 = 1.0
        assertThat(player.getEffectiveHandStrength()).isEqualTo(1.0f);
    }

    @Test
    void getEffectiveHandStrength_zeroStrengthReturnsPotential() {
        player.setHandStrength(0.0f);
        player.setHandPotential(0.4f);

        // EHS = 0.0 + 1.0 * 0.4 = 0.4
        assertThat(player.getEffectiveHandStrength()).isEqualTo(0.4f);
    }

    // ========== Position Name Tests ==========

    @Test
    void getPositionName_returnsCorrectNames() {
        assertThat(ClientPlayer.getPositionName(ClientPlayer.EARLY)).isEqualTo("early");
        assertThat(ClientPlayer.getPositionName(ClientPlayer.MIDDLE)).isEqualTo("middle");
        assertThat(ClientPlayer.getPositionName(ClientPlayer.LATE)).isEqualTo("late");
        assertThat(ClientPlayer.getPositionName(ClientPlayer.SMALL)).isEqualTo("small");
        assertThat(ClientPlayer.getPositionName(ClientPlayer.BIG)).isEqualTo("big");
    }

    @Test
    void getPositionName_returnsNoneForUnknown() {
        assertThat(ClientPlayer.getPositionName(-1)).isEqualTo("none");
        assertThat(ClientPlayer.getPositionName(99)).isEqualTo("none");
    }

    // ========== Card Management Tests ==========

    @Test
    void removeHand_clearsCardsAndSortedHand() {
        player.getHand().addCard(Card.getCard("Ah"));
        player.getHand().addCard(Card.getCard("Kd"));

        player.removeHand();

        assertThat(player.getHand().size()).isEqualTo(0);
    }

    @Test
    void newHand_replacesExistingHand() {
        player.getHand().addCard(Card.getCard("Ah"));

        Hand newHand = player.newHand(Hand.TYPE_NORMAL);

        assertThat(newHand.size()).isEqualTo(0);
        assertThat(player.getHand()).isSameAs(newHand);
    }

    @Test
    void getHandSorted_returnsSortedView() {
        player.getHand().addCard(Card.getCard("2c"));
        player.getHand().addCard(Card.getCard("Ah"));

        var sorted = player.getHandSorted();

        assertThat(sorted).isNotNull();
        // Sorted hand should have same size
        assertThat(sorted.size()).isEqualTo(2);
    }

    @Test
    void getHandSorted_cachedUntilHandChanges() {
        player.getHand().addCard(Card.getCard("Ah"));
        var sorted1 = player.getHandSorted();
        var sorted2 = player.getHandSorted();

        assertThat(sorted1).isSameAs(sorted2);
    }

    // ========== Profile Tests ==========

    @Test
    void setProfile_updatesName() {
        var profile = new com.donohoedigital.games.poker.PlayerProfile("TestProfile");
        player.setProfile(profile);

        assertThat(player.getName()).isEqualTo("TestProfile");
        assertThat(player.isProfileDefined()).isTrue();
    }

    @Test
    void setProfile_null_leavesNameUnchanged() {
        player.setProfile(null);

        assertThat(player.getName()).isEqualTo("Alice");
        assertThat(player.isProfileDefined()).isFalse();
    }

    // ========== Display Helpers ==========

    @Test
    void getDisplayName_offlineReturnsPlainName() {
        assertThat(player.getDisplayName(false)).isEqualTo("Alice");
    }

    @Test
    void isComputer_falseForHuman() {
        assertThat(player.isComputer()).isFalse();
    }

    @Test
    void isComputer_trueForNonHuman() {
        ClientPlayer ai = new ClientPlayer(2, "Bot", false);
        assertThat(ai.isComputer()).isTrue();
    }

    @Test
    void fold_setsFoldedTrue() {
        player.fold("test", 0);
        assertThat(player.isFolded()).isTrue();
    }

    @Test
    void getAction_returnsNull() {
        assertThat(player.getAction(false)).isNull();
    }

    // ========== AllIn Win Tracking ==========

    @Test
    void allInWin_trackingAccumulates() {
        assertThat(player.getAllInWin()).isEqualTo(0);
        player.addAllInWin();
        player.addAllInWin();
        assertThat(player.getAllInWin()).isEqualTo(2);
        player.clearAllInWin();
        assertThat(player.getAllInWin()).isEqualTo(0);
    }

    // ========== toString Tests ==========

    @Test
    void toString_returnsName() {
        assertThat(player.toString()).isEqualTo("Alice");
    }

    @Test
    void toString_unnamedFallback() {
        ClientPlayer unnamed = new ClientPlayer(5, null, true);
        assertThat(unnamed.toString()).isEqualTo("[unnamed-5]");
    }

    @Test
    void toStringShort_includesSeatAndChips() {
        player.setSeat(3);
        player.setChipCount(1500);
        assertThat(player.toStringShort()).isEqualTo("Alice (seat=3 chips=1500)");
    }

    // ========== Table Linkage ==========

    @Test
    void setTable_linksBothTableAndSeat() {
        ClientPokerTable mockTable = org.mockito.Mockito.mock(ClientPokerTable.class);
        player.setTable(mockTable, 5);

        assertThat(player.getTable()).isSameAs(mockTable);
        assertThat(player.getSeat()).isEqualTo(5);
    }
}
```

**Step 2: Run the test**

Run: `mvn test -pl poker -Dtest=ClientPlayerTest -P dev`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/ClientPlayerTest.java
git commit -m "test: add ClientPlayer unit tests (identity, allIn, chipCalc, handStrength)"
```

---

## Task 2: RemoteHoldemHand additional coverage

The existing `RemoteHoldemHandTest` covers initial state, round updates, community cards, and player order — but misses bet/call calculations, pot odds, win tracking, blind/ante accessors, pots, and action options. Coverage is currently 0% per JaCoCo despite the existing test file (the methods tested are simple delegates; the untested methods contain the actual logic).

**Files:**
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/online/RemoteHoldemHandTest.java`

**Step 1: Add tests for bet/call, pot odds, wins, blinds, and pots**

Append the following tests to the existing `RemoteHoldemHandTest` class:

```java
    // ========== Bet tracking ==========

    @Test
    void updatePlayerBet_tracksPerPlayerBets() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);

        hand.updatePlayerBet(alice.getID(), 100);
        assertThat(hand.getBet(alice)).isEqualTo(100);

        hand.updatePlayerBet(alice.getID(), 200);
        assertThat(hand.getBet(alice)).isEqualTo(200);
    }

    @Test
    void updatePlayerBet_zeroRemovesEntry() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.updatePlayerBet(alice.getID(), 100);
        hand.updatePlayerBet(alice.getID(), 0);

        assertThat(hand.getBet(alice)).isEqualTo(0);
    }

    @Test
    void getBet_returnsHighestBetAcrossPlayers() {
        hand.updatePlayerBet(1, 100);
        hand.updatePlayerBet(2, 300);
        hand.updatePlayerBet(3, 200);

        assertThat(hand.getBet()).isEqualTo(300);
    }

    @Test
    void getBet_returnsZeroWhenNoBets() {
        assertThat(hand.getBet()).isEqualTo(0);
    }

    @Test
    void clearBets_removesAllBets() {
        hand.updatePlayerBet(1, 100);
        hand.updatePlayerBet(2, 200);
        hand.clearBets();

        assertThat(hand.getBet()).isEqualTo(0);
    }

    // ========== Call calculation ==========

    @Test
    void getCall_fallbackComputesFromBets() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.updatePlayerBet(alice.getID(), 50);
        hand.updatePlayerBet(2, 200); // someone else bet 200

        // Call = highBet - playerBet = 200 - 50 = 150
        assertThat(hand.getCall(alice)).isEqualTo(150);
    }

    @Test
    void getCall_fallbackNeverNegative() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.updatePlayerBet(alice.getID(), 300);
        hand.updatePlayerBet(2, 100);

        // Alice has higher bet — call should be 0
        assertThat(hand.getCall(alice)).isEqualTo(0);
    }

    @Test
    void getCall_usesActionOptionsWhenAvailable() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.updateActionOptions(new ActionOptionsData(50, 100, 200, 500, 1000));

        // Should use callAmount from options (50), not compute from bets
        assertThat(hand.getCall(alice)).isEqualTo(50);
    }

    // ========== Min/max bet/raise from action options ==========

    @Test
    void getMinBet_returnsZeroWithoutOptions() {
        assertThat(hand.getMinBet()).isEqualTo(0);
    }

    @Test
    void getMinBet_returnsOptionsValue() {
        hand.updateActionOptions(new ActionOptionsData(50, 100, 200, 500, 1000));
        assertThat(hand.getMinBet()).isEqualTo(100);
    }

    @Test
    void getMinRaise_returnsOptionsValue() {
        hand.updateActionOptions(new ActionOptionsData(50, 100, 200, 500, 1000));
        assertThat(hand.getMinRaise()).isEqualTo(200);
    }

    @Test
    void getMaxBet_returnsOptionsValue() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.updateActionOptions(new ActionOptionsData(50, 100, 200, 500, 1000));
        assertThat(hand.getMaxBet(alice)).isEqualTo(500);
    }

    @Test
    void getMaxRaise_returnsOptionsValue() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.updateActionOptions(new ActionOptionsData(50, 100, 200, 500, 1000));
        assertThat(hand.getMaxRaise(alice)).isEqualTo(1000);
    }

    // ========== Pot odds ==========

    @Test
    void getPotOdds_calculatesCorrectly() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.updatePlayerBet(2, 100);
        hand.updatePot(400);
        // No action options — call = 100 - 0 = 100
        // potOdds = 100 * (100 / (100 + 400)) = 20.0
        assertThat(hand.getPotOdds(alice)).isCloseTo(20.0f, org.assertj.core.data.Offset.offset(0.01f));
    }

    @Test
    void getPotOdds_returnsZeroWhenCallIsZero() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.updatePot(500);
        // No bets, call = 0
        assertThat(hand.getPotOdds(alice)).isEqualTo(0.0f);
    }

    // ========== Win tracking ==========

    @Test
    void wins_accumulatesForSplitPots() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.wins(alice, 300, 0);
        hand.wins(alice, 200, 1);

        assertThat(hand.getWin(alice)).isEqualTo(500);
    }

    @Test
    void getWin_returnsZeroForPlayerWithNoWins() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.getWin(alice)).isEqualTo(0);
    }

    @Test
    void clearWins_resetsAllWins() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        hand.wins(alice, 500, 0);
        hand.clearWins();
        assertThat(hand.getWin(alice)).isEqualTo(0);
    }

    // ========== Blind / ante accessors ==========

    @Test
    void blindAndAnteAccessors() {
        hand.setSmallBlind(25);
        hand.setBigBlind(50);
        hand.setAnte(5);

        assertThat(hand.getSmallBlind()).isEqualTo(25);
        assertThat(hand.getBigBlind()).isEqualTo(50);
        assertThat(hand.getAnte()).isEqualTo(5);
    }

    @Test
    void blindSeatAccessors() {
        hand.updateSmallBlindSeat(3);
        hand.updateBigBlindSeat(4);

        assertThat(hand.getRemoteSmallBlindSeat()).isEqualTo(3);
        assertThat(hand.getRemoteBigBlindSeat()).isEqualTo(4);
    }

    // ========== Pots ==========

    @Test
    void getNumPots_initiallyZero() {
        assertThat(hand.getNumPots()).isEqualTo(0);
    }

    @Test
    void getPot_boundsCheck() {
        assertThat(hand.getPot(-1)).isNull();
        assertThat(hand.getPot(0)).isNull();
    }

    // ========== State flags ==========

    @Test
    void isAllInShowdown_alwaysFalse() {
        assertThat(hand.isAllInShowdown()).isFalse();
    }

    @Test
    void isStoredInDatabase_alwaysFalse() {
        assertThat(hand.isStoredInDatabase()).isFalse();
    }

    @Test
    void isFolded_delegatesToPlayer() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.isFolded(alice)).isFalse();
        alice.setFolded(true);
        assertThat(hand.isFolded(alice)).isTrue();
    }

    @Test
    void getLastAction_returnsActionNone() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.getLastAction(alice)).isEqualTo(HandAction.ACTION_NONE);
    }

    @Test
    void getLastActionThisRound_returnsActionNone() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.getLastActionThisRound(alice)).isEqualTo(HandAction.ACTION_NONE);
    }

    @Test
    void isActionInRound_alwaysFalse() {
        assertThat(hand.isActionInRound(0)).isFalse();
        assertThat(hand.isActionInRound(3)).isFalse();
    }

    @Test
    void getFoldRound_returnsNegativeOne() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.getFoldRound(alice)).isEqualTo(-1);
    }

    @Test
    void getOverbet_returnsZero() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.getOverbet(alice)).isEqualTo(0);
    }

    @Test
    void getTotalBet_returnsZero() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.getTotalBet(alice)).isEqualTo(0);
    }

    @Test
    void getNumPriorRaises_returnsZero() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.getNumPriorRaises(alice)).isEqualTo(0);
    }

    @Test
    void getStartDate_returnsZero() {
        assertThat(hand.getStartDate()).isEqualTo(0);
    }

    @Test
    void getEndDate_returnsZero() {
        assertThat(hand.getEndDate()).isEqualTo(0);
    }

    @Test
    void getLastAction_noHistory_returnsNull() {
        assertThat(hand.getLastAction()).isNull();
    }

    // ========== getNumWithCards ==========

    @Test
    void getNumWithCards_countsNonFolded() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        ClientPlayer bob = new ClientPlayer(2, "Bob", false);
        ClientPlayer carol = new ClientPlayer(3, "Carol", false);
        bob.setFolded(true);

        hand.updatePlayerOrder(List.of(alice, bob, carol));

        assertThat(hand.getNumWithCards()).isEqualTo(2);
    }

    // ========== MinChip delegation ==========

    @Test
    void getMinChip_delegatesToOwnerTable() {
        ClientPokerTable mockTable = org.mockito.Mockito.mock(ClientPokerTable.class);
        org.mockito.Mockito.when(mockTable.getMinChip()).thenReturn(25);
        hand.setOwnerTable(mockTable);

        assertThat(hand.getMinChip()).isEqualTo(25);
    }

    @Test
    void getMinChip_returnsOneWithNoTable() {
        assertThat(hand.getMinChip()).isEqualTo(1);
    }

    // ========== Deck/Muck unsupported ==========

    @Test
    void getDeck_throwsUnsupported() {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> hand.getDeck());
    }

    @Test
    void getMuck_throwsUnsupported() {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> hand.getMuck());
    }

    // ========== getRoundForDisplay ==========

    @Test
    void getRoundForDisplay_returnsLegacyInt() {
        hand.updateRound(BettingRound.FLOP);
        assertThat(hand.getRoundForDisplay()).isEqualTo(BettingRound.FLOP.toLegacy());
    }

    // ========== getCommunityForDisplay ==========

    @Test
    void getCommunityForDisplay_sameAsCommunity() {
        Hand community = new Hand();
        community.addCard(Card.getCard("Ah"));
        hand.updateCommunity(community);

        assertThat(hand.getCommunityForDisplay()).isSameAs(hand.getCommunity());
    }

    // ========== getCommunitySorted caching ==========

    @Test
    void getCommunitySorted_cachedUntilCommunityChanges() {
        Hand community = new Hand();
        community.addCard(Card.getCard("Ah"));
        hand.updateCommunity(community);

        var sorted1 = hand.getCommunitySorted();
        var sorted2 = hand.getCommunitySorted();
        assertThat(sorted1).isSameAs(sorted2);
    }
```

**Step 2: Run the tests**

Run: `mvn test -pl poker -Dtest=RemoteHoldemHandTest -P dev`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/RemoteHoldemHandTest.java
git commit -m "test: expand RemoteHoldemHand coverage (bets, potOdds, wins, blinds, state flags)"
```

---

## Task 3: RemotePokerTable additional coverage

The existing `RemotePokerTableTest` covers basic state updates and listeners but misses seat offset calculations (`getDisplaySeat`/`getTableSeat`), `addPlayer`, `removePlayer`, `isAllComputer`, `getNumOpenSeats`, `setRemoved`, hand number, min chip, level, and several constant-return methods.

**Files:**
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/online/RemotePokerTableTest.java`

**Step 1: Add tests for seat math, player management, and state methods**

Append the following tests to the existing `RemotePokerTableTest` class:

```java
    // ========== Seat offset / display seat ==========

    @Test
    void getDisplaySeat_noHumanPlayerReturnsIdentity() {
        // No human locally controlled player -> offset = 0 -> identity mapping
        ClientPlayer bot = new ClientPlayer(1, "Bot", false);
        table.setRemotePlayer(2, bot);

        assertThat(table.getDisplaySeat(0)).isEqualTo(0);
        assertThat(table.getDisplaySeat(5)).isEqualTo(5);
        assertThat(table.getDisplaySeat(9)).isEqualTo(9);
    }

    @Test
    void getTableSeat_reverseOfGetDisplaySeat() {
        // With no offset, table seat == display seat
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            int display = table.getDisplaySeat(i);
            assertThat(table.getTableSeat(display)).isEqualTo(i);
        }
    }

    // ========== addPlayer ==========

    @Test
    void addPlayer_setsFirstAvailableSeat() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        table.addPlayer(alice);

        assertThat(table.getPlayer(0)).isSameAs(alice);
        assertThat(alice.getSeat()).isEqualTo(0);
        assertThat(alice.getTable()).isSameAs(table);
    }

    @Test
    void addPlayer_skipsOccupiedSeats() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        ClientPlayer bob = new ClientPlayer(2, "Bob", false);
        table.setRemotePlayer(0, alice);
        table.addPlayer(bob);

        assertThat(table.getPlayer(1)).isSameAs(bob);
    }

    // ========== removePlayer ==========

    @Test
    void removePlayer_clearsSeat() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        table.setRemotePlayer(3, alice);
        table.removePlayer(3);

        assertThat(table.getPlayer(3)).isNull();
        assertThat(table.getNumOccupiedSeats()).isEqualTo(0);
    }

    @Test
    void removePlayer_outOfBoundsNoOp() {
        table.removePlayer(-1); // should not throw
        table.removePlayer(PokerConstants.SEATS); // should not throw
    }

    // ========== getNumOpenSeats ==========

    @Test
    void getNumOpenSeats_allEmpty() {
        assertThat(table.getNumOpenSeats()).isEqualTo(PokerConstants.SEATS);
    }

    @Test
    void getNumOpenSeats_subtractsOccupied() {
        table.setRemotePlayer(0, new ClientPlayer(1, "A", true));
        table.setRemotePlayer(5, new ClientPlayer(2, "B", false));

        assertThat(table.getNumOpenSeats()).isEqualTo(PokerConstants.SEATS - 2);
    }

    // ========== isAllComputer ==========

    @Test
    void isAllComputer_trueWhenEmpty() {
        assertThat(table.isAllComputer()).isTrue();
    }

    @Test
    void isAllComputer_trueWhenOnlyBots() {
        table.setRemotePlayer(0, new ClientPlayer(1, "Bot1", false));
        table.setRemotePlayer(1, new ClientPlayer(2, "Bot2", false));

        assertThat(table.isAllComputer()).isTrue();
    }

    @Test
    void isAllComputer_falseWhenHumanPresent() {
        table.setRemotePlayer(0, new ClientPlayer(1, "Bot1", false));
        table.setRemotePlayer(1, new ClientPlayer(2, "Human", true));

        assertThat(table.isAllComputer()).isFalse();
    }

    // ========== Hand number / min chip ==========

    @Test
    void handNum_defaultsToZero() {
        assertThat(table.getHandNum()).isEqualTo(0);
    }

    @Test
    void setHandNum_updatesHandNum() {
        table.setHandNum(42);
        assertThat(table.getHandNum()).isEqualTo(42);
    }

    @Test
    void minChip_defaultsToZero() {
        assertThat(table.getMinChip()).isEqualTo(0);
    }

    @Test
    void setMinChip_updatesMinChip() {
        table.setMinChip(25);
        assertThat(table.getMinChip()).isEqualTo(25);
    }

    // ========== Constant-return methods ==========

    @Test
    void isCurrent_alwaysTrue() {
        assertThat(table.isCurrent()).isTrue();
    }

    @Test
    void isZipMode_alwaysFalse() {
        assertThat(table.isZipMode()).isFalse();
    }

    @Test
    void isRemoteTable_alwaysTrue() {
        assertThat(table.isRemoteTable()).isTrue();
    }

    @Test
    void getNumObservers_alwaysZero() {
        assertThat(table.getNumObservers()).isEqualTo(0);
    }

    @Test
    void getObserver_alwaysNull() {
        assertThat(table.getObserver(0)).isNull();
    }

    @Test
    void isRebuyAllowed_alwaysFalse() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(table.isRebuyAllowed(alice)).isFalse();
        assertThat(table.isRebuyAllowed(alice, 1)).isFalse();
    }

    @Test
    void isRebuyDone_alwaysTrue() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(table.isRebuyDone(alice)).isTrue();
    }

    @Test
    void isAddonAllowed_alwaysFalse() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(table.isAddonAllowed(alice)).isFalse();
    }

    @Test
    void getRebuyList_alwaysEmpty() {
        assertThat(table.getRebuyList()).isEmpty();
    }

    @Test
    void getAddonList_alwaysEmpty() {
        assertThat(table.getAddonList()).isEmpty();
    }

    // ========== Listener management ==========

    @Test
    void removeListener_stopsNotifications() {
        List<PokerTableEvent> received = new ArrayList<>();
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_NEW_HAND);
        table.removePokerTableListener(received::add, PokerTableEvent.TYPE_NEW_HAND);

        table.fireEvent(PokerTableEvent.TYPE_NEW_HAND);

        assertThat(received).isEmpty();
    }

    @Test
    void addListener_mergesTypeFlags() {
        List<PokerTableEvent> received = new ArrayList<>();
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_NEW_HAND);
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_PLAYER_ACTION);

        table.fireEvent(PokerTableEvent.TYPE_NEW_HAND);
        table.fireEvent(PokerTableEvent.TYPE_PLAYER_ACTION);

        assertThat(received).hasSize(2);
    }

    @Test
    void listener_filteredByType() {
        List<PokerTableEvent> received = new ArrayList<>();
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_NEW_HAND);

        table.fireEvent(PokerTableEvent.TYPE_PLAYER_ACTION);

        assertThat(received).isEmpty();
    }

    // ========== setRemoved fires event ==========

    @Test
    void setRemoved_firesTableRemovedEvent() {
        List<PokerTableEvent> received = new ArrayList<>();
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_TABLE_REMOVED);

        table.setRemoved(true);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).getType()).isEqualTo(PokerTableEvent.TYPE_TABLE_REMOVED);
    }

    @Test
    void setRemoved_false_doesNotFireEvent() {
        List<PokerTableEvent> received = new ArrayList<>();
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_TABLE_REMOVED);

        table.setRemoved(false);

        assertThat(received).isEmpty();
    }

    // ========== updateFromState links table to players ==========

    @Test
    void updateFromState_linksPlayerToTable() {
        ClientPlayer[] players = new ClientPlayer[PokerConstants.SEATS];
        players[3] = new ClientPlayer(1, "Alice", true);
        table.updateFromState(players, 3);

        assertThat(players[3].getTable()).isSameAs(table);
        assertThat(players[3].getSeat()).isEqualTo(3);
    }

    // ========== setRemoteHand links hand to table ==========

    @Test
    void setRemoteHand_linksHandToTable() {
        RemoteHoldemHand hand = new RemoteHoldemHand();
        table.setRemoteHand(hand);

        assertThat(hand.getClientTable()).isSameAs(table);
    }

    @Test
    void setRemoteHand_null_doesNotThrow() {
        table.setRemoteHand(null);
        assertThat(table.getHoldemHand()).isNull();
    }
```

**Step 2: Run the tests**

Run: `mvn test -pl poker -Dtest=RemotePokerTableTest -P dev`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/RemotePokerTableTest.java
git commit -m "test: expand RemotePokerTable coverage (seat math, player mgmt, listeners, state)"
```

---

## Task 4: Run full test suite and verify coverage

**Step 1: Run full poker module tests**

Run: `mvn test -pl poker -P dev`
Expected: All tests PASS

**Step 2: Generate coverage report**

Run: `mvn verify -pl poker -P coverage`
Expected: BUILD SUCCESS, coverage threshold met

**Step 3: Check coverage improvement**

Run: `cat code/poker/target/site/jacoco/jacoco.csv | awk -F',' '$3 ~ /ClientPlayer|RemoteHoldemHand|RemotePokerTable/ {total=$4+$5; covered=$5; print $3 ": " int(covered*100/total) "%"}'`
Expected: Non-zero coverage for all three classes

**Step 4: Update coverage threshold if significantly improved**

If the main package coverage has risen above 14%, consider bumping the JaCoCo threshold in `code/poker/pom.xml` from `0.13` to match (actual minus ~1% buffer). Only do this if the new threshold passes reliably.

**Step 5: Commit threshold update (if applicable)**

```bash
git commit -am "build: raise poker module coverage threshold to match new tests"
```

---

## Execution Summary

| Task | Description | New Tests | Est. Instructions Covered |
|------|-------------|-----------|--------------------------|
| 1 | ClientPlayer unit tests | ~35 tests (new file) | ~400 of 730 |
| 2 | RemoteHoldemHand expansion | ~40 tests (additions) | ~350 of 526 |
| 3 | RemotePokerTable expansion | ~30 tests (additions) | ~300 of 575 |
| 4 | Full verification + threshold | - | - |

**Total estimated coverage gain:** ~1,050 additional instructions covered. Main package coverage should rise from ~14% to ~16-17%.

**pokergameprotocol migration impact:** Task 12 of that plan will need to update imports in these test files (`Card` -> `ClientCard`, `Hand` -> `ClientHand`, `BettingRound` -> `ClientBettingRound`). The test logic stays identical.
