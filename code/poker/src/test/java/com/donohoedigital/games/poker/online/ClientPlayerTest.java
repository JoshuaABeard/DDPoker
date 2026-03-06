/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandSorted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClientPlayer}.
 */
class ClientPlayerTest {

    private ClientPlayer player;

    @BeforeEach
    void setUp() {
        player = new ClientPlayer(1, "Alice", true);
    }

    // =================================================================
    // Constructor tests
    // =================================================================

    @Test
    void constructor_setsIdNameAndHumanFlag() {
        assertThat(player.getID()).isEqualTo(1);
        assertThat(player.getName()).isEqualTo("Alice");
        assertThat(player.isHuman()).isTrue();
    }

    @Test
    void constructor_withPlayerKey() {
        ClientPlayer p = new ClientPlayer("key-123", 2, "Bob", false);
        assertThat(p.getPlayerId()).isEqualTo("key-123");
        assertThat(p.getID()).isEqualTo(2);
        assertThat(p.getName()).isEqualTo("Bob");
        assertThat(p.isHuman()).isFalse();
    }

    @Test
    void constructor_withoutPlayerKey_delegatesToFourArgConstructor() {
        ClientPlayer p = new ClientPlayer(3, "Carol", true);
        assertThat(p.getPlayerId()).isNull();
        assertThat(p.getID()).isEqualTo(3);
    }

    @Test
    void constructor_withProfile() {
        PlayerProfile profile = new PlayerProfile("Dave");
        ClientPlayer p = new ClientPlayer("key-456", 4, profile, false);
        assertThat(p.getName()).isEqualTo("Dave");
        assertThat(p.getProfile()).isSameAs(profile);
        assertThat(p.getPlayerId()).isEqualTo("key-456");
    }

    @Test
    void defaultValues_afterConstruction() {
        assertThat(player.getChipCount()).isZero();
        assertThat(player.isFolded()).isFalse();
        assertThat(player.isSittingOut()).isFalse();
        assertThat(player.isCardsExposed()).isFalse();
        assertThat(player.isDisconnected()).isFalse();
        assertThat(player.isBooted()).isFalse();
        assertThat(player.isWaiting()).isFalse();
        assertThat(player.getSeat()).isZero();
        assertThat(player.getPrize()).isZero();
        assertThat(player.getPlace()).isZero();
        assertThat(player.getBuyin()).isZero();
        assertThat(player.getAddon()).isZero();
        assertThat(player.getRebuy()).isZero();
        assertThat(player.getNumRebuys()).isZero();
        assertThat(player.getAllInWin()).isZero();
        assertThat(player.getHandScore()).isEqualTo(-1);
        assertThat(player.getHand()).isNotNull();
        assertThat(player.getHand().size()).isZero();
    }

    // =================================================================
    // isAllIn tests
    // =================================================================

    @Nested
    class IsAllIn {

        @Test
        void true_whenNotFolded_zeroChips_hasCards() {
            player.setChipCount(0);
            player.setFolded(false);
            player.getHand().addCard(Card.getCard("Ah"));
            player.getHand().addCard(Card.getCard("Kh"));

            assertThat(player.isAllIn()).isTrue();
        }

        @Test
        void false_whenFolded() {
            player.setChipCount(0);
            player.setFolded(true);
            player.getHand().addCard(Card.getCard("Ah"));

            assertThat(player.isAllIn()).isFalse();
        }

        @Test
        void false_whenHasChips() {
            player.setChipCount(100);
            player.setFolded(false);
            player.getHand().addCard(Card.getCard("Ah"));

            assertThat(player.isAllIn()).isFalse();
        }

        @Test
        void false_whenNoCards() {
            player.setChipCount(0);
            player.setFolded(false);

            assertThat(player.isAllIn()).isFalse();
        }
    }

    // =================================================================
    // isInHand tests
    // =================================================================

    @Nested
    class IsInHand {

        @Test
        void true_whenHasCards_notFolded() {
            player.getHand().addCard(Card.getCard("Ah"));
            player.setFolded(false);

            assertThat(player.isInHand()).isTrue();
        }

        @Test
        void false_whenFolded() {
            player.getHand().addCard(Card.getCard("Ah"));
            player.setFolded(true);

            assertThat(player.isInHand()).isFalse();
        }

        @Test
        void false_whenNoCards() {
            player.setFolded(false);

            assertThat(player.isInHand()).isFalse();
        }
    }

