/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver.websocket;

import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.GameServerAutoConfiguration;
import com.donohoedigital.games.poker.gameserver.GameServerProperties;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.service.AuthService;
import com.donohoedigital.games.poker.gameserver.service.BanService;
import com.donohoedigital.games.poker.gameserver.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Auto-configuration for WebSocket support. Creates all WebSocket-layer beans
 * after GameServerAutoConfiguration.
 *
 * <p>
 * {@code @EnableWebSocket} here ensures the WebSocket infrastructure
 * (HandlerMapping, etc.) is registered when this auto-configuration is active.
 */
@AutoConfiguration(after = GameServerAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "game.server.enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocket
public class WebSocketAutoConfiguration {

    @Bean
    public GameConnectionManager gameConnectionManager() {
        return new GameConnectionManager();
    }

    @Bean
    public OutboundMessageConverter outboundMessageConverter() {
        return new OutboundMessageConverter();
    }

    @Bean
    public RateLimiter actionRateLimiter(GameServerProperties properties) {
        return new RateLimiter(properties.rateLimitMillis());
    }

    @Bean
    public RateLimiter chatRateLimiter() {
        // Chat: 5 messages per 10 seconds = 2000ms minimum interval
        return new RateLimiter(2000);
    }

    @Bean
    public InboundMessageRouter inboundMessageRouter(GameInstanceManager gameInstanceManager,
            GameConnectionManager gameConnectionManager, @Qualifier("actionRateLimiter") RateLimiter actionRateLimiter,
            @Qualifier("chatRateLimiter") RateLimiter chatRateLimiter, ObjectMapper objectMapper) {
        return new InboundMessageRouter(gameInstanceManager, gameConnectionManager, actionRateLimiter, chatRateLimiter,
                objectMapper);
    }

    @Bean
    public LobbyBroadcaster lobbyBroadcaster(GameConnectionManager gameConnectionManager) {
        return new LobbyBroadcaster(gameConnectionManager);
    }

    @Bean
    public GameWebSocketHandler gameWebSocketHandler(JwtTokenProvider jwtTokenProvider,
            GameInstanceManager gameInstanceManager, GameConnectionManager gameConnectionManager,
            InboundMessageRouter inboundMessageRouter, OutboundMessageConverter outboundMessageConverter,
            ObjectMapper objectMapper, GameService gameService, AuthService authService,
            GameServerProperties properties) {
        return new GameWebSocketHandler(jwtTokenProvider, gameInstanceManager, gameConnectionManager,
                inboundMessageRouter, outboundMessageConverter, objectMapper, gameService, authService, properties);
    }

    @Bean
    public LobbyWebSocketHandler lobbyWebSocketHandler(JwtTokenProvider jwtTokenProvider, BanService banService,
            ObjectMapper objectMapper) {
        return new LobbyWebSocketHandler(jwtTokenProvider, banService, objectMapper);
    }

    @Bean
    public GameWebSocketConfig gameWebSocketConfig(GameWebSocketHandler gameHandler,
            LobbyWebSocketHandler lobbyHandler) {
        return new GameWebSocketConfig(gameHandler, lobbyHandler);
    }
}
