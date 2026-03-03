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
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that enforces email verification for authenticated users.
 *
 * <p>
 * Runs after {@link JwtAuthenticationFilter}. If the authenticated user's
 * {@code emailVerified} claim is {@code false}, returns HTTP 403 with error
 * code {@code EMAIL_NOT_VERIFIED} for all endpoints except the exempt list.
 */
public class EmailVerificationFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PATHS = Set.of("/api/v1/auth/register", "/api/v1/auth/login",
            "/api/v1/auth/logout", "/api/v1/auth/verify-email", "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/check-username");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (EXEMPT_PATHS.contains(request.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationFilter.JwtAuthenticationToken jwtAuth)) {
            chain.doFilter(request, response);
            return;
        }

        if (jwtAuth.isEmailVerified()) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"EMAIL_NOT_VERIFIED\",\"message\":\"Email verification required\"}");
    }
}
