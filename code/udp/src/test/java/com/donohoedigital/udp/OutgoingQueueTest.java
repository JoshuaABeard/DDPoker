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

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OutgoingQueue — byte/message accounting, peak tracking, thread
 * lifecycle, and graceful shutdown.
 *
 * Note: addSend() enqueues a SendInfo + increments byte count, but the
 * background thread (which actually calls link.send()) is never started in
 * these tests. This lets us verify all counter behaviour in isolation. Tests
 * that require a real UDPLink.send() path (i.e. the full run-loop) are skipped
 * because UDPLink requires a bound socket.
 */
class OutgoingQueueTest {

    private OutgoingQueue queue;

    @BeforeEach
    void setUp() {
        queue = new OutgoingQueue();
    }

    // =================================================================
    // Initial state
    // =================================================================

    @Test
    void should_HaveZeroBytesOnQueue_When_NewlyCreated() {
        assertThat(queue.getBytesOnQueue()).isEqualTo(0L);
    }

    @Test
    void should_HaveZeroMessagesOnQueue_When_NewlyCreated() {
        assertThat(queue.getMessagesOnQueue()).isEqualTo(0);
    }

    @Test
    void should_HaveZeroPeakMessages_When_NewlyCreated() {
        assertThat(queue.getPeakMessagesOnQueue()).isEqualTo(0);
    }

    // =================================================================
    // Thread identity
    // =================================================================

    @Test
    void should_HaveCorrectThreadName_When_Created() {
        assertThat(queue.getName()).isEqualTo("OutgoingQueue");
    }

    // =================================================================
    // addSend() — byte counting (thread NOT started)
    // =================================================================

    @Test
    void should_IncreaseBytesOnQueue_When_MessageAdded() {
        UDPMessage msg = minimalMessage();
        int packetLen = msg.getPacketLength();

        queue.addSend(null, msg);

        assertThat(queue.getBytesOnQueue()).isEqualTo(packetLen);
    }

    @Test
    void should_AccumulateBytesOnQueue_When_MultipleMessagesAdded() {
        UDPMessage msg1 = minimalMessage();
        UDPMessage msg2 = minimalMessage();
        int expectedBytes = msg1.getPacketLength() + msg2.getPacketLength();

        queue.addSend(null, msg1);
        queue.addSend(null, msg2);

        assertThat(queue.getBytesOnQueue()).isEqualTo(expectedBytes);
    }

    // =================================================================
    // addSend() — message counting (thread NOT started)
    // =================================================================

    @Test
    void should_IncreaseMessagesOnQueue_When_MessageAdded() {
        queue.addSend(null, minimalMessage());

        assertThat(queue.getMessagesOnQueue()).isEqualTo(1);
    }

    @Test
    void should_ReflectCorrectCount_When_MultipleMessagesAdded() {
        queue.addSend(null, minimalMessage());
        queue.addSend(null, minimalMessage());
        queue.addSend(null, minimalMessage());

        assertThat(queue.getMessagesOnQueue()).isEqualTo(3);
    }

    // =================================================================
    // addSend() — peak tracking (thread NOT started)
    // =================================================================

    @Test
    void should_TrackPeakMessages_When_MessagesAdded() {
        queue.addSend(null, minimalMessage());
        queue.addSend(null, minimalMessage());

        assertThat(queue.getPeakMessagesOnQueue()).isGreaterThanOrEqualTo(1);
    }

    // =================================================================
    // finish() — graceful shutdown without start()
    // =================================================================

    @Test
    void should_CompleteQuickly_When_FinishCalledOnNonStartedQueue() {
        // finish() sets the done flag and puts a QUIT sentinel on the queue;
        // the join() should return immediately since the thread was never started.
        long before = System.currentTimeMillis();
        queue.finish();
        long elapsed = System.currentTimeMillis() - before;

        assertThat(elapsed).isLessThan(6000L);
    }

    // =================================================================
    // finish() — full lifecycle with start()
    // =================================================================

    @Test
    void should_StopGracefully_When_StartedThenFinished() throws InterruptedException {
        queue.setDaemon(true);
        queue.start();

        queue.finish();

        // After finish(), the thread must have exited within the 5-second join window.
        assertThat(queue.isAlive()).isFalse();
    }

    // =================================================================
    // Helper
    // =================================================================

    /**
     * Build a minimal UDPMessage with no data payload. The constructor is
     * package-private; we reach it because this test is in the same package. We
     * pass a bare UDPServer (no init() call) so getID() returns null — which is
     * acceptable here because getPacketLength() only sums HEADER_SIZE + data
     * lengths without touching field values.
     */
    private static UDPMessage minimalMessage() {
        UDPServer server = new UDPServer(new NoOpHandler(), false);
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 9999);
        return new UDPMessage(server, 1L, UDPID.UNKNOWN_ID, addr, addr);
    }

    private static class NoOpHandler implements UDPLinkHandler {
        @Override
        public int getTimeout(UDPLink link) {
            return UDPLink.DEFAULT_TIMEOUT;
        }

        @Override
        public int getPossibleTimeoutNotificationInterval(UDPLink link) {
            return 0;
        }

        @Override
        public int getPossibleTimeoutNotificationStart(UDPLink link) {
            return 0;
        }
    }
}
