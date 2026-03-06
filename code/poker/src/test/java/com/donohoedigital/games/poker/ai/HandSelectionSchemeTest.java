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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.HandGroup;
import com.donohoedigital.games.poker.display.ClientCard;
import com.donohoedigital.games.poker.display.ClientHand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HandSelectionSchemeTest {

    private HandSelectionScheme createScheme() {
        return new HandSelectionScheme("TestScheme");
    }

    // =================================================================
    // getHandStrength(ClientCard, ClientCard) tests
    // =================================================================

    @Nested
    class GetHandStrengthCards {

        @Test
        void returns_zero_when_no_groups() {
            HandSelectionScheme scheme = createScheme();
            ClientCard aceSpades = ClientCard.getCard(ClientCard.SPADES, ClientCard.ACE);
            ClientCard kingHearts = ClientCard.getCard(ClientCard.HEARTS, ClientCard.KING);

            assertThat(scheme.getHandStrength(aceSpades, kingHearts)).isEqualTo(0.0f);
        }

        @Test
        void returns_zero_when_hand_not_in_any_group() {
            HandSelectionScheme scheme = createScheme();
            // Add a group containing only pocket aces
            HandGroup group = HandGroup.parse("AA", 8);
            scheme.getHandGroups().add(group);

            // 7-2 offsuit should not match
            ClientCard seven = ClientCard.getCard(ClientCard.SPADES, ClientCard.SEVEN);
            ClientCard two = ClientCard.getCard(ClientCard.HEARTS, ClientCard.TWO);

            assertThat(scheme.getHandStrength(seven, two)).isEqualTo(0.0f);
        }

        @Test
        void returns_strength_divided_by_ten_when_hand_matches_group() {
            HandSelectionScheme scheme = createScheme();
            HandGroup group = HandGroup.parse("AA", 8);
            scheme.getHandGroups().add(group);

            ClientCard aceSpades = ClientCard.getCard(ClientCard.SPADES, ClientCard.ACE);
            ClientCard aceHearts = ClientCard.getCard(ClientCard.HEARTS, ClientCard.ACE);

            assertThat(scheme.getHandStrength(aceSpades, aceHearts)).isEqualTo(0.8f);
        }

        @Test
        void returns_strength_for_suited_hand() {
            HandSelectionScheme scheme = createScheme();
            HandGroup group = HandGroup.parse("AKs", 7);
            scheme.getHandGroups().add(group);

            ClientCard aceSpades = ClientCard.getCard(ClientCard.SPADES, ClientCard.ACE);
            ClientCard kingSpades = ClientCard.getCard(ClientCard.SPADES, ClientCard.KING);

            assertThat(scheme.getHandStrength(aceSpades, kingSpades)).isEqualTo(0.7f);
        }

        @Test
        void returns_zero_for_offsuit_when_only_suited_in_group() {
            HandSelectionScheme scheme = createScheme();
            HandGroup group = HandGroup.parse("AKs", 7);
            scheme.getHandGroups().add(group);

            ClientCard aceSpades = ClientCard.getCard(ClientCard.SPADES, ClientCard.ACE);
            ClientCard kingHearts = ClientCard.getCard(ClientCard.HEARTS, ClientCard.KING);

            assertThat(scheme.getHandStrength(aceSpades, kingHearts)).isEqualTo(0.0f);
        }

        @Test
        void last_matching_group_wins() {
            HandSelectionScheme scheme = createScheme();
            // First group: AA with strength 4
            HandGroup group1 = HandGroup.parse("AA", 4);
            scheme.getHandGroups().add(group1);
            // Second group: AA with strength 9
            HandGroup group2 = HandGroup.parse("AA", 9);
            scheme.getHandGroups().add(group2);

            ClientCard aceSpades = ClientCard.getCard(ClientCard.SPADES, ClientCard.ACE);
            ClientCard aceHearts = ClientCard.getCard(ClientCard.HEARTS, ClientCard.ACE);

            // Iteration is reverse (last group checked first), so group2 matches first
            assertThat(scheme.getHandStrength(aceSpades, aceHearts)).isEqualTo(0.9f);
        }

        @Test
        void card_order_does_not_matter() {
            HandSelectionScheme scheme = createScheme();
            HandGroup group = HandGroup.parse("AK", 6);
            scheme.getHandGroups().add(group);

            ClientCard aceSpades = ClientCard.getCard(ClientCard.SPADES, ClientCard.ACE);
            ClientCard kingHearts = ClientCard.getCard(ClientCard.HEARTS, ClientCard.KING);

            // AK offsuit - both orderings should work since HandGroup.contains normalizes
            float strength1 = scheme.getHandStrength(aceSpades, kingHearts);
            float strength2 = scheme.getHandStrength(kingHearts, aceSpades);

            assertThat(strength1).isEqualTo(0.6f);
            assertThat(strength2).isEqualTo(0.6f);
        }
    }

    // =================================================================
    // getHandStrength(ClientHand) tests
    // =================================================================

    @Nested
    class GetHandStrengthClientHand {

        @Test
        void delegates_to_card_overload() {
            HandSelectionScheme scheme = createScheme();
            HandGroup group = HandGroup.parse("AA", 10);
            scheme.getHandGroups().add(group);

            ClientCard aceSpades = ClientCard.getCard(ClientCard.SPADES, ClientCard.ACE);
            ClientCard aceHearts = ClientCard.getCard(ClientCard.HEARTS, ClientCard.ACE);
            ClientHand hand = ClientHand.of(aceSpades, aceHearts);

            assertThat(scheme.getHandStrength(hand)).isEqualTo(1.0f);
        }

        @Test
        void returns_zero_for_unmatched_hand() {
            HandSelectionScheme scheme = createScheme();

            ClientCard seven = ClientCard.getCard(ClientCard.SPADES, ClientCard.SEVEN);
            ClientCard two = ClientCard.getCard(ClientCard.HEARTS, ClientCard.TWO);
            ClientHand hand = ClientHand.of(seven, two);

            assertThat(scheme.getHandStrength(hand)).isEqualTo(0.0f);
        }
    }

    // =================================================================
    // Group management tests
    // =================================================================

    @Nested
    class GroupManagement {

        @Test
        void new_scheme_starts_with_empty_groups() {
            HandSelectionScheme scheme = createScheme();

            assertThat(scheme.getHandGroups()).isEmpty();
        }

        @Test
        void ensureEmptyGroup_adds_one_group() {
            HandSelectionScheme scheme = createScheme();
            scheme.ensureEmptyGroup();

            assertThat(scheme.getHandGroups()).hasSize(1);
            assertThat(scheme.getHandGroups().get(0).getClassCount()).isZero();
        }

        @Test
        void ensureEmptyGroup_called_twice_results_in_one_empty_group() {
            // ensureEmptyGroup removes existing empty groups before adding a new one
            HandSelectionScheme scheme = createScheme();
            scheme.ensureEmptyGroup();
            scheme.ensureEmptyGroup();

            assertThat(scheme.getHandGroups()).hasSize(1);
            assertThat(scheme.getHandGroups().get(0).getClassCount()).isZero();
        }

        @Test
        void removeEmptyGroups_removes_groups_with_zero_hand_classes() {
            HandSelectionScheme scheme = createScheme();
            scheme.getHandGroups().add(new HandGroup());
            scheme.getHandGroups().add(HandGroup.parse("AA", 5));
            scheme.getHandGroups().add(new HandGroup());

            scheme.removeEmptyGroups();

            assertThat(scheme.getHandGroups()).hasSize(1);
            assertThat(scheme.getHandGroups().get(0).getClassCount()).isPositive();
        }

        @Test
        void removeEmptyGroups_keeps_all_when_none_empty() {
            HandSelectionScheme scheme = createScheme();
            scheme.getHandGroups().add(HandGroup.parse("AA", 5));
            scheme.getHandGroups().add(HandGroup.parse("KK", 4));

            scheme.removeEmptyGroups();

            assertThat(scheme.getHandGroups()).hasSize(2);
        }

        @Test
        void ensureEmptyGroup_removes_existing_empty_before_adding() {
            HandSelectionScheme scheme = createScheme();
            scheme.getHandGroups().add(new HandGroup());
            scheme.getHandGroups().add(HandGroup.parse("AA", 5));

            scheme.ensureEmptyGroup();

            // The pre-existing empty group is removed, then one new empty group is added
            // So we should have: the AA group + the new empty group
            assertThat(scheme.getHandGroups()).hasSize(2);
            // The first group should be the one with hands (AA)
            assertThat(scheme.getHandGroups().get(0).getClassCount()).isPositive();
            // The second should be the newly added empty group
            assertThat(scheme.getHandGroups().get(1).getClassCount()).isZero();
        }
    }

    // =================================================================
    // Description tests
    // =================================================================

    @Nested
    class Description {

        @Test
        void default_description_is_empty_string() {
            HandSelectionScheme scheme = createScheme();

            assertThat(scheme.getDescription()).isEmpty();
        }

        @Test
        void set_get_description_round_trips() {
            HandSelectionScheme scheme = createScheme();
            scheme.setDescription("Premium hands only");

            assertThat(scheme.getDescription()).isEqualTo("Premium hands only");
        }

        @Test
        void description_can_be_overwritten() {
            HandSelectionScheme scheme = createScheme();
            scheme.setDescription("First");
            scheme.setDescription("Second");

            assertThat(scheme.getDescription()).isEqualTo("Second");
        }
    }

    // =================================================================
    // Map tests
    // =================================================================

    @Nested
    class MapTests {

        @Test
        void getMap_returns_non_null_for_named_constructor() {
            HandSelectionScheme scheme = createScheme();

            assertThat(scheme.getMap()).isNotNull();
        }

        @Test
        void getMap_is_null_for_default_constructor() {
            HandSelectionScheme scheme = new HandSelectionScheme();

            assertThat(scheme.getMap()).isNull();
        }
    }
}
