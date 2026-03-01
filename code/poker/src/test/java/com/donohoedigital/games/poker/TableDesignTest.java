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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TableDesign - color getters/setters, default values, copy
 * constructor, and write/read round-trip serialization.
 */
class TableDesignTest {

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    // =================================================================
    // Default Values Tests
    // =================================================================

    @Test
    void should_HaveDefaultGreenTopColor_When_CreatedWithNoArgs() {
        TableDesign design = new TableDesign();

        // Default top color is new Color(38, 175, 23)
        assertThat(design.getColorTop()).isEqualTo(new Color(38, 175, 23));
    }

    @Test
    void should_HaveDefaultDarkGreenBottomColor_When_CreatedWithNoArgs() {
        TableDesign design = new TableDesign();

        // Default bottom color is new Color(20, 82, 1)
        assertThat(design.getColorBottom()).isEqualTo(new Color(20, 82, 1));
    }

    @Test
    void should_HaveDefaultColors_When_CreatedWithName() {
        TableDesign design = new TableDesign("MyTable");

        assertThat(design.getColorTop()).isEqualTo(new Color(38, 175, 23));
        assertThat(design.getColorBottom()).isEqualTo(new Color(20, 82, 1));
    }

    @Test
    void should_ReturnName_When_NameProvided() {
        TableDesign design = new TableDesign("Vegas");

        assertThat(design.getName()).isEqualTo("Vegas");
    }

    @Test
    void should_ReturnNullName_When_CreatedWithNoArgs() {
        TableDesign design = new TableDesign();

        assertThat(design.getName()).isNull();
    }

    // =================================================================
    // Color Getter/Setter Tests
    // =================================================================

    @Test
    void should_SetColorTop_When_SetColorTopCalled() {
        TableDesign design = new TableDesign("test");
        Color red = new Color(255, 0, 0);

        design.setColorTop(red);

        assertThat(design.getColorTop()).isEqualTo(red);
    }

    @Test
    void should_SetColorBottom_When_SetColorBottomCalled() {
        TableDesign design = new TableDesign("test");
        Color blue = new Color(0, 0, 255);

        design.setColorBottom(blue);

        assertThat(design.getColorBottom()).isEqualTo(blue);
    }

    @Test
    void should_SetBothColors_When_BothSettersCalledIndependently() {
        TableDesign design = new TableDesign("test");
        Color top = new Color(100, 150, 200);
        Color bottom = new Color(10, 20, 30);

        design.setColorTop(top);
        design.setColorBottom(bottom);

        assertThat(design.getColorTop()).isEqualTo(top);
        assertThat(design.getColorBottom()).isEqualTo(bottom);
    }

    @Test
    void should_AcceptColorWithAlpha_When_AlphaColorSet() {
        TableDesign design = new TableDesign("test");
        Color colorWithAlpha = new Color(100, 150, 200, 128);

        design.setColorTop(colorWithAlpha);

        assertThat(design.getColorTop()).isEqualTo(colorWithAlpha);
    }

    @Test
    void should_AllowNullColor_When_NullSet() {
        TableDesign design = new TableDesign("test");

        design.setColorTop(null);
        design.setColorBottom(null);

        assertThat(design.getColorTop()).isNull();
        assertThat(design.getColorBottom()).isNull();
    }

    // =================================================================
    // Copy Constructor Tests
    // =================================================================

    @Test
    void should_CopyColors_When_CopyConstructorUsed() {
        TableDesign original = new TableDesign("original");
        Color top = new Color(50, 100, 150);
        Color bottom = new Color(5, 10, 15);
        original.setColorTop(top);
        original.setColorBottom(bottom);

        TableDesign copy = new TableDesign(original);

        assertThat(copy.getColorTop()).isEqualTo(top);
        assertThat(copy.getColorBottom()).isEqualTo(bottom);
    }

