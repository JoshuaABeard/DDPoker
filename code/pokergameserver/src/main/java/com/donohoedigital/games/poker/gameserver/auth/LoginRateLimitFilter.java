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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that rate-limits login attempts per IP address.
 *
 * <p>
 * Allows up to {@value #MAX_ATTEMPTS} POST requests to /api/v1/auth/login per
 * IP within a {@value #WINDOW_MILLIS}ms sliding window. Requests over the limit
 * receive HTTP 429. Window resets after expiry.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final int MAX_ATTEMPTS = 5;
    static final long WINDOW_MILLIS = 15 * 60 * 1000L; // 15 minutes

    private record AttemptRecord(AtomicInteger count, long windowStart) {
    }

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();
    private final long windowMillis;

    /**
     * Create filter with default 15-minute window.
     */
    public LoginRateLimitFilter() {
        this.windowMillis = WINDOW_MILLIS;
    }

    /**
     * Create filter with custom window (for testing).
     *
     * @param windowMillis
     *            window size in milliseconds
     */
    public LoginRateLimitFilter(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        AttemptRecord record = attempts.compute(ip, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v.windowStart() > windowMillis) {
                return new AttemptRecord(new AtomicInteger(1), now);
            }
            v.count().incrementAndGet();
            return v;
        });

        if (record.count().get() > MAX_ATTEMPTS) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"success\":false,\"message\":\"Too many login attempts. Please try again later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/v1/auth/login".equals(request.getRequestURI());
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
