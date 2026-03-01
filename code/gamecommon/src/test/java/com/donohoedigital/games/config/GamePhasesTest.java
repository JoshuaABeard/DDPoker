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
package com.donohoedigital.games.config;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GamePhases - collection of GamePhase instances parsed from XML.
 */
class GamePhasesTest {

    private static final Namespace ns = Namespace.NO_NAMESPACE;

    // ========== Construction Tests ==========

    @Test
    void should_BeEmpty_When_RootHasNoPhaseChildren() throws Exception {
        Element root = buildRoot("<phases />");

        GamePhases phases = new GamePhases(root, ns);

        assertThat(phases).isEmpty();
        assertThat(phases.size()).isZero();
    }

    @Test
    void should_ContainOnePhase_When_RootHasOnePhaseChild() throws Exception {
        Element root = buildRoot("<phases><phase name=\"alpha\" class=\"java.lang.String\" /></phases>");

        GamePhases phases = new GamePhases(root, ns);

        assertThat(phases).hasSize(1);
        assertThat(phases.containsKey("alpha")).isTrue();
    }

    @Test
    void should_ContainMultiplePhases_When_RootHasMultiplePhaseChildren() throws Exception {
        Element root = buildRoot("<phases>" + "<phase name=\"alpha\" class=\"java.lang.String\" />"
                + "<phase name=\"beta\" class=\"java.lang.Integer\" />"
                + "<phase name=\"gamma\" class=\"java.lang.Long\" />" + "</phases>");

        GamePhases phases = new GamePhases(root, ns);

        assertThat(phases).hasSize(3);
        assertThat(phases.keySet()).containsExactlyInAnyOrder("alpha", "beta", "gamma");
    }

    // ========== Lookup by Name Tests ==========

    @Test
    void should_ReturnPhase_When_LookingUpByName() throws Exception {
        Element root = buildRoot("<phases><phase name=\"myPhase\" class=\"java.lang.String\" /></phases>");

        GamePhases phases = new GamePhases(root, ns);

        GamePhase phase = phases.get("myPhase");
        assertThat(phase).isNotNull();
        assertThat(phase.getName()).isEqualTo("myPhase");
        assertThat(phase.getClassName()).isEqualTo("java.lang.String");
    }

    @Test
    void should_ReturnNull_When_LookingUpNonExistentName() throws Exception {
        Element root = buildRoot("<phases><phase name=\"exists\" class=\"java.lang.String\" /></phases>");

        GamePhases phases = new GamePhases(root, ns);

        assertThat(phases.get("doesNotExist")).isNull();
    }

    // ========== Phase Inheritance via Extends ==========

    @Test
    void should_ResolveExtends_When_ParentPhaseDeclaredFirst() throws Exception {
        Element root = buildRoot("<phases>" + "<phase name=\"base\" class=\"java.lang.String\" cache=\"true\" />"
                + "<phase name=\"derived\" extends=\"base\" />" + "</phases>");

        GamePhases phases = new GamePhases(root, ns);

        assertThat(phases).hasSize(2);
        GamePhase derived = phases.get("derived");
        assertThat(derived).isNotNull();
        assertThat(derived.getClassName()).isEqualTo("java.lang.String");
        assertThat(derived.isCached()).isTrue();
    }

    @Test
    void should_ThrowError_When_ExtendsTargetNotYetDeclared() throws Exception {
        // The extends target must appear before the phase that extends it.
        // If order is wrong, GamePhase constructor throws ApplicationError.
        Element root = buildRoot("<phases>" + "<phase name=\"derived\" extends=\"base\" />"
                + "<phase name=\"base\" class=\"java.lang.String\" />" + "</phases>");

        assertThatThrownBy(() -> new GamePhases(root, ns)).hasMessageContaining("wasn't found");
    }

    // ========== Duplicate Phase Handling ==========

    @Test
    void should_RetainLastDefinition_When_DuplicatePhaseNamesExist() throws Exception {
        // GamePhases extends HashMap; duplicate keys silently overwrite.
        Element root = buildRoot("<phases>" + "<phase name=\"dup\" class=\"java.lang.String\" />"
                + "<phase name=\"dup\" class=\"java.lang.Integer\" />" + "</phases>");

        GamePhases phases = new GamePhases(root, ns);

        // Map keeps last writer; size is 1 and the class is from the second
        // declaration.
        assertThat(phases).hasSize(1);
        assertThat(phases.get("dup").getClassName()).isEqualTo("java.lang.Integer");
    }

    // ========== Test Helper ==========

    private Element buildRoot(String xml) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new StringReader(xml));
        return doc.getRootElement();
    }
}
