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
package com.donohoedigital.games.poker.gameserver.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donohoedigital.games.poker.gameserver.persistence.entity.HandActionEntity;
import com.donohoedigital.games.poker.gameserver.persistence.entity.HandHistoryEntity;
import com.donohoedigital.games.poker.gameserver.persistence.entity.HandPlayerEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandActionRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandHistoryRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandPlayerRepository;
import com.donohoedigital.games.poker.protocol.dto.HandActionDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandExportData;
import com.donohoedigital.games.poker.protocol.dto.HandPlayerDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandStatsData;
import com.donohoedigital.games.poker.protocol.dto.HandSummaryData;

/**
 * Service for storing and querying hand history data.
 */
@Service
@Transactional
public class HandHistoryService {

    private final HandHistoryRepository handHistoryRepository;
    private final HandPlayerRepository handPlayerRepository;
    private final HandActionRepository handActionRepository;

    public HandHistoryService(HandHistoryRepository handHistoryRepository, HandPlayerRepository handPlayerRepository,
            HandActionRepository handActionRepository) {
        this.handHistoryRepository = handHistoryRepository;
        this.handPlayerRepository = handPlayerRepository;
        this.handActionRepository = handActionRepository;
    }

    /**
     * Store a complete hand with players and actions.
     *
     * @param gameId
     *            the game ID
     * @param hand
     *            the hand history entity
     * @param players
     *            the player entities
     * @param actions
     *            the action entities
     */
    public void storeHand(String gameId, HandHistoryEntity hand, List<HandPlayerEntity> players,
            List<HandActionEntity> actions) {
        hand.setGameId(gameId);
        HandHistoryEntity saved = handHistoryRepository.save(hand);
        Long handId = saved.getId();

        for (HandPlayerEntity player : players) {
            player.setHandId(handId);
        }
        handPlayerRepository.saveAll(players);

        for (HandActionEntity action : actions) {
            action.setHandId(handId);
        }
        handActionRepository.saveAll(actions);
    }

    /**
     * Count hands for a game.
     */
    @Transactional(readOnly = true)
    public long getHandCount(String gameId) {
        return handHistoryRepository.countByGameId(gameId);
    }

    /**
     * Get paginated hand summaries for a game.
     */
    @Transactional(readOnly = true)
    public Page<HandSummaryData> getHandSummaries(String gameId, Pageable pageable) {
        Page<HandHistoryEntity> page = handHistoryRepository.findByGameIdOrderByHandNumberDesc(gameId, pageable);
        List<Long> handIds = page.getContent().stream().map(HandHistoryEntity::getId).toList();
        Map<Long, List<HandPlayerEntity>> playersByHand = groupPlayersByHand(handIds);

        return page.map(hand -> toSummaryData(hand, playersByHand.getOrDefault(hand.getId(), List.of())));
    }

    /**
     * Get full hand detail including players and actions.
     *
     * @return the hand detail DTO, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<HandDetailData> getHandDetail(String gameId, Long handId) {
        return handHistoryRepository.findByGameIdAndId(gameId, handId).map(hand -> {
            List<HandPlayerEntity> players = handPlayerRepository.findByHandId(handId);
            List<HandActionEntity> actions = handActionRepository.findByHandIdOrderBySequenceAsc(handId);
            return toDetailData(hand, players, actions);
        });
    }

    /**
     * Get all hands with full data for export.
     */
    @Transactional(readOnly = true)
    public List<HandExportData> getHandsForExport(String gameId) {
        List<HandHistoryEntity> hands = handHistoryRepository.findByGameId(gameId);
        if (hands.isEmpty()) {
            return List.of();
        }

        List<Long> handIds = hands.stream().map(HandHistoryEntity::getId).toList();
        Map<Long, List<HandPlayerEntity>> playersByHand = groupPlayersByHand(handIds);
        Map<Long, List<HandActionEntity>> actionsByHand = groupActionsByHand(handIds);

        return hands.stream().map(hand -> {
            Long hid = hand.getId();
            List<HandPlayerEntity> players = playersByHand.getOrDefault(hid, List.of());
            List<HandActionEntity> actions = actionsByHand.getOrDefault(hid, List.of());
            return toExportData(hand, players, actions);
        }).toList();
    }

