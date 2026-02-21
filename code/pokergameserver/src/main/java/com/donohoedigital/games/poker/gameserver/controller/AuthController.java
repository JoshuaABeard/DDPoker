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

import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.auth.JwtProperties;
import com.donohoedigital.games.poker.gameserver.dto.ForgotPasswordRequest;
import com.donohoedigital.games.poker.gameserver.dto.LoginRequest;
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.gameserver.dto.ProfileResponse;
import com.donohoedigital.games.poker.gameserver.dto.RegisterRequest;
import com.donohoedigital.games.poker.gameserver.dto.ResetPasswordRequest;
import com.donohoedigital.games.poker.gameserver.service.AuthService;

/**
 * REST controller for authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final String cookieName;
    private final Environment environment;

    public AuthController(AuthService authService, JwtProperties jwtProperties, Environment environment) {
        this.authService = authService;
        this.cookieName = jwtProperties.getCookieName();
        this.environment = environment;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        LoginResponse result = authService.register(request.username(), request.password(), request.email());

        if (result.success()) {
            setAuthCookie(response, result.token());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse result = authService.login(request.username(), request.password(), request.rememberMe());

        if (result.success()) {
            setAuthCookie(response, result.token());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearAuthCookie(response);
        return ResponseEntity.ok().build();
    }

    /**
     * Issues a short-lived WebSocket connect token for the authenticated user.
     *
     * <p>
     * The web client's JWT lives in an HttpOnly cookie inaccessible to JavaScript.
     * This endpoint bridges the gap: authenticated cookie → short-lived WS token
     * the client appends as {@code ?token=xxx} to the WebSocket URL.
     *
     * <p>
     * Security properties:
     * <ul>
     * <li>Rate-limited to 5 requests per minute per user.</li>
     * <li>Token TTL: 60 seconds (single-use, validated in
     * GameWebSocketHandler).</li>
     * <li>Token scope: {@code "ws-connect"} — cannot be used for REST
     * authentication.</li>
     * </ul>
     *
     * @return {@code { "token": "..." }} on success; 429 if rate-limited
     */
    @GetMapping("/ws-token")
    public ResponseEntity<Map<String, String>> wsToken() {
        Long profileId = getAuthenticatedProfileId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        String token = authService.generateWsToken(profileId, username);
        if (token == null) {
            return ResponseEntity.status(429).body(Map.of("message", "Rate limit exceeded. Try again in a minute."));
        }
        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * Returns the current authenticated user's profile data.
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> me() {
        Long profileId = getAuthenticatedProfileId();
        ProfileResponse profile = authService.getCurrentUser(profileId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    /**
     * Initiates a password reset for the given email address.
     *
     * <p>
     * Always returns 200 to avoid leaking whether the email is registered or rate
     * limiting was triggered. In dev/embedded mode (or when no Spring profile is
     * active), the reset token is returned in the response body for use in the
     * desktop UI. Other profiles return 503 since email delivery is not configured.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

        if (!isDevOrEmbedded()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Email delivery not configured. Contact your server administrator."));
        }

        String token = authService.forgotPassword(request.email());
        if (token != null) {
            return ResponseEntity.ok(Map.of("resetToken", token));
        }
        return ResponseEntity.ok(Map.of());
    }

    /**
     * Completes a password reset using a one-time token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.token(), request.newPassword());
            return ResponseEntity.ok(Map.of());
        } catch (AuthService.InvalidResetTokenException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Long getAuthenticatedProfileId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationFilter.JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getProfileId();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Returns true if the application is in dev or embedded mode.
     *
     * <p>
     * No active profiles (typical in tests and local development) is treated as dev
     * mode. Explicit "dev" or "embedded" Spring profiles also qualify.
     */
    private boolean isDevOrEmbedded() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return true;
        }
        for (String profile : activeProfiles) {
            if ("dev".equals(profile) || "embedded".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token).httpOnly(true).secure(false).path("/")
                .maxAge(7 * 24 * 60 * 60).sameSite("Strict").build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "").httpOnly(true).secure(false).path("/").maxAge(0)
                .sameSite("Strict").build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
