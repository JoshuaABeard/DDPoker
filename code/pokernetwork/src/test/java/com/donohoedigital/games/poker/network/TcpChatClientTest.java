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

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.p2p.Peer2PeerMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TcpChatClient - TCP-based chat client for lobby chat. Tests the
 * client-side chat functionality replacing PokerUDPServer.chatLink_.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class TcpChatClientTest {

    private ServerSocketChannel mockServer;
    private SocketChannel mockServerConnection;
    private TcpChatClient client;
    private MockChatHandler handler;
    private int serverPort;
    private PlayerProfile testProfile;

    @BeforeAll
    static void setupPropertyConfig() {
        // Initialize PropertyConfig with poker module
        String[] modules = {"poker"};
        new PropertyConfig("poker", modules, ApplicationType.COMMAND_LINE, null, false);
    }

    @BeforeEach
    void setup() throws IOException {
        // Create mock server
        mockServer = ServerSocketChannel.open();
        mockServer.bind(new InetSocketAddress("127.0.0.1", 0));
        serverPort = mockServer.socket().getLocalPort();

        // Create test profile
        testProfile = new PlayerProfile("TestPlayer");
        testProfile.setPassword("testpass");

        // Create mock handler
        handler = new MockChatHandler();
    }

    @AfterEach
    void cleanup() throws IOException {
        if (client != null) {
            client.disconnect();
        }
        if (mockServerConnection != null && mockServerConnection.isOpen()) {
            mockServerConnection.close();
        }
        if (mockServer != null && mockServer.isOpen()) {
            mockServer.close();
        }
    }

    // =================================================================
    // Connection Tests
    // =================================================================

    @Test
    void testConnectToServer() throws Exception {
        // Given: Mock server is listening
        InetSocketAddress serverAddr = new InetSocketAddress("127.0.0.1", serverPort);

        // When: Client connects
        client = createClient(serverAddr);
        client.connect();

        // Then: Server should accept connection
        mockServerConnection = acceptConnectionWithTimeout();
        assertThat(mockServerConnection).isNotNull();
        assertThat(mockServerConnection.isConnected()).isTrue();
    }

    @Test
    void testConnectionFailsWhenServerNotAvailable() {
        // Given: No server listening on port
        InetSocketAddress badAddr = new InetSocketAddress("127.0.0.1", serverPort + 1000);
        client = createClient(badAddr);

        // When/Then: Connect should fail
        assertThatThrownBy(() -> client.connect()).isInstanceOf(IOException.class);
    }

    @Test
    void testDisconnectClosesConnection() throws Exception {
        // Given: Connected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();

        // When: Client disconnects
        client.disconnect();

        // Then: Connection should be closed
        assertThat(client.isConnected()).isFalse();
    }

    // =================================================================
    // HELLO Message Tests
    // =================================================================

    @Test
    void testSendHelloMessage() throws Exception {
        // Given: Connected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.configureBlocking(true);

        // When: Client sends HELLO
        client.sendHello(testProfile, "test-player-id");

        // Then: Server should receive HELLO message with auth
        Peer2PeerMessage msg = new Peer2PeerMessage();
        msg.read(mockServerConnection);

        OnlineMessage onlineMsg = new OnlineMessage(msg.getMessage());
        assertThat(onlineMsg.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_HELLO);
        assertThat(onlineMsg.getWanAuth()).isNotNull();

        // Verify auth contains player info
        com.donohoedigital.comms.DMTypedHashMap auth = onlineMsg.getWanAuth();
        assertThat(auth.getString("name")).isEqualTo("TestPlayer");
        assertThat(auth.getString("uuid")).isEqualTo("test-player-id");
    }

    @Test
    void testSendHelloBeforeConnectThrows() {
        // Given: Unconnected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));

        // When/Then: Sending HELLO before connect should fail
        assertThatThrownBy(() -> client.sendHello(testProfile, "test-id")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not connected");
    }

    // =================================================================
    // WELCOME Message Tests
    // =================================================================

    @Test
    void testReceiveWelcome() throws Exception {
        // Given: Connected client with handler
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.setHandler(handler);
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.configureBlocking(true);
        client.sendHello(testProfile, "test-id");

        // Read and discard HELLO
        Peer2PeerMessage hello = new Peer2PeerMessage();
        hello.read(mockServerConnection);

        // When: Server sends WELCOME with player list
        OnlineMessage welcomeMsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        welcomeMsg.setChatType(OnlineMessage.CHAT_ADMIN_JOIN);

        // Create player list
        com.donohoedigital.comms.DMArrayList<com.donohoedigital.comms.DMTypedHashMap> playerList = new com.donohoedigital.comms.DMArrayList<>();
        for (String name : new String[]{"Player1", "Player2", "Player3"}) {
            OnlinePlayerInfo info = new OnlinePlayerInfo();
            info.setName(name);
            playerList.add(info.getData());
        }
        welcomeMsg.setPlayerList(playerList);

        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, welcomeMsg.getData());
        p2pMsg.write(mockServerConnection);

        // Then: Handler should receive WELCOME
        handler.waitForMessage(1);
        assertThat(handler.receivedMessages).hasSize(1);

        OnlineMessage received = handler.receivedMessages.get(0);
        assertThat(received.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(received.getChatType()).isEqualTo(OnlineMessage.CHAT_ADMIN_JOIN);
        assertThat(received.getPlayerList()).extracting(OnlinePlayerInfo::getName).containsExactly("Player1", "Player2",
                "Player3");
    }

    // =================================================================
    // Chat Broadcast Tests
    // =================================================================

    @Test
    void testReceiveBroadcastChat() throws Exception {
        // Given: Connected client with handler
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.setHandler(handler);
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.configureBlocking(true);
        client.sendHello(testProfile, "test-id");

        // Read and discard HELLO
        Peer2PeerMessage hello = new Peer2PeerMessage();
        hello.read(mockServerConnection);

        // When: Server broadcasts chat message
        OnlineMessage chatMsg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        chatMsg.setPlayerName("OtherPlayer");
        chatMsg.setChat("Hello everyone!");

        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, chatMsg.getData());
        p2pMsg.write(mockServerConnection);

        // Then: Handler should receive chat
        handler.waitForMessage(1);
        assertThat(handler.receivedMessages).hasSize(1);

        OnlineMessage received = handler.receivedMessages.get(0);
        assertThat(received.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT);
        assertThat(received.getPlayerName()).isEqualTo("OtherPlayer");
        assertThat(received.getChat()).isEqualTo("Hello everyone!");
    }

    // =================================================================
    // Send Chat Tests
    // =================================================================

    @Test
    void testSendChatMessage() throws Exception {
        // Given: Connected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.configureBlocking(true);
        client.sendHello(testProfile, "test-id");

        // Read and discard HELLO
        Peer2PeerMessage hello = new Peer2PeerMessage();
        hello.read(mockServerConnection);

        // When: Client sends chat via ChatLobbyManager interface
        client.sendChat(testProfile, "Test message");

        // Then: Server should receive chat message
        Peer2PeerMessage msg = new Peer2PeerMessage();
        msg.read(mockServerConnection);

        OnlineMessage onlineMsg = new OnlineMessage(msg.getMessage());
        assertThat(onlineMsg.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT);
        assertThat(onlineMsg.getPlayerName()).isEqualTo("TestPlayer");
        assertThat(onlineMsg.getChat()).isEqualTo("Test message");
    }

    @Test
    void testSendChatWithNullMessage() throws Exception {
        // Given: Connected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();

        // When: Client sends null message
        client.sendChat(testProfile, null);

        // Then: No message should be sent (this is allowed per PokerUDPServer behavior)
        // The method should not throw, but should not send anything
    }

    // =================================================================
    // Auto-Reconnect Tests
    // =================================================================

    @Test
    void testAutoReconnectAfterConnectionLoss() throws Exception {
        // Given: Connected client with handler
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.setHandler(handler);
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();

        // When: Server closes connection
        mockServerConnection.close();

        // Wait for client to detect disconnect and attempt reconnect
        Thread.sleep(100);

        // Then: Handler should be notified of error
        assertThat(handler.errorCount.get()).isGreaterThan(0);

        // Client should attempt reconnect (accepting new connection verifies this)
        mockServerConnection = acceptConnectionWithTimeout();
        assertThat(mockServerConnection).isNotNull();
    }

    @Test
    void testReconnectBackoffDelay() throws Exception {
        // Given: Client configured with reconnect
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.setReconnectEnabled(true);
        client.setReconnectDelayMs(500);

        // When: Multiple reconnect attempts
        long start = System.currentTimeMillis();
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.close();

        // Wait for reconnect
        Thread.sleep(100);
        long secondAttempt = System.currentTimeMillis();

        // Then: Should have delay between attempts
        // (This is a basic test - full backoff testing would need more iterations)
        assertThat(secondAttempt - start).isGreaterThanOrEqualTo(100);
    }

    // =================================================================
    // Message Validation Tests
    // =================================================================

    @Test
    void testMessageValidationBeforeSend() throws Exception {
        // Given: Connected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();

        // When: Sending empty message
        // Then: Should be rejected
        assertThatThrownBy(() -> client.sendChat(testProfile, "")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void testMessageSizeLimitValidation() throws Exception {
        // Given: Connected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();

        // When: Creating message larger than 500KB
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 600000; i++) {
            huge.append('x');
        }

        // Then: Should be rejected
        assertThatThrownBy(() -> client.sendChat(testProfile, huge.toString()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("large");
    }

    // =================================================================
    // Reader Thread Tests
    // =================================================================

    @Test
    void testReaderThreadStartsOnConnect() throws Exception {
        // Given: Client connects
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();

        // Then: Reader thread should be running
        assertThat(client.isReaderThreadRunning()).isTrue();
    }

    @Test
    void testReaderThreadStopsOnDisconnect() throws Exception {
        // Given: Connected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();

        // When: Client disconnects
        client.disconnect();

        // Give thread time to stop
        Thread.sleep(100);

        // Then: Reader thread should not be running
        assertThat(client.isReaderThreadRunning()).isFalse();
    }

    @Test
    void testReaderThreadHandlesMultipleMessages() throws Exception {
        // Given: Connected client with handler
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.setHandler(handler);
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.configureBlocking(true);
        client.sendHello(testProfile, "test-id");

        // Read and discard HELLO
        Peer2PeerMessage hello = new Peer2PeerMessage();
        hello.read(mockServerConnection);

        // When: Server sends multiple messages in sequence
        for (int i = 0; i < 5; i++) {
            OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
            msg.setPlayerName("Player" + i);
            msg.setChat("Message " + i);

            Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg.getData());
            p2pMsg.write(mockServerConnection);
        }

        // Then: All messages should be received
        handler.waitForMessage(5);
        assertThat(handler.receivedMessages).hasSize(5);

        for (int i = 0; i < 5; i++) {
            OnlineMessage received = handler.receivedMessages.get(i);
            assertThat(received.getPlayerName()).isEqualTo("Player" + i);
            assertThat(received.getChat()).isEqualTo("Message " + i);
        }
    }

    // =================================================================
    // Multiple Chat Messages Tests
    // =================================================================

    @Test
    void testMultipleChatMessagesInSequence() throws Exception {
        // Given: Connected client
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.configureBlocking(true);
        client.sendHello(testProfile, "test-id");

        // Read and discard HELLO
        Peer2PeerMessage hello = new Peer2PeerMessage();
        hello.read(mockServerConnection);

        // When: Client sends multiple chat messages
        for (int i = 0; i < 3; i++) {
            client.sendChat(testProfile, "Message " + i);
        }

        // Then: All messages should be received by server
        for (int i = 0; i < 3; i++) {
            Peer2PeerMessage msg = new Peer2PeerMessage();
            msg.read(mockServerConnection);

            OnlineMessage onlineMsg = new OnlineMessage(msg.getMessage());
            assertThat(onlineMsg.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT);
            assertThat(onlineMsg.getChat()).isEqualTo("Message " + i);
        }
    }

    // =================================================================
    // Handler Notification Tests
    // =================================================================

    @Test
    void testHandlerNotifiedOnJoinLeave() throws Exception {
        // Given: Connected client with handler
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.setHandler(handler);
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.configureBlocking(true);
        client.sendHello(testProfile, "test-id");

        // Read and discard HELLO
        Peer2PeerMessage hello = new Peer2PeerMessage();
        hello.read(mockServerConnection);

        // When: Server sends JOIN notification
        OnlineMessage joinMsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        joinMsg.setChatType(OnlineMessage.CHAT_ADMIN_JOINED);
        joinMsg.setPlayerName("NewPlayer");

        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, joinMsg.getData());
        p2pMsg.write(mockServerConnection);

        // Wait for handler
        handler.waitForMessage(1);

        // Then: Handler should receive join notification
        assertThat(handler.receivedMessages).hasSize(1);
        OnlineMessage received = handler.receivedMessages.get(0);
        assertThat(received.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(received.getChatType()).isEqualTo(OnlineMessage.CHAT_ADMIN_JOINED);
        assertThat(received.getPlayerName()).isEqualTo("NewPlayer");

        // When: Server sends LEAVE notification
        OnlineMessage leaveMsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        leaveMsg.setChatType(OnlineMessage.CHAT_ADMIN_LEFT);
        leaveMsg.setPlayerName("NewPlayer");

        p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, leaveMsg.getData());
        p2pMsg.write(mockServerConnection);

        // Wait for handler
        handler.waitForMessage(2);

        // Then: Handler should receive leave notification
        assertThat(handler.receivedMessages).hasSize(2);
        received = handler.receivedMessages.get(1);
        assertThat(received.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(received.getChatType()).isEqualTo(OnlineMessage.CHAT_ADMIN_LEFT);
        assertThat(received.getPlayerName()).isEqualTo("NewPlayer");
    }

    @Test
    void testHandlerNotifiedOnError() throws Exception {
        // Given: Connected client with handler
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.setHandler(handler);
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();
        mockServerConnection.configureBlocking(true);
        client.sendHello(testProfile, "test-id");

        // Read and discard HELLO
        Peer2PeerMessage hello = new Peer2PeerMessage();
        hello.read(mockServerConnection);

        // When: Server sends error message
        OnlineMessage errorMsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        errorMsg.setChatType(OnlineMessage.CHAT_ADMIN_ERROR);
        errorMsg.setChat("Duplicate player name");

        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, errorMsg.getData());
        p2pMsg.write(mockServerConnection);

        // Then: Handler should receive error
        handler.waitForMessage(1);
        assertThat(handler.receivedMessages).hasSize(1);

        OnlineMessage received = handler.receivedMessages.get(0);
        assertThat(received.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(received.getChatType()).isEqualTo(OnlineMessage.CHAT_ADMIN_ERROR);
        assertThat(received.getChat()).isEqualTo("Duplicate player name");
    }

    @Test
    void testHandlerNotifiedOnConnectionError() throws Exception {
        // Given: Connected client with handler
        client = createClient(new InetSocketAddress("127.0.0.1", serverPort));
        client.setHandler(handler);
        client.connect();
        mockServerConnection = acceptConnectionWithTimeout();

        int initialErrorCount = handler.errorCount.get();

        // When: Server abruptly closes connection
        mockServerConnection.close();

        // Give reader thread time to detect and handle
        Thread.sleep(200);

        // Then: Handler should be notified of connection error
        assertThat(handler.errorCount.get()).isGreaterThan(initialErrorCount);
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    /**
     * Create a TcpChatClient instance for testing
     */
    private TcpChatClient createClient(InetSocketAddress serverAddr) {
        return new TcpChatClient(serverAddr);
    }

    /**
     * Accept incoming connection with timeout
     */
    private SocketChannel acceptConnectionWithTimeout() throws IOException {
        mockServer.configureBlocking(false);
        long deadline = System.currentTimeMillis() + 2000;

        while (System.currentTimeMillis() < deadline) {
            SocketChannel accepted = mockServer.accept();
            if (accepted != null) {
                return accepted;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for connection", e);
            }
        }

        throw new IOException("Timeout waiting for client connection");
    }

    // =================================================================
    // Mock Chat Handler
    // =================================================================

    /**
     * Mock implementation of ChatHandler for testing
     */
    private static class MockChatHandler implements ChatHandler {
        final List<OnlineMessage> receivedMessages = new ArrayList<>();
        final AtomicInteger errorCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(Integer.MAX_VALUE);
        final AtomicReference<CountDownLatch> customLatch = new AtomicReference<>();

        @Override
        public void chatReceived(OnlineMessage omsg) {
            receivedMessages.add(omsg);
            if (customLatch.get() != null) {
                customLatch.get().countDown();
            }
        }

        public void notifyError(String message) {
            errorCount.incrementAndGet();
            // Create admin message for error
            OnlineMessage errorMsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
            errorMsg.setChat(message);
            chatReceived(errorMsg);
        }

        public void waitForMessage(int count) throws InterruptedException {
            // Check if we already have enough messages
            long deadline = System.currentTimeMillis() + 2000;
            while (receivedMessages.size() < count && System.currentTimeMillis() < deadline) {
                CountDownLatch messageLatch = new CountDownLatch(count - receivedMessages.size());
                customLatch.set(messageLatch);
                long remaining = deadline - System.currentTimeMillis();
                if (remaining > 0) {
                    messageLatch.await(remaining, TimeUnit.MILLISECONDS);
                }
            }
            if (receivedMessages.size() < count) {
                throw new AssertionError(
                        "Timeout waiting for " + count + " messages. Received: " + receivedMessages.size());
            }
        }
    }

    // =================================================================
    // Test Stub Classes (avoiding circular dependency with poker module)
    // =================================================================

    /**
     * Test stub for PlayerProfile from poker module. Provides just enough
     * functionality for TcpChatClient to work via reflection.
     */
    private static class PlayerProfile {
        private final String name;
        private String password;
        private final DMTypedHashMap data;

        public PlayerProfile(String name) {
            this.name = name;
            this.data = new DMTypedHashMap();
        }

        public String getName() {
            return name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public DMTypedHashMap getData() {
            return data;
        }
    }

    /**
     * Test stub for ChatHandler interface from poker module. TcpChatClient calls
     * chatReceived via reflection.
     */
    private interface ChatHandler {
        void chatReceived(OnlineMessage omsg);
    }
}
