/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Community Contributors
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
package com.donohoedigital.server;

import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.PropertyConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadPool}.
 *
 * <p>
 * Construction approach: ThreadPool requires a GameServer (abstract, no
 * abstract methods) and a BaseServlet (abstract, one abstract method). Both are
 * stubbed with minimal inner classes. SocketThread is used directly; its static
 * initialiser reads two PropertyConfig keys that are satisfied by the testapp
 * config loaded in {@code @BeforeAll}.
 *
 * <p>
 * The ThreadPool constructor sleeps 250 ms to let worker threads start; tests
 * accept this latency.
 */
class ThreadPoolTest {

    // -----------------------------------------------------------------------
    // One-time setup
    // -----------------------------------------------------------------------

    @BeforeAll
    static void initConfig() {
        // PropertyConfig is a singleton; re-initialising is safe (only warns).
        // The testapp config under src/test/resources/config/testapp/ supplies the
        // two properties that SocketThread's static initialiser requires:
        // settings.server.readtimeout.millis and settings.server.readwait.millis
        new PropertyConfig("testapp", new String[]{"testapp"}, ApplicationType.COMMAND_LINE, null, false);
    }

    // -----------------------------------------------------------------------
    // Minimal stubs
    // -----------------------------------------------------------------------

    /** Concrete GameServer stub — no abstract methods to implement. */
    private static class StubGameServer extends GameServer {
        StubGameServer() {
            setAppName("test-app");
        }
    }

    /** Concrete BaseServlet stub — implements the single abstract method. */
    private static class StubServlet extends BaseServlet {
        @Override
        public DDMessage processMessage(HttpServletRequest request, HttpServletResponse response, DDMessage received)
                throws IOException {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------

    private ThreadPool pool;

    /**
     * Helper — creates a pool with the given size and tears it down after each test
     * via @AfterEach.
     */
    private ThreadPool createPool(int size) {
        StubGameServer server = new StubGameServer();
        StubServlet servlet = new StubServlet();
        // StubGameServer is never init()'d (no ports bound), so we call setServer
        // manually
        // to satisfy BaseServlet's requirement before passing it to ThreadPool.
        servlet.setServer(server);
        pool = new ThreadPool(server, size, servlet, SocketThread.class.getName());
        return pool;
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
            pool = null;
        }
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnCorrectSize_When_PoolCreatedWithOneWorker() {
        createPool(1);

        assertThat(pool.size()).isEqualTo(1);
    }

    @Test
    void should_ReturnCorrectSize_When_PoolCreatedWithThreeWorkers() {
        createPool(3);

        assertThat(pool.size()).isEqualTo(3);
    }

    @Test
    void should_HaveAllWorkersIdle_When_PoolFirstCreated() {
        createPool(2);

        assertThat(pool.getNumIdleWorkers()).isEqualTo(2);
    }

    @Test
    void should_ReturnWorker_When_WorkerIsAvailable() {
        createPool(1);

        SocketThread worker = pool.getWorker();

        assertThat(worker).isNotNull();
    }

    @Test
    void should_DecrementIdleCount_When_WorkerIsRetrieved() {
        createPool(2);

        pool.getWorker();

        assertThat(pool.getNumIdleWorkers()).isEqualTo(1);
    }

    @Test
    void should_ReturnNull_When_NoIdleWorkersAvailable() {
        createPool(1);
        // Drain the single worker
        pool.getWorker();

        SocketThread worker = pool.getWorker();

        assertThat(worker).isNull();
    }

    @Test
    void should_RestoreIdleCount_When_WorkerIsReturnedToPool() {
        createPool(1);
        SocketThread worker = pool.getWorker();
        assertThat(pool.getNumIdleWorkers()).isEqualTo(0);

        pool.returnWorker(worker);

        assertThat(pool.getNumIdleWorkers()).isEqualTo(1);
    }

    @Test
    void should_IncreasePoolSize_When_AddWorkersIsCalled() {
        createPool(1);

        pool.addWorkers(2);

        assertThat(pool.size()).isEqualTo(3);
    }

    @Test
    void should_ClearWorkersAndIdlePool_When_ShutdownIsCalled() {
        createPool(2);

        pool.shutdown();

        assertThat(pool.size()).isEqualTo(0);
        assertThat(pool.getNumIdleWorkers()).isEqualTo(0);
        // prevent @AfterEach from calling shutdown again on the now-cleared pool
        pool = null;
    }

    @Test
    void should_ReturnServer_When_GetServerIsCalled() {
        StubGameServer server = new StubGameServer();
        StubServlet servlet = new StubServlet();
        servlet.setServer(server);
        pool = new ThreadPool(server, 1, servlet, SocketThread.class.getName());

        assertThat(pool.getServer()).isSameAs(server);
    }
}
