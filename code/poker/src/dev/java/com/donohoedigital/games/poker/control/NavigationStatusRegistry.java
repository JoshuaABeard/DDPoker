/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.control;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks durable navigation request/apply status for control-server observability.
 */
final class NavigationStatusRegistry {

    private static final Object lock = new Object();

    private static long nextRequestId = 0;
    private static long lastRequestId = 0;
    private static long lastAppliedRequestId = 0;

    private static String lastRequestedPhase = "NONE";
    private static long lastRequestedAtMs = 0;

    private static String pendingPhase = "NONE";
    private static boolean pending = false;

    private static String lastAppliedPhase = "NONE";
    private static long lastAppliedAtMs = 0;

    private static String lastError = "";
    private static long lastErrorAtMs = 0;

    private NavigationStatusRegistry() {
    }

    static long markRequested(String phase) {
        synchronized (lock) {
            long requestId = ++nextRequestId;
            lastRequestId = requestId;
            lastRequestedPhase = phase;
            lastRequestedAtMs = System.currentTimeMillis();
            pendingPhase = phase;
            pending = true;
            lastError = "";
            lastErrorAtMs = 0;
            return requestId;
        }
    }

    static void markApplied(String phase, long requestId) {
        synchronized (lock) {
            if (requestId < lastAppliedRequestId) {
                return;
            }
            lastAppliedRequestId = requestId;
            lastAppliedPhase = phase;
            lastAppliedAtMs = System.currentTimeMillis();
            if (requestId == lastRequestId) {
                pending = false;
                pendingPhase = "NONE";
            }
            lastError = "";
            lastErrorAtMs = 0;
        }
    }

    static void markFailed(String phase, long requestId, String errorMessage) {
        synchronized (lock) {
            if (requestId == lastRequestId) {
                pending = false;
                pendingPhase = "NONE";
            }
            lastError = String.format("%s: %s", phase, errorMessage == null ? "unknown error" : errorMessage);
            lastErrorAtMs = System.currentTimeMillis();
        }
    }

    static Map<String, Object> snapshot(String currentLifecyclePhase) {
        synchronized (lock) {
            if (pending && pendingPhase.equals(currentLifecyclePhase)) {
                markApplied(pendingPhase, lastRequestId);
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("lastRequestId", lastRequestId);
            map.put("lastRequestedPhase", lastRequestedPhase);
            map.put("lastRequestedAtMs", lastRequestedAtMs);
            map.put("pending", pending);
            map.put("pendingPhase", pending ? pendingPhase : "NONE");
            map.put("lastAppliedRequestId", lastAppliedRequestId);
            map.put("lastAppliedPhase", lastAppliedPhase);
            map.put("lastAppliedAtMs", lastAppliedAtMs);
            map.put("lastError", lastError);
            map.put("lastErrorAtMs", lastErrorAtMs);
            map.put("currentLifecyclePhase", currentLifecyclePhase);
            return map;
        }
    }
}
