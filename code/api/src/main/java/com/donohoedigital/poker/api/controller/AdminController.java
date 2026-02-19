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
package com.donohoedigital.poker.api.controller;

import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin endpoints - profile search. All endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private OnlineProfileService profileService;

    /**
     * Search profiles (admin).
     */
    @GetMapping("/profiles")
    public ResponseEntity<?> searchProfiles(@RequestParam(required = false) String name,
            @RequestParam(required = false) String email, @RequestParam(required = false) String key,
            @RequestParam(defaultValue = "false") boolean includeRetired, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        int offset = page * pageSize;
        int total = profileService.getMatchingOnlineProfilesCount(name, email, key, includeRetired);

        List<OnlineProfile> profiles = profileService.getMatchingOnlineProfiles(total, offset, pageSize, name, email,
                key, includeRetired);

        Map<String, Object> response = new HashMap<>();
        response.put("profiles", profiles);
        response.put("total", total);
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }
}
