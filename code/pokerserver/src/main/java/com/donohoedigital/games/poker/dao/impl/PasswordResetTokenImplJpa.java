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
package com.donohoedigital.games.poker.dao.impl;

import com.donohoedigital.db.dao.impl.JpaBaseDao;
import com.donohoedigital.games.poker.dao.PasswordResetTokenDao;
import com.donohoedigital.games.poker.model.PasswordResetToken;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * PasswordResetToken DAO implementation
 */
@Repository
public class PasswordResetTokenImplJpa extends JpaBaseDao<PasswordResetToken, Long> implements PasswordResetTokenDao {

    @Override
    @SuppressWarnings({"unchecked"})
    public PasswordResetToken findByToken(String token) {
        if (token == null) {
            return null;
        }

        Query query = entityManager.createQuery("select t from PasswordResetToken t " + "where t.token = :token");
        query.setParameter("token", token);

        List<PasswordResetToken> list = (List<PasswordResetToken>) query.getResultList();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public int deleteExpiredAndUsedTokens() {
        Query query = entityManager
                .createQuery("delete from PasswordResetToken t " + "where t.used = true or t.expiryDate < :now");
        query.setParameter("now", Instant.now());

        return query.executeUpdate();
    }

    @Override
    public int invalidateTokensForProfile(Long profileId) {
        if (profileId == null) {
            return 0;
        }

        Query query = entityManager.createQuery("update PasswordResetToken t " + "set t.used = true "
                + "where t.profileId = :profileId and t.used = false");
        query.setParameter("profileId", profileId);

        return query.executeUpdate();
    }
}
