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
package com.donohoedigital.games.poker.network;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerURL - URL parser for poker game connections
 */
class PokerURLTest {

    // =================================================================
    // Constructor and Parsing Tests
    // =================================================================

    @Test
    void should_ParseGameIDAndPassword_When_GivenValidURL() {
        PokerURL url = new PokerURL("poker://localhost:1234/game123/pass456");

        assertThat(url.getGameID()).isEqualTo("game123");
        assertThat(url.getPassword()).isEqualTo("pass456");
    }

    @Test
    void should_ParseSimpleURL_When_GivenMinimalFormat() {
        PokerURL url = new PokerURL("poker://host:1234/id/pw");

        assertThat(url.getGameID()).isEqualTo("id");
        assertThat(url.getPassword()).isEqualTo("pw");
    }

    @Test
    void should_ParseURL_When_GameIDContainsNumbers() {
        PokerURL url = new PokerURL("poker://server:5000/game-12345/secret");

        assertThat(url.getGameID()).isEqualTo("game-12345");
        assertThat(url.getPassword()).isEqualTo("secret");
    }

    @Test
    void should_ParseURL_When_PasswordContainsSpecialCharacters() {
        PokerURL url = new PokerURL("poker://localhost:1234/myGame/p@ss!123");

        assertThat(url.getGameID()).isEqualTo("myGame");
        assertThat(url.getPassword()).isEqualTo("p@ss!123");
    }

    @Test
    void should_ParseURL_When_GameIDHasUnderscore() {
        PokerURL url = new PokerURL("poker://server:5000/game_test_001/password");

        assertThat(url.getGameID()).isEqualTo("game_test_001");
        assertThat(url.getPassword()).isEqualTo("password");
    }

    // =================================================================
    // Protocol Tests
    // =================================================================

    @Test
    void should_AlwaysReturnTrue_When_IsTCPCalled() {
        PokerURL url = new PokerURL("poker://localhost:1234/game/pass");

        assertThat(url.isTCP()).isTrue();
    }

    @Test
    void should_ReturnTrue_For_IsTCP_Regardless_Of_URL() {
        PokerURL url1 = new PokerURL("poker://host1:1234/game1/pass1");
        PokerURL url2 = new PokerURL("poker://host2:9999/game2/pass2");

        assertThat(url1.isTCP()).isTrue();
        assertThat(url2.isTCP()).isTrue();
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReturnURLString_When_ToStringCalled() {
        String spec = "poker://localhost:1234/game123/pass456";
        PokerURL url = new PokerURL(spec);

        String result = url.toString();
        assertThat(result).isNotNull();
        assertThat(result).contains("poker://");
    }

    @Test
    void should_PreserveHost_When_ToStringCalled() {
        PokerURL url = new PokerURL("poker://myserver.com:5000/game/pass");

        String result = url.toString();
        assertThat(result).contains("myserver.com");
    }

    // =================================================================
    // Edge Cases and Error Handling
    // =================================================================

    @Test
    void should_ThrowException_When_URLMissingDelimiter() {
        assertThatThrownBy(() -> new PokerURL("poker://localhost:1234/gameNoPassword")).isInstanceOf(Exception.class);
    }

    @Test
    void should_ThrowException_When_URLHasEmptyURI() {
        assertThatThrownBy(() -> new PokerURL("poker://localhost:1234/")).isInstanceOf(Exception.class);
    }

    @Test
    void should_HandleLongGameID() {
        String longGameID = "game-" + "x".repeat(100);
        PokerURL url = new PokerURL("poker://localhost:1234/" + longGameID + "/pass");

        assertThat(url.getGameID()).isEqualTo(longGameID);
        assertThat(url.getPassword()).isEqualTo("pass");
    }

    @Test
    void should_HandleLongPassword() {
        String longPassword = "p".repeat(100);
        PokerURL url = new PokerURL("poker://localhost:1234/game/" + longPassword);

        assertThat(url.getGameID()).isEqualTo("game");
        assertThat(url.getPassword()).isEqualTo(longPassword);
    }

    // =================================================================
    // Integration with P2PURL Tests
    // =================================================================

    @Test
    void should_InheritP2PURLBehavior_When_Created() {
        PokerURL url = new PokerURL("poker://localhost:1234/game/pass");

        // Should have access to inherited methods
        assertThat(url.toString()).isNotNull();
    }

    @Test
    void should_ParseHostAndPort_When_CreatingURL() {
        PokerURL url = new PokerURL("poker://testhost:9876/gameid/password");

        // Verify the URL was constructed (inherited behavior from P2PURL)
        String urlString = url.toString();
        assertThat(urlString).contains("testhost");
    }

    // =================================================================
    // Consistency Tests
    // =================================================================

    @Test
    void should_ReturnSameValues_When_CalledMultipleTimes() {
        PokerURL url = new PokerURL("poker://localhost:1234/game123/pass456");

        String gameId1 = url.getGameID();
        String gameId2 = url.getGameID();
        String pass1 = url.getPassword();
        String pass2 = url.getPassword();

        assertThat(gameId1).isEqualTo(gameId2);
        assertThat(pass1).isEqualTo(pass2);
    }

    @Test
    void should_NotModifyOriginalSpec_When_GettersUsed() {
        String spec = "poker://localhost:1234/original/secret";
        PokerURL url = new PokerURL(spec);

        // Call getters
        url.getGameID();
        url.getPassword();
        url.isTCP();

        // Values should still be correct
        assertThat(url.getGameID()).isEqualTo("original");
        assertThat(url.getPassword()).isEqualTo("secret");
    }

    // =================================================================
    // Real-world Scenario Tests
    // =================================================================

    @Test
    void should_ParseTypicalProductionURL() {
        PokerURL url = new PokerURL("poker://poker.example.com:7777/tournament-final-2024/secure123");

        assertThat(url.getGameID()).isEqualTo("tournament-final-2024");
        assertThat(url.getPassword()).isEqualTo("secure123");
        assertThat(url.isTCP()).isTrue();
    }

    @Test
    void should_ParseLocalhostDevelopmentURL() {
        PokerURL url = new PokerURL("poker://127.0.0.1:5000/dev-game/test");

        assertThat(url.getGameID()).isEqualTo("dev-game");
        assertThat(url.getPassword()).isEqualTo("test");
    }

    @Test
    void should_ParseURLWithNumericGameID() {
        PokerURL url = new PokerURL("poker://localhost:1234/12345/pass");

        assertThat(url.getGameID()).isEqualTo("12345");
    }
}