    /**
     * Compute aggregated hand stats for a game, grouped by hand class.
     */
    @Transactional(readOnly = true)
    public List<HandStatsData> getHandStats(String gameId) {
        List<HandHistoryEntity> hands = handHistoryRepository.findByGameId(gameId);
        if (hands.isEmpty()) {
            return List.of();
        }

        List<Long> handIds = hands.stream().map(HandHistoryEntity::getId).toList();
        Map<Long, List<HandPlayerEntity>> playersByHand = groupPlayersByHand(handIds);

        // Aggregate stats by hand class using the first player's (human's) hole cards
        Map<String, StatsAccumulator> accumulators = new HashMap<>();

        for (HandHistoryEntity hand : hands) {
            List<HandPlayerEntity> players = playersByHand.getOrDefault(hand.getId(), List.of());
            if (players.isEmpty()) {
                continue;
            }

            // First player is typically the human player
            HandPlayerEntity humanPlayer = players.get(0);
            List<String> holeCards = parseCardJson(humanPlayer.getHoleCards());
            if (holeCards.size() < 2) {
                continue;
            }

            String handClass = getHandClass(holeCards.get(0), holeCards.get(1));
            StatsAccumulator acc = accumulators.computeIfAbsent(handClass, k -> new StatsAccumulator());
            acc.count++;

            int chipDiff = humanPlayer.getEndChips() - humanPlayer.getStartChips();
            if (chipDiff > 0) {
                acc.wins++;
            } else if (chipDiff < 0) {
                acc.losses++;
            } else {
                acc.passes++;
            }

            acc.totalBet += Math.max(0, humanPlayer.getStartChips() - humanPlayer.getEndChips());
            acc.totalChips += humanPlayer.getEndChips();

            int cardsDealt = hand.getCommunityCardsDealt();
            if (cardsDealt >= 3)
                acc.sawFlop++;
            if (cardsDealt >= 4)
                acc.sawTurn++;
            if (cardsDealt >= 5)
                acc.sawRiver++;
            if (humanPlayer.isCardsExposed())
                acc.showdowns++;
        }

        List<HandStatsData> results = new ArrayList<>();
        for (Map.Entry<String, StatsAccumulator> entry : accumulators.entrySet()) {
            StatsAccumulator acc = entry.getValue();
            double count = acc.count;
            results.add(new HandStatsData(entry.getKey(), acc.count, acc.wins / count * 100.0,
                    acc.losses / count * 100.0, acc.passes / count * 100.0, acc.totalBet / count,
                    acc.totalChips / count, acc.sawFlop / count * 100.0, acc.sawTurn / count * 100.0,
                    acc.sawRiver / count * 100.0, acc.showdowns / count * 100.0));
        }

        results.sort((a, b) -> Integer.compare(b.count(), a.count()));
        return results;
    }

    // ---- Mapping methods ----

    /**
     * Convert a hand entity and its players to a summary DTO.
     */
    HandSummaryData toSummaryData(HandHistoryEntity hand, List<HandPlayerEntity> players) {
        // Use the first player's hole cards (typically the human player)
        List<String> holeCards = players.isEmpty() ? null : parseCardJson(players.get(0).getHoleCards());
        List<String> communityCards = parseCardJson(hand.getCommunityCards());

        return new HandSummaryData(hand.getId(), hand.getHandNumber(), hand.getTableId(), holeCards, communityCards,
                hand.getStartDate());
    }

    /**
     * Convert entities to a full detail DTO.
     */
    HandDetailData toDetailData(HandHistoryEntity hand, List<HandPlayerEntity> players,
            List<HandActionEntity> actions) {
        return new HandDetailData(hand.getId(), hand.getHandNumber(), hand.getTableId(), hand.getGameStyle(),
                hand.getGameType(), hand.getStartDate(), hand.getEndDate(), hand.getAnte(), hand.getSmallBlind(),
                hand.getBigBlind(), parseCardJson(hand.getCommunityCards()), hand.getCommunityCardsDealt(),
                players.stream().map(this::toPlayerDetailData).toList(),
                actions.stream().map(this::toActionDetailData).toList());
    }

    /**
     * Convert entities to an export DTO.
     */
    HandExportData toExportData(HandHistoryEntity hand, List<HandPlayerEntity> players,
            List<HandActionEntity> actions) {
        return new HandExportData(hand.getId(), hand.getHandNumber(), null, null, String.valueOf(hand.getTableId()),
                hand.getGameStyle(), hand.getGameType(), hand.getStartDate(), hand.getEndDate(), hand.getAnte(),
                hand.getSmallBlind(), hand.getBigBlind(), null, parseCardJson(hand.getCommunityCards()),
                players.stream().map(this::toPlayerDetailData).toList(),
                actions.stream().map(this::toActionDetailData).toList());
    }

