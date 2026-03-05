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

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.BanRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.service.EmailService;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Admin endpoints - profile search and account management.
 *
 * <p>
 * Note: all /api/v1/** endpoints require authentication. Role-based
 * authorization (ROLE_ADMIN) should be added when the role system is
 * implemented.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final OnlineProfileRepository profileRepository;
    private final BanRepository banRepository;

    @Autowired(required = false)
    private EmailService emailService;

    public AdminController(OnlineProfileRepository profileRepository, BanRepository banRepository) {
        this.profileRepository = profileRepository;
        this.banRepository = banRepository;
    }

    /**
     * Search profiles (admin).
     */
    @GetMapping("/profiles")
    public ResponseEntity<Page<OnlineProfile>> searchProfiles(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "includeRetired", defaultValue = "false") boolean includeRetired,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "50") int pageSize) {

        String namePattern = name == null || name.isEmpty() ? "%" : "%" + name + "%";
        String emailPattern = email == null || email.isEmpty() ? "%" : "%" + email + "%";

        Page<OnlineProfile> profiles = profileRepository.searchProfiles(namePattern, emailPattern, includeRetired,
                PageRequest.of(page, pageSize, Sort.by("name")));
        return ResponseEntity.ok(profiles);
    }

    /**
     * Manually mark a profile as email-verified.
     */
    @PostMapping("/profiles/{id}/verify")
    public ResponseEntity<Void> manuallyVerify(@PathVariable("id") Long id) {
        OnlineProfile p = profileRepository.findById(id).orElse(null);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        p.setEmailVerified(true);
        p.setEmailVerificationToken(null);
        p.setEmailVerificationTokenExpiry(null);
        profileRepository.save(p);
        return ResponseEntity.ok().build();
    }

    /**
     * Clear account lockout for a profile.
     */
    @PostMapping("/profiles/{id}/unlock")
    public ResponseEntity<Void> unlockAccount(@PathVariable("id") Long id) {
        OnlineProfile p = profileRepository.findById(id).orElse(null);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        p.setLockedUntil(null);
        p.setFailedLoginAttempts(0);
        p.setLockoutCount(0);
        profileRepository.save(p);
        return ResponseEntity.ok().build();
    }

    /**
     * Trigger a new verification email for an unverified profile.
     */
    @PostMapping("/profiles/{id}/resend-verification")
    public ResponseEntity<Void> adminResendVerification(@PathVariable("id") Long id) {
        OnlineProfile p = profileRepository.findById(id).orElse(null);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        if (p.isEmailVerified()) {
            return ResponseEntity.badRequest().build();
        }
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        p.setEmailVerificationToken(token);
        p.setEmailVerificationTokenExpiry(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
        profileRepository.save(p);
        if (emailService != null) {
            emailService.sendVerificationEmail(p.getEmail(), p.getName(), token);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * List all bans.
     */
    @GetMapping("/bans")
    public ResponseEntity<List<BanEntity>> listBans() {
        return ResponseEntity.ok(banRepository.findAll());
    }

    /**
     * Add a ban.
     */
    @PostMapping("/bans")
    public ResponseEntity<BanEntity> addBan(@RequestBody BanEntity ban) {
        BanEntity saved = banRepository.save(ban);
        return ResponseEntity.ok(saved);
    }

    /**
     * Remove a ban by ID.
     */
    @DeleteMapping("/bans/{id}")
    public ResponseEntity<Void> removeBan(@PathVariable("id") Long id) {
        if (!banRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        banRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
