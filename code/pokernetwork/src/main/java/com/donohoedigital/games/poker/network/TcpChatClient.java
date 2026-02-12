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
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.p2p.Peer2PeerMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TCP-based chat client for lobby chat. Replaces PokerUDPServer.chatLink_
 * functionality. Uses Peer2PeerMessage TCP wire protocol for all I/O.
 *
 * Note: This class uses generic Object types for profile and handler to avoid
 * circular dependencies between pokernetwork and poker modules. The actual
 * types (PlayerProfile, ChatHandler) are used via reflection or duck typing.
 */
public class TcpChatClient {
    private static final Logger logger = LogManager.getLogger(TcpChatClient.class);
    private static final int MAX_MESSAGE_SIZE = 500000; // 500KB

    private final InetSocketAddress serverAddress;
    private final ReentrantLock lock = new ReentrantLock();

    private SocketChannel channel;
    private Object handler; // ChatHandler from poker module
    private Thread readerThread;
    private volatile boolean running = false;
    private volatile boolean reconnectEnabled = true;
    private long reconnectDelayMs = 1000;

    /**
     * Create a new TcpChatClient
     *
     * @param serverAddress
     *            Server address to connect to
     */
    public TcpChatClient(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * Set the chat handler for receiving messages
     *
     * @param handler
     *            Handler to receive messages (ChatHandler from poker module)
     */
    public void setHandler(Object handler) {
        this.handler = handler;
    }

    /**
     * Enable or disable automatic reconnection
     *
     * @param enabled
     *            Whether to enable reconnection
     */
    public void setReconnectEnabled(boolean enabled) {
        this.reconnectEnabled = enabled;
    }

    /**
     * Set the delay between reconnection attempts
     *
     * @param delayMs
     *            Delay in milliseconds
     */
    public void setReconnectDelayMs(long delayMs) {
        this.reconnectDelayMs = delayMs;
    }

    /**
     * Connect to the chat server
     *
     * @throws IOException
     *             If connection fails
     */
    public void connect() throws IOException {
        lock.lock();
        try {
            if (isConnected()) {
                return;
            }

            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(serverAddress);

            // Wait for connection to complete
            long deadline = System.currentTimeMillis() + 5000;
            while (!channel.finishConnect()) {
                if (System.currentTimeMillis() > deadline) {
                    channel.close();
                    throw new IOException("Connection timeout");
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    channel.close();
                    throw new IOException("Connection interrupted", e);
                }
            }

            // Set to blocking mode for Peer2PeerMessage reading/writing
            channel.configureBlocking(true);

            // Start reader thread
            startReaderThread();

            logger.info("Connected to chat server at {}", serverAddress);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Disconnect from the chat server
     */
    public void disconnect() {
        lock.lock();
        try {
            running = false;
            reconnectEnabled = false;

            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException e) {
                    logger.warn("Error closing channel", e);
                }
                channel = null;
            }

            // Wait for reader thread to stop
            if (readerThread != null && readerThread.isAlive()) {
                try {
                    readerThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                readerThread = null;
            }

            logger.info("Disconnected from chat server");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Send HELLO message with player profile authentication
     *
     * @param profile
     *            Player profile (PlayerProfile from poker module)
     * @param playerId
     *            Player ID
     * @throws IOException
     *             If not connected or send fails
     */
    public void sendHello(Object profile, String playerId) throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to chat server");
        }

        // Use reflection to get profile data
        String playerName;
        String password;
        try {
            playerName = (String) profile.getClass().getMethod("getName").invoke(profile);
            password = (String) profile.getClass().getMethod("getPassword").invoke(profile);
        } catch (Exception e) {
            throw new IOException("Failed to extract profile data", e);
        }

        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT_HELLO);
        msg.setPlayerName(playerName);

        // Set auth data
        DMTypedHashMap auth = new DMTypedHashMap();
        auth.setString("name", playerName);
        auth.setString("uuid", playerId);
        if (password != null) {
            auth.setString("password", password);
        }
        msg.setWanAuth(auth);

        sendMessage(msg);
        logger.debug("Sent HELLO message for player {}", playerName);
    }

    /**
     * Send a chat message
     *
     * @param profile
     *            Player profile (PlayerProfile from poker module)
     * @param message
     *            Message text
     */
    public void sendChat(Object profile, String message) {
        // Allow null messages (no-op per PokerUDPServer behavior)
        if (message == null) {
            return;
        }

        // Validate message
        if (message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        if (message.length() > MAX_MESSAGE_SIZE) {
            throw new IllegalArgumentException("Message too large: " + message.length() + " bytes");
        }

        if (!isConnected()) {
            logger.warn("Cannot send chat - not connected");
            return;
        }

        // Use reflection to get profile name
        String playerName;
        try {
            playerName = (String) profile.getClass().getMethod("getName").invoke(profile);
        } catch (Exception e) {
            logger.error("Failed to extract player name from profile", e);
            return;
        }

        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setPlayerName(playerName);
        msg.setChat(message);

        try {
            sendMessage(msg);
            logger.debug("Sent chat message from {}: {}", playerName, message);
        } catch (IOException e) {
            logger.error("Error sending chat message", e);
        }
    }

    /**
     * Check if connected to server
     *
     * @return True if connected
     */
    public boolean isConnected() {
        return channel != null && channel.isOpen() && channel.isConnected();
    }

    /**
     * Check if reader thread is running
     *
     * @return True if reader thread is running
     */
    public boolean isReaderThreadRunning() {
        return readerThread != null && readerThread.isAlive() && running;
    }

    /**
     * Send a message to the server
     *
     * @param msg
     *            Message to send
     * @throws IOException
     *             If send fails
     */
    private void sendMessage(OnlineMessage msg) throws IOException {
        lock.lock();
        try {
            if (!isConnected()) {
                throw new IOException("Not connected");
            }

            Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg.getData());
            p2pMsg.write(channel);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Start the reader thread
     */
    private void startReaderThread() {
        running = true;
        readerThread = new Thread(this::readerLoop, "TcpChatClient-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Reader thread loop
     */
    private void readerLoop() {
        logger.debug("Reader thread started");

        while (running) {
            try {
                if (!isConnected()) {
                    break;
                }

                // Read message
                Peer2PeerMessage p2pMsg = new Peer2PeerMessage();
                p2pMsg.read(channel);

                // Process message
                OnlineMessage msg = new OnlineMessage(p2pMsg.getMessage());
                processMessage(msg);

            } catch (EOFException e) {
                logger.info("Connection closed by server");
                handleConnectionError("Connection closed by server");
                break;
            } catch (IOException e) {
                logger.warn("Error reading message", e);
                handleConnectionError("Error reading message: " + e.getMessage());
                break;
            } catch (Exception e) {
                logger.error("Unexpected error in reader thread", e);
                handleConnectionError("Unexpected error: " + e.getMessage());
                break;
            }
        }

        logger.debug("Reader thread stopped");
    }

    /**
     * Process incoming message
     *
     * @param msg
     *            Message to process
     */
    private void processMessage(OnlineMessage msg) {
        if (handler == null) {
            logger.warn("No handler set, discarding message");
            return;
        }

        logger.debug("Received message: {}", msg.toStringCategory());

        try {
            // Handle error messages specially - disconnect after notifying handler
            if (msg.getCategory() == OnlineMessage.CAT_CHAT_ADMIN
                    && msg.getChatType() == PokerConstants.CHAT_ADMIN_ERROR) {
                // Call handler.chatReceived(msg) via reflection
                handler.getClass().getMethod("chatReceived", OnlineMessage.class).invoke(handler, msg);
                disconnect();
                return;
            }

            // Notify handler via reflection
            handler.getClass().getMethod("chatReceived", OnlineMessage.class).invoke(handler, msg);
        } catch (Exception e) {
            logger.error("Error processing message", e);
        }
    }

    /**
     * Handle connection error
     *
     * @param message
     *            Error message
     */
    private void handleConnectionError(String message) {
        // Notify handler
        if (handler != null) {
            try {
                // Call notifyError if it exists (from MockChatHandler)
                handler.getClass().getMethod("notifyError", String.class).invoke(handler, message);
            } catch (Exception e) {
                // If notifyError doesn't exist, create an admin error message
                try {
                    OnlineMessage errorMsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
                    errorMsg.setChatType(PokerConstants.CHAT_ADMIN_ERROR);
                    errorMsg.setChat(message);
                    handler.getClass().getMethod("chatReceived", OnlineMessage.class).invoke(handler, errorMsg);
                } catch (Exception ex) {
                    logger.warn("Failed to notify handler of error", ex);
                }
            }
        }

        // Attempt reconnect if enabled
        if (reconnectEnabled && running) {
            reconnect();
        }
    }

    /**
     * Attempt to reconnect to server
     */
    private void reconnect() {
        logger.info("Attempting to reconnect to chat server in {}ms", reconnectDelayMs);

        // Close current connection
        lock.lock();
        try {
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException e) {
                    logger.warn("Error closing channel during reconnect", e);
                }
                channel = null;
            }
        } finally {
            lock.unlock();
        }

        // Wait before reconnecting
        try {
            Thread.sleep(reconnectDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // Try to reconnect
        try {
            connect();
            logger.info("Reconnected to chat server");
        } catch (IOException e) {
            logger.warn("Reconnect failed: {}", e.getMessage());
            // Will try again on next error if still enabled
        }
    }
}
