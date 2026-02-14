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
package com.donohoedigital.games.config;

import com.donohoedigital.comms.MsgState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SaveDetails - Save strategy configuration for game state
 * persistence.
 */
class SaveDetailsTest {

    // ========== Constructor Tests ==========

    @Test
    void should_InitializeWithSaveAll_When_DefaultConstructorUsed() {
        SaveDetails details = new SaveDetails();

        assertThat(details.getSaveGameHashData()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(details.getSaveGameSubclassData()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(details.getSavePlayers()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(details.getSaveAI()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(details.getSaveObservers()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(details.getSaveCurrentPhase()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(details.getSaveTerritories()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(details.getSaveCustom()).isEqualTo(SaveDetails.SAVE_ALL);
    }

    @Test
    void should_InitializeWithGivenValue_When_ValueConstructorUsed() {
        SaveDetails details = new SaveDetails(SaveDetails.SAVE_DIRTY);

        assertThat(details.getSaveGameHashData()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(details.getSaveGameSubclassData()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(details.getSavePlayers()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(details.getSaveAI()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(details.getSaveObservers()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(details.getSaveCurrentPhase()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(details.getSaveTerritories()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(details.getSaveCustom()).isEqualTo(SaveDetails.SAVE_DIRTY);
    }

    @Test
    void should_InitializeWithSaveNone_When_SaveNoneValueProvided() {
        SaveDetails details = new SaveDetails(SaveDetails.SAVE_NONE);

        assertThat(details.getSaveGameHashData()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(details.getSaveGameSubclassData()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(details.getSavePlayers()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(details.getSaveAI()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(details.getSaveObservers()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(details.getSaveCurrentPhase()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(details.getSaveTerritories()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(details.getSaveCustom()).isEqualTo(SaveDetails.SAVE_NONE);
    }

    // ========== Constants Tests ==========

    @Test
    void should_HaveCorrectSaveConstants_When_Accessed() {
        assertThat(SaveDetails.SAVE_ALL).isEqualTo(1);
        assertThat(SaveDetails.SAVE_DIRTY).isEqualTo(2);
        assertThat(SaveDetails.SAVE_NONE).isEqualTo(3);
    }

    @Test
    void should_HaveCorrectTerritoryConstants_When_Accessed() {
        assertThat(SaveDetails.TERRITORY_OWNER_UNITS).isEqualTo(1);
        assertThat(SaveDetails.TERRITORY_ALL_UNITS).isEqualTo(2);
    }

    // ========== Getter/Setter Tests ==========

    @Test
    void should_UpdateValue_When_SetSaveGameHashData() {
        SaveDetails details = new SaveDetails();

        details.setSaveGameHashData(SaveDetails.SAVE_DIRTY);

        assertThat(details.getSaveGameHashData()).isEqualTo(SaveDetails.SAVE_DIRTY);
    }

    @Test
    void should_UpdateValue_When_SetSaveGameSubclassData() {
        SaveDetails details = new SaveDetails();

        details.setSaveGameSubclassData(SaveDetails.SAVE_NONE);

        assertThat(details.getSaveGameSubclassData()).isEqualTo(SaveDetails.SAVE_NONE);
    }

    @Test
    void should_UpdateValue_When_SetSavePlayers() {
        SaveDetails details = new SaveDetails();

        details.setSavePlayers(SaveDetails.SAVE_DIRTY);

        assertThat(details.getSavePlayers()).isEqualTo(SaveDetails.SAVE_DIRTY);
    }

    @Test
    void should_UpdateValue_When_SetSaveAI() {
        SaveDetails details = new SaveDetails();

        details.setSaveAI(SaveDetails.SAVE_NONE);

        assertThat(details.getSaveAI()).isEqualTo(SaveDetails.SAVE_NONE);
    }

    @Test
    void should_UpdateValue_When_SetSaveObservers() {
        SaveDetails details = new SaveDetails();

        details.setSaveObservers(SaveDetails.SAVE_DIRTY);

        assertThat(details.getSaveObservers()).isEqualTo(SaveDetails.SAVE_DIRTY);
    }

    @Test
    void should_UpdateValue_When_SetSaveCurrentPhase() {
        SaveDetails details = new SaveDetails();

        details.setSaveCurrentPhase(SaveDetails.SAVE_NONE);

        assertThat(details.getSaveCurrentPhase()).isEqualTo(SaveDetails.SAVE_NONE);
    }

    @Test
    void should_UpdateValue_When_SetSaveTerritories() {
        SaveDetails details = new SaveDetails();

        details.setSaveTerritories(SaveDetails.SAVE_DIRTY);

        assertThat(details.getSaveTerritories()).isEqualTo(SaveDetails.SAVE_DIRTY);
    }

    @Test
    void should_UpdateValue_When_SetTerritoriesDirtyType() {
        SaveDetails details = new SaveDetails();

        details.setTerritoriesDirtyType(SaveDetails.TERRITORY_ALL_UNITS);

        assertThat(details.getTerritoriesDirtyType()).isEqualTo(SaveDetails.TERRITORY_ALL_UNITS);
    }

    @Test
    void should_UpdateValue_When_SetTerritoriesUnitOwnerID() {
        SaveDetails details = new SaveDetails();

        details.setTerritoriesUnitOwnerID(42);

        assertThat(details.getTerritoriesUnitOwnerID()).isEqualTo(42);
    }

    @Test
    void should_UpdateValue_When_SetSaveCustom() {
        SaveDetails details = new SaveDetails();

        details.setSaveCustom(SaveDetails.SAVE_NONE);

        assertThat(details.getSaveCustom()).isEqualTo(SaveDetails.SAVE_NONE);
    }

    // ========== Custom Info Tests ==========

    @Test
    void should_ReturnNull_When_CustomInfoNotSet() {
        SaveDetails details = new SaveDetails();

        assertThat(details.getCustomInfo()).isNull();
    }

    @Test
    void should_ReturnCustomInfo_When_CustomInfoSet() {
        SaveDetails details = new SaveDetails();
        SaveDetails customInfo = new SaveDetails(SaveDetails.SAVE_DIRTY);

        details.setCustomInfo(customInfo);

        assertThat(details.getCustomInfo()).isSameAs(customInfo);
    }

    // ========== Serialization Tests ==========

    @Test
    void should_RoundTripCorrectly_When_MarshalAndDemarshal() {
        SaveDetails original = new SaveDetails();
        original.setSaveGameHashData(SaveDetails.SAVE_DIRTY);
        original.setSaveGameSubclassData(SaveDetails.SAVE_NONE);
        original.setSavePlayers(SaveDetails.SAVE_ALL);
        original.setSaveAI(SaveDetails.SAVE_DIRTY);
        original.setSaveObservers(SaveDetails.SAVE_NONE);
        original.setSaveCurrentPhase(SaveDetails.SAVE_ALL);
        original.setSaveTerritories(SaveDetails.SAVE_DIRTY);
        original.setTerritoriesDirtyType(SaveDetails.TERRITORY_ALL_UNITS);
        original.setTerritoriesUnitOwnerID(123);

        MsgState state = new MsgState();
        String marshalled = original.marshal(state);
        SaveDetails restored = new SaveDetails();
        restored.demarshal(state, marshalled);

        assertThat(restored.getSaveGameHashData()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(restored.getSaveGameSubclassData()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(restored.getSavePlayers()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(restored.getSaveAI()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(restored.getSaveObservers()).isEqualTo(SaveDetails.SAVE_NONE);
        assertThat(restored.getSaveCurrentPhase()).isEqualTo(SaveDetails.SAVE_ALL);
        assertThat(restored.getSaveTerritories()).isEqualTo(SaveDetails.SAVE_DIRTY);
        assertThat(restored.getTerritoriesDirtyType()).isEqualTo(SaveDetails.TERRITORY_ALL_UNITS);
        assertThat(restored.getTerritoriesUnitOwnerID()).isEqualTo(123);
    }

    @Test
    void should_PreserveCustomInfo_When_MarshalAndDemarshal() {
        SaveDetails original = new SaveDetails();
        SaveDetails customInfo = new SaveDetails(SaveDetails.SAVE_DIRTY);
        original.setCustomInfo(customInfo);

        MsgState state = new MsgState();
        String marshalled = original.marshal(state);
        SaveDetails restored = new SaveDetails();
        restored.demarshal(state, marshalled);

        assertThat(restored.getCustomInfo()).isNotNull();
        assertThat(restored.getCustomInfo()).isInstanceOf(SaveDetails.class);
        SaveDetails restoredCustom = (SaveDetails) restored.getCustomInfo();
        assertThat(restoredCustom.getSaveGameHashData()).isEqualTo(SaveDetails.SAVE_DIRTY);
    }

    @Test
    void should_HandleNullCustomInfo_When_MarshalAndDemarshal() {
        SaveDetails original = new SaveDetails();
        original.setCustomInfo(null);

        MsgState state = new MsgState();
        String marshalled = original.marshal(state);
        SaveDetails restored = new SaveDetails();
        restored.demarshal(state, marshalled);

        assertThat(restored.getCustomInfo()).isNull();
    }
}
