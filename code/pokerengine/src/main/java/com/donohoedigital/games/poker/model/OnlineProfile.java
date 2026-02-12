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

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.db.model.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Date;

/**
 * Represents an online player profile.
 */
@Entity
@Table(name = "wan_profile")
public class OnlineProfile implements BaseModel<Long> {
    private DMTypedHashMap data_;

    /**
     * Dummy types
     */
    public enum Dummy {
        HUMAN("__DUMMY__"), AI_BEST("__AIBEST__"), AI_REST("__AIREST__");

        // name and constructor for name
        private final String sName;

        private Dummy(String sName) {
            this.sName = sName;
        }

        /**
         * get type name for string
         */
        public String getName() {
            return sName;
        }
    }

    public static final String PROFILE_ID = "profileid";
    public static final String PROFILE_NAME = "profilename";
    public static final String PROFILE_EMAIL = "profileemail";
    public static final String PROFILE_PASSWORD = "profilepassword";
    public static final String PROFILE_PASSWORD_HASH = "profilepasswordhash";
    public static final String PROFILE_UUID = "profileuuid";
    public static final String PROFILE_CREATE_DATE = "profilecreatedate";
    public static final String PROFILE_MODIFY_DATE = "profilemodifydate";
    public static final String PROFILE_RETIRED = "profileretired";
    public static final String PROFILE_ACTIVATED = "profileactivated";
    public static final String PROFILE_LICENSE_KEY = "profilelicensekey";

    /**
     * Creates an uninitialized instance of OnlineProfile
     */
    public OnlineProfile() {
        data_ = new DMTypedHashMap();
    }

    /**
     * Creates an a new instance of OnlineProfile with the given name
     */
    public OnlineProfile(String name) {
        this();
        setName(name);
    }

    /**
     * Creates a new instance of OnlineProfile with the given source data
     */
    public OnlineProfile(DMTypedHashMap data) {
        data_ = data;
    }

    @Transient
    public DMTypedHashMap getData() {
        return data_;
    }

    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wpr_id", nullable = false)
    public Long getId() {
        return data_.getLong(PROFILE_ID);
    }

    public void setId(Long id) {
        data_.setLong(PROFILE_ID, id);
    }

    @Column(name = "wpr_name", unique = true, nullable = false)
    public String getName() {
        return data_.getString(PROFILE_NAME);
    }

    public void setName(String s) {
        data_.setString(PROFILE_NAME, s);
    }

    @Column(name = "wpr_uuid", nullable = false, unique = true, length = 36)
    public String getUuid() {
        return data_.getString(PROFILE_UUID);
    }

    public void setUuid(String s) {
        data_.setString(PROFILE_UUID, s);
    }

    @Column(name = "wpr_email", nullable = false)
    public String getEmail() {
        return data_.getString(PROFILE_EMAIL);
    }

    public void setEmail(String s) {
        data_.setString(PROFILE_EMAIL, s);
    }

    @Column(name = "wpr_password", nullable = false)
    @JsonIgnore
    public String getPasswordHash() {
        return data_.getString(PROFILE_PASSWORD_HASH);
    }

    public void setPasswordHash(String hash) {
        data_.setString(PROFILE_PASSWORD_HASH, hash);
    }

    @Transient
    public String getPassword() {
        // Transient field for client-server message transport only (no decryption)
        return data_.getString(PROFILE_PASSWORD);
    }

    public void setPassword(String s) {
        // Store plaintext in transient field for client-server message transport only
        // Hashing is the service layer's job (OnlineProfileService.hashAndSetPassword)
        data_.setString(PROFILE_PASSWORD, s);
    }

    @Column(name = "wpr_is_retired", nullable = false)
    public boolean isRetired() {
        return data_.getBoolean(PROFILE_RETIRED, false);
    }

    public void setRetired(boolean b) {
        data_.setBoolean(PROFILE_RETIRED, b);
    }

    @Column(name = "wpr_create_date", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateDate() {
        return data_.getLongAsDate(PROFILE_CREATE_DATE);
    }

    public void setCreateDate(Date date) {
        data_.setLongFromDate(PROFILE_CREATE_DATE, date);
    }

    @Column(name = "wpr_modify_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModifyDate() {
        return data_.getLongAsDate(PROFILE_MODIFY_DATE);
    }

    public void setModifyDate(Date date) {
        data_.setLongFromDate(PROFILE_MODIFY_DATE, date);
    }

    /**
     * override equals - uses name for equality
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OnlineProfile))
            return false;
        final OnlineProfile other = (OnlineProfile) o;
        return getName().equals(other.getName());
    }

    /**
     * override hashcode
     */
    @Override
    public int hashCode() {
        String sName = getName();
        return sName == null ? super.hashCode() : sName.hashCode();
    }

    /**
     * Auto set create/modify date on insert
     */
    @PrePersist
    private void onInsert() {
        setCreateDate(new Date());
        setModifyDate(new Date());
    }

    /**
     * Auto set modify date on update
     */
    @PreUpdate
    private void onUpdate() {
        setModifyDate(new Date());
    }

    /**
     * Debug
     */
    @Override
    public String toString() {
        return "OnlineProfile: " + data_;
    }

    // ===================================================================
    // License key and activation stubs (removed in Community Edition)
    // ===================================================================

    /**
     * Returns the license key. Always returns null in Community Edition - license
     * keys are not used.
     *
     * @deprecated License functionality removed in Community Edition
     */
    @Deprecated
    @Column(name = "wpr_license_key", nullable = true, length = 19)
    public String getLicenseKey() {
        // Always return null - license keys not used in Community Edition
        return null;
    }

    /**
     * Sets the license key (no-op in Community Edition). This method exists only
     * for backward compatibility and does nothing.
     *
     * @deprecated License functionality removed in Community Edition
     */
    @Deprecated
    public void setLicenseKey(String key) {
        // No-op - don't store license keys in Community Edition
    }

    /**
     * Returns whether this profile is activated. Always returns true in Community
     * Edition - all profiles are activated.
     *
     * @deprecated Activation functionality removed in Community Edition
     */
    @Deprecated
    @Column(name = "wpr_is_activated", nullable = false)
    public boolean isActivated() {
        // Always return true - all profiles activated in Community Edition
        return true;
    }

    /**
     * Sets whether this profile is activated (no-op in Community Edition). This
     * method exists only for backward compatibility and does nothing.
     *
     * @deprecated Activation functionality removed in Community Edition
     */
    @Deprecated
    public void setActivated(boolean activated) {
        // No-op - all profiles always activated in Community Edition
    }
}
