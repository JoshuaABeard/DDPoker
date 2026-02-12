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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.network.OnlineMessage;
import com.donohoedigital.games.poker.network.OnlinePlayerInfo;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.server.service.BannedKeyService;
import com.donohoedigital.p2p.Peer2PeerMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TDD tests for TcpChatServer - TCP-based chat server for lobby. Tests written
 * before implementation per TDD approach.
 *
 * NOTE: Disabled due to Mockito incompatibility with Java 25. ByteBuddy (used
 * by Mockito) officially supports up to Java 24. Tests will be re-enabled when
 * Mockito/ByteBuddy add Java 25 support.
 */
@Disabled("Java 25 incompatibility with Mockito/ByteBuddy")
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class TcpChatServerTest {

    private TcpChatServer chatServer;
    private OnlineProfileService mockProfileService;
    private BannedKeyService mockBannedKeyService;
    private List<SocketChannel> clientChannels;
    private int chatServerPort;

    @BeforeAll
    static void setupPropertyConfig() {
        // Initialize PropertyConfig with testapp module (matches Peer2PeerMessageTest
        // pattern)
        String[] modules = {"testapp"};
        new PropertyConfig("testapp", modules, ApplicationType.COMMAND_LINE, null, false);
    }

    @BeforeEach
    void setup() throws Exception {
        // Setup mock services
        mockProfileService = mock(OnlineProfileService.class);
        mockBannedKeyService = mock(BannedKeyService.class);

        // Setup server (will be created in each test as needed)
        clientChannels = new ArrayList<>();
    }

    @AfterEach
    void cleanup() throws Exception {
        // Close all client channels
        for (SocketChannel channel : clientChannels) {
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (Exception ignored) {
                }
            }
        }
        clientChannels.clear();

        // Shutdown server
        if (chatServer != null) {
            chatServer.shutdown();
            chatServer = null;
        }
    }

    // =================================================================
    // Test 1: Connect and Send Hello
    // =================================================================

    @Test
    void testConnectAndSendHello() throws Exception {
        // Setup valid auth
        String licenseKey = "valid-key-12345";
        String profileName = "TestPlayer";
        setupValidProfile(profileName, licenseKey);

        // Start server
        setupServer();

        // Connect client
        SocketChannel client = connectClient();

        // Send HELLO message
        OnlineMessage hello = createHelloMessage(profileName, licenseKey);
        sendMessage(client, hello);

        // Receive WELCOME
        Peer2PeerMessage response = receiveMessage(client);

        // Verify WELCOME received
        assertThat(response).isNotNull();
        OnlineMessage welcome = new OnlineMessage(response.getMessage());
        assertThat(welcome.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(welcome.getChatType()).isEqualTo(PokerConstants.CHAT_ADMIN_WELCOME);
        assertThat(welcome.getChat()).contains("TestPlayer");

        // Verify player list is present and contains our player
        List<OnlinePlayerInfo> playerList = welcome.getPlayerList();
        assertThat(playerList).isNotNull();
        assertThat(playerList).hasSize(1);

        OnlinePlayerInfo player = playerList.get(0);
        assertThat(player.getName()).isEqualTo(profileName);
    }

    // =================================================================
    // Test 2: Receive Broadcast Chat
    // =================================================================

    @Test
    void testReceiveBroadcastChat() throws Exception {
        // Setup two valid profiles
        String key1 = "key-player1";
        String key2 = "key-player2";
        setupValidProfile("Player1", key1);
        setupValidProfile("Player2", key2);

        // Start server
        setupServer();

        // Connect two clients
        SocketChannel client1 = connectClient();
        SocketChannel client2 = connectClient();

        // Both send HELLO
        sendMessage(client1, createHelloMessage("Player1", key1));
        sendMessage(client2, createHelloMessage("Player2", key2));

        // Read WELCOME messages
        receiveMessage(client1); // WELCOME for client1
        receiveMessage(client2); // WELCOME for client2
        receiveMessage(client1); // JOIN notification for Player2

        // Client1 sends chat
        OnlineMessage chatMsg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        chatMsg.setChat("Hello everyone!");
        chatMsg.setPlayerName("Player1");
        sendMessage(client1, chatMsg);

        // Client2 should receive the chat
        Peer2PeerMessage received = receiveMessage(client2);
        assertThat(received).isNotNull();

        OnlineMessage receivedChat = new OnlineMessage(received.getMessage());
        assertThat(receivedChat.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT);
        assertThat(receivedChat.getChat()).isEqualTo("Hello everyone!");
        assertThat(receivedChat.getPlayerName()).isEqualTo("Player1");

        // Client1 should NOT receive their own chat back
        // (set short timeout to verify no message arrives)
        Peer2PeerMessage noMessage = receiveMessageWithTimeout(client1, 500);
        assertThat(noMessage).isNull();
    }

    // =================================================================
    // Test 3: Connection Lost Detection
    // =================================================================

    @Test
    void testConnectionLostDetection() throws Exception {
        // Setup valid profile
        String key = "key-player1";
        setupValidProfile("Player1", key);

        // Start server and connect second client to observe
        setupServer();
        SocketChannel client1 = connectClient();
        SocketChannel client2 = connectClient();

        // Both connect
        sendMessage(client1, createHelloMessage("Player1", key));
        setupValidProfile("Player2", "key-player2");
        sendMessage(client2, createHelloMessage("Player2", "key-player2"));

        // Clear welcome messages
        receiveMessage(client1); // WELCOME
        receiveMessage(client2); // WELCOME
        receiveMessage(client1); // JOIN for Player2

        // Client1 disconnects abruptly (close socket)
        client1.close();

        // Client2 should receive LEAVE notification
        Peer2PeerMessage leaveMsg = receiveMessage(client2);
        assertThat(leaveMsg).isNotNull();

        OnlineMessage leave = new OnlineMessage(leaveMsg.getMessage());
        assertThat(leave.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(leave.getChatType()).isEqualTo(PokerConstants.CHAT_ADMIN_LEAVE);
        assertThat(leave.getPlayerInfo().getName()).isEqualTo("Player1");
    }

    // =================================================================
    // Test 4: Reconnect After Loss
    // =================================================================

    @Test
    void testReconnectAfterLoss() throws Exception {
        // Setup valid profile
        String key = "key-player1";
        String profile = "Player1";
        setupValidProfile(profile, key);

        // Start server
        setupServer();

        // Connect, hello, disconnect
        SocketChannel client1 = connectClient();
        sendMessage(client1, createHelloMessage(profile, key));
        receiveMessage(client1); // WELCOME
        client1.close();

        // Brief pause to ensure server detects disconnect
        Thread.sleep(100);

        // Reconnect with same profile
        SocketChannel client2 = connectClient();
        sendMessage(client2, createHelloMessage(profile, key));

        // Should receive fresh WELCOME
        Peer2PeerMessage welcome = receiveMessage(client2);
        assertThat(welcome).isNotNull();

        OnlineMessage welcomeMsg = new OnlineMessage(welcome.getMessage());
        assertThat(welcomeMsg.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(welcomeMsg.getChatType()).isEqualTo(PokerConstants.CHAT_ADMIN_WELCOME);
    }

    // =================================================================
    // Test 5: Receive Player List
    // =================================================================

    @Test
    void testReceivePlayerList() throws Exception {
        // Setup three valid profiles
        setupValidProfile("Alice", "key-alice");
        setupValidProfile("Bob", "key-bob");
        setupValidProfile("Carol", "key-carol");

        // Start server
        setupServer();

        // Connect Alice and Bob first
        SocketChannel alice = connectClient();
        SocketChannel bob = connectClient();
        sendMessage(alice, createHelloMessage("Alice", "key-alice"));
        sendMessage(bob, createHelloMessage("Bob", "key-bob"));

        // Clear their welcomes
        receiveMessage(alice); // WELCOME
        receiveMessage(bob); // WELCOME
        receiveMessage(alice); // Bob JOIN

        // Carol connects
        SocketChannel carol = connectClient();
        sendMessage(carol, createHelloMessage("Carol", "key-carol"));

        // Carol receives WELCOME with player list containing Alice and Bob
        Peer2PeerMessage welcome = receiveMessage(carol);
        OnlineMessage welcomeMsg = new OnlineMessage(welcome.getMessage());

        List<OnlinePlayerInfo> playerList = welcomeMsg.getPlayerList();
        assertThat(playerList).isNotNull();
        assertThat(playerList).hasSize(3); // Alice, Bob, Carol

        List<String> names = new ArrayList<>();
        for (OnlinePlayerInfo player : playerList) {
            names.add(player.getName());
        }

        assertThat(names).containsExactlyInAnyOrder("Alice", "Bob", "Carol");
    }

    // =================================================================
    // Test 6: Broadcast Does Not Block on Slow Client
    // =================================================================

    @Test
    void testBroadcastDoesNotBlockOnSlowClient() throws Exception {
        // Setup profiles
        setupValidProfile("FastClient", "key-fast");
        setupValidProfile("SlowClient", "key-slow");
        setupValidProfile("Sender", "key-sender");

        // Start server
        setupServer();

        // Connect all clients
        SocketChannel fast = connectClient();
        SocketChannel slow = connectClient();
        SocketChannel sender = connectClient();

        // All send HELLO
        sendMessage(fast, createHelloMessage("FastClient", "key-fast"));
        sendMessage(slow, createHelloMessage("SlowClient", "key-slow"));
        sendMessage(sender, createHelloMessage("Sender", "key-sender"));

        // Clear welcome messages for fast and sender
        receiveMessage(fast); // WELCOME
        receiveMessage(sender); // WELCOME
        receiveMessage(slow); // WELCOME
        receiveMessage(fast); // SlowClient JOIN
        receiveMessage(sender); // FastClient JOIN
        receiveMessage(fast); // Sender JOIN
        receiveMessage(sender); // SlowClient JOIN

        // Simulate slow client by NOT reading from its channel
        // (leave messages queued in kernel buffer)

        // Sender sends multiple chat messages
        for (int i = 0; i < 10; i++) {
            OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT);
            chat.setChat("Message " + i);
            chat.setPlayerName("Sender");
            sendMessage(sender, chat);
        }

        // Fast client should receive all messages without blocking
        for (int i = 0; i < 10; i++) {
            Peer2PeerMessage msg = receiveMessage(fast);
            assertThat(msg).isNotNull();
            OnlineMessage chat = new OnlineMessage(msg.getMessage());
            assertThat(chat.getChat()).isEqualTo("Message " + i);
        }

        // Slow client may be disconnected by server or messages may be queued
        // The key requirement is that fast client was not blocked
        // This test verifies that fast client got all messages promptly
    }

    // =================================================================
    // Test 7: Concurrent Connect/Disconnect
    // =================================================================

    @Test
    void testConcurrentConnectDisconnect() throws Exception {
        // Setup multiple profiles
        for (int i = 0; i < 10; i++) {
            setupValidProfile("Player" + i, "key-player" + i);
        }

        // Start server
        setupServer();

        // Track successful connections
        AtomicInteger successfulConnects = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        // Spawn 10 threads that connect, hello, chat, disconnect
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int playerId = i;
            Thread t = new Thread(() -> {
                try {
                    SocketChannel client = SocketChannel.open();
                    client.connect(new InetSocketAddress("127.0.0.1", chatServerPort));
                    client.configureBlocking(true);
                    synchronized (clientChannels) {
                        clientChannels.add(client);
                    }

                    sendMessage(client, createHelloMessage("Player" + playerId, "key-player" + playerId));
                    receiveMessage(client); // WELCOME

                    OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT);
                    chat.setChat("Hello from Player" + playerId);
                    chat.setPlayerName("Player" + playerId);
                    sendMessage(client, chat);

                    client.close();
                    successfulConnects.incrementAndGet();
                } catch (Exception e) {
                    // Log but don't fail - some overlap is expected
                } finally {
                    latch.countDown();
                }
            });
            threads.add(t);
        }

        // Start all threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all to complete (with timeout)
        assertThat(latch.await(8, TimeUnit.SECONDS)).isTrue();

        // Verify most/all connections succeeded (at least 8 out of 10)
        assertThat(successfulConnects.get()).isGreaterThanOrEqualTo(8);
    }

    // =================================================================
    // Test 8: Message Size Validation
    // =================================================================

    @Test
    void testMessageSizeValidation() throws Exception {
        // Setup valid profile
        setupValidProfile("Player1", "key-player1");

        // Start server
        setupServer();

        // Connect
        SocketChannel client = connectClient();
        sendMessage(client, createHelloMessage("Player1", "key-player1"));
        receiveMessage(client); // WELCOME

        // Send chat message exceeding 500KB limit (Peer2PeerMessage limit)
        StringBuilder largeChat = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            largeChat.append("0123456789"); // 1MB total
        }

        OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT);
        chat.setChat(largeChat.toString());
        chat.setPlayerName("Player1");

        // Attempt to send should either fail on write or server should reject
        // Server should close connection after receiving oversized message
        try {
            sendMessage(client, chat);
            // If send succeeded, server should disconnect us
            Peer2PeerMessage response = receiveMessageWithTimeout(client, 2000);
            // Either no response (disconnect) or error message
            if (response != null) {
                OnlineMessage error = new OnlineMessage(response.getMessage());
                assertThat(error.getChatType()).isEqualTo(PokerConstants.CHAT_ADMIN_ERROR);
            }
        } catch (Exception e) {
            // Expected - message too large
        }

        // Verify client is disconnected
        assertThat(client.isConnected()).isFalse();
    }

    // =================================================================
    // Test 9: Duplicate Key Rejection
    // =================================================================

    @Test
    void testDuplicateKeyRejection() throws Exception {
        // Setup valid profile
        String sharedKey = "shared-key-123";
        setupValidProfile("Player1", sharedKey);
        setupValidProfile("Player2", sharedKey); // Same key, different profile

        // Start server
        setupServer();

        // Client1 connects with key
        SocketChannel client1 = connectClient();
        sendMessage(client1, createHelloMessage("Player1", sharedKey));
        receiveMessage(client1); // WELCOME

        // Client2 tries to connect with same key
        SocketChannel client2 = connectClient();
        sendMessage(client2, createHelloMessage("Player2", sharedKey));

        // Client2 should receive error and be disconnected
        Peer2PeerMessage response = receiveMessage(client2);
        assertThat(response).isNotNull();

        OnlineMessage error = new OnlineMessage(response.getMessage());
        assertThat(error.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(error.getChatType()).isEqualTo(PokerConstants.CHAT_ADMIN_ERROR);
        assertThat(error.getChat()).containsIgnoringCase("duplicate");

        // Client2 should be disconnected
        Thread.sleep(100); // Allow disconnect to process
        assertThat(client2.isConnected()).isFalse();

        // Client1 should still be connected
        assertThat(client1.isConnected()).isTrue();
    }

    // =================================================================
    // Test 10: Duplicate Profile Rejection
    // =================================================================

    @Test
    void testDuplicateProfileRejection() throws Exception {
        // Setup same profile with different keys
        String profileName = "DuplicateProfile";
        setupValidProfile(profileName, "key-first");
        setupValidProfile(profileName, "key-second");

        // Start server
        setupServer();

        // Client1 connects
        SocketChannel client1 = connectClient();
        sendMessage(client1, createHelloMessage(profileName, "key-first"));
        receiveMessage(client1); // WELCOME

        // Client2 tries to connect with same profile name but different key
        SocketChannel client2 = connectClient();
        sendMessage(client2, createHelloMessage(profileName, "key-second"));

        // Client2 should receive error
        Peer2PeerMessage response = receiveMessage(client2);
        OnlineMessage error = new OnlineMessage(response.getMessage());
        assertThat(error.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(error.getChatType()).isEqualTo(PokerConstants.CHAT_ADMIN_ERROR);
        assertThat(error.getChat()).containsIgnoringCase("duplicate");

        // Client2 should be disconnected
        Thread.sleep(100);
        assertThat(client2.isConnected()).isFalse();
    }

    // =================================================================
    // Test 11: Invalid License Key
    // =================================================================

    @Test
    void testInvalidLicenseKey() throws Exception {
        // Setup mock to reject key
        when(mockBannedKeyService.isBanned(anyString())).thenReturn(false);

        // Start server
        setupServer();

        // Connect with invalid key
        SocketChannel client = connectClient();
        OnlineMessage hello = createHelloMessage("TestPlayer", "invalid-key-format");
        sendMessage(client, hello);

        // Should receive error
        Peer2PeerMessage response = receiveMessage(client);
        OnlineMessage error = new OnlineMessage(response.getMessage());
        assertThat(error.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(error.getChatType()).isEqualTo(PokerConstants.CHAT_ADMIN_ERROR);

        // Client should be disconnected
        Thread.sleep(100);
        assertThat(client.isConnected()).isFalse();
    }

    // =================================================================
    // Test 12: Banned Key Rejected
    // =================================================================

    @Test
    void testBannedKeyRejected() throws Exception {
        // Setup banned key
        String bannedKey = "banned-key-666";
        setupValidProfile("BannedPlayer", bannedKey);
        when(mockBannedKeyService.isBanned(bannedKey)).thenReturn(true);

        // Start server
        setupServer();

        // Connect with banned key
        SocketChannel client = connectClient();
        sendMessage(client, createHelloMessage("BannedPlayer", bannedKey));

        // Should receive error
        Peer2PeerMessage response = receiveMessage(client);
        OnlineMessage error = new OnlineMessage(response.getMessage());
        assertThat(error.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT_ADMIN);
        assertThat(error.getChatType()).isEqualTo(PokerConstants.CHAT_ADMIN_ERROR);

        // Client should be disconnected
        Thread.sleep(100);
        assertThat(client.isConnected()).isFalse();
    }

    // =================================================================
    // Test 13: Rate Limit
    // =================================================================

    @Test
    void testRateLimit() throws Exception {
        // Setup valid profile
        setupValidProfile("Spammer", "key-spammer");

        // Start server
        setupServer();

        // Connect
        SocketChannel client = connectClient();
        sendMessage(client, createHelloMessage("Spammer", "key-spammer"));
        receiveMessage(client); // WELCOME

        // Send messages rapidly (31 messages in quick succession)
        // Rate limit is 30 messages per 60 seconds
        boolean rateLimitTriggered = false;
        for (int i = 0; i < 31; i++) {
            try {
                OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT);
                chat.setChat("Spam message " + i);
                chat.setPlayerName("Spammer");
                sendMessage(client, chat);

                // Check if we got disconnected
                if (!client.isConnected()) {
                    rateLimitTriggered = true;
                    break;
                }
            } catch (IOException e) {
                // Connection closed by server due to rate limit
                rateLimitTriggered = true;
                break;
            }
        }

        // Should have triggered rate limit
        assertThat(rateLimitTriggered).isTrue();
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    /**
     * Setup and start the chat server
     */
    private void setupServer() throws Exception {
        chatServer = new TcpChatServer();
        chatServer.setOnlineProfileService(mockProfileService);
        chatServer.setBannedKeyService(mockBannedKeyService);
        chatServer.setAppName("TestChatServer");
        chatServer.setConfigLoadRequired(false);
        chatServer.setPortKey("settings.tcp.chat.port");
        chatServer.setBindLoopback(true);
        chatServer.setExceptionOnNoPortsBound(false);

        // Mock port property
        System.setProperty("settings.tcp.chat.port", "0"); // Use any available port

        chatServer.init();
        chatServer.start();

        // Wait for server to start
        Thread.sleep(100);

        // Get actual port
        chatServerPort = chatServer.getPreferredPort();
    }

    /**
     * Connect a client to the chat server
     */
    private SocketChannel connectClient() throws IOException {
        SocketChannel client = SocketChannel.open();
        client.connect(new InetSocketAddress("127.0.0.1", chatServerPort));
        client.configureBlocking(true);
        clientChannels.add(client);
        return client;
    }

    /**
     * Setup a valid profile in the mock service
     */
    private void setupValidProfile(String name, String key) {
        OnlineProfile profile = new OnlineProfile();
        profile.setName(name);
        profile.setLicenseKey(key);
        profile.setPassword("hashed-password-" + name);
        profile.setActivated(true);
        profile.setCreateDate(new Date());
        profile.setEmail(name.toLowerCase() + "@example.com");

        when(mockProfileService.getOnlineProfileByName(name)).thenReturn(profile);
        when(mockProfileService.getAllOnlineProfilesForEmail(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(mockBannedKeyService.isBanned(key)).thenReturn(false);
    }

    /**
     * Create a HELLO message with auth credentials
     */
    private OnlineMessage createHelloMessage(String profileName, String licenseKey) {
        OnlineMessage hello = new OnlineMessage(OnlineMessage.CAT_CHAT_HELLO);

        // Create auth data
        DMTypedHashMap authData = new DMTypedHashMap();
        authData.setString(OnlineProfile.PROFILE_NAME, profileName);
        authData.setString(OnlineProfile.PROFILE_LICENSE_KEY, licenseKey);
        authData.setString(OnlineProfile.PROFILE_PASSWORD, "hashed-password-" + profileName);
        hello.setWanAuth(authData);

        // Create player info
        OnlinePlayerInfo playerInfo = new OnlinePlayerInfo();
        playerInfo.setName(profileName);
        playerInfo.setPlayerId("public-" + licenseKey);
        hello.setPlayerInfo(playerInfo);

        // Set version
        hello.getData().setVersion(PokerConstants.VERSION);

        return hello;
    }

    /**
     * Send an OnlineMessage to the server
     */
    private void sendMessage(SocketChannel channel, OnlineMessage msg) throws IOException {
        Peer2PeerMessage p2p = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg.getData());
        p2p.write(channel);
    }

    /**
     * Receive a message from the server
     */
    private Peer2PeerMessage receiveMessage(SocketChannel channel) throws IOException {
        Peer2PeerMessage msg = new Peer2PeerMessage();
        msg.read(channel);
        return msg;
    }

    /**
     * Receive a message with timeout (returns null if timeout)
     */
    private Peer2PeerMessage receiveMessageWithTimeout(SocketChannel channel, int timeoutMs) {
        try {
            channel.configureBlocking(false);
            channel.socket().setSoTimeout(timeoutMs);

            Peer2PeerMessage msg = new Peer2PeerMessage();
            msg.read(channel);
            return msg;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                channel.configureBlocking(true);
            } catch (IOException ignored) {
            }
        }
    }
}