    private HandPlayerDetailData toPlayerDetailData(HandPlayerEntity player) {
        return new HandPlayerDetailData(player.getPlayerId(), player.getPlayerName(), player.getSeatNumber(),
                player.getStartChips(), player.getEndChips(), parseCardJson(player.getHoleCards()),
                player.getPreflopActions(), player.getFlopActions(), player.getTurnActions(), player.getRiverActions(),
                player.isCardsExposed());
    }

    private HandActionDetailData toActionDetailData(HandActionEntity action) {
        return new HandActionDetailData(action.getPlayerId(), action.getSequence(), action.getRound(),
                action.getActionType(), action.getAmount(), action.getSubAmount(), action.isAllIn());
    }

    // ---- Card parsing and hand class computation ----

    /**
     * Parse a JSON array string like ["Ac","Kh"] into a list of card strings.
     */
    static List<String> parseCardJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        // Simple JSON array parser for string arrays
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of();
        }

        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String part : inner.split(",")) {
            String card = part.trim();
            // Remove surrounding quotes
            if (card.startsWith("\"") && card.endsWith("\"")) {
                card = card.substring(1, card.length() - 1);
            }
            if (!card.isEmpty()) {
                result.add(card);
            }
        }
        return result;
    }

    /**
     * Compute hand class from two card strings (e.g., "Ac", "Kh" -> "AKo"). Ported
     * from PokerDatabaseProcs.getHandClass().
     */
    static String getHandClass(String card1, String card2) {
        int rank1 = getCardRank(card1);
        int suit1 = getSuitRank(card1);
        int rank2 = getCardRank(card2);
        int suit2 = getSuitRank(card2);

        if (rank1 == rank2) {
            return getRankDisplay(rank1) + getRankDisplay(rank2);
        } else if (rank1 > rank2) {
            return getRankDisplay(rank1) + getRankDisplay(rank2) + (suit1 == suit2 ? "s" : "o");
        } else {
            return getRankDisplay(rank2) + getRankDisplay(rank1) + (suit1 == suit2 ? "s" : "o");
        }
    }

    private static int getCardRank(String card) {
        if (card == null || card.isEmpty()) {
            return -1;
        }
        char rank = card.charAt(0);
        return switch (rank) {
            case 'A', 'a' -> 14;
            case 'K', 'k' -> 13;
            case 'Q', 'q' -> 12;
            case 'J', 'j' -> 11;
            case 'T', 't' -> 10;
            case '9' -> 9;
            case '8' -> 8;
            case '7' -> 7;
            case '6' -> 6;
            case '5' -> 5;
            case '4' -> 4;
            case '3' -> 3;
            case '2' -> 2;
            default -> -1;
        };
    }

    private static int getSuitRank(String card) {
        if (card == null || card.length() < 2) {
            return -1;
        }
        char suit = card.charAt(1);
        return switch (suit) {
            case 'C', 'c' -> 0;
            case 'D', 'd' -> 1;
            case 'H', 'h' -> 2;
            case 'S', 's' -> 3;
            default -> -1;
        };
    }

    private static String getRankDisplay(int rank) {
        return switch (rank) {
            case 14 -> "A";
            case 13 -> "K";
            case 12 -> "Q";
            case 11 -> "J";
            case 10 -> "T";
            case 9 -> "9";
            case 8 -> "8";
            case 7 -> "7";
            case 6 -> "6";
            case 5 -> "5";
            case 4 -> "4";
            case 3 -> "3";
            case 2 -> "2";
            default -> "x";
        };
    }

    // ---- Helper methods ----

    private Map<Long, List<HandPlayerEntity>> groupPlayersByHand(List<Long> handIds) {
        if (handIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<HandPlayerEntity>> map = new HashMap<>();
        for (HandPlayerEntity player : handPlayerRepository.findByHandIdIn(handIds)) {
            map.computeIfAbsent(player.getHandId(), k -> new ArrayList<>()).add(player);
        }
        return map;
    }

    private Map<Long, List<HandActionEntity>> groupActionsByHand(List<Long> handIds) {
        if (handIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<HandActionEntity>> map = new HashMap<>();
        for (HandActionEntity action : handActionRepository.findByHandIdIn(handIds)) {
            map.computeIfAbsent(action.getHandId(), k -> new ArrayList<>()).add(action);
        }
        return map;
    }

    /**
     * Accumulator for computing hand statistics.
     */
    private static class StatsAccumulator {
        int count;
        int wins;
        int losses;
        int passes;
        long totalBet;
        long totalChips;
        int sawFlop;
        int sawTurn;
        int sawRiver;
        int showdowns;
    }
}
