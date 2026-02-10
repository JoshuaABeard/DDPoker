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

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.HandGroup;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandSelectionScheme AI hand selection logic.
 */
class HandSelectionSchemeTest
{
    @BeforeEach
    void setUp()
    {
        // Initialize ConfigManager for tests (only once)
        if (!com.donohoedigital.config.PropertyConfig.isInitialized())
        {
            new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        }
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void should_CreateEmptyScheme_When_DefaultConstructorUsed()
    {
        HandSelectionScheme scheme = new HandSelectionScheme();

        assertThat(scheme.getName()).isEmpty();
    }

    @Test
    void should_CreateNamedScheme_When_StringConstructorUsed()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("TestScheme");

        assertThat(scheme.getName()).isEqualTo("TestScheme");
        assertThat(scheme.getMap()).isNotNull();
        assertThat(scheme.getHandGroups()).isNotNull().isEmpty();
    }

    @Test
    void should_CopySchemeWithNewName_When_CopyConstructorUsed()
    {
        HandSelectionScheme original = new HandSelectionScheme("Original");
        original.setDescription("Original description");

        HandSelectionScheme copy = new HandSelectionScheme(original, "Copy");

        assertThat(copy.getName()).isEqualTo("Copy");
        assertThat(copy.getDescription()).isEqualTo("Original description");
        assertThat(copy.getMap()).isNotNull();
    }

    @Test
    void should_PreserveIDWhenSameName_When_CopyConstructorUsed()
    {
        HandSelectionScheme original = new HandSelectionScheme("SameName");
        original.getMap().setLong("id", 12345L);

        HandSelectionScheme copy = new HandSelectionScheme(original, "SameName");

        assertThat(copy.getMap().getLong("id")).isEqualTo(12345L);
    }

    @Test
    void should_RemoveIDWhenDifferentName_When_CopyConstructorUsed()
    {
        HandSelectionScheme original = new HandSelectionScheme("Original");
        original.getMap().setLong("id", 12345L);

        HandSelectionScheme copy = new HandSelectionScheme(original, "Different");

        assertThat(copy.getMap().containsKey("id")).isFalse();
    }

    // ========================================
    // Profile Metadata Tests
    // ========================================

    @Test
    void should_ReturnProfileBegin_When_GetBeginCalled()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        String begin = scheme.getBegin();