    // =================================================================
    // showFoldedHand tests
    // =================================================================

    @Nested
    class ShowFoldedHand {

        @Test
        void true_whenFolded_andCardsExposed() {
            player.setFolded(true);
            player.setCardsExposed(true);

            assertThat(player.showFoldedHand()).isTrue();
        }

        @Test
        void false_whenNotFolded() {
            player.setFolded(false);
            player.setCardsExposed(true);

            assertThat(player.showFoldedHand()).isFalse();
        }

        @Test
        void false_whenCardsNotExposed() {
            player.setFolded(true);
            player.setCardsExposed(false);

            assertThat(player.showFoldedHand()).isFalse();
        }
    }

    // =================================================================
    // Chip and spend tracking
    // =================================================================

    @Test
    void addChips_incrementsChipCount() {
        player.setChipCount(100);
        player.addChips(50);

        assertThat(player.getChipCount()).isEqualTo(150);
    }

    @Test
    void addRebuy_updatesCountAndChips() {
        player.setChipCount(100);
        player.addRebuy(50, 500, false);

        assertThat(player.getNumRebuys()).isEqualTo(1);
        assertThat(player.getRebuy()).isEqualTo(50);
        assertThat(player.getChipCount()).isEqualTo(600);
    }

    @Test
    void addRebuy_accumulates() {
        player.addRebuy(50, 500, false);
        player.addRebuy(50, 500, false);

        assertThat(player.getNumRebuys()).isEqualTo(2);
        assertThat(player.getRebuy()).isEqualTo(100);
    }

    @Test
    void addAddon_updatesAmountAndChips() {
        player.setChipCount(100);
        player.addAddon(75, 750);

        assertThat(player.getAddon()).isEqualTo(75);
        assertThat(player.getChipCount()).isEqualTo(850);
    }

    @Test
    void getTotalSpent_sumsBuyinRebuyAddon() {
        player.setBuyin(100);
        player.addRebuy(50, 500, false);
        player.addAddon(25, 250);

        assertThat(player.getTotalSpent()).isEqualTo(175);
    }

    // =================================================================
    // Effective hand strength
    // =================================================================

    @Test
    void getEffectiveHandStrength_formula() {
        player.setHandStrength(0.5f);
        player.setHandPotential(0.3f);

        // EHS = HS + (1-HS)*HP = 0.5 + 0.5*0.3 = 0.65
        assertThat(player.getEffectiveHandStrength()).isCloseTo(0.65f, within(0.001f));
    }

    @Test
    void getEffectiveHandStrength_zeroValues() {
        player.setHandStrength(0.0f);
        player.setHandPotential(0.0f);

        assertThat(player.getEffectiveHandStrength()).isCloseTo(0.0f, within(0.001f));
    }

    @Test
    void getEffectiveHandStrength_maxValues() {
        player.setHandStrength(1.0f);
        player.setHandPotential(1.0f);

        // EHS = 1.0 + (1-1.0)*1.0 = 1.0
        assertThat(player.getEffectiveHandStrength()).isCloseTo(1.0f, within(0.001f));
    }

    // =================================================================
    // Position names (static)
    // =================================================================

    @Test
    void getPositionName_returnsCorrectNames() {
        assertThat(ClientPlayer.getPositionName(ClientPlayer.EARLY)).isEqualTo("early");
        assertThat(ClientPlayer.getPositionName(ClientPlayer.MIDDLE)).isEqualTo("middle");
        assertThat(ClientPlayer.getPositionName(ClientPlayer.LATE)).isEqualTo("late");
        assertThat(ClientPlayer.getPositionName(ClientPlayer.SMALL)).isEqualTo("small");
        assertThat(ClientPlayer.getPositionName(ClientPlayer.BIG)).isEqualTo("big");
    }

    @Test
    void getPositionName_unknownReturnsNone() {
        assertThat(ClientPlayer.getPositionName(99)).isEqualTo("none");
        assertThat(ClientPlayer.getPositionName(-1)).isEqualTo("none");
    }

    // =================================================================
    // Card management
    // =================================================================

    @Test
    void removeHand_clearsCardsAndSortedHand() {
        player.getHand().addCard(Card.getCard("Ah"));
        player.getHand().addCard(Card.getCard("Kh"));
        // Force handSorted_ to be populated
        player.getHandSorted();

        player.removeHand();

        assertThat(player.getHand().size()).isZero();
    }

