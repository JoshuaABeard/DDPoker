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
package com.donohoedigital.games.poker.engine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for improved deck shuffling randomness using SecureRandom
 */
public class DeckRandomnessTest {

    /**
     * Test that unseeded mode (seed=0) produces non-deterministic shuffles using
     * SecureRandom without setSeed()
     */
    @Test
    public void testUnseededModeUsesSecureRandom() {
        // Create two decks with seed=0 (unseeded mode)
        Deck deck1 = new Deck(true, 0);
        Deck deck2 = new Deck(true, 0);

        // Verify at least one card position is different
        // (extremely high probability with SecureRandom)
        boolean foundDifference = false;
        for (int i = 0; i < 52; i++) {
            if (!deck1.get(i).equals(deck2.get(i))) {
                foundDifference = true;
                break;
            }
        }
        assertTrue("Production decks should have at least one different card position", foundDifference);
    }

    /**
     * Test that seeded mode (seed>0) produces deterministic shuffles
     */
    @Test
    public void testSeededModeIsDeterministic() {
        long testSeed = 9183349L; // Test seed for deterministic shuffling

        // Create two decks with the same seed
        Deck deck1 = new Deck(true, testSeed);
        Deck deck2 = new Deck(true, testSeed);

        // Verify every card is in the same position
        for (int i = 0; i < 52; i++) {
            assertTrue("Card at position " + i + " should match", deck1.get(i).equals(deck2.get(i)));
        }
    }

    /**
     * Test that different seeds produce different shuffles in seeded mode
     */
    @Test
    public void testDifferentSeedsProduceDifferentShuffles() {
        Deck deck1 = new Deck(true, 9183349L);
        Deck deck2 = new Deck(true, 9183478L); // Different seed

        // They should be different - check if at least one card differs
        boolean foundDifference = false;
        for (int i = 0; i < 52; i++) {
            if (!deck1.get(i).equals(deck2.get(i))) {
                foundDifference = true;
                break;
            }
        }
        assertTrue("Decks with different seeds should have at least one different card position", foundDifference);
    }

    /**
     * Test ThreadLocal isolation - multiple threads should get independent
     * SecureRandom instances and produce different shuffles
     */
    @Test
    public void testThreadLocalIsolation() throws InterruptedException {
        final int numThreads = 4;
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final List<Deck> deckResults = new ArrayList<>();
        final AtomicInteger failures = new AtomicInteger(0);

        // Create multiple threads that create decks simultaneously
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    // Each thread creates a deck with seed=0 (unseeded mode)
                    Deck deck = new Deck(true, 0);
                    synchronized (deckResults) {
                        deckResults.add(deck);
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        assertEquals("All threads should complete successfully", 0, failures.get());
        assertEquals("Should have results from all threads", numThreads, deckResults.size());

        // At least some of the decks should be different (extremely high probability)
        // Check that not all are identical by comparing first card of each deck
        Deck first = deckResults.get(0);
        boolean foundDifferent = false;
        for (int i = 1; i < deckResults.size(); i++) {
            if (!first.get(0).equals(deckResults.get(i).get(0))) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue("ThreadLocal SecureRandom should produce different shuffles across threads", foundDifferent);
    }

    /**
     * Test that shuffle() uses ThreadLocalRandom and produces non-deterministic
     * results
     */
    @Test
    public void testShuffleUsesThreadLocalRandom() {
        // Create unshuffled deck
        Deck deck1 = new Deck(false);
        Deck deck2 = new Deck(false);

        // Verify they start identical
        for (int i = 0; i < 52; i++) {
            assertTrue("Unshuffled decks should be identical at position " + i, deck1.get(i).equals(deck2.get(i)));
        }

        // Shuffle both
        deck1.shuffle();
        deck2.shuffle();

        // After shuffling, they should be different (extremely high probability)
        boolean foundDifference = false;
        for (int i = 0; i < 52; i++) {
            if (!deck1.get(i).equals(deck2.get(i))) {
                foundDifference = true;
                break;
            }
        }
        assertTrue("shuffle() should produce non-deterministic results", foundDifference);
    }

    /**
     * Test that addRandom() uses ThreadLocalRandom and produces non-deterministic
     * results
     */
    @Test
    public void testAddRandomUsesThreadLocalRandom() {
        // Create two decks, each with 47 cards (remove 5 cards)
        Deck deck1 = new Deck(false);
        Deck deck2 = new Deck(false);

        // Remove 5 cards from each deck to create hands
        Hand hand1 = new Hand(5);
        Hand hand2 = new Hand(5);
        for (int i = 0; i < 5; i++) {
            hand1.addCard(deck1.nextCard());
            hand2.addCard(deck2.nextCard());
        }

        // Now add the hands back randomly
        deck1.addRandom(hand1);
        deck2.addRandom(hand2);

        // The decks should be different after addRandom() shuffles them
        // (extremely high probability with ThreadLocalRandom)
        boolean foundDifference = false;
        for (int i = 0; i < 52; i++) {
            if (!deck1.get(i).equals(deck2.get(i))) {
                foundDifference = true;
                break;
            }
        }
        assertTrue("addRandom() should produce non-deterministic results (ThreadLocalRandom)", foundDifference);
    }

    /**
     * Test that production shuffle is truly random across multiple iterations
     */
    @Test
    public void testProductionShuffleRandomnessAcrossMultipleDecks() {
        final int numDecks = 10;
        List<Deck> decks = new ArrayList<>();

        // Create 10 production decks
        for (int i = 0; i < numDecks; i++) {
            decks.add(new Deck(true, 0));
        }

        // Count unique first cards (simple uniqueness check)
        long uniqueFirstCards = decks.stream().map(d -> d.get(0)).distinct().count();

        // With SecureRandom, we expect at least half to have different first cards
        // (extremely conservative - actual probability is much higher)
        assertTrue("Unseeded mode should produce varied shuffles, got " + uniqueFirstCards
                + " unique first cards out of " + numDecks, uniqueFirstCards >= numDecks / 2);
    }

    /**
     * Test that seed 0 is treated as unseeded (random) mode
     */
    @Test
    public void testSeedZeroIsUnseededMode() {
        Deck deck1 = new Deck(true, 0);
        Deck deck2 = new Deck(true, 0);

        // Seed 0 should use SecureRandom, so decks should differ
        boolean foundDifference = false;
        for (int i = 0; i < 52; i++) {
            if (!deck1.get(i).equals(deck2.get(i))) {
                foundDifference = true;
                break;
            }
        }
        assertTrue("Seed 0 should trigger unseeded mode (SecureRandom)", foundDifference);
    }
}
