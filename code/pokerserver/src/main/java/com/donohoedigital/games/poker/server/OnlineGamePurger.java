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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.service.*;
import org.apache.logging.log4j.*;
import org.springframework.context.support.*;

import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Command line tool to clean up old WAN games.
 */
public class OnlineGamePurger extends BaseCommandLineApp {
    private static Logger logger = LogManager.getLogger(OnlineGamePurger.class);

    private static final int DEFAULT_PERIOD_SECS = (60 * 60 * 24 * 7); // one week
    // CONCURRENCY-2: Use DateTimeFormatter instead of SimpleDateFormat
    // (thread-safe)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private Date date_ = null;
    private Integer mode_ = null;
    private OnlineGameService service;

    /**
     * Run purger.
     */
    public static void main(String[] args) {
        try {
            new OnlineGamePurger("poker", args);
        } catch (ApplicationError ae) {
            logger.error("OnlineGamePurger ending due to ApplicationError: " + ae.toString(), ae);
        } catch (Throwable t) {
            logger.error("OnlineGamePurger ending due to unexpected error", t);
        }

        System.exit(0);
    }

    /**
     * Create the purger instance.
     */
    public OnlineGamePurger(String configName, String[] args) {
        super(configName, args);

        // Set the purge information using the given options.
        String date = htOptions_.getString("date");

        if (date != null) {
            try {
                // CONCURRENCY-2: Convert LocalDate to Date
                LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
                date_ = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                CommandLine.exitWithError("Unable to parse date: " + date_);
            }
        } else {
            date_ = new Date(System.currentTimeMillis() - (htOptions_.getInteger("period") * 1000));
        }

        mode_ = htOptions_.getInteger("mode");

        switch (mode_) {
            case OnlineGame.MODE_REG :
            case OnlineGame.MODE_PLAY :
                break;

            case OnlineGame.MODE_STOP :
                CommandLine.exitWithError("Cannot purge stopped games");
                break;

            case OnlineGame.MODE_END :
                CommandLine.exitWithError("Cannot purge ended games");
                break;

            default :
                if (mode_ == 42)
                    logger.error("TESTING ERROR");
                CommandLine.exitWithError("Unknown mode: " + mode_);
        }

        // get the service from spring
        try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-pokertools.xml")) {
            service = (OnlineGameService) ctx.getBean("onlineGameService");

            // Do the work.
            doPurge();
        }
    }

    /**
     * Setup the command line options.
     */
    @Override
    protected void setupApplicationCommandLineOptions() {
        CommandLine.addIntegerOption("period", DEFAULT_PERIOD_SECS);
        CommandLine.setDescription("period", "period of time (in seconds) prior to now that games are valid",
                "604800 (one week)");

        CommandLine.addStringOption("date", null);
        CommandLine.setDescription("date", "date (YYYY-MM-DD) after which games are valid", "2005-08-01");

        CommandLine.addIntegerOption("mode", -1);
        CommandLine.setRequired("mode");
        CommandLine.setDescription("mode", "purge games in the given mode");
    }

    /**
     * Purge the records.
     */
    private void doPurge() {
        int count = service.purgeGames(date_, mode_);
        logger.info("Purged count: " + count);
    }
}
