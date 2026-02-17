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

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application source class for the embedded game server.
 *
 * <p>
 * This class is the entry point passed to
 * {@link org.springframework.boot.SpringApplication} when the desktop client
 * starts its embedded Spring Boot context. All game server infrastructure (REST
 * API, WebSocket, JPA, security) is wired via auto-configuration from the
 * {@code pokergameserver} module — no custom beans are needed here.
 *
 * <p>
 * Activated via the {@code embedded} Spring profile, which loads
 * {@code application-embedded.properties}.
 */
@SpringBootApplication
public class EmbeddedServerConfig {
    // No beans needed here — pokergameserver auto-configurations handle everything
    // via
    // META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
}
