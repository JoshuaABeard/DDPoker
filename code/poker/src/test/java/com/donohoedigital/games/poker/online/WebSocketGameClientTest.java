/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class WebSocketGameClientTest {

    @Test
    void connect_initialFailure_startsReconnectCycle() {
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder builder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(builder);

        CompletableFuture<WebSocket> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("connect failed"));
        when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class))).thenReturn(failed);

        WebSocketGameClient client = new WebSocketGameClient(new ObjectMapper().registerModule(new JavaTimeModule()),
                httpClient);

        try {
            CompletableFuture<Void> connectFuture = client.connect("localhost", 11885, "game-1", "jwt-1");

            assertThatThrownBy(connectFuture::join).isInstanceOf(CompletionException.class)
                    .hasRootCauseMessage("connect failed");

            verify(builder, timeout(3000).atLeast(2)).buildAsync(any(URI.class), any(WebSocket.Listener.class));
            assertThat(client.isConnected()).isFalse();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void schedulerIsAliveAfterDisconnectAndReconnect() {
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder builder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(builder);

        WebSocket connectedWs = mock(WebSocket.class);
        // First connect succeeds, then all attempts fail
        CompletableFuture<WebSocket> success = CompletableFuture.completedFuture(connectedWs);
        CompletableFuture<WebSocket> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("connect failed"));
        when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class))).thenReturn(success, failed);

        WebSocketGameClient client = new WebSocketGameClient(new ObjectMapper().registerModule(new JavaTimeModule()),
                httpClient);
        try {
            // First game: connect then disconnect
            client.connect("localhost", 11885, "game-1", "jwt-1").join();
            assertThat(client.isConnected()).isTrue();
            client.disconnect();
            assertThat(client.isConnected()).isFalse();

            // Second game: connect fails, should trigger reconnect attempts
            client.connect("localhost", 11885, "game-2", "jwt-2");

            // Should see at least 3 buildAsync calls: success + failure + 1 reconnect
            // attempt
            verify(builder, timeout(3000).atLeast(3)).buildAsync(any(URI.class), any(WebSocket.Listener.class));
        } finally {
            client.disconnect();
        }
    }

    @Test
    void connectSucceedsAfterPreviousDisconnect() {
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder builder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(builder);

        WebSocket connectedWs = mock(WebSocket.class);
        when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class)))
                .thenReturn(CompletableFuture.completedFuture(connectedWs));

        WebSocketGameClient client = new WebSocketGameClient(new ObjectMapper().registerModule(new JavaTimeModule()),
                httpClient);
        try {
            client.connect("localhost", 11885, "game-1", "jwt-1").join();
            assertThat(client.isConnected()).isTrue();
            client.disconnect();
            assertThat(client.isConnected()).isFalse();

            // Second game: connect should succeed
            client.connect("localhost", 11885, "game-2", "jwt-2").join();
            assertThat(client.isConnected()).isTrue();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void connectResetsSequenceCountersToZero() {
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder builder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(builder);

        WebSocket connectedWs = mock(WebSocket.class);
        when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class)))
                .thenReturn(CompletableFuture.completedFuture(connectedWs));

        WebSocketGameClient client = new WebSocketGameClient(new ObjectMapper().registerModule(new JavaTimeModule()),
                httpClient);
        try {
            // First connect
            client.connect("localhost", 11885, "game-1", "jwt-1").join();
            assertThat(client.isConnected()).isTrue();

            // Simulate having received messages up to sequence 50
            client.setLastReceivedSequenceForTest(50);
            assertThat(client.getLastReceivedSequenceForTest()).isEqualTo(50);

            // Disconnect and reconnect to a new game
            client.disconnect();
            client.connect("localhost", 11885, "game-2", "jwt-2").join();

            // After connect(), lastReceivedSequence must be 0 so gap detection
            // doesn't trigger on the first broadcast message
            assertThat(client.getLastReceivedSequenceForTest()).isEqualTo(0);
        } finally {
            client.disconnect();
        }
    }

    @Test
    void connect_newSessionInvalidatesPendingReconnectAttempts() throws InterruptedException {
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder builder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(builder);

        CompletableFuture<WebSocket> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("connect failed"));

        WebSocket connectedWs = mock(WebSocket.class);
        CompletableFuture<WebSocket> success = CompletableFuture.completedFuture(connectedWs);

        when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class))).thenReturn(failed, success);

        WebSocketGameClient client = new WebSocketGameClient(new ObjectMapper().registerModule(new JavaTimeModule()),
                httpClient);

        try {
            CompletableFuture<Void> failedConnect = client.connect("localhost", 11885, "game-1", "jwt-1");
            assertThatThrownBy(failedConnect::join).isInstanceOf(CompletionException.class)
                    .hasRootCauseMessage("connect failed");

            client.connect("localhost", 11885, "game-2", "jwt-2").join();

            verify(builder, timeout(1000).times(2)).buildAsync(any(URI.class), any(WebSocket.Listener.class));

            Thread.sleep(1200);
            verify(builder, times(2)).buildAsync(any(URI.class), any(WebSocket.Listener.class));
            assertThat(client.isConnected()).isTrue();
        } finally {
            client.disconnect();
        }
    }

    @Test
    void connect_usesProvidedHostInWsUrl() {
        // Capture the URI passed to buildAsync and assert it contains the non-localhost
        // host.
        HttpClient httpClient = mock(HttpClient.class);
        WebSocket.Builder builder = mock(WebSocket.Builder.class);
        when(httpClient.newWebSocketBuilder()).thenReturn(builder);

        WebSocket connectedWs = mock(WebSocket.class);
        when(builder.buildAsync(any(URI.class), any(WebSocket.Listener.class)))
                .thenReturn(CompletableFuture.completedFuture(connectedWs));

        WebSocketGameClient client = new WebSocketGameClient(new ObjectMapper().registerModule(new JavaTimeModule()),
                httpClient);
        try {
            client.connect("game.example.com", 9090, "abc", "tok").join();

            // Capture the URI that was passed to buildAsync
            org.mockito.ArgumentCaptor<URI> uriCaptor = org.mockito.ArgumentCaptor.forClass(URI.class);
            verify(builder).buildAsync(uriCaptor.capture(), any(WebSocket.Listener.class));

            URI uri = uriCaptor.getValue();
            assertThat(uri.getHost()).isEqualTo("game.example.com");
            assertThat(uri.getPort()).isEqualTo(9090);
            assertThat(uri.getPath()).isEqualTo("/ws/games/abc");
            assertThat(uri.getQuery()).isEqualTo("token=tok");
        } finally {
            client.disconnect();
        }
    }
}
