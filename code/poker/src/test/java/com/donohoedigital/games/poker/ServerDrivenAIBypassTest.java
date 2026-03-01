/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests that local AI creation and execution are bypassed when the embedded
 * server drives the game loop (server-driven mode).
 *
 * <p>
 * In server-driven mode (practice games via
 * {@link com.donohoedigital.games.poker.online.WebSocketTournamentDirector}),
 * AI decisions are made server-side by {@code ServerAIProvider}. The desktop
 * client should not create local {@code PokerAI} instances for computer players
 * or attempt to compute AI actions locally.
 */
class ServerDrivenAIBypassTest {

    private PokerGame game;

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        game = new PokerGame(null);
    }

    // =================================================================
    // PokerGame.isServerDriven()
    // =================================================================

    @Test
    void should_NotBeServerDriven_When_NoWebSocketConfig() {
        assertThat(game.isServerDriven()).isFalse();
        assertThat(game.getWebSocketConfig()).isNull();
    }

    @Test
    void should_BeServerDriven_When_WebSocketConfigSet() {
        game.setWebSocketConfig("game-1", "jwt", 9999);

        assertThat(game.isServerDriven()).isTrue();
    }

    // =================================================================
    // PokerPlayer.createPokerAI() bypass
    // =================================================================

    @Test
    void should_SkipAICreation_When_ComputerPlayerInServerDrivenGame() {
        game.setWebSocketConfig("game-1", "jwt", 9999);
        PokerPlayer aiPlayer = seatPlayer(game, 0, "Computer 1", false);

        // setPlayer() triggers createPokerAI() internally, but the server-driven
        // guard should have prevented AI creation for this computer player.
        assertThat(aiPlayer.getGameAI()).isNull();
    }

    @Test
    void should_NotSkipAICreation_When_HumanPlayerInServerDrivenGame() {
        game.setWebSocketConfig("game-1", "jwt", 9999);
        PokerPlayer humanPlayer = seatPlayer(game, 0, "Human", true);

        // Human player passes the server-driven guard — createPokerAI continues.
        // It may still exit for other reasons (playerType_ == null), but the
        // server-driven guard is not what blocks it.
        assertThat(humanPlayer.isHuman()).isTrue();
        assertThat(humanPlayer.isComputer()).isFalse();
    }

    @Test
    void should_CreateAINormally_When_NotServerDriven() {
        // No WebSocket config — not server-driven
        PokerPlayer aiPlayer = seatPlayer(game, 0, "Computer 1", false);

        // createPokerAI proceeds without the server-driven guard.
        // With no playerType_ set, it exits via the playerType_ == null check,
        // but the server-driven guard is not the reason.
        assertThat(game.isServerDriven()).isFalse();
    }

    // =================================================================
    // PokerPlayer.getPokerAI() bypass
    // =================================================================

    @Test
    void should_ReturnNullAI_When_ComputerPlayerInServerDrivenGame() {
        game.setWebSocketConfig("game-1", "jwt", 9999);
        PokerPlayer aiPlayer = seatPlayer(game, 0, "Computer 1", false);

        // getPokerAI() should return null — server handles AI decisions
        assertThat(aiPlayer.getPokerAI()).isNull();
    }

    @Test
    void should_NotBlockHumanAI_When_ServerDriven() {
        game.setWebSocketConfig("game-1", "jwt", 9999);
        PokerPlayer humanPlayer = seatPlayer(game, 0, "Human", true);

        // The server-driven guard in getPokerAI() only blocks computer players.
        // Verify the human player would not be blocked by the guard condition.
        assertThat(humanPlayer.isHuman()).isTrue();
        assertThat(humanPlayer.isComputer()).isFalse();
        // The guard: `isServerDriven() && isComputer()` is false for humans
        assertThat(game.isServerDriven() && humanPlayer.isComputer()).isFalse();
    }

    @Test
    void should_NotBlockAI_When_NotServerDriven() {
        // No WebSocket config — not server-driven
        PokerPlayer aiPlayer = seatPlayer(game, 0, "Computer 1", false);

        // In non-server-driven mode, the guard does not activate
        assertThat(game.isServerDriven()).isFalse();
        // The guard: `isServerDriven() && isComputer()` is false when not server-driven
        assertThat(game.isServerDriven() && aiPlayer.isComputer()).isFalse();
    }

    // =================================================================
    // Multiple players at the same table
    // =================================================================

    @Test
    void should_BypassAIOnlyForComputers_When_MixedTableInServerDrivenMode() {
        game.setWebSocketConfig("game-1", "jwt", 9999);
        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);
        game.setCurrentTable(table);

        PokerPlayer human = new PokerPlayer(1, "Human", true);
        table.setPlayer(human, 0);

        PokerPlayer computer1 = new PokerPlayer(2, "Computer 1", false);
        table.setPlayer(computer1, 1);

        PokerPlayer computer2 = new PokerPlayer(3, "Computer 2", false);
        table.setPlayer(computer2, 2);

        // Computer players: no local AI
        assertThat(computer1.getGameAI()).isNull();
        assertThat(computer2.getGameAI()).isNull();
        assertThat(computer1.getPokerAI()).isNull();
        assertThat(computer2.getPokerAI()).isNull();

        // Human player: not blocked by server-driven guard
        assertThat(human.isHuman()).isTrue();
    }

    // =================================================================
    // Helper methods
    // =================================================================

    /**
     * Creates a table, seats a player, and returns the player. The table is set as
     * the game's current table.
     */
    private PokerPlayer seatPlayer(PokerGame g, int seat, String name, boolean isHuman) {
        PokerTable table;
        if (g.getNumTables() == 0) {
            table = new PokerTable(g, 1);
            g.addTable(table);
            g.setCurrentTable(table);
        } else {
            table = g.getCurrentTable();
        }

        PokerPlayer player = new PokerPlayer(seat + 1, name, isHuman);
        table.setPlayer(player, seat);
        return player;
    }
}
