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
package com.donohoedigital.games.poker.gameserver.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT authentication.
 */
@ConfigurationProperties(prefix = "game.server.jwt")
public class JwtProperties {

    /**
     * Path to RSA private key (PEM format). Null for validation-only mode.
     */
    private String privateKeyPath;

    /**
     * Path to RSA public key (PEM format). Required.
     */
    private String publicKeyPath;

    /**
     * Token expiration in milliseconds (regular login). Default: 1 hour.
     */
    private long expiration = 3600000L;

    /**
     * Token expiration in milliseconds (remember me). Default: 7 days.
     */
    private long rememberMeExpiration = 604800000L;

    /**
     * Cookie name for JWT token. Default: "DDPoker-JWT".
     */
    private String cookieName = "DDPoker-JWT";

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getRememberMeExpiration() {
        return rememberMeExpiration;
    }

    public void setRememberMeExpiration(long rememberMeExpiration) {
        this.rememberMeExpiration = rememberMeExpiration;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }
}
