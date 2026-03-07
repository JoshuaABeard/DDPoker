/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import com.donohoedigital.games.poker.gameserver.auth.CorsProperties;
import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager;
import com.donohoedigital.games.poker.gameserver.auth.JwtProperties;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.UUID;

/**
 * Spring Boot configuration for launching the pokergameserver in-process during
 * E2E tests. Mirrors the configuration used by the pokergameserver's own
 * integration tests.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        com.donohoedigital.games.poker.gameserver.auth.GameServerSecurityAutoConfiguration.class,
        com.donohoedigital.games.poker.gameserver.GameServerWebAutoConfiguration.class})
@EnableWebSecurity
@ComponentScan(basePackages = {"com.donohoedigital.games.poker.gameserver.controller",
        "com.donohoedigital.games.poker.gameserver.service"})
class GameServerTestApplication {

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
        String uniqueId = UUID.randomUUID().toString();
        Path privateKey = Path.of(System.getProperty("java.io.tmpdir"), "e2e-test-jwt-private-" + uniqueId + ".pem");
        Path publicKey = Path.of(System.getProperty("java.io.tmpdir"), "e2e-test-jwt-public-" + uniqueId + ".pem");

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
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/api/v1/auth/**").permitAll().requestMatchers("/api/v1/dev/**")
                                .permitAll().requestMatchers("/api/v1/**").authenticated().anyRequest().permitAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
