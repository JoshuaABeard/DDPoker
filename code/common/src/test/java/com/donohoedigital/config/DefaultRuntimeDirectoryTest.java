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
package com.donohoedigital.config;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuntimeDirectoryTest {

    private final DefaultRuntimeDirectory dir = new DefaultRuntimeDirectory();

    @Test
    void getClientHome_returnsFilePrefsConfigDirectory() {
        File result = dir.getClientHome("poker3.3.0");

        assertThat(result.getPath()).isEqualTo(FilePrefs.getConfigDirectory());
    }

    @Test
    void getClientHome_ignoresAppNameParameter() {
        File resultA = dir.getClientHome("poker3.3.0");
        File resultB = dir.getClientHome("anything");

        assertThat(resultA.getPath()).isEqualTo(resultB.getPath());
    }

    @Test
    void getServerHome_doesNotContainDeploymentHardcode() {
        // The old workaround that patched user.home containing "root" to a hardcoded
        // deployment path has been removed; verify the path is derived from env/system
        // only.
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", "/root");
            DefaultRuntimeDirectory freshDir = new DefaultRuntimeDirectory();
            File result = freshDir.getServerHome();
            // Path should use /root or WORK env var, not any hardcoded deployment path
            assertThat(result.getPath()).contains("ddpoker");
            assertThat(result.getPath()).contains("runtime");
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
