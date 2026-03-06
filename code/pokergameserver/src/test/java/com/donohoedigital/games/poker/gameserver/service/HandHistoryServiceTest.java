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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.donohoedigital.games.poker.gameserver.persistence.entity.HandActionEntity;
import com.donohoedigital.games.poker.gameserver.persistence.entity.HandHistoryEntity;
import com.donohoedigital.games.poker.gameserver.persistence.entity.HandPlayerEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandActionRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandHistoryRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandPlayerRepository;
import com.donohoedigital.games.poker.protocol.dto.HandDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandExportData;
import com.donohoedigital.games.poker.protocol.dto.HandStatsData;
import com.donohoedigital.games.poker.protocol.dto.HandSummaryData;

@ExtendWith(MockitoExtension.class)
class HandHistoryServiceTest {

    @Mock
    private HandHistoryRepository handHistoryRepository;

    @Mock
    private HandPlayerRepository handPlayerRepository;

    @Mock
    private HandActionRepository handActionRepository;

    private HandHistoryService service;

    @BeforeEach
    void setUp() {
        service = new HandHistoryService(handHistoryRepository, handPlayerRepository, handActionRepository);
    }

    @Test
    void storeHand_savesAllEntities() {
        String gameId = "game-123";
        HandHistoryEntity hand = new HandHistoryEntity();
        hand.setHandNumber(1);

        HandHistoryEntity savedHand = new HandHistoryEntity();
        savedHand.setId(42L);
        savedHand.setGameId(gameId);
        savedHand.setHandNumber(1);
        when(handHistoryRepository.save(any())).thenReturn(savedHand);

        HandPlayerEntity player1 = new HandPlayerEntity();
        player1.setPlayerId(1);
        HandPlayerEntity player2 = new HandPlayerEntity();
        player2.setPlayerId(2);
        List<HandPlayerEntity> players = List.of(player1, player2);

        HandActionEntity action1 = new HandActionEntity();
        action1.setSequence(0);
        HandActionEntity action2 = new HandActionEntity();
        action2.setSequence(1);
        List<HandActionEntity> actions = List.of(action1, action2);

        service.storeHand(gameId, hand, players, actions);

        // Verify hand saved with gameId
        ArgumentCaptor<HandHistoryEntity> handCaptor = ArgumentCaptor.forClass(HandHistoryEntity.class);
        verify(handHistoryRepository).save(handCaptor.capture());
        assertThat(handCaptor.getValue().getGameId()).isEqualTo(gameId);

        // Verify players saved with handId
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HandPlayerEntity>> playersCaptor = ArgumentCaptor.forClass(List.class);
        verify(handPlayerRepository).saveAll(playersCaptor.capture());
        List<HandPlayerEntity> savedPlayers = playersCaptor.getValue();
        assertThat(savedPlayers).hasSize(2);
        assertThat(savedPlayers).allSatisfy(p -> assertThat(p.getHandId()).isEqualTo(42L));

        // Verify actions saved with handId
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HandActionEntity>> actionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(handActionRepository).saveAll(actionsCaptor.capture());
        List<HandActionEntity> savedActions = actionsCaptor.getValue();
        assertThat(savedActions).hasSize(2);
        assertThat(savedActions).allSatisfy(a -> assertThat(a.getHandId()).isEqualTo(42L));
    }

    @Test
    void getHandCount_delegatesToRepository() {
        when(handHistoryRepository.countByGameId("game-1")).thenReturn(5L);

        long count = service.getHandCount("game-1");

        assertThat(count).isEqualTo(5L);
        verify(handHistoryRepository).countByGameId("game-1");
    }

    @Test
    void getHandSummaries_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        HandHistoryEntity hand = new HandHistoryEntity();
        hand.setId(1L);
        hand.setHandNumber(1);
        hand.setCommunityCards("[\"Ac\",\"Kh\",\"Td\"]");
        Page<HandHistoryEntity> page = new PageImpl<>(List.of(hand), pageable, 1);
        when(handHistoryRepository.findByGameIdOrderByHandNumberDesc("game-1", pageable)).thenReturn(page);

        HandPlayerEntity player = new HandPlayerEntity();
        player.setHandId(1L);
        player.setHoleCards("[\"Ac\",\"Kh\"]");
        when(handPlayerRepository.findByHandIdIn(List.of(1L))).thenReturn(List.of(player));

        Page<HandSummaryData> result = service.getHandSummaries("game-1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).handNumber()).isEqualTo(1);
        assertThat(result.getContent().get(0).holeCards()).containsExactly("Ac", "Kh");
    }

