/*
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.gameserver;

import com.donohoedigital.games.poker.gameserver.auth.CorsProperties;
import com.donohoedigital.games.poker.gameserver.auth.GameServerSecurityAutoConfiguration;
import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager;
import com.donohoedigital.games.poker.gameserver.auth.JwtProperties;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
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
 * Standalone Spring Boot application for running the pokergameserver as an
 * independent process. Used by Playwright E2E tests and local development.
 */
@Configuration
@EnableAutoConfiguration(exclude = {GameServerSecurityAutoConfiguration.class, GameServerWebAutoConfiguration.class})
@EnableWebSecurity
@ComponentScan(basePackages = {"com.donohoedigital.games.poker.gameserver",
        "com.donohoedigital.games.poker.gameserver.controller", "com.donohoedigital.games.poker.gameserver.service"})
public class GameServerStandaloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameServerStandaloneApplication.class, args);
    }

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
        Path privateKey = Path.of(System.getProperty("java.io.tmpdir"), "standalone-jwt-private-" + uniqueId + ".pem");
        Path publicKey = Path.of(System.getProperty("java.io.tmpdir"), "standalone-jwt-public-" + uniqueId + ".pem");

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
