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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ZipUtil — a ZipFile wrapper for reading ZIP archives entry by
 * entry.
 *
 * <p>
 * Note: ZipUtil is a file-level ZIP reader (wraps java.util.zip.ZipFile). The
 * task description originally asked for byte-level compress/decompress tests,
 * but no such API exists in this class. These tests cover the actual
 * file-reading API instead.
 */
class ZipUtilTest {

    @TempDir
    Path tempDir;

    /** Build a ZIP file containing the given entries and return its File handle. */
    private File buildZip(String... nameAndContentPairs) throws IOException {
        File zipFile = tempDir.resolve("test.zip").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (int i = 0; i < nameAndContentPairs.length; i += 2) {
                String name = nameAndContentPairs[i];
                byte[] content = nameAndContentPairs[i + 1].getBytes(StandardCharsets.UTF_8);
                zos.putNextEntry(new ZipEntry(name));
                zos.write(content);
                zos.closeEntry();
            }
        }
        return zipFile;
    }

    @Test
    void should_ReturnEntryName_When_ZipContainsSingleEntry() throws IOException {
        File zip = buildZip("hello.txt", "Hello World");
        ZipUtil zu = new ZipUtil(zip);

        String name = zu.getNextFileName();
        zu.close();

        assertThat(name).isEqualTo("hello.txt");
    }

    @Test
    void should_ReturnNullAfterAllEntries_When_NoMoreEntriesExist() throws IOException {
        File zip = buildZip("a.txt", "aaa");
        ZipUtil zu = new ZipUtil(zip);

        zu.getNextFileName(); // consume the single entry
        String second = zu.getNextFileName(); // nothing left
        zu.close();

        assertThat(second).isNull();
    }

    @Test
    void should_ReturnByteContents_When_EntryIsRead() throws IOException {
        String content = "Hello World";
        File zip = buildZip("data.txt", content);
        ZipUtil zu = new ZipUtil(zip);
        zu.getNextFileName();

        byte[] bytes = zu.getByteContents();
        zu.close();

        assertThat(bytes).isEqualTo(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void should_ReturnNullByteContents_When_NoEntrySelected() throws IOException {
        File zip = buildZip("a.txt", "ignored");
        ZipUtil zu = new ZipUtil(zip);
        // intentionally do NOT call getNextFileName()

        byte[] bytes = zu.getByteContents();
        zu.close();

        assertThat(bytes).isNull();
    }

    @Test
    void should_IterateAllEntries_When_ZipContainsMultipleFiles() throws IOException {
        File zip = buildZip("first.txt", "alpha", "second.txt", "beta", "third.txt", "gamma");
        ZipUtil zu = new ZipUtil(zip);

        assertThat(zu.getNextFileName()).isEqualTo("first.txt");
        assertThat(zu.getNextFileName()).isEqualTo("second.txt");
        assertThat(zu.getNextFileName()).isEqualTo("third.txt");
        assertThat(zu.getNextFileName()).isNull();

        zu.close();
    }

    @Test
    void should_ReturnStringBufferContents_When_TextEntryIsRead() throws IOException {
        File zip = buildZip("readme.txt", "line1\nline2\n");
        ZipUtil zu = new ZipUtil(zip);
        zu.getNextFileName();

        StringBuilder sb = zu.getStringBufferContents();
        zu.close();

        assertThat(sb).isNotNull();
        assertThat(sb.toString()).contains("line1").contains("line2");
    }

    @Test
    void should_ThrowZipException_When_FileIsNotAValidZip() {
        File notAZip = tempDir.resolve("corrupt.zip").toFile();
        try (FileOutputStream fos = new FileOutputStream(notAZip)) {
            fos.write(new byte[]{0x00, 0x01, 0x02, 0x03});
        } catch (IOException e) {
            fail("Could not create corrupt file: " + e.getMessage());
        }

        assertThatThrownBy(() -> new ZipUtil(notAZip)).isInstanceOf(ZipException.class);
    }
}
