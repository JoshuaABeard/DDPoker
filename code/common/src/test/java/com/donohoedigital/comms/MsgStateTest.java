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
package com.donohoedigital.comms;

import com.donohoedigital.base.ApplicationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for MsgState - object ID tracking and class ID management used during
 * serialization/deserialization.
 */
class MsgStateTest {

    private MsgState state;

    @BeforeEach
    void setUp() {
        state = new MsgState();
    }

    // ---- Constructor ----

    @Test
    void should_CreateEmptyState_When_DefaultConstructed() {
        assertThat(state).isNotNull();
    }

    // ---- setId / getId ----

    @Test
    void should_AssignAndRetrieveId_When_SetIdCalledWithObjectAndId() {
        Object obj = "testObject";
        state.setId(obj, 1000);

        assertThat(state.isIdUsed(obj)).isTrue();
    }

    @Test
    void should_ReturnAssignedId_When_GetIdCalledAfterSetId() {
        Object obj = "testObject";
        state.setId(obj, 1500);

        Integer id = state.getId(obj);
        assertThat(id).isEqualTo(1500);
    }

    @Test
    void should_AutoAssignId_When_GetIdCalledForNewObject() {
        Object obj = "autoId";

        Integer id = state.getId(obj);
        assertThat(id).isGreaterThanOrEqualTo(1000);
    }

    @Test
    void should_ReturnSameId_When_GetIdCalledTwiceForSameObject() {
        Object obj = "sameObject";

        Integer id1 = state.getId(obj);
        Integer id2 = state.getId(obj);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void should_ReturnNull_When_GetIdCalledWithNull() {
        assertThat(state.getId(null)).isNull();
    }

    @Test
    void should_UseObjectID_When_ObjectImplementsObjectID() {
        ObjectID oid = () -> 42;
        state.setId(oid);

        Integer id = state.getId(oid);
        assertThat(id).isEqualTo(42);
    }

    // ---- isIdUsed ----

    @Test
    void should_ReturnFalse_When_ObjectNotRegistered() {
        assertThat(state.isIdUsed("unknown")).isFalse();
    }

    @Test
    void should_ReturnTrue_When_ObjectRegistered() {
        Object obj = "registered";
        state.getId(obj);

        assertThat(state.isIdUsed(obj)).isTrue();
    }

    // ---- getObject ----

    @Test
    void should_ReturnObject_When_IdIsRegistered() {
        Object obj = "findMe";
        state.setId(obj, 2000);

        assertThat(state.getObject(2000)).isSameAs(obj);
    }

    @Test
    void should_ReturnNull_When_GetObjectCalledWithNull() {
        assertThat(state.getObject(null)).isNull();
    }

    @Test
    void should_ThrowError_When_GetObjectCalledWithUnknownId() {
        assertThatThrownBy(() -> state.getObject(9999)).isInstanceOf(ApplicationError.class);
    }

    // ---- getObjectNullOkay ----

    @Test
    void should_ReturnNull_When_GetObjectNullOkayCalledWithUnknownId() {
        assertThat(state.getObjectNullOkay(9999)).isNull();
    }

    @Test
    void should_ReturnNull_When_GetObjectNullOkayCalledWithNull() {
        assertThat(state.getObjectNullOkay(null)).isNull();
    }

    @Test
    void should_ReturnObject_When_GetObjectNullOkayCalledWithKnownId() {
        Object obj = "known";
        state.setId(obj, 3000);

        assertThat(state.getObjectNullOkay(3000)).isSameAs(obj);
    }

    // ---- resetIds ----

    @Test
    void should_ClearAllMappings_When_ResetIdsCalled() {
        // Subclass to expose protected resetIds
        class TestableMsgState extends MsgState {
            void reset() {
                resetIds();
            }
        }

        TestableMsgState testState = new TestableMsgState();
        testState.setId("willBeCleared", 5000);
        assertThat(testState.isIdUsed("willBeCleared")).isTrue();

        testState.reset();

        assertThat(testState.isIdUsed("willBeCleared")).isFalse();
    }

    // ---- Class ID management ----

    @Test
    void should_AssignClassId_When_GetClassIdCalled() {
        TokenizedList classNames = new TokenizedList();
        MsgState classState = new MsgState() {
            {
                setClassNames(classNames);
            }
        };

        Integer id = classState.getClassId("com.example.Foo");
        assertThat(id).isEqualTo(0);
    }

    @Test
    void should_ReturnSameClassId_When_CalledTwiceForSameClass() {
        TokenizedList classNames = new TokenizedList();
        MsgState classState = new MsgState() {
            {
                setClassNames(classNames);
            }
        };

        Integer id1 = classState.getClassId("com.example.Foo");
        Integer id2 = classState.getClassId("com.example.Foo");

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void should_AssignIncrementingIds_When_MultipleClassesRegistered() {
        TokenizedList classNames = new TokenizedList();
        MsgState classState = new MsgState() {
            {
                setClassNames(classNames);
            }
        };

        Integer id1 = classState.getClassId("com.example.Foo");
        Integer id2 = classState.getClassId("com.example.Bar");

        assertThat(id1).isEqualTo(0);
        assertThat(id2).isEqualTo(1);
    }

    @Test
    void should_ReturnNull_When_GetClassIdCalledWithNull() {
        assertThat(state.getClassId(null)).isNull();
    }

    @Test
    void should_ReturnClassName_When_GetClassNameCalledWithKnownId() {
        TokenizedList classNames = new TokenizedList();
        MsgState classState = new MsgState() {
            {
                setClassNames(classNames);
            }
        };

        classState.getClassId("com.example.Foo");

        assertThat(classState.getClassName(0)).isEqualTo("com.example.Foo");
    }

    @Test
    void should_ReturnNull_When_GetClassNameCalledWithNull() {
        assertThat(state.getClassName(null)).isNull();
    }

    @Test
    void should_ReturnNull_When_GetClassNameCalledWithUnknownId() {
        assertThat(state.getClassName(999)).isNull();
    }

    // ---- Duplicate detection ----

    @Test
    void should_ThrowError_When_SettingIdForAlreadyRegisteredObject() {
        state.setId("obj", 1000);

        assertThatThrownBy(() -> state.setId("obj", 2000)).isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowError_When_SettingDuplicateIdValue() {
        state.setId("obj1", 1000);

        assertThatThrownBy(() -> state.setId("obj2", 1000)).isInstanceOf(ApplicationError.class);
    }
}
