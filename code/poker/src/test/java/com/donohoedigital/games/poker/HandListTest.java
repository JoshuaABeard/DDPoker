/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandList - manages poker hand groups (pre-flop starting hands).
 */
class HandListTest {

    @BeforeEach
    void setUp() {
        ConfigManager configMgr = new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        configMgr.loadGuiConfig(); // Required for Card.toString() in toHTML()
    }

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyList_WhenDefaultConstructor() {
        HandList list = new HandList();

        assertThat(list.size()).isZero();
        assertThat(list.getName()).isEqualTo("New Group");
        assertThat(list.getCount()).isZero();
    }

    @Test
    void should_SetName_WhenConstructorWithName() {
        HandList list = new HandList("Premium Hands");

        assertThat(list.getName()).isEqualTo("Premium Hands");
        assertThat(list.size()).isZero();
    }

    @Test
    void should_CopyHandsAndName_WhenCopyConstructor() {
        HandList original = new HandList("Original");
        original.addAllPairs(Card.ACE);
        original.addAllSuited(Card.ACE, Card.KING);

        HandList copy = new HandList(original);

        assertThat(copy.size()).isEqualTo(original.size());
        assertThat(copy.getName()).isEqualTo("Original");
        assertThat(copy.size()).isEqualTo(10); // 6 pairs AA + 4 suited AK
    }

    @Test
    void should_OverrideName_WhenCopyConstructorWithName() {
        HandList original = new HandList("Original");
        original.addAllPairs(Card.ACE);

        HandList copy = new HandList(original, "Copy");

        assertThat(copy.getName()).isEqualTo("Copy");
        assertThat(copy.size()).isEqualTo(6); // 6 pairs AA
    }

    // ========== Add Pairs Tests ==========

    @Test
    void should_Add6Hands_WhenAddAllPairsSingleRank() {
        HandList list = new HandList();

        list.addAllPairs(Card.ACE);

        assertThat(list.size()).isEqualTo(6); // 6 combinations of AA
        assertThat(list.getCount()).isEqualTo(1); // 1 unique pair type
    }

    @Test
    void should_AddCorrectHands_WhenAddAllPairsAces() {
        HandList list = new HandList();

        list.addAllPairs(Card.ACE);

        // Should contain all 6 combinations: AcAd, AcAh, AcAs, AdAh, AdAs, AhAs
        assertThat(list.size()).isEqualTo(6);
        for (int i = 0; i < list.size(); i++) {
            Hand hand = list.get(i);
            assertThat(hand.getCard(0).getRank()).isEqualTo(Card.ACE);
            assertThat(hand.getCard(1).getRank()).isEqualTo(Card.ACE);
        }
    }

    @Test
    void should_Add39Hands_WhenAddAllPairsRange() {
        HandList list = new HandList();

        list.addAllPairs(Card.JACK, Card.ACE); // JJ, QQ, KK, AA

        assertThat(list.size()).isEqualTo(24); // 4 ranks × 6 combinations = 24
        // Actually let me recalculate: JJ(6) + QQ(6) + KK(6) + AA(6) = 24
    }

    @Test
    void should_HandleReversedRange_WhenAddAllPairs() {
        HandList list = new HandList();

        list.addAllPairs(Card.ACE, Card.JACK); // Reversed order

        assertThat(list.size()).isEqualTo(24); // Should still add JJ-AA
    }

    // ========== Add Suited Tests ==========

    @Test
    void should_Add4Hands_WhenAddAllSuited() {
        HandList list = new HandList();

        list.addAllSuited(Card.ACE, Card.KING);

        assertThat(list.size()).isEqualTo(4); // AKs in 4 suits
        assertThat(list.getCount()).isEqualTo(1);
    }

    @Test
    void should_AddOnlySuitedHands_WhenAddAllSuited() {
        HandList list = new HandList();

        list.addAllSuited(Card.ACE, Card.KING);

        for (int i = 0; i < list.size(); i++) {
            Hand hand = list.get(i);
            assertThat(hand.getCard(0).getCardSuit()).isEqualTo(hand.getCard(1).getCardSuit());
        }
    }

    @Test
    void should_DoNothing_WhenAddAllSuitedSameRank() {
        HandList list = new HandList();

        list.addAllSuited(Card.ACE, Card.ACE);

        assertThat(list.size()).isZero(); // No suited pairs possible
        assertThat(list.getCount()).isZero();
    }

    // ========== Add Unsuited Tests ==========

    @Test
    void should_Add12Hands_WhenAddAllUnsuited() {
        HandList list = new HandList();

        list.addAllUnsuited(Card.ACE, Card.KING);

        assertThat(list.size()).isEqualTo(12); // AKo in 12 off-suit combinations
        assertThat(list.getCount()).isEqualTo(1);
    }

    @Test
    void should_AddOnlyUnsuitedHands_WhenAddAllUnsuited() {
        HandList list = new HandList();

        list.addAllUnsuited(Card.ACE, Card.KING);

        for (int i = 0; i < list.size(); i++) {
            Hand hand = list.get(i);
            assertThat(hand.getCard(0).getCardSuit()).isNotEqualTo(hand.getCard(1).getCardSuit());
        }
    }