    @Test
    void getHandDetail_assemblesFullData() {
        HandHistoryEntity hand = new HandHistoryEntity();
        hand.setId(10L);
        hand.setHandNumber(1);
        hand.setCommunityCards("[\"Ac\",\"Kh\",\"Td\"]");
        when(handHistoryRepository.findByGameIdAndId("game-1", 10L)).thenReturn(Optional.of(hand));

        HandPlayerEntity player = new HandPlayerEntity();
        player.setHandId(10L);
        player.setPlayerId(1);
        player.setPlayerName("Player1");
        player.setHoleCards("[\"Ac\",\"Kh\"]");
        when(handPlayerRepository.findByHandId(10L)).thenReturn(List.of(player));

        HandActionEntity action = new HandActionEntity();
        action.setHandId(10L);
        action.setSequence(0);
        action.setActionType("CALL");
        when(handActionRepository.findByHandIdOrderBySequenceAsc(10L)).thenReturn(List.of(action));

        Optional<HandDetailData> result = service.getHandDetail("game-1", 10L);

        assertThat(result).isPresent();
        HandDetailData detail = result.get();
        assertThat(detail.handNumber()).isEqualTo(1);
        assertThat(detail.players()).hasSize(1);
        assertThat(detail.players().get(0).playerName()).isEqualTo("Player1");
        assertThat(detail.actions()).hasSize(1);
        assertThat(detail.actions().get(0).actionType()).isEqualTo("CALL");
    }

    @Test
    void getHandDetail_returnsEmptyWhenNotFound() {
        when(handHistoryRepository.findByGameIdAndId("game-1", 99L)).thenReturn(Optional.empty());

        Optional<HandDetailData> result = service.getHandDetail("game-1", 99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getHandsForExport_returnsAllHandsWithFullData() {
        HandHistoryEntity hand1 = new HandHistoryEntity();
        hand1.setId(1L);
        hand1.setHandNumber(1);
        HandHistoryEntity hand2 = new HandHistoryEntity();
        hand2.setId(2L);
        hand2.setHandNumber(2);
        when(handHistoryRepository.findByGameId("game-1")).thenReturn(List.of(hand1, hand2));

        HandPlayerEntity p1 = new HandPlayerEntity();
        p1.setHandId(1L);
        HandPlayerEntity p2 = new HandPlayerEntity();
        p2.setHandId(2L);
        when(handPlayerRepository.findByHandIdIn(List.of(1L, 2L))).thenReturn(List.of(p1, p2));

        HandActionEntity a1 = new HandActionEntity();
        a1.setHandId(1L);
        HandActionEntity a2 = new HandActionEntity();
        a2.setHandId(2L);
        when(handActionRepository.findByHandIdIn(List.of(1L, 2L))).thenReturn(List.of(a1, a2));

        List<HandExportData> result = service.getHandsForExport("game-1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).players()).hasSize(1);
        assertThat(result.get(0).actions()).hasSize(1);
        assertThat(result.get(1).players()).hasSize(1);
        assertThat(result.get(1).actions()).hasSize(1);
    }

    @Test
    void getHandsForExport_returnsEmptyListWhenNoHands() {
        when(handHistoryRepository.findByGameId("game-empty")).thenReturn(List.of());

        List<HandExportData> result = service.getHandsForExport("game-empty");

        assertThat(result).isEmpty();
        verifyNoInteractions(handPlayerRepository);
        verifyNoInteractions(handActionRepository);
    }

    @Test
    void getHandStats_computesStatsByHandClass() {
        HandHistoryEntity hand = new HandHistoryEntity();
        hand.setId(1L);
        hand.setCommunityCardsDealt(5);
        when(handHistoryRepository.findByGameId("game-1")).thenReturn(List.of(hand));

        HandPlayerEntity player = new HandPlayerEntity();
        player.setHandId(1L);
        player.setPlayerId(0);
        player.setHoleCards("[\"Ac\",\"Kh\"]");
        player.setStartChips(1000);
        player.setEndChips(1200);
        player.setCardsExposed(true);
        when(handPlayerRepository.findByHandIdIn(List.of(1L))).thenReturn(List.of(player));

        List<HandStatsData> result = service.getHandStats("game-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).handClass()).isEqualTo("AKo");
        assertThat(result.get(0).count()).isEqualTo(1);
        assertThat(result.get(0).winPct()).isEqualTo(100.0);
    }

    @Test
    void getHandStats_returnsEmptyForNoHands() {
        when(handHistoryRepository.findByGameId("game-empty")).thenReturn(List.of());

        List<HandStatsData> result = service.getHandStats("game-empty");

        assertThat(result).isEmpty();
    }

    @Test
    void parseCardJson_handlesVariousFormats() {
        assertThat(HandHistoryService.parseCardJson(null)).isEmpty();
        assertThat(HandHistoryService.parseCardJson("")).isEmpty();
        assertThat(HandHistoryService.parseCardJson("[]")).isEmpty();
        assertThat(HandHistoryService.parseCardJson("[\"Ac\",\"Kh\"]")).containsExactly("Ac", "Kh");
        assertThat(HandHistoryService.parseCardJson("[\"Td\"]")).containsExactly("Td");
    }

    @Test
    void getHandClass_computesCorrectly() {
        // Pair
        assertThat(HandHistoryService.getHandClass("Ac", "Ad")).isEqualTo("AA");
        // Suited (higher rank first)
        assertThat(HandHistoryService.getHandClass("Ac", "Kc")).isEqualTo("AKs");
        // Offsuit (higher rank first)
        assertThat(HandHistoryService.getHandClass("Ac", "Kh")).isEqualTo("AKo");
        // Reversed order (lower card first)
        assertThat(HandHistoryService.getHandClass("Kh", "Ac")).isEqualTo("AKo");
        assertThat(HandHistoryService.getHandClass("9s", "Ts")).isEqualTo("T9s");
    }
}
