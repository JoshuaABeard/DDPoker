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
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DataMarshaller marshal/demarshal round-trips covering built-in
 * wrapper types, collection types, TokenizedList, and poker-specific
 * DataMarshal implementations.
 */
class PokerDataMarshallerTest {

    @BeforeAll
    static void initConfig() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    // =================================================================
    // Built-in Wrapper Type Round-Trips
    // =================================================================

    @Test
    void should_RoundTripInteger_When_IntegerMarshalled() {
        DataMarshaller.DMInteger original = new DataMarshaller.DMInteger(42);
        String marshalled = DataMarshaller.marshal(original);

        assertThat(marshalled).startsWith("i");

        DataMarshal result = DataMarshaller.demarshal(marshalled);
        assertThat(result).isInstanceOf(DataMarshaller.DMInteger.class);
        assertThat(((DataMarshaller.DMWrapper) result).value()).isEqualTo(42);
    }

    @Test
    void should_RoundTripNegativeInteger_When_NegativeIntegerMarshalled() {
        DataMarshaller.DMInteger original = new DataMarshaller.DMInteger(-500);
        String marshalled = DataMarshaller.marshal(original);
        DataMarshal result = DataMarshaller.demarshal(marshalled);

        assertThat(((DataMarshaller.DMWrapper) result).value()).isEqualTo(-500);
    }

    @Test
    void should_RoundTripZeroInteger_When_ZeroMarshalled() {
        DataMarshaller.DMInteger original = new DataMarshaller.DMInteger(0);
        String marshalled = DataMarshaller.marshal(original);
        DataMarshal result = DataMarshaller.demarshal(marshalled);

        assertThat(((DataMarshaller.DMWrapper) result).value()).isEqualTo(0);
    }

    @Test
    void should_RoundTripString_When_StringMarshalled() {
        DataMarshaller.DMString original = new DataMarshaller.DMString("TestPlayer");
        String marshalled = DataMarshaller.marshal(original);

        assertThat(marshalled).startsWith("s");

        DataMarshal result = DataMarshaller.demarshal(marshalled);
        assertThat(result).isInstanceOf(DataMarshaller.DMString.class);
        assertThat(((DataMarshaller.DMWrapper) result).value()).isEqualTo("TestPlayer");
    }

    @Test
    void should_RoundTripEmptyString_When_EmptyStringMarshalled() {
        DataMarshaller.DMString original = new DataMarshaller.DMString("");
        String marshalled = DataMarshaller.marshal(original);
        DataMarshal result = DataMarshaller.demarshal(marshalled);

        assertThat(((DataMarshaller.DMWrapper) result).value()).isEqualTo("");
    }

    @Test
    void should_RoundTripBooleanTrue_When_TrueMarshalled() {
        DataMarshaller.DMBoolean original = new DataMarshaller.DMBoolean(true);
        String marshalled = DataMarshaller.marshal(original);

        assertThat(marshalled).startsWith("b");

        DataMarshal result = DataMarshaller.demarshal(marshalled);
        assertThat(result).isInstanceOf(DataMarshaller.DMBoolean.class);
        assertThat(((DataMarshaller.DMWrapper) result).value()).isEqualTo(true);
    }

    @Test
    void should_RoundTripBooleanFalse_When_FalseMarshalled() {
        DataMarshaller.DMBoolean original = new DataMarshaller.DMBoolean(false);
        String marshalled = DataMarshaller.marshal(original);
        DataMarshal result = DataMarshaller.demarshal(marshalled);

        assertThat(((DataMarshaller.DMWrapper) result).value()).isEqualTo(false);
    }

    @Test
    void should_RoundTripDouble_When_DoubleMarshalled() {
        DataMarshaller.DMDouble original = new DataMarshaller.DMDouble(3.14159);
        String marshalled = DataMarshaller.marshal(original);

        assertThat(marshalled).startsWith("d");

        DataMarshal result = DataMarshaller.demarshal(marshalled);
        assertThat(result).isInstanceOf(DataMarshaller.DMDouble.class);
        assertThat((double) ((DataMarshaller.DMWrapper) result).value()).isEqualTo(3.14159);
    }

    @Test
    void should_RoundTripLong_When_LongMarshalled() {
        DataMarshaller.DMLong original = new DataMarshaller.DMLong(9876543210L);
        String marshalled = DataMarshaller.marshal(original);

        assertThat(marshalled).startsWith("l");

        DataMarshal result = DataMarshaller.demarshal(marshalled);
        assertThat(result).isInstanceOf(DataMarshaller.DMLong.class);
        assertThat(((DataMarshaller.DMWrapper) result).value()).isEqualTo(9876543210L);
    }

    @Test
    void should_RoundTripNull_When_NullMarshalled() {
        DataMarshaller.DMNull original = new DataMarshaller.DMNull();
        String marshalled = DataMarshaller.marshal(original);

        assertThat(marshalled).startsWith("~");

        DataMarshal result = DataMarshaller.demarshal(marshalled);
        assertThat(result).isInstanceOf(DataMarshaller.DMNull.class);
        assertThat(((DataMarshaller.DMWrapper) result).value()).isNull();
    }

