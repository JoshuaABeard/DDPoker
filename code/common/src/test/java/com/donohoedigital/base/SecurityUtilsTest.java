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
package com.donohoedigital.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SecurityUtils - cryptographic utility methods for hashing,
 * encryption, and key generation
 */
class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        // Reset to default provider before each test
        SecurityUtils.setSecurityProvider(new SecurityProvider());
    }

    // =================================================================
    // Hash Tests (InputStream)
    // =================================================================

    @Test
    void should_HashInputStream_When_NoKeyOrAlgorithm() throws IOException {
        byte[] data = "Hello World".getBytes();
        InputStream stream = new ByteArrayInputStream(data);

        String hash = SecurityUtils.hash(stream, null, null);

        assertThat(hash).isNotEmpty();
        // Hash should be Base64 encoded
        assertThat(hash).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void should_HashInputStream_When_KeyProvided() throws IOException {
        byte[] data = "Hello World".getBytes();
        byte[] key = "salt".getBytes();
        InputStream stream = new ByteArrayInputStream(data);

        String hash = SecurityUtils.hash(stream, key, null);

        assertThat(hash).isNotEmpty();
        assertThat(hash).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void should_HashInputStream_When_CustomAlgorithm() throws IOException {
        byte[] data = "Hello World".getBytes();
        InputStream stream = new ByteArrayInputStream(data);

        String hash = SecurityUtils.hash(stream, null, "MD5");

        assertThat(hash).isNotEmpty();
        assertThat(hash).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void should_ProduceDifferentHashes_When_DifferentKeys() throws IOException {
        byte[] data = "Hello World".getBytes();
        byte[] key1 = "salt1".getBytes();
        byte[] key2 = "salt2".getBytes();

        String hash1 = SecurityUtils.hash(new ByteArrayInputStream(data), key1, null);
        String hash2 = SecurityUtils.hash(new ByteArrayInputStream(data), key2, null);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void should_ProduceSameHash_When_SameInputAndKey() throws IOException {
        byte[] data = "Hello World".getBytes();
        byte[] key = "salt".getBytes();

        String hash1 = SecurityUtils.hash(new ByteArrayInputStream(data), key, null);
        String hash2 = SecurityUtils.hash(new ByteArrayInputStream(data), key, null);

        assertThat(hash1).isEqualTo(hash2);
    }

    // =================================================================
    // hashRaw Tests (InputStream)
    // =================================================================

    @Test
    void should_ReturnRawHash_When_HashRawCalled() throws IOException {
        byte[] data = "Hello World".getBytes();
        InputStream stream = new ByteArrayInputStream(data);

        byte[] rawHash = SecurityUtils.hashRaw(stream, null, null);

        assertThat(rawHash).isNotEmpty();
        // SHA hash should be 20 bytes
        assertThat(rawHash).hasSize(20);
    }

    @Test
    void should_ReturnMD5RawHash_When_MD5Algorithm() throws IOException {
        byte[] data = "Hello World".getBytes();
        InputStream stream = new ByteArrayInputStream(data);

        byte[] rawHash = SecurityUtils.hashRaw(stream, null, "MD5");

        assertThat(rawHash).isNotEmpty();
        // MD5 hash should be 16 bytes
        assertThat(rawHash).hasSize(16);
    }

    // =================================================================
    // Hash Tests (byte array)
    // =================================================================

    @Test
    void should_HashByteArray_When_NoKeyOrAlgorithm() {
        byte[] data = "Hello World".getBytes();

        String hash = SecurityUtils.hash(data, null, null);

        assertThat(hash).isNotEmpty();
        assertThat(hash).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void should_HashByteArray_When_KeyProvided() {
        byte[] data = "Hello World".getBytes();
        byte[] key = "salt".getBytes();

        String hash = SecurityUtils.hash(data, key, null);

        assertThat(hash).isNotEmpty();
        assertThat(hash).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void should_HashByteArray_When_CustomAlgorithm() {
        byte[] data = "Hello World".getBytes();

        String hash = SecurityUtils.hash(data, null, "MD5");

        assertThat(hash).isNotEmpty();
        assertThat(hash).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void should_ProduceSameHash_When_StreamAndByteArraySame() throws IOException {
        byte[] data = "Hello World".getBytes();
        byte[] key = "salt".getBytes();

        String hashStream = SecurityUtils.hash(new ByteArrayInputStream(data), key, "SHA");
        String hashByteArray = SecurityUtils.hash(data, key, "SHA");

        assertThat(hashStream).isEqualTo(hashByteArray);
    }

    // =================================================================
    // hashRaw Tests (byte array)
    // =================================================================

    @Test
    void should_ReturnRawHashByteArray_When_HashRawCalled() {
        byte[] data = "Hello World".getBytes();

        byte[] rawHash = SecurityUtils.hashRaw(data, null, null);

        assertThat(rawHash).isNotEmpty();
        assertThat(rawHash).hasSize(20); // SHA hash
    }

    @Test
    void should_ReturnMD5RawHashByteArray_When_MD5Algorithm() {
        byte[] data = "Hello World".getBytes();

        byte[] rawHash = SecurityUtils.hashRaw(data, null, "MD5");

        assertThat(rawHash).isNotEmpty();
        assertThat(rawHash).hasSize(16); // MD5 hash
    }

    // =================================================================
    // Encryption/Decryption Tests
    // =================================================================

    @Test
    void should_EncryptAndDecrypt_When_ValidKeyUsed() {
        byte[] data = "Secret Message".getBytes();
        byte[] key = SecurityUtils.generateKey();

        String encrypted = SecurityUtils.encrypt(data, key);
        byte[] decrypted = SecurityUtils.decrypt(encrypted, key);

        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).matches("[A-Za-z0-9+/=]+");
        assertThat(decrypted).isEqualTo(data);
        assertThat(new String(decrypted)).isEqualTo("Secret Message");
    }

    @Test
    void should_ProduceDifferentCiphertext_When_DifferentKeys() {
        byte[] data = "Secret Message".getBytes();
        byte[] key1 = SecurityUtils.generateKey();
        byte[] key2 = SecurityUtils.generateKey();

        String encrypted1 = SecurityUtils.encrypt(data, key1);
        String encrypted2 = SecurityUtils.encrypt(data, key2);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void should_EncryptEmptyData_When_EmptyByteArray() {
        byte[] data = new byte[0];
        byte[] key = SecurityUtils.generateKey();

        String encrypted = SecurityUtils.encrypt(data, key);
        byte[] decrypted = SecurityUtils.decrypt(encrypted, key);

        assertThat(encrypted).isNotEmpty(); // Still has padding
        assertThat(decrypted).isEmpty();
    }

    // =================================================================
    // encryptRaw/decryptRaw Tests
    // =================================================================

    @Test
    void should_EncryptRawAndDecryptRaw_When_ValidKey() {
        byte[] data = "Secret Message".getBytes();
        byte[] key = SecurityUtils.generateKey();

        byte[] encryptedRaw = SecurityUtils.encryptRaw(data, key);
        byte[] decryptedRaw = SecurityUtils.decryptRaw(encryptedRaw, key);

        assertThat(encryptedRaw).isNotEmpty();
        assertThat(decryptedRaw).isEqualTo(data);
    }

    @Test
    void should_MatchEncryptDecrypt_When_RawAndEncodedVersions() {
        byte[] data = "Secret Message".getBytes();
        byte[] key = SecurityUtils.generateKey();

        // Test that encrypt() is just Base64(encryptRaw())
        String encrypted = SecurityUtils.encrypt(data, key);
        byte[] encryptedRaw = SecurityUtils.encryptRaw(data, key);
        String encryptedFromRaw = Base64.getEncoder().encodeToString(encryptedRaw);

        assertThat(encrypted).isEqualTo(encryptedFromRaw);
    }

    // =================================================================
    // Key Generation Tests
    // =================================================================

    @Test
    void should_GenerateKey_When_Called() {
        byte[] key = SecurityUtils.generateKey();

        assertThat(key).isNotNull();
        assertThat(key).isNotEmpty();
        // DES key should be 8 bytes
        assertThat(key).hasSize(8);
    }

    @Test
    void should_GenerateDifferentKeys_When_CalledMultipleTimes() {
        byte[] key1 = SecurityUtils.generateKey();
        byte[] key2 = SecurityUtils.generateKey();

        assertThat(key1).isNotEqualTo(key2);
    }

    // =================================================================
    // Key Encryption/Decryption Tests
    // =================================================================

    @Test
    void should_EncryptAndDecryptKey_When_KeyProvided() {
        byte[] originalKey = SecurityUtils.generateKey();

        String encryptedKey = SecurityUtils.encryptKey(originalKey);
        byte[] decryptedKey = SecurityUtils.decryptKey(encryptedKey);

        assertThat(encryptedKey).isNotEmpty();
        assertThat(encryptedKey).matches("[A-Za-z0-9+/=]+");
        assertThat(decryptedKey).isEqualTo(originalKey);
    }

    @Test
    void should_BeReversible_When_KeyEncryptedAndDecrypted() {
        byte[] key1 = SecurityUtils.generateKey();
        byte[] key2 = SecurityUtils.generateKey();

        String encrypted1 = SecurityUtils.encryptKey(key1);
        String encrypted2 = SecurityUtils.encryptKey(key2);
        byte[] decrypted1 = SecurityUtils.decryptKey(encrypted1);
        byte[] decrypted2 = SecurityUtils.decryptKey(encrypted2);

        assertThat(decrypted1).isEqualTo(key1);
        assertThat(decrypted2).isEqualTo(key2);
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    // =================================================================
    // SecureRandom Tests
    // =================================================================

    @Test
    void should_GetSecureRandom_When_Called() {
        SecureRandom random = SecurityUtils.getSecureRandom();

        assertThat(random).isNotNull();
    }

    @Test
    void should_ReturnSameInstance_When_CalledMultipleTimes() {
        SecureRandom random1 = SecurityUtils.getSecureRandom();
        SecureRandom random2 = SecurityUtils.getSecureRandom();

        // Should be same instance (singleton pattern)
        assertThat(random1).isSameAs(random2);
    }

    @Test
    void should_GenerateRandomBytes_When_SecureRandomUsed() {
        SecureRandom random = SecurityUtils.getSecureRandom();

        byte[] bytes1 = new byte[16];
        byte[] bytes2 = new byte[16];
        random.nextBytes(bytes1);
        random.nextBytes(bytes2);

        // Should be different (extremely unlikely to be same)
        assertThat(bytes1).isNotEqualTo(bytes2);
    }

    // =================================================================
    // MD5 Hash Tests
    // =================================================================

    @Test
    void should_GetMD5Hash_When_StringProvided() {
        String input = "Hello World";
        byte[] key = "salt".getBytes();

        String hash = SecurityUtils.getMD5Hash(input, key);

        assertThat(hash).isNotEmpty();
        assertThat(hash).matches("[A-Za-z0-9+/=]+");
    }

    @Test
    void should_ProduceSameMD5Hash_When_SameInputAndKey() {
        String input = "Hello World";
        byte[] key = "salt".getBytes();

        String hash1 = SecurityUtils.getMD5Hash(input, key);
        String hash2 = SecurityUtils.getMD5Hash(input, key);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void should_ProduceDifferentMD5Hash_When_DifferentKeys() {
        String input = "Hello World";
        byte[] key1 = "salt1".getBytes();
        byte[] key2 = "salt2".getBytes();

        String hash1 = SecurityUtils.getMD5Hash(input, key1);
        String hash2 = SecurityUtils.getMD5Hash(input, key2);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void should_MatchHashMethod_When_MD5HashCalled() {
        String input = "Hello World";
        byte[] key = "salt".getBytes();

        String md5Hash = SecurityUtils.getMD5Hash(input, key);
        String regularHash = SecurityUtils.hash(input.getBytes(), key, "MD5");

        assertThat(md5Hash).isEqualTo(regularHash);
    }

    // =================================================================
    // Custom SecurityProvider Tests
    // =================================================================

    @Test
    void should_UseCustomProvider_When_ProviderSet() {
        SecurityProvider customProvider = new SecurityProvider() {
            @Override
            public String getHashAlgorithm() {
                return "MD5";
            }
        };

        SecurityUtils.setSecurityProvider(customProvider);

        byte[] data = "Hello".getBytes();
        byte[] hash = SecurityUtils.hashRaw(data, null, null);

        // MD5 produces 16 bytes, SHA produces 20 bytes
        assertThat(hash).hasSize(16);

        // Reset to default
        SecurityUtils.setSecurityProvider(new SecurityProvider());
    }

    // =================================================================
    // Edge Cases
    // =================================================================

    @Test
    void should_HashLargeData_When_LargeByteArray() {
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        String hash = SecurityUtils.hash(largeData, null, null);

        assertThat(hash).isNotEmpty();
    }

    @Test
    void should_EncryptLargeData_When_LargeByteArray() {
        byte[] largeData = new byte[1000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        byte[] key = SecurityUtils.generateKey();

        String encrypted = SecurityUtils.encrypt(largeData, key);
        byte[] decrypted = SecurityUtils.decrypt(encrypted, key);

        assertThat(decrypted).isEqualTo(largeData);
    }
}
