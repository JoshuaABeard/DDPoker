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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.comms.DDHttpClient;
import com.donohoedigital.config.PropertyConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Utility for detecting public IP address by querying external services.
 * Implements caching and fallback across multiple IP detection services.
 * <p>
 * This is used for P2P game hosting where players need to know their public IP
 * address to allow other players to connect to their hosted games.
 */
public class PublicIPDetector {
    private static final Logger logger = LogManager.getLogger(PublicIPDetector.class);

    /**
     * Interface for fetching HTTP content (for dependency injection in tests).
     */
    public interface HttpFetcher {
        /**
         * Fetch content from URL.
         *
         * @param url URL to fetch
         * @return Response content, or null if fetch fails
         */
        String fetch(String url);
    }

    // Caching fields
    private String cachedPublicIP = null;
    private long cachedPublicIPTimestamp = 0;
    private final long cacheTTLMillis;

    // Service URLs (configurable, with fallbacks)
    private final String[] ipServices;

    // HTTP fetcher (injectable for testing)
    private final HttpFetcher httpFetcher;

    // IPv4 pattern (basic validation)
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$"
    );

    // Default cache TTL: 5 minutes (configurable)
    private static final long DEFAULT_CACHE_TTL = 300000L;

    /**
     * Constructor for testing with custom HTTP fetcher.
     *
     * @param httpFetcher HTTP fetcher for mocking in tests
     */
    PublicIPDetector(HttpFetcher httpFetcher) {
        this(httpFetcher, DEFAULT_CACHE_TTL);
    }

    /**
     * Constructor for testing with custom HTTP fetcher and TTL.
     *
     * @param httpFetcher    HTTP fetcher for mocking in tests
     * @param cacheTTLMillis Cache time-to-live in milliseconds
     */
    PublicIPDetector(HttpFetcher httpFetcher, long cacheTTLMillis) {
        this.httpFetcher = httpFetcher;
        this.cacheTTLMillis = cacheTTLMillis;
        this.ipServices = new String[]{
                "https://api.ipify.org",
                "https://icanhazip.com",
                "https://checkip.amazonaws.com"
        };
    }

    /**
     * Default constructor using real HTTP client.
     */
    public PublicIPDetector() {
        this.cacheTTLMillis = PropertyConfig.getIntegerProperty(
                "settings.publicip.cache.ttl",
                (int) DEFAULT_CACHE_TTL
        );

        String primaryUrl = PropertyConfig.getStringProperty(
                "settings.publicip.service.url",
                "https://api.ipify.org",
                false
        );

        this.ipServices = new String[]{
                primaryUrl,
                "https://icanhazip.com",
                "https://checkip.amazonaws.com"
        };

        this.httpFetcher = this::fetchFromHttpService;
    }

    /**
     * Fetch public IP from external service with caching.
     * Tries multiple services with fallback if the primary service fails.
     * Thread-safe for concurrent access.
     *
     * @return Public IP address, or null if all services fail or return invalid IPs
     */
    public synchronized String fetchPublicIP() {
        // Check cache first
        if (cachedPublicIP != null &&
                (System.currentTimeMillis() - cachedPublicIPTimestamp) < cacheTTLMillis) {
            logger.debug("Returning cached public IP: {}", cachedPublicIP);
            return cachedPublicIP;
        }

        // Try each service in order
        for (String serviceUrl : ipServices) {
            try {
                logger.debug("Attempting to fetch public IP from: {}", serviceUrl);
                String ip = httpFetcher.fetch(serviceUrl);

                if (ip != null) {
                    ip = ip.trim(); // Remove whitespace/newlines

                    if (isValidPublicIP(ip)) {
                        // Cache and return
                        cachedPublicIP = ip;
                        cachedPublicIPTimestamp = System.currentTimeMillis();
                        logger.info("Successfully fetched public IP: {} from {}", ip, serviceUrl);
                        return ip;
                    } else {
                        logger.warn("Service {} returned invalid or private IP: {}", serviceUrl, ip);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch IP from {}: {}", serviceUrl, e.getMessage());
            }
        }

        // All services failed
        logger.error("All IP detection services failed");
        return null;
    }

    /**
     * Fetch IP address from HTTP service using DDHttpClient.
     * This is the real implementation (not used in tests).
     *
     * @param url URL of the IP detection service
     * @return IP address as string, or null if fetch fails
     */
    private String fetchFromHttpService(String url) {
        DDHttpClient client = null;
        BufferedReader reader = null;

        try {
            URL serviceUrl = new URL(url);
            client = new DDHttpClient(serviceUrl, null, null);

            // Read response
            InputStream is = client.getInputStream();
            if (is == null) {
                logger.error("Failed to get input stream from {}", url);
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(is));
            return reader.readLine(); // Read first line
        } catch (Exception e) {
            logger.error("Error fetching IP from {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to fetch from service", e);
        } finally {
            // Ensure resources are closed
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                logger.warn("Error closing reader: {}", e.getMessage());
            }

            try {
                if (client != null) {
                    client.close();
                }
            } catch (Exception e) {
                logger.warn("Error closing HTTP client: {}", e.getMessage());
            }
        }
    }

    /**
     * Validate that IP is:
     * 1. Valid IPv4 format
     * 2. Not a private/local IP address
     *
     * @param ip IP address to validate
     * @return true if valid public IP, false otherwise
     */
    private boolean isValidPublicIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Check IPv4 format
        if (!IPV4_PATTERN.matcher(ip).matches()) {
            return false;
        }

        // Validate octets are in range 0-255
        String[] octets = ip.split("\\.");
        for (String octet : octets) {
            int value = Integer.parseInt(octet);
            if (value < 0 || value > 255) {
                return false;
            }
        }

        // Check if private/local IP
        return !isPrivateIP(ip);
    }

    /**
     * Check if IP is a private/local/special address.
     * Includes RFC 1918 private ranges and special-use addresses.
     *
     * @param ip IP address to check
     * @return true if private/local/special IP, false otherwise
     */
    private boolean isPrivateIP(String ip) {
        String[] octets = ip.split("\\.");
        int first = Integer.parseInt(octets[0]);
        int second = Integer.parseInt(octets[1]);
        int third = Integer.parseInt(octets[2]);
        int fourth = Integer.parseInt(octets[3]);

        // 0.0.0.0 - Invalid/unspecified address
        if (first == 0 && second == 0 && third == 0 && fourth == 0) {
            return true;
        }

        // 255.255.255.255 - Broadcast address
        if (first == 255 && second == 255 && third == 255 && fourth == 255) {
            return true;
        }

        // 10.0.0.0/8 (RFC 1918 - private)
        if (first == 10) {
            return true;
        }

        // 172.16.0.0/12 (RFC 1918 - private: 172.16.0.0 - 172.31.255.255)
        if (first == 172 && second >= 16 && second <= 31) {
            return true;
        }

        // 192.168.0.0/16 (RFC 1918 - private)
        if (first == 192 && second == 168) {
            return true;
        }

        // 127.0.0.0/8 (loopback)
        if (first == 127) {
            return true;
        }

        // 169.254.0.0/16 (link-local)
        if (first == 169 && second == 254) {
            return true;
        }

        return false;
    }

    /**
     * Clear the cached IP (useful for testing or forcing refresh).
     * Thread-safe for concurrent access.
     */
    synchronized void clearCache() {
        cachedPublicIP = null;
        cachedPublicIPTimestamp = 0;
    }
}
