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
package com.donohoedigital.games.poker.gameserver.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * JPA entity for hand player data. Stores per-player information for each hand.
 */
@Entity
@Table(name = "hand_players", indexes = {@Index(name = "idx_hand_players_hand_id", columnList = "hand_id")})
public class HandPlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hand_id", nullable = false)
    private Long handId;

    @Column(name = "player_id", nullable = false)
    private int playerId;

    @Column(name = "player_name", length = 100)
    private String playerName;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "start_chips", nullable = false)
    private int startChips;

    @Column(name = "end_chips", nullable = false)
    private int endChips;

    @Column(name = "hole_cards", columnDefinition = "TEXT")
    private String holeCards;

    @Column(name = "preflop_actions", nullable = false)
    private int preflopActions;

    @Column(name = "flop_actions", nullable = false)
    private int flopActions;

    @Column(name = "turn_actions", nullable = false)
    private int turnActions;

    @Column(name = "river_actions", nullable = false)
    private int riverActions;

    @Column(name = "cards_exposed", nullable = false)
    private boolean cardsExposed;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getHandId() {
        return handId;
    }

    public void setHandId(Long handId) {
        this.handId = handId;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public int getStartChips() {
        return startChips;
    }

    public void setStartChips(int startChips) {
        this.startChips = startChips;
    }

    public int getEndChips() {
        return endChips;
    }

    public void setEndChips(int endChips) {
        this.endChips = endChips;
    }

    public String getHoleCards() {
        return holeCards;
    }

    public void setHoleCards(String holeCards) {
        this.holeCards = holeCards;
    }

    public int getPreflopActions() {
        return preflopActions;
    }

    public void setPreflopActions(int preflopActions) {
        this.preflopActions = preflopActions;
    }

    public int getFlopActions() {
        return flopActions;
    }

    public void setFlopActions(int flopActions) {
        this.flopActions = flopActions;
    }

    public int getTurnActions() {
        return turnActions;
    }

    public void setTurnActions(int turnActions) {
        this.turnActions = turnActions;
    }

    public int getRiverActions() {
        return riverActions;
    }

    public void setRiverActions(int riverActions) {
        this.riverActions = riverActions;
    }

    public boolean isCardsExposed() {
        return cardsExposed;
    }

    public void setCardsExposed(boolean cardsExposed) {
        this.cardsExposed = cardsExposed;
    }
}
