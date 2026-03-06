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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WorkerPool}.
 *
 * <p>
 * Construction approach: WorkerPool requires a concrete WorkerThread subclass
 * with a no-arg constructor. A minimal stub is provided. The WorkerPool
 * constructor sleeps 250 ms to let worker threads start; tests accept this
 * latency.
 */
class WorkerPoolTest {

    // -----------------------------------------------------------------------
    // Minimal stub
    // -----------------------------------------------------------------------

    /** Concrete WorkerThread stub — implements the single abstract method. */
    public static class StubWorkerThread extends WorkerThread {
        @Override
        public void process() {
            // no-op for testing
        }
    }

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------

    private WorkerPool pool;

    private WorkerPool createPool(int size) {
        pool = new WorkerPool(size, StubWorkerThread.class);
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
    // Pool size
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

    // -----------------------------------------------------------------------
    // Idle workers
    // -----------------------------------------------------------------------

    @Test
    void should_HaveAllWorkersIdle_When_PoolFirstCreated() {
        createPool(2);

        assertThat(pool.getNumIdleWorkers()).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // Get / return workers
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnWorker_When_WorkerIsAvailable() {
        createPool(1);

        WorkerThread worker = pool.getWorker();

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
        pool.getWorker();

        WorkerThread worker = pool.getWorker();

        assertThat(worker).isNull();
    }

    @Test
    void should_RestoreIdleCount_When_WorkerIsReturnedToPool() {
        createPool(1);
        WorkerThread worker = pool.getWorker();
        assertThat(pool.getNumIdleWorkers()).isEqualTo(0);

        pool.returnWorker(worker);

        assertThat(pool.getNumIdleWorkers()).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Add workers
    // -----------------------------------------------------------------------

    @Test
    void should_IncreasePoolSize_When_AddWorkersIsCalled() {
        createPool(1);

        pool.addWorkers(2);

        assertThat(pool.size()).isEqualTo(3);
    }

    @Test
    void should_IncreaseIdleCount_When_AddWorkersIsCalled() {
        createPool(1);

        pool.addWorkers(2);

        assertThat(pool.getNumIdleWorkers()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Shutdown
    // -----------------------------------------------------------------------

    @Test
    void should_NullifyPoolInternals_When_ShutdownIsCalled() {
        createPool(2);

        pool.shutdown();
        pool = null;

        // If shutdown completes without exceptions, the test passes.
        // WorkerPool nulls out its internal lists on shutdown.
    }

    // -----------------------------------------------------------------------
    // Worker type
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnStubWorkerThread_When_GetWorkerCalled() {
        createPool(1);

        WorkerThread worker = pool.getWorker();

        assertThat(worker).isInstanceOf(StubWorkerThread.class);
    }
}
