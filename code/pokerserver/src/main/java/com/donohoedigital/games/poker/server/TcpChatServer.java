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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;
import com.donohoedigital.base.InputValidator;
import com.donohoedigital.base.RateLimiter;
import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.DMArrayList;
import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.network.OnlineMessage;
import com.donohoedigital.games.poker.network.OnlinePlayerInfo;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.server.service.BannedKeyService;
import com.donohoedigital.p2p.Peer2PeerMessage;
import com.donohoedigital.server.BaseServlet;
import com.donohoedigital.server.GameServer;
import com.donohoedigital.server.SocketThread;
import com.donohoedigital.server.ThreadPool;
import jakarta.servlet.ServletException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TCP-based chat server for online lobby. Replaces UDP-based ChatServer. Uses
 * GameServer framework with Peer2PeerMessage protocol.
 */
public class TcpChatServer extends GameServer {
    private static Logger logger = LogManager.getLogger(TcpChatServer.class);

    private OnlineProfileService onlineProfileService;
    private BannedKeyService bannedKeyService;

    // Chat connections list (synchronized access required)
    private final List<ChatConnection> connections = new ArrayList<>();

    // Rate limiter for chat messages (30 per 60 seconds)
    private final RateLimiter chatRateLimiter = new RateLimiter();

    /**
     * Constructor
     */
    public TcpChatServer() {
        setAppName("TcpChatServer");
        setPortKey("settings.tcp.chat.port");
        setServlet(new ChatServlet());
        setLogStatus(true);
        // Note: Thread class must be set via property settings.server.thread.class
        // or passed to init via parameter
    }

    /**
     * Override init to set thread class if not configured.
     *
     * CLEANUP-BACKEND-3: Sets global system property as a side effect. Only sets if
     * not already configured to minimize interference with other server instances.
     * TODO: Refactor to pass thread class through constructor instead of system
     * properties.
     */
    @Override
    public void init() {
        // Set thread class to use our custom ChatSocketThread (only if not already set)
        String existingThreadClass = System.getProperty("settings.server.thread.class");
        if (existingThreadClass == null || existingThreadClass.isEmpty()) {
            System.setProperty("settings.server.thread.class", ChatSocketThread.class.getName());
        }
        super.init();
    }

    /**
     * Get online profile service
     */
    public OnlineProfileService getOnlineProfileService() {
        return onlineProfileService;
    }

    /**
     * Set online profile service
     */
    @Autowired
    public void setOnlineProfileService(OnlineProfileService onlineProfileService) {
        this.onlineProfileService = onlineProfileService;
    }

    /**
     * Get banned key service
     */
    public BannedKeyService getBannedKeyService() {
        return bannedKeyService;
    }

    /**
     * Set banned key service
     */
    @Autowired
    public void setBannedKeyService(BannedKeyService bannedKeyService) {
        this.bannedKeyService = bannedKeyService;
    }

    /**
     * Override to handle disconnects
     */
    @Override
    protected void socketClosing(SocketChannel channel) {
        removeConnection(channel);
        super.socketClosing(channel);
    }

    /**
     * Remove connection from list and notify others
     */
    private void removeConnection(SocketChannel channel) {
        ChatConnection removed = null;

        synchronized (connections) {
            Iterator<ChatConnection> iter = connections.iterator();
            while (iter.hasNext()) {
                ChatConnection conn = iter.next();
                if (conn.channel == channel) {
                    removed = conn;
                    iter.remove();
                    break;
                }
            }
        }

        if (removed != null) {
            logger.info(removed.playerInfo.getName() + " GOODBYE");
            sendLeaveNotification(removed);
        }
    }

    /**
     * Send LEAVE notification to all connected clients
     */
    private void sendLeaveNotification(ChatConnection leaving) {
        OnlineMessage leaveMsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        leaveMsg.setChat(PropertyConfig.getMessage("msg.chat.goodbye", Utils.encodeHTML(leaving.playerInfo.getName())));
        leaveMsg.setChatType(PokerConstants.CHAT_ADMIN_LEAVE);
        leaveMsg.setPlayerInfo(leaving.playerInfo);

        broadcastMessage(leaveMsg, null);
    }

