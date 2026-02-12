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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Simple tests for Peer2PeerMessage - verifies basic functionality without I/O
 */
class Peer2PeerMessageSimpleTest {

    @BeforeAll
    static void setupPropertyConfig() {
        // Initialize PropertyConfig with testapp module
        String[] modules = {"testapp"};
        new PropertyConfig("testapp", modules, ApplicationType.COMMAND_LINE, null, false);
    }

    @Test
    void should_CreateMessage_When_TypeAndDataProvided() {
        DDMessage msg = new DDMessage();
        msg.setString("key", "value");

        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);

        assertThat(p2pMsg).isNotNull();
        assertThat(p2pMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_MSG);
        assertThat(p2pMsg.getMessage()).isNotNull();
        assertThat(p2pMsg.getMessage().getString("key")).isEqualTo("value");
    }

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

    @Test
    void should_AllowTypeChange_When_SetTypeCalled() {
        DDMessage msg = new DDMessage();
        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);

        p2pMsg.setType(Peer2PeerMessage.P2P_REPLY);

        assertThat(p2pMsg.getType()).isEqualTo(Peer2PeerMessage.P2P_REPLY);
    }

    @Test
    void should_HaveProtocolVersion1_When_MessageCreated() {
        DDMessage msg = new DDMessage();
        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);

        assertThat(p2pMsg.getProtocol()).isEqualTo(1);
    }

    @Test
    void should_IncludeTypeInString_When_ToStringCalled() {
        DDMessage msg = new DDMessage();
        msg.setString("key", "value");
        Peer2PeerMessage p2pMsg = new Peer2PeerMessage(Peer2PeerMessage.P2P_MSG, msg);

        String result = p2pMsg.toString();

        assertThat(result).contains("msg");
        assertThat(result).contains("key");
    }
}
