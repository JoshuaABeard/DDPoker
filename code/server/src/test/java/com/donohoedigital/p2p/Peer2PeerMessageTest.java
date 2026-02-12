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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.PropertyConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Peer2PeerMessage - TCP wire protocol for P2P communication. Wire
 * format: 'D'(2) | protocol(4) | type(4) | size(4) | CRC32(8) | DDMessage
 * payload
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class Peer2PeerMessageTest {

    private ServerSocketChannel serverChannel;
    private SocketChannel clientChannel;
    private SocketChannel acceptedChannel;

    @BeforeAll
    static void setupPropertyConfig() {
        // Initialize PropertyConfig with testapp module
        // This loads server timeout properties from test resources
        String[] modules = {"testapp"};
        new PropertyConfig("testapp", modules, ApplicationType.COMMAND_LINE, null, false);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (clientChannel != null && clientChannel.isOpen())
            clientChannel.close();
        if (acceptedChannel != null && acceptedChannel.isOpen())
            acceptedChannel.close();
        if (serverChannel != null && serverChannel.isOpen())
            serverChannel.close();
    }

    // =================================================================
    // Round-Trip Tests
    // =================================================================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void should_ReadWrittenMessage_When_RoundTripPerformed() throws Exception {
        setupChannels();

        // Create message with test data
        DDMessage originalMsg = new DDMessage();
        originalMsg.setString("test-key", "test-value");
        originalMsg.setInteger("test-number", 42);

        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, originalMsg);

        // Write to client channel
        int bytesWritten = writeMsg.write(clientChannel);
        assertThat(bytesWritten).isGreaterThan(0);

        // Read from accepted channel
        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        // Verify content matches
        assertThat(readMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_MSG);
        assertThat(readMsg.getProtocol()).isEqualTo(1);
        assertThat(readMsg.getMessage()).isNotNull();
        assertThat(readMsg.getMessage().getString("test-key")).isEqualTo("test-value");
        assertThat(readMsg.getMessage().getInteger("test-number")).isEqualTo(42);
    }

    @Test
    void should_HandleEmptyMessage_When_NoDataAdded() throws Exception {
        setupChannels();

        // Create empty message
        DDMessage emptyMsg = new DDMessage();
        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_TEST, emptyMsg);

        // Write and read
        writeMsg.write(clientChannel);

        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        // Verify it works
        assertThat(readMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_TEST);
        assertThat(readMsg.getMessage()).isNotNull();
    }

    @Test
    void should_HandleLargeMessage_When_NearSizeLimit() throws Exception {
        setupChannels();

        // Create message with moderate size (50KB is sufficient for testing)
        DDMessage largeMsg = new DDMessage();
        StringBuilder sb = new StringBuilder();
        // Create ~50KB of data
        for (int i = 0; i < 5000; i++) {
            sb.append("0123456789");
        }
        largeMsg.setString("large-data", sb.toString());

        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, largeMsg);

        // Write and read
        writeMsg.write(clientChannel);

        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        // Verify content matches
        assertThat(readMsg.getMessage().getString("large-data")).isEqualTo(sb.toString());
    }

    @Test
    void should_HandleMultipleMessages_When_SentSequentially() throws Exception {
        setupChannels();

        // Send three messages in sequence
        for (int i = 0; i < 3; i++) {
            DDMessage msg = new DDMessage();
            msg.setInteger("sequence", i);
            Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);
            writeMsg.write(clientChannel);

            Peer2PeerMessage readMsg = new Peer2PeerMessage();
            readMsg.read(acceptedChannel);
            assertThat(readMsg.getMessage().getInteger("sequence")).isEqualTo(i);
        }
    }

    // =================================================================
    // CRC32 Validation Tests
    // =================================================================

    @Test
    void should_RejectMessage_When_CRC32Corrupted() throws Exception {
        setupChannels();

        // Manually craft a message with invalid CRC
        // Format: 'D'(2) | protocol(4) | type(4) | size(4) | CRC32(8) | payload
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.putChar('D'); // Starting char
        buffer.putInt(1); // Protocol
        buffer.putInt(Peer2PeerMessage.P2P_MSG); // Type
        buffer.putInt(10); // Size (small payload)
        buffer.putLong(0xDEADBEEF); // Invalid CRC
        buffer.put(new byte[10]); // Dummy payload
        buffer.flip();
        clientChannel.write(buffer);

        // Try to read - should fail with ApplicationError
        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        assertThatThrownBy(() -> readMsg.read(acceptedChannel)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("CRC mismatch");
    }

    @Test
    void should_RejectMessage_When_StartingCharacterInvalid() throws Exception {
        setupChannels();

        // Send invalid starting character
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.putChar('X'); // Should be 'D'
        buffer.flip();
        clientChannel.write(buffer);

        // Try to read - should fail
        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        assertThatThrownBy(() -> readMsg.read(acceptedChannel)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("no starting D");
    }

    // =================================================================
    // Message Size Limit Tests
    // =================================================================

    @Test
    void should_RejectMessage_When_SizeExceedsLimit() throws Exception {
        setupChannels();

        // Manually craft a message with size > 500000
        // Need valid CRC for protocol, type, and size fields
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.putChar('D'); // Starting char

        // Calculate CRC for the header fields
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(1); // Protocol
        crc32.update(Peer2PeerMessage.P2P_MSG); // Type
        crc32.update(600000); // Size > 500000 (invalid)

        buffer.putInt(1); // Protocol
        buffer.putInt(Peer2PeerMessage.P2P_MSG); // Type
        buffer.putInt(600000); // Size > 500000 (invalid)
        buffer.putLong(crc32.getValue()); // Valid CRC for header
        buffer.flip();
        clientChannel.write(buffer);

        // Try to read - should fail (note: error message has typo "to long" instead of
        // "too long")
        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        assertThatThrownBy(() -> readMsg.read(acceptedChannel)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("to long");
    }

    // =================================================================
    // Connection Lifecycle Tests
    // =================================================================

    @Test
    void should_ThrowEOFException_When_ChannelClosedMidRead() throws Exception {
        setupChannels();

        // Start writing a message
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.putChar('D');
        buffer.putInt(1);
        buffer.flip();
        clientChannel.write(buffer);

        // Close channel before full message is sent
        clientChannel.close();

        // Try to read - should get EOF
        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        assertThatThrownBy(() -> readMsg.read(acceptedChannel)).isInstanceOf(EOFException.class);
    }

    // =================================================================
    // Type and Protocol Tests
    // =================================================================

    @Test
    void should_PreserveMessageType_When_P2P_MSGUsed() throws Exception {
        setupChannels();

        DDMessage msg = new DDMessage();
        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);
        writeMsg.write(clientChannel);

        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        assertThat(readMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_MSG);
    }

    @Test
    void should_PreserveMessageType_When_P2P_REPLYUsed() throws Exception {
        setupChannels();

        DDMessage msg = new DDMessage();
        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_REPLY, msg);
        writeMsg.write(clientChannel);

        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        assertThat(readMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_REPLY);
    }

    @Test
    void should_PreserveMessageType_When_P2P_TESTUsed() throws Exception {
        setupChannels();

        DDMessage msg = new DDMessage();
        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_TEST, msg);
        writeMsg.write(clientChannel);

        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        assertThat(readMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_TEST);
    }

    @Test
    void should_AllowTypeChange_When_SetTypeCalled() throws Exception {
        setupChannels();

        DDMessage msg = new DDMessage();
        Peer2PeerMessage writeMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);
        writeMsg.setType(Peer2PeerMessage.P2P_REPLY);
        writeMsg.write(clientChannel);

        Peer2PeerMessage readMsg = new Peer2PeerMessage();
        readMsg.read(acceptedChannel);

        assertThat(readMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_REPLY);
    }

    // =================================================================
    // KeepAlive Tests
    // =================================================================

    @Test
    void should_DefaultToKeepAliveTrue_When_MessageCreated() {
        DDMessage msg = new DDMessage();
        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);

        assertThat(p2pMsg.isKeepAlive()).isTrue();
    }

    @Test
    void should_AllowKeepAliveChange_When_SetKeepAliveCalled() {
        DDMessage msg = new DDMessage();
        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);

        p2pMsg.setKeepAlive(false);

        assertThat(p2pMsg.isKeepAlive()).isFalse();
    }

    // =================================================================
    // From IP Tests
    // =================================================================

    @Test
    void should_StoreFromIP_When_SetFromIPCalled() {
        Peer2PeerMessage msg = new Peer2PeerMessage();

        msg.setFromIP("192.168.1.1");

        assertThat(msg.getFromIP()).isEqualTo("192.168.1.1");
    }

    @Test
    void should_ReturnNull_When_FromIPNotSet() {
        Peer2PeerMessage msg = new Peer2PeerMessage();

        assertThat(msg.getFromIP()).isNull();
    }

    // =================================================================
    // ToString Tests
    // =================================================================

    @Test
    void should_IncludeTypeAndMessage_When_ToStringCalled() {
        DDMessage msg = new DDMessage();
        msg.setString("key", "value");
        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);

        String result = p2pMsg.toString();

        assertThat(result).contains("msg");
        assertThat(result).contains("key");
    }

    @Test
    void should_IncludeFromIP_When_ToStringCalledWithIP() {
        DDMessage msg = new DDMessage();
        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);
        p2pMsg.setFromIP("192.168.1.100");

        String result = p2pMsg.toString();

        assertThat(result).contains("192.168.1.100");
    }

    // =================================================================
    // Helper Methods
    // =================================================================

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