    @Test
    void newHand_createsNewHandOfGivenType() {
        player.getHand().addCard(Card.getCard("Ah"));

        Hand h = player.newHand(Hand.TYPE_DEAL_HIGH);

        assertThat(h).isNotNull();
        assertThat(h.size()).isZero();
        assertThat(player.getHand()).isSameAs(h);
    }

    @Test
    void getHandSorted_cachesBetweenCalls() {
        player.getHand().addCard(Card.getCard("Ah"));
        player.getHand().addCard(Card.getCard("Kh"));

        HandSorted first = player.getHandSorted();
        HandSorted second = player.getHandSorted();

        assertThat(first).isSameAs(second);
    }

    @Test
    void getHandSorted_refreshesWhenHandChanges() {
        player.getHand().addCard(Card.getCard("Ah"));
        HandSorted first = player.getHandSorted();

        player.getHand().addCard(Card.getCard("Kh"));
        HandSorted second = player.getHandSorted();

        assertThat(first).isNotSameAs(second);
    }

    // =================================================================
    // Profile
    // =================================================================

    @Test
    void setProfile_updatesName() {
        PlayerProfile profile = new PlayerProfile("NewName");
        player.setProfile(profile);

        assertThat(player.getName()).isEqualTo("NewName");
        assertThat(player.getProfile()).isSameAs(profile);
    }

    @Test
    void setProfile_null_doesNotChangeName() {
        player.setProfile(null);

        assertThat(player.getName()).isEqualTo("Alice");
        assertThat(player.getProfile()).isNull();
    }

    // =================================================================
    // Display
    // =================================================================

    @Test
    void getDisplayName_nonOnline_returnsName() {
        assertThat(player.getDisplayName(false)).isEqualTo("Alice");
    }

    @Test
    void isComputer_falseForHumanPlayer() {
        assertThat(player.isComputer()).isFalse();
    }

    @Test
    void isComputer_trueForNonHumanNonObserver() {
        ClientPlayer aiPlayer = new ClientPlayer(2, "Bot", false);
        assertThat(aiPlayer.isComputer()).isTrue();
    }

    @Test
    void isComputer_falseForObserver() {
        ClientPlayer observer = new ClientPlayer(2, "Observer", false);
        observer.setObserver(true);
        assertThat(observer.isComputer()).isFalse();
    }

    // =================================================================
    // Fold and action
    // =================================================================

    @Test
    void fold_setsFoldedTrue() {
        player.fold("test", 0);

        assertThat(player.isFolded()).isTrue();
    }

    @Test
    void getAction_returnsNull() {
        assertThat(player.getAction(true)).isNull();
        assertThat(player.getAction(false)).isNull();
    }

    // =================================================================
    // AllIn win tracking
    // =================================================================

    @Test
    void addAllInWin_increments() {
        player.addAllInWin();
        player.addAllInWin();

        assertThat(player.getAllInWin()).isEqualTo(2);
    }

    @Test
    void clearAllInWin_resetsToZero() {
        player.addAllInWin();
        player.addAllInWin();
        player.clearAllInWin();

        assertThat(player.getAllInWin()).isZero();
    }

    // =================================================================
    // toString / toStringShort
    // =================================================================

    @Test
    void toString_returnsName() {
        assertThat(player.toString()).isEqualTo("Alice");
    }

    @Test
    void toString_nullName_returnsUnnamedWithId() {
        ClientPlayer unnamed = new ClientPlayer(5, null, false);
        assertThat(unnamed.toString()).isEqualTo("[unnamed-5]");
    }

    @Test
    void toStringShort_includesSeatAndChips() {
        player.setSeat(3);
        player.setChipCount(1500);

        assertThat(player.toStringShort()).isEqualTo("Alice (seat=3 chips=1500)");
    }

    // =================================================================
    // Table linkage
    // =================================================================

    @Test
    void setTable_setsTableAndSeat() {
        ClientPokerTable mockTable = mock(ClientPokerTable.class);
        player.setTable(mockTable, 5);

        assertThat(player.getTable()).isSameAs(mockTable);
        assertThat(player.getSeat()).isEqualTo(5);
    }

    // =================================================================
    // Miscellaneous setters/getters
    // =================================================================

    @Test
    void chipCountAtStart_getterSetter() {
        player.setChipCountAtStart(1000);
        assertThat(player.getChipCountAtStart()).isEqualTo(1000);

        player.adjustChipCountAtStart(500);
        assertThat(player.getChipCountAtStart()).isEqualTo(1500);
    }

