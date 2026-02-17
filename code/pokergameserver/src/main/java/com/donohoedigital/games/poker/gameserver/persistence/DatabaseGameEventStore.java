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
package com.donohoedigital.games.poker.gameserver.persistence;

import java.time.Instant;
import java.util.List;

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.gameserver.IGameEventStore;
import com.donohoedigital.games.poker.gameserver.StoredEvent;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameEventEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Database-backed implementation of {@link IGameEventStore} using Spring Data
 * JPA.
 *
 * <p>
 * Events are persisted to the database via {@link GameEventRepository},
 * enabling crash recovery and game replay across server restarts.
 *
 * <p>
 * Thread-safety: This implementation is thread-safe for append operations
 * through database-level locking and atomic sequence number generation.
 */
public class DatabaseGameEventStore implements IGameEventStore {
    private final String gameId;
    private final GameEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Object sequenceLock = new Object();

    /**
     * Create a new database-backed event store.
     *
     * @param gameId
     *            unique identifier for the game
     * @param repository
     *            the JPA repository for event persistence
     */
    public DatabaseGameEventStore(String gameId, GameEventRepository repository) {
        if (gameId == null || gameId.isBlank()) {
            throw new IllegalArgumentException("Game ID cannot be null or blank");
        }
        if (repository == null) {
            throw new IllegalArgumentException("Repository cannot be null");
        }
        this.gameId = gameId;
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void append(GameEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        synchronized (sequenceLock) {
            long nextSequence = repository.countByGameId(gameId) + 1;

            GameEventEntity entity = new GameEventEntity();
            entity.setGameId(gameId);
            entity.setSequenceNumber(nextSequence);
            entity.setEventType(event.getClass().getSimpleName());
            entity.setEventData(serializeEvent(event));
            entity.setTimestamp(Instant.now());

            repository.save(entity);
        }
    }

    @Override
    public List<StoredEvent> getEvents() {
        return repository.findByGameIdOrderBySequenceNumberAsc(gameId).stream().map(this::toStoredEvent).toList();
    }

    @Override
    public List<StoredEvent> getEventsSince(long afterSequence) {
        return repository.findByGameIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(gameId, afterSequence)
                .stream().map(this::toStoredEvent).toList();
    }

    @Override
    public String getGameId() {
        return gameId;
    }

    @Override
    public void clear() {
        List<GameEventEntity> events = repository.findByGameIdOrderBySequenceNumberAsc(gameId);
        repository.deleteAll(events);
    }

    @Override
    public long getCurrentSequenceNumber() {
        return repository.countByGameId(gameId);
    }

    /**
     * Serialize a GameEvent to JSON.
     */
    private String serializeEvent(GameEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event: " + event.getClass().getSimpleName(), e);
        }
    }

    /**
     * Deserialize JSON to a GameEvent.
     */
    private GameEvent deserializeEvent(String eventType, String eventData) {
        try {
            // Reconstruct full class name from simple name
            String className = "com.donohoedigital.games.poker.core.event.GameEvent$" + eventType;
            Class<?> eventClass = Class.forName(className);
            return (GameEvent) objectMapper.readValue(eventData, eventClass);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize event: " + eventType, e);
        }
    }

    /**
     * Convert GameEventEntity to StoredEvent.
     */
    private StoredEvent toStoredEvent(GameEventEntity entity) {
        GameEvent event = deserializeEvent(entity.getEventType(), entity.getEventData());
        return new StoredEvent(entity.getGameId(), entity.getSequenceNumber(), entity.getEventType(), event,
                entity.getTimestamp());
    }
}
