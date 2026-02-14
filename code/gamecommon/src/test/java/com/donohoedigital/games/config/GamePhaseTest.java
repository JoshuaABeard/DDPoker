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
package com.donohoedigital.games.config;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GamePhase - Phase configuration and inheritance.
 */
class GamePhaseTest {

    private GamePhases phases;
    private static final Namespace ns = Namespace.NO_NAMESPACE;

    @BeforeEach
    void setUp() throws Exception {
        // Create empty phases map for testing
        Element root = new Element("phases");
        phases = new GamePhases(root, ns);
    }

    // ========== XML Construction Tests ==========

    @Test
    void should_CreatePhase_When_ValidXMLProvided() throws Exception {
        Element phaseElement = buildPhaseElement("<phase name=\"testPhase\" class=\"java.lang.String\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.getName()).isEqualTo("testPhase");
        assertThat(phase.getClassName()).isEqualTo("java.lang.String");
    }

    @Test
    void should_LoadClass_When_ClassnameProvided() throws Exception {
        Element phaseElement = buildPhaseElement("<phase name=\"testPhase\" class=\"java.lang.String\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.getClassObject()).isEqualTo(String.class);
    }

    // ========== Flag Tests ==========

    @Test
    void should_DefaultToFalse_When_CacheNotSpecified() throws Exception {
        Element phaseElement = buildPhaseElement("<phase name=\"test\" class=\"java.lang.String\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.isCached()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_CacheIsTrue() throws Exception {
        Element phaseElement = buildPhaseElement("<phase name=\"test\" class=\"java.lang.String\" cache=\"true\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.isCached()).isTrue();
    }

    @Test
    void should_DefaultToFalse_When_HistoryNotSpecified() throws Exception {
        Element phaseElement = buildPhaseElement("<phase name=\"test\" class=\"java.lang.String\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.isHistory()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_HistoryIsTrue() throws Exception {
        Element phaseElement = buildPhaseElement("<phase name=\"test\" class=\"java.lang.String\" history=\"true\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.isHistory()).isTrue();
    }

    @Test
    void should_DefaultToFalse_When_TransientNotSpecified() throws Exception {
        Element phaseElement = buildPhaseElement("<phase name=\"test\" class=\"java.lang.String\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.isTransient()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_TransientIsTrue() throws Exception {
        Element phaseElement = buildPhaseElement(
                "<phase name=\"test\" class=\"java.lang.String\" transient=\"true\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.isTransient()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_WindowNotSpecified() throws Exception {
        Element phaseElement = buildPhaseElement("<phase name=\"test\" class=\"java.lang.String\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.isWindow()).isFalse();
        assertThat(phase.getWindowName()).isNull();
    }

    @Test
    void should_ReturnTrue_When_WindowSpecified() throws Exception {
        Element phaseElement = buildPhaseElement(
                "<phase name=\"test\" class=\"java.lang.String\" window=\"mainWindow\" />");

        GamePhase phase = new GamePhase(phaseElement, ns, "test");

        assertThat(phase.isWindow()).isTrue();
        assertThat(phase.getWindowName()).isEqualTo("mainWindow");
    }

    // ========== Inheritance Tests ==========

    @Test
    void should_InheritClass_When_ExtendsPhaseWithoutClass() throws Exception {
        Element parent = buildPhaseElement("<phase name=\"parent\" class=\"java.lang.String\" />");
        GamePhase parentPhase = new GamePhase(parent, ns, "test");
        phases.put("parent", parentPhase);

        Element child = buildPhaseElement("<phase name=\"child\" extends=\"parent\" />");
        GamePhase childPhase = new GamePhase(child, ns, "test");

        assertThat(childPhase.getClassName()).isEqualTo("java.lang.String");
        assertThat(childPhase.getClassObject()).isEqualTo(String.class);
    }

    @Test
    void should_InheritFlags_When_ExtendsPhase() throws Exception {
        Element parent = buildPhaseElement(
                "<phase name=\"parent\" class=\"java.lang.String\" cache=\"true\" history=\"true\" />");
        GamePhase parentPhase = new GamePhase(parent, ns, "test");
        phases.put("parent", parentPhase);

        Element child = buildPhaseElement("<phase name=\"child\" extends=\"parent\" />");
        GamePhase childPhase = new GamePhase(child, ns, "test");

        assertThat(childPhase.isCached()).isTrue();
        assertThat(childPhase.isHistory()).isTrue();
    }

    @Test
    void should_OverrideInheritedClass_When_ChildSpecifiesClass() throws Exception {
        Element parent = buildPhaseElement("<phase name=\"parent\" class=\"java.lang.String\" />");
        GamePhase parentPhase = new GamePhase(parent, ns, "test");
        phases.put("parent", parentPhase);

        Element child = buildPhaseElement("<phase name=\"child\" extends=\"parent\" class=\"java.lang.Integer\" />");
        GamePhase childPhase = new GamePhase(child, ns, "test");

        assertThat(childPhase.getClassName()).isEqualTo("java.lang.Integer");
        assertThat(childPhase.getClassObject()).isEqualTo(Integer.class);
    }

    @Test
    void should_OverrideInheritedFlags_When_ChildSpecifiesFlags() throws Exception {
        Element parent = buildPhaseElement(
                "<phase name=\"parent\" class=\"java.lang.String\" cache=\"true\" history=\"false\" />");
        GamePhase parentPhase = new GamePhase(parent, ns, "test");
        phases.put("parent", parentPhase);

        Element child = buildPhaseElement(
                "<phase name=\"child\" extends=\"parent\" cache=\"false\" history=\"true\" />");
        GamePhase childPhase = new GamePhase(child, ns, "test");

        assertThat(childPhase.isCached()).isFalse();
        assertThat(childPhase.isHistory()).isTrue();
    }

    @Test
    void should_ThrowError_When_ExtendingNonExistentPhase() throws Exception {
        Element child = buildPhaseElement("<phase name=\"child\" extends=\"nonexistent\" />");

        assertThatThrownBy(() -> new GamePhase(child, ns, "test")).hasMessageContaining("nonexistent")
                .hasMessageContaining("wasn't found");
    }

    @Test
    void should_ThrowError_When_ClassNotDefined() throws Exception {
        Element phase = buildPhaseElement("<phase name=\"test\" />");

        assertThatThrownBy(() -> new GamePhase(phase, ns, "test")).hasMessageContaining("Class not defined");
    }

    // ========== Utility Tests ==========

    @Test
    void should_ReturnSimpleName_When_NoPhaseParam() throws Exception {
        Element phase = buildPhaseElement("<phase name=\"test\" class=\"java.lang.String\" />");
        GamePhase gamePhase = new GamePhase(phase, ns, "test");

        String buttonName = gamePhase.getButtonNameFromParam("myButton");

        assertThat(buttonName).isEqualTo("myButton");
    }

    // Note: Testing getButtonNameFromParam with actual params requires complex XML
    // setup
    // Skipping this test as it requires full PropertyConfig initialization

    @Test
    void should_IncludeNameAndFlags_When_ToStringCalled() throws Exception {
        Element phase = buildPhaseElement(
                "<phase name=\"testPhase\" class=\"java.lang.String\" cache=\"true\" history=\"false\" />");
        GamePhase gamePhase = new GamePhase(phase, ns, "test");

        String result = gamePhase.toString();

        assertThat(result).contains("testPhase");
        assertThat(result).contains("cached: true");
        assertThat(result).contains("history: false");
    }

    // ========== Test Helpers ==========

    private Element buildPhaseElement(String xml) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new StringReader("<root>" + xml + "</root>"));
        return doc.getRootElement().getChildren().get(0);
    }
}
