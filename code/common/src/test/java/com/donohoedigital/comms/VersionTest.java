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
package com.donohoedigital.comms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Version - Version parsing and comparison.
 * Supports formats like: 3.1.2, 3.1a5, 3.1b10, 3.1.2d, 3.1.2_en, 3.1.2-community
 */
class VersionTest {

    // =================================================================
    // String Constructor Parsing Tests
    // =================================================================

    @Test
    void should_ParseMajorMinor_When_SimpleVersionProvided() {
        Version version = new Version("3.1");

        assertThat(version.getMajor()).isEqualTo(3);
        assertThat(version.getMinor()).isEqualTo(1);
        assertThat(version.getPatch()).isZero();
        assertThat(version.isProduction()).isTrue();
    }

    @Test
    void should_ParseMajorMinorPatch_When_FullVersionProvided() {
        Version version = new Version("3.1.2");

        assertThat(version.getMajor()).isEqualTo(3);
        assertThat(version.getMinor()).isEqualTo(1);
        assertThat(version.getPatch()).isEqualTo(2);
        assertThat(version.isProduction()).isTrue();
    }

    @Test
    void should_ParseAlphaVersion_When_AlphaStringProvided() {
        Version version = new Version("3.1a5");

        assertThat(version.getMajor()).isEqualTo(3);
        assertThat(version.getMinor()).isEqualTo(1);
        assertThat(version.isAlpha()).isTrue();
        assertThat(version.getAlphaBetaVersion()).isEqualTo(5);
        assertThat(version.isProduction()).isFalse();
    }

    @Test
    void should_ParseBetaVersion_When_BetaStringProvided() {
        Version version = new Version("3.1b10");

        assertThat(version.getMajor()).isEqualTo(3);
        assertThat(version.getMinor()).isEqualTo(1);
        assertThat(version.isBeta()).isTrue();
        assertThat(version.getAlphaBetaVersion()).isEqualTo(10);
        assertThat(version.isProduction()).isFalse();
    }

    @Test
    void should_ParseOldStylePatch_When_PatchWithPProvided() {
        Version version = new Version("3.1p2");

        assertThat(version.getMajor()).isEqualTo(3);
        assertThat(version.getMinor()).isEqualTo(1);
        assertThat(version.getPatch()).isEqualTo(2);
    }

    @Test
    void should_ParseLocale_When_LocaleSuffixProvided() {
        Version version = new Version("3.1.2_en");

        assertThat(version.getMajor()).isEqualTo(3);
        assertThat(version.getMinor()).isEqualTo(1);
        assertThat(version.getPatch()).isEqualTo(2);
        assertThat(version.getLocale()).isEqualTo("en");
    }

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateProductionVersion_When_IntConstructorUsed() {
        Version version = new Version(3, 1, 2, false);

        assertThat(version.getMajor()).isEqualTo(3);
        assertThat(version.getMinor()).isEqualTo(1);
        assertThat(version.getPatch()).isEqualTo(2);
        assertThat(version.isProduction()).isTrue();
        assertThat(version.isVerify()).isFalse();
    }

    @Test
    void should_CreateAlphaVersion_When_AlphaTypeProvided() {
        Version version = new Version(Version.TYPE_ALPHA, 3, 1, 5, 0, false);

        assertThat(version.isAlpha()).isTrue();
        assertThat(version.getAlphaBetaVersion()).isEqualTo(5);
        assertThat(version.isProduction()).isFalse();
    }

    @Test
    void should_CreateBetaVersion_When_BetaTypeProvided() {
        Version version = new Version(Version.TYPE_BETA, 3, 1, 10, 0, false);

        assertThat(version.isBeta()).isTrue();
        assertThat(version.getAlphaBetaVersion()).isEqualTo(10);
        assertThat(version.isProduction()).isFalse();
    }

