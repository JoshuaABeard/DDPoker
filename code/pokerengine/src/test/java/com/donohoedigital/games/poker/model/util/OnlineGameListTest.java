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
package com.donohoedigital.games.poker.model.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.donohoedigital.comms.DMArrayList;
import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.xml.SimpleXMLEncoder;

/**
 * Tests for OnlineGameList.
 */
public class OnlineGameListTest {

    private OnlineGameList list;

    @Before
    public void setUp() {
        list = new OnlineGameList();
    }

    @Test
    public void testGetAsDMListWithEmptyList() {
        DMArrayList<DMTypedHashMap> dmList = list.getAsDMList();

        assertNotNull(dmList);
        assertTrue(dmList.isEmpty());
    }

    @Test
    public void testGetAsDMListWithSingleGame() {
        OnlineGame game = new OnlineGame();
        game.setUrl("http://example.com/game1");
        game.setHostPlayer("Test Host");
        list.add(game);

        DMArrayList<DMTypedHashMap> dmList = list.getAsDMList();

        assertEquals(1, dmList.size());
        DMTypedHashMap data = dmList.get(0);
        assertEquals("http://example.com/game1", data.getString(OnlineGame.WAN_URL));
    }

    @Test
    public void testGetAsDMListWithMultipleGames() {
        OnlineGame game1 = new OnlineGame();
        game1.setUrl("http://example.com/game1");
        game1.setHostPlayer("Host 1");

        OnlineGame game2 = new OnlineGame();
        game2.setUrl("http://example.com/game2");
        game2.setHostPlayer("Host 2");

        list.add(game1);
        list.add(game2);

        DMArrayList<DMTypedHashMap> dmList = list.getAsDMList();

        assertEquals(2, dmList.size());
        assertEquals("http://example.com/game1", dmList.get(0).getString(OnlineGame.WAN_URL));
        assertEquals("http://example.com/game2", dmList.get(1).getString(OnlineGame.WAN_URL));
    }

    @Test
    public void testToStringWithEmptyList() {
        String result = list.toString();
        assertEquals(0, result.length());
    }

    @Test
    public void testToStringWithSingleGame() {
        OnlineGame game = new OnlineGame();
        game.setUrl("http://example.com/game1");
        game.setHostPlayer("Test Host");
        list.add(game);

        String result = list.toString();

        assertFalse(result.isEmpty());
        assertTrue(result.contains("Entry #0"));
        assertTrue(result.contains("http://example.com/game1"));
    }

    @Test
    public void testToStringWithMultipleGames() {
        OnlineGame game1 = new OnlineGame();
        game1.setUrl("http://example.com/game1");
        OnlineGame game2 = new OnlineGame();
        game2.setUrl("http://example.com/game2");

        list.add(game1);
        list.add(game2);

        String result = list.toString();

        assertFalse(result.isEmpty());
        assertTrue(result.contains("Entry #0"));
        assertTrue(result.contains("http://example.com/game1"));
        assertTrue(result.contains("Entry #1"));
        assertTrue(result.contains("http://example.com/game2"));
    }

    @Test
    public void testEncodeXMLWithEmptyList() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();

        list.encodeXML(encoder);

        String xml = encoder.toString();
        assertFalse(xml.isEmpty());
        assertTrue(xml.contains("<games"));
        assertTrue(xml.contains("</games>"));
    }

    @Test
    public void testEncodeXMLWithGames() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();

        OnlineGame game1 = new OnlineGame();
        game1.setUrl("http://example.com/game1");
        game1.setHostPlayer("Host 1");
        game1.setMode(OnlineGame.MODE_REG);
        game1.setTournament(new com.donohoedigital.games.poker.model.TournamentProfile("Tournament 1"));

        OnlineGame game2 = new OnlineGame();
        game2.setUrl("http://example.com/game2");
        game2.setHostPlayer("Host 2");
        game2.setMode(OnlineGame.MODE_PLAY);
        game2.setTournament(new com.donohoedigital.games.poker.model.TournamentProfile("Tournament 2"));

        list.add(game1);
        list.add(game2);

        list.encodeXML(encoder);

        String xml = encoder.toString();
        assertFalse(xml.isEmpty());
        assertTrue(xml.contains("<games"));
        assertTrue(xml.contains("</games>"));
    }
}