    // =================================================================
    // TokenizedList Round-Trips
    // =================================================================

    @Test
    void should_RoundTripTokenizedList_When_MixedTypesAdded() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken(100);
        original.addToken("hello");
        original.addToken(true);
        original.addToken(2.5);
        original.addToken(42L);

        String marshalled = original.marshal(state);

        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        assertThat(restored.removeIntToken()).isEqualTo(100);
        assertThat(restored.removeStringToken()).isEqualTo("hello");
        assertThat(restored.removeBooleanToken()).isTrue();
        assertThat(restored.removeDoubleToken()).isEqualTo(2.5);
        assertThat(restored.removeLongToken()).isEqualTo(42L);
        assertThat(restored.hasMoreTokens()).isFalse();
    }

    @Test
    void should_RoundTripTokenizedList_When_NullTokensPresent() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken(1);
        original.addTokenNull();
        original.addToken("after_null");

        String marshalled = original.marshal(state);

        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        assertThat(restored.removeIntToken()).isEqualTo(1);
        assertThat(restored.removeToken()).isNull();
        assertThat(restored.removeStringToken()).isEqualTo("after_null");
    }

    // =================================================================
    // DMArrayList Round-Trips
    // =================================================================

    @Test
    @SuppressWarnings("unchecked")
    void should_RoundTripDMArrayList_When_IntegersAdded() {
        MsgState state = new MsgState();
        DMArrayList<Integer> original = new DMArrayList<>();
        original.add(10);
        original.add(20);
        original.add(30);

        String marshalled = DataMarshaller.marshal(state, original);
        DataMarshal result = DataMarshaller.demarshal(state, marshalled);

        assertThat(result).isInstanceOf(DMArrayList.class);
        DMArrayList<Integer> restored = (DMArrayList<Integer>) result;
        assertThat(restored).containsExactly(10, 20, 30);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_RoundTripDMArrayList_When_StringsAdded() {
        MsgState state = new MsgState();
        DMArrayList<String> original = new DMArrayList<>();
        original.add("Alice");
        original.add("Bob");

        String marshalled = DataMarshaller.marshal(state, original);
        DMArrayList<String> restored = (DMArrayList<String>) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored).containsExactly("Alice", "Bob");
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_RoundTripEmptyDMArrayList_When_EmptyListMarshalled() {
        MsgState state = new MsgState();
        DMArrayList<String> original = new DMArrayList<>();

        String marshalled = DataMarshaller.marshal(state, original);
        DMArrayList<String> restored = (DMArrayList<String>) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored).isEmpty();
    }

    // =================================================================
    // DMTypedHashMap Round-Trips
    // =================================================================

    @Test
    void should_RoundTripDMTypedHashMap_When_NameValuePairsAdded() {
        MsgState state = new MsgState();
        DMTypedHashMap original = new DMTypedHashMap();
        original.setInteger("chips", 1500);
        original.setString("name", "TestPlayer");
        original.setBoolean("active", true);

        String marshalled = DataMarshaller.marshal(state, original);
        DataMarshal result = DataMarshaller.demarshal(state, marshalled);

        assertThat(result).isInstanceOf(DMTypedHashMap.class);
        DMTypedHashMap restored = (DMTypedHashMap) result;
        assertThat(restored.getInteger("chips")).isEqualTo(1500);
        assertThat(restored.getString("name")).isEqualTo("TestPlayer");
        assertThat(restored.getBoolean("active")).isTrue();
    }

    // =================================================================
    // HandAction Round-Trip (poker-specific DataMarshal)
    // =================================================================

    @Test
    void should_RoundTripHandAction_When_FoldActionMarshalled() {
        MsgState state = new MsgState();
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        player.setChipCount(1000);

        // Register player in MsgState so marshal/demarshal can reference it by ID
        Integer playerId = state.getId(player);

        HandAction original = new HandAction(player, HoldemHand.ROUND_PRE_FLOP, HandAction.ACTION_FOLD);
        String marshalled = DataMarshaller.marshal(state, original);

        assertThat(marshalled).startsWith("?");

        // Demarshal with the same state (player is already registered)
        HandAction restored = (HandAction) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getAction()).isEqualTo(HandAction.ACTION_FOLD);
        assertThat(restored.getRound()).isEqualTo(HoldemHand.ROUND_PRE_FLOP);
        assertThat(restored.getAmount()).isZero();
        assertThat(restored.getPlayer()).isSameAs(player);
    }

    @Test
    void should_RoundTripHandAction_When_RaiseActionMarshalled() {
        MsgState state = new MsgState();
        PokerPlayer player = new PokerPlayer(1, "Raiser", true);
        player.setChipCount(500);

        state.getId(player);

        HandAction original = new HandAction(player, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 200, 50, null);
        String marshalled = DataMarshaller.marshal(state, original);

        HandAction restored = (HandAction) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getAction()).isEqualTo(HandAction.ACTION_RAISE);
        assertThat(restored.getRound()).isEqualTo(HoldemHand.ROUND_FLOP);
        assertThat(restored.getAmount()).isEqualTo(200);
        assertThat(restored.getSubAmount()).isEqualTo(50);
        assertThat(restored.getPlayer()).isSameAs(player);
    }

    @Test
    void should_PreserveAllInFlag_When_AllInPlayerActionMarshalled() {
        MsgState state = new MsgState();
        PokerPlayer player = new PokerPlayer(1, "AllInPlayer", true);
        player.setChipCount(0); // Zero chips means all-in when action is created

        state.getId(player);

        HandAction original = new HandAction(player, HoldemHand.ROUND_RIVER, HandAction.ACTION_CALL, 300);

        assertThat(original.isAllIn()).isTrue();

        String marshalled = DataMarshaller.marshal(state, original);
        HandAction restored = (HandAction) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.isAllIn()).isTrue();
        assertThat(restored.getAmount()).isEqualTo(300);
    }

    @Test
    void should_PreserveDebugString_When_HandActionWithDebugMarshalled() {
        MsgState state = new MsgState();
        PokerPlayer player = new PokerPlayer(1, "DebugPlayer", true);
        player.setChipCount(1000);

        state.getId(player);

        HandAction original = new HandAction(player, HoldemHand.ROUND_TURN, HandAction.ACTION_BET, 100, "AI-decided");
        String marshalled = DataMarshaller.marshal(state, original);

        HandAction restored = (HandAction) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getDebug()).isEqualTo("AI-decided");
        assertThat(restored.getAction()).isEqualTo(HandAction.ACTION_BET);
    }

    // =================================================================
    // Pot Round-Trip (poker-specific DataMarshal)
    // =================================================================

    @Test
    void should_RoundTripPot_When_PotWithPlayersMarshalled() {
        MsgState state = new MsgState();
        PokerPlayer player1 = new PokerPlayer(1, "Player1", true);
        PokerPlayer player2 = new PokerPlayer(2, "Player2", false);
        player1.setChipCount(1000);
        player2.setChipCount(800);

        state.getId(player1);
        state.getId(player2);

        Pot original = new Pot(HoldemHand.ROUND_FLOP, 0);
        original.addChips(player1, 200);
        original.addChips(player2, 200);

        String marshalled = DataMarshaller.marshal(state, original);

        assertThat(marshalled).startsWith("P");

        Pot restored = (Pot) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getChipCount()).isEqualTo(400);
        assertThat(restored.getRound()).isEqualTo(HoldemHand.ROUND_FLOP);
        assertThat(restored.getNumPlayers()).isEqualTo(2);
        assertThat(restored.getPlayerAt(0)).isSameAs(player1);
        assertThat(restored.getPlayerAt(1)).isSameAs(player2);
    }

    @Test
    void should_PreserveSideBet_When_SidePotMarshalled() {
        MsgState state = new MsgState();
        PokerPlayer player = new PokerPlayer(1, "SidePlayer", true);
        player.setChipCount(500);

        state.getId(player);

        Pot original = new Pot(HoldemHand.ROUND_PRE_FLOP, 1);
        original.addChips(player, 100);
        original.setSideBet(50);

        String marshalled = DataMarshaller.marshal(state, original);
        Pot restored = (Pot) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getSideBet()).isEqualTo(50);
        assertThat(restored.getChipCount()).isEqualTo(100);
    }

    // =================================================================
    // Type Prefix Verification
    // =================================================================

    @Test
    void should_UseCorrectTypePrefix_When_EachTypeMarshalled() {
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMInteger(1))).startsWith("i");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMString("x"))).startsWith("s");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMBoolean(true))).startsWith("b");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMDouble(1.0))).startsWith("d");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMLong(1L))).startsWith("l");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMNull())).startsWith("~");
    }

    @Test
    void should_ProduceExpectedFormat_When_SimpleValuesMarshalled() {
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMInteger(42))).isEqualTo("i42");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMString("hello"))).isEqualTo("shello");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMBoolean(true))).isEqualTo("b+");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMBoolean(false))).isEqualTo("b-");
        assertThat(DataMarshaller.marshal(new DataMarshaller.DMLong(99L))).isEqualTo("l99");
    }

    // =================================================================
    // GameClock Round-Trip
    // =================================================================

    @Test
    void should_RoundTripGameClock_When_ClockMarshalled() {
        MsgState state = new MsgState();
        GameClock original = new GameClock();
        original.setSecondsRemaining(300);

        String marshalled = DataMarshaller.marshal(state, original);

        assertThat(marshalled).startsWith("c");

        GameClock restored = (GameClock) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getSecondsRemaining()).isEqualTo(300);
    }
}
