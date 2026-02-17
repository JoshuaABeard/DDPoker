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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameInstanceEntity;

/**
 * Tests for {@link GameInstanceRepository}.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameInstanceRepositoryTest {

    @Autowired
    private GameInstanceRepository repository;

    @Test
    void testSaveAndFindById() {
        GameInstanceEntity entity = createTestEntity("game-1", GameInstanceState.WAITING_FOR_PLAYERS);

        GameInstanceEntity saved = repository.save(entity);

        assertThat(saved.getGameId()).isEqualTo("game-1");

        Optional<GameInstanceEntity> found = repository.findById("game-1");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Game");
        assertThat(found.get().getStatus()).isEqualTo(GameInstanceState.WAITING_FOR_PLAYERS);
    }

    @Test
    void testFindByStatus() {
        repository.save(createTestEntity("game-1", GameInstanceState.WAITING_FOR_PLAYERS));
        repository.save(createTestEntity("game-2", GameInstanceState.IN_PROGRESS));
        repository.save(createTestEntity("game-3", GameInstanceState.WAITING_FOR_PLAYERS));
        repository.save(createTestEntity("game-4", GameInstanceState.COMPLETED));

        List<GameInstanceEntity> waiting = repository.findByStatus(GameInstanceState.WAITING_FOR_PLAYERS);

        assertThat(waiting).hasSize(2);
        assertThat(waiting).extracting(GameInstanceEntity::getGameId).containsExactlyInAnyOrder("game-1", "game-3");
    }

    @Test
    void testFindByHostingType() {
        GameInstanceEntity server1 = createTestEntity("game-1", GameInstanceState.IN_PROGRESS);
        server1.setHostingType("SERVER");

        GameInstanceEntity server2 = createTestEntity("game-2", GameInstanceState.IN_PROGRESS);
        server2.setHostingType("SERVER");

        GameInstanceEntity community = createTestEntity("game-3", GameInstanceState.IN_PROGRESS);
        community.setHostingType("COMMUNITY");

        repository.save(server1);
        repository.save(server2);
        repository.save(community);

        List<GameInstanceEntity> serverGames = repository.findByHostingType("SERVER");

        assertThat(serverGames).hasSize(2);
        assertThat(serverGames).extracting(GameInstanceEntity::getGameId).containsExactlyInAnyOrder("game-1", "game-2");
    }

    @Test
    void testFindByOwnerProfileId() {
        GameInstanceEntity owner100_1 = createTestEntity("game-1", GameInstanceState.IN_PROGRESS);
        owner100_1.setOwnerProfileId(100L);

        GameInstanceEntity owner100_2 = createTestEntity("game-2", GameInstanceState.WAITING_FOR_PLAYERS);
        owner100_2.setOwnerProfileId(100L);

        GameInstanceEntity owner200 = createTestEntity("game-3", GameInstanceState.IN_PROGRESS);
        owner200.setOwnerProfileId(200L);

        repository.save(owner100_1);
        repository.save(owner100_2);
        repository.save(owner200);

        List<GameInstanceEntity> owner100Games = repository.findByOwnerProfileId(100L);

        assertThat(owner100Games).hasSize(2);
        assertThat(owner100Games).extracting(GameInstanceEntity::getGameId).containsExactlyInAnyOrder("game-1",
                "game-2");
    }

    @Test
    void testUpdateStatus() {
        GameInstanceEntity entity = createTestEntity("game-1", GameInstanceState.WAITING_FOR_PLAYERS);
        repository.save(entity);

        repository.updateStatus("game-1", GameInstanceState.IN_PROGRESS);
        repository.flush();

        GameInstanceEntity updated = repository.findById("game-1").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GameInstanceState.IN_PROGRESS);
    }

    @Test
    void testUpdateStatusWithStartTime() {
        GameInstanceEntity entity = createTestEntity("game-1", GameInstanceState.WAITING_FOR_PLAYERS);
        repository.save(entity);

        Instant startTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        repository.updateStatusWithStartTime("game-1", GameInstanceState.IN_PROGRESS, startTime);
        repository.flush();

        GameInstanceEntity updated = repository.findById("game-1").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GameInstanceState.IN_PROGRESS);
        assertThat(updated.getStartedAt()).isEqualTo(startTime);
    }

    @Test
    void testUpdateStatusWithCompletionTime() {
        GameInstanceEntity entity = createTestEntity("game-1", GameInstanceState.IN_PROGRESS);
        entity.setStartedAt(Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        repository.save(entity);

        Instant completionTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        repository.updateStatusWithCompletionTime("game-1", GameInstanceState.COMPLETED, completionTime);
        repository.flush();

        GameInstanceEntity updated = repository.findById("game-1").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(GameInstanceState.COMPLETED);
        assertThat(updated.getCompletedAt()).isEqualTo(completionTime);
    }

    @Test
    void testUpdatePlayerCount() {
        GameInstanceEntity entity = createTestEntity("game-1", GameInstanceState.WAITING_FOR_PLAYERS);
        entity.setPlayerCount(0);
        repository.save(entity);

        repository.updatePlayerCount("game-1", 5);
        repository.flush();

        GameInstanceEntity updated = repository.findById("game-1").orElseThrow();
        assertThat(updated.getPlayerCount()).isEqualTo(5);
    }

    @Test
    void testDelete() {
        GameInstanceEntity entity = createTestEntity("game-1", GameInstanceState.CANCELLED);
        repository.save(entity);

        assertThat(repository.findById("game-1")).isPresent();

        repository.deleteById("game-1");

        assertThat(repository.findById("game-1")).isEmpty();
    }

    // Helper method
    private GameInstanceEntity createTestEntity(String gameId, GameInstanceState status) {
        GameInstanceEntity entity = new GameInstanceEntity();
        entity.setGameId(gameId);
        entity.setName("Test Game");
        entity.setStatus(status);
        entity.setOwnerProfileId(100L);
        entity.setOwnerName("TestOwner");
        entity.setProfileData("{\"name\":\"Test\"}");
        entity.setMaxPlayers(10);
        entity.setPlayerCount(0);
        entity.setHostingType("SERVER");
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
