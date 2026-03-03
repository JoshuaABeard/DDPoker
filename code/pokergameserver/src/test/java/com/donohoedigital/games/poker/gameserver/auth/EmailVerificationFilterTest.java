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
package com.donohoedigital.games.poker.gameserver.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link EmailVerificationFilter}.
 */
class EmailVerificationFilterTest {

    private EmailVerificationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new EmailVerificationFilter();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void setAuthentication(String username, boolean emailVerified) {
        JwtAuthenticationFilter.JwtAuthenticationToken auth = new JwtAuthenticationFilter.JwtAuthenticationToken(
                username, 1L, emailVerified);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // Tests
    // =========================================================================

    @Test
    void unverifiedUser_blockedOnProtectedEndpoint() throws Exception {
        setAuthentication("alice", false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/game/lobby");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("EMAIL_NOT_VERIFIED");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void verifiedUser_passesThrough() throws Exception {
        setAuthentication("alice", true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/game/lobby");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isNotEqualTo(403);
    }

    @Test
    void exemptEndpoint_register_passesThrough_evenWhenUnverified() throws Exception {
        setAuthentication("alice", false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void exemptEndpoint_login_passesThrough_evenWhenUnverified() throws Exception {
        setAuthentication("alice", false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void exemptEndpoint_verifyEmail_passesThrough_evenWhenUnverified() throws Exception {
        setAuthentication("alice", false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/verify-email");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void unauthenticatedRequest_passesThrough() throws Exception {
        // No authentication set in SecurityContext

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/game/lobby");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void unverifiedUser_403ResponseBody_hasCorrectErrorCode() throws Exception {
        setAuthentication("bob", false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/game/action");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("EMAIL_NOT_VERIFIED")
                .contains("Email verification required");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void exemptEndpoint_resendVerification_passesThrough_evenWhenUnverified() throws Exception {
        setAuthentication("alice", false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/resend-verification");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void nonJwtAuthentication_passesThrough() throws Exception {
        // Set a non-JWT authentication (e.g., anonymous or basic auth)
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user", "pass", java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/game/lobby");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
