/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Community Contributors
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
package com.donohoedigital.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerSecurityProvider}.
 */
class ServerSecurityProviderTest {

    private final ServerSecurityProvider provider = new ServerSecurityProvider();

    // -----------------------------------------------------------------------
    // k() key generation
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnNonNull_When_KCalled() {
        byte[] key = provider.k();

        assertThat(key).isNotNull();
    }

    @Test
    void should_ReturnNonEmptyArray_When_KCalled() {
        byte[] key = provider.k();

        assertThat(key).isNotEmpty();
    }

    @Test
    void should_ReturnArrayAtLeastEncryptionKeyLength_When_KCalled() {
        byte[] key = provider.k();

        assertThat(key.length).isGreaterThanOrEqualTo(provider.getEncryptionKeyLength());
    }

    @Test
    void should_ReturnConsistentResults_When_KCalledMultipleTimes() {
        byte[] key1 = provider.k();
        byte[] key2 = provider.k();

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void should_ReturnSameResultAcrossInstances_When_KCalledOnDifferentInstances() {
        ServerSecurityProvider provider2 = new ServerSecurityProvider();

        byte[] key1 = provider.k();
        byte[] key2 = provider2.k();

        assertThat(key1).isEqualTo(key2);
    }

    // -----------------------------------------------------------------------
    // Inherited configuration methods
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnSHA_When_GetHashAlgorithmCalled() {
        assertThat(provider.getHashAlgorithm()).isEqualTo("SHA");
    }

    @Test
    void should_ReturnDES_When_GetEncryptionAlgorithmCalled() {
        assertThat(provider.getEncryptionAlgorithm()).isEqualTo("DES");
    }

    @Test
    void should_ReturnPositiveLength_When_GetEncryptionKeyLengthCalled() {
        assertThat(provider.getEncryptionKeyLength()).isGreaterThan(0);
    }
}