    @Test
    void sittingOut_getterSetter() {
        player.setSittingOut(true);
        assertThat(player.isSittingOut()).isTrue();
    }

    @Test
    void disconnected_getterSetter() {
        player.setDisconnected(true);
        assertThat(player.isDisconnected()).isTrue();
    }

    @Test
    void booted_getterSetter() {
        player.setBooted(true);
        assertThat(player.isBooted()).isTrue();
    }

    @Test
    void waiting_getterSetter() {
        player.setWaiting(true);
        assertThat(player.isWaiting()).isTrue();
    }

    @Test
    void onlineActivated_getterSetter() {
        player.setOnlineActivated(true);
        assertThat(player.isOnlineActivated()).isTrue();
    }

    @Test
    void handStrengthAndPotential_getterSetter() {
        player.setHandStrength(0.75f);
        player.setHandPotential(0.25f);

        assertThat(player.getHandStrength()).isCloseTo(0.75f, within(0.001f));
        assertThat(player.getHandPotential()).isCloseTo(0.25f, within(0.001f));
        assertThat(player.getHandPotentialDisplay()).isCloseTo(0.25f, within(0.001f));
    }

    @Test
    void place_getterSetter() {
        player.setPlace(3);
        assertThat(player.getPlace()).isEqualTo(3);
    }

    @Test
    void prize_getterSetter() {
        player.setPrize(5000);
        assertThat(player.getPrize()).isEqualTo(5000);
    }

    @Test
    void bounty_getterSetter() {
        player.setBountyCollected(200);
        player.setBountyCount(3);
        assertThat(player.getBountyCollected()).isEqualTo(200);
        assertThat(player.getBountyCount()).isEqualTo(3);
    }

    @Test
    void allInPercAndScore_getterSetter() {
        player.setAllInPerc("45%");
        player.setAllInScore(85);
        assertThat(player.getAllInPerc()).isEqualTo("45%");
        assertThat(player.getAllInScore()).isEqualTo(85);
    }

    @Test
    void handScore_getterSetter() {
        player.setHandScore(42);
        assertThat(player.getHandScore()).isEqualTo(42);
    }

    @Test
    void version_getterSetter() {
        com.donohoedigital.comms.Version v = new com.donohoedigital.comms.Version("3.3.0");
        player.setVersion(v);
        assertThat(player.getVersion()).isSameAs(v);
    }

    @Test
    void profilePath_getterSetter() {
        player.setProfilePath("/some/path");
        assertThat(player.getProfilePath()).isEqualTo("/some/path");
    }

    @Test
    void isProfileDefined_reflectsProfileState() {
        assertThat(player.isProfileDefined()).isFalse();
        player.setProfile(new PlayerProfile("Test"));
        assertThat(player.isProfileDefined()).isTrue();
    }

    @Test
    void muckLosing_defaultsToTrue() {
        assertThat(player.isMuckLosing()).isTrue();
    }

    @Test
    void emptyConstructor_createsPlayer() {
        ClientPlayer empty = new ClientPlayer();
        assertThat(empty.getHand()).isNotNull();
        assertThat(empty.isHuman()).isFalse();
    }

    @Test
    void sortByName_comparator() {
        ClientPlayer a = new ClientPlayer(1, "Alice", true);
        ClientPlayer b = new ClientPlayer(2, "Bob", true);
        ClientPlayer c = new ClientPlayer(3, "Carol", true);

        java.util.List<ClientPlayer> players = new java.util.ArrayList<>(java.util.List.of(c, a, b));
        players.sort(ClientPlayer.SORTBYNAME);

        assertThat(players).extracting(ClientPlayer::getName).containsExactly("Alice", "Bob", "Carol");
    }

    @Test
    void noOpMethods_doNotThrow() {
        // These are no-op methods for save/load compatibility
        assertThatCode(() -> player.createPokerAI()).doesNotThrowAnyException();
        assertThatCode(() -> player.addPendingRebuys()).doesNotThrowAnyException();
        assertThatCode(() -> player.gameLoaded()).doesNotThrowAnyException();
        assertThatCode(() -> player.betTest(100)).doesNotThrowAnyException();
        assertThatCode(() -> player.loadProfile(null)).doesNotThrowAnyException();
        assertThatCode(() -> player.setTimeoutMessageSecondsLeft(10)).doesNotThrowAnyException();
    }
}
