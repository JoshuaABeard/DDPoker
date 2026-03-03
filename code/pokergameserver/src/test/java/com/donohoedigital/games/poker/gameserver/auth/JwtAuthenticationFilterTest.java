/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.Cookie;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * <p>
 * Tests verify token extraction from Authorization header and cookie, as well
 * as proper authentication setup in the security context.
 * </p>
 */
class JwtAuthenticationFilterTest {

    private JwtTokenProvider tokenProvider;
    private JwtAuthenticationFilter filter;

    private static final String COOKIE_NAME = "DDPoker-JWT";
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String USERNAME = "testuser";
    private static final Long PROFILE_ID = 42L;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(JwtTokenProvider.class);
        filter = new JwtAuthenticationFilter(tokenProvider, COOKIE_NAME);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_setAuthentication_when_validBearerTokenPresent() throws Exception {
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);
        when(tokenProvider.getProfileIdFromToken(VALID_TOKEN)).thenReturn(PROFILE_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        JwtAuthenticationFilter.JwtAuthenticationToken auth = (JwtAuthenticationFilter.JwtAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(USERNAME);
        assertThat(auth.getProfileId()).isEqualTo(PROFILE_ID);
    }

    @Test
    void should_setAuthentication_when_validCookiePresent() throws Exception {
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);
        when(tokenProvider.getProfileIdFromToken(VALID_TOKEN)).thenReturn(PROFILE_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, VALID_TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void should_notSetAuthentication_when_noTokenPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void should_notSetAuthentication_when_tokenInvalid() throws Exception {
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void should_continueFilterChain_even_when_noToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Chain must have been invoked
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void should_skipNonJwtCookies_and_findCorrectOne() throws Exception {
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);
        when(tokenProvider.getProfileIdFromToken(VALID_TOKEN)).thenReturn(PROFILE_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("session", "other-value"), new Cookie(COOKIE_NAME, VALID_TOKEN),
                new Cookie("csrf", "token123"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void should_preferBearerHeader_over_cookie() throws Exception {
        String headerToken = "header.token";
        when(tokenProvider.validateToken(headerToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(headerToken)).thenReturn("headeruser");
        when(tokenProvider.getProfileIdFromToken(headerToken)).thenReturn(1L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + headerToken);
        request.setCookies(new Cookie(COOKIE_NAME, "cookie.token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Should have authenticated with the header token (not the cookie)
        JwtAuthenticationFilter.JwtAuthenticationToken auth = (JwtAuthenticationFilter.JwtAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo("headeruser");
    }

    @Test
    void should_ignoreHeadersThatDoNotStartWithBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // -------------------------------------------------------------------------
    // JwtAuthenticationToken inner class
    // -------------------------------------------------------------------------

    @Test
    void jwtAuthenticationToken_should_storeProfileId() {
        JwtAuthenticationFilter.JwtAuthenticationToken token = new JwtAuthenticationFilter.JwtAuthenticationToken(
                "alice", 77L, false);
        assertThat(token.getProfileId()).isEqualTo(77L);
        assertThat(token.getPrincipal()).isEqualTo("alice");
    }

    @Test
    void jwtAuthenticationToken_should_storeEmailVerified() {
        JwtAuthenticationFilter.JwtAuthenticationToken tokenTrue = new JwtAuthenticationFilter.JwtAuthenticationToken(
                "alice", 77L, true);
        assertThat(tokenTrue.isEmailVerified()).isTrue();

        JwtAuthenticationFilter.JwtAuthenticationToken tokenFalse = new JwtAuthenticationFilter.JwtAuthenticationToken(
                "bob", 88L, false);
        assertThat(tokenFalse.isEmailVerified()).isFalse();
    }

    @Test
    void should_setEmailVerifiedTrue_when_tokenContainsEmailVerifiedTrue() throws Exception {
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);
        when(tokenProvider.getProfileIdFromToken(VALID_TOKEN)).thenReturn(PROFILE_ID);
        when(tokenProvider.getEmailVerifiedFromToken(VALID_TOKEN)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        JwtAuthenticationFilter.JwtAuthenticationToken auth = (JwtAuthenticationFilter.JwtAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        assertThat(auth.isEmailVerified()).isTrue();
    }

    @Test
    void should_setEmailVerifiedFalse_when_tokenContainsEmailVerifiedFalse() throws Exception {
        when(tokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(USERNAME);
        when(tokenProvider.getProfileIdFromToken(VALID_TOKEN)).thenReturn(PROFILE_ID);
        when(tokenProvider.getEmailVerifiedFromToken(VALID_TOKEN)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        JwtAuthenticationFilter.JwtAuthenticationToken auth = (JwtAuthenticationFilter.JwtAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        assertThat(auth.isEmailVerified()).isFalse();
    }
}
