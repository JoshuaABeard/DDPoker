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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for CommandLine — a static argument-parsing utility.
 *
 * <p>
 * CommandLine is entirely static state. Each test resets that state via
 * reflection before running so tests are independent of each other.
 *
 * <p>
 * Paths that call {@code System.exit} (unknown option, missing required option,
 * bad integer value) cannot be tested without a SecurityManager, so those are
 * not covered here. The tests focus on the happy-path parsing API.
 */
class CommandLineTest {

    /**
     * Reset all static fields on CommandLine to their initial values before each
     * test so that option registrations and parsed values do not leak between
     * tests.
     */
    @BeforeEach
    void resetCommandLineState() throws Exception {
        setStaticField("sUsage_", null);
        setStaticField("nMinRequiredParams_", null);
        setStaticField("nMaxRequiredParams_", null);
        setStaticField("sParamName_", "[file]");
        setStaticField("sParamUsage_", "[file 1] ... [file N]");
        setStaticField("sParamDesc_", "a file");
        setStaticField("htOpts_", new HashMap<String, Object>());
        setStaticField("htValues_", new TypedHashMap());
        setStaticField("saRemainingArgs_", null);
        setStaticField("sMacFileArg_", null);
    }

    private void setStaticField(String name, Object value) throws Exception {
        Field field = CommandLine.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    // -----------------------------------------------------------------------
    // Flag arguments
    // -----------------------------------------------------------------------

    @Test
    void should_SetFlagTrue_When_FlagArgumentProvided() {
        CommandLine.addFlagOption("verbose");
        CommandLine.parseArgs(new String[]{"-verbose"});

        TypedHashMap opts = CommandLine.getOptions();
        assertThat(opts.getBoolean("verbose")).isTrue();
    }

    @Test
    void should_NotContainFlag_When_FlagNotProvided() {
        CommandLine.addFlagOption("verbose");
        CommandLine.parseArgs(new String[]{});

        TypedHashMap opts = CommandLine.getOptions();
        assertThat(opts.getBoolean("verbose")).isNull();
    }

    // -----------------------------------------------------------------------
    // String key-value arguments
    // -----------------------------------------------------------------------

    @Test
    void should_ParseStringValue_When_StringOptionProvided() {
        CommandLine.addStringOption("name", "default");
        CommandLine.parseArgs(new String[]{"-name", "Alice"});

        assertThat(CommandLine.getOptions().getString("name")).isEqualTo("Alice");
    }

    @Test
    void should_UseDefaultStringValue_When_StringOptionOmitted() {
        CommandLine.addStringOption("host", "localhost");
        CommandLine.parseArgs(new String[]{});

        assertThat(CommandLine.getOptions().getString("host")).isEqualTo("localhost");
    }

    // -----------------------------------------------------------------------
    // Integer key-value arguments
    // -----------------------------------------------------------------------

    @Test
    void should_ParseIntegerValue_When_IntegerOptionProvided() {
        CommandLine.addIntegerOption("port", 8080);
        CommandLine.parseArgs(new String[]{"-port", "9090"});

        assertThat(CommandLine.getOptions().getInteger("port")).isEqualTo(9090);
    }

    @Test
    void should_UseDefaultIntegerValue_When_IntegerOptionOmitted() {
        CommandLine.addIntegerOption("port", 8080);
        CommandLine.parseArgs(new String[]{});

        assertThat(CommandLine.getOptions().getInteger("port")).isEqualTo(8080);
    }

    // -----------------------------------------------------------------------
    // Double key-value arguments
    // -----------------------------------------------------------------------

    @Test
    void should_ParseDoubleValue_When_DoubleOptionProvided() {
        CommandLine.addDoubleOption("ratio", 1.0);
        CommandLine.parseArgs(new String[]{"-ratio", "3.14"});

        assertThat(CommandLine.getOptions().getDouble("ratio")).isEqualTo(3.14);
    }

    // -----------------------------------------------------------------------
    // Remaining (non-option) arguments
    // -----------------------------------------------------------------------

    @Test
    void should_CollectRemainingArgs_When_ArgumentsAreNotOptions() {
        CommandLine.parseArgs(new String[]{"file1.txt", "file2.txt"});

        String[] remaining = CommandLine.getRemainingArgs();
        assertThat(remaining).containsExactly("file1.txt", "file2.txt");
    }

    @Test
    void should_ReturnEmptyRemainingArgs_When_NoNonOptionArgumentsProvided() {
        CommandLine.addFlagOption("verbose");
        CommandLine.parseArgs(new String[]{"-verbose"});

        assertThat(CommandLine.getRemainingArgs()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Empty argument list
    // -----------------------------------------------------------------------

    @Test
    void should_ParseSuccessfully_When_ArgArrayIsEmpty() {
        CommandLine.addStringOption("opt", "default");
        CommandLine.parseArgs(new String[]{});

        assertThat(CommandLine.getOptions().getString("opt")).isEqualTo("default");
        assertThat(CommandLine.getRemainingArgs()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Mixed options and remaining args
    // -----------------------------------------------------------------------

    @Test
    void should_SeparateOptionsFromRemainingArgs_When_BothArePresent() {
        CommandLine.addStringOption("mode", "easy");
        CommandLine.parseArgs(new String[]{"-mode", "hard", "extra1", "extra2"});

        assertThat(CommandLine.getOptions().getString("mode")).isEqualTo("hard");
        assertThat(CommandLine.getRemainingArgs()).containsExactly("extra1", "extra2");
    }

    // -----------------------------------------------------------------------
    // Double-dash stop (bare "-" ends option processing)
    // -----------------------------------------------------------------------

    @Test
    void should_TreatAllRemainingAsArgs_When_BareDashEncountered() {
        CommandLine.addFlagOption("flag");
        // A bare "-" signals end of option processing; "-flag" after it is a plain arg
        CommandLine.parseArgs(new String[]{"-", "-flag"});

        assertThat(CommandLine.getOptions().getBoolean("flag")).isNull();
        assertThat(CommandLine.getRemainingArgs()).containsExactly("-flag");
    }
}
