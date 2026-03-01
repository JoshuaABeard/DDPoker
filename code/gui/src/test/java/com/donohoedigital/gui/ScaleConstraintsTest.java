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
package com.donohoedigital.gui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScaleConstraintsTest {

    @Test
    void should_HaveZeroDefaultsAndNullFont_When_ConstructedWithNoArgs() {
        ScaleConstraints c = new ScaleConstraints();
        assertThat(c.getX()).isEqualTo(0.0d);
        assertThat(c.getY()).isEqualTo(0.0d);
        assertThat(c.getScale()).isEqualTo(1.0d);
        assertThat(c.getFont()).isNull();
    }

    @Test
    void should_StoreValues_When_ConstructedWithArgs() {
        ScaleConstraints c = new ScaleConstraints(0.25d, 0.75d, 0.5d, null);
        assertThat(c.getX()).isEqualTo(0.25d);
        assertThat(c.getY()).isEqualTo(0.75d);
        assertThat(c.getScale()).isEqualTo(0.5d);
        assertThat(c.getFont()).isNull();
    }

    @Test
    void should_CopyAllFields_When_ConstructedFromAnother() {
        ScaleConstraints original = new ScaleConstraints(0.1d, 0.2d, 0.3d, null);
        ScaleConstraints copy = new ScaleConstraints(original);
        assertThat(copy.getX()).isEqualTo(0.1d);
        assertThat(copy.getY()).isEqualTo(0.2d);
        assertThat(copy.getScale()).isEqualTo(0.3d);
        assertThat(copy.getFont()).isNull();
    }

    @Test
    void should_RetainZeroDefaults_When_CopyConstructedFromNull() {
        ScaleConstraints c = new ScaleConstraints(null);
        assertThat(c.getX()).isEqualTo(0.0d);
        assertThat(c.getY()).isEqualTo(0.0d);
        assertThat(c.getScale()).isEqualTo(0.0d);
        assertThat(c.getFont()).isNull();
    }

    @Test
    void should_UpdateFields_When_SettersAreCalled() {
        ScaleConstraints c = new ScaleConstraints();
        c.setX(0.5d);
        c.setY(0.6d);
        c.setScale(2);
        c.setFont(null);
        assertThat(c.getX()).isEqualTo(0.5d);
        assertThat(c.getY()).isEqualTo(0.6d);
        assertThat(c.getScale()).isEqualTo(2.0d);
        assertThat(c.getFont()).isNull();
    }

    @Test
    void should_BeEqual_When_AllFieldsMatch() {
        ScaleConstraints a = new ScaleConstraints(0.1d, 0.2d, 0.5d, null);
        ScaleConstraints b = new ScaleConstraints(0.1d, 0.2d, 0.5d, null);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void should_NotBeEqual_When_AnyNumericFieldDiffers() {
        ScaleConstraints base = new ScaleConstraints(0.1d, 0.2d, 0.5d, null);
        assertThat(base).isNotEqualTo(new ScaleConstraints(0.9d, 0.2d, 0.5d, null));
        assertThat(base).isNotEqualTo(new ScaleConstraints(0.1d, 0.9d, 0.5d, null));
        assertThat(base).isNotEqualTo(new ScaleConstraints(0.1d, 0.2d, 0.9d, null));
    }

    @Test
    void should_NotBeEqual_When_FontDiffers() {
        java.awt.Font f1 = new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12);
        java.awt.Font f2 = new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12);
        ScaleConstraints c1 = new ScaleConstraints(0.0, 0.0, 1.0, f1);
        ScaleConstraints c2 = new ScaleConstraints(0.0, 0.0, 1.0, f2);
        // equals uses reference equality (==) for font, so two distinct Font instances
        // with the same properties are NOT equal per the current implementation
        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void should_ProduceDeepCopy_When_CloneIsCalled() {
        ScaleConstraints original = new ScaleConstraints(0.1d, 0.2d, 0.5d, null);
        ScaleConstraints clone = (ScaleConstraints) original.clone();
        assertThat(clone).isEqualTo(original);
        clone.setX(0.99d);
        assertThat(original.getX()).isEqualTo(0.1d); // mutation of clone does not affect original
    }

    @Test
    void should_IncludeCoordinatesAndScale_When_ToStringIsCalled() {
        ScaleConstraints c = new ScaleConstraints(0.25d, 0.75d, 0.5d, null);
        String s = c.toString();
        assertThat(s).contains("Scale").contains("0.25").contains("0.75").contains("0.5");
    }
}
