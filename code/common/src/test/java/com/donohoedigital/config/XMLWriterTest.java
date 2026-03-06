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
package com.donohoedigital.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;

class XMLWriterTest {

    private StringWriter stringWriter;
    private XMLWriter writer;

    @BeforeEach
    void setUp() {
        stringWriter = new StringWriter();
        writer = new XMLWriter(stringWriter);
    }

    private String output() {
        writer.flush();
        return stringWriter.toString();
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Test
    void should_CreateWriter_When_ConstructedWithWriter() {
        assertThat(writer).isNotNull();
        writer.print("test");
        assertThat(output()).isEqualTo("test");
    }

    @Test
    void should_CreateWriter_When_ConstructedWithWriterAndAutoFlush() {
        StringWriter sw = new StringWriter();
        XMLWriter w = new XMLWriter(sw, true);

        w.print("auto");

        assertThat(sw.toString()).isEqualTo("auto");
    }

    // -------------------------------------------------------------------------
    // printXMLHeaderLine
    // -------------------------------------------------------------------------

    @Test
    void should_OutputXmlDeclaration_When_PrintXMLHeaderLineCalled() {
        writer.printXMLHeaderLine();

        assertThat(output()).isEqualTo("<?xml version=\"1.0\"?>\n");
    }

    // -------------------------------------------------------------------------
    // printElementStartLine / printElementEndLine
    // -------------------------------------------------------------------------

    @Test
    void should_OutputStartTag_When_PrintElementStartLineCalled() {
        writer.printElementStartLine("items", 1);

        assertThat(output()).isEqualTo("    <items>\n");
    }

    @Test
    void should_OutputEndTag_When_PrintElementEndLineWithNameCalled() {
        writer.printElementEndLine("items", 1);

        assertThat(output()).isEqualTo("    </items>\n");
    }

    @Test
    void should_OutputSelfClosingEnd_When_PrintElementEndLineCalled() {
        writer.printElementEndLine();

        assertThat(output()).isEqualTo("/>\n");
    }

    // -------------------------------------------------------------------------
    // printElement (simple element)
    // -------------------------------------------------------------------------

    @Test
    void should_OutputElement_When_PrintElementCalled() {
        writer.printElement("name", "John");

        assertThat(output()).isEqualTo("<name>John</name>");
    }

    @Test
    void should_OutputElementWithNumericValue_When_PrintElementCalledWithInteger() {
        writer.printElement("count", 42);

        assertThat(output()).isEqualTo("<count>42</count>");
    }

    // -------------------------------------------------------------------------
    // printElementLine (indented element)
    // -------------------------------------------------------------------------

    @Test
    void should_OutputIndentedElement_When_PrintElementLineCalled() {
        writer.printElementLine("city", "Portland", 2);

        assertThat(output()).isEqualTo("        <city>Portland</city>\n");
    }

    @Test
    void should_OutputElementAtZeroIndent_When_IndentIsZero() {
        writer.printElementLine("root", "val", 0);

        assertThat(output()).isEqualTo("<root>val</root>\n");
    }

    // -------------------------------------------------------------------------
    // printAttribute
    // -------------------------------------------------------------------------

    @Test
    void should_OutputAttribute_When_PrintAttributeCalled() {
        writer.printAttribute("id", "123");

        assertThat(output()).isEqualTo(" id=\"123\"");
    }

    @Test
    void should_OutputAttributeWithNumericValue_When_PrintAttributeCalledWithInteger() {
        writer.printAttribute("size", 10);

        assertThat(output()).isEqualTo(" size=\"10\"");
    }

    // -------------------------------------------------------------------------
    // printNewLine
    // -------------------------------------------------------------------------

    @Test
    void should_OutputNewline_When_PrintNewLineCalled() {
        writer.printNewLine();

        assertThat(output()).isEqualTo("\n");
    }

    // -------------------------------------------------------------------------
    // printIndent
    // -------------------------------------------------------------------------

    @Test
    void should_OutputSpaces_When_PrintIndentCalled() {
        writer.printIndent(2);

        assertThat(output()).isEqualTo("        "); // 2 * 4 = 8 spaces
    }

    @Test
    void should_OutputNoSpaces_When_PrintIndentCalledWithZero() {
        writer.printIndent(0);

        assertThat(output()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // setIndentSize / getIndentSize
    // -------------------------------------------------------------------------

    @Test
    void should_DefaultToFour_When_GetIndentSizeCalled() {
        assertThat(writer.getIndentSize()).isEqualTo(4);
    }

    @Test
    void should_ChangeIndentSize_When_SetIndentSizeCalled() {
        writer.setIndentSize(2);

        assertThat(writer.getIndentSize()).isEqualTo(2);

        writer.printIndent(3);

        assertThat(output()).isEqualTo("      "); // 3 * 2 = 6 spaces
    }

    // -------------------------------------------------------------------------
    // printRootElementStartLine
    // -------------------------------------------------------------------------

    @Test
    void should_OutputRootElement_When_PrintRootElementStartLineCalled() {
        writer.printRootElementStartLine("config", "http://example.com/ns", "config.xsd", 0);

        String result = output();

        assertThat(result).contains("<config ");
        assertThat(result).contains("xmlns=\"http://example.com/ns\"");
        assertThat(result).contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        assertThat(result).contains("\">");
    }

    // -------------------------------------------------------------------------
    // printElementStartOpen / printElementClose / printElementEnd
    // -------------------------------------------------------------------------

    @Test
    void should_OutputOpenTag_When_PrintElementStartOpenCalled() {
        writer.printElementStartOpen("item");

        assertThat(output()).isEqualTo("<item");
    }

    @Test
    void should_OutputCloseBracket_When_PrintElementCloseCalled() {
        writer.printElementClose();

        assertThat(output()).isEqualTo(">");
    }

    @Test
    void should_OutputCloseBracketWithNewline_When_PrintElementCloseLineCalled() {
        writer.printElementCloseLine();

        assertThat(output()).isEqualTo(">\n");
    }

    @Test
    void should_OutputSelfClosingTag_When_PrintElementEndCalled() {
        writer.printElementEnd();

        assertThat(output()).isEqualTo("/>");
    }

    @Test
    void should_OutputClosingTag_When_PrintElementEndWithNameCalled() {
        writer.printElementEnd("item");

        assertThat(output()).isEqualTo("</item>");
    }

    // -------------------------------------------------------------------------
    // Complete document
    // -------------------------------------------------------------------------

    @Test
    void should_ProduceValidXmlDocument_When_AllMethodsCombined() {
        writer.printXMLHeaderLine();
        writer.printElementStartLine("root", 0);
        writer.printElementLine("name", "Alice", 1);
        writer.printElementLine("age", 30, 1);

        // Element with attributes
        writer.printIndent(1);
        writer.printElementStartOpen("item");
        writer.printAttribute("type", "book");
        writer.printAttribute("count", 5);
        writer.printElementEndLine();

        writer.printElementEndLine("root", 0);

        String result = output();

        assertThat(result).isEqualTo("<?xml version=\"1.0\"?>\n" + "<root>\n" + "    <name>Alice</name>\n"
                + "    <age>30</age>\n" + "    <item type=\"book\" count=\"5\"/>\n" + "</root>\n");
    }

    @Test
    void should_ProduceNestedElements_When_MultipleIndentLevelsUsed() {
        writer.printElementStartLine("library", 0);
        writer.printElementStartLine("section", 1);
        writer.printElementLine("book", "Moby Dick", 2);
        writer.printElementEndLine("section", 1);
        writer.printElementEndLine("library", 0);

        String result = output();

        assertThat(result).isEqualTo("<library>\n" + "    <section>\n" + "        <book>Moby Dick</book>\n"
                + "    </section>\n" + "</library>\n");
    }
}
