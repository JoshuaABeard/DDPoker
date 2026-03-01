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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.db.model.BaseModel;
import jakarta.persistence.*;

import java.util.Date;

/**
 * Represents a saved tournament configuration template. Stores game
 * configuration as a JSON blob associated with a player profile.
 */
@Entity
@Table(name = "wan_template")
public class TournamentTemplate implements BaseModel<Long> {

    private Long id;
    private Long profileId;
    private String name;
    private String config;
    private Date createDate;
    private Date modifyDate;

    /**
     * Creates an uninitialized instance.
     */
    public TournamentTemplate() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wtp_id", nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "wtp_profile_id", nullable = false)
    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    @Column(name = "wtp_name", nullable = false, length = 100)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "wtp_config", nullable = false, columnDefinition = "TEXT")
    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    @Column(name = "wtp_create_date", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "wtp_modify_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    /**
     * Auto set create/modify date on insert.
     */
    @PrePersist
    private void onInsert() {
        setCreateDate(new Date());
        setModifyDate(new Date());
    }

    /**
     * Auto set modify date on update.
     */
    @PreUpdate
    private void onUpdate() {
        setModifyDate(new Date());
    }

    @Override
    public String toString() {
        return "TournamentTemplate{id=" + id + ", profileId=" + profileId + ", name='" + name + "'}";
    }
}
