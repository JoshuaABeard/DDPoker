/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;

/**
 * Server-side poker hand implementation. Implements GameHand without Swing
 * dependencies. Replaces HoldemHand for server-side game hosting.
 *
 * This is the core poker hand state machine handling: - Dealing mechanics -
 * Blind/ante posting - Action processing (fold/check/call/bet/raise) - Pot
 * management and side pot calculation - Showdown resolution - GameHand
 * interface for AI integration
 */
public class ServerHand implements GameHand {
    private static final Logger logger = LoggerFactory.getLogger(ServerHand.class);
    private static final int NO_CURRENT_PLAYER = -1;

    // Table reference
    private final MockTable table;

    // Deck and cards
    private final ServerDeck deck;
    private final List<Card> community = new ArrayList<>();
    private final Map<Integer, List<Card>> playerHands = new HashMap<>(); // playerId -> hole cards

    // Betting state
    private BettingRound round = BettingRound.NONE;
    private final List<ServerPlayer> playerOrder = new ArrayList<>();
    private int currentPlayerIndex = -1;

    // Pots
    private final List<ServerPot> pots = new ArrayList<>();
    private int currentBet = 0; // Current bet level this round
    private final Map<Integer, Integer> playerBets = new HashMap<>(); // playerId -> total bet this round

    // Action history
    private final List<ServerHandAction> history = new ArrayList<>();

    // Blinds/antes for this hand
    private final int smallBlindAmount;
    private final int bigBlindAmount;
    private final int anteAmount;
    private final int smallBlindSeat;
    private final int bigBlindSeat;
    private final int handNumber;

    // Status tracking
    private boolean done;
    private boolean allInShowdown;
    private int potStatus; // NO_POT_ACTION, CALLED_POT, RAISED_POT, RERAISED_POT

    // Resolution
    private List<ServerPlayer> preWinners;
    private List<ServerPlayer> preLosers;

    /**
     * Create a new server hand.
     *
     * @param table
     *            the game table
     * @param handNumber
     *            hand sequence number
     * @param smallBlind
     *            small blind amount
     * @param bigBlind
     *            big blind amount
     * @param ante
     *            ante amount
     * @param button
     *            dealer button seat
     * @param sbSeat
     *            small blind seat
     * @param bbSeat
     *            big blind seat
     */
    public ServerHand(MockTable table, int handNumber, int smallBlind, int bigBlind, int ante, int button, int sbSeat,
            int bbSeat) {
        this.table = table;
        this.handNumber = handNumber;
        this.smallBlindAmount = smallBlind;
        this.bigBlindAmount = bigBlind;
        this.anteAmount = ante;
        this.smallBlindSeat = sbSeat;
        this.bigBlindSeat = bbSeat;
        this.deck = new ServerDeck(); // Auto-shuffled
        this.done = false;
    }

    /**
     * Deal cards and post blinds/antes.
     */
    public void deal() {
        logger.debug("[ServerHand] deal() handNumber={} seats={}", handNumber, table.getNumSeats());
        // Reset per-hand player state (folded/allIn carry over from previous hand
        // on the same ServerPlayer objects; clear them before starting a new hand)
        MockTable t = table;
        for (int seat = 0; seat < t.getNumSeats(); seat++) {
            ServerPlayer player = t.getPlayer(seat);
            if (player != null) {
                player.setFolded(false);
                player.setAllIn(false);
            }
        }

        // Initialize player order (all active players in seat order)
        initializePlayerOrder();

        // Post antes first
        if (anteAmount > 0) {
            postAntes();
        }

        // Post blinds
        postBlinds();

        // Note: antes/blinds stay in playerBets until advanceRound() calls calcPots()
        // This keeps the architecture simple: playerBets → calcPots() → pots

        // Deal 2 cards to each player
        dealHoleCards();

        // Set initial betting round
        round = BettingRound.PRE_FLOP;
    }

    /**
     * Initialize player order with all active players in seat order starting from
     * button.
     */
    private void initializePlayerOrder() {
        MockTable t = table;
        int button = t.getButton();
        int numSeats = t.getNumSeats();

        playerOrder.clear();

        // Add players starting from button
        for (int i = 0; i < numSeats; i++) {
            int seat = (button + i) % numSeats;
            ServerPlayer player = t.getPlayer(seat);
            if (player != null && !player.isSittingOut()) {
                playerOrder.add(player);
            }
        }
    }

