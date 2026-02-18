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
package com.donohoedigital.games.poker.gameserver.controller;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Security configuration for auth-boundary controller tests. Applies the same
 * URL authorization rules as production: GET /api/v1/games is public, all other
 * /api/v1/** endpoints require authentication. Requests with an "Authorization:
 * Bearer test-token" header are treated as authenticated; unauthenticated
 * requests to protected endpoints receive 401.
 *
 * <p>
 * This is a plain {@code @Configuration} (not {@code @SpringBootConfiguration})
 * so it does not conflict with {@link TestSecurityConfiguration} during
 * {@code @WebMvcTest} bootstrapping. The {@code restricted-security} profile
 * ensures exactly one {@code SecurityFilterChain} is active per test context:
 * when this profile is active, {@link TestSecurityConfiguration}'s permitAll
 * chain is suppressed via {@code @Profile("!restricted-security")}.
 */
@Configuration
@Profile("restricted-security")
public class TestRestrictedSecurityConfiguration {

    @Bean
    public SecurityFilterChain restrictedTestSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, e) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/v1/games", "/api/v1/games/*")
                        .permitAll().requestMatchers("/api/v1/**").authenticated().anyRequest().permitAll())
                .addFilterBefore(new TestAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Filter that authenticates requests bearing "Authorization: Bearer ..." and
     * leaves all others unauthenticated, allowing Spring Security to enforce access
     * rules normally.
     */
    private static class TestAuthFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                JwtAuthenticationFilter.JwtAuthenticationToken auth = new JwtAuthenticationFilter.JwtAuthenticationToken(
                        "testuser", 1L);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
