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

import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.comms.Version;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerMain - main application class and entry point.
 */
class PokerMainTest {

    private PokerMain pokerMain;

    @BeforeEach
    void setUp() {
        ConfigManager configMgr = new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        configMgr.loadGuiConfig(); // Required for StylesConfig
        // Create PokerMain in headless mode without loading names
        pokerMain = new PokerMain("poker", "poker", new String[0], true, false);
    }

    // ========== Version and Configuration Tests ==========

    @Test
    void should_ReturnPokerConstantsVersion_WhenGetVersion() {
        Version version = pokerMain.getVersion();

        assertThat(version).isEqualTo(PokerConstants.VERSION);
    }

    @Test
    void should_ReturnHardcodedSplashBackground() {
        String background = pokerMain.getSplashBackgroundFile();

        assertThat(background).isEqualTo("poker-splash-nochoice.jpg");
    }

    @Test
    void should_ReturnHardcodedSplashIcon() {
        String icon = pokerMain.getSplashIconFile();

        assertThat(icon).isEqualTo("pokericon32.gif");
    }

    @Test
    void should_ReturnHardcodedSplashTitle() {
        String title = pokerMain.getSplashTitle();

        assertThat(title).isEqualTo("DD Poker");
    }

    // ========== Name Management Tests ==========

    @Test
    void should_ReturnEmptyList_WhenNamesNotLoaded() {
        // Names are not loaded when bLoadNames=false
        List<String> names = pokerMain.getNames();

        assertThat(names).isNotNull();
        assertThat(names).isEmpty();
    }

    @Test
    void should_ReturnUnmodifiableList_FromGetNames() {
        List<String> names = pokerMain.getNames();

        assertThatThrownBy(() -> names.add("test")).isInstanceOf(UnsupportedOperationException.class);
    }

    // ========== Message Validation Tests ==========

    @Test
    void should_ReturnTrue_WhenMessageIsValid() {
        DDMessage message = new DDMessage(0);
        message.setVersion(PokerConstants.VERSION);
        message.setKey("testKey");

        boolean valid = pokerMain.isValid(message);

        assertThat(valid).isTrue();
    }

    @Test
    void should_ReturnFalse_WhenMessageHasNoKey() {
        DDMessage message = new DDMessage(0);
        message.setVersion(PokerConstants.VERSION);
        // No key set

        boolean valid = pokerMain.isValid(message);

        assertThat(valid).isFalse();
    }

    @Test
    void should_ReturnFalse_WhenMessageHasNullVersion() {
        DDMessage message = new DDMessage(0);
        message.setVersion(null);
        message.setKey("testKey");

        boolean valid = pokerMain.isValid(message);

        assertThat(valid).isFalse();
    }

    // ========== Network Configuration Tests ==========

    @Test
    void should_ReturnZero_WhenP2PNotInitialized() {
        // P2P is not initialized in headless test mode
        int port = pokerMain.getPort();

        assertThat(port).isEqualTo(0);
    }

    @Test
    void should_ReturnLocalhost_WhenP2PNotInitialized() {
        // P2P is not initialized in headless test mode
        String ip = pokerMain.getIP();

        assertThat(ip).isEqualTo("127.0.0.1");
    }

    // ========== Online Game Comparison Tests ==========

    @Test
    void should_ReturnTrue_WhenBothOnlineGamesAreNull() {
        boolean equivalent = pokerMain.isEquivalentOnlineGame(null, null);

        assertThat(equivalent).isTrue();
    }

    @Test
    void should_ReturnFalse_WhenFirstOnlineGameIsNull() {
        DMTypedHashMap game2 = new DMTypedHashMap();
        game2.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);

        boolean equivalent = pokerMain.isEquivalentOnlineGame(null, game2);

        assertThat(equivalent).isFalse();
    }

    @Test
    void should_ReturnFalse_WhenSecondOnlineGameIsNull() {
        DMTypedHashMap game1 = new DMTypedHashMap();
        game1.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);

        boolean equivalent = pokerMain.isEquivalentOnlineGame(game1, null);

        assertThat(equivalent).isFalse();
    }

    @Test
    void should_ReturnFalse_WhenOnlineGamesHaveDifferentStatus() {
        DMTypedHashMap game1 = new DMTypedHashMap();
        game1.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);

        DMTypedHashMap game2 = new DMTypedHashMap();
        game2.setInteger(PokerMain.ONLINE_GAME_STATUS, 2);

        boolean equivalent = pokerMain.isEquivalentOnlineGame(game1, game2);

        assertThat(equivalent).isFalse();
    }

    @Test
    void should_ReturnFalse_WhenOnlineGamesHaveDifferentLanConnect() {
        DMTypedHashMap game1 = new DMTypedHashMap();
        game1.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game1.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");

        DMTypedHashMap game2 = new DMTypedHashMap();
        game2.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game2.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url2");

        boolean equivalent = pokerMain.isEquivalentOnlineGame(game1, game2);

        assertThat(equivalent).isFalse();
    }

    @Test
    void should_ReturnTrue_WhenOnlineGamesHaveSameStatusAndLanConnect_AndNoProfile() {
        DMTypedHashMap game1 = new DMTypedHashMap();
        game1.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game1.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");

        DMTypedHashMap game2 = new DMTypedHashMap();
        game2.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game2.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");

        boolean equivalent = pokerMain.isEquivalentOnlineGame(game1, game2);

        assertThat(equivalent).isTrue();
    }

    @Test
    void should_ReturnFalse_WhenOneGameHasProfile_AndOtherDoesNot() {
        TournamentProfile profile1 = new TournamentProfile("Test");

        DMTypedHashMap game1 = new DMTypedHashMap();
        game1.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game1.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");
        game1.setObject(PokerMain.ONLINE_GAME_PROFILE, profile1);

        DMTypedHashMap game2 = new DMTypedHashMap();
        game2.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game2.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");

        boolean equivalent = pokerMain.isEquivalentOnlineGame(game1, game2);

        assertThat(equivalent).isFalse();
    }

    @Test
    void should_ReturnFalse_WhenProfilesHaveDifferentNames() {
        TournamentProfile profile1 = new TournamentProfile("Test1");
        TournamentProfile profile2 = new TournamentProfile("Test2");

        DMTypedHashMap game1 = new DMTypedHashMap();
        game1.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game1.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");
        game1.setObject(PokerMain.ONLINE_GAME_PROFILE, profile1);

        DMTypedHashMap game2 = new DMTypedHashMap();
        game2.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game2.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");
        game2.setObject(PokerMain.ONLINE_GAME_PROFILE, profile2);

        boolean equivalent = pokerMain.isEquivalentOnlineGame(game1, game2);

        assertThat(equivalent).isFalse();
    }

    @Test
    void should_ReturnTrue_WhenBothProfilesAreTheSameInstance() {
        TournamentProfile profile = new TournamentProfile("Test");

        DMTypedHashMap game1 = new DMTypedHashMap();
        game1.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game1.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");
        game1.setObject(PokerMain.ONLINE_GAME_PROFILE, profile);

        DMTypedHashMap game2 = new DMTypedHashMap();
        game2.setInteger(PokerMain.ONLINE_GAME_STATUS, 1);
        game2.setString(PokerMain.ONLINE_GAME_LAN_CONNECT, "url1");
        game2.setObject(PokerMain.ONLINE_GAME_PROFILE, profile);

        boolean equivalent = pokerMain.isEquivalentOnlineGame(game1, game2);

        assertThat(equivalent).isTrue();
    }

}