    @Test
    void should_CopyColorsWithNewName_When_CopyConstructorWithNameUsed() {
        TableDesign original = new TableDesign("original");
        Color top = new Color(200, 100, 50);
        Color bottom = new Color(50, 25, 10);
        original.setColorTop(top);
        original.setColorBottom(bottom);

        TableDesign copy = new TableDesign(original, "newName");

        assertThat(copy.getColorTop()).isEqualTo(top);
        assertThat(copy.getColorBottom()).isEqualTo(bottom);
        assertThat(copy.getName()).isEqualTo("newName");
    }

    @Test
    void should_UseDefaultColors_When_CopyConstructorCalledWithNullProto() {
        // TableDesign(null, name) is what the no-arg and name constructors call
        TableDesign design = new TableDesign((TableDesign) null, "named");

        assertThat(design.getColorTop()).isEqualTo(new Color(38, 175, 23));
        assertThat(design.getColorBottom()).isEqualTo(new Color(20, 82, 1));
    }

    @Test
    void should_CopyDefaultColors_When_CopyingDesignWithDefaults() {
        TableDesign original = new TableDesign("original");
        // Don't change colors - keep defaults

        TableDesign copy = new TableDesign(original, "copy");

        assertThat(copy.getColorTop()).isEqualTo(original.getColorTop());
        assertThat(copy.getColorBottom()).isEqualTo(original.getColorBottom());
    }

    @Test
    void should_BeIndependent_When_CopiedDesignColorsChanged() {
        TableDesign original = new TableDesign("original");
        TableDesign copy = new TableDesign(original, "copy");

        // Change original's color after copy
        original.setColorTop(new Color(255, 0, 0));

        // Copy should retain the original default, not reflect the change
        assertThat(copy.getColorTop()).isEqualTo(new Color(38, 175, 23));
    }

    // =================================================================
    // Constants Tests
    // =================================================================

    @Test
    void should_HaveCorrectProfileBegin_When_ConstantChecked() {
        assertThat(TableDesign.PROFILE_BEGIN).isEqualTo("table");
    }

    @Test
    void should_HaveCorrectTableDesignDir_When_ConstantChecked() {
        assertThat(TableDesign.TABLE_DESIGN_DIR).isEqualTo("tables");
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReturnName_When_ToStringCalled() {
        TableDesign design = new TableDesign("MyDesign");

        assertThat(design.toString()).isEqualTo("MyDesign");
    }

    // =================================================================
    // Write/Read Round-Trip Tests
    // =================================================================

    @Test
    void should_PreserveColors_When_WrittenAndRead() throws IOException {
        TableDesign original = new TableDesign("RoundTrip");
        original.setCreateDate();
        Color top = new Color(200, 50, 100);
        Color bottom = new Color(10, 200, 150);
        original.setColorTop(top);
        original.setColorBottom(bottom);

        // Write to string
        StringWriter writer = new StringWriter();
        original.write(writer);
        String serialized = writer.toString();

        // Read back
        TableDesign loaded = new TableDesign("placeholder");
        loaded.read(new StringReader(serialized), true);

        assertThat(loaded.getColorTop()).isEqualTo(top);
        assertThat(loaded.getColorBottom()).isEqualTo(bottom);
    }

    @Test
    void should_PreserveName_When_WrittenAndRead() throws IOException {
        TableDesign original = new TableDesign("NamedDesign");
        original.setCreateDate();

        StringWriter writer = new StringWriter();
        original.write(writer);

        TableDesign loaded = new TableDesign("placeholder");
        loaded.read(new StringReader(writer.toString()), true);

        assertThat(loaded.getName()).isEqualTo("NamedDesign");
    }

    @Test
    void should_PreserveDefaultColors_When_WrittenAndRead() throws IOException {
        TableDesign original = new TableDesign("Defaults");
        original.setCreateDate();

        StringWriter writer = new StringWriter();
        original.write(writer);

        TableDesign loaded = new TableDesign("placeholder");
        loaded.read(new StringReader(writer.toString()), true);

        assertThat(loaded.getColorTop()).isEqualTo(new Color(38, 175, 23));
        assertThat(loaded.getColorBottom()).isEqualTo(new Color(20, 82, 1));
    }
}
