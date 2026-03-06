/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import java.util.Date;

/**
 * Client-side tournament history record. Replaces
 * {@code com.donohoedigital.games.poker.model.TournamentHistory} in the desktop
 * client. Used for local database storage and display only.
 */
public class ClientTournamentHistory {

    // player types
    public static final int PLAYER_TYPE_AI = 1;
    public static final int PLAYER_TYPE_ONLINE = 2;
    public static final int PLAYER_TYPE_LOCAL = 3;

    private Long id;
    private int buyin;
    private int rebuys;
    private int addons;
    private int place;
    private int prize;
    private String playerName;
    private int playerType;
    private Date endDate;
    private boolean ended;
    private String tournamentName;
    private int numPlayers;

    // client-only fields
    private long gameId;
    private String tournamentType;
    private Date startDate;
    private int numRemaining;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String sTournamentName) {
        tournamentName = sTournamentName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String sPlayerName) {
        playerName = sPlayerName;
    }

    public int getPlayerType() {
        return playerType;
    }

    public boolean isComputer() {
        return playerType == PLAYER_TYPE_AI;
    }

    public void setPlayerType(int nPlayerType) {
        playerType = nPlayerType;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date date) {
        endDate = date;
    }

    public int getTotalSpent() {
        return getBuyin() + getRebuy() + getAddon();
    }

    public int getBuyin() {
        return buyin;
    }

    public void setBuyin(int nBuyin) {
        buyin = nBuyin;
    }

    public int getAddon() {
        return addons;
    }

    public void setAddon(int nAddon) {
        addons = nAddon;
    }

    public int getRebuy() {
        return rebuys;
    }

    public void setRebuy(int nRebuys) {
        rebuys = nRebuys;
    }

    public void setPlacePrizeNumPlayers(int nPlace, int nPrize, int numPlayers, int nRank, int nChipCount) {
        if (nPlace == 0) {
            nPlace = nRank - (numPlayers + 1);
            nPrize = -nChipCount;
        }
        setPlace(nPlace);
        setPrize(nPrize);
        setNumPlayers(numPlayers);
    }

    public boolean isAlive() {
        return !isEnded() && getPlace() < 0;
    }

    public int getRank() {
        return place + (numPlayers + 1);
    }

    public int getNumChips() {
        return getPrize() * -1;
    }

    public int getNet() {
        return prize - getTotalSpent();
    }

    public int getPlace() {
        return place;
    }

    public void setPlace(int nPlace) {
        place = nPlace;
    }

    public int getPrize() {
        return prize;
    }

    public void setPrize(int nPrize) {
        prize = nPrize;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public void setNumPlayers(int nNum) {
        numPlayers = nNum;
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean b) {
        ended = b;
    }

    public long getGameId() {
        return gameId;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }

    public String getTournamentType() {
        return tournamentType;
    }

    public void setTournamentType(String sTournamentType) {
        tournamentType = sTournamentType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date date) {
        startDate = date;
    }

    public int getNumRemaining() {
        return numRemaining;
    }

    public void setNumRemaining(int nNum) {
        numRemaining = nNum;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClientTournamentHistory))
            return false;
        ClientTournamentHistory h = (ClientTournamentHistory) o;
        return id != null && id.equals(h.getId());
    }

    @Override
    public int hashCode() {
        if (id == null)
            return super.hashCode();
        else
            return (int) id.longValue();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("Player=").append(getPlayerName());
        sb.append(", id=").append(getId());
        sb.append(", type=").append(getPlayerType());
        sb.append(", place=").append(getPlace());
        sb.append('}');
        return sb.toString();
    }
}