    @Test
    void should_Add6PairHands_WhenAddAllUnsuitedSameRank() {
        HandList list = new HandList();

        list.addAllUnsuited(Card.ACE, Card.ACE);

        assertThat(list.size()).isEqualTo(6); // Falls back to addAllPairs
    }

    // ========== Add All (Suited + Unsuited) Tests ==========

    @Test
    void should_Add16Hands_WhenAddAllDifferentRanks() {
        HandList list = new HandList();

        list.addAll(Card.ACE, Card.KING);

        assertThat(list.size()).isEqualTo(16); // 4 suited + 12 unsuited
    }

    @Test
    void should_Add6Hands_WhenAddAllSameRank() {
        HandList list = new HandList();

        list.addAll(Card.KING, Card.KING);

        assertThat(list.size()).isEqualTo(6); // Falls back to pairs
    }

    // ========== Remove Tests ==========

    @Test
    void should_Remove6Hands_WhenRemoveAllPairs() {
        HandList list = new HandList();
        list.addAllPairs(Card.ACE);

        list.removeAllPairs(Card.ACE);

        assertThat(list.size()).isZero();
        assertThat(list.getCount()).isZero();
    }

    @Test
    void should_Remove4Hands_WhenRemoveAllSuited() {
        HandList list = new HandList();
        list.addAllSuited(Card.ACE, Card.KING);

        list.removeAllSuited(Card.ACE, Card.KING);

        assertThat(list.size()).isZero();
        assertThat(list.getCount()).isZero();
    }

    @Test
    void should_Remove12Hands_WhenRemoveAllUnsuited() {
        HandList list = new HandList();
        list.addAllUnsuited(Card.ACE, Card.KING);

        list.removeAllUnsuited(Card.ACE, Card.KING);

        assertThat(list.size()).isZero();
        assertThat(list.getCount()).isZero();
    }

    @Test
    void should_RemoveSpecificHand_WhenRemoveSingleHand() {
        HandList list = new HandList();
        Card aceClubs = Card.getCard(CardSuit.CLUBS, Card.ACE);
        Card aceDiamonds = Card.getCard(CardSuit.DIAMONDS, Card.ACE);
        HandSorted hand = new HandSorted(aceClubs, aceDiamonds);

        list.add(hand);
        assertThat(list.size()).isEqualTo(1);

        list.remove(hand);
        assertThat(list.size()).isZero();
    }

    // ========== ContainsAny Tests ==========

    @Test
    void should_ReturnTrue_WhenContainsAnyFindsHand() {
        HandList list = new HandList();
        list.addAllPairs(Card.ACE);

        boolean contains = list.containsAny(Card.ACE, Card.ACE);

        assertThat(contains).isTrue();
    }

    @Test
    void should_ReturnFalse_WhenContainsAnyDoesNotFindHand() {
        HandList list = new HandList();
        list.addAllPairs(Card.ACE);

        boolean contains = list.containsAny(Card.KING, Card.KING);

        assertThat(contains).isFalse();
    }

    @Test
    void should_ReturnTrue_WhenContainsAnySuitedFindsHand() {
        HandList list = new HandList();
        list.addAllSuited(Card.ACE, Card.KING);

        boolean contains = list.containsAny(Card.ACE, Card.KING, true);

        assertThat(contains).isTrue();
    }

    @Test
    void should_ReturnFalse_WhenContainsAnySuitedDoesNotFindHand() {
        HandList list = new HandList();
        list.addAllUnsuited(Card.ACE, Card.KING); // Only unsuited

        boolean contains = list.containsAny(Card.ACE, Card.KING, true); // Looking for suited

        assertThat(contains).isFalse();
    }

    @Test
    void should_ReturnTrue_WhenContainsAnyUnsuitedFindsHand() {
        HandList list = new HandList();
        list.addAllUnsuited(Card.ACE, Card.KING);

        boolean contains = list.containsAny(Card.ACE, Card.KING, false);

        assertThat(contains).isTrue();
    }

    @Test
    void should_ReturnFalse_WhenContainsAnyUnsuitedDoesNotFindHand() {
        HandList list = new HandList();
        list.addAllSuited(Card.ACE, Card.KING); // Only suited

        boolean contains = list.containsAny(Card.ACE, Card.KING, false); // Looking for unsuited

        assertThat(contains).isFalse();
    }

    // ========== Get and Size Tests ==========

    @Test
    void should_ReturnCorrectSize_WhenHandsAdded() {
        HandList list = new HandList();
        list.addAllPairs(Card.ACE);
        list.addAllSuited(Card.ACE, Card.KING);

        assertThat(list.size()).isEqualTo(10); // 6 + 4
    }

    @Test
    void should_ReturnCorrectHand_WhenGetByIndex() {
        HandList list = new HandList();
        Card aceClubs = Card.getCard(CardSuit.CLUBS, Card.ACE);
        Card aceDiamonds = Card.getCard(CardSuit.DIAMONDS, Card.ACE);
        HandSorted hand = new HandSorted(aceClubs, aceDiamonds);

        list.add(hand);

        HandSorted retrieved = list.get(0);
        assertThat(retrieved).isEqualTo(hand);
    }

