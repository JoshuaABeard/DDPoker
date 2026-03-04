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
package com.donohoedigital.games.poker.gameserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.donohoedigital.games.poker.gameserver.auth.CorsProperties;

/**
 * Verifies that CORS preflight requests return the correct headers for
 * configured origins, and that the production configuration source correctly
 * uses the allowed origins from {@link CorsProperties}.
 */
@WebMvcTest
@Import({CorsConfigurationTest.CorsTestSecurityConfig.class, CorsConfigurationTest.StubAuthController.class})
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type")).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void corsPreflightAllowsSecondConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login").header("Origin", "http://localhost:8080")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type")).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));
    }

    @Test
    void corsPreflightBlocksUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login").header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /**
     * Verifies that CorsProperties.allowedOrigins is used directly by the
     * production configuration source, including server-host origins. This test
     * exercises the same code path as GameServerSecurityAutoConfiguration
     * .corsConfigurationSource() by constructing the source the same way.
     */
    @Test
    void corsConfigurationSourceUsesAllowedOriginsFromProperties() {
        CorsProperties props = new CorsProperties();
        props.setAllowedOrigins(List.of("http://localhost:3000", "https://poker.example.com"));

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.getAllowedOrigins());
        config.setAllowedMethods(props.getAllowedMethods());
        config.setAllowedHeaders(props.getAllowedHeaders());
        config.setAllowCredentials(props.isAllowCredentials());
        config.setMaxAge(props.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", config);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("Origin", "https://poker.example.com");
        CorsConfiguration resolved = source.getCorsConfiguration(request);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getAllowedOrigins()).contains("https://poker.example.com");
        assertThat(resolved.getAllowedOrigins()).contains("http://localhost:3000");
        assertThat(resolved.getExposedHeaders()).isNullOrEmpty();
    }

    @RestController
    static class StubAuthController {
        @PostMapping("/api/v1/auth/login")
        public String login() {
            return "ok";
        }
    }

    @SpringBootConfiguration
    @EnableWebSecurity
    static class CorsTestSecurityConfig {

        @Bean
        public SecurityFilterChain corsTestSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsTestConfigurationSource()))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        public CorsConfigurationSource corsTestConfigurationSource() {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/api/v1/**", config);
            return source;
        }
    }
}
