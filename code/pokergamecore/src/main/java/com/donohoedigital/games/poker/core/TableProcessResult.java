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
package com.donohoedigital.games.poker.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.state.TableState;

/**
 * Result of processing a table state in TournamentEngine. Replaces TDreturn
 * inner class. Immutable data structure that tells the caller what happened and
 * what to do next.
 */
public final class TableProcessResult {
    private final TableState nextState;
    private final TableState pendingState;
    private final String phaseToRun;
    private final Map<String, Object> phaseParams;
    private final boolean shouldSave;
    private final boolean shouldAutoSave;
    private final boolean shouldSleep;
    private final boolean shouldRunOnClient;
    private final boolean shouldAddAllHumans;
    private final boolean shouldOnlySendToWaitList;
    private final List<GameEvent> events;

    private TableProcessResult(Builder builder) {
        this.nextState = builder.nextState;
        this.pendingState = builder.pendingState;
        this.phaseToRun = builder.phaseToRun;
        this.phaseParams = builder.phaseParams != null
                ? Collections.unmodifiableMap(builder.phaseParams)
                : Collections.emptyMap();
        this.shouldSave = builder.shouldSave;
        this.shouldAutoSave = builder.shouldAutoSave;
        this.shouldSleep = builder.shouldSleep;
        this.shouldRunOnClient = builder.shouldRunOnClient;
        this.shouldAddAllHumans = builder.shouldAddAllHumans;
        this.shouldOnlySendToWaitList = builder.shouldOnlySendToWaitList;
        this.events = builder.events != null ? Collections.unmodifiableList(builder.events) : Collections.emptyList();
    }

    /** @return the next table state to transition to, or null if no state change */
    public TableState nextState() {
        return nextState;
    }

    /** @return the pending table state for delayed transitions, or null */
    public TableState pendingState() {
        return pendingState;
    }

    /** @return the name of the phase to run, or null */
    public String phaseToRun() {
        return phaseToRun;
    }

    /** @return immutable map of phase parameters (never null) */
    public Map<String, Object> phaseParams() {
        return phaseParams;
    }

    /** @return true if the game state should be saved */
    public boolean shouldSave() {
        return shouldSave;
    }

    /** @return true if the game state should be auto-saved */
    public boolean shouldAutoSave() {
        return shouldAutoSave;
    }

    /** @return true if the tournament director should sleep after processing */
    public boolean shouldSleep() {
        return shouldSleep;
    }

    /** @return true if the phase should run on the client (online mode) */
    public boolean shouldRunOnClient() {
        return shouldRunOnClient;
    }

    /** @return true if all human players should be added */
    public boolean shouldAddAllHumans() {
        return shouldAddAllHumans;
    }

    /** @return true if updates should only be sent to wait list */
    public boolean shouldOnlySendToWaitList() {
        return shouldOnlySendToWaitList;
    }

    /**
     * @return immutable list of game events emitted during processing (never null)
     */
    public List<GameEvent> events() {
        return events;
    }

    /**
     * Create a new builder with default values.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for TableProcessResult with fluent API. */
    public static final class Builder {
        private TableState nextState;
        private TableState pendingState;
        private String phaseToRun;
        private Map<String, Object> phaseParams;
        private boolean shouldSave;
        private boolean shouldAutoSave;
        private boolean shouldSleep = true; // default: sleep
        private boolean shouldRunOnClient;
        private boolean shouldAddAllHumans = true; // default: add all humans
        private boolean shouldOnlySendToWaitList;
        private List<GameEvent> events;

        private Builder() {
        }

        public Builder nextState(TableState nextState) {
            this.nextState = nextState;
            return this;
        }

        public Builder pendingState(TableState pendingState) {
            this.pendingState = pendingState;
            return this;
        }

        public Builder phaseToRun(String phaseToRun) {
            this.phaseToRun = phaseToRun;
            return this;
        }

        public Builder phaseParams(Map<String, Object> phaseParams) {
            this.phaseParams = phaseParams;
            return this;
        }

        public Builder shouldSave(boolean shouldSave) {
            this.shouldSave = shouldSave;
            return this;
        }

        public Builder shouldAutoSave(boolean shouldAutoSave) {
            this.shouldAutoSave = shouldAutoSave;
            return this;
        }

        public Builder shouldSleep(boolean shouldSleep) {
            this.shouldSleep = shouldSleep;
            return this;
        }

        public Builder shouldRunOnClient(boolean shouldRunOnClient) {
            this.shouldRunOnClient = shouldRunOnClient;
            return this;
        }

        public Builder shouldAddAllHumans(boolean shouldAddAllHumans) {
            this.shouldAddAllHumans = shouldAddAllHumans;
            return this;
        }

        public Builder shouldOnlySendToWaitList(boolean shouldOnlySendToWaitList) {
            this.shouldOnlySendToWaitList = shouldOnlySendToWaitList;
            return this;
        }

        public Builder events(List<GameEvent> events) {
            this.events = events;
            return this;
        }

        public TableProcessResult build() {
            return new TableProcessResult(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TableProcessResult that = (TableProcessResult) o;
        return shouldSave == that.shouldSave && shouldAutoSave == that.shouldAutoSave && shouldSleep == that.shouldSleep
                && shouldRunOnClient == that.shouldRunOnClient && shouldAddAllHumans == that.shouldAddAllHumans
                && shouldOnlySendToWaitList == that.shouldOnlySendToWaitList && nextState == that.nextState
                && pendingState == that.pendingState && Objects.equals(phaseToRun, that.phaseToRun)
                && Objects.equals(phaseParams, that.phaseParams) && Objects.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextState, pendingState, phaseToRun, phaseParams, shouldSave, shouldAutoSave, shouldSleep,
                shouldRunOnClient, shouldAddAllHumans, shouldOnlySendToWaitList, events);
    }

    @Override
    public String toString() {
        return "TableProcessResult{" + "nextState=" + nextState + ", pendingState=" + pendingState + ", phaseToRun='"
                + phaseToRun + '\'' + ", shouldSave=" + shouldSave + ", shouldAutoSave=" + shouldAutoSave
                + ", shouldSleep=" + shouldSleep + ", shouldRunOnClient=" + shouldRunOnClient + ", shouldAddAllHumans="
                + shouldAddAllHumans + ", shouldOnlySendToWaitList=" + shouldOnlySendToWaitList + ", eventCount="
                + events.size() + '}';
    }
}