    // ========== Percent Tests ==========

    @Test
    void should_CalculateCorrectPercent_WhenHandsAdded() {
        HandList list = new HandList();
        list.addAllPairs(Card.ACE); // 6 hands

        double percent = list.getPercent();

        // 6 / 13.26 ≈ 0.452 (where 13.26 = 1326/100, total starting hand combinations)
        assertThat(percent).isCloseTo(0.452, within(0.001));
    }

    @Test
    void should_Return100Percent_WhenAllHandsAdded() {
        HandList list = new HandList();

        // Add all 169 possible starting hands (13 pairs + 78*2 suited/unsuited combos)
        for (int rank1 = Card.TWO; rank1 <= Card.ACE; rank1++) {
            list.addAllPairs(rank1);
            for (int rank2 = rank1 + 1; rank2 <= Card.ACE; rank2++) {
                list.addAll(rank1, rank2);
            }
        }

        double percent = list.getPercent();

        // 1326 / 13.26 = 100
        assertThat(percent).isCloseTo(100.0, within(0.1));
    }

    // ========== Description Tests ==========

    @Test
    void should_ReturnNull_WhenDescriptionNotSet() {
        HandList list = new HandList();

        assertThat(list.getDescription()).isNull();
    }

    @Test
    void should_SetDescription_WhenValidString() {
        HandList list = new HandList();

        list.setDescription("Premium starting hands");

        assertThat(list.getDescription()).isEqualTo("Premium starting hands");
    }

    @Test
    void should_ClearDescription_WhenSetToNull() {
        HandList list = new HandList();
        list.setDescription("Description");

        list.setDescription(null);

        assertThat(list.getDescription()).isNull();
    }

    @Test
    void should_ClearDescription_WhenSetToEmptyString() {
        HandList list = new HandList();
        list.setDescription("Description");

        list.setDescription("");

        assertThat(list.getDescription()).isNull();
    }

    // ========== HTML Tests ==========

    @Test
    void should_GenerateHTML_WhenHandsPresent() {
        HandList list = new HandList();
        list.addAllPairs(Card.ACE);

        String html = list.toHTML();

        assertThat(html).isNotEmpty();
        assertThat(html).contains("A"); // Should contain Ace representation
    }

    @Test
    void should_IncludeDescription_WhenToHTMLWithDescription() {
        HandList list = new HandList();
        list.setDescription("Premium Hands");
        list.addAllPairs(Card.ACE);

        String html = list.toHTML();

        assertThat(html).contains("Premium Hands");
        assertThat(html).contains("<br><br>");
    }

    @Test
    void should_ReturnEmptyString_WhenToHTMLWithNoHands() {
        HandList list = new HandList();

        String html = list.toHTML();

        assertThat(html).isEmpty();
    }

    // ========== AddAll(HandList) Tests ==========

    @Test
    void should_AddAllHandsFromOtherList_WhenAddAllCalled() {
        HandList list1 = new HandList();
        list1.addAllPairs(Card.ACE);

        HandList list2 = new HandList();
        list2.addAllPairs(Card.KING);

        list1.addAll(list2);

        assertThat(list1.size()).isEqualTo(12); // 6 AA + 6 KK
    }

    @Test
    void should_NotAffectOriginal_WhenAddAllFromOtherList() {
        HandList list1 = new HandList();
        list1.addAllPairs(Card.ACE);

        HandList list2 = new HandList();
        list2.addAllPairs(Card.KING);

        list1.addAll(list2);

        assertThat(list2.size()).isEqualTo(6); // Original unchanged
    }

    // ========== ToString Tests ==========

    @Test
    void should_ReturnName_WhenToString() {
        HandList list = new HandList("My Hands");

        assertThat(list.toString()).isEqualTo("My Hands");
    }

    // ========== Edge Cases ==========

    @Test
    void should_HandleEmptyList_WhenRemovingNonExistentHands() {
        HandList list = new HandList();

        list.removeAllPairs(Card.ACE); // Should not throw

        assertThat(list.size()).isZero();
    }

    @Test
    void should_HandleDuplicateAdds_WhenSameHandAddedTwice() {
        HandList list = new HandList();

        list.addAllPairs(Card.ACE);
        list.addAllPairs(Card.ACE); // Add again

        assertThat(list.size()).isEqualTo(12); // 6 + 6 (duplicates allowed)
        assertThat(list.getCount()).isEqualTo(2); // Count tracks calls
    }

    @Test
    void should_ReturnZero_WhenGetCountOnEmptyList() {
        HandList list = new HandList();

        assertThat(list.getCount()).isZero();
    }

    @Test
    void should_HandleLowRanks_WhenAddingDeuces() {
        HandList list = new HandList();

        list.addAllPairs(Card.TWO);

        assertThat(list.size()).isEqualTo(6);
    }
}
