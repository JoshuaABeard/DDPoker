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
package com.donohoedigital.games.poker.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerConnection TCP functionality.
 * Tests the TCP-specific behavior of PokerConnection wrapper class.
 */
class PokerConnectionTcpTest {

    private ServerSocketChannel serverChannel;
    private SocketChannel clientChannel;
    private SocketChannel acceptedChannel;

    @BeforeEach
    void setupChannels() throws IOException {
        // Create server socket
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress("127.0.0.1", 0));
        int port = serverChannel.socket().getLocalPort();

        // Create client connection
        clientChannel = SocketChannel.open();
        clientChannel.connect(new InetSocketAddress("127.0.0.1", port));
        clientChannel.configureBlocking(true);

        // Accept connection on server side
        acceptedChannel = serverChannel.accept();
        acceptedChannel.configureBlocking(true);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (clientChannel != null && clientChannel.isOpen()) clientChannel.close();
        if (acceptedChannel != null && acceptedChannel.isOpen()) acceptedChannel.close();
        if (serverChannel != null && serverChannel.isOpen()) serverChannel.close();
    }

    // =================================================================
    // TCP Connection Type Tests
    // =================================================================

    @Test
    void should_ReturnTrueForIsTCP_When_CreatedWithSocketChannel() {
        PokerConnection conn = new PokerConnection(clientChannel);

        assertThat(conn.isTCP()).isTrue();
        assertThat(conn.isUDP()).isFalse();
    }

    @Test
    void should_ReturnSocketChannel_When_GetSocketCalled() {
        PokerConnection conn = new PokerConnection(clientChannel);

        assertThat(conn.getSocket()).isEqualTo(clientChannel);
    }

    @Test
    void should_ReturnNullForUDPID_When_CreatedWithSocketChannel() {
        PokerConnection conn = new PokerConnection(clientChannel);

        assertThat(conn.getUDPID()).isNull();
    }

    // =================================================================
    // Equality Tests
    // =================================================================

    @Test
    void should_BeEqual_When_WrappingSameSocketChannel() {
        PokerConnection conn1 = new PokerConnection(clientChannel);
        PokerConnection conn2 = new PokerConnection(clientChannel);

        assertThat(conn1).isEqualTo(conn2);
    }

    @Test
    void should_NotBeEqual_When_WrappingDifferentSocketChannels() {
        PokerConnection conn1 = new PokerConnection(clientChannel);
        PokerConnection conn2 = new PokerConnection(acceptedChannel);

        assertThat(conn1).isNotEqualTo(conn2);
    }

    @Test
    void should_BeEqualToItself_When_SameInstance() {
        PokerConnection conn = new PokerConnection(clientChannel);

        assertThat(conn).isEqualTo(conn);
    }

    @Test
    void should_NotBeEqual_When_ComparedToNull() {
        PokerConnection conn = new PokerConnection(clientChannel);

        assertThat(conn).isNotEqualTo(null);
    }

    @Test
    void should_NotBeEqual_When_ComparedToDifferentType() {
        PokerConnection conn = new PokerConnection(clientChannel);

        assertThat(conn).isNotEqualTo("not a connection");
    }

    // =================================================================
    // HashCode Tests
    // =================================================================

    @Test
    void should_HaveConsistentHashCode_When_CalledMultipleTimes() {
        PokerConnection conn = new PokerConnection(clientChannel);

        int hash1 = conn.hashCode();
        int hash2 = conn.hashCode();

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void should_HaveSameHashCode_When_WrappingSameSocketChannel() {
        PokerConnection conn1 = new PokerConnection(clientChannel);
        PokerConnection conn2 = new PokerConnection(clientChannel);

        assertThat(conn1.hashCode()).isEqualTo(conn2.hashCode());
    }

    @Test
    void should_MatchSocketChannelHashCode_When_CreatedWithSocketChannel() {
        PokerConnection conn = new PokerConnection(clientChannel);

        assertThat(conn.hashCode()).isEqualTo(clientChannel.hashCode());
    }

    // =================================================================
    // ToString Tests
    // =================================================================

    @Test
    void should_ReturnIPAddress_When_ToStringCalledOnTCPConnection() {
        PokerConnection conn = new PokerConnection(clientChannel);

        String result = conn.toString();

        // Should contain IP address format
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        // Should contain "127.0.0.1" or similar loopback address
        assertThat(result).matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+.*");
    }

    @Test
    void should_ReturnConsistentString_When_ToStringCalledMultipleTimes() {
        PokerConnection conn = new PokerConnection(clientChannel);

        String str1 = conn.toString();
        String str2 = conn.toString();

        assertThat(str1).isEqualTo(str2);
    }

    // =================================================================
    // Edge Cases
    // =================================================================

    @Test
    void should_HandleClosedChannel_When_ToStringCalled() throws IOException {
        PokerConnection conn = new PokerConnection(clientChannel);
        clientChannel.close();

        // Should not throw exception
        String result = conn.toString();
        assertThat(result).isNotNull();
    }
}
