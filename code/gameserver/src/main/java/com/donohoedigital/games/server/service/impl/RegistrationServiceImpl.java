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
package com.donohoedigital.games.server.service.impl;

import com.donohoedigital.db.*;
import com.donohoedigital.games.server.*;
import com.donohoedigital.games.server.dao.*;
import com.donohoedigital.games.server.model.*;
import com.donohoedigital.games.server.model.util.*;
import com.donohoedigital.games.server.service.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA. User: donohoe Date: Mar 18, 2008 Time: 2:09:33 PM
 * To change this template use File | Settings | File Templates.
 */
@Service("registrationService")
public class RegistrationServiceImpl implements RegistrationService {
    private RegistrationDao dao;
    private BannedKeyDao bannedDao;

    // Cache for banned keys query (expensive operation)
    private static class BannedKeysCache {
        final List<RegInfo> data;
        final long timestamp;

        BannedKeysCache(List<RegInfo> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMillis) {
            return (System.currentTimeMillis() - timestamp) > ttlMillis;
        }
    }

    private volatile BannedKeysCache bannedKeysCache = null;
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000; // 10 minutes

    @Autowired
    public void setRegistrationDao(RegistrationDao dao) {
        this.dao = dao;
    }

    @Autowired
    public void setBannedKeyDao(BannedKeyDao bannedDao) {
        this.bannedDao = bannedDao;
    }

    /**
     * Clear the banned keys cache. Called when banned keys are modified.
     */
    public void clearBannedKeysCache() {
        bannedKeysCache = null;
    }

    @Transactional(readOnly = true)
    public int countOperatingSystem(String keyStart, String os) {
        return dao.countOperatingSystem(keyStart, os);
    }

    @Transactional(readOnly = true)
    public int countRegistrationType(String keyStart, Registration.Type type) {
        return dao.countRegistrationType(keyStart, type);
    }

    @Transactional(readOnly = true)
    public int countKeysInUse(String keyStart) {
        return dao.countKeysInUse(keyStart);
    }

    @Transactional(readOnly = true)
    public List<RegDayOfYearCount> countByDayOfYear(String keyStart) {
        return dao.countByDayOfYear(keyStart);
    }

    @Transactional(readOnly = true)
    public List<RegHourCount> countByHour(String keyStart) {
        return dao.countByHour(keyStart);
    }

    @Transactional(readOnly = true)
    public int getMatchingRegistrationsCount(String nameSearch, String emailSearch, String keySearch,
            String addressSearch) {
        return dao.getMatchingCount(nameSearch, emailSearch, keySearch, addressSearch);
    }

    @Transactional(readOnly = true)
    public PagedList<Registration> getMatchingRegistrations(Integer count, int offset, int pagesize, String nameSearch,
            String emailSearch, String keySearch, String addressSearch) {
        return dao.getMatching(count, offset, pagesize, nameSearch, emailSearch, keySearch, addressSearch);
    }

    @Transactional(readOnly = true)
    public List<Registration> getAllRegistrationsForKey(String sKey) {
        return dao.getAllForKey(sKey);
    }

    @Transactional(readOnly = true)
    public List<Registration> getBannedRegistrations() {
        return dao.getAllBanned();
    }

    @Transactional(readOnly = true)
    public List<RegInfo> getBannedKeys(String keyStart) {
        // Check cache first (cache stores all keys, we filter by keyStart below)
        BannedKeysCache cache = bannedKeysCache;
        if (cache == null || cache.isExpired(CACHE_TTL_MILLIS)) {
            // Cache miss or expired - fetch fresh data
            cache = fetchBannedKeysData();
            bannedKeysCache = cache;
        }

        // Filter cached results by keyStart if needed
        if (keyStart == null) {
            return cache.data;
        }

        List<RegInfo> filtered = new ArrayList<RegInfo>();
        for (RegInfo info : cache.data) {
            if (info.getKey().startsWith(keyStart)) {
                filtered.add(info);
            }
        }
        return filtered;
    }

    /**
     * Fetch all banned keys data (called when cache is invalid). This is an
     * expensive operation that queries all banned keys and registrations.
     */
    private BannedKeysCache fetchBannedKeysData() {
        Map<String, RegInfo> regInfoMap = new HashMap<String, RegInfo>();

        // get banned keys and create RegInfo for each
        List<BannedKey> banned = bannedDao.getAll();
        for (BannedKey bkey : banned) {
            RegInfo info = new RegInfo(bkey.getKey(), true, bkey.getComment());
            regInfoMap.put(bkey.getKey(), info);
        }

        // get registrations using banned keys and add to RegInfo
        List<Registration> bannedRegs = getBannedRegistrations();
        for (Registration reg : bannedRegs) {
            RegInfo regInfo = regInfoMap.get(reg.getLicenseKey());
            if (regInfo != null) {
                regInfo.addRegistration(reg, true);
            }
        }

        // return sorted array in cache object
        List<RegInfo> all = new ArrayList<RegInfo>(regInfoMap.values());
        Collections.sort(all);
        return new BannedKeysCache(all);
    }

    @Transactional(readOnly = true)
    public List<RegInfo> getSuspectKeys(String keyStart, int nMin) {
        Map<String, RegInfo> regInfoMap = new HashMap<String, RegInfo>();

        // get all suspect keys
        List<String> suspectKeys = dao.getAllSuspectKeys(nMin);
        for (String key : suspectKeys) {
            if (keyStart != null && !key.startsWith(keyStart))
                continue;

            // for each key, get all registration records and
            // record RegInfo data
            List<Registration> registrations = dao.getAllForKey(key);
            for (Registration reg : registrations) {
                RegInfo info = regInfoMap.get(key);

                if (info == null) {
                    info = new RegInfo(key, false, null);
                    regInfoMap.put(key, info);
                }

                info.addRegistration(reg, true);
            }
        }

        // return sorted array
        List<RegInfo> all = new ArrayList<RegInfo>(regInfoMap.values());
        Collections.sort(all);
        return all;
    }

    @Transactional
    public void saveRegistration(Registration registration) {
        // mark if this is a duplicate
        registration.setDuplicate(dao.isDuplicate(registration));

        // save the registration
        dao.save(registration);

        // if this is a registration (and not a duplicate), then mark existing rows
        // as duplicates of this. These rows are usually previous activations, but
        // the markDuplicates logic will also mark other matching registrations
        // as duplicates
        if (registration.isRegistration() && !registration.isDuplicate()) {
            dao.markDuplicates(registration);
        }
    }

    @Transactional
    public void deleteRegistration(Registration registration) {
        Registration lookup = dao.get(registration.getId());
        dao.delete(lookup);
    }
}
