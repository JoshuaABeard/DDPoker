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

import com.donohoedigital.games.poker.model.util.TournamentHistoryList;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.service.TournamentHistoryService;
import com.donohoedigital.games.poker.model.OnlineProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * Tournament history endpoints - player statistics and history.
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private TournamentHistoryService historyService;

    @Autowired
    private OnlineProfileService profileService;

    /**
     * Get tournament history for a player.
     */
    @GetMapping
    public ResponseEntity<TournamentHistoryList> getHistory(@RequestParam String name,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int pageSize) {

        // Lookup profile by name
        OnlineProfile profile = profileService.getOnlineProfileByName(name);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        int offset = page * pageSize;
        int total = historyService.getAllTournamentHistoriesForProfileCount(profile.getId(), null, from, to);

        TournamentHistoryList history = historyService.getAllTournamentHistoriesForProfile(total, offset, pageSize,
                profile.getId(), null, from, to);

        return ResponseEntity.ok(history);
    }
}
