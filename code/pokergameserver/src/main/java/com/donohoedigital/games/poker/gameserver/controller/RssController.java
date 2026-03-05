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
package com.donohoedigital.games.poker.gameserver.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineGameRepository;
import com.donohoedigital.games.poker.model.OnlineGame;

/**
 * RSS feed endpoints - generate RSS feeds for game lists.
 */
@RestController
@RequestMapping("/api/v1/rss")
public class RssController {

    private final OnlineGameRepository gameRepository;

    public RssController(OnlineGameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * RSS feed for games by mode. Modes: available, current, ended
     */
    @GetMapping(value = "/{mode}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getRssFeed(@PathVariable("mode") String mode) {
        List<Integer> modes = getModeList(mode);
        List<OnlineGame> games = gameRepository.findByModeIn(modes, PageRequest.of(0, 50));
        String rss = generateRss(mode, games);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(rss);
    }

    private List<Integer> getModeList(String mode) {
        return switch (mode.toLowerCase()) {
            case "available" -> List.of(OnlineGame.MODE_REG);
            case "current" -> List.of(OnlineGame.MODE_REG, OnlineGame.MODE_PLAY);
            case "ended" -> List.of(OnlineGame.MODE_END);
            default -> List.of(OnlineGame.MODE_REG, OnlineGame.MODE_PLAY, OnlineGame.MODE_END);
        };
    }

    private String generateRss(String mode, List<OnlineGame> games) {
        SimpleDateFormat rssDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        StringBuilder rss = new StringBuilder();
        rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        rss.append("<rss version=\"2.0\">\n");
        rss.append("  <channel>\n");
        rss.append("    <title>DD Poker - ").append(capitalize(mode)).append(" Games</title>\n");
        rss.append("    <link>https://www.ddpoker.com</link>\n");
        rss.append("    <description>DD Poker online games</description>\n");
        rss.append("    <lastBuildDate>").append(rssDateFormat.format(new Date())).append("</lastBuildDate>\n");

        for (OnlineGame game : games) {
            String gameName = game.getUrl() != null ? game.getUrl() : "Game " + game.getId();
            rss.append("    <item>\n");
            rss.append("      <title>").append(escapeXml(gameName)).append("</title>\n");
            rss.append("      <link>https://www.ddpoker.com/game/").append(game.getId()).append("</link>\n");
            rss.append("      <description>Host: ").append(escapeXml(game.getHostPlayer())).append("</description>\n");
            if (game.getCreateDate() != null) {
                rss.append("      <pubDate>").append(rssDateFormat.format(game.getCreateDate())).append("</pubDate>\n");
            }
            rss.append("      <guid>game-").append(game.getId()).append("</guid>\n");
            rss.append("    </item>\n");
        }

        rss.append("  </channel>\n");
        rss.append("</rss>");
        return rss.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String escapeXml(String str) {
        if (str == null)
            return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
                "&apos;");
    }
}
