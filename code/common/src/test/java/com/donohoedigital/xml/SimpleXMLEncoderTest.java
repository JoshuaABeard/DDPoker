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
package com.donohoedigital.xml;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SimpleXMLEncoder — a builder-style XML document encoder.
 *
 * <p>
 * Note: SimpleXMLEncoder encodes values via Utils.encodeXML(), which escapes
 * {@code &}, {@code <}, {@code >}, and {@code "} but not single quotes (those
 * are only escaped in Javascript mode). Tests reflect that actual behaviour.
 */
class SimpleXMLEncoderTest {

    // -----------------------------------------------------------------------
    // XML declaration
    // -----------------------------------------------------------------------

    @Test
    void should_StartWithXmlDeclaration_When_EncoderIsCreated() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        assertThat(encoder.toString()).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    // -----------------------------------------------------------------------
    // Simple tag encoding
    // -----------------------------------------------------------------------

    @Test
    void should_EncodeSimpleStringTag_When_AddTagCalledWithPlainValue() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("root");
        encoder.addTag("name", "Alice");
        encoder.finishCurrentObject();

        String xml = encoder.toString();
        assertThat(xml).contains("<name>Alice</name>");
    }

    @Test
    void should_EncodeIntegerTag_When_AddTagCalledWithIntegerValue() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("root");
        encoder.addTag("count", 42);
        encoder.finishCurrentObject();

        String xml = encoder.toString();
        assertThat(xml).contains("<count>42</count>");
    }

    @Test
    void should_SkipNullTag_When_AddTagCalledWithNullValue() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("root");
        encoder.addTag("missing", (Object) null);
        encoder.finishCurrentObject();

        String xml = encoder.toString();
        assertThat(xml).doesNotContain("<missing>");
    }

    // -----------------------------------------------------------------------
    // Special XML character encoding
    // -----------------------------------------------------------------------

    @Test
    void should_EncodeAmpersand_When_ValueContainsAmpersand() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("root");
        encoder.addTag("text", "Tom & Jerry");
        encoder.finishCurrentObject();

        assertThat(encoder.toString()).contains("<text>Tom &amp; Jerry</text>");
    }

    @Test
    void should_EncodeLessThanAndGreaterThan_When_ValueContainsAngleBrackets() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("root");
        encoder.addTag("expr", "a < b > c");
        encoder.finishCurrentObject();

        assertThat(encoder.toString()).contains("<expr>a &lt; b &gt; c</expr>");
    }

    @Test
    void should_EncodeDoubleQuote_When_ValueContainsDoubleQuote() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("root");
        encoder.addTag("msg", "say \"hello\"");
        encoder.finishCurrentObject();

        assertThat(encoder.toString()).contains("<msg>say &quot;hello&quot;</msg>");
    }

    @Test
    void should_NotEncodeSingleQuote_When_ValueContainsSingleQuote() {
        // Utils.encodeXML does NOT encode single quotes (only Javascript mode does)
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("root");
        encoder.addTag("msg", "it's fine");
        encoder.finishCurrentObject();

        assertThat(encoder.toString()).contains("<msg>it's fine</msg>");
    }

    // -----------------------------------------------------------------------
    // Nested elements
    // -----------------------------------------------------------------------

    @Test
    void should_ProduceNestedTags_When_MultipleObjectsArePushed() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("outer");
        encoder.setCurrentObject("inner");
        encoder.addTag("value", "42");
        encoder.finishCurrentObject(); // close inner
        encoder.finishCurrentObject(); // close outer

        String xml = encoder.toString();
        assertThat(xml).contains("<outer>");
        assertThat(xml).contains("<inner>");
        assertThat(xml).contains("<value>42</value>");
        assertThat(xml).contains("</inner>");
        assertThat(xml).contains("</outer>");
    }

    @Test
    void should_IndentNestedTags_When_ObjectsAreNested() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("parent");
        encoder.setCurrentObject("child");
        encoder.finishCurrentObject();
        encoder.finishCurrentObject();

        String xml = encoder.toString();
        // indent() emits 2 spaces per stack depth. When "child" opens, "parent" is on
        // the stack (depth = 1), so the child tag gets 2 spaces of indentation.
        assertThat(xml).contains("  <child>");
    }

    // -----------------------------------------------------------------------
    // Method chaining
    // -----------------------------------------------------------------------

    @Test
    void should_SupportMethodChaining_When_AddTagReturnsEncoder() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.setCurrentObject("root").addTag("a", "1").addTag("b", "2").finishCurrentObject();

        String xml = encoder.toString();
        assertThat(xml).contains("<a>1</a>");
        assertThat(xml).contains("<b>2</b>");
    }

    // -----------------------------------------------------------------------
    // Comment
    // -----------------------------------------------------------------------

    @Test
    void should_AddComment_When_AddCommentCalledWithEscaping() {
        SimpleXMLEncoder encoder = new SimpleXMLEncoder();
        encoder.addComment("note: a < b", true);

        assertThat(encoder.toString()).contains("<!--");
        assertThat(encoder.toString()).contains("note: a &lt; b");
    }
}
