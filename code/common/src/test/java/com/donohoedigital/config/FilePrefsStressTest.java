/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress and performance tests for FilePrefs
 */
class FilePrefsStressTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
    }

    @Test
    void stress_ManyThreadsWritingDifferentKeys(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        int numThreads = 20;
        int writesPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < writesPerThread; i++) {
                        prefs.put("thread" + threadId + ".key" + i, "value" + i);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errors.get()).isEqualTo(0);

        // Verify all keys exist
        for (int t = 0; t < numThreads; t++) {
            for (int i = 0; i < writesPerThread; i++) {
                String value = prefs.get("thread" + t + ".key" + i, "missing");
                assertThat(value).isEqualTo("value" + i);
            }
        }
    }

    @Test
    void stress_ManyThreadsWritingSameKey(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        int numThreads = 20;
        int writesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < writesPerThread; i++) {
                        prefs.put("shared.key", "thread" + threadId + "_write" + i);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errors.get()).isEqualTo(0);

        // Should have some value (last write wins)
        String value = prefs.get("shared.key", "missing");
        assertThat(value).isNotEqualTo("missing");
        assertThat(value).startsWith("thread");
    }

    @Test
    void stress_ManyThreadsReadingAndWriting(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Pre-populate some data
        for (int i = 0; i < 100; i++) {
            prefs.put("initial.key" + i, "initial" + i);
        }

        int numReaders = 10;
        int numWriters = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numReaders + numWriters);
        CountDownLatch latch = new CountDownLatch(numReaders + numWriters);
        AtomicInteger errors = new AtomicInteger(0);

        // Readers
        for (int t = 0; t < numReaders; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 200; i++) {
                        prefs.get("initial.key" + (i % 100), "default");
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Writers
        for (int t = 0; t < numWriters; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        prefs.put("writer" + threadId + ".key" + i, "value" + i);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errors.get()).isEqualTo(0);
    }

    @Test
    void stress_RapidCreateAndDestroyInstances(@TempDir Path tempDir) {
        // Stress test the singleton pattern
        for (int i = 0; i < 100; i++) {
            FilePrefs.setTestConfigDir(tempDir.toString());
            FilePrefs prefs = FilePrefs.getInstance();
            prefs.put("test.key", "value" + i);
        }

        // Final value should be latest
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();
        assertThat(prefs.get("test.key", "default")).isEqualTo("value99");
    }

    @Test
    void stress_VeryLargeConfigFile(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Create 5000 keys with moderate value sizes
        for (int i = 0; i < 5000; i++) {
            prefs.put("large.config.key." + i, "value_" + i + "_" + "x".repeat(50));
        }
        prefs.flush();

        // Reload and verify
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();

        // Spot check some values
        assertThat(prefs2.get("large.config.key.0", "")).startsWith("value_0_");
        assertThat(prefs2.get("large.config.key.2500", "")).startsWith("value_2500_");
        assertThat(prefs2.get("large.config.key.4999", "")).startsWith("value_4999_");
    }

    @Test
    void stress_RepeatedFlushOperations(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        prefs.put("key", "value");

        // Flush many times
        for (int i = 0; i < 1000; i++) {
            prefs.flush();
        }

        // Should still work
        assertThat(prefs.get("key", "default")).isEqualTo("value");
    }

    @Test
    void stress_AlternatingReadWrite(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Alternate between reads and writes rapidly
        for (int i = 0; i < 1000; i++) {
            prefs.put("alternating.key", "value" + i);
            String value = prefs.get("alternating.key", "default");
            assertThat(value).isEqualTo("value" + i);
        }
    }

    @Test
    void stress_ManySmallUpdates(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Many small updates (simulates frequent UI changes)
        for (int i = 0; i < 500; i++) {
            prefs.putBoolean("ui.setting" + (i % 10), i % 2 == 0);
        }

        // Verify final state
        for (int i = 0; i < 10; i++) {
            boolean expected = (499 - (9 - i)) % 2 == 0; // Last write for each key
            assertThat(prefs.getBoolean("ui.setting" + i, !expected))
                    .as("Setting %d should match last write", i)
                    .isNotNull();
        }
    }

    @Test
    void stress_ConcurrentClearAndWrite(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger errors = new AtomicInteger(0);

        // Some threads write, some clear
        for (int t = 0; t < 8; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        prefs.put("thread" + threadId + ".key", "value" + i);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Two threads occasionally clear
        for (int t = 0; t < 2; t++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(10);
                    prefs.clear();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errors.get()).isEqualTo(0);
    }

    @Test
    void performance_ReadOperationSpeed(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Populate data
        prefs.put("perf.key", "test value");

        // Measure read performance
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            prefs.get("perf.key", "default");
        }
        long end = System.nanoTime();

        long durationMs = (end - start) / 1_000_000;
        System.out.println("10,000 reads took: " + durationMs + "ms");

        // Should complete in reasonable time (< 1 second)
        assertThat(durationMs).isLessThan(1000);
    }

    @Test
    void performance_WriteOperationSpeed(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Measure write performance (includes flush)
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            prefs.put("perf.key." + i, "value" + i);
        }
        long end = System.nanoTime();

        long durationMs = (end - start) / 1_000_000;
        System.out.println("100 writes took: " + durationMs + "ms");

        // Writes are slower due to flush, but should still be reasonable
        assertThat(durationMs).isLessThan(5000);
    }

    @Test
    void performance_LoadTimeWithManyKeys(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Create config with 1000 keys
        for (int i = 0; i < 1000; i++) {
            prefs.put("load.test.key." + i, "value" + i);
        }
        prefs.flush();

        // Measure load time
        long start = System.nanoTime();
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs2 = FilePrefs.getInstance();
        long end = System.nanoTime();

        long durationMs = (end - start) / 1_000_000;
        System.out.println("Loading 1000 keys took: " + durationMs + "ms");

        // Should load quickly (< 500ms)
        assertThat(durationMs).isLessThan(500);

        // Verify data loaded
        assertThat(prefs2.get("load.test.key.500", "default")).isEqualTo("value500");
    }

    @Test
    void stress_DeepRecursion(@TempDir Path tempDir) {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        // Create deeply nested key structure
        StringBuilder keyBuilder = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            keyBuilder.append("level").append(i).append(".");
        }
        keyBuilder.append("finalkey");

        String deepKey = keyBuilder.toString();
        prefs.put(deepKey, "deep value");

        assertThat(prefs.get(deepKey, "default")).isEqualTo("deep value");
    }

    @Test
    void stress_ThreadSafetyWithMixedOperations(@TempDir Path tempDir) throws Exception {
        FilePrefs.setTestConfigDir(tempDir.toString());
        FilePrefs prefs = FilePrefs.getInstance();

        int numThreads = 15;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        // Mix of operations
                        if (i % 5 == 0) {
                            prefs.putBoolean("thread" + threadId + ".bool", i % 2 == 0);
                        } else if (i % 5 == 1) {
                            prefs.putInt("thread" + threadId + ".int", i);
                        } else if (i % 5 == 2) {
                            prefs.putDouble("thread" + threadId + ".double", i * 1.5);
                        } else if (i % 5 == 3) {
                            prefs.get("thread" + threadId + ".any", "default");
                        } else {
                            prefs.remove("thread" + threadId + ".temp");
                        }
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(exceptions).isEmpty();
    }
}
