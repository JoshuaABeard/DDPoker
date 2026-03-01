/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.dashboard;

import java.util.Map;

/**
 * Static holder for server-provided advisor data from ADVISOR_UPDATE WebSocket
 * messages. Separated from {@link DashboardAdvisor} to avoid class-loading
 * issues in tests (DashboardAdvisor has PropertyConfig static initializers).
 */
public class AdvisorState {

    private static volatile String currentAdvice_ = null;
    private static volatile String currentTitle_ = null;
    private static volatile double currentEquity_ = 0;
    private static volatile double currentPotOdds_ = 0;
    private static volatile Map<String, Double> currentImprovementOdds_ = null;
    private static volatile Double currentPositivePotential_ = null;
    private static volatile Double currentNegativePotential_ = null;

    public static String getCurrentAdvice() {
        return currentAdvice_;
    }

    public static String getCurrentTitle() {
        return currentTitle_;
    }

    public static Map<String, Double> getCurrentImprovementOdds() {
        return currentImprovementOdds_;
    }

    public static double getCurrentEquity() {
        return currentEquity_;
    }

    public static double getCurrentPotOdds() {
        return currentPotOdds_;
    }

    public static Double getCurrentPositivePotential() {
        return currentPositivePotential_;
    }

    public static Double getCurrentNegativePotential() {
        return currentNegativePotential_;
    }

    public static void setCurrentAdvice(String advice, String title) {
        currentAdvice_ = advice;
        currentTitle_ = title;
    }

    public static void setAdvisorData(double equity, double potOdds, Map<String, Double> improvementOdds,
            Double positivePotential, Double negativePotential) {
        currentEquity_ = equity;
        currentPotOdds_ = potOdds;
        currentImprovementOdds_ = improvementOdds;
        currentPositivePotential_ = positivePotential;
        currentNegativePotential_ = negativePotential;
    }

    public static void clear() {
        currentAdvice_ = null;
        currentTitle_ = null;
        currentEquity_ = 0;
        currentPotOdds_ = 0;
        currentImprovementOdds_ = null;
        currentPositivePotential_ = null;
        currentNegativePotential_ = null;
    }

    private AdvisorState() {
    }
}
