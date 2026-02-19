/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration data and logic for community game hosting.
 *
 * <p>
 * Encapsulates:
 * <ul>
 * <li>Port preference persistence — the port that players connect to for
 * forwarding</li>
 * <li>WebSocket URL construction — the URL shared with friends to join</li>
 * <li>LAN and public IP detection — uses {@link PublicIPDetector} for the
 * public IP</li>
 * </ul>
 *
 * <p>
 * Replaces the P2P-wired {@code OnlineConfiguration.java} as the data layer for
 * community hosting setup. UI phases that present the hosting dialog inject
 * this class and call its methods directly.
 */
public class CommunityHostingConfig {

    private static final Logger logger = LogManager.getLogger(CommunityHostingConfig.class);

    /** Default port for community hosting. Matches the legacy P2P server port. */
    public static final int DEFAULT_COMMUNITY_PORT = 11885;

    /** Preferences key for the user's preferred community hosting port. */
    static final String PREF_COMMUNITY_PORT = "communityHostingPort";

    private final PublicIPDetector publicIPDetector;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Production constructor — uses the real {@link PublicIPDetector}. */
    public CommunityHostingConfig() {
        this.publicIPDetector = new PublicIPDetector();
    }

    /**
     * Package-private constructor for testing — allows injecting a stub
     * {@link PublicIPDetector}.
     */
    CommunityHostingConfig(PublicIPDetector publicIPDetector) {
        this.publicIPDetector = publicIPDetector;
    }

    // -------------------------------------------------------------------------
    // WebSocket URL construction
    // -------------------------------------------------------------------------

    /**
     * Constructs the WebSocket URL that players use to join a community-hosted
     * game.
     *
     * @param publicIp
     *            IP address of the hosting machine (public or LAN)
     * @param port
     *            port the embedded server is listening on
     * @param gameId
     *            server-assigned game identifier
     * @return WebSocket URL in the form {@code ws://ip:port/ws/games/gameId}
     */
    public static String buildGameUrl(String publicIp, int port, String gameId) {
        return "ws://" + publicIp + ":" + port + "/ws/games/" + gameId;
    }

    // -------------------------------------------------------------------------
    // IP detection
    // -------------------------------------------------------------------------

    /**
     * Detects the machine's public IP using external HTTP services via
     * {@link PublicIPDetector}.
     *
     * @return detected public IP address, or {@code null} if all services fail
     */
    public String detectPublicIp() {
        return publicIPDetector.fetchPublicIP();
    }

    /**
     * Returns the machine's LAN (local network) IPv4 address. Iterates over active,
     * non-loopback network interfaces and returns the first IPv4 address found.
     * Falls back to {@code "127.0.0.1"} if no suitable address is available (e.g.
     * in headless CI environments).
     */
    public static String detectLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // IPv4 addresses have a 4-byte address array
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not determine LAN IP", e);
        }
        return "127.0.0.1";
    }

    // -------------------------------------------------------------------------
    // Port preference persistence
    // -------------------------------------------------------------------------

    /**
     * Loads the user's preferred community hosting port from the given preferences
     * node. Returns {@link #DEFAULT_COMMUNITY_PORT} if no preference has been
     * saved.
     *
     * @param prefs
     *            the preferences node to read from (use
     *            {@code Prefs.getUserPrefs("communityhosting")} in production)
     */
    public static int loadPort(Preferences prefs) {
        return prefs.getInt(PREF_COMMUNITY_PORT, DEFAULT_COMMUNITY_PORT);
    }

    /**
     * Persists the user's preferred community hosting port to the given preferences
     * node.
     *
     * @param prefs
     *            the preferences node to write to (use
     *            {@code Prefs.getUserPrefs("communityhosting")} in production)
     * @param port
     *            the port to save
     */
    public static void savePort(Preferences prefs, int port) {
        prefs.putInt(PREF_COMMUNITY_PORT, port);
    }
}