    private void postAntes() {
        MockTable t = table;
        for (int seat = 0; seat < t.getNumSeats(); seat++) {
            ServerPlayer player = t.getPlayer(seat);
            if (player != null && !player.isSittingOut()) {
                int actualAmount = Math.min(anteAmount, player.getChipCount());
                player.subtractChips(actualAmount);
                // Track in playerBets - will be added to pots by calcPots()
                playerBets.put(player.getID(), playerBets.getOrDefault(player.getID(), 0) + actualAmount);
                if (actualAmount < anteAmount) {
                    player.setAllIn(true);
                }
            }
        }
    }

    private void postBlinds() {
        MockTable t = table;

        // Small blind
        ServerPlayer sb = t.getPlayer(smallBlindSeat);
        if (sb != null) {
            int actualSB = Math.min(smallBlindAmount, sb.getChipCount());
            sb.subtractChips(actualSB);
            // Track in playerBets - will be added to pots by calcPots()
            playerBets.put(sb.getID(), playerBets.getOrDefault(sb.getID(), 0) + actualSB);
            if (actualSB < smallBlindAmount) {
                sb.setAllIn(true);
            }
        }

        // Big blind
        ServerPlayer bb = t.getPlayer(bigBlindSeat);
        if (bb != null) {
            int actualBB = Math.min(bigBlindAmount, bb.getChipCount());
            bb.subtractChips(actualBB);
            // Track in playerBets - will be added to pots by calcPots()
            playerBets.put(bb.getID(), playerBets.getOrDefault(bb.getID(), 0) + actualBB);
            if (actualBB < bigBlindAmount) {
                bb.setAllIn(true);
            }
            currentBet = bigBlindAmount;
        }
    }

    private void dealHoleCards() {
        MockTable t = table;
        for (int seat = 0; seat < t.getNumSeats(); seat++) {
            ServerPlayer player = t.getPlayer(seat);
            if (player != null && !player.isSittingOut()) {
                List<Card> hand = new ArrayList<>(2);
                hand.add(deck.nextCard());
                hand.add(deck.nextCard());
                playerHands.put(player.getID(), hand);
            }
        }
    }

    private void addToPot(ServerPlayer player, int amount) {
        // Treat NONE as PRE_FLOP for pot tracking (antes/blinds are part of preflop)
        int potRound = (round == BettingRound.NONE) ? BettingRound.PRE_FLOP.toLegacy() : round.toLegacy();

        // Find or create pot for current round
        ServerPot currentRoundPot = null;
        for (ServerPot pot : pots) {
            if (pot.getRound() == potRound && pot.getSideBet() == 0) {
                currentRoundPot = pot;
                break;
            }
        }

        if (currentRoundPot == null) {
            currentRoundPot = new ServerPot(potRound, 0);
            pots.add(currentRoundPot);
        }

        // Add to main pot for current round - calcPots() will redistribute into side
        // pots as needed
        currentRoundPot.addChips(player, amount);
    }

    /**
     * Calculate side pots for all-in situations. Called when betting round
     * completes. Moves chips from playerBets into pots.
     */
    private void calcPots() {
        // Create pot info for each player
        List<PotInfo> info = new ArrayList<>();
        MockTable t = table;

        for (int seat = 0; seat < t.getNumSeats(); seat++) {
            ServerPlayer player = t.getPlayer(seat);
            if (player != null) {
                int playerBet = playerBets.getOrDefault(player.getID(), 0);
                info.add(new PotInfo(player, playerBet));
            }
        }

        // Sort by bet amount (players all-in for less go first)
        Collections.sort(info);

        // Reset pots for this round and add side pots as needed
        int lastSideBet = 0;
        ServerPot mainPot = resetMainPotForRound();

        for (int i = 0; i < info.size(); i++) {
            PotInfo potInfo = info.get(i);
            if (potInfo.needsSidePot() && potInfo.bet > lastSideBet) {
                ServerPot sidePot = new ServerPot(round.toLegacy(), potInfo.bet);
                pots.add(sidePot);
                lastSideBet = potInfo.bet;
            }
        }

        // Distribute chips from each player into appropriate pots
        for (ServerPot pot : pots) {
            if (pot.getRound() != round.toLegacy())
                continue;

            int sideBet = pot.getSideBet();
            for (PotInfo potInfo : info) {
                if (potInfo.bet == 0)
                    continue;

                if (sideBet == 0) {
                    // Main pot - take all remaining chips
                    pot.addChips(potInfo.player, potInfo.bet);
                    potInfo.bet = 0;
                } else {
                    // Side pot - take up to sideBet amount
                    int contribution = Math.min(sideBet, potInfo.bet);
                    pot.addChips(potInfo.player, contribution);
                    potInfo.bet -= contribution;
                }
            }
        }
    }

