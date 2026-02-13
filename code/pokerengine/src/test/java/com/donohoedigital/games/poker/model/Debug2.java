package com.donohoedigital.games.poker.model;

public class Debug2 {
    public static void main(String[] args) {
        TournamentProfile profile = new TournamentProfile("Test");
        BlindTemplate.SLOW.generateLevels(profile, 5, false, 0);

        for (int i = 1; i <= 5; i++) {
            System.out.println("Level " + i + ": " + profile.getAnte(i) + "/" + profile.getSmallBlind(i) + "/"
                    + profile.getBigBlind(i));
        }
    }
}
