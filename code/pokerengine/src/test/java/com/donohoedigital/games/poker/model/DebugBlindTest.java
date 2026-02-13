package com.donohoedigital.games.poker.model;

public class DebugBlindTest {
    public static void main(String[] args) {
        TournamentProfile profile = new TournamentProfile("Test");

        BlindTemplate.STANDARD.generateLevels(profile, 40, false, 0);

        System.out.println("Generated 40 levels");
        for (int i = 1; i <= 45; i++) {
            int small = profile.getSmallBlind(i);
            int big = profile.getBigBlind(i);
            if (big > 0) {
                System.out.println("Level " + i + ": " + small + "/" + big);
            }
        }

        System.out.println("Last level with blinds: " + profile.getLastLevel());
    }
}
