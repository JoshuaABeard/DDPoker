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
package com.donohoedigital.games.poker.server;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EmbeddedGameServer#buildStartupProperties} — verifies
 * that the embedded server is restricted to localhost when a specific port is
 * requested and that no address constraint is applied in random-port mode.
 */
class EmbeddedGameServerBindingTest {

    private final EmbeddedGameServer server = new EmbeddedGameServer();

    @Test
    void specificPortSetsLocalhostAddress() {
        Properties props = server.buildStartupProperties(11885, true);

        assertThat(props.getProperty("server.address")).isEqualTo("127.0.0.1");
        assertThat(props.getProperty("server.port")).isEqualTo("11885");
    }

    @Test
    void randomPortModeDoesNotSetServerAddress() {
        Properties props = server.buildStartupProperties(null, false);

        assertThat(props.getProperty("server.address")).isNull();
        assertThat(props.getProperty("server.port")).isNull();
    }

    @Test
    void localhostAddressIsNot0000() {
        Properties props = server.buildStartupProperties(8080, true);

        assertThat(props.getProperty("server.address")).isNotEqualTo("0.0.0.0");
    }
}
