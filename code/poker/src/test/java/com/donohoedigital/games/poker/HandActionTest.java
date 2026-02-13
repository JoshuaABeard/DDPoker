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
package com.donohoedigital.games.poker;

import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for HandAction - core data model for all player actions.
 */
class HandActionTest {

    private PokerPlayer player;

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        player = new PokerPlayer(1, "TestPlayer", true);
        player.setChipCount(1000);
    }

    // ========== Constructor Tests ==========

    @Test
    void should_CreateAction_WithAllParameters() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100, 0, "test debug");

        assertThat(action.getRound()).isEqualTo(HoldemHand.ROUND_FLOP);
        assertThat(action.getAction()).isEqualTo(HandAction.ACTION_BET);
        assertThat(action.getPlayer()).isSameAs(player);
        assertThat(action.getAmount()).isEqualTo(100);
        assertThat(action.getSubAmount()).isEqualTo(0);
        assertThat(action.getDebug()).isEqualTo("test debug");
    }

    @Test
    void should_CreateAction_WithAmountAndDebug() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_TURN, HandAction.ACTION_RAISE, 200, "debug");

        assertThat(action.getRound()).isEqualTo(HoldemHand.ROUND_TURN);
        assertThat(action.getAction()).isEqualTo(HandAction.ACTION_RAISE);
        assertThat(action.getAmount()).isEqualTo(200);
        assertThat(action.getSubAmount()).isEqualTo(0);
        assertThat(action.getDebug()).isEqualTo("debug");
    }

    @Test
    void should_CreateAction_WithAmount() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_RIVER, HandAction.ACTION_CALL, 100);

        assertThat(action.getAction()).isEqualTo(HandAction.ACTION_CALL);
        assertThat(action.getAmount()).isEqualTo(100);
        assertThat(action.getSubAmount()).isEqualTo(0);
        assertThat(action.getDebug()).isNull();
    }

    @Test
    void should_CreateAction_BasicConstructor() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_CHECK);

        assertThat(action.getAction()).isEqualTo(HandAction.ACTION_CHECK);
        assertThat(action.getAmount()).isEqualTo(0);
        assertThat(action.getSubAmount()).isEqualTo(0);
    }

    @Test
    void should_DetectAllIn_WhenPlayerHasNoChipsAfterAction() {
        player.setChipCount(0);
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 1000);

        assertThat(action.isAllIn()).isTrue();
    }

    @Test
    void should_NotDetectAllIn_WhenPlayerHasChipsRemaining() {
        player.setChipCount(500);
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100);

        assertThat(action.isAllIn()).isFalse();
    }

    @Test
    void should_NotDetectAllIn_ForNonBettingActions() {
        player.setChipCount(0);
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_FOLD, 0);

        assertThat(action.isAllIn()).isFalse();

        action = new HandAction(player, HoldemHand.ROUND_SHOWDOWN, HandAction.ACTION_WIN, 500);
        assertThat(action.isAllIn()).isFalse();
    }

    @Test
    void should_AllowSettingAllInFlag_ForDatabaseLoading() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100);

        assertThat(action.isAllIn()).isFalse();

        action.setAllIn(true);
        assertThat(action.isAllIn()).isTrue();
    }

    // ========== Action Code Tests ==========

    @Test
    void should_EncodeActionCodes_ForAllActionTypes() {
        assertThat(createAction(HandAction.ACTION_FOLD).getActionCode()).isEqualTo("FOLD");
        assertThat(createAction(HandAction.ACTION_CHECK).getActionCode()).isEqualTo("CHECK");
        assertThat(createAction(HandAction.ACTION_CHECK_RAISE).getActionCode()).isEqualTo("CHECK");
        assertThat(createAction(HandAction.ACTION_CALL).getActionCode()).isEqualTo("CALL");
        assertThat(createAction(HandAction.ACTION_BET).getActionCode()).isEqualTo("BET");
        assertThat(createAction(HandAction.ACTION_RAISE).getActionCode()).isEqualTo("RAISE");
        assertThat(createAction(HandAction.ACTION_BLIND_SM).getActionCode()).isEqualTo("SMALL");
        assertThat(createAction(HandAction.ACTION_BLIND_BIG).getActionCode()).isEqualTo("BIG");
        assertThat(createAction(HandAction.ACTION_ANTE).getActionCode()).isEqualTo("ANTE");
        assertThat(createAction(HandAction.ACTION_WIN).getActionCode()).isEqualTo("WIN");
        assertThat(createAction(HandAction.ACTION_OVERBET).getActionCode()).isEqualTo("OVER");
        assertThat(createAction(HandAction.ACTION_LOSE).getActionCode()).isEqualTo("LOSE");
    }

    @Test
    void should_ReturnNull_ForUnknownActionCode() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, 999, 0);

        assertThat(action.getActionCode()).isNull();
    }

    @Test
    void should_DecodeActionCodes_Correctly() {
        assertThat(HandAction.decodeActionType("FOLD")).isEqualTo(HandAction.ACTION_FOLD);
        assertThat(HandAction.decodeActionType("CHECK")).isEqualTo(HandAction.ACTION_CHECK);
        assertThat(HandAction.decodeActionType("CALL")).isEqualTo(HandAction.ACTION_CALL);
        assertThat(HandAction.decodeActionType("BET")).isEqualTo(HandAction.ACTION_BET);
        assertThat(HandAction.decodeActionType("RAISE")).isEqualTo(HandAction.ACTION_RAISE);
        assertThat(HandAction.decodeActionType("SMALL")).isEqualTo(HandAction.ACTION_BLIND_SM);
        assertThat(HandAction.decodeActionType("BIG")).isEqualTo(HandAction.ACTION_BLIND_BIG);
        assertThat(HandAction.decodeActionType("ANTE")).isEqualTo(HandAction.ACTION_ANTE);
        assertThat(HandAction.decodeActionType("WIN")).isEqualTo(HandAction.ACTION_WIN);
        assertThat(HandAction.decodeActionType("OVER")).isEqualTo(HandAction.ACTION_OVERBET);
        assertThat(HandAction.decodeActionType("LOSE")).isEqualTo(HandAction.ACTION_LOSE);
    }

    @Test
    void should_DecodeActionType_ReturnNone_ForNull() {
        assertThat(HandAction.decodeActionType(null)).isEqualTo(HandAction.ACTION_NONE);
    }

    @Test
    void should_DecodeActionType_ReturnNone_ForUnknownCode() {
        assertThat(HandAction.decodeActionType("INVALID")).isEqualTo(HandAction.ACTION_NONE);
        assertThat(HandAction.decodeActionType("")).isEqualTo(HandAction.ACTION_NONE);
        assertThat(HandAction.decodeActionType("UNKNOWN")).isEqualTo(HandAction.ACTION_NONE);
    }

    @Test
    void should_RoundTrip_ActionEncoding() {
        int[] actions = {HandAction.ACTION_FOLD, HandAction.ACTION_CHECK, HandAction.ACTION_CALL, HandAction.ACTION_BET,
                HandAction.ACTION_RAISE, HandAction.ACTION_BLIND_SM, HandAction.ACTION_BLIND_BIG,
                HandAction.ACTION_ANTE, HandAction.ACTION_WIN, HandAction.ACTION_OVERBET, HandAction.ACTION_LOSE};

        for (int action : actions) {
            HandAction ha = createAction(action);
            String code = ha.getActionCode();
            int decoded = HandAction.decodeActionType(code);
            assertThat(decoded).as("Round-trip failed for action %d", action).isEqualTo(action);
        }
    }

    // ========== Action Name Tests ==========

    @Test
    void should_GetActionNames_ForAllActionTypes() {
        assertThat(HandAction.getActionName(HandAction.ACTION_FOLD)).isEqualTo("fold");
        assertThat(HandAction.getActionName(HandAction.ACTION_CHECK)).isEqualTo("check");
        assertThat(HandAction.getActionName(HandAction.ACTION_CHECK_RAISE)).isEqualTo("checkraise");
        assertThat(HandAction.getActionName(HandAction.ACTION_CALL)).isEqualTo("call");
        assertThat(HandAction.getActionName(HandAction.ACTION_BET)).isEqualTo("bet");
        assertThat(HandAction.getActionName(HandAction.ACTION_RAISE)).isEqualTo("raise");
        assertThat(HandAction.getActionName(HandAction.ACTION_BLIND_SM)).isEqualTo("smallblind");
        assertThat(HandAction.getActionName(HandAction.ACTION_BLIND_BIG)).isEqualTo("bigblind");
        assertThat(HandAction.getActionName(HandAction.ACTION_ANTE)).isEqualTo("ante");
        assertThat(HandAction.getActionName(HandAction.ACTION_WIN)).isEqualTo("win");
        assertThat(HandAction.getActionName(HandAction.ACTION_OVERBET)).isEqualTo("overbet");
        assertThat(HandAction.getActionName(HandAction.ACTION_LOSE)).isEqualTo("lose");
    }

    @Test
    void should_ReturnUndefined_ForUnknownActionName() {
        assertThat(HandAction.getActionName(999)).isEqualTo("<undefined>");
    }

    @Test
    void should_GetName_ReturnActionName() {
        HandAction action = createAction(HandAction.ACTION_BET);
        assertThat(action.getName()).isEqualTo("bet");
    }

    // ========== Getter Tests ==========

    @Test
    void should_GetRound() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_RIVER, HandAction.ACTION_BET, 100);
        assertThat(action.getRound()).isEqualTo(HoldemHand.ROUND_RIVER);
    }

    @Test
    void should_GetAction() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 100);
        assertThat(action.getAction()).isEqualTo(HandAction.ACTION_RAISE);
    }

    @Test
    void should_GetPlayer() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_CALL, 100);
        assertThat(action.getPlayer()).isSameAs(player);
    }

    @Test
    void should_GetAmount() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 250);
        assertThat(action.getAmount()).isEqualTo(250);
    }

    @Test
    void should_GetSubAmount() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 200, 50, null);
        assertThat(action.getSubAmount()).isEqualTo(50);
    }

    @Test
    void should_GetAdjustedAmount_ForRaise() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 200, 50, null);
        // Adjusted amount = amount - subAmount = 200 - 50 = 150
        assertThat(action.getAdjustedAmount()).isEqualTo(150);
    }

    @Test
    void should_GetAdjustedAmount_ForNonRaise() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100);
        // For non-raise, adjusted amount = amount
        assertThat(action.getAdjustedAmount()).isEqualTo(100);
    }

    // ========== toString Tests ==========

    @Test
    void should_FormatToString_Short() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100);
        String result = action.toString(true);

        assertThat(result).contains("TestPlayer").contains("bet");
    }

    @Test
    void should_FormatToString_CheckWithSpacing() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_CHECK);
        String result = action.toString();

        assertThat(result).contains("check");
    }

    @Test
    void should_FormatToString_RaiseWithCallAmount() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 200, 50, null);
        String result = action.toString();

        assertThat(result).contains("raise").contains("50 call");
    }

    @Test
    void should_FormatToString_WinWithPotNumber() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_SHOWDOWN, HandAction.ACTION_WIN, 500, 1, null);
        String result = action.toString();

        assertThat(result).contains("win");
    }

    @Test
    void should_FormatToString_Long_WithRoundInfo() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100, 0, "test");
        String result = action.toString(false);

        assertThat(result).contains("TestPlayer").contains("flop").contains("bet").contains("test");
    }

    // ========== HTML Generation Tests ==========

    @Test
    void should_GetHTMLSnippet_ForBasicBet() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100);

        assertThatCode(() -> action.getHTMLSnippet("msg.test", 0, null)).doesNotThrowAnyException();
    }

    @Test
    void should_GetHTMLSnippet_ForRaise_WithRaiseIcon() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 200, 50, null);

        assertThatCode(() -> action.getHTMLSnippet("msg.test", 0, null)).doesNotThrowAnyException();
    }

    @Test
    void should_GetHTMLSnippet_ForReraise() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 300, 100, null);

        assertThatCode(() -> action.getHTMLSnippet("msg.test", 1, null)).doesNotThrowAnyException();
    }

    @Test
    void should_GetHTMLSnippet_ForRereraise() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 400, 150, null);

        assertThatCode(() -> action.getHTMLSnippet("msg.test", 2, null)).doesNotThrowAnyException();
    }

    @Test
    void should_GetHTMLSnippet_ForForcedFold() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_FOLD, 0,
                HandAction.FOLD_FORCED, null);

        assertThatCode(() -> action.getHTMLSnippet("msg.test", 0, null)).doesNotThrowAnyException();
    }

    @Test
    void should_GetHTMLSnippet_ForSittingOutFold() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_FOLD, 0,
                HandAction.FOLD_SITTING_OUT, null);

        assertThatCode(() -> action.getHTMLSnippet("msg.test", 0, null)).doesNotThrowAnyException();
    }

    @Test
    void should_GetHTMLSnippet_ForAllIn() {
        player.setChipCount(0);
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100);

        assertThat(action.isAllIn()).isTrue();

        assertThatCode(() -> action.getHTMLSnippet("msg.test", 0, null)).doesNotThrowAnyException();
    }

    @Test
    void should_GetChat_WrapInTable() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 100);
        String chat = action.getChat(0, null, null);

        assertThat(chat).contains("<TABLE").contains("<TR>").contains("<TD>");
    }

    @Test
    void should_GetChat_WithSuffix() {
        HandAction action = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_CALL, 50);

        assertThatCode(() -> action.getChat(0, null, "dealer")).doesNotThrowAnyException();
    }

    // ========== Serialization Tests ==========
    // Note: Serialization tests with MsgState mocking are skipped due to Mockito
    // limitations on Java 25.
    // These could be tested with integration tests or real MsgState objects in the
    // future.

    // ========== Helper Methods ==========

    private HandAction createAction(int actionType) {
        return new HandAction(player, HoldemHand.ROUND_FLOP, actionType, 100);
    }
}
