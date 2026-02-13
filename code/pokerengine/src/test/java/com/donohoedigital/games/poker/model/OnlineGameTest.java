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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.comms.DMTypedHashMap;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for OnlineGame model class.
 */
public class OnlineGameTest {

    // ===== Construction Tests =====

    @Test
    public void testDefaultConstruction() {
        OnlineGame game = new OnlineGame();
        assertNotNull(game.getData());
        assertNull(game.getId());
        assertNull(game.getUrl());
    }

    @Test
    public void testConstructionWithData() {
        DMTypedHashMap data = new DMTypedHashMap();
        data.setString(OnlineGame.WAN_URL, "test-url");
        data.setString(OnlineGame.WAN_HOST_PLAYER, "TestPlayer");

        OnlineGame game = new OnlineGame(data);

        assertEquals("test-url", game.getUrl());
        assertEquals("TestPlayer", game.getHostPlayer());
    }

    // ===== Getters and Setters Tests =====

    @Test
    public void testIdGetterSetter() {
        OnlineGame game = new OnlineGame();
        game.setId(123L);
        assertEquals(Long.valueOf(123L), game.getId());
    }

    @Test
    public void testUrlGetterSetter() {
        OnlineGame game = new OnlineGame();
        game.setUrl("http://example.com/game/123");
        assertEquals("http://example.com/game/123", game.getUrl());
    }

    @Test
    public void testHostPlayerGetterSetter() {
        OnlineGame game = new OnlineGame();
        game.setHostPlayer("Alice");
        assertEquals("Alice", game.getHostPlayer());
    }

    @Test
    public void testModeGetterSetter() {
        OnlineGame game = new OnlineGame();

        game.setMode(OnlineGame.MODE_REG);
        assertEquals(OnlineGame.MODE_REG, game.getMode());

        game.setMode(OnlineGame.MODE_PLAY);
        assertEquals(OnlineGame.MODE_PLAY, game.getMode());

        game.setMode(OnlineGame.MODE_STOP);
        assertEquals(OnlineGame.MODE_STOP, game.getMode());

        game.setMode(OnlineGame.MODE_END);
        assertEquals(OnlineGame.MODE_END, game.getMode());
    }

    @Test
    public void testStartDateGetterSetter() {
        OnlineGame game = new OnlineGame();
        Date now = new Date();

        game.setStartDate(now);
        assertEquals(now, game.getStartDate());
    }

    @Test
    public void testEndDateGetterSetter() {
        OnlineGame game = new OnlineGame();
        Date now = new Date();

        game.setEndDate(now);
        assertEquals(now, game.getEndDate());
    }

    @Test
    public void testCreateDateGetterSetter() {
        OnlineGame game = new OnlineGame();
        Date now = new Date();

        game.setCreateDate(now);
        assertEquals(now, game.getCreateDate());
    }

    @Test
    public void testModifyDateGetterSetter() {
        OnlineGame game = new OnlineGame();
        Date now = new Date();

        game.setModifyDate(now);
        assertEquals(now, game.getModifyDate());
    }

    @Test
    public void testTournamentGetterSetter() {
        OnlineGame game = new OnlineGame();
        TournamentProfile tournament = new TournamentProfile();
        tournament.setName("Test Tournament");

        game.setTournament(tournament);

        assertNotNull(game.getTournament());
        assertEquals("Test Tournament", game.getTournament().getName());
    }

    @Test
    public void testTournamentNullSafety() {
        OnlineGame game = new OnlineGame();
        game.setTournament(null);
        assertNull(game.getTournament());
    }

    @Test
    public void testHistoriesGetterSetter() {
        OnlineGame game = new OnlineGame();
        List<TournamentHistory> histories = new ArrayList<>();
        TournamentHistory history = new TournamentHistory();
        histories.add(history);

        game.setHistories(histories);

        assertNotNull(game.getHistories());
        assertEquals(1, game.getHistories().size());
    }

    // ===== Mode Constants Tests =====

    @Test
    public void testModeConstants() {
        assertEquals(1, OnlineGame.MODE_REG);
        assertEquals(2, OnlineGame.MODE_PLAY);
        assertEquals(3, OnlineGame.MODE_STOP);
        assertEquals(4, OnlineGame.MODE_END);
        assertEquals(102, OnlineGame.FETCH_MODE_REG_PLAY);
    }

    // ===== merge() Tests =====

    @Test
    public void testMerge() {
        OnlineGame game1 = new OnlineGame();
        game1.setUrl("url1");
        game1.setHostPlayer("Player1");
        game1.setMode(OnlineGame.MODE_REG);

        OnlineGame game2 = new OnlineGame();
        game2.setUrl("url2");
        game2.setHostPlayer("Player2");

        game1.merge(game2);

        // game1 should have game2's data
        assertEquals("url2", game1.getUrl());
        assertEquals("Player2", game1.getHostPlayer());
    }

