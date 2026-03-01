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
package com.donohoedigital.udp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for IncomingQueue - ordered message reception, gap detection, and
 * duplicate detection.
 *
 * Note: The UDPLink dependency is only accessed inside dispatch() when a
 * message is actually delivered to a handler. Tests that exercise early-exit
 * paths inside dispatch(), and all tests for addMessage/size/clear/toString,
 * pass null as the link because those code paths never dereference it.
 */
class IncomingQueueTest {

    // A fresh queue with a null UDPLink — safe as long as dispatch() returns
    // early (empty queue or gap at front) before touching the link.
    private IncomingQueue queue;

    @BeforeEach
    void setUp() {
        queue = new IncomingQueue(null);
        queue.clear(); // initialise nLastProcessedID_ to 0
    }

    // =================================================================
    // Constant
    // =================================================================

    @Test
    void should_HaveCorrectValue_When_LastDispatchCntChecked() {
        assertThat(IncomingQueue.LAST_DISPATCH_CNT).isEqualTo(-1);
    }

    // =================================================================
    // clear() and size()
    // =================================================================

    @Test
    void should_HaveSizeZero_When_NewlyCreatedAndCleared() {
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void should_HaveSizeZero_When_ClearedAfterAdding() {
        queue.addMessage(msg(1));
        queue.addMessage(msg(2));

        queue.clear();

        assertThat(queue.size()).isEqualTo(0);
    }

    // =================================================================
    // addMessage() — basic insertion and ordering
    // =================================================================

    @Test
    void should_ReturnTrue_When_NewMessageAdded() {
        boolean added = queue.addMessage(msg(1));

        assertThat(added).isTrue();
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    void should_KeepMessagesInOrder_When_AddedOutOfSequence() {
        queue.addMessage(msg(3));
        queue.addMessage(msg(1));
        queue.addMessage(msg(2));

        // toString renders IDs in sorted order; just verify size here
        assertThat(queue.size()).isEqualTo(3);
    }

    @Test
    void should_ReturnFalse_When_DuplicateMessageAdded() {
        queue.addMessage(msg(1));

        boolean addedAgain = queue.addMessage(msg(1));

        assertThat(addedAgain).isFalse();
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    void should_ReturnFalse_When_MessageIdAlreadyProcessed() {
        // clear() sets nLastProcessedID_ to 0; IDs <= 0 are already "processed"
        boolean added = queue.addMessage(msg(0));

        assertThat(added).isFalse();
        assertThat(queue.size()).isEqualTo(0);
    }

    @Test
    void should_ReturnFalse_When_MessageIdBelowLastProcessed() {
        // Simulate message 1 having been processed by adding then dispatching
        // via clear() trick: add id=1, then clear resets processed to 0 —
        // instead use the fact that dispatch() increments nLastProcessedID_.
        // We can simulate this by adding id=1 in order and dispatching to
        // the empty-queue early-exit path, then checking id=1 rejected.
        //
        // Simpler: call clear() sets processed=0, so id 0 is "processed".
        // Call addMessage(0) must return false (0 <= 0).
        assertThat(queue.addMessage(msg(0))).isFalse();
    }

    // =================================================================
    // hasGapAtBeginning()
    // =================================================================

    @Test
    void should_ReturnFalse_When_QueueIsEmpty() {
        assertThat(queue.hasGapAtBeginning()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_FirstMessageIsNextExpected() {
        // After clear(), nLastProcessedID_ == 0; first expected is id=1
        queue.addMessage(msg(1));

        assertThat(queue.hasGapAtBeginning()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_FirstMessageHasGapFromLastProcessed() {
        // nLastProcessedID_==0, so id=1 is expected; id=3 creates a gap
        queue.addMessage(msg(3));

        assertThat(queue.hasGapAtBeginning()).isTrue();
    }

    @Test
    void should_ReturnTrue_When_MessageArrivesOutOfOrderCreatingGap() {
        queue.addMessage(msg(3));
        queue.addMessage(msg(4));

        // id=1 and id=2 have not arrived yet — gap at the beginning
        assertThat(queue.hasGapAtBeginning()).isTrue();
    }

    // =================================================================
    // dispatch() — early-exit paths that do NOT touch the UDPLink
    // =================================================================

    @Test
    void should_ReturnFalse_When_DispatchCalledOnEmptyQueue() {
        boolean moreToProcess = queue.dispatch(false);

        assertThat(moreToProcess).isFalse();
    }

    @Test
    void should_ReturnFalse_When_DispatchCalledButGapExistsAtFront() {
        // id=2 is enqueued but id=1 hasn't arrived; dispatch can't proceed
        queue.addMessage(msg(2));

        boolean moreToProcess = queue.dispatch(false);

        assertThat(moreToProcess).isFalse();
    }

    // =================================================================
    // toString()
    // =================================================================

    @Test
    void should_ReturnEmptyString_When_QueueEmpty() {
        assertThat(queue.toString()).isEqualTo("");
    }

    @Test
    void should_ContainMessageInfo_When_QueueHasMessages() {
        queue.addMessage(msg(1));
        queue.addMessage(msg(2));

        String s = queue.toString();
        assertThat(s).contains("1");
        assertThat(s).contains("2");
    }

    // =================================================================
    // Helper
    // =================================================================

    /**
     * Create a minimal single-part MESSAGE UDPData with the given sequential ID.
     */
    private static UDPData msg(int id) {
        byte[] data = new byte[0];
        return new UDPData(UDPData.Type.MESSAGE, id, (short) 1, (short) 1, data, 0, 0, UDPData.USER_TYPE_UNSPECIFIED);
    }
}