        assertThat(begin).isEqualTo("handselection");
    }

    @Test
    void should_ReturnProfileDirName_When_GetProfileDirNameCalled()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        String dirName = scheme.getProfileDirName();

        assertThat(dirName).isEqualTo("handselection");
    }

    @Test
    void should_ReturnProfileFileList_When_GetProfileFileListCalled()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        List<?> list = scheme.getProfileFileList();

        assertThat(list).isNotNull();
    }

    // ========================================
    // Hand Group Management Tests
    // ========================================

    @Test
    void should_ReturnHandGroups_When_GetHandGroupsCalled()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        List<HandGroup> groups = scheme.getHandGroups();

        assertThat(groups).isNotNull().isEmpty();
    }

    @Test
    void should_AddEmptyGroup_When_EnsureEmptyGroupCalled()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        scheme.ensureEmptyGroup();

        assertThat(scheme.getHandGroups()).hasSize(1);
        assertThat(scheme.getHandGroups().get(0).getClassCount()).isZero();
    }

    @Test
    void should_RemoveEmptyGroups_When_RemoveEmptyGroupsCalled()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        // Add empty group
        scheme.getHandGroups().add(new HandGroup());
        // Add non-empty group
        HandGroup nonEmpty = new HandGroup();
        nonEmpty.setContainsPair(Card.ACE, true);
        scheme.getHandGroups().add(nonEmpty);
        // Add another empty group
        scheme.getHandGroups().add(new HandGroup());

        scheme.removeEmptyGroups();

        assertThat(scheme.getHandGroups()).hasSize(1);
        assertThat(scheme.getHandGroups().get(0).getClassCount()).isGreaterThan(0);
    }

    @Test
    void should_OnlyHaveOneEmptyGroup_When_EnsureEmptyGroupCalledMultipleTimes()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        scheme.ensureEmptyGroup();
        scheme.ensureEmptyGroup();
        scheme.ensureEmptyGroup();

        assertThat(scheme.getHandGroups()).hasSize(1);
    }

    // ========================================
    // Description Tests
    // ========================================

    @Test
    void should_ReturnEmptyString_When_NoDescriptionSet()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        String desc = scheme.getDescription();

        assertThat(desc).isEmpty();
    }

    @Test
    void should_ReturnDescription_When_DescriptionSet()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        scheme.setDescription("Test description");

        assertThat(scheme.getDescription()).isEqualTo("Test description");
    }

    @Test
    void should_UpdateDescription_When_DescriptionChanged()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        scheme.setDescription("First");
        scheme.setDescription("Second");

        assertThat(scheme.getDescription()).isEqualTo("Second");
    }

    // ========================================
    // Hand Strength Tests
    // ========================================

    @Test
    void should_ReturnZeroStrength_When_NoHandGroupsMatch()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");
        Card aceHearts = new Card(CardSuit.HEARTS, Card.ACE);
        Card aceSpades = new Card(CardSuit.SPADES, Card.ACE);

        float strength = scheme.getHandStrength(aceHearts, aceSpades);

        assertThat(strength).isZero();
    }

    @Test
    void should_ReturnCorrectStrength_When_HandGroupMatches()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        // Add a hand group with pocket aces at strength 10
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        group.setStrength(10);
        scheme.getHandGroups().add(group);

        Card aceHearts = new Card(CardSuit.HEARTS, Card.ACE);
        Card aceSpades = new Card(CardSuit.SPADES, Card.ACE);

        float strength = scheme.getHandStrength(aceHearts, aceSpades);

        assertThat(strength).isEqualTo(1.0f); // 10 / 10.0 = 1.0
    }

    @Test
    void should_ReturnHandStrengthFromHand_When_HandObjectProvided()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        // Add a hand group with pocket kings at strength 9
        HandGroup group = new HandGroup();
        group.setContainsPair(Card.KING, true);
        group.setStrength(9);
        scheme.getHandGroups().add(group);

        Hand hand = new Hand();
        hand.addCard(new Card(CardSuit.HEARTS, Card.KING));
        hand.addCard(new Card(CardSuit.SPADES, Card.KING));

        float strength = scheme.getHandStrength(hand);

        assertThat(strength).isEqualTo(0.9f); // 9 / 10.0 = 0.9
    }

    @Test
    void should_ReturnHighestStrength_When_MultipleGroupsMatch()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        // Add overlapping groups - higher strength group should be found last
        HandGroup broadGroup = new HandGroup();
        for (int i = Card.TWO; i <= Card.ACE; i++)
        {
            broadGroup.setContainsPair(i, true);
        }
        broadGroup.setStrength(5);
        scheme.getHandGroups().add(broadGroup);

        HandGroup specificGroup = new HandGroup();
        specificGroup.setContainsPair(Card.ACE, true);
        specificGroup.setStrength(10);
        scheme.getHandGroups().add(specificGroup);

        Card aceHearts = new Card(CardSuit.HEARTS, Card.ACE);
        Card aceSpades = new Card(CardSuit.SPADES, Card.ACE);

        float strength = scheme.getHandStrength(aceHearts, aceSpades);

        // Should return 1.0 from the specific group (processed last)
        assertThat(strength).isEqualTo(1.0f);
    }

    @Test
    void should_DistinguishSuitedVsOffsuit_When_CheckingHandStrength()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        // Add only suited AK
        HandGroup suitedGroup = new HandGroup();
        suitedGroup.setContainsSuited(Card.KING, Card.ACE, true);
        suitedGroup.setStrength(8);
        scheme.getHandGroups().add(suitedGroup);

        Card aceHearts = new Card(CardSuit.HEARTS, Card.ACE);
        Card kingHearts = new Card(CardSuit.HEARTS, Card.KING);
        Card kingSpades = new Card(CardSuit.SPADES, Card.KING);

        float suitedStrength = scheme.getHandStrength(aceHearts, kingHearts);
        float offsuitStrength = scheme.getHandStrength(aceHearts, kingSpades);

        assertThat(suitedStrength).isEqualTo(0.8f);
        assertThat(offsuitStrength).isZero();
    }

    // ========================================
    // Map Accessor Tests
    // ========================================

    @Test
    void should_ReturnMap_When_GetMapCalled()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        DMTypedHashMap map = scheme.getMap();

        assertThat(map).isNotNull();
    }

    @Test
    void should_PersistDataInMap_When_ValuesSet()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        scheme.getMap().setString("testKey", "testValue");

        assertThat(scheme.getMap().getString("testKey")).isEqualTo("testValue");
    }

    // ========================================
    // Write Tests
    // ========================================

    @Test
    void should_WriteScheme_When_WriteCalled() throws Exception
    {
        HandSelectionScheme scheme = new HandSelectionScheme("TestScheme");
        scheme.setDescription("Test description");

        StringWriter writer = new StringWriter();
        scheme.write(writer);

        String output = writer.toString();
        assertThat(output).contains("TestScheme");
        assertThat(output).contains("desc");
    }

    @Test
    void should_GenerateIDOnWrite_When_NoIDExists() throws Exception
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        StringWriter writer = new StringWriter();
        scheme.write(writer);

        assertThat(scheme.getMap().containsKey("id")).isTrue();
        assertThat(scheme.getMap().getLong("id")).isGreaterThan(0);
    }

    @Test
    void should_PreserveExistingID_When_WriteCalledMultipleTimes() throws Exception
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        scheme.write(new StringWriter());
        long firstID = scheme.getMap().getLong("id");

        Thread.sleep(10); // Ensure time passes
        scheme.write(new StringWriter());
        long secondID = scheme.getMap().getLong("id");

        assertThat(secondID).isEqualTo(firstID);
    }

    @Test
    void should_WriteHandGroups_When_HandGroupsExist() throws Exception
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        group.setStrength(10);
        scheme.getHandGroups().add(group);

        StringWriter writer = new StringWriter();
        scheme.write(writer);

        String output = writer.toString();
        assertThat(output).contains("hands0");
        assertThat(output).contains("|10");
    }

    @Test
    void should_NotWriteEmptyGroups_When_GroupHasNoClasses() throws Exception
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        scheme.getHandGroups().add(new HandGroup()); // Empty group

        StringWriter writer = new StringWriter();
        scheme.write(writer);

        String output = writer.toString();
        assertThat(output).doesNotContain("hands0");
    }

    // ========================================
    // toHTML Tests
    // ========================================

    @Test
    void should_GenerateHTML_When_ToHTMLCalled()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");
        scheme.setDescription("Test description");

        String html = scheme.toHTML();

        assertThat(html).isNotNull();
        assertThat(html).contains("Test description");
    }

    @Test
    void should_IncludeHandGroupsInHTML_When_HandGroupsExist()
    {
        HandSelectionScheme scheme = new HandSelectionScheme("Test");

        HandGroup group = new HandGroup();
        group.setContainsPair(Card.ACE, true);
        group.setStrength(10);
        scheme.getHandGroups().add(group);

        String html = scheme.toHTML();

        assertThat(html).isNotEmpty();
    }

    // ========================================
    // Static Helper Tests
    // ========================================

    @Test
    void should_ReturnNull_When_GetByNameCalledWithNonexistentName()
    {
        HandSelectionScheme result = HandSelectionScheme.getByName("NonexistentScheme");

        assertThat(result).isNull();
    }
}
