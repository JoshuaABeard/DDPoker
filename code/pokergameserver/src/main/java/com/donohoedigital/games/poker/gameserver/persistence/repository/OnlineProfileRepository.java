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
package com.donohoedigital.games.poker.gameserver.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Spring Data JPA repository for {@link OnlineProfile}.
 *
 * <p>
 * Used for authentication (login) and profile CRUD operations.
 * </p>
 */
public interface OnlineProfileRepository extends JpaRepository<OnlineProfile, Long> {

    /**
     * Find a profile by username (case-sensitive).
     *
     * @param name
     *            the username
     * @return the profile, if found
     */
    Optional<OnlineProfile> findByName(String name);

    /**
     * Find a profile by email address.
     *
     * @param email
     *            the email address
     * @return the profile, if found
     */
    Optional<OnlineProfile> findByEmail(String email);

    /**
     * Check if a username already exists.
     *
     * @param name
     *            the username
     * @return true if the username is taken
     */
    boolean existsByName(String name);

    /**
     * Check if an email address is already registered.
     *
     * @param email
     *            the email address
     * @return true if the email is in use
     */
    boolean existsByEmail(String email);
}
