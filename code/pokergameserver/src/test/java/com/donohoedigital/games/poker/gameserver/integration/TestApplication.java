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
package com.donohoedigital.games.poker.gameserver.integration;

import java.nio.file.Path;
import java.security.KeyPair;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.donohoedigital.games.poker.gameserver.auth.CorsProperties;
import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager;
import com.donohoedigital.games.poker.gameserver.auth.JwtProperties;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;

/**
 * Test application configuration for integration tests.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = com.donohoedigital.games.poker.gameserver.auth.GameServerSecurityAutoConfiguration.class)
@EnableWebSecurity
@ComponentScan(basePackages = {"com.donohoedigital.games.poker.gameserver.controller",
        "com.donohoedigital.games.poker.gameserver.service"}, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test\\$TestConfig"),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.donohoedigital.games.poker.gameserver.controller.TestSecurityConfiguration.class)})
public class TestApplication {

    @Bean
    public JwtProperties jwtProperties() {
        JwtProperties props = new JwtProperties();
        props.setCookieName("DDPoker-JWT");
        return props;
    }

    @Bean
    public CorsProperties corsProperties() {
        return new CorsProperties();
    }

    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider() throws Exception {
        KeyPair keyPair = JwtKeyManager.generateKeyPair();
        String uniqueId = java.util.UUID.randomUUID().toString();
        Path privateKey = Path.of(System.getProperty("java.io.tmpdir"),
                "integration-test-jwt-private-" + uniqueId + ".pem");
        Path publicKey = Path.of(System.getProperty("java.io.tmpdir"),
                "integration-test-jwt-public-" + uniqueId + ".pem");

        JwtKeyManager.savePrivateKey(keyPair.getPrivate(), privateKey);
        JwtKeyManager.savePublicKey(keyPair.getPublic(), publicKey);

        return new JwtTokenProvider(privateKey, publicKey, 3600000L, 604800000L);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        return new JwtAuthenticationFilter(tokenProvider, "DDPoker-JWT");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated().anyRequest().permitAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
