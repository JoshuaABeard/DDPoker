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
package com.donohoedigital.games.poker.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.state.TableState;

/** Tests for {@link TableProcessResult}. */
class TableProcessResultTest {

    @Test
    void builder_shouldCreateResultWithDefaults() {
        TableProcessResult result = TableProcessResult.builder().build();

        assertThat(result.nextState()).isNull();
        assertThat(result.pendingState()).isNull();
        assertThat(result.phaseToRun()).isNull();
        assertThat(result.phaseParams()).isEmpty();
        assertThat(result.shouldSave()).isFalse();
        assertThat(result.shouldAutoSave()).isFalse();
        assertThat(result.shouldSleep()).isTrue(); // default: true
        assertThat(result.shouldRunOnClient()).isFalse();
        assertThat(result.shouldAddAllHumans()).isTrue(); // default: true
        assertThat(result.shouldOnlySendToWaitList()).isFalse();
        assertThat(result.events()).isEmpty();
    }

    @Test
    void builder_shouldSetNextState() {
        TableProcessResult result = TableProcessResult.builder().nextState(TableState.BETTING).build();

        assertThat(result.nextState()).isEqualTo(TableState.BETTING);
    }

    @Test
    void builder_shouldSetPendingState() {
        TableProcessResult result = TableProcessResult.builder().pendingState(TableState.CLEAN).build();

        assertThat(result.pendingState()).isEqualTo(TableState.CLEAN);
    }

    @Test
    void builder_shouldSetPhaseToRun() {
        TableProcessResult result = TableProcessResult.builder().phaseToRun("DealPhase").build();

        assertThat(result.phaseToRun()).isEqualTo("DealPhase");
    }

    @Test
    void builder_shouldSetPhaseParams() {
        Map<String, Object> params = Map.of("param1", "value1", "param2", 42);

        TableProcessResult result = TableProcessResult.builder().phaseParams(params).build();

        assertThat(result.phaseParams()).containsAllEntriesOf(params);
    }

    @Test
    void builder_shouldSetFlags() {
        TableProcessResult result = TableProcessResult.builder().shouldSave(true).shouldAutoSave(true)
                .shouldSleep(false).shouldRunOnClient(true).shouldAddAllHumans(false).shouldOnlySendToWaitList(true)
                .build();

        assertThat(result.shouldSave()).isTrue();
        assertThat(result.shouldAutoSave()).isTrue();
        assertThat(result.shouldSleep()).isFalse();
        assertThat(result.shouldRunOnClient()).isTrue();
        assertThat(result.shouldAddAllHumans()).isFalse();
        assertThat(result.shouldOnlySendToWaitList()).isTrue();
    }

    @Test
    void builder_shouldSetEvents() {
        List<GameEvent> events = List.of(new GameEvent.HandStarted(1, 10), new GameEvent.HandCompleted(1));

        TableProcessResult result = TableProcessResult.builder().events(events).build();

        assertThat(result.events()).containsExactlyElementsOf(events);
    }

    @Test
    void phaseParams_shouldBeImmutable() {
        Map<String, Object> params = Map.of("key", "value");
        TableProcessResult result = TableProcessResult.builder().phaseParams(params).build();

        assertThatThrownBy(() -> result.phaseParams().put("newKey", "newValue"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void events_shouldBeImmutable() {
        List<GameEvent> events = List.of(new GameEvent.HandStarted(1, 10));
        TableProcessResult result = TableProcessResult.builder().events(events).build();

        assertThatThrownBy(() -> result.events().add(new GameEvent.HandCompleted(1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void equals_shouldWorkCorrectly() {
        TableProcessResult result1 = TableProcessResult.builder().nextState(TableState.BETTING).shouldSave(true)
                .build();

        TableProcessResult result2 = TableProcessResult.builder().nextState(TableState.BETTING).shouldSave(true)
                .build();

        TableProcessResult result3 = TableProcessResult.builder().nextState(TableState.CLEAN).shouldSave(true).build();

        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isNotEqualTo(result3);
    }

    @Test
    void hashCode_shouldBeConsistent() {
        TableProcessResult result1 = TableProcessResult.builder().nextState(TableState.BETTING).shouldSave(true)
                .build();

        TableProcessResult result2 = TableProcessResult.builder().nextState(TableState.BETTING).shouldSave(true)
                .build();

        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void toString_shouldContainKeyInfo() {
        TableProcessResult result = TableProcessResult.builder().nextState(TableState.BETTING).phaseToRun("DealPhase")
                .shouldSave(true).build();

        String str = result.toString();

        assertThat(str).contains("nextState=BETTING");
        assertThat(str).contains("phaseToRun='DealPhase'");
        assertThat(str).contains("shouldSave=true");
    }
}