    /**
     * Send JOIN notification to all connected clients
     */
    private void sendJoinNotification(ChatConnection joining, SocketChannel skip) {
        OnlineMessage joinMsg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        joinMsg.setChat(PropertyConfig.getMessage("msg.chat.hello", Utils.encodeHTML(joining.playerInfo.getName())));
        joinMsg.setChatType(PokerConstants.CHAT_ADMIN_JOIN);
        joinMsg.setPlayerInfo(joining.playerInfo);

        broadcastMessage(joinMsg, skip);
    }

    /**
     * Broadcast message to all connected clients except one
     */
    private void broadcastMessage(OnlineMessage msg, SocketChannel skip) {
        Peer2PeerMessage p2p = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg.getData());

        synchronized (connections) {
            for (ChatConnection conn : connections) {
                if (conn.channel == skip)
                    continue;

                try {
                    // DOS-1: Check if queue is full before offering
                    if (!conn.writeQueue.offer(p2p)) {
                        // Queue is full - slow consumer, disconnect to prevent memory exhaustion
                        logger.warn(
                                "Write queue full for " + conn.playerInfo.getName() + " - disconnecting slow client");
                        try {
                            conn.channel.close();
                        } catch (IOException e) {
                            logger.warn("Error closing slow client channel: " + Utils.formatExceptionText(e));
                        }
                        continue;
                    }

                    // Try to send immediately if possible
                    sendQueuedMessages(conn);
                } catch (Exception e) {
                    logger.warn("Error queuing message to " + conn.playerInfo.getName() + ": "
                            + Utils.formatExceptionText(e));
                }
            }
        }
    }

    /**
     * Send all queued messages for a connection
     */
    private void sendQueuedMessages(ChatConnection conn) {
        try {
            Peer2PeerMessage msg;
            while ((msg = conn.writeQueue.poll()) != null) {
                msg.write(conn.channel);
            }
        } catch (IOException e) {
            logger.warn("Error sending queued messages to " + conn.playerInfo.getName() + ": "
                    + Utils.formatExceptionText(e));
            // Connection likely dead, will be cleaned up by socketClosing
        }
    }

    /**
     * Get player list for WELCOME message
     */
    private DMArrayList<DMTypedHashMap> getPlayerList() {
        synchronized (connections) {
            DMArrayList<DMTypedHashMap> list = new DMArrayList<>(connections.size());
            for (ChatConnection conn : connections) {
                list.add(conn.playerInfo.getData());
            }
            return list;
        }
    }

    /**
     * Log current player list
     */
    private void logPlayers() {
        synchronized (connections) {
            logger.debug(connections.size() + " players in lobby:");
            for (ChatConnection conn : connections) {
                logger.debug("  ==> " + conn.playerInfo.getName());
            }
        }
    }

    /**
     * Get status HTML for ./stats command
     */
    private String getStatusHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>TCP Chat Server Status</h2>");
        sb.append("<p>Connected players: ").append(connections.size()).append("</p>");
        sb.append("<ul>");
        synchronized (connections) {
            for (ChatConnection conn : connections) {
                sb.append("<li>").append(Utils.encodeHTML(conn.playerInfo.getName())).append(" (")
                        .append(conn.channel.socket().getInetAddress().getHostAddress()).append(":")
                        .append(conn.channel.socket().getPort()).append(")</li>");
            }
        }
        sb.append("</ul>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Inner servlet class that doesn't actually handle HTTP
     */
    private class ChatServlet extends BaseServlet {
        @Override
        public com.donohoedigital.comms.DDMessage processMessage(jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response, com.donohoedigital.comms.DDMessage ddreceived)
                throws IOException {
            throw new ApplicationError(ErrorCodes.ERROR_INVALID, "processMessage should not be called", null);
        }
    }

    /**
     * Custom socket thread for chat server
     */
    public static class ChatSocketThread extends SocketThread {
        private Peer2PeerMessage msg_;
        private boolean bKeepAlive_ = true;
        private TcpChatServer chatServer_;

        /**
         * Constructor
         */
        public ChatSocketThread() {
        }

        /**
         * Init - get chat server reference
         */
        @Override
        public void init(ThreadPool pool, BaseServlet servlet) {
            super.init(pool, servlet);
            chatServer_ = (TcpChatServer) pool.getServer();
        }

        /**
         * Keep socket alive after reading
         */
        @Override
        protected boolean isKeepAlive() {
            return bKeepAlive_;
        }

        /**
         * Handle exceptions
         */
        @Override
        protected boolean handleException(Throwable t) {
            boolean bShutDown = false;

            try {
                String sRemoteAddr = Utils.getIPAddress(channel_);
                if (t instanceof ApplicationError) {
                    ApplicationError ae = (ApplicationError) t;
                    if (ae.getErrorCode() == ErrorCodes.ERROR_INVALID_MESSAGE) {
                        bShutDown = true;
                    }

                    String sMsg = Utils.formatExceptionText(ae);
                    logger.error(Utils.getExceptionMessage(ae) + ": [" + sRemoteAddr + "]; stacktrace: " + sMsg);
                } else {
                    bShutDown = true;

                    boolean bLogError = true;
                    if (t instanceof EOFException) {
                        bLogError = false;
                    } else if (t instanceof IOException) {
                        String sMessage = Utils.getExceptionMessage(t);
                        if (sMessage.indexOf("forcibly closed") != -1 || sMessage.indexOf("reset by peer") != -1) {
                            bLogError = false;
                        }
                    }

                    if (bLogError) {
                        String sMsg = Utils.formatExceptionText(t);
                        logger.error(t.getClass().getName() + " [" + sRemoteAddr + "]; stacktrace: " + sMsg);
                    }
                }
            } catch (Throwable tt) {
                logger.error("Exception in handleException: " + Utils.formatExceptionText(tt)
                        + "; occurred while handling exception: " + Utils.formatExceptionText(t));
            }

            return bShutDown;
        }

        /**
         * Init to begin new request
         */
        @Override
        protected void initRequest() {
            msg_ = null;
        }

        /**
         * Read data from socket
         */
        @Override
        protected void readData(SocketChannel channel) throws IOException {
            bKeepAlive_ = true;
            msg_ = new Peer2PeerMessage();
            msg_.setFromIP(Utils.getIPAddress(channel));
            msg_.read(channel);
        }

        /**
         * Process the message
         */
        @Override
        protected void process() throws IOException, ServletException {
            OnlineMessage onlineMsg = new OnlineMessage(msg_.getMessage());

            if (onlineMsg.getCategory() == OnlineMessage.CAT_CHAT_HELLO) {
                handleHello(channel_, onlineMsg);
            } else if (onlineMsg.getCategory() == OnlineMessage.CAT_CHAT) {
                handleChat(channel_, onlineMsg);
            }
        }

        /**
         * Handle HELLO message - authenticate and add user
         */
        private void handleHello(SocketChannel channel, OnlineMessage msg) {
            DMTypedHashMap authData = msg.getWanAuth();
            if (authData == null) {
                sendError(channel, PropertyConfig.getMessage("msg.wanprofile.unavailable"));
                return;
            }

            OnlineProfile auth = new OnlineProfile(authData);
            String profileName = auth.getName();

            // Authenticate using service (uses bcrypt password hashing)
            OnlineProfile authRequest = new OnlineProfile();
            authRequest.setName(auth.getName());
            authRequest.setPassword(auth.getPassword());

            OnlineProfile user = chatServer_.onlineProfileService.authenticateOnlineProfile(authRequest);

            // Verify profile exists and credentials match
            if (user == null) {
                sendError(channel, PropertyConfig.getMessage("msg.wanprofile.unavailable"));
                return;
            }

            // Check for ban via profile
            String banCheck = PokerServlet.banCheck(chatServer_.bannedKeyService, user);
            if (banCheck != null) {
                sendError(channel, banCheck);
                return;
            }

            // Create player info
            OnlinePlayerInfo playerInfo = new OnlinePlayerInfo();
            playerInfo.setName(user.getName());
            playerInfo.setPublicUseKey(msg.getData().getKey());
            playerInfo.setCreateDate(user.getCreateDate());

            // Get aliases
            List<OnlineProfile> aliases = chatServer_.onlineProfileService.getAllOnlineProfilesForEmail(user.getEmail(),
                    user.getName());
            if (!aliases.isEmpty()) {
                DMArrayList<DMTypedHashMap> aliasData = new DMArrayList<>(aliases.size());
                for (OnlineProfile alias : aliases) {
                    OnlinePlayerInfo aliasInfo = new OnlinePlayerInfo();
                    aliasInfo.setName(alias.getName());
                    aliasInfo.setCreateDate(alias.getCreateDate());
                    aliasData.add(aliasInfo.getData());
                }
                playerInfo.setAliases(aliasData);
            }

            // Check for duplicates
            String remoteIP = Utils.getIPAddress(channel);
            synchronized (chatServer_.connections) {
                for (ChatConnection existing : chatServer_.connections) {
                    // Same profile name from different connection
                    if (existing.playerInfo.getNameLower().equals(playerInfo.getNameLower())
                            && existing.channel != channel) {
                        logger.info("Duplicate profile rejected for " + profileName);
                        sendError(channel, PropertyConfig.getMessage("msg.chat.dupprofile",
                                Utils.encodeHTML(profileName), remoteIP));
                        return;
                    }

                    // Same channel reconnecting - remove old entry
                    if (existing.channel == channel) {
                        chatServer_.connections.remove(existing);
                        break;
                    }
                }

                // Add new connection
                ChatConnection newConn = new ChatConnection(channel, playerInfo);
                chatServer_.connections.add(newConn);

                logger.info(playerInfo.getName() + " HELLO (" + channel.socket().getInetAddress().getHostAddress() + ":"
                        + channel.socket().getPort() + ")");

                // Send WELCOME with player list
                sendWelcome(channel,
                        PropertyConfig.getMessage("msg.chat.welcome", Utils.encodeHTML(playerInfo.getName())));

                // Notify others of JOIN
                chatServer_.sendJoinNotification(newConn, channel);

                // Log players
                chatServer_.logPlayers();
            }
        }

        /**
         * Handle CHAT message - broadcast to others
         */
        private void handleChat(SocketChannel channel, OnlineMessage msg) {
            String playerName = msg.getPlayerName();
            String chatText = msg.getChat();

            logger.debug(playerName + " said \"" + chatText + "\"");

            // SEC-BACKEND-6: Validate chat message length (1-500 chars)
            if (!InputValidator.isValidChatMessage(chatText)) {
                sendError(channel, "Chat message must be between 1 and 500 characters.");
                return;
            }

            // Rate limit check (30 messages per 60 seconds)
            if (!chatServer_.chatRateLimiter.allowRequest(playerName, 30, 60000)) {
                sendError(channel, "Too many chat messages. Please slow down.");
                return;
            }

            // SEC-BACKEND-6: Sanitize chat text to prevent XSS
            msg.setChat(Utils.encodeHTML(chatText));

            // Broadcast to others (not sender)
            chatServer_.broadcastMessage(msg, channel);
        }

        /**
         * Send WELCOME message with player list
         */
        private void sendWelcome(SocketChannel channel, String message) {
            OnlineMessage welcome = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
            welcome.setChat(message);
            welcome.setChatType(PokerConstants.CHAT_ADMIN_WELCOME);
            welcome.setPlayerList(chatServer_.getPlayerList());

            try {
                Peer2PeerMessage p2p = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, welcome.getData());
                p2p.write(channel);
            } catch (IOException e) {
                logger.warn("Error sending welcome: " + Utils.formatExceptionText(e));
            }
        }

        /**
         * Send regular message to a channel
         */
        private void sendMessage(SocketChannel channel, String message) {
            OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
            msg.setChat(message);

            try {
                Peer2PeerMessage p2p = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg.getData());
                p2p.write(channel);
            } catch (IOException e) {
                logger.warn("Error sending message: " + Utils.formatExceptionText(e));
            }
        }

        /**
         * Send error message and close connection
         */
        private void sendError(SocketChannel channel, String message) {
            OnlineMessage error = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
            error.setChat(message);
            error.setChatType(PokerConstants.CHAT_ADMIN_ERROR);

            try {
                Peer2PeerMessage p2p = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, error.getData());
                p2p.write(channel);
            } catch (IOException e) {
                logger.warn("Error sending error message: " + Utils.formatExceptionText(e));
            }

            // Close connection
            bKeepAlive_ = false;
        }
    }

    /**
     * Represents a chat connection with player info and write queue
     */
    private static class ChatConnection {
        final SocketChannel channel;
        final OnlinePlayerInfo playerInfo;
        final LinkedBlockingQueue<Peer2PeerMessage> writeQueue;

        ChatConnection(SocketChannel channel, OnlinePlayerInfo playerInfo) {
            this.channel = channel;
            this.playerInfo = playerInfo;
            // DOS-1: Limit queue capacity to prevent memory exhaustion
            this.writeQueue = new LinkedBlockingQueue<>(100);
        }
    }
}