    /**
     * Reset main pot for current round, removing side pots from previous
     * calculations.
     */
    private ServerPot resetMainPotForRound() {
        ServerPot mainPot = null;
        for (int i = pots.size() - 1; i >= 0; i--) {
            ServerPot pot = pots.get(i);
            if (pot.getRound() != round.toLegacy()) {
                break;
            }
            if (mainPot != null) {
                pots.remove(mainPot);
            }
            mainPot = pot;
        }

        if (mainPot == null) {
            mainPot = new ServerPot(round.toLegacy(), 0);
            pots.add(mainPot);
        } else {
            mainPot.reset();
        }
        return mainPot;
    }

    /**
     * Helper class for side pot calculation. Tracks each player's bet and whether
     * they need a side pot.
     */
    private class PotInfo implements Comparable<PotInfo> {
        ServerPlayer player;
        int bet;
        boolean needsSide;

        PotInfo(ServerPlayer player, int bet) {
            this.player = player;
            this.bet = bet;
            this.needsSide = bet < currentBet && player.isAllIn();
        }

        boolean needsSidePot() {
            return needsSide;
        }

        @Override
        public int compareTo(PotInfo other) {
            // Players needing side pots come first, sorted by bet amount
            if (needsSidePot()) {
                if (other.needsSidePot()) {
                    return Integer.compare(bet, other.bet);
                } else {
                    return -1;
                }
            } else if (other.needsSidePot()) {
                return 1;
            } else {
                return Integer.compare(bet, other.bet);
            }
        }
    }

    @Override
    public BettingRound getRound() {
        return round;
    }

    @Override
    public void setRound(BettingRound round) {
        BettingRound oldRound = this.round;
        this.round = round;

        // If advancing rounds, deal appropriate community cards
        if (oldRound.toLegacy() < round.toLegacy()) {
            // Deal missing community cards when jumping to a later round
            if (round == BettingRound.FLOP && community.isEmpty()) {
                dealFlop();
            } else if (round == BettingRound.TURN && community.size() < 4) {
                if (community.isEmpty()) {
                    dealFlop();
                }
                if (community.size() == 3) {
                    dealTurn();
                }
            } else if (round == BettingRound.RIVER && community.size() < 5) {
                if (community.isEmpty()) {
                    dealFlop();
                }
                if (community.size() == 3) {
                    dealTurn();
                }
                if (community.size() == 4) {
                    dealRiver();
                }
            }
        }

        // Reset betting for new round
        currentBet = 0;
        playerBets.clear();
    }

    @Override
    public boolean isDone() {
        if (done)
            return true;

        // Hand is done if only one player remains (uncontested)
        if (isUncontested())
            return true;

        // Count players that have acted
        MockTable t = table;
        int playersToAct = 0;
        int playersActed = 0;
        int allInCount = 0;

        for (int seat = 0; seat < t.getNumSeats(); seat++) {
            ServerPlayer player = t.getPlayer(seat);
            if (player == null || player.isFolded())
                continue;

            // Increment players that have to act (anyone still in hand)
            playersToAct++;

            // If they have acted, bump counter
            if (hasActedThisRound(player)) {
                playersActed++;
            }
            // Count all-in
            else if (player.isAllIn()) {
                // If player went all-in on blind/ante in preflop, mark as acted
                if (round == BettingRound.PRE_FLOP) {
                    playersActed++;
                } else {
                    allInCount++;
                }
            }
            // If no call needed and can't raise, mark as acted
            else if (getAmountToCall(player) == 0 && player.getChipCount() == 0) {
                playersActed++;
            }
        }

        // If only one player left who can bet (everyone else all in), and no one acted
        // yet, done
        if (playersActed == 0 && getNumWithChips() <= 1)
            return true;

        // All players must have acted (or be all-in)
        if (playersToAct != (playersActed + allInCount))
            return false;

        // Not done if pot not good (players may have all acted, but still have raises
        // to call)
        return isPotGood();
    }

