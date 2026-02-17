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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class for RSA key pair generation and PEM file I/O.
 */
public class JwtKeyManager {

    private static final String RSA_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /**
     * Generate a new RSA 2048-bit key pair.
     *
     * @return the generated key pair
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }

    /**
     * Save a private key to a PEM file.
     *
     * @param privateKey
     *            the private key to save
     * @param path
     *            the file path
     */
    public static void savePrivateKey(PrivateKey privateKey, Path path) throws IOException {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        Files.writeString(path, pem);
    }

    /**
     * Save a public key to a PEM file.
     *
     * @param publicKey
     *            the public key to save
     * @param path
     *            the file path
     */
    public static void savePublicKey(PublicKey publicKey, Path path) throws IOException {
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKey.getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        Files.writeString(path, pem);
    }

    /**
     * Load a private key from a PEM file.
     *
     * @param path
     *            the file path
     * @return the private key
     */
    public static PrivateKey loadPrivateKey(Path path) throws IOException, GeneralSecurityException {
        String pem = Files.readString(path);
        String key = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM);
        return factory.generatePrivate(spec);
    }

    /**
     * Load a public key from a PEM file.
     *
     * @param path
     *            the file path
     * @return the public key
     */
    public static PublicKey loadPublicKey(Path path) throws IOException, GeneralSecurityException {
        String pem = Files.readString(path);
        String key = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM);
        return factory.generatePublic(spec);
    }
}
