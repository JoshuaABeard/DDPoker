/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * JPA entity for bans. Supports both profile ID and email-based bans.
 *
 * <p>
 * Ban types:
 * </p>
 * <ul>
 * <li>PROFILE - Ban a specific account by profile ID</li>
 * <li>EMAIL - Ban all accounts sharing an email (catches alt accounts)</li>
 * </ul>
 */
@Entity
@Table(name = "bans", indexes = {@Index(name = "idx_ban_profile_id", columnList = "profile_id"),
        @Index(name = "idx_ban_email", columnList = "email")})
public class BanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ban_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BanType banType;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "email")
    private String email;

    @Column(name = "until", nullable = false)
    private LocalDate until;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Ban type enumeration.
     */
    public enum BanType {
        /** Ban a specific profile ID */
        PROFILE,
        /** Ban all accounts with this email */
        EMAIL
    }

    @PrePersist
    private void onInsert() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (until == null) {
            until = LocalDate.of(2099, 12, 31); // Default to permanent ban
        }
    }

    /**
     * Check if this ban is currently active.
     *
     * @return true if the ban hasn't expired yet
     */
    public boolean isActive() {
        return !LocalDate.now().isAfter(until);
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BanType getBanType() {
        return banType;
    }

    public void setBanType(BanType banType) {
        this.banType = banType;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getUntil() {
        return until;
    }

    public void setUntil(LocalDate until) {
        this.until = until;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
