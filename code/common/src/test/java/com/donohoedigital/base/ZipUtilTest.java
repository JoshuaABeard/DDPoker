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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ZipUtil - utility for reading and iterating through ZIP file
 * entries
 */
class ZipUtilTest {

    @TempDir
    Path tempDir;

    private File zipFile;

    @BeforeEach
    void setUp() throws Exception {
        zipFile = tempDir.resolve("test.zip").toFile();
    }

    @AfterEach
    void tearDown() {
        if (zipFile != null && zipFile.exists()) {
            zipFile.delete();
        }
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    private void createZipWithTextFile(String fileName, String content) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private void createZipWithMultipleFiles() throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // File 1
            ZipEntry entry1 = new ZipEntry("file1.txt");
            zos.putNextEntry(entry1);
            zos.write("Content of file 1".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // File 2
            ZipEntry entry2 = new ZipEntry("file2.txt");
            zos.putNextEntry(entry2);
            zos.write("Content of file 2".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // File 3
            ZipEntry entry3 = new ZipEntry("file3.txt");
            zos.putNextEntry(entry3);
            zos.write("Content of file 3".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private void createZipWithBinaryContent(String fileName, byte[] content) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
        }
    }

    private void createZipWithCSV(String fileName, String csvContent) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(csvContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private void createEmptyZip() throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Empty ZIP
        }
    }

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateZipUtil_When_ValidZipFile() throws Exception {
        createZipWithTextFile("test.txt", "Hello World");

        ZipUtil zipUtil = new ZipUtil(zipFile);

        assertThat(zipUtil).isNotNull();
        zipUtil.close();
    }

    @Test
    void should_ThrowException_When_NonExistentFile() {
        File nonExistent = tempDir.resolve("nonexistent.zip").toFile();

        assertThatThrownBy(() -> new ZipUtil(nonExistent)).isInstanceOf(IOException.class);
    }

    // =================================================================
    // getNextFileName Tests
    // =================================================================

    @Test
    void should_ReturnFileName_When_ZipHasEntry() throws Exception {
        createZipWithTextFile("test.txt", "Hello");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        String fileName = zipUtil.getNextFileName();

        assertThat(fileName).isEqualTo("test.txt");
        zipUtil.close();
    }

    @Test
    void should_ReturnNull_When_NoMoreEntries() throws Exception {
        createZipWithTextFile("test.txt", "Hello");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName(); // First file
        String secondFileName = zipUtil.getNextFileName(); // No more files

        assertThat(secondFileName).isNull();
        zipUtil.close();
    }

    @Test
    void should_ReturnNull_When_EmptyZip() throws Exception {
        createEmptyZip();

        ZipUtil zipUtil = new ZipUtil(zipFile);
        String fileName = zipUtil.getNextFileName();

        assertThat(fileName).isNull();
        zipUtil.close();
    }

    @Test
    void should_IterateAllFiles_When_MultipleEntries() throws Exception {
        createZipWithMultipleFiles();

        ZipUtil zipUtil = new ZipUtil(zipFile);

        String file1 = zipUtil.getNextFileName();
        String file2 = zipUtil.getNextFileName();
        String file3 = zipUtil.getNextFileName();
        String noMore = zipUtil.getNextFileName();

        assertThat(file1).isEqualTo("file1.txt");
        assertThat(file2).isEqualTo("file2.txt");
        assertThat(file3).isEqualTo("file3.txt");
        assertThat(noMore).isNull();

        zipUtil.close();
    }

    // =================================================================
    // getByteContents Tests
    // =================================================================

    @Test
    void should_ReturnByteContents_When_CurrentEntryExists() throws Exception {
        String content = "Hello World";
        createZipWithTextFile("test.txt", content);

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName(); // Position to first entry
        byte[] bytes = zipUtil.getByteContents();

        assertThat(bytes).isNotNull();
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(content);
        zipUtil.close();
    }

    @Test
    void should_ReturnNull_When_NoCurrentEntry() throws Exception {
        createZipWithTextFile("test.txt", "Hello");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        // Don't call getNextFileName()
        byte[] bytes = zipUtil.getByteContents();

        assertThat(bytes).isNull();
        zipUtil.close();
    }

    @Test
    void should_ReturnNull_When_CurrentEntryCleared() throws Exception {
        createZipWithTextFile("test.txt", "Hello");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName(); // First entry
        zipUtil.getNextFileName(); // Clears current entry (no more entries)
        byte[] bytes = zipUtil.getByteContents();

        assertThat(bytes).isNull();
        zipUtil.close();
    }

    @Test
    void should_ReturnBinaryContent_When_BinaryFile() throws Exception {
        byte[] binaryData = {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
        createZipWithBinaryContent("binary.dat", binaryData);

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        byte[] bytes = zipUtil.getByteContents();

        assertThat(bytes).isEqualTo(binaryData);
        zipUtil.close();
    }

    @Test
    void should_ReturnEmptyArray_When_EmptyFile() throws Exception {
        createZipWithTextFile("empty.txt", "");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        byte[] bytes = zipUtil.getByteContents();

        assertThat(bytes).isNotNull();
        assertThat(bytes).isEmpty();
        zipUtil.close();
    }

    // =================================================================
    // getStringBufferContents Tests
    // =================================================================

    @Test
    void should_ReturnStringBuffer_When_TextFile() throws Exception {
        String content = "Line 1\nLine 2\nLine 3";
        createZipWithTextFile("test.txt", content);

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        StringBuilder sb = zipUtil.getStringBufferContents();

        assertThat(sb).isNotNull();
        assertThat(sb.toString()).isEqualTo("Line 1\nLine 2\nLine 3\n");
        zipUtil.close();
    }

    @Test
    void should_ReturnNull_When_NoCurrentEntryForStringBuffer() throws Exception {
        createZipWithTextFile("test.txt", "Hello");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        // Don't call getNextFileName()
        StringBuilder sb = zipUtil.getStringBufferContents();

        assertThat(sb).isNull();
        zipUtil.close();
    }

    @Test
    void should_AddNewlines_When_MultilineText() throws Exception {
        String content = "Line 1\nLine 2\nLine 3";
        createZipWithTextFile("multiline.txt", content);

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        StringBuilder sb = zipUtil.getStringBufferContents();

        // BufferedReader adds newlines after each line
        assertThat(sb.toString()).contains("Line 1\n");
        assertThat(sb.toString()).contains("Line 2\n");
        assertThat(sb.toString()).contains("Line 3\n");
        zipUtil.close();
    }

    @Test
    void should_ReturnEmptyStringBuilder_When_EmptyTextFile() throws Exception {
        createZipWithTextFile("empty.txt", "");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        StringBuilder sb = zipUtil.getStringBufferContents();

        assertThat(sb).isNotNull();
        assertThat(sb.toString()).isEmpty();
        zipUtil.close();
    }

    // =================================================================
    // getParsedCSVContents Tests
    // =================================================================

    @Test
    void should_ParseCSV_When_ValidCSVFile() throws Exception {
        String csvContent = "Name,Age,City\nJohn,30,NYC\nJane,25,LA";
        createZipWithCSV("data.csv", csvContent);

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        Vector vector = zipUtil.getParsedCSVContents();

        assertThat(vector).isNotNull();
        assertThat(vector).hasSize(3);

        String[] header = (String[]) vector.get(0);
        assertThat(header).containsExactly("Name", "Age", "City");

        String[] row1 = (String[]) vector.get(1);
        assertThat(row1).containsExactly("John", "30", "NYC");

        String[] row2 = (String[]) vector.get(2);
        assertThat(row2).containsExactly("Jane", "25", "LA");

        zipUtil.close();
    }

    @Test
    void should_ReturnNull_When_NoCurrentEntryForCSV() throws Exception {
        createZipWithCSV("data.csv", "A,B,C");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        // Don't call getNextFileName()
        Vector vector = zipUtil.getParsedCSVContents();

        assertThat(vector).isNull();
        zipUtil.close();
    }

    @Test
    void should_ReturnEmptyVector_When_EmptyCSVFile() throws Exception {
        createZipWithCSV("empty.csv", "");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        Vector vector = zipUtil.getParsedCSVContents();

        assertThat(vector).isNotNull();
        assertThat(vector).isEmpty();
        zipUtil.close();
    }

    @Test
    void should_HandleQuotedValues_When_CSVHasQuotes() throws Exception {
        String csvContent = "Name,Description\n\"John Doe\",\"A, B, C\"";
        createZipWithCSV("quoted.csv", csvContent);

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        Vector vector = zipUtil.getParsedCSVContents();

        assertThat(vector).hasSize(2);
        String[] row = (String[]) vector.get(1);
        assertThat(row).containsExactly("John Doe", "A, B, C");

        zipUtil.close();
    }

    // =================================================================
    // close Tests
    // =================================================================

    @Test
    void should_CloseSuccessfully_When_CalledAfterReading() throws Exception {
        createZipWithTextFile("test.txt", "Hello");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        zipUtil.getByteContents();

        // Should not throw exception
        assertThatCode(() -> zipUtil.close()).doesNotThrowAnyException();
    }

    @Test
    void should_ClearState_When_Closed() throws Exception {
        createZipWithTextFile("test.txt", "Hello");

        ZipUtil zipUtil = new ZipUtil(zipFile);
        zipUtil.getNextFileName();
        zipUtil.close();

        // After close, getNextFileName() should return null
        String fileName = zipUtil.getNextFileName();
        assertThat(fileName).isNull();
    }

    @Test
    void should_HandleMultipleClose_When_CalledTwice() throws Exception {
        createZipWithTextFile("test.txt", "Hello");

        ZipUtil zipUtil = new ZipUtil(zipFile);

        // Close multiple times should not throw exception
        assertThatCode(() -> {
            zipUtil.close();
            zipUtil.close();
        }).doesNotThrowAnyException();
    }

    // =================================================================
    // Integration Tests
    // =================================================================

    @Test
    void should_ReadMultipleFilesSequentially_When_Iterating() throws Exception {
        createZipWithMultipleFiles();

        ZipUtil zipUtil = new ZipUtil(zipFile);

        // Read file 1
        String file1Name = zipUtil.getNextFileName();
        byte[] file1Content = zipUtil.getByteContents();
        assertThat(file1Name).isEqualTo("file1.txt");
        assertThat(new String(file1Content, StandardCharsets.UTF_8)).isEqualTo("Content of file 1");

        // Read file 2
        String file2Name = zipUtil.getNextFileName();
        StringBuilder file2Content = zipUtil.getStringBufferContents();
        assertThat(file2Name).isEqualTo("file2.txt");
        assertThat(file2Content.toString()).isEqualTo("Content of file 2\n");

        // Read file 3
        String file3Name = zipUtil.getNextFileName();
        byte[] file3Content = zipUtil.getByteContents();
        assertThat(file3Name).isEqualTo("file3.txt");
        assertThat(new String(file3Content, StandardCharsets.UTF_8)).isEqualTo("Content of file 3");

        zipUtil.close();
    }
}
