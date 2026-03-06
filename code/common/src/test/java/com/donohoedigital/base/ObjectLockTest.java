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

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ObjectLock — a simple synchronized reference counter with an ID.
 */
class ObjectLockTest {

    // -----------------------------------------------------------------------
    // Constructor and initial state
    // -----------------------------------------------------------------------

    @Test
    void should_StoreID_When_Constructed() {
        ObjectLock lock = new ObjectLock("test-id");
        assertThat(lock.getID()).isEqualTo("test-id");
    }

    @Test
    void should_StartAtOne_When_Constructed() {
        ObjectLock lock = new ObjectLock("lock");
        assertThat(lock.getInstanceCount()).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Increment
    // -----------------------------------------------------------------------

    @Test
    void should_IncrementCount_When_IncrementCalled() {
        ObjectLock lock = new ObjectLock("lock");
        int result = lock.increment();
        assertThat(result).isEqualTo(2);
        assertThat(lock.getInstanceCount()).isEqualTo(2);
    }

    @Test
    void should_IncrementMultipleTimes_When_CalledRepeatedly() {
        ObjectLock lock = new ObjectLock("lock");
        lock.increment();
        lock.increment();
        lock.increment();
        assertThat(lock.getInstanceCount()).isEqualTo(4);
    }

    // -----------------------------------------------------------------------
    // Decrement
    // -----------------------------------------------------------------------

    @Test
    void should_DecrementCount_When_DecrementCalled() {
        ObjectLock lock = new ObjectLock("lock");
        lock.increment(); // now 2
        int result = lock.decrement();
        assertThat(result).isEqualTo(1);
        assertThat(lock.getInstanceCount()).isEqualTo(1);
    }

    @Test
    void should_GoToZero_When_DecrementedFromOne() {
        ObjectLock lock = new ObjectLock("lock");
        int result = lock.decrement();
        assertThat(result).isZero();
    }

    @Test
    void should_GoNegative_When_DecrementedBelowZero() {
        ObjectLock lock = new ObjectLock("lock");
        lock.decrement(); // 0
        int result = lock.decrement(); // -1
        assertThat(result).isEqualTo(-1);
        assertThat(lock.getInstanceCount()).isNegative();
    }

    // -----------------------------------------------------------------------
    // Null ID
    // -----------------------------------------------------------------------

    @Test
    void should_AllowNullID_When_Constructed() {
        ObjectLock lock = new ObjectLock(null);
        assertThat(lock.getID()).isNull();
        assertThat(lock.getInstanceCount()).isEqualTo(1);
    }
}
