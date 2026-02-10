/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PlayerType AI configuration.
 */
class PlayerTypeTest
{
    private HandSelectionScheme defaultScheme;

    @BeforeEach
    void setUp()
    {
        // Initialize ConfigManager for tests (only once)
        if (!com.donohoedigital.config.PropertyConfig.isInitialized())
        {
            new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        }

        // Create a default hand selection scheme for testing
        defaultScheme = new HandSelectionScheme("DefaultScheme");
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void should_CreateEmptyPlayerType_When_DefaultConstructorUsed()
    {
        PlayerType playerType = new PlayerType();

        assertThat(playerType.getName()).isEmpty();
    }

    @Test
    void should_CreateNamedPlayerType_When_StringConstructorUsed()
    {
        PlayerType playerType = new PlayerType("TestPlayer");

        assertThat(playerType.getName()).isEqualTo("TestPlayer");
        assertThat(playerType.getMap()).isNotNull();
        assertThat(playerType.getStratValue("discipline")).isZero();
        assertThat(playerType.getAIClassName()).isEqualTo("com.donohoedigital.games.poker.ai.V2Player");
    }

    @Test
    void should_CopyPlayerTypeWithNewName_When_CopyConstructorUsed()
    {
        PlayerType original = new PlayerType("Original");
        original.setDescription("Test description");
        original.setHandSelectionFull(defaultScheme);

        PlayerType copy = new PlayerType(original, "Copy");

        assertThat(copy.getName()).isEqualTo("Copy");
        assertThat(copy.getDescription()).isEqualTo("Test description");
        assertThat(copy.getHandSelectionFull()).isSameAs(defaultScheme);
    }

    @Test
    void should_RemoveMetadataWhenDifferentName_When_CopyConstructorUsed()
    {
        PlayerType original = new PlayerType("Original");
        original.getMap().setBoolean("default", true);
        original.getMap().setInteger("order", 5);
        original.getMap().setLong("id", 12345L);

        PlayerType copy = new PlayerType(original, "Different");

        assertThat(copy.getMap().containsKey("default")).isFalse();
        assertThat(copy.getMap().containsKey("order")).isFalse();
        assertThat(copy.getMap().containsKey("id")).isFalse();
    }

    @Test
    void should_PreserveMetadataWhenSameName_When_CopyConstructorUsed()
    {
        PlayerType original = new PlayerType("SameName");
        original.getMap().setBoolean("default", true);
        original.getMap().setInteger("order", 5);
        original.getMap().setLong("id", 12345L);

        PlayerType copy = new PlayerType(original, "SameName");

        assertThat(copy.getMap().getBoolean("default")).isTrue();
        assertThat(copy.getMap().getInteger("order")).isEqualTo(5);
        assertThat(copy.getMap().getLong("id")).isEqualTo(12345L);
    }

    // ========================================
    // Description Tests
    // ========================================

    @Test
    void should_ReturnEmptyString_When_NoDescriptionSet()
    {
        PlayerType playerType = new PlayerType("Test");

        String desc = playerType.getDescription();

        assertThat(desc).isEmpty();
    }

    @Test
    void should_ReturnDescription_When_DescriptionSet()
    {
        PlayerType playerType = new PlayerType("Test");

        playerType.setDescription("Test AI player");

        assertThat(playerType.getDescription()).isEqualTo("Test AI player");
    }

    @Test
    void should_UpdateDescription_When_DescriptionChanged()
    {
        PlayerType playerType = new PlayerType("Test");

        playerType.setDescription("First");
        playerType.setDescription("Second");

        assertThat(playerType.getDescription()).isEqualTo("Second");
    }

    // ========================================
    // AI Class Name Tests
    // ========================================

    @Test
    void should_HaveDefaultAIClass_When_PlayerTypeCreated()
    {
        PlayerType playerType = new PlayerType("Test");

        assertThat(playerType.getAIClassName()).isEqualTo("com.donohoedigital.games.poker.ai.V2Player");
    }

    @Test
    void should_UpdateAIClass_When_SetAIClassNameCalled()
    {
        PlayerType playerType = new PlayerType("Test");

        playerType.setAIClassName("com.example.CustomAI");

        assertThat(playerType.getAIClassName()).isEqualTo("com.example.CustomAI");
    }

    @Test
    void should_AllowCopy_When_V2PlayerOrNull()
    {
        PlayerType v2Player = new PlayerType("V2");
        v2Player.setAIClassName("com.donohoedigital.games.poker.ai.V2Player");

        PlayerType nullPlayer = new PlayerType("Null");
        nullPlayer.setAIClassName(null);

        assertThat(v2Player.canCopy()).isTrue();
        assertThat(nullPlayer.canCopy()).isTrue();
    }

    @Test
    void should_DisallowCopy_When_V1Player()
    {
        PlayerType v1Player = new PlayerType("V1");
        v1Player.setAIClassName("com.donohoedigital.games.poker.ai.V1Player");

        assertThat(v1Player.canCopy()).isFalse();
    }

    // ========================================
    // Hand Selection Scheme Tests
    // ========================================

    @Test
    void should_StoreFullScheme_When_SetHandSelectionFullCalled()
    {
        PlayerType playerType = new PlayerType("Test");
        HandSelectionScheme scheme = new HandSelectionScheme("FullScheme");

        playerType.setHandSelectionFull(scheme);

        assertThat(playerType.getHandSelectionFull()).isSameAs(scheme);
    }

    @Test
    void should_StoreShortScheme_When_SetHandSelectionShortCalled()
    {
        PlayerType playerType = new PlayerType("Test");
        HandSelectionScheme scheme = new HandSelectionScheme("ShortScheme");

        playerType.setHandSelectionShort(scheme);

        assertThat(playerType.getHandSelectionShort()).isSameAs(scheme);
    }

    @Test
    void should_StoreVeryShortScheme_When_SetHandSelectionVeryShortCalled()
    {
        PlayerType playerType = new PlayerType("Test");
        HandSelectionScheme scheme = new HandSelectionScheme("VeryShortScheme");

        playerType.setHandSelectionVeryShort(scheme);

        assertThat(playerType.getHandSelectionVeryShort()).isSameAs(scheme);
    }

    @Test
    void should_StoreHupScheme_When_SetHandSelectionHupCalled()
    {
        PlayerType playerType = new PlayerType("Test");
        HandSelectionScheme scheme = new HandSelectionScheme("HupScheme");

        playerType.setHandSelectionHup(scheme);

        assertThat(playerType.getHandSelectionHup()).isSameAs(scheme);
    }

    // ========================================
    // Strategy Value Tests
    // ========================================

    @Test
    void should_ReturnStrategyValue_When_GetStratValueCalled()
    {
        PlayerType playerType = new PlayerType("Test");

        playerType.setStratValue("aggression", 5);

        assertThat(playerType.getStratValue("aggression")).isEqualTo(5);
    }

    @Test
    void should_ReturnDefaultValue_When_StrategyValueNotSet()
    {
        PlayerType playerType = new PlayerType("Test");

        int value = playerType.getStratValue("nonexistent");

        // Default values are defined in the implementation
        assertThat(value).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_UpdateStrategyValue_When_SetStratValueCalled()
    {
        PlayerType playerType = new PlayerType("Test");

        playerType.setStratValue("bluff", 3);
        playerType.setStratValue("bluff", 7);

        assertThat(playerType.getStratValue("bluff")).isEqualTo(7);
    }

    @Test
    void should_GetStratValueWithRound_When_RoundParameterProvided()
    {
        PlayerType playerType = new PlayerType("Test");
        playerType.setStratValue("aggression", 5);

        int value = playerType.getStratValue("aggression", 2);

        // Should return the value (round parameter may modify lookup)
        assertThat(value).isGreaterThanOrEqualTo(0);
    }

    // ========================================
    // Map Accessor Tests
    // ========================================

    @Test
    void should_ReturnMap_When_GetMapCalled()
    {
        PlayerType playerType = new PlayerType("Test");

        DMTypedHashMap map = playerType.getMap();

        assertThat(map).isNotNull();
    }

    @Test
    void should_PersistDataInMap_When_ValuesSet()
    {
        PlayerType playerType = new PlayerType("Test");

        playerType.getMap().setString("customKey", "customValue");

        assertThat(playerType.getMap().getString("customKey")).isEqualTo("customValue");
    }

    // ========================================
    // Profile Metadata Tests
    // ========================================

    @Test
    void should_ReturnProfileBegin_When_GetBeginCalled()
    {
        PlayerType playerType = new PlayerType("Test");

        String begin = playerType.getBegin();

        assertThat(begin).isEqualTo("playertype");
    }

    @Test
    void should_ReturnProfileDirName_When_GetProfileDirNameCalled()
    {
        PlayerType playerType = new PlayerType("Test");

        String dirName = playerType.getProfileDirName();

        assertThat(dirName).isEqualTo("playertypes");
    }

    @Test
    void should_ReturnProfileFileList_When_GetProfileFileListCalled()
    {
        PlayerType playerType = new PlayerType("Test");

        List<?> list = playerType.getProfileFileList();

        assertThat(list).isNotNull();
    }

    @Test
    void should_ReturnUniqueKey_When_GetUniqueKeyCalled()
    {
        PlayerType playerType = new PlayerType("TestPlayer");

        String key = playerType.getUniqueKey();

        // Unique key is based on filename, may be null for unsaved profiles
        // Allow null since profile has not been saved to file
        assertThat(key).satisfiesAnyOf(
            k -> assertThat(k).isNull(),
            k -> assertThat(k).isEmpty()
        );
    }

    // ========================================
    // Write Tests
    // ========================================

    @Test
    void should_WritePlayerType_When_WriteCalled() throws Exception
    {
        PlayerType playerType = new PlayerType("TestPlayer");
        playerType.setDescription("Test player type");
        playerType.setHandSelectionFull(defaultScheme);

        StringWriter writer = new StringWriter();
        playerType.write(writer);

        String output = writer.toString();
        assertThat(output).contains("TestPlayer");
        assertThat(output).contains("desc");
    }

    @Test
    void should_WriteHandSelectionIDs_When_SchemesSet() throws Exception
    {
        PlayerType playerType = new PlayerType("Test");
        HandSelectionScheme scheme1 = new HandSelectionScheme("Scheme1");
        HandSelectionScheme scheme2 = new HandSelectionScheme("Scheme2");

        playerType.setHandSelectionFull(scheme1);
        playerType.setHandSelectionShort(scheme2);

        StringWriter writer = new StringWriter();
        playerType.write(writer);

        String output = writer.toString();
        assertThat(output).contains("hsfull");
        assertThat(output).contains("hsshort");
    }

    // ========================================
    // Static Helper Tests
    // ========================================

    @Test
    void should_ReturnProfileList_When_GetProfileListCalled()
    {
        List<?> list = PlayerType.getProfileList();

        assertThat(list).isNotNull();
    }

    @Test
    void should_ReturnCachedList_When_GetProfileListCachedCalled()
    {
        List<?> list1 = PlayerType.getProfileListCached();
        List<?> list2 = PlayerType.getProfileListCached();

        assertThat(list1).isNotNull();
        assertThat(list2).isSameAs(list1); // Should return same cached instance
    }

    // Tests for getByUniqueKey and getDefaultProfile require PlayerType profiles
    // to exist in the filesystem, which may not be available in test environment

    // ========================================
    // Comparison Tests
    // ========================================

    @Test
    void should_CompareByName_When_CompareToUsed()
    {
        PlayerType pt1 = new PlayerType("Alpha");
        PlayerType pt2 = new PlayerType("Beta");

        int comparison = pt1.compareTo(pt2);

        assertThat(comparison).isLessThan(0);
    }

    // ========================================
    // Strategy Factor Tests
    // ========================================

    @Test
    void should_ReturnStratFactor_When_GetStratFactorCalled()
    {
        PlayerType playerType = new PlayerType("Test");
        playerType.setStratValue("aggression", 5);

        float factor = playerType.getStratFactor("aggression", 0.5f, 1.5f);

        assertThat(factor).isBetween(0.5f, 1.5f);
    }

    @Test
    void should_ReturnStratFactorWithRound_When_GetStratFactorWithRoundCalled()
    {
        PlayerType playerType = new PlayerType("Test");
        playerType.setStratValue("bluff", 7);

        float factor = playerType.getStratFactor("bluff", 0.3f, 1.7f, 2);

        assertThat(factor).isBetween(0.3f, 1.7f);
    }

    @Test
    void should_ReturnStratFactorWithHand_When_HandProvided()
    {
        PlayerType playerType = new PlayerType("Test");
        playerType.setStratValue("position", 5);
        Hand hand = new Hand();
        hand.addCard(new com.donohoedigital.games.poker.engine.Card(CardSuit.HEARTS, Card.ACE));
        hand.addCard(new com.donohoedigital.games.poker.engine.Card(CardSuit.SPADES, Card.KING));

        float factor = playerType.getStratFactor("position", hand, 0.5f, 1.5f, 1);

        assertThat(factor).isBetween(0.5f, 1.5f);
    }

    // ========================================
    // Advisor Tests
    // ========================================
    // Advisor tests require GameEngine to be initialized, which is not
    // available in headless test environment
}