    /**
     * Is pot good? Have all players matched the current bet or gone all-in?
     */
    private boolean isPotGood() {
        MockTable t = table;
        int highestBet = getCurrentBet();

        for (int seat = 0; seat < t.getNumSeats(); seat++) {
            ServerPlayer player = t.getPlayer(seat);
            if (player == null || player.isFolded() || player.isAllIn())
                continue;

            int playerBet = playerBets.getOrDefault(player.getID(), 0);
            if (playerBet != highestBet)
                return false;
        }
        return true;
    }

    /**
     * Get number of players with chips left to bet (and still in hand).
     */
    private int getNumWithChips() {
        MockTable t = table;
        int count = 0;
        for (int seat = 0; seat < t.getNumSeats(); seat++) {
            ServerPlayer player = t.getPlayer(seat);
            if (player != null && !player.isFolded() && !player.isAllIn()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get current highest bet for this round.
     */
    private int getCurrentBet() {
        return currentBet;
    }

    @Override
    public int getNumWithCards() {
        MockTable t = table;
        int count = 0;
        for (int seat = 0; seat < t.getNumSeats(); seat++) {
            ServerPlayer player = t.getPlayer(seat);
            if (player != null && !player.isFolded() && playerHands.containsKey(player.getID())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getCurrentPlayerInitIndex() {
        return currentPlayerIndex;
    }

    @Override
    public void advanceRound() {
        logger.debug("[ServerHand] advanceRound() currentRound={}", round);
        // Calculate pots for the round that just completed BEFORE advancing round
        // This creates side pots for all-in situations based on current round
        if (!playerBets.isEmpty()) {
            calcPots();
        }

        // Reset betting for new round
        currentBet = 0;
        playerBets.clear();

        // Advance to next round and deal community cards
        switch (round) {
            case NONE :
            case PRE_FLOP :
                round = BettingRound.FLOP;
                dealFlop();
                break;
            case FLOP :
                round = BettingRound.TURN;
                dealTurn();
                break;
            case TURN :
                round = BettingRound.RIVER;
                dealRiver();
                break;
            case RIVER :
                round = BettingRound.SHOWDOWN;
                break;
            default :
                break;
        }
    }

    private void dealFlop() {
        burn();
        community.add(deck.nextCard());
        community.add(deck.nextCard());
        community.add(deck.nextCard());
    }

    private void dealTurn() {
        burn();
        community.add(deck.nextCard());
    }

    private void dealRiver() {
        burn();
        community.add(deck.nextCard());
    }

    private void burn() {
        deck.nextCard(); // Burn a card (discard)
    }

    @Override
    public void preResolve(boolean isOnline) {
        preWinners = new ArrayList<>();
        preLosers = new ArrayList<>();

        if (!isOnline) {
            return;
        }

        // Pre-evaluate each pot to determine winners/losers
        for (ServerPot pot : pots) {
            List<ServerPlayer> eligiblePlayers = pot.getEligiblePlayers();

            if (pot.isOverbet()) {
                // Overbet - player gets their money back, not really a win
                continue;
            }

            // Evaluate hands
            int highScore = 0;
            List<ServerPlayer> potWinners = new ArrayList<>();

            for (ServerPlayer player : eligiblePlayers) {
                if (player.isFolded()) {
                    continue;
                }

                HandEvaluationResult result = evaluateHand(player);

                if (result.score > highScore) {
                    highScore = result.score;
                    potWinners.clear();
                    potWinners.add(player);
                } else if (result.score == highScore) {
                    potWinners.add(player);
                }
            }

            // Add to pre-winners/losers lists
            for (ServerPlayer player : eligiblePlayers) {
                if (!player.isFolded()) {
                    if (potWinners.contains(player)) {
                        if (!preWinners.contains(player)) {
                            preWinners.add(player);
                        }
                    } else {
                        if (!preLosers.contains(player)) {
                            preLosers.add(player);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void resolve() {
        logger.debug("[ServerHand] resolve() numWithCards={} pots={}", getNumWithCards(), pots.size());
        done = true;

        // Calculate final pots from remaining bets before resolving
        if (!playerBets.isEmpty()) {
            calcPots();
        }

        // Resolve each pot
        for (int i = 0; i < pots.size(); i++) {
            resolvePot(i);
        }
    }

    /**
     * Resolve a single pot, determining winners and distributing chips.
     */
    private void resolvePot(int potIndex) {
        ServerPot pot = pots.get(potIndex);
        List<ServerPlayer> eligiblePlayers = pot.getEligiblePlayers();

        // Handle overbet (uncalled bet) - return to the bettor
        if (pot.isOverbet()) {
            ServerPlayer player = eligiblePlayers.get(0);
            player.addChips(pot.getChips());
            pot.addWinner(player);
            pot.reset(); // Clear pot after distribution
            return;
        }

        // Find non-folded players
        List<ServerPlayer> nonFoldedPlayers = new ArrayList<>();
        for (ServerPlayer player : eligiblePlayers) {
            if (!player.isFolded()) {
                nonFoldedPlayers.add(player);
            }
        }

        // If only one non-folded player, they win uncontested
        List<ServerPlayer> winners = new ArrayList<>();
        if (nonFoldedPlayers.size() == 1) {
            ServerPlayer winner = nonFoldedPlayers.get(0);
            winners.add(winner);
            pot.addWinner(winner);
        } else if (nonFoldedPlayers.size() == 0) {
            // All eligible players folded - give pot to last player standing in hand
            // This handles cases where BB wins when everyone folds before them
            MockTable t = table;
            for (int seat = 0; seat < t.getNumSeats(); seat++) {
                ServerPlayer player = t.getPlayer(seat);
                if (player != null && !player.isFolded()) {
                    winners.add(player);
                    pot.addWinner(player);
                    break; // Only one non-folded player should exist
                }
            }
        } else if (nonFoldedPlayers.size() > 1) {
            // Evaluate hands for showdown
            HandEvaluationResult best = null;
            List<HandEvaluationResult> allResults = new ArrayList<>();

            for (ServerPlayer player : nonFoldedPlayers) {
                HandEvaluationResult result = evaluateHand(player);
                allResults.add(result);

                if (best == null || result.score > best.score) {
                    best = result;
                }
            }

            // Find all winners (players with best score)
            if (best != null) {
                for (HandEvaluationResult result : allResults) {
                    if (result.score == best.score) {
                        winners.add(result.player);
                        pot.addWinner(result.player);
                    }
                }
            }
        }
        // else: all players folded - should not happen, but pot stays unclaimed

        // Distribute pot to winners
        if (!winners.isEmpty()) {
            int potAmount = pot.getChips();
            int share = potAmount / winners.size();
            int remainder = potAmount % winners.size();

            for (int i = 0; i < winners.size(); i++) {
                ServerPlayer winner = winners.get(i);
                int amount = share;
                // First winner gets odd chips
                if (i == 0) {
                    amount += remainder;
                }
                winner.addChips(amount);
            }
        }

        // Clear pot after distribution to prevent double-counting
        pot.reset();
    }

    /**
     * Evaluate a player's hand using server-native hand evaluator.
     */
    private HandEvaluationResult evaluateHand(ServerPlayer player) {
        List<Card> playerCards = playerHands.get(player.getID());
        ServerHandEvaluator evaluator = new ServerHandEvaluator();
        int score = evaluator.getScore(playerCards, community);
        return new HandEvaluationResult(player, score);
    }

    /**
     * Helper class for hand evaluation results.
     */
    private static class HandEvaluationResult {
        final ServerPlayer player;
        final int score;

        HandEvaluationResult(ServerPlayer player, int score) {
            this.player = player;
            this.score = score;
        }
    }

    @Override
    public void storeHandHistory() {
        // Server uses event store instead of database hand history
    }

    @Override
    public List<GamePlayerInfo> getPreWinners() {
        return preWinners != null ? new ArrayList<>(preWinners) : new ArrayList<>();
    }

    @Override
    public List<GamePlayerInfo> getPreLosers() {
        return preLosers != null ? new ArrayList<>(preLosers) : new ArrayList<>();
    }

    @Override
    public boolean isUncontested() {
        return getNumWithCards() == 1;
    }

    @Override
    public GamePlayerInfo getCurrentPlayerWithInit() {
        GamePlayerInfo current = getCurrentPlayer();

        // If current player is not defined, initialize player order
        if (current == null) {
            initPlayerIndex();
            current = getCurrentPlayer();
        }

        return current;
    }

    /**
     * Get current player without initialization.
     */
    private GamePlayerInfo getCurrentPlayer() {
        if (currentPlayerIndex == NO_CURRENT_PLAYER || currentPlayerIndex >= playerOrder.size()) {
            return null;
        }
        return playerOrder.get(currentPlayerIndex);
    }

    /**
     * Initialize player index to start of betting round.
     */
    private void initPlayerIndex() {
        // Start at -1, then playerActed will find first active player
        playerActed(-1);
    }

    /**
     * Advance to next active player after current index.
     */
    private void playerActed() {
        playerActed(currentPlayerIndex);
    }

    /**
     * Find next active player starting from given index.
     */
    private void playerActed(int startIndex) {
        if (isDone()) {
            currentPlayerIndex = NO_CURRENT_PLAYER;
            return;
        }

        int index = startIndex;
        boolean found = false;

        // Loop through players to find next active one
        while (!found) {
            index++;
            if (index >= playerOrder.size()) {
                index = 0;
            }

            ServerPlayer player = playerOrder.get(index);

            // Skip folded and all-in players
            if (!player.isFolded() && !player.isAllIn()) {
                found = true;
            }

            // Prevent infinite loop if all players folded/all-in
            if (index == startIndex) {
                break;
            }
        }

        currentPlayerIndex = found ? index : NO_CURRENT_PLAYER;
    }

    @Override
    public int getAmountToCall(GamePlayerInfo player) {
        int playerBet = playerBets.getOrDefault(player.getID(), 0);
        return Math.max(0, currentBet - playerBet);
    }

    @Override
    public int getMinBet() {
        return bigBlindAmount;
    }

    @Override
    public int getMinRaise() {
        // Minimum raise is at least the size of the big blind, or the size of the last
        // raise
        return currentBet + bigBlindAmount;
    }

    @Override
    public void applyPlayerAction(GamePlayerInfo player, PlayerAction action) {
        ServerPlayer sp = (ServerPlayer) player;
        int chipsBefore = sp.getChipCount();
        logger.debug("[ServerHand] applyPlayerAction player={} action={} amount={} chipsBefore={}", sp.getName(),
                action.actionType(), action.amount(), chipsBefore);

        switch (action.actionType()) {
            case FOLD :
                sp.setFolded(true);
                history.add(new ServerHandAction(sp, round.toLegacy(), ServerHandAction.ACTION_FOLD, 0, 0, false));
                break;

            case CHECK :
                history.add(new ServerHandAction(sp, round.toLegacy(), ServerHandAction.ACTION_CHECK, 0, 0, false));
                break;

            case CALL :
                int callAmount = getAmountToCall(player);
                // All-in protection: can't bet more than player has
                int actualCall = Math.min(callAmount, sp.getChipCount());
                sp.subtractChips(actualCall);
                // Track in playerBets - will be added to pots by calcPots()
                playerBets.put(sp.getID(), playerBets.getOrDefault(sp.getID(), 0) + actualCall);
                history.add(new ServerHandAction(sp, round.toLegacy(), ServerHandAction.ACTION_CALL, actualCall, 0,
                        sp.getChipCount() == 0));
                break;

            case BET :
                int betAmount = action.amount();
                // All-in protection: can't bet more than player has
                int actualBet = Math.min(betAmount, sp.getChipCount());
                sp.subtractChips(actualBet);
                // Track in playerBets - will be added to pots by calcPots()
                playerBets.put(sp.getID(), playerBets.getOrDefault(sp.getID(), 0) + actualBet);
                currentBet = actualBet; // Current bet is what player actually bet
                history.add(new ServerHandAction(sp, round.toLegacy(), ServerHandAction.ACTION_BET, actualBet, 0,
                        sp.getChipCount() == 0));
                break;

            case RAISE :
                int raiseAmount = action.amount();
                // All-in protection: can't raise more than player has
                int actualRaise = Math.min(raiseAmount, sp.getChipCount());
                sp.subtractChips(actualRaise);
                // Track in playerBets - will be added to pots by calcPots()
                playerBets.put(sp.getID(), playerBets.getOrDefault(sp.getID(), 0) + actualRaise);
                currentBet = actualRaise; // Current bet is what player actually raised to
                history.add(new ServerHandAction(sp, round.toLegacy(), ServerHandAction.ACTION_RAISE, actualRaise, 0,
                        sp.getChipCount() == 0));
                break;

            default :
                break;
        }

        // Check if all-in
        if (sp.getChipCount() == 0) {
            sp.setAllIn(true);
        }
        logger.debug("[ServerHand] applyPlayerAction result player={} chipsAfter={} allIn={}", sp.getName(),
                sp.getChipCount(), sp.isAllIn());

        // Advance to next player
        playerActed();
    }

    @Override
    public Card[] getCommunityCards() {
        if (community.isEmpty())
            return null;
        return community.toArray(new Card[0]);
    }

    @Override
    public Card[] getPlayerCards(GamePlayerInfo player) {
        List<Card> cards = playerHands.get(player.getID());
        if (cards == null || cards.isEmpty())
            return null;
        return cards.toArray(new Card[0]);
    }

    @Override
    public int getPotSize() {
        // Sum pot chips (from previous calcPots() calls) + current round bets
        // (playerBets)
        int potTotal = 0;
        for (ServerPot pot : pots) {
            potTotal += pot.getChips();
        }

        // Add pending bets for current betting round (not yet moved to pots)
        int playerBetsTotal = playerBets.values().stream().mapToInt(Integer::intValue).sum();

        return potTotal + playerBetsTotal;
    }

    @Override
    public int getPotStatus() {
        return potStatus;
    }

    @Override
    public float getPotOdds(GamePlayerInfo player) {
        int callAmount = getAmountToCall(player);
        if (callAmount == 0)
            return 0;
        return (float) getPotSize() / callAmount;
    }

    // === Betting History ===

    @Override
    public boolean wasRaisedPreFlop() {
        for (ServerHandAction action : history) {
            if (action.round() == BettingRound.PRE_FLOP.toLegacy()) {
                if (action.action() == ServerHandAction.ACTION_RAISE) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public GamePlayerInfo getFirstBettor(int round, boolean includeRaises) {
        for (ServerHandAction action : history) {
            if (action.round() == round) {
                int actionType = action.action();
                if (actionType == ServerHandAction.ACTION_BET) {
                    return action.player();
                }
                if (includeRaises && actionType == ServerHandAction.ACTION_RAISE) {
                    return action.player();
                }
            }
        }
        return null;
    }

    @Override
    public GamePlayerInfo getLastBettor(int round, boolean includeRaises) {
        GamePlayerInfo lastBettor = null;
        for (ServerHandAction action : history) {
            if (action.round() == round) {
                int actionType = action.action();
                if (actionType == ServerHandAction.ACTION_BET) {
                    lastBettor = action.player();
                }
                if (includeRaises && actionType == ServerHandAction.ACTION_RAISE) {
                    lastBettor = action.player();
                }
            }
        }
        return lastBettor;
    }

    @Override
    public boolean wasFirstRaiserPreFlop(GamePlayerInfo player) {
        for (ServerHandAction action : history) {
            if (action.round() == BettingRound.PRE_FLOP.toLegacy()) {
                if (action.action() == ServerHandAction.ACTION_RAISE) {
                    return action.player().getID() == player.getID();
                }
            }
        }
        return false;
    }

    @Override
    public boolean wasLastRaiserPreFlop(GamePlayerInfo player) {
        ServerHandAction lastRaiser = null;
        for (ServerHandAction action : history) {
            if (action.round() == BettingRound.PRE_FLOP.toLegacy()) {
                if (action.action() == ServerHandAction.ACTION_RAISE) {
                    lastRaiser = action;
                }
            }
        }
        return lastRaiser != null && lastRaiser.player().getID() == player.getID();
    }

    @Override
    public boolean wasOnlyRaiserPreFlop(GamePlayerInfo player) {
        int raiseCount = 0;
        boolean playerRaised = false;

        for (ServerHandAction action : history) {
            if (action.round() == BettingRound.PRE_FLOP.toLegacy()) {
                if (action.action() == ServerHandAction.ACTION_RAISE) {
                    raiseCount++;
                    if (action.player().getID() == player.getID()) {
                        playerRaised = true;
                    }
                }
            }
        }

        return playerRaised && raiseCount == 1;
    }

    @Override
    public boolean wasPotAction(int round) {
        for (ServerHandAction action : history) {
            if (action.round() == round) {
                int actionType = action.action();
                // Any action besides fold means there was pot action
                if (actionType != ServerHandAction.ACTION_FOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean paidToPlay(GamePlayerInfo player) {
        // Check if player put any chips in (beyond blinds/antes)
        for (ServerHandAction action : history) {
            if (action.player().getID() == player.getID()) {
                int actionType = action.action();
                if (actionType == ServerHandAction.ACTION_CALL || actionType == ServerHandAction.ACTION_BET
                        || actionType == ServerHandAction.ACTION_RAISE) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean couldLimp(GamePlayerInfo player) {
        // Player could limp if they were not a blind and preflop wasn't raised before
        // their turn
        if (isBlind(player)) {
            return false;
        }

        // Check if there was a raise before this player acted
        for (ServerHandAction action : history) {
            if (action.round() == BettingRound.PRE_FLOP.toLegacy()) {
                if (action.player().getID() == player.getID()) {
                    return false; // Already acted
                }
                if (action.action() == ServerHandAction.ACTION_RAISE) {
                    return false; // Raise before player's turn
                }
            }
        }

        return true;
    }

    @Override
    public boolean limped(GamePlayerInfo player) {
        // Limped = called the big blind preflop without a raise
        for (ServerHandAction action : history) {
            if (action.round() == BettingRound.PRE_FLOP.toLegacy()) {
                if (action.player().getID() == player.getID()) {
                    return action.action() == ServerHandAction.ACTION_CALL && action.amount() == bigBlindAmount;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isBlind(GamePlayerInfo player) {
        return player.getSeat() == smallBlindSeat || player.getSeat() == bigBlindSeat;
    }

    @Override
    public boolean hasActedThisRound(GamePlayerInfo player) {
        // Check if player has any action in current round (excluding antes/blinds)
        int currentRoundValue = round.toLegacy();
        for (ServerHandAction action : history) {
            if (action.player().getID() == player.getID() && action.round() == currentRoundValue) {
                int actionType = action.action();
                // Exclude antes and blinds - only voluntary actions count
                if (actionType != ServerHandAction.ACTION_FOLD && actionType >= ServerHandAction.ACTION_CHECK) {
                    return true;
                }
                if (actionType == ServerHandAction.ACTION_FOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getLastActionThisRound(GamePlayerInfo player) {
        int currentRoundValue = round.toLegacy();
        for (int i = history.size() - 1; i >= 0; i--) {
            ServerHandAction action = history.get(i);
            if (action.player().getID() == player.getID() && action.round() == currentRoundValue) {
                return action.action();
            }
        }
        return ServerHandAction.ACTION_NONE;
    }

    @Override
    public int getFirstVoluntaryAction(GamePlayerInfo player, int round) {
        for (ServerHandAction action : history) {
            if (action.player().getID() == player.getID() && action.round() == round) {
                int actionType = action.action();
                // Skip blinds/antes - only voluntary actions
                if (actionType >= ServerHandAction.ACTION_FOLD) {
                    return actionType;
                }
            }
        }
        return ServerHandAction.ACTION_NONE;
    }

    @Override
    public int getNumLimpers() {
        int limperCount = 0;
        for (ServerHandAction action : history) {
            if (action.round() == BettingRound.PRE_FLOP.toLegacy()) {
                if (action.action() == ServerHandAction.ACTION_CALL && action.amount() == bigBlindAmount) {
                    limperCount++;
                }
            }
        }
        return limperCount;
    }

    @Override
    public int getNumFoldsSinceLastBet() {
        int foldCount = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            ServerHandAction action = history.get(i);
            int actionType = action.action();

            if (actionType == ServerHandAction.ACTION_BET || actionType == ServerHandAction.ACTION_RAISE) {
                break; // Stop at last bet/raise
            }

            if (actionType == ServerHandAction.ACTION_FOLD) {
                foldCount++;
            }
        }
        return foldCount;
    }

    /**
     * Get the pots for projection.
     *
     * @return list of all pots
     */
    public List<ServerPot> getPots() {
        return pots;
    }

    /**
     * Get the total amount bet by a specific player in the current betting round.
     *
     * @param playerId
     *            the player ID to look up
     * @return the player's total bet this round, or 0 if they haven't bet
     */
    public int getPlayerBet(int playerId) {
        return playerBets.getOrDefault(playerId, 0);
    }

    /**
     * Get the total of all pending bets for the current round (chips that have been
     * placed but not yet moved to a pot via calcPots).
     *
     * @return total pending bet amount across all players
     */
    public int getPendingBetTotal() {
        return playerBets.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Minimal table interface required by ServerHand.
     * <p>
     * ServerGameTable implements this interface to provide hand logic with access
     * to player and button information without coupling to the full GameTable API.
     */
    interface MockTable {
        ServerPlayer getPlayer(int seat);

        int getNumSeats();

        int getButton();
    }
}
