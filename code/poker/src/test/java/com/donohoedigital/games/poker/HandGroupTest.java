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
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandGroup - poker hand classification groups used for AI strategy.
 */
class HandGroupTest {

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyGroup_WithDefaultConstructor() {
        HandGroup group = new HandGroup();

        assertThat(group.getClassCount()).isZero();
        assertThat(group.getHandCount()).isZero();
    }

    @Test
    void should_CreateGroupWithName() {
        HandGroup group = new HandGroup("Test Group");

        assertThat(group.getName()).isEqualTo("Test Group");
        assertThat(group.getClassCount()).isZero();
    }

    @Test
    void should_CopyGroup_WithPrototypeConstructor() {
        HandGroup original = new HandGroup("Original");
        original.setContainsPair(Card.ACE, true);
        original.setContainsSuited(Card.ACE, Card.KING, true);

        HandGroup copy = new HandGroup(original);

        assertThat(copy.containsPair(Card.ACE)).isTrue();
        assertThat(copy.containsSuited(Card.ACE, Card.KING)).isTrue();
        assertThat(copy.getClassCount()).isEqualTo(original.getClassCount());
    }

    @Test
    void should_CopyGroupWithNewName() {
        HandGroup original = new HandGroup("Original");
        original.setContainsPair(Card.ACE, true);

        HandGroup copy = new HandGroup(original, "Copy");

        assertThat(copy.getName()).isEqualTo("Copy");
        assertThat(copy.containsPair(Card.ACE)).isTrue();
    }

    // ========== Pair Tests ==========

    @Test
    void should_AddPair() {
        HandGroup group = new HandGroup();

        group.setContainsPair(Card.ACE, true);

        assertThat(group.containsPair(Card.ACE)).isTrue();
        assertThat(group.getClassCount()).isEqualTo(1);
        assertThat(group.getHandCount()).isEqualTo(6); // 6 combinations of AA
    }

