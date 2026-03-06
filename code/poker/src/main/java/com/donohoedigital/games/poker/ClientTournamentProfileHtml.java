/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.model.TournamentProfile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflection-based bridge to {@code pokerengine.TournamentProfileHtml}.
 * Eliminates compile-time imports from
 * {@code com.donohoedigital.games.poker.engine} while preserving tournament
 * profile HTML rendering functionality.
 */
public final class ClientTournamentProfileHtml {

    private static final Logger logger = LogManager.getLogger(ClientTournamentProfileHtml.class);

    private static volatile boolean initialized;
    private static boolean available;
    private static Constructor<?> ctor;
    private static Method getProfile;
    private static Method toHTMLSummary;
    private static Method toHTML;
    private static Method toHTMLSpots;
    private static Method toHTMLOnline;
    private static Method getSpotHTML;
    private static Method getBlindsText;

    private final Object delegate;

    public ClientTournamentProfileHtml(TournamentProfile profile) {
        ensureInitialized();
        Object d = null;
        if (available) {
            try {
                d = ctor.newInstance(profile);
            } catch (Exception e) {
                logger.warn("Failed to create TournamentProfileHtml instance", e);
            }
        }
        this.delegate = d;
    }

    public TournamentProfile getProfile() {
        if (delegate == null)
            return null;
        try {
            return (TournamentProfile) getProfile.invoke(delegate);
        } catch (Exception e) {
            return null;
        }
    }

    public String toHTMLSummary(boolean bListMode, String sLocale) {
        if (delegate == null)
            return "";
        try {
            return (String) toHTMLSummary.invoke(delegate, bListMode, sLocale);
        } catch (Exception e) {
            return "";
        }
    }

    public String toHTML(String sLocale) {
        if (delegate == null)
            return "";
        try {
            return (String) toHTML.invoke(delegate, sLocale);
        } catch (Exception e) {
            return "";
        }
    }

    public String toHTMLSpots() {
        if (delegate == null)
            return "";
        try {
            return (String) toHTMLSpots.invoke(delegate);
        } catch (Exception e) {
            return "";
        }
    }

    public String toHTMLOnline() {
        if (delegate == null)
            return "";
        try {
            return (String) toHTMLOnline.invoke(delegate);
        } catch (Exception e) {
            return "";
        }
    }

    public String getSpotHTML(int i, boolean bShowPercAndEstimate, String sExtraKey) {
        if (delegate == null)
            return "";
        try {
            return (String) getSpotHTML.invoke(delegate, i, bShowPercAndEstimate, sExtraKey);
        } catch (Exception e) {
            return "";
        }
    }

    public String getBlindsText(String prefix, int nLevel, boolean briefAmounts) {
        if (delegate == null)
            return "";
        try {
            return (String) getBlindsText.invoke(delegate, prefix, nLevel, briefAmounts);
        } catch (Exception e) {
            return "";
        }
    }

    private static synchronized void ensureInitialized() {
        if (initialized)
            return;
        initialized = true;
        try {
            Class<?> clazz = Class.forName("com.donohoedigital.games.poker.engine.TournamentProfileHtml");
            ctor = clazz.getConstructor(TournamentProfile.class);
            getProfile = clazz.getMethod("getProfile");
            toHTMLSummary = clazz.getMethod("toHTMLSummary", boolean.class, String.class);
            toHTML = clazz.getMethod("toHTML", String.class);
            toHTMLSpots = clazz.getMethod("toHTMLSpots");
            toHTMLOnline = clazz.getMethod("toHTMLOnline");
            getSpotHTML = clazz.getMethod("getSpotHTML", int.class, boolean.class, String.class);
            getBlindsText = clazz.getMethod("getBlindsText", String.class, int.class, boolean.class);
            available = true;
        } catch (Exception e) {
            logger.warn("TournamentProfileHtml not available on classpath: {}", e.getMessage());
            available = false;
        }
    }
}
