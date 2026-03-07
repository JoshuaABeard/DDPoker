/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JwtKeyManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void generateKeyPair_producesRsaKeys() {
        KeyPair keyPair = JwtKeyManager.generateKeyPair();

        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
        assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
        assertEquals("RSA", keyPair.getPublic().getAlgorithm());
    }

    @Test
    void saveAndLoadPrivateKey_roundTrip() throws Exception {
        KeyPair keyPair = JwtKeyManager.generateKeyPair();
        Path keyFile = tempDir.resolve("private.pem");

        JwtKeyManager.savePrivateKey(keyPair.getPrivate(), keyFile);
        assertTrue(Files.exists(keyFile));

        String pem = Files.readString(keyFile);
        assertTrue(pem.contains("-----BEGIN PRIVATE KEY-----"));
        assertTrue(pem.contains("-----END PRIVATE KEY-----"));

        PrivateKey loaded = JwtKeyManager.loadPrivateKey(keyFile);
        assertArrayEquals(keyPair.getPrivate().getEncoded(), loaded.getEncoded());
    }

    @Test
    void saveAndLoadPublicKey_roundTrip() throws Exception {
        KeyPair keyPair = JwtKeyManager.generateKeyPair();
        Path keyFile = tempDir.resolve("public.pem");

        JwtKeyManager.savePublicKey(keyPair.getPublic(), keyFile);
        assertTrue(Files.exists(keyFile));

        String pem = Files.readString(keyFile);
        assertTrue(pem.contains("-----BEGIN PUBLIC KEY-----"));
        assertTrue(pem.contains("-----END PUBLIC KEY-----"));

        PublicKey loaded = JwtKeyManager.loadPublicKey(keyFile);
        assertArrayEquals(keyPair.getPublic().getEncoded(), loaded.getEncoded());
    }

    @Test
    void twoGeneratedKeyPairs_areDifferent() {
        KeyPair kp1 = JwtKeyManager.generateKeyPair();
        KeyPair kp2 = JwtKeyManager.generateKeyPair();

        assertFalse(java.util.Arrays.equals(kp1.getPrivate().getEncoded(), kp2.getPrivate().getEncoded()));
    }
}
