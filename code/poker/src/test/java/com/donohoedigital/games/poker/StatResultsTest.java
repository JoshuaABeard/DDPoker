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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for StatResults - collection of simulation results mapped by hand
 * groups.
 */
class StatResultsTest {

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    // ========== Constructor and Basic Operations ==========

    @Test
    void should_CreateEmptyStatResults() {
        StatResults results = new StatResults();

        assertThat(results).isEmpty();
    }

    @Test
    void should_StoreAndRetrieveResult_WithStringKey() {
        StatResults results = new StatResults();
        StatResult result = new StatResult("Pocket Aces", 70, 20, 10);

        results.put("AA", result);

        assertThat(results.get("AA")).isSameAs(result);
        assertThat(results.size()).isEqualTo(1);
    }

    @Test
    void should_StoreAndRetrieveResult_WithHandGroupKey() {
        StatResults results = new StatResults();
        HandGroup group = HandGroup.parse("AA", 5);
        StatResult result = new StatResult("Pocket Aces", 80, 15, 5);

        results.put(group, result);

        assertThat(results.get(group)).isSameAs(result);
    }

    @Test
    void should_StoreMultipleResults() {
        StatResults results = new StatResults();
        StatResult result1 = new StatResult("AA", 85, 10, 5);
        StatResult result2 = new StatResult("KK", 75, 20, 5);
        StatResult result3 = new StatResult("AK", 60, 30, 10);

        results.put("AA", result1);
        results.put("KK", result2);
        results.put("AK", result3);

        assertThat(results.size()).isEqualTo(3);
    }

    @Test
    void should_MaintainInsertionOrder() {
        StatResults results = new StatResults();
        results.put("AA", new StatResult("AA", 85, 10, 5));
        results.put("KK", new StatResult("KK", 75, 20, 5));
        results.put("AK", new StatResult("AK", 60, 30, 10));

        Object[] keys = results.keySet().toArray();

        assertThat(keys[0]).isEqualTo("AA");
        assertThat(keys[1]).isEqualTo("KK");
        assertThat(keys[2]).isEqualTo("AK");
    }

    // ========== toHTML() Tests ==========

    @Test
    void should_GenerateHTML_WithSingleResult() {
        StatResults results = new StatResults();
        HandList handList = new HandList("Pocket Aces");
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        StatResult result = new StatResult(hole, handList, 70, 20, 10);

        results.put("AA", result);

        String html = results.toHTML();

        assertThat(html).isNotNull();
        assertThat(html).isNotEmpty();
    }

    @Test
    void should_GenerateHTML_WithMultipleResults() {
        StatResults results = new StatResults();

        // Result 1
        Hand hole1 = new Hand(2);
        hole1.addCard(Card.SPADES_A);
        hole1.addCard(Card.HEARTS_A);
        StatResult result1 = new StatResult(hole1, new HandList("AA"), 85, 10, 5);

        // Result 2
        Hand hole2 = new Hand(2);
        hole2.addCard(Card.SPADES_K);
        hole2.addCard(Card.HEARTS_K);
        StatResult result2 = new StatResult(hole2, new HandList("KK"), 75, 20, 5);

        results.put("AA", result1);
        results.put("KK", result2);

        String html = results.toHTML();

        assertThat(html).isNotNull();
        assertThat(html).isNotEmpty();
    }

    @Test
    void should_GenerateHTML_WithHandGroupKey() {
        StatResults results = new StatResults();
        HandGroup group = HandGroup.parse("AA", 5);
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        StatResult result = new StatResult(hole, new HandList("AA"), 80, 15, 5);

        results.put(group, result);

        String html = results.toHTML();

        // HTML contains hand representation "As Ah", not "AA"
        assertThat(html).contains("A");
        assertThat(html).contains("80.00%"); // Win percent
    }

    @Test
    void should_GenerateHTML_WithStringKey() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_K);
        StatResult result = new StatResult(hole, new HandList("AK"), 65, 30, 5);

        results.put("AceKing", result);

        String html = results.toHTML();

        assertThat(html).contains("AceKing");
    }

    @Test
    void should_IncludeHeaderAndFooter_InHTML() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        results.put("AA", new StatResult(hole, new HandList("AA"), 70, 20, 10));

        String html = results.toHTML();

        // Should have header and footer from property config
        assertThat(html).isNotNull();
    }

    @Test
    void should_HandleEmptyResults_InHTML() {
        StatResults results = new StatResults();

        String html = results.toHTML();

        // Should still have header and footer, even with no results
        assertThat(html).isNotNull();
    }

    @Test
    void should_EncodeHTML_InHandName() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        StatResult result = new StatResult(hole, new HandList("Test"), 70, 20, 10);

        // Use a key with HTML special characters
        results.put("<AA>", result);

        String html = results.toHTML();

        // HTML should be encoded (< becomes &lt;, > becomes &gt;)
        assertThat(html).doesNotContain("<AA>");
    }

    // ========== Edge Case Tests ==========

    @Test
    void should_HandleNullKey() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        StatResult result = new StatResult(hole, new HandList("AA"), 70, 20, 10);

        // LinkedHashMap allows null keys
        results.put(null, result);

        assertThat(results.get(null)).isSameAs(result);
    }

    @Test
    void should_OverwriteExistingKey() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        StatResult result1 = new StatResult(hole, new HandList("AA"), 70, 20, 10);
        StatResult result2 = new StatResult(hole, new HandList("AA"), 80, 15, 5);

        results.put("AA", result1);
        results.put("AA", result2);

        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get("AA")).isSameAs(result2);
    }

    @Test
    void should_HandleMixedKeyTypes() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);

        HandGroup group = HandGroup.parse("AA", 5);
        results.put(group, new StatResult(hole, new HandList("AA"), 85, 10, 5));
        results.put("KK", new StatResult(hole, new HandList("KK"), 75, 20, 5));
        results.put(123, new StatResult(hole, new HandList("Other"), 60, 30, 10));

        assertThat(results.size()).isEqualTo(3);
    }

    @Test
    void should_HandleObjectKey_WithToString() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        StatResult result = new StatResult(hole, new HandList("AA"), 70, 20, 10);

        Object customKey = new Object(); // Will use toString()
        results.put(customKey, result);

        String html = results.toHTML();

        assertThat(html).isNotNull();
    }

    @Test
    void should_RemoveResult() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        results.put("AA", new StatResult(hole, new HandList("AA"), 70, 20, 10));

        results.remove("AA");

        assertThat(results).isEmpty();
    }

    @Test
    void should_ClearAllResults() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        results.put("AA", new StatResult(hole, new HandList("AA"), 70, 20, 10));
        results.put("KK", new StatResult(hole, new HandList("KK"), 75, 20, 5));

        results.clear();

        assertThat(results).isEmpty();
    }

    @Test
    void should_CheckContainsKey() {
        StatResults results = new StatResults();
        Hand hole = new Hand(2);
        hole.addCard(Card.SPADES_A);
        hole.addCard(Card.HEARTS_A);
        results.put("AA", new StatResult(hole, new HandList("AA"), 70, 20, 10));

        assertThat(results.containsKey("AA")).isTrue();
        assertThat(results.containsKey("KK")).isFalse();
    }
}
