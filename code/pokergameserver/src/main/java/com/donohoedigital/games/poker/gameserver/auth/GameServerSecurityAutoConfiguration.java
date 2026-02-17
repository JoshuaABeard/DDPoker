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
package com.donohoedigital.games.poker.gameserver.auth;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Auto-configuration for JWT-based security.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity")
@ConditionalOnProperty(prefix = "game.server.jwt", name = "public-key-path")
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@EnableWebSecurity
public class GameServerSecurityAutoConfiguration {

    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
        Path privateKeyPath = properties.getPrivateKeyPath() != null ? Paths.get(properties.getPrivateKeyPath()) : null;
        Path publicKeyPath = Paths.get(properties.getPublicKeyPath());

        return new JwtTokenProvider(privateKeyPath, publicKeyPath, properties.getExpiration(),
                properties.getRememberMeExpiration());
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider, JwtProperties properties) {
        return new JwtAuthenticationFilter(tokenProvider, properties.getCookieName());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            CorsProperties corsProperties) throws Exception {
        http.csrf(csrf -> csrf.disable()) // JWT is immune to CSRF
                .cors(cors -> {
                    if (!corsProperties.getAllowedOrigins().isEmpty()) {
                        cors.configurationSource(corsConfigurationSource(corsProperties));
                    } else {
                        cors.disable();
                    }
                }).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**").permitAll() // Auth endpoints
                                                                                                    // public
                        .requestMatchers("/api/v1/**").authenticated() // All other API endpoints require auth
                        .anyRequest().permitAll() // Allow non-API requests
                ).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getAllowedOrigins());
        configuration.setAllowedMethods(properties.getAllowedMethods());
        configuration.setAllowedHeaders(properties.getAllowedHeaders());
        configuration.setAllowCredentials(properties.isAllowCredentials());
        configuration.setMaxAge(properties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }
}
