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
package com.donohoedigital.games.poker.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.donohoedigital.comms.MsgState;
import com.donohoedigital.games.config.BaseProfile;
import com.donohoedigital.games.config.SaveDetails;

/**
 * Tests for PokerSaveDetails.
 */
public class PokerSaveDetailsTest {

    private PokerSaveDetails details;

    @Before
    public void setUp() {
        details = new PokerSaveDetails();
    }

    @Test
    public void testDefaultConstructor() {
        PokerSaveDetails newDetails = new PokerSaveDetails();
        assertNotNull(newDetails);
        assertEquals(PokerSaveDetails.NO_PLAYER, newDetails.getPlayerID());
        assertEquals(PokerSaveDetails.NO_OVERRIDE, newDetails.getOverrideState());
        assertEquals(SaveDetails.SAVE_ALL, newDetails.getSaveProfileData());
        assertEquals(SaveDetails.SAVE_ALL, newDetails.getSaveTables());
    }

    @Test
    public void testConstructorWithInitValue() {
        PokerSaveDetails newDetails = new PokerSaveDetails(5);
        assertNotNull(newDetails);
        assertEquals(5, newDetails.getSaveProfileData());
        assertEquals(5, newDetails.getSaveTables());
    }

    @Test
    public void testSetAndGetSaveProfileData() {
        details.setSaveProfileData(42);
        assertEquals(42, details.getSaveProfileData());
    }

    @Test
    public void testSetAndGetSaveTables() {
        details.setSaveTables(10);
        assertEquals(10, details.getSaveTables());
    }

    @Test
    public void testSetAndGetPlayerID() {
        details.setPlayerID(123);
        assertEquals(123, details.getPlayerID());
    }

    @Test
    public void testSetAndGetHideOthersCards() {
        details.setHideOthersCards(true);
        assertTrue(details.isHideOthersCards());

        details.setHideOthersCards(false);
        assertFalse(details.isHideOthersCards());
    }

    @Test
    public void testSetAndGetSetCurrentTableToLocal() {
        details.setSetCurrentTableToLocal(true);
        assertTrue(details.isSetCurrentTableToLocal());

        details.setSetCurrentTableToLocal(false);
        assertFalse(details.isSetCurrentTableToLocal());
    }

    @Test
    public void testSetAndGetOverrideState() {
        details.setOverrideState(5);
        assertEquals(5, details.getOverrideState());
    }

    @Test
    public void testSetAndGetOtherTableUpdate() {
        details.setOtherTableUpdate(true);
        assertTrue(details.isOtherTableUpdate());

        details.setOtherTableUpdate(false);
        assertFalse(details.isOtherTableUpdate());
    }

    @Test
    public void testSetAndGetRemovedTables() {
        int[] removed = new int[]{1, 2, 5};
        details.setRemovedTables(removed);

        assertSame(removed, details.getRemovedTables());
        assertEquals(3, details.getRemovedTables().length);
        assertEquals(1, details.getRemovedTables()[0]);
        assertEquals(2, details.getRemovedTables()[1]);
        assertEquals(5, details.getRemovedTables()[2]);
    }

    @Test
    public void testSetAndGetPlayerTypeProfiles() {
        List<BaseProfile> profiles = new ArrayList<BaseProfile>();
        details.setPlayerTypeProfiles(profiles);

        assertSame(profiles, details.getPlayerTypeProfiles());
        assertEquals(0, details.getPlayerTypeProfiles().size());
    }

    @Test
    public void testConstants() {
        assertEquals(-1, PokerSaveDetails.NO_OVERRIDE);
        assertEquals(-1, PokerSaveDetails.NO_PLAYER);
    }

    @Test
    public void testMarshalWithDefaultValues() {
        MsgState state = new MsgState();
        String result = details.marshal(state);

        assertNotNull(result);
    }

    @Test
    public void testMarshalWithAllFields() {
        details.setSaveProfileData(10);
        details.setSaveTables(5);
        details.setPlayerID(123);
        details.setHideOthersCards(true);
        details.setSetCurrentTableToLocal(false);
        details.setOverrideState(2);
        details.setOtherTableUpdate(true);
        details.setRemovedTables(new int[]{1, 2, 3});

        MsgState state = new MsgState();
        String result = details.marshal(state);

        assertNotNull(result);
    }

