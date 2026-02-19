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
/*
 * CheckEndHand.java
 *
 * Created on February 20, 2004, 10:35 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.config.*;
import org.apache.logging.log4j.*;
import com.donohoedigital.base.*;

import java.util.*;

/**
 *
 * @author Doug Donohoe
 */
public class CheckEndHand extends ChainPhase {
    static Logger logger = LogManager.getLogger(CheckEndHand.class);

    private boolean bGameOver_ = false;
    private PokerGame game_;
    private PokerDirector td_;

    // -------------------------------------------------------------------------
    // Game-over result states (inlined from deleted GameOverChecker)
    // -------------------------------------------------------------------------

    private enum GameOverResult {
        CONTINUE, REBUY_OFFERED, GAME_OVER, TOURNAMENT_WON, NEVER_BROKE_ACTIVE
    }

    private static boolean isHumanBroke(PokerPlayer human) {
        return human.getChipCount() == 0 && !human.isObserver();
    }

    private static boolean shouldOfferRebuy(PokerPlayer human, PokerTable table) {
        return human.getChipCount() == 0 && !human.isObserver() && table.isRebuyAllowed(human);
    }

    private static GameOverResult checkGameOverStatus(PokerGame game, PokerPlayer human, PokerTable table,
            boolean neverBrokeCheatActive) {
        boolean bOnline = game.isOnlineGame();

        if (isHumanBroke(human)) {
            if (shouldOfferRebuy(human, table)) {
                return GameOverResult.REBUY_OFFERED;
            } else if (!bOnline) {
                if (neverBrokeCheatActive) {
                    return GameOverResult.NEVER_BROKE_ACTIVE;
                }
                return GameOverResult.GAME_OVER;
            }
        }

        if (game.isOnePlayerLeft()) {
            return GameOverResult.TOURNAMENT_WON;
        }

        return GameOverResult.CONTINUE;
    }

    private static int calculateNeverBrokeTransfer(int chipLeaderAmount, int tableMinChip) {
        int nAdd = chipLeaderAmount / 2;
        nAdd -= nAdd % tableMinChip;
        return nAdd;
    }

    /**
     * Move players to current table is someone was eliminated and check to see if
     * game is over
     */
    public void process() {
        game_ = (PokerGame) context_.getGame();
        td_ = (PokerDirector) context_.getGameManager();
        bGameOver_ = isGameOver(game_, true, td_);
    }

    /**
     * Static for testing from elsewhere
     */
    public static boolean isGameOver(PokerGame game, boolean bDoRebuyAndCleanup, PokerDirector td) {
        PokerPlayer human = game.getHumanPlayer();
        PokerTable table = game.getCurrentTable();

        boolean bGameOver = false;
        boolean bOnline = game.isOnlineGame();
        boolean neverBrokeCheatActive = PokerUtils.isCheatOn(game.getGameContext(),
                PokerConstants.OPTION_CHEAT_NEVERBROKE);

        // Determine game-over status
        GameOverResult result = checkGameOverStatus(game, human, table, neverBrokeCheatActive);

        // Handle each result with appropriate UI actions
        switch (result) {
            case REBUY_OFFERED :
                // Offer rebuy to human player
                if (bDoRebuyAndCleanup) {
                    if (!NewLevelActions.rebuy(game, ShowTournamentTable.REBUY_BROKE, table.getLevel())) {
                        // Double check in case of overlapping dialogs (rare)
                        if (isHumanBroke(human)) {
                            // Check if never-broke cheat should activate after rebuy declined
                            if (neverBrokeCheatActive && !bOnline) {
                                // Transfer chips from leader (same as NEVER_BROKE_ACTIVE case below)
                                List<PokerPlayer> rank = game.getPlayersByRank();
                                PokerPlayer lead = rank.get(0);
                                int nAdd = calculateNeverBrokeTransfer(lead.getChipCount(), table.getMinChip());
                                human.setChipCount(nAdd);
                                lead.setChipCount(lead.getChipCount() - nAdd);
                                if (!TESTING(PokerConstants.TESTING_AUTOPILOT)) {
                                    EngineUtils.displayInformationDialog(game.getGameContext(),
                                            PropertyConfig.getMessage("msg.neverbroke.info", nAdd,
                                                    Utils.encodeHTML(lead.getName()), lead.getTable().getName()),
                                            "msg.neverbroke.title", "neverbroke");
                                }
                                bGameOver = false;
                            } else {
                                bGameOver = true;
                            }
                        }
                    }
                }
                break;

            case GAME_OVER :
                bGameOver = true;
                break;

            case NEVER_BROKE_ACTIVE :
                // Transfer chips from leader to human
                if (bDoRebuyAndCleanup) {
                    List<PokerPlayer> rank = game.getPlayersByRank();
                    PokerPlayer lead = rank.get(0);
                    int nAdd = calculateNeverBrokeTransfer(lead.getChipCount(), table.getMinChip());
                    human.setChipCount(nAdd);
                    lead.setChipCount(lead.getChipCount() - nAdd);
                    if (!TESTING(PokerConstants.TESTING_AUTOPILOT)) {
                        EngineUtils.displayInformationDialog(
                                game.getGameContext(), PropertyConfig.getMessage("msg.neverbroke.info", nAdd,
                                        Utils.encodeHTML(lead.getName()), lead.getTable().getName()),
                                "msg.neverbroke.title", "neverbroke");
                    }
                }
                bGameOver = false;
                break;

            case TOURNAMENT_WON :
                bGameOver = true;
                break;

            case CONTINUE :
                // Game continues
                break;
        }

        // AI rebuys
        if (bDoRebuyAndCleanup && !bGameOver)
            PokerUtils.showComputerBuys(game.getGameContext(), game, table.getRebuyList(), "rebuy");

        // verify pot during testing
        if (bDoRebuyAndCleanup && DebugConfig.isTestingOn())
            game.verifyChipCount();

        return bGameOver;
    }

    /**
     * Goto the next phase
     */
    protected void nextPhase() {
        // practice - show modal dialog asking if the wish to watch
        // AI finish tournament or to quit
        if (bGameOver_ && !game_.isOnlineGame() && !game_.isOnePlayerLeft()) {
            // if they want to quit, set game over and process quit phase
            Phase phase = context_.processPhaseNow("PracticeGameOver", null);

            String sName = ((GameButton) phase.getResult()).getName();
            if (sName.startsWith("quit")) {
                td_.setGameOver();
                context_.processPhase("QuitGameNoConfirm");
                return;
            }
            // we handle this here so we can tell TournamentDirector that
            // the game is over
            else if (sName.startsWith("tryAgain")) {
                td_.setGameOver();
                TypedHashMap params = new TypedHashMap();
                params.setObject(RestartTournament.PARAM_PROFILE, game_.getProfile());
                context_.restart("RestartTournament", params);
                return;
            }
        }

        // otherwise, move along
        super.nextPhase();
    }
}