    @Test
    void should_RemovePair() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);

        group.setContainsPair(Card.ACE, false);

        assertThat(group.containsPair(Card.ACE)).isFalse();
        assertThat(group.getClassCount()).isZero();
        assertThat(group.getHandCount()).isZero();
    }

    @Test
    void should_AddMultiplePairs() {
        HandGroup group = new HandGroup();

        group.setContainsPair(Card.ACE, true);
        group.setContainsPair(Card.KING, true);
        group.setContainsPair(Card.QUEEN, true);

        assertThat(group.getClassCount()).isEqualTo(3);
        assertThat(group.getHandCount()).isEqualTo(18); // 6 + 6 + 6
    }

    // ========== Suited Tests ==========

    @Test
    void should_AddSuitedHand() {
        HandGroup group = new HandGroup();

        group.setContainsSuited(Card.ACE, Card.KING, true);

        assertThat(group.containsSuited(Card.ACE, Card.KING)).isTrue();
        assertThat(group.getClassCount()).isEqualTo(1);
        assertThat(group.getHandCount()).isEqualTo(4); // 4 suited combinations
    }

    @Test
    void should_HandleSuitedWithReversedRanks() {
        HandGroup group = new HandGroup();

        group.setContainsSuited(Card.KING, Card.ACE, true);

        assertThat(group.containsSuited(Card.ACE, Card.KING)).isTrue();
        assertThat(group.containsSuited(Card.KING, Card.ACE)).isTrue();
    }

    @Test
    void should_RemoveSuitedHand() {
        HandGroup group = new HandGroup();
        group.setContainsSuited(Card.ACE, Card.KING, true);

        group.setContainsSuited(Card.ACE, Card.KING, false);

        assertThat(group.containsSuited(Card.ACE, Card.KING)).isFalse();
        assertThat(group.getClassCount()).isZero();
    }

    // ========== Offsuit Tests ==========

    @Test
    void should_AddOffsuitHand() {
        HandGroup group = new HandGroup();

        group.setContainsOffsuit(Card.ACE, Card.KING, true);

        assertThat(group.containsOffsuit(Card.ACE, Card.KING)).isTrue();
        assertThat(group.getClassCount()).isEqualTo(1);
        assertThat(group.getHandCount()).isEqualTo(12); // 12 offsuit combinations
    }

    @Test
    void should_HandleOffsuitWithReversedRanks() {
        HandGroup group = new HandGroup();

        group.setContainsOffsuit(Card.KING, Card.ACE, true);

        assertThat(group.containsOffsuit(Card.ACE, Card.KING)).isTrue();
        assertThat(group.containsOffsuit(Card.KING, Card.ACE)).isTrue();
    }

    @Test
    void should_TreatPairAsOffsuit_WhenSameRank() {
        HandGroup group = new HandGroup();

        group.setContainsOffsuit(Card.ACE, Card.ACE, true);

        assertThat(group.containsPair(Card.ACE)).isTrue();
        assertThat(group.containsOffsuit(Card.ACE, Card.ACE)).isTrue();
    }

    // ========== Combined Contains Tests ==========

    @Test
    void should_SetBothSuitedAndOffsuit_WithGenericContains() {
        HandGroup group = new HandGroup();

        group.setContains(Card.ACE, Card.KING, true);

        assertThat(group.containsSuited(Card.ACE, Card.KING)).isTrue();
        assertThat(group.containsOffsuit(Card.ACE, Card.KING)).isTrue();
        assertThat(group.getClassCount()).isEqualTo(2);
        // Note: Generic setContains updates classCount but not handCount
    }

    @Test
    void should_SetPair_WithGenericContains() {
        HandGroup group = new HandGroup();

        group.setContains(Card.ACE, Card.ACE, true);

        assertThat(group.containsPair(Card.ACE)).isTrue();
    }

    @Test
    void should_SetSuitedOnly_WithSuitedFlag() {
        HandGroup group = new HandGroup();

        group.setContains(Card.ACE, Card.KING, true, true);

        assertThat(group.containsSuited(Card.ACE, Card.KING)).isTrue();
        assertThat(group.containsOffsuit(Card.ACE, Card.KING)).isFalse();
    }

    // ========== Count Tests ==========

    @Test
    void should_TrackClassCount_Correctly() {
        HandGroup group = new HandGroup();

        assertThat(group.getClassCount()).isZero();

        group.setContainsPair(Card.ACE, true);
        assertThat(group.getClassCount()).isEqualTo(1);

        group.setContainsSuited(Card.ACE, Card.KING, true);
        assertThat(group.getClassCount()).isEqualTo(2);

        group.setContainsOffsuit(Card.ACE, Card.KING, true);
        assertThat(group.getClassCount()).isEqualTo(3);
    }

    @Test
    void should_TrackHandCount_Correctly() {
        HandGroup group = new HandGroup();

        group.setContainsPair(Card.ACE, true); // 6 hands
        group.setContainsSuited(Card.ACE, Card.KING, true); // 4 hands
        group.setContainsOffsuit(Card.ACE, Card.KING, true); // 12 hands

        assertThat(group.getHandCount()).isEqualTo(22);
    }

    @Test
    void should_CalculatePercent_BasedOnHandCount() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true); // 6 hands

        double percent = group.getPercent();

        assertThat(percent).isGreaterThan(0).isLessThan(1);
    }

    // ========== Summary Tests ==========

    @Test
    void should_GenerateSummary_ForPairs() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);

        String summary = group.getSummary();

        assertThat(summary).contains("AA");
    }

    @Test
    void should_GenerateSummary_ForSuited() {
        HandGroup group = new HandGroup();
        group.setContainsSuited(Card.ACE, Card.KING, true);

        String summary = group.getSummary();

        assertThat(summary).contains("AKs");
    }

    @Test
    void should_GenerateSummary_ForOffsuit() {
        HandGroup group = new HandGroup();
        group.setContainsOffsuit(Card.ACE, Card.KING, true);

        String summary = group.getSummary();

        assertThat(summary).contains("AK");
        assertThat(summary).doesNotContain("AKs");
    }

    @Test
    void should_GenerateSummary_WithRanges_ForConsecutivePairs() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        group.setContainsPair(Card.KING, true);
        group.setContainsPair(Card.QUEEN, true);

        String summary = group.getSummary();

        assertThat(summary).contains("AA-QQ");
    }

    @Test
    void should_CacheSummary() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);

        String summary1 = group.getSummary();
        String summary2 = group.getSummary();

        assertThat(summary1).isSameAs(summary2);
    }

    @Test
    void should_InvalidateSummary_WhenContentsChange() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        String summary1 = group.getSummary();

        group.setContainsPair(Card.KING, true);
        String summary2 = group.getSummary();

        assertThat(summary1).isNotEqualTo(summary2);
    }

    // ========== Expand Tests ==========

    @Test
    void should_ExpandToHandList() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);

        HandList expanded = group.expand();

        assertThat(expanded).isNotNull();
        assertThat(expanded.size()).isEqualTo(6); // 6 combinations of AA
    }

    @Test
    void should_CacheExpandedHandList() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);

        HandList expanded1 = group.expand();
        HandList expanded2 = group.expand();

        assertThat(expanded1).isSameAs(expanded2);
    }

    @Test
    void should_InvalidateExpanded_WhenContentsChange() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        HandList expanded1 = group.expand();

        group.setContainsPair(Card.KING, true);
        HandList expanded2 = group.expand();

        assertThat(expanded1).isNotSameAs(expanded2);
        assertThat(expanded2.size()).isGreaterThan(expanded1.size());
    }

    // ========== Parse Tests ==========

    @Test
    void should_ParseSimpleHand() {
        HandGroup group = HandGroup.parse("AKs", 5);

        assertThat(group.containsSuited(Card.ACE, Card.KING)).isTrue();
        assertThat(group.getStrength()).isEqualTo(5);
    }

    @Test
    void should_ParseOffsuitHand() {
        HandGroup group = HandGroup.parse("AK", 5);

        assertThat(group.containsOffsuit(Card.ACE, Card.KING)).isTrue();
        assertThat(group.containsSuited(Card.ACE, Card.KING)).isFalse();
    }

    @Test
    void should_ParsePair() {
        HandGroup group = HandGroup.parse("AA", 10);

        assertThat(group.containsPair(Card.ACE)).isTrue();
    }

    @Test
    void should_ParseMultipleHands() {
        HandGroup group = HandGroup.parse("AA,KK,AK", 5);

        assertThat(group.containsPair(Card.ACE)).isTrue();
        assertThat(group.containsPair(Card.KING)).isTrue();
        assertThat(group.containsOffsuit(Card.ACE, Card.KING)).isTrue();
    }

    @Test
    void should_ParseRange_ForPairs() {
        HandGroup group = HandGroup.parse("AA-QQ", 5);

        assertThat(group.containsPair(Card.ACE)).isTrue();
        assertThat(group.containsPair(Card.KING)).isTrue();
        assertThat(group.containsPair(Card.QUEEN)).isTrue();
        assertThat(group.containsPair(Card.JACK)).isFalse();
    }

    @Test
    void should_ParseRange_ForSuited() {
        HandGroup group = HandGroup.parse("AKs-ATs", 5);

        assertThat(group.containsSuited(Card.ACE, Card.KING)).isTrue();
        assertThat(group.containsSuited(Card.ACE, Card.QUEEN)).isTrue();
        assertThat(group.containsSuited(Card.ACE, Card.JACK)).isTrue();
        assertThat(group.containsSuited(Card.ACE, Card.TEN)).isTrue();
        assertThat(group.containsSuited(Card.ACE, Card.NINE)).isFalse();
    }

    // ========== HashCode/Equals Tests ==========

    @Test
    void should_GenerateHashCode() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        group.setStrength(5);

        int hash = group.hashCode();

        assertThat(hash).isNotZero();
    }

    @Test
    void should_CacheHashCode() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        group.setStrength(5);

        int hash1 = group.hashCode();
        int hash2 = group.hashCode();

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void should_InvalidateHashCode_WhenContentsChange() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.KING, true);
        group.setStrength(5);
        int hash1 = group.hashCode();

        // Remove King and add Ace (higher rank) - should change hash
        group.setContainsPair(Card.KING, false);
        group.setContainsPair(Card.ACE, true);
        int hash2 = group.hashCode();

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void should_BeEqual_WhenHashCodesMatch() {
        HandGroup group1 = HandGroup.parse("AA", 5);
        HandGroup group2 = HandGroup.parse("AA", 5);

        assertThat(group1).isEqualTo(group2);
        assertThat(group1.hashCode()).isEqualTo(group2.hashCode());
    }

    @Test
    void should_NotBeEqual_WhenDifferentContents() {
        HandGroup group1 = HandGroup.parse("AA", 5);
        HandGroup group2 = HandGroup.parse("KK", 5);

        assertThat(group1).isNotEqualTo(group2);
    }

    // ========== Clear Tests ==========

    @Test
    void should_ClearAllContents() {
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        group.setContainsSuited(Card.ACE, Card.KING, true);
        group.setContainsOffsuit(Card.ACE, Card.KING, true);

        group.clearContents();

        assertThat(group.getClassCount()).isZero();
        assertThat(group.getHandCount()).isZero();
        assertThat(group.containsPair(Card.ACE)).isFalse();
        assertThat(group.containsSuited(Card.ACE, Card.KING)).isFalse();
        assertThat(group.containsOffsuit(Card.ACE, Card.KING)).isFalse();
    }

    // ========== GetAllHands Tests ==========

    @Test
    void should_GetAllHands_ReturnsSingleton() {
        HandGroup all1 = HandGroup.getAllHands();
        HandGroup all2 = HandGroup.getAllHands();

        assertThat(all1).isSameAs(all2);
    }

    @Test
    void should_GetAllHands_ContainsAllPossibleHands() {
        HandGroup all = HandGroup.getAllHands();

        // Check all pairs
        for (int rank = Card.TWO; rank <= Card.ACE; rank++) {
            assertThat(all.containsPair(rank)).isTrue();
        }

        // Check suited hands
        for (int rank1 = Card.THREE; rank1 <= Card.ACE; rank1++) {
            for (int rank2 = Card.TWO; rank2 < rank1; rank2++) {
                assertThat(all.containsSuited(rank1, rank2)).isTrue();
            }
        }

        // Check offsuit hands
        for (int rank1 = Card.THREE; rank1 <= Card.ACE; rank1++) {
            for (int rank2 = Card.TWO; rank2 < rank1; rank2++) {
                assertThat(all.containsOffsuit(rank1, rank2)).isTrue();
            }
        }
    }

    // ========== SetContains Group Tests ==========

    @Test
    void should_AddGroupContents() {
        HandGroup group1 = HandGroup.parse("AA,KK", 5);
        HandGroup group2 = new HandGroup();

        group2.setContains(group1, true);

        assertThat(group2.containsPair(Card.ACE)).isTrue();
        assertThat(group2.containsPair(Card.KING)).isTrue();
    }

    @Test
    void should_RemoveGroupContents() {
        HandGroup group1 = HandGroup.parse("AA,KK,QQ", 5);
        HandGroup group2 = new HandGroup(group1);

        HandGroup toRemove = HandGroup.parse("KK", 5);
        group2.setContains(toRemove, false);

        assertThat(group2.containsPair(Card.ACE)).isTrue();
        assertThat(group2.containsPair(Card.KING)).isFalse();
        assertThat(group2.containsPair(Card.QUEEN)).isTrue();
    }

    // ========== Utility Tests ==========

    @Test
    void should_ToString_ReturnName() {
        HandGroup group = new HandGroup("Test Group");

        assertThat(group.toString()).isEqualTo("Test Group");
    }

    @Test
    void should_GetAndSetDescription() {
        HandGroup group = new HandGroup();

        group.setDescription("Test description");

        assertThat(group.getDescription()).isEqualTo("Test description");
    }

    @Test
    void should_GenerateHTML() {
        HandGroup group = HandGroup.parse("AA", 5);
        group.setDescription("Premium pair");

        String html = group.toHTML();

        assertThat(html).contains("Premium pair");
        assertThat(html).contains("AA");
    }

    @Test
    void should_GetStrength() {
        HandGroup group = new HandGroup();

        group.setStrength(7);

        assertThat(group.getStrength()).isEqualTo(7);
    }
}
