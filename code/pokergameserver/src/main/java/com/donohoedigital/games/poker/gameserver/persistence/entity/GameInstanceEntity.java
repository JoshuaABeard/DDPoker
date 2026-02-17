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

import com.donohoedigital.games.poker.gameserver.GameInstanceState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for game instances. Stores game metadata and configuration.
 *
 * <p>
 * The game configuration (GameConfig) is stored as JSON in the profileData
 * column.
 * </p>
 */
@Entity
@Table(name = "game_instances")
public class GameInstanceEntity {

    @Id
    @Column(name = "game_id", length = 64)
    private String gameId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private GameInstanceState status;

    @Column(name = "owner_profile_id", nullable = false)
    private Long ownerProfileId;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "profile_data", nullable = false, columnDefinition = "TEXT")
    private String profileData;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "player_count", nullable = false)
    private int playerCount;

    @Column(name = "hosting_type", nullable = false, length = 20)
    private String hostingType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Getters and setters

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GameInstanceState getStatus() {
        return status;
    }

    public void setStatus(GameInstanceState status) {
        this.status = status;
    }

    public Long getOwnerProfileId() {
        return ownerProfileId;
    }

    public void setOwnerProfileId(Long ownerProfileId) {
        this.ownerProfileId = ownerProfileId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getProfileData() {
        return profileData;
    }

    public void setProfileData(String profileData) {
        this.profileData = profileData;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public String getHostingType() {
        return hostingType;
    }

    public void setHostingType(String hostingType) {
        this.hostingType = hostingType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
