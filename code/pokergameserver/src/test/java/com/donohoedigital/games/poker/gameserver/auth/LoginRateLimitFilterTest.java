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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for {@link LoginRateLimitFilter}.
 */
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
    }

    // ====================================
    // Helpers
    // ====================================

    private MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletRequest loginRequestXff(String xff) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", xff);
        request.setRemoteAddr("10.0.0.1");
        return request;
    }

    // ====================================
    // Tests
    // ====================================

    @Test
    void loginRequest_underLimit_allowed() throws Exception {
        // 5 requests from the same IP — all should pass
        for (int i = 0; i < LoginRateLimitFilter.MAX_ATTEMPTS; i++) {
            MockHttpServletRequest request = loginRequest("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus(), "Request " + (i + 1) + " should pass");
        }
    }

    @Test
    void loginRequest_overLimit_blocked() throws Exception {
        // Use the 5 allowed attempts
        for (int i = 0; i < LoginRateLimitFilter.MAX_ATTEMPTS; i++) {
            MockHttpServletRequest req = loginRequest("192.168.1.2");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, new MockFilterChain());
        }

        // 6th request should be blocked
        MockHttpServletRequest request = loginRequest("192.168.1.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus(), "6th request should be rate-limited");
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("Too many login attempts"),
                "Response body should contain rate limit message");
        // Chain should not have been called
        assertEquals(0, chain.getRequest() == null ? 0 : 1, "Filter chain should not be called after rate limit");
    }

    @Test
    void nonLoginRequest_notFiltered() throws Exception {
        // GET to login URL should not be rate-limited
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus(), "GET request should pass through without rate limiting");
        // Chain should have been called
        assertTrue(chain.getRequest() != null, "Filter chain should have been called");
    }

    @Test
    void nonLoginPath_notFiltered() throws Exception {
        // POST to a different URL should not be rate-limited
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        request.setRemoteAddr("192.168.1.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/register");
            req.setRemoteAddr("192.168.1.4");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            MockFilterChain c = new MockFilterChain();
            filter.doFilter(req, resp, c);
            assertEquals(200, resp.getStatus(), "Register requests should not be rate-limited");
        }
    }

    @Test
    void windowReset_allowsAfterExpiry() throws Exception {
        // Use a filter with a 0ms window so it expires immediately
        LoginRateLimitFilter shortWindowFilter = new LoginRateLimitFilter(0);

        // Use up the limit
        for (int i = 0; i < LoginRateLimitFilter.MAX_ATTEMPTS; i++) {
            MockHttpServletRequest req = loginRequest("192.168.1.5");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            shortWindowFilter.doFilter(req, resp, new MockFilterChain());
        }

        // After window expiry (0ms), a new request should start a fresh window
        Thread.sleep(5); // ensure window has expired
        MockHttpServletRequest request = loginRequest("192.168.1.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        shortWindowFilter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus(), "Request after window reset should be allowed");
    }

    @Test
    void differentIPs_isolatedLimits() throws Exception {
        // Exhaust limit for IP A
        for (int i = 0; i < LoginRateLimitFilter.MAX_ATTEMPTS; i++) {
            MockHttpServletRequest req = loginRequest("10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        // IP B should still be allowed
        MockHttpServletRequest requestB = loginRequest("10.0.0.2");
        MockHttpServletResponse responseB = new MockHttpServletResponse();
        filter.doFilter(requestB, responseB, new MockFilterChain());

        assertEquals(200, responseB.getStatus(), "Different IP should not be rate-limited");
    }

    @Test
    void xForwardedFor_usedForIpExtraction() throws Exception {
        // Exhaust limit using X-Forwarded-For header
        for (int i = 0; i < LoginRateLimitFilter.MAX_ATTEMPTS; i++) {
            MockHttpServletRequest req = loginRequestXff("203.0.113.1");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        // 6th request with same XFF should be blocked
        MockHttpServletRequest request = loginRequestXff("203.0.113.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(429, response.getStatus(), "Request over limit via XFF should be blocked");
    }
}
