/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SecurityUtils — hashing, encryption, key generation.
 */
class SecurityUtilsTest {

    @AfterEach
    void resetProvider() {
        SecurityUtils.setSecurityProvider(new SecurityProvider());
    }

    // ===== hash(InputStream, key, algorithm) — known-answer SHA-256 =====

    @Test
    void should_ReturnKnownBase64_When_HashInputStreamWithSha256AndKey() throws Exception {
        byte[] key = "salt".getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));

        String result = SecurityUtils.hash(stream, key, "SHA-256");

        // SHA-256( key="salt" + data="hello" ) encoded as Base64
        assertThat(result).isEqualTo("zTGzuY7OYMtznAv3cLLeiSrgrRM/ZFUTw9g/CHV6hDo=");
    }

    @Test
    void should_ReturnKnownBase64_When_HashInputStreamWithSha256AndNullKey() throws Exception {
        InputStream stream = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));

        String result = SecurityUtils.hash(stream, null, "SHA-256");

        // SHA-256( data="hello" ) encoded as Base64
        assertThat(result).isEqualTo("LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ=");
    }

    // ===== hash(byte[], key, algorithm) — null algorithm uses default provider
    // =====

    @Test
    void should_UseSha1_When_AlgorithmIsNull() {
        byte[] key = "salt".getBytes(StandardCharsets.UTF_8);
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        // Default SecurityProvider returns "SHA" (SHA-1)
        String result = SecurityUtils.hash(data, key, null);

        // SHA-1( key="salt" + data="hello" ) encoded as Base64
        assertThat(result).isEqualTo("NM1XrMFQZy0J0MVjXAbYwvCUpBk=");
    }

    // ===== hashRaw(byte[], key, algorithm) — returns raw bytes =====

    @Test
    void should_ReturnRawBytesOfCorrectLength_When_HashRawWithSha256() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        byte[] raw = SecurityUtils.hashRaw(data, null, "SHA-256");

        assertThat(raw).hasSize(32); // SHA-256 produces 32 bytes
    }

    @Test
    void should_MatchBase64OfHashRaw_When_ComparedToHash() {
        byte[] key = "salt".getBytes(StandardCharsets.UTF_8);
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        byte[] raw = SecurityUtils.hashRaw(data, key, "SHA-256");
        String encoded = SecurityUtils.hash(data, key, "SHA-256");

        // hash() must be exactly Base64(hashRaw())
        assertThat(encoded).isEqualTo(java.util.Base64.getEncoder().encodeToString(raw));
    }

    // ===== encrypt / decrypt round-trip =====

    @Test
    void should_RecoverOriginalValue_When_EncryptThenDecrypt() {
        byte[] key = SecurityUtils.generateKey();
        byte[] plaintext = "poker-secret".getBytes(StandardCharsets.UTF_8);

        String encrypted = SecurityUtils.encrypt(plaintext, key);
        byte[] decrypted = SecurityUtils.decrypt(encrypted, key);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void should_ProduceDifferentCiphertext_When_EncryptedWithDifferentKeys() {
        byte[] key1 = SecurityUtils.generateKey();
        byte[] key2 = SecurityUtils.generateKey();
        byte[] plaintext = "same-data".getBytes(StandardCharsets.UTF_8);

        String encrypted1 = SecurityUtils.encrypt(plaintext, key1);
        String encrypted2 = SecurityUtils.encrypt(plaintext, key2);

        // Two distinct random keys almost certainly yield different ciphertext
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    // ===== generateKey() =====

    @Test
    void should_ReturnNonNullKey_When_GenerateKeyCalled() {
        byte[] key = SecurityUtils.generateKey();

        assertThat(key).isNotNull();
        assertThat(key.length).isGreaterThan(0);
    }

    @Test
    void should_BeUsableForEncryptDecrypt_When_GeneratedKeyUsed() {
        byte[] key = SecurityUtils.generateKey();
        byte[] data = "test-round-trip".getBytes(StandardCharsets.UTF_8);

        String encrypted = SecurityUtils.encrypt(data, key);
        byte[] decrypted = SecurityUtils.decrypt(encrypted, key);

        assertThat(decrypted).isEqualTo(data);
    }

    // ===== getMD5Hash(String, byte[]) =====

    @Test
    void should_ReturnKnownBase64_When_GetMD5HashWithKey() {
        byte[] key = "salt".getBytes(StandardCharsets.UTF_8);

        String result = SecurityUtils.getMD5Hash("hello", key);

        // MD5( key="salt" + data="hello" ) encoded as Base64
        assertThat(result).isEqualTo("Bt7MiwlXJPgBA3EsI1WGvg==");
    }

    @Test
    void should_ReturnKnownBase64_When_GetMD5HashWithNullKey() {
        String result = SecurityUtils.getMD5Hash("hello", null);

        // MD5( data="hello" ) encoded as Base64
        assertThat(result).isEqualTo("XUFAKrxLKna5cZ2REBfFkg==");
    }

    // ===== Custom SecurityProvider — override hash algorithm =====

    @Test
    void should_UseSha256_When_ProviderOverridesHashAlgorithm() {
        SecurityUtils.setSecurityProvider(new SecurityProvider() {
            @Override
            public String getHashAlgorithm() {
                return "SHA-256";
            }
        });

        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        // null algorithm falls back to provider, which now returns SHA-256
        String result = SecurityUtils.hash(data, null, null);

        assertThat(result).isEqualTo("LPJNul+wow4m6DsqxbninhsWHlwfp0JecwQzYpOLmCQ=");
    }

    // ===== encryptKey / decryptKey round-trip (uses internal provider key) =====

    @Test
    void should_RecoverKey_When_EncryptKeyThenDecryptKey() {
        byte[] original = SecurityUtils.generateKey();

        String stored = SecurityUtils.encryptKey(original);
        byte[] recovered = SecurityUtils.decryptKey(stored);

        assertThat(recovered).isEqualTo(original);
    }
}
