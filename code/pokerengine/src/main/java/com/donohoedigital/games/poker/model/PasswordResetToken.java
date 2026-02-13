/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.db.model.BaseModel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a password reset token for secure password reset flow. Tokens
 * expire after a configurable period (default 1 hour) and can only be used
 * once.
 */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken implements BaseModel<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prt_id", nullable = false)
    private Long id;

    @Column(name = "prt_token", unique = true, nullable = false, length = 36)
    private String token;

    @Column(name = "prt_profile_id", nullable = false)
    private Long profileId;

    @Column(name = "prt_create_date", nullable = false)
    private Instant createDate;

    @Column(name = "prt_expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "prt_used", nullable = false)
    private boolean used;

    /**
     * Default token expiration time in milliseconds (1 hour)
     */
    public static final long DEFAULT_EXPIRY_MS = 60 * 60 * 1000;

    /**
     * Creates an uninitialized instance
     */
    public PasswordResetToken() {
        this.used = false;
    }

    /**
     * Creates a new password reset token for the given profile
     *
     * @param profileId
     *            ID of the profile this token is for
     * @param expiryMs
     *            Expiry time in milliseconds from now
     */
    public PasswordResetToken(Long profileId, long expiryMs) {
        this();
        this.token = UUID.randomUUID().toString();
        this.profileId = profileId;
        this.createDate = Instant.now();
        this.expiryDate = createDate.plusMillis(expiryMs);
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    /**
     * Check if this token is valid (not expired and not used)
     */
    @Transient
    public boolean isValid() {
        return !used && Instant.now().isBefore(expiryDate);
    }

    /**
     * Mark this token as used
     */
    public void markAsUsed() {
        this.used = true;
    }
}
