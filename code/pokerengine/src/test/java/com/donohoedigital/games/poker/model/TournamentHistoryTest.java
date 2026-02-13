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

import com.donohoedigital.comms.MsgState;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Tests for TournamentHistory model class.
 */
public class TournamentHistoryTest {

    // ===== Construction and Basic Getters/Setters =====

    @Test
    public void testDefaultConstruction() {
        TournamentHistory history = new TournamentHistory();
        assertNull(history.getId());
        assertNull(history.getTournamentName());
        assertNull(history.getPlayerName());
        assertEquals(0, history.getPlayerType());
        assertEquals(0, history.getBuyin());
        assertEquals(0, history.getRebuy());
        assertEquals(0, history.getAddon());
        assertEquals(0, history.getPlace());
        assertEquals(0, history.getPrize());
        assertEquals(0, history.getNumPlayers());
    }

    @Test
    public void testIdGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setId(123L);
        assertEquals(Long.valueOf(123L), history.getId());
    }

    @Test
    public void testTournamentNameGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setTournamentName("Test Tournament");
        assertEquals("Test Tournament", history.getTournamentName());
    }

    @Test
    public void testPlayerNameGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setPlayerName("Alice");
        assertEquals("Alice", history.getPlayerName());
    }

    @Test
    public void testPlayerTypeGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setPlayerType(TournamentHistory.PLAYER_TYPE_ONLINE);
        assertEquals(TournamentHistory.PLAYER_TYPE_ONLINE, history.getPlayerType());
    }

    @Test
    public void testEndDateGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        Date now = new Date();
        history.setEndDate(now);
        assertEquals(now, history.getEndDate());
    }

    @Test
    public void testBuyinGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setBuyin(100);
        assertEquals(100, history.getBuyin());
    }

    @Test
    public void testRebuyGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setRebuy(50);
        assertEquals(50, history.getRebuy());
    }

    @Test
    public void testAddonGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setAddon(25);
        assertEquals(25, history.getAddon());
    }

    @Test
    public void testPlaceGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setPlace(3);
        assertEquals(3, history.getPlace());
    }

    @Test
    public void testPrizeGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setPrize(500);
        assertEquals(500, history.getPrize());
    }

    @Test
    public void testNumPlayersGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setNumPlayers(10);
        assertEquals(10, history.getNumPlayers());
    }

    @Test
    public void testDisconnectsGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setDisconnects(2);
        assertEquals(2, history.getDisconnects());
    }

    @Test
    public void testRank1GetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setRank1(1234.56);
        assertEquals(1234.56, history.getRank1(), 0.01);
    }

    @Test
    public void testEndedGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setEnded(true);
        assertTrue(history.isEnded());

        history.setEnded(false);
        assertFalse(history.isEnded());
    }

    @Test
    public void testProfileGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        OnlineProfile profile = new OnlineProfile();
        history.setProfile(profile);
        assertEquals(profile, history.getProfile());
    }

    @Test
    public void testGameGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        OnlineGame game = new OnlineGame();
        history.setGame(game);
        assertEquals(game, history.getGame());
    }

    // ===== Transient Client-Only Fields =====

    @Test
    public void testGameIdGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setGameId(456L);
        assertEquals(456L, history.getGameId());
    }

    @Test
    public void testTournamentTypeGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setTournamentType("ONLINE");
        assertEquals("ONLINE", history.getTournamentType());
    }

    @Test
    public void testStartDateGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        Date now = new Date();
        history.setStartDate(now);
        assertEquals(now, history.getStartDate());
    }

    @Test
    public void testNumRemainingGetterSetter() {
        TournamentHistory history = new TournamentHistory();
        history.setNumRemaining(5);
        assertEquals(5, history.getNumRemaining());
    }

    // ===== Player Type Constants Tests =====

    @Test
    public void testPlayerTypeConstants() {
        assertEquals(1, TournamentHistory.PLAYER_TYPE_AI);
        assertEquals(2, TournamentHistory.PLAYER_TYPE_ONLINE);
        assertEquals(3, TournamentHistory.PLAYER_TYPE_LOCAL);
    }

    @Test
    public void testIsComputerTrue() {
        TournamentHistory history = new TournamentHistory();
        history.setPlayerType(TournamentHistory.PLAYER_TYPE_AI);
        assertTrue(history.isComputer());
    }

    @Test
    public void testIsComputerFalse() {
        TournamentHistory history = new TournamentHistory();
        history.setPlayerType(TournamentHistory.PLAYER_TYPE_ONLINE);
        assertFalse(history.isComputer());

        history.setPlayerType(TournamentHistory.PLAYER_TYPE_LOCAL);
        assertFalse(history.isComputer());
    }

    // ===== Calculated Fields Tests =====

    @Test
    public void testGetTotalSpent() {
        TournamentHistory history = new TournamentHistory();
        history.setBuyin(100);
        history.setRebuy(50);
        history.setAddon(25);

        assertEquals(175, history.getTotalSpent());
    }

    @Test
    public void testGetTotalSpentZero() {
        TournamentHistory history = new TournamentHistory();
        assertEquals(0, history.getTotalSpent());
    }

    @Test
    public void testGetNet() {
        TournamentHistory history = new TournamentHistory();
        history.setBuyin(100);
        history.setRebuy(50);
        history.setAddon(25);
        history.setPrize(500);

        // Net = 500 - (100 + 50 + 25) = 325
        assertEquals(325, history.getNet());
    }

    @Test
    public void testGetNetNegative() {
        TournamentHistory history = new TournamentHistory();
        history.setBuyin(100);
        history.setRebuy(50);
        history.setPrize(50);

        // Net = 50 - (100 + 50) = -100
        assertEquals(-100, history.getNet());
    }

    @Test
    public void testGetDdr1() {
        TournamentHistory history = new TournamentHistory();
        history.setRank1(1234.56);
        assertEquals(1234, history.getDdr1());
    }

    @Test
    public void testGetRankForUnfinishedGame() {
        TournamentHistory history = new TournamentHistory();
        history.setNumPlayers(10);
        history.setPlace(-5); // Negative place for unfinished game

        // Rank = place + (numPlayers + 1) = -5 + 11 = 6
        assertEquals(6, history.getRank());
    }

    @Test
    public void testGetNumChips() {
        TournamentHistory history = new TournamentHistory();
        history.setPrize(-1000); // Negative prize = chip count

        // NumChips = -prize = -(-1000) = 1000
        assertEquals(1000, history.getNumChips());
    }

    // ===== setPlacePrizeNumPlayers() Tests =====

    @Test
    public void testSetPlacePrizeNumPlayersForEndedGame() {
        TournamentHistory history = new TournamentHistory();
        history.setPlacePrizeNumPlayers(3, 500, 10, 0, 0);

        assertEquals(3, history.getPlace());
        assertEquals(500, history.getPrize());
        assertEquals(10, history.getNumPlayers());
    }

    @Test
    public void testSetPlacePrizeNumPlayersForUnfinishedGame() {
        TournamentHistory history = new TournamentHistory();
        // nPlace=0, nRank=5, numPlayers=10, chipCount=1500
        history.setPlacePrizeNumPlayers(0, 0, 10, 5, 1500);

        // Place = nRank - (numPlayers + 1) = 5 - 11 = -6
        assertEquals(-6, history.getPlace());
        // Prize = -chipCount = -1500
        assertEquals(-1500, history.getPrize());
        assertEquals(10, history.getNumPlayers());
    }

    // ===== isAlive() Tests =====

    @Test
    public void testIsAliveTrue() {
        TournamentHistory history = new TournamentHistory();
        history.setEnded(false);
        history.setPlace(-5); // Negative place = still alive

        assertTrue(history.isAlive());
    }

    @Test
    public void testIsAliveFalseWhenEnded() {
        TournamentHistory history = new TournamentHistory();
        history.setEnded(true);
        history.setPlace(-5);

        assertFalse(history.isAlive());
    }

    @Test
    public void testIsAliveFalseWhenBusted() {
        TournamentHistory history = new TournamentHistory();
        history.setEnded(false);
        history.setPlace(5); // Positive place = busted

        assertFalse(history.isAlive());
    }

    // ===== equals() and hashCode() Tests =====

    @Test
    public void testEqualsSameId() {
        TournamentHistory history1 = new TournamentHistory();
        history1.setId(123L);

        TournamentHistory history2 = new TournamentHistory();
        history2.setId(123L);

        assertTrue(history1.equals(history2));
        assertTrue(history2.equals(history1));
    }

    @Test
    public void testEqualsDifferentId() {
        TournamentHistory history1 = new TournamentHistory();
        history1.setId(123L);

        TournamentHistory history2 = new TournamentHistory();
        history2.setId(456L);

        assertFalse(history1.equals(history2));
    }

    @Test
    public void testEqualsNonTournamentHistory() {
        TournamentHistory history = new TournamentHistory();
        history.setId(123L);

        assertFalse(history.equals("not a history"));
        assertFalse(history.equals(null));
    }

    @Test
    public void testHashCodeConsistency() {
        TournamentHistory history = new TournamentHistory();
        history.setId(123L);

        int hash1 = history.hashCode();
        int hash2 = history.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeEqualObjects() {
        TournamentHistory history1 = new TournamentHistory();
        history1.setId(123L);

        TournamentHistory history2 = new TournamentHistory();
        history2.setId(123L);

        assertEquals(history1.hashCode(), history2.hashCode());
    }

    @Test
    public void testHashCodeNullId() {
        TournamentHistory history = new TournamentHistory();
        // Should not throw even with null ID
        int hash = history.hashCode();
        assertNotEquals(0, hash); // Should return super.hashCode()
    }

    // ===== toString() Tests =====

    @Test
    public void testToString() {
        TournamentHistory history = new TournamentHistory();
        history.setId(123L);
        history.setPlayerName("Alice");
        history.setPlayerType(TournamentHistory.PLAYER_TYPE_ONLINE);
        history.setPlace(3);

        String str = history.toString();

        assertNotNull(str);
        assertTrue(str.contains("Alice"));
        assertTrue(str.contains("123"));
    }

    // ===== Serialization Tests =====

    @Test
    public void testMarshalDemarshal() {
        TournamentHistory original = new TournamentHistory();
        original.setTournamentName("Test Tournament");
        original.setPlayerName("Alice");
        original.setPlayerType(TournamentHistory.PLAYER_TYPE_ONLINE);
        original.setId(123L);
        original.setEndDate(new Date(1000000000L));
        original.setPlace(3);
        original.setPrize(500);
        original.setBuyin(100);
        original.setRebuy(50);
        original.setAddon(25);
        original.setNumPlayers(10);
        original.setDisconnects(1);

        MsgState state = new MsgState();
        String marshalled = original.marshal(state);
        assertNotNull(marshalled);

        TournamentHistory restored = new TournamentHistory();
        restored.demarshal(state, marshalled);

        assertEquals(original.getTournamentName(), restored.getTournamentName());
        assertEquals(original.getPlayerName(), restored.getPlayerName());
        assertEquals(original.getPlayerType(), restored.getPlayerType());
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getEndDate(), restored.getEndDate());
        assertEquals(original.getPlace(), restored.getPlace());
        assertEquals(original.getPrize(), restored.getPrize());
        assertEquals(original.getBuyin(), restored.getBuyin());
        assertEquals(original.getRebuy(), restored.getRebuy());
        assertEquals(original.getAddon(), restored.getAddon());
        assertEquals(original.getNumPlayers(), restored.getNumPlayers());
        assertEquals(original.getDisconnects(), restored.getDisconnects());
    }

    @Test
    public void testMarshalDemarshalNullId() {
        TournamentHistory original = new TournamentHistory();
        original.setTournamentName("Test");
        original.setPlayerName("Bob");
        original.setPlayerType(TournamentHistory.PLAYER_TYPE_AI);
        original.setId(null); // Null ID
        original.setEndDate(new Date());
        original.setPlace(5);
        original.setPrize(100);
        original.setBuyin(50);
        original.setRebuy(0);
        original.setAddon(0);
        original.setNumPlayers(20);

        MsgState state = new MsgState();
        String marshalled = original.marshal(state);

        TournamentHistory restored = new TournamentHistory();
        restored.demarshal(state, marshalled);

        // ID marshals as 0 when null
        assertEquals(Long.valueOf(0L), restored.getId());
        assertEquals(original.getTournamentName(), restored.getTournamentName());
        assertEquals(original.getPlayerName(), restored.getPlayerName());
    }

    // ===== Edge Cases =====

    @Test
    public void testNullDates() {
        TournamentHistory history = new TournamentHistory();
        history.setEndDate(null);
        history.setStartDate(null);

        assertNull(history.getEndDate());
        assertNull(history.getStartDate());
    }

    @Test
    public void testZeroValues() {
        TournamentHistory history = new TournamentHistory();
        history.setBuyin(0);
        history.setRebuy(0);
        history.setAddon(0);
        history.setPrize(0);

        assertEquals(0, history.getTotalSpent());
        assertEquals(0, history.getNet());
    }

    @Test
    public void testNegativePlace() {
        TournamentHistory history = new TournamentHistory();
        history.setPlace(-10);
        assertEquals(-10, history.getPlace());
    }

    @Test
    public void testLargeRankValue() {
        TournamentHistory history = new TournamentHistory();
        history.setRank1(99999.99);
        assertEquals(99999.99, history.getRank1(), 0.01);
        assertEquals(99999, history.getDdr1());
    }
}
