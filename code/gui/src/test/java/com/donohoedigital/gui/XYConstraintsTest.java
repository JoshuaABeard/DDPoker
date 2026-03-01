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

class XYConstraintsTest {

    @Test
    void should_HaveZeroDefaults_When_ConstructedWithNoArgs() {
        XYConstraints c = new XYConstraints();
        assertThat(c.getX()).isEqualTo(0);
        assertThat(c.getY()).isEqualTo(0);
        assertThat(c.getWidth()).isEqualTo(0);
        assertThat(c.getHeight()).isEqualTo(0);
    }

    @Test
    void should_StoreValues_When_ConstructedWithArgs() {
        XYConstraints c = new XYConstraints(10, 20, 100, 200);
        assertThat(c.getX()).isEqualTo(10);
        assertThat(c.getY()).isEqualTo(20);
        assertThat(c.getWidth()).isEqualTo(100);
        assertThat(c.getHeight()).isEqualTo(200);
    }

    @Test
    void should_CopyAllFields_When_ConstructedFromAnother() {
        XYConstraints original = new XYConstraints(5, 15, 50, 150);
        XYConstraints copy = new XYConstraints(original);
        assertThat(copy.getX()).isEqualTo(5);
        assertThat(copy.getY()).isEqualTo(15);
        assertThat(copy.getWidth()).isEqualTo(50);
        assertThat(copy.getHeight()).isEqualTo(150);
    }

    @Test
    void should_RetainZeroDefaults_When_CopyConstructedFromNull() {
        XYConstraints c = new XYConstraints(null);
        assertThat(c.getX()).isEqualTo(0);
        assertThat(c.getY()).isEqualTo(0);
        assertThat(c.getWidth()).isEqualTo(0);
        assertThat(c.getHeight()).isEqualTo(0);
    }

    @Test
    void should_UpdateField_When_SettersAreCalled() {
        XYConstraints c = new XYConstraints();
        c.setX(3);
        c.setY(7);
        c.setWidth(30);
        c.setHeight(70);
        assertThat(c.getX()).isEqualTo(3);
        assertThat(c.getY()).isEqualTo(7);
        assertThat(c.getWidth()).isEqualTo(30);
        assertThat(c.getHeight()).isEqualTo(70);
    }

    @Test
    void should_BeEqual_When_AllFieldsMatch() {
        XYConstraints a = new XYConstraints(1, 2, 3, 4);
        XYConstraints b = new XYConstraints(1, 2, 3, 4);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void should_NotBeEqual_When_AnyFieldDiffers() {
        XYConstraints base = new XYConstraints(1, 2, 3, 4);
        assertThat(base).isNotEqualTo(new XYConstraints(9, 2, 3, 4));
        assertThat(base).isNotEqualTo(new XYConstraints(1, 9, 3, 4));
        assertThat(base).isNotEqualTo(new XYConstraints(1, 2, 9, 4));
        assertThat(base).isNotEqualTo(new XYConstraints(1, 2, 3, 9));
    }

    @Test
    void should_NotBeEqual_When_ComparedToNonXYConstraints() {
        XYConstraints c = new XYConstraints(1, 2, 3, 4);
        assertThat(c).isNotEqualTo("not an XYConstraints");
        assertThat(c).isNotEqualTo(null);
    }

    @Test
    void should_HaveSameHashCode_When_FieldsAreEqual() {
        XYConstraints a = new XYConstraints(1, 2, 3, 4);
        XYConstraints b = new XYConstraints(1, 2, 3, 4);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void should_ProduceDeepCopy_When_CloneIsCalled() {
        XYConstraints original = new XYConstraints(5, 10, 15, 20);
        XYConstraints clone = (XYConstraints) original.clone();
        assertThat(clone).isEqualTo(original);
        clone.setX(99);
        assertThat(original.getX()).isEqualTo(5); // mutation of clone does not affect original
    }

    @Test
    void should_IncludeAllFields_When_ToStringIsCalled() {
        XYConstraints c = new XYConstraints(1, 2, 3, 4);
        String s = c.toString();
        assertThat(s).contains("1").contains("2").contains("3").contains("4");
        assertThat(s).startsWith("XYConstraints[");
    }
}
