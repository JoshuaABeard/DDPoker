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
package com.donohoedigital.p2p;

import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.PropertyConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * I/O tests for Peer2PeerMessage - tests actual socket communication
 */
class Peer2PeerMessageIOTest {

    private ServerSocketChannel serverChannel;
    private SocketChannel clientChannel;
    private SocketChannel acceptedChannel;

    @BeforeAll
    static void setupPropertyConfig() {
        // Initialize PropertyConfig with testapp module
        String[] modules = {"testapp"};
        new PropertyConfig("testapp", modules, ApplicationType.COMMAND_LINE, null, false);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (clientChannel != null && clientChannel.isOpen()) clientChannel.close();
        if (acceptedChannel != null && acceptedChannel.isOpen()) acceptedChannel.close();
        if (serverChannel != null && serverChannel.isOpen()) serverChannel.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void should_WriteAndReadMessage_When_SocketsConnected() throws Exception {
        setupChannels();

        // Create a simple message
        DDMessage originalMsg = new DDMessage();
        originalMsg.setString("test", "value");

        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, originalMsg);

        // Write to client channel
        int bytesWritten = writeMsg.write(clientChannel);
        assertThat(bytesWritten).isGreaterThan(0);

        // Read from accepted channel
        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        // Verify
        assertThat(readMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_MSG);
        assertThat(readMsg.getMessage().getString("test")).isEqualTo("value");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void should_HandleEmptyMessage_When_NoDataAdded() throws Exception {
        setupChannels();

        DDMessage emptyMsg = new DDMessage();
        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_TEST, emptyMsg);

        writeMsg.write(clientChannel);

        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        assertThat(readMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_TEST);
    }

    /**
     * Set up a pair of connected socket channels for testing
     */
    private void setupChannels() throws IOException {
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
}
