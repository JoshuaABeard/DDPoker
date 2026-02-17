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
package com.donohoedigital.games.poker.gameserver.service;

import org.springframework.stereotype.Service;

import com.donohoedigital.games.poker.gameserver.persistence.repository.BanRepository;

/**
 * Service for checking if profiles or emails are banned.
 */
@Service
public class BanService {

    private final BanRepository banRepository;

    public BanService(BanRepository banRepository) {
        this.banRepository = banRepository;
    }

    /**
     * Check if a profile is currently banned.
     *
     * @param profileId
     *            the profile ID
     * @return true if profile has an active ban
     */
    public boolean isProfileBanned(Long profileId) {
        return banRepository.findByProfileId(profileId).stream().anyMatch(ban -> ban.isActive());
    }

    /**
     * Check if an email is currently banned.
     *
     * @param email
     *            the email address
     * @return true if email has an active ban
     */
    public boolean isEmailBanned(String email) {
        return banRepository.findByEmail(email).stream().anyMatch(ban -> ban.isActive());
    }
}