    @Test
    public void testDemarshalWithEmptyData() {
        MsgState state = new MsgState();
        String data = details.marshal(state);

        PokerSaveDetails newDetails = new PokerSaveDetails();
        newDetails.demarshal(state, data);

        assertEquals(details.getSaveProfileData(), newDetails.getSaveProfileData());
        assertEquals(details.getSaveTables(), newDetails.getSaveTables());
        assertEquals(details.getPlayerID(), newDetails.getPlayerID());
    }

    @Test
    public void testDemarshalWithAllFields() {
        details.setSaveProfileData(10);
        details.setSaveTables(5);
        details.setPlayerID(123);
        details.setHideOthersCards(true);
        details.setSetCurrentTableToLocal(false);
        details.setOverrideState(2);
        details.setOtherTableUpdate(true);
        details.setRemovedTables(new int[]{1, 3, 5});

        MsgState state = new MsgState();
        String data = details.marshal(state);

        PokerSaveDetails newDetails = new PokerSaveDetails();
        newDetails.demarshal(state, data);

        assertEquals(10, newDetails.getSaveProfileData());
        assertEquals(5, newDetails.getSaveTables());
        assertEquals(123, newDetails.getPlayerID());
        assertTrue(newDetails.isHideOthersCards());
        assertFalse(newDetails.isSetCurrentTableToLocal());
        assertEquals(2, newDetails.getOverrideState());
        assertTrue(newDetails.isOtherTableUpdate());
        assertNotNull(newDetails.getRemovedTables());
        assertEquals(3, newDetails.getRemovedTables().length);
        assertEquals(1, newDetails.getRemovedTables()[0]);
        assertEquals(3, newDetails.getRemovedTables()[1]);
        assertEquals(5, newDetails.getRemovedTables()[2]);
    }

    @Test
    public void testRemovedTablesNull() {
        details.setRemovedTables(null);
        assertNull(details.getRemovedTables());
    }

    @Test
    public void testRemovedTablesEmpty() {
        details.setRemovedTables(new int[0]);
        assertNotNull(details.getRemovedTables());
        assertEquals(0, details.getRemovedTables().length);
    }

    @Test
    public void testPlayerTypeProfilesNull() {
        details.setPlayerTypeProfiles(null);
        assertNull(details.getPlayerTypeProfiles());
    }

    @Test
    public void testPlayerTypeProfilesEmpty() {
        List<BaseProfile> profiles = new ArrayList<BaseProfile>();
        details.setPlayerTypeProfiles(profiles);

        assertNotNull(details.getPlayerTypeProfiles());
        assertEquals(0, details.getPlayerTypeProfiles().size());
    }

    @Test
    public void testMarshalDemarshalRoundTrip() {
        details.setSaveProfileData(7);
        details.setSaveTables(3);
        details.setPlayerID(456);
        details.setHideOthersCards(false);
        details.setSetCurrentTableToLocal(true);
        details.setOverrideState(1);
        details.setOtherTableUpdate(false);
        details.setRemovedTables(new int[]{2, 4, 6, 8});

        MsgState state = new MsgState();
        String data = details.marshal(state);

        PokerSaveDetails newDetails = new PokerSaveDetails();
        newDetails.demarshal(state, data);

        assertEquals(details.getSaveProfileData(), newDetails.getSaveProfileData());
        assertEquals(details.getSaveTables(), newDetails.getSaveTables());
        assertEquals(details.getPlayerID(), newDetails.getPlayerID());
        assertEquals(details.isHideOthersCards(), newDetails.isHideOthersCards());
        assertEquals(details.isSetCurrentTableToLocal(), newDetails.isSetCurrentTableToLocal());
        assertEquals(details.getOverrideState(), newDetails.getOverrideState());
        assertEquals(details.isOtherTableUpdate(), newDetails.isOtherTableUpdate());

        assertNotNull(newDetails.getRemovedTables());
        assertEquals(4, newDetails.getRemovedTables().length);
        for (int i = 0; i < 4; i++) {
            assertEquals(details.getRemovedTables()[i], newDetails.getRemovedTables()[i]);
        }
    }

    @Test
    public void testMarshalWithNullRemovedTables() {
        details.setSaveProfileData(1);
        details.setSaveTables(1);
        details.setRemovedTables(null);

        MsgState state = new MsgState();
        String result = details.marshal(state);

        assertNotNull(result);

        PokerSaveDetails newDetails = new PokerSaveDetails();
        newDetails.demarshal(state, result);

        assertNull(newDetails.getRemovedTables());
    }
}