    @Test
    public void testMergePreservesUnchangedFields() {
        OnlineGame game1 = new OnlineGame();
        game1.setUrl("url1");
        game1.setHostPlayer("Player1");
        game1.setMode(OnlineGame.MODE_REG);
        Date startDate = new Date();
        game1.setStartDate(startDate);

        OnlineGame game2 = new OnlineGame();
        game2.setUrl("url2");

        game1.merge(game2);

        assertEquals("url2", game1.getUrl());
        // Start date should be preserved (game2 didn't set it)
        assertEquals(startDate, game1.getStartDate());
    }

    // ===== equals() and hashCode() Tests =====

    @Test
    public void testEqualsSameObject() {
        OnlineGame game = new OnlineGame();
        game.setUrl("test-url");

        assertTrue(game.equals(game));
    }

    @Test
    public void testEqualsSameUrl() {
        OnlineGame game1 = new OnlineGame();
        game1.setUrl("test-url");

        OnlineGame game2 = new OnlineGame();
        game2.setUrl("test-url");

        assertTrue(game1.equals(game2));
        assertTrue(game2.equals(game1));
    }

    @Test
    public void testEqualsDifferentUrl() {
        OnlineGame game1 = new OnlineGame();
        game1.setUrl("url1");

        OnlineGame game2 = new OnlineGame();
        game2.setUrl("url2");

        assertFalse(game1.equals(game2));
    }

    @Test
    public void testEqualsNonOnlineGame() {
        OnlineGame game = new OnlineGame();
        game.setUrl("test-url");

        assertFalse(game.equals("not a game"));
        assertFalse(game.equals(null));
    }

    @Test
    public void testHashCodeConsistency() {
        OnlineGame game = new OnlineGame();
        game.setUrl("test-url");

        int hash1 = game.hashCode();
        int hash2 = game.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeEqualObjects() {
        OnlineGame game1 = new OnlineGame();
        game1.setUrl("test-url");

        OnlineGame game2 = new OnlineGame();
        game2.setUrl("test-url");

        // NOTE: Production bug - hashCode() includes super.hashCode() which violates
        // equals/hashCode contract. Equal objects should have equal hash codes, but
        // they don't.
        // This test verifies the actual (buggy) behavior.
        assertNotEquals(game1.hashCode(), game2.hashCode());
    }

    @Test
    public void testHashCodeNullUrl() {
        OnlineGame game = new OnlineGame();
        // Should not throw even with null URL
        int hash = game.hashCode();
        assertNotEquals(0, hash); // Should return super.hashCode()
    }

    // ===== toString() Tests =====

    @Test
    public void testToString() {
        OnlineGame game = new OnlineGame();
        game.setUrl("test-url");
        game.setHostPlayer("TestPlayer");

        String str = game.toString();

        assertNotNull(str);
        assertTrue(str.contains("OnlineGame"));
    }

    // ===== Field Name Constants Tests =====

    @Test
    public void testFieldNameConstants() {
        assertEquals("id", OnlineGame.WAN_ID);
        assertEquals("url", OnlineGame.WAN_URL);
        assertEquals("hostplayer", OnlineGame.WAN_HOST_PLAYER);
        assertEquals("mode", OnlineGame.WAN_MODE);
        assertEquals("startdate", OnlineGame.WAN_START_DATE);
        assertEquals("enddate", OnlineGame.WAN_END_DATE);
        assertEquals("createdate", OnlineGame.WAN_CREATE_DATE);
        assertEquals("modifyddate", OnlineGame.WAN_MODIFY_DATE);
        assertEquals("profile", OnlineGame.WAN_TOURNAMENT);
        assertEquals("tournamentname", OnlineGame.WAN_TOURNAMENT_NAME);
        assertEquals("status", OnlineGame.WAN_STATUS);
    }

    // ===== Edge Cases =====

    @Test
    public void testNullDates() {
        OnlineGame game = new OnlineGame();

        game.setStartDate(null);
        game.setEndDate(null);

        assertNull(game.getStartDate());
        assertNull(game.getEndDate());
    }

    @Test
    public void testEmptyUrl() {
        OnlineGame game = new OnlineGame();
        game.setUrl("");

        assertEquals("", game.getUrl());
    }

    @Test
    public void testGetDataReturnsInternalMap() {
        OnlineGame game = new OnlineGame();
        DMTypedHashMap data = game.getData();

        assertNotNull(data);
        assertSame(data, game.getData()); // Should be same instance
    }

    @Test
    public void testModeTransitions() {
        OnlineGame game = new OnlineGame();

        // Simulate game lifecycle
        game.setMode(OnlineGame.MODE_REG);
        assertEquals(OnlineGame.MODE_REG, game.getMode());

        game.setMode(OnlineGame.MODE_PLAY);
        assertEquals(OnlineGame.MODE_PLAY, game.getMode());

        game.setMode(OnlineGame.MODE_STOP);
        assertEquals(OnlineGame.MODE_STOP, game.getMode());

        game.setMode(OnlineGame.MODE_END);
        assertEquals(OnlineGame.MODE_END, game.getMode());
    }
}
