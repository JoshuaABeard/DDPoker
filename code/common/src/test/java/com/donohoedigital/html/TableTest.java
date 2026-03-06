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
package com.donohoedigital.html;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TableTest {

    // -------------------------------------------------------------------------
    // Table
    // -------------------------------------------------------------------------

    @Test
    void should_SetCellSpacingAndPadding_When_Constructed() {
        Table table = new Table(2, 3);
        table.addColumn(new TableColumn("Col", TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, null));

        String html = table.toString();

        assertThat(html).contains("CELLSPACING=2");
        assertThat(html).contains("CELLPADDING=3");
    }

    @Test
    void should_AddAndGetColumn_When_ColumnAdded() {
        Table table = new Table(0, 0);
        TableColumn col = new TableColumn("Name", TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, null);

        table.addColumn(col);

        assertThat(table.getColumn(0)).isSameAs(col);
    }

    @Test
    void should_GenerateValidTableHtml_When_ColumnsAndRowsAdded() {
        Table table = new Table(1, 2);
        table.addColumn(new TableColumn("Header1", TableColumn.VALIGN.CENTER, TableColumn.HALIGN.CENTER, null));
        table.addColumn(new TableColumn("Header2", TableColumn.VALIGN.CENTER, TableColumn.HALIGN.CENTER, null));

        TableRow row = new TableRow();
        row.addData("Cell1");
        row.addData("Cell2");
        table.addRow(row);

        String html = table.toString();

        assertThat(html).startsWith("<TABLE").endsWith("</TABLE>");
        assertThat(html).contains("<TR>");
        assertThat(html).contains("</TR>");
        assertThat(html).contains("Header1");
        assertThat(html).contains("Header2");
        assertThat(html).contains("Cell1");
        assertThat(html).contains("Cell2");
    }

    @Test
    void should_GenerateMultipleRows_When_MultipleRowsAdded() {
        Table table = new Table(0, 0);
        table.addColumn(new TableColumn("Col", TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, null));

        TableRow row1 = new TableRow();
        row1.addData("A");
        table.addRow(row1);

        TableRow row2 = new TableRow();
        row2.addData("B");
        table.addRow(row2);

        String html = table.toString();

        assertThat(html).contains("A");
        assertThat(html).contains("B");
        // Header TR + 2 data TRs = 3 TRs
        assertThat(html.split("<TR>")).hasSize(4); // 1 before first + 3 TRs
    }

    @Test
    void should_ReturnEquivalentContent_When_UsingToStringBuilderOrToString() {
        Table table = new Table(5, 10);
        table.addColumn(new TableColumn("X", TableColumn.VALIGN.BOTTOM, TableColumn.HALIGN.RIGHT, "#FF0000"));

        String fromToString = table.toString();
        String fromBuilder = table.toStringBuilder().toString();

        assertThat(fromToString).isEqualTo(fromBuilder);
    }

    // -------------------------------------------------------------------------
    // TableColumn
    // -------------------------------------------------------------------------

    @Test
    void should_RenderHeaderCentered_When_ToStringCalled() {
        TableColumn col = new TableColumn("Title", TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, null);

        String header = col.toString();

        // Headers are always rendered with CENTER/CENTER alignment
        assertThat(header).contains("VALIGN=\"center\"");
        assertThat(header).contains("ALIGN=\"center\"");
        assertThat(header).contains("Title");
    }

    @Test
    void should_RenderDataWithColumnAlignment_When_ToStringWithDataCalled() {
        TableColumn col = new TableColumn("H", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, null);
        TableData data = new TableData("value", null);

        String rendered = col.toString(data);

        assertThat(rendered).contains("VALIGN=\"top\"");
        assertThat(rendered).contains("ALIGN=\"right\"");
        assertThat(rendered).contains("value");
    }

    @Test
    void should_RenderCorrectAlignment_When_ValignBottom_HalignLeft() {
        TableColumn col = new TableColumn("H", TableColumn.VALIGN.BOTTOM, TableColumn.HALIGN.LEFT, null);
        TableData data = new TableData("test", null);

        String rendered = col.toString(data);

        assertThat(rendered).contains("VALIGN=\"bottom\"");
        assertThat(rendered).contains("ALIGN=\"left\"");
    }

    @Test
    void should_RenderCorrectAlignment_When_ValignCenter_HalignCenter() {
        TableColumn col = new TableColumn("H", TableColumn.VALIGN.CENTER, TableColumn.HALIGN.CENTER, null);
        TableData data = new TableData("test", null);

        String rendered = col.toString(data);

        assertThat(rendered).contains("VALIGN=\"center\"");
        assertThat(rendered).contains("ALIGN=\"center\"");
    }

    // -------------------------------------------------------------------------
    // TableColumn VALIGN enum
    // -------------------------------------------------------------------------

    @Test
    void should_RenderAllValignValues_When_ToStringCalled() {
        assertThat(TableColumn.VALIGN.TOP.toString()).isEqualTo(" VALIGN=\"top\"");
        assertThat(TableColumn.VALIGN.CENTER.toString()).isEqualTo(" VALIGN=\"center\"");
        assertThat(TableColumn.VALIGN.BOTTOM.toString()).isEqualTo(" VALIGN=\"bottom\"");
    }

    // -------------------------------------------------------------------------
    // TableColumn HALIGN enum
    // -------------------------------------------------------------------------

    @Test
    void should_RenderAllHalignValues_When_ToStringCalled() {
        assertThat(TableColumn.HALIGN.LEFT.toString()).isEqualTo(" ALIGN=\"left\"");
        assertThat(TableColumn.HALIGN.CENTER.toString()).isEqualTo(" ALIGN=\"center\"");
        assertThat(TableColumn.HALIGN.RIGHT.toString()).isEqualTo(" ALIGN=\"right\"");
    }

    // -------------------------------------------------------------------------
    // TableRow
    // -------------------------------------------------------------------------

    @Test
    void should_RenderRowWithStringData_When_AddDataStringCalled() {
        Table table = new Table(0, 0);
        table.addColumn(new TableColumn("H", TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, null));

        TableRow row = new TableRow();
        row.addData("hello");

        String html = row.toString(table);

        assertThat(html).startsWith("<TR>");
        assertThat(html).endsWith("</TR>");
        assertThat(html).contains("hello");
    }

    @Test
    void should_RenderRowWithTableData_When_AddDataTableDataCalled() {
        Table table = new Table(0, 0);
        table.addColumn(new TableColumn("H", TableColumn.VALIGN.BOTTOM, TableColumn.HALIGN.RIGHT, null));

        TableRow row = new TableRow();
        row.addData(new TableData("world", "#00FF00"));

        String html = row.toString(table);

        assertThat(html).contains("world");
        assertThat(html).contains("bgcolor=\"#00FF00\"");
    }

    @Test
    void should_RenderMultipleCells_When_MultipleDataAdded() {
        Table table = new Table(0, 0);
        table.addColumn(new TableColumn("H1", TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, null));
        table.addColumn(new TableColumn("H2", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, null));

        TableRow row = new TableRow();
        row.addData("A");
        row.addData("B");

        String html = row.toString(table);

        assertThat(html).contains("A");
        assertThat(html).contains("B");
        assertThat(html.split("<TD")).hasSize(3); // 1 before first + 2 TDs
    }

    // -------------------------------------------------------------------------
    // TableData
    // -------------------------------------------------------------------------

    @Test
    void should_RenderTdWithAlignment_When_NoBgColor() {
        TableData data = new TableData("content", null);

        String html = data.toString(TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT);

        assertThat(html).isEqualTo("<TD VALIGN=\"top\" ALIGN=\"left\">content</TD>");
    }

    @Test
    void should_RenderTdWithBgColor_When_BgColorProvided() {
        TableData data = new TableData("content", "#AABBCC");

        String html = data.toString(TableColumn.VALIGN.CENTER, TableColumn.HALIGN.CENTER);

        assertThat(html).contains("bgcolor=\"#AABBCC\"");
        assertThat(html).startsWith("<TD");
        assertThat(html).endsWith("</TD>");
        assertThat(html).contains("content");
    }

    @Test
    void should_RenderCorrectOrder_When_AlignmentAndColorPresent() {
        TableData data = new TableData("X", "#FFF");

        String html = data.toString(TableColumn.VALIGN.BOTTOM, TableColumn.HALIGN.RIGHT);

        // Verify order: VALIGN, HALIGN, bgcolor, then content
        assertThat(html).isEqualTo("<TD VALIGN=\"bottom\" ALIGN=\"right\" bgcolor=\"#FFF\">X</TD>");
    }

    // -------------------------------------------------------------------------
    // Integration: Full table rendering
    // -------------------------------------------------------------------------

    @Test
    void should_ProduceCompleteHtml_When_FullTableBuilt() {
        Table table = new Table(1, 2);

        table.addColumn(new TableColumn("Name", TableColumn.VALIGN.TOP, TableColumn.HALIGN.LEFT, "#CCCCCC"));
        table.addColumn(new TableColumn("Score", TableColumn.VALIGN.TOP, TableColumn.HALIGN.RIGHT, "#CCCCCC"));

        TableRow row1 = new TableRow();
        row1.addData("Alice");
        row1.addData(new TableData("100", "#EEEEFF"));
        table.addRow(row1);

        TableRow row2 = new TableRow();
        row2.addData("Bob");
        row2.addData("200");
        table.addRow(row2);

        String html = table.toString();

        // Structure checks
        assertThat(html).startsWith("<TABLE CELLSPACING=1 CELLPADDING=2>");
        assertThat(html).endsWith("</TABLE>");

        // Header row has centered alignment and background color
        assertThat(html).contains("bgcolor=\"#CCCCCC\"");
        assertThat(html).contains("Name");
        assertThat(html).contains("Score");

        // Data rows
        assertThat(html).contains("Alice");
        assertThat(html).contains("100");
        assertThat(html).contains("bgcolor=\"#EEEEFF\"");
        assertThat(html).contains("Bob");
        assertThat(html).contains("200");
    }
}
