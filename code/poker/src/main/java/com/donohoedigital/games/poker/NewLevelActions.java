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
 * NewLevelActions.java
 *
 * Created on January 26, 2005, 9:52 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.*;

import com.donohoedigital.base.*;

/**
 *
 * @author donohoe
 */
public class NewLevelActions extends ChainPhase implements CancelablePhase {

    /**
     * Callback installed by GameControlServer (dev build only) to intercept the
     * mid-tournament rebuy dialog and expose it as MODE_REBUY_CHECK input mode.
     * Null in normal interactive play; set during embedded server startup.
     */
    public interface RebuyDecisionProvider {
        /**
         * Called instead of the Swing dialog when a rebuy opportunity arises.
         *
         * @param setInputModeFn
         *            Runnable that sets inputMode = MODE_REBUY_CHECK
         * @param timeoutSeconds
         *            Seconds to wait before treating as declined
         * @return true if the player accepted the rebuy
         */
        boolean waitForDecision(Runnable setInputModeFn, int timeoutSeconds);
    }

    /** Set by GameControlServer on startup; null in normal interactive builds. */
    public static volatile RebuyDecisionProvider rebuyDecisionProvider;
    private PokerGame game_;
    private PokerDirector td_;
    private boolean bCanceled_ = false;

    public void process() {
        EngineUtils.addCancelable(this);

        game_ = (PokerGame) context_.getGame();
        ClientPokerTable table = game_.getCurrentTable();
        td_ = (PokerDirector) context_.getGameManager();

        ClientTournamentProfile profile = game_.getProfile();
        int nThisLevel = table.getLevel();
        int nNextLevel = nThisLevel + 1;

        if (!profile.isBreak(nThisLevel)) {
            // check last human rebuy
            ClientPlayer human = game_.getHumanPlayer();
            if (!bCanceled_ && nThisLevel == profile.getLastRebuyLevel() && table.isRebuyAllowed(human, nThisLevel)) {
                rebuy(game_, ShowTournamentTable.REBUY_LAST, nThisLevel);
            }
            PokerUtils.showComputerBuys(context_, game_, table.getRebuyList(),
                    nThisLevel == profile.getLastRebuyLevel() ? "rebuylast" : "rebuy");

            // check human add-on
            if (!bCanceled_ && table.isAddonAllowed(human)) {
                addon();
            }
            PokerUtils.showComputerBuys(context_, game_, table.getAddonList(), "addon");
        }

        // notify about new level
        if (!TESTING(PokerClientConstants.TESTING_AUTOPILOT)) {
            String sMsg;
            if (profile.isBreak(nNextLevel)) {
                sMsg = PropertyConfig.getMessage("msg.dialog.break", nNextLevel, profile.getMinutes(nNextLevel));
                EngineUtils.displayInformationDialog(context_, sMsg, "msg.windowtitle.break", "newbreak", "nobreak");
            } else {
                // Blind/ante values are server-authoritative and delivered via the
                // LEVEL_CHANGED WebSocket message. The client does not compute them.
                sMsg = PropertyConfig.getMessage("msg.dialog.next", nNextLevel, 0, 0, 0);
                EngineUtils.displayInformationDialog(context_, Utils.fixHtmlTextFor15(sMsg), "msg.windowtitle.level",
                        "newlevel", "nolevel");
            }
        }
    }

    /**
     * note cancelled (set flag so we don't display any more dialogs)
     */
    public void cancelPhase() {
        bCanceled_ = true;
    }

    /**
     * Next phase
     */
    public void nextPhase() {
        // remove cancel
        EngineUtils.removeCancelable(this);

        // don't bother removing wait list if cancelled
        if (bCanceled_)
            return;

        super.nextPhase();
    }

    /**
     * Rebuy - return true if player did rebuy
     */
    public static boolean rebuy(PokerGame game, int nType, int nLevel) {
        ClientPlayer player = game.getHumanPlayer();

        // just a safety check for case where rebuy is pressed/triggered
        // before it can be removed
        if (player.isObserver() || player.isEliminated())
            return false;

        ClientTournamentProfile prof = game.getProfile();
        PokerDirector td = (PokerDirector) game.getGameContext().getGameManager();
        int nCost = prof.getRebuyCost();
        int nChips = prof.getRebuyChips();
        boolean bPending = player.isInHand();

        // Control-server path: bypass the Swing dialog so the API can respond
        RebuyDecisionProvider provider = rebuyDecisionProvider;
        if (provider != null) {
            boolean accepted = provider.waitForDecision(() -> game.setInputMode(PokerTableInput.MODE_REBUY_CHECK), 30);
            if (accepted) {
                td.doRebuy(player, nLevel, nCost, nChips, bPending);
                return true;
            }
            return false;
        }

        String sPending = "";
        if (bPending)
            sPending = PropertyConfig.getMessage("msg.dorebuy.pending");

        String sMsg = PropertyConfig.getMessage("msg.dorebuy." + nType, nCost, nChips, prof.getLastRebuyLevel(),
                sPending);

        if (game.isOnlineGame() && PokerUtils.isOptionOn(PokerClientConstants.OPTION_ONLINE_AUDIO)) {
            AudioConfig.playFX("onlineact");
        }

        if (TESTING(PokerClientConstants.TESTING_AUTOPILOT) || EngineUtils.displayCancelableConfirmationDialog(
                game.getGameContext(), sMsg, "msg.windowtitle.rebuy", null, null, !game.isOnlineGame() ? 0 : 10)) {

            td.doRebuy(player, nLevel, nCost, nChips, bPending);
            return true;
        }

        return false;
    }

    /**
     * Addon
     */
    private void addon() {
        ClientTournamentProfile prof = game_.getProfile();
        PokerDirector td = (PokerDirector) context_.getGameManager();
        int nCost = prof.getAddonCost();
        int nChips = prof.getAddonChips();

        String sMsg = PropertyConfig.getMessage("msg.doaddon", nCost, nChips, prof.getAddonLevel());

        if (game_.isOnlineGame() && PokerUtils.isOptionOn(PokerClientConstants.OPTION_ONLINE_AUDIO)) {
            AudioConfig.playFX("onlineact");
        }

        if (TESTING(PokerClientConstants.TESTING_AUTOPILOT) || EngineUtils.displayCancelableConfirmationDialog(context_,
                sMsg, "msg.windowtitle.addon", null, null, !game_.isOnlineGame() ? 0 : 10)) {
            ClientPlayer p = game_.getHumanPlayer();
            td.doAddon(p, nCost, nChips);
        }
    }
}