    @Test
    void should_CreateVersionWithSuffix_When_SuffixProvided() {
        Version version = new Version(3, 1, 2, false, "-community");

        assertThat(version.toString()).endsWith("-community");
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_FormatSimpleVersion_When_ToStringCalled() {
        Version version = new Version("3.1");

        assertThat(version.toString()).isEqualTo("3.1");
    }

    @Test
    void should_FormatFullVersion_When_ToStringCalled() {
        Version version = new Version("3.1.2");

        assertThat(version.toString()).isEqualTo("3.1.2");
    }

    @Test
    void should_FormatAlphaVersion_When_ToStringCalled() {
        Version version = new Version("3.1a5");

        assertThat(version.toString()).isEqualTo("3.1a5");
    }

    @Test
    void should_FormatBetaVersion_When_ToStringCalled() {
        Version version = new Version("3.1b10");

        assertThat(version.toString()).isEqualTo("3.1b10");
    }

    @Test
    void should_ConvertOldStyle_When_OldStyleParsed() {
        Version version = new Version("3.1p2");

        // Old style "3.1p2" converts to new style "3.1.2"
        assertThat(version.toString()).isEqualTo("3.1.2");
    }

    // =================================================================
    // isBefore Tests - Same Major.Minor
    // =================================================================

    @Test
    void should_ReturnFalse_When_SameVersion() {
        Version v1 = new Version("3.1.2");
        Version v2 = new Version("3.1.2");

        assertThat(v1.isBefore(v2)).isFalse();
        assertThat(v2.isBefore(v1)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_LowerPatchVersion() {
        Version v1 = new Version("3.1.1");
        Version v2 = new Version("3.1.2");

        assertThat(v1.isBefore(v2)).isTrue();
        assertThat(v2.isBefore(v1)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_AlphaBeforeProduction() {
        Version alpha = new Version("3.1a1");
        Version prod = new Version("3.1");

        assertThat(alpha.isBefore(prod)).isTrue();
        assertThat(prod.isBefore(alpha)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_BetaBeforeProduction() {
        Version beta = new Version("3.1b1");
        Version prod = new Version("3.1");

        assertThat(beta.isBefore(prod)).isTrue();
        assertThat(prod.isBefore(beta)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_AlphaBeforeBeta() {
        Version alpha = new Version("3.1a5");
        Version beta = new Version("3.1b1");

        assertThat(alpha.isBefore(beta)).isTrue();
        assertThat(beta.isBefore(alpha)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_LowerAlphaVersion() {
        Version alpha1 = new Version("3.1a1");
        Version alpha2 = new Version("3.1a5");

        assertThat(alpha1.isBefore(alpha2)).isTrue();
        assertThat(alpha2.isBefore(alpha1)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_LowerBetaVersion() {
        Version beta1 = new Version("3.1b5");
        Version beta2 = new Version("3.1b10");

        assertThat(beta1.isBefore(beta2)).isTrue();
        assertThat(beta2.isBefore(beta1)).isFalse();
    }

    // =================================================================
    // isBefore Tests - Different Major.Minor
    // =================================================================

    @Test
    void should_ReturnTrue_When_LowerMajorVersion() {
        Version v1 = new Version("2.5");
        Version v2 = new Version("3.1");

        assertThat(v1.isBefore(v2)).isTrue();
        assertThat(v2.isBefore(v1)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_LowerMinorVersion() {
        Version v1 = new Version("3.0");
        Version v2 = new Version("3.1");

        assertThat(v1.isBefore(v2)).isTrue();
        assertThat(v2.isBefore(v1)).isFalse();
    }

    @Test
    void should_CompareCorrectly_When_MajorDifferent() {
        Version v1 = new Version("2.9.99");
        Version v2 = new Version("3.0");

        assertThat(v1.isBefore(v2)).isTrue();
    }

    // =================================================================
    // isAfter Tests
    // =================================================================

    @Test
    void should_ReturnFalse_When_SameVersionAfter() {
        Version v1 = new Version("3.1.2");
        Version v2 = new Version("3.1.2");

        assertThat(v1.isAfter(v2)).isFalse();
        assertThat(v2.isAfter(v1)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_HigherPatchVersion() {
        Version v1 = new Version("3.1.2");
        Version v2 = new Version("3.1.1");

        assertThat(v1.isAfter(v2)).isTrue();
        assertThat(v2.isAfter(v1)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_ProductionAfterAlpha() {
        Version prod = new Version("3.1");
        Version alpha = new Version("3.1a1");

        assertThat(prod.isAfter(alpha)).isTrue();
        assertThat(alpha.isAfter(prod)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_ProductionAfterBeta() {
        Version prod = new Version("3.1");
        Version beta = new Version("3.1b1");

        assertThat(prod.isAfter(beta)).isTrue();
        assertThat(beta.isAfter(prod)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_BetaAfterAlpha() {
        Version beta = new Version("3.1b1");
        Version alpha = new Version("3.1a5");

        assertThat(beta.isAfter(alpha)).isTrue();
        assertThat(alpha.isAfter(beta)).isFalse();
    }

    // =================================================================
    // isMajorMinorBefore Tests
    // =================================================================

    @Test
    void should_IgnorePatch_When_MajorMinorComparison() {
        Version v1 = new Version("3.1.1");
        Version v2 = new Version("3.1.9");

        // Same major.minor, so neither is "before" the other
        assertThat(v1.isMajorMinorBefore(v2)).isFalse();
        assertThat(v2.isMajorMinorBefore(v1)).isFalse();
    }

    @Test
    void should_ConsiderAlphaBeta_When_MajorMinorComparison() {
        Version alpha = new Version("3.1a1");
        Version prod = new Version("3.1");

        assertThat(alpha.isMajorMinorBefore(prod)).isTrue();
        assertThat(prod.isMajorMinorBefore(alpha)).isFalse();
    }

    @Test
    void should_CompareMinor_When_MajorMinorComparison() {
        Version v1 = new Version("3.0.5");
        Version v2 = new Version("3.1.1");

        assertThat(v1.isMajorMinorBefore(v2)).isTrue();
        assertThat(v2.isMajorMinorBefore(v1)).isFalse();
    }

    // =================================================================
    // Getters and Setters Tests
    // =================================================================

    @Test
    void should_ReturnMajorAsString_When_GetMajorAsStringCalled() {
        Version version = new Version("3.1");

        assertThat(version.getMajorAsString()).isEqualTo("3");
    }

    @Test
    void should_SetLocale_When_SetLocaleCalled() {
        Version version = new Version("3.1");
        version.setLocale("fr");

        assertThat(version.getLocale()).isEqualTo("fr");
    }

    // =================================================================
    // Edge Cases and Complex Scenarios
    // =================================================================

    @Test
    void should_HandleZeroVersions_When_Parsing() {
        Version version = new Version("0.0");

        assertThat(version.getMajor()).isZero();
        assertThat(version.getMinor()).isZero();
    }

    @Test
    void should_HandleLargeNumbers_When_Parsing() {
        Version version = new Version("99.99.99");

        assertThat(version.getMajor()).isEqualTo(99);
        assertThat(version.getMinor()).isEqualTo(99);
        assertThat(version.getPatch()).isEqualTo(99);
    }

    @Test
    void should_CompareCorrectly_When_ComplexVersions() {
        Version v1 = new Version("3.1a5.2");
        Version v2 = new Version("3.1b1.1");

        assertThat(v1.isBefore(v2)).isTrue();
    }

    @Test
    void should_BeTransitive_When_ComparingVersions() {
        Version v1 = new Version("3.0");
        Version v2 = new Version("3.1");
        Version v3 = new Version("3.2");

        assertThat(v1.isBefore(v2)).isTrue();
        assertThat(v2.isBefore(v3)).isTrue();
        assertThat(v1.isBefore(v3)).isTrue(); // Transitivity
    }

    @Test
    void should_BeSymmetric_When_ComparingVersions() {
        Version v1 = new Version("3.1");
        Version v2 = new Version("3.2");

        boolean v1BeforeV2 = v1.isBefore(v2);
        boolean v2AfterV1 = v2.isAfter(v1);

        assertThat(v1BeforeV2).isEqualTo(v2AfterV1);
    }
}
