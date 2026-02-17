/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver.persistence.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameEventEntity;

/**
 * Tests for {@link GameEventRepository}.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameEventRepositoryTest {

    @Autowired
    private GameEventRepository repository;

    @Test
    void testSaveAndFind() {
        GameEventEntity event = createEvent("game-1", 1, "GameStarted", "{\"players\":10}");

        GameEventEntity saved = repository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getGameId()).isEqualTo("game-1");
        assertThat(saved.getSequenceNumber()).isEqualTo(1);
    }

    @Test
    void testFindByGameIdOrderedBySequence() {
        repository.save(createEvent("game-1", 3, "Event3", "{}"));
        repository.save(createEvent("game-1", 1, "Event1", "{}"));
        repository.save(createEvent("game-1", 2, "Event2", "{}"));
        repository.save(createEvent("game-2", 1, "OtherGame", "{}"));

        List<GameEventEntity> events = repository.findByGameIdOrderBySequenceNumberAsc("game-1");

        assertThat(events).hasSize(3);
        assertThat(events).extracting(GameEventEntity::getSequenceNumber).containsExactly(1L, 2L, 3L);
        assertThat(events).extracting(GameEventEntity::getEventType).containsExactly("Event1", "Event2", "Event3");
    }

    @Test
    void testFindByGameIdAndSequenceGreaterThan() {
        repository.save(createEvent("game-1", 1, "Event1", "{}"));
        repository.save(createEvent("game-1", 2, "Event2", "{}"));
        repository.save(createEvent("game-1", 3, "Event3", "{}"));
        repository.save(createEvent("game-1", 4, "Event4", "{}"));
        repository.save(createEvent("game-1", 5, "Event5", "{}"));

        List<GameEventEntity> events = repository
                .findByGameIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc("game-1", 2L);

        assertThat(events).hasSize(3);
        assertThat(events).extracting(GameEventEntity::getSequenceNumber).containsExactly(3L, 4L, 5L);
    }

    @Test
    void testCountByGameId() {
        repository.save(createEvent("game-1", 1, "Event1", "{}"));
        repository.save(createEvent("game-1", 2, "Event2", "{}"));
        repository.save(createEvent("game-1", 3, "Event3", "{}"));
        repository.save(createEvent("game-2", 1, "OtherGame", "{}"));

        long count = repository.countByGameId("game-1");

        assertThat(count).isEqualTo(3);
    }

    @Test
    void testEventDataStoredAsText() {
        String jsonData = "{\"eventType\":\"PlayerAction\",\"playerId\":42,\"action\":\"RAISE\",\"amount\":100}";
        GameEventEntity event = createEvent("game-1", 1, "PlayerAction", jsonData);

        repository.save(event);

        GameEventEntity found = repository.findByGameIdOrderBySequenceNumberAsc("game-1").get(0);
        assertThat(found.getEventData()).isEqualTo(jsonData);
    }

    @Test
    void testMultipleGamesIndependent() {
        repository.save(createEvent("game-1", 1, "G1-E1", "{}"));
        repository.save(createEvent("game-1", 2, "G1-E2", "{}"));
        repository.save(createEvent("game-2", 1, "G2-E1", "{}"));
        repository.save(createEvent("game-2", 2, "G2-E2", "{}"));
        repository.save(createEvent("game-2", 3, "G2-E3", "{}"));

        assertThat(repository.countByGameId("game-1")).isEqualTo(2);
        assertThat(repository.countByGameId("game-2")).isEqualTo(3);
    }

    // Helper method
    private GameEventEntity createEvent(String gameId, long sequenceNumber, String eventType, String eventData) {
        GameEventEntity event = new GameEventEntity();
        event.setGameId(gameId);
        event.setSequenceNumber(sequenceNumber);
        event.setEventType(eventType);
        event.setEventData(eventData);
        event.setTimestamp(Instant.now());
        return event;
    }
}
