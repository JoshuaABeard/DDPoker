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

import com.donohoedigital.games.poker.gameserver.GameServerAutoConfiguration;
import com.donohoedigital.games.poker.gameserver.GameServerWebAutoConfiguration;
import com.donohoedigital.games.poker.gameserver.auth.GameServerSecurityAutoConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.GameServerPersistenceAutoConfiguration;
import com.donohoedigital.games.poker.gameserver.websocket.WebSocketAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot application source class for the embedded game server.
 *
 * <p>
 * This class is the entry point passed to
 * {@link org.springframework.boot.SpringApplication} when the desktop client
 * starts its embedded Spring Boot context. All game server infrastructure (REST
 * API, WebSocket, JPA, security) is wired via explicit {@link Import}.
 *
 * <p>
 * We use {@code @Import} rather than relying on
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * because the maven-assembly-plugin fat-JAR build overwrites that file with
 * Spring Boot's own version, silently dropping our entries. Explicit imports
 * survive the merge.
 *
 * <p>
 * Activated via the {@code embedded} Spring profile, which loads
 * {@code application-embedded.properties}.
 */
@SpringBootApplication
@Import({GameServerAutoConfiguration.class, GameServerWebAutoConfiguration.class,
        GameServerPersistenceAutoConfiguration.class, GameServerSecurityAutoConfiguration.class,
        WebSocketAutoConfiguration.class})
public class EmbeddedServerConfig {
}
