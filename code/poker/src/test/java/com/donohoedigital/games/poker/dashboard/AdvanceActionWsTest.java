/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.dashboard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for AdvanceAction._getAdvanceActionWS — the private method that maps
 * pre-selected advance-action buttons to WebSocket action strings. Uses Unsafe
 * to set up the deeply Swing-coupled AdvanceAction without a full GUI context,
 * and invokes _getAdvanceActionWS directly via reflection.
 */
class AdvanceActionWsTest {

    private static final Unsafe UNSAFE = getUnsafe();

    private AdvanceAction instance;
    private Method method;

    // Checkbox fields wired via Unsafe
    private JCheckBox checkfold;
    private JCheckBox call;
    private JCheckBox bet;
    private JCheckBox raise;
    private JCheckBox betpot;
    private JCheckBox raisepot;
    private JCheckBox allin;

    @BeforeEach
    void setUp() throws Exception {
        // Allocate an AdvanceAction without calling its constructor (needs GameContext)
        instance = (AdvanceAction) UNSAFE.allocateInstance(AdvanceAction.class);

        // Create plain JCheckBox instances for each advance-action button.
        // The fields are typed as inner class Advance (extends DDCheckBox extends
        // JCheckBox), so we use Unsafe.putObject to bypass type checking.
        checkfold = new JCheckBox();
        call = new JCheckBox();
        bet = new JCheckBox();
        raise = new JCheckBox();
        betpot = new JCheckBox();
        raisepot = new JCheckBox();
        allin = new JCheckBox();

        putField(instance, "checkfold_", checkfold);
        putField(instance, "call_", call);
        putField(instance, "bet_", bet);
        putField(instance, "raise_", raise);
        putField(instance, "betpot_", betpot);
        putField(instance, "raisepot_", raisepot);
        putField(instance, "allin_", allin);

        // Get the fixed _getAdvanceActionWS with canAllIn and allInAmount params
        method = AdvanceAction.class.getDeclaredMethod("_getAdvanceActionWS", boolean.class, boolean.class, int.class,
                boolean.class, int.class, boolean.class, int.class);
        method.setAccessible(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        putStaticField(AdvanceAction.class, "impl_", null);
    }

    /**
     * When allin is selected and canAllIn is true, return ALL_IN with the actual
     * allInAmount.
     */
    @Test
    void allinSelectedWithCanAllInTrueReturnsAllIn() throws Exception {
        allin.setSelected(true);

        String[] result = invoke(false, false, 0, false, 0, true, 500);

        assertThat(result).containsExactly("ALL_IN", "500");
    }

    /**
     * When allin is selected but canAllIn is false (short-stack scenario), fall
     * back to CALL.
     */
    @Test
    void allinSelectedWithCanAllInFalseReturnsCall() throws Exception {
        allin.setSelected(true);

        String[] result = invoke(false, false, 0, false, 0, false, 0);

        assertThat(result).containsExactly("CALL", "0");
    }

    @Test
    void checkfoldSelectedWithCanCheckTrueReturnsCheck() throws Exception {
        checkfold.setSelected(true);

        String[] result = invoke(true, false, 0, false, 0, false, 0);

        assertThat(result).containsExactly("CHECK", "0");
    }

    @Test
    void checkfoldSelectedWithCanCheckFalseReturnsFold() throws Exception {
        checkfold.setSelected(true);

        String[] result = invoke(false, false, 0, false, 0, false, 0);

        assertThat(result).containsExactly("FOLD", "0");
    }

    @Test
    void noButtonSelectedReturnsNull() throws Exception {
        String[] result = invoke(true, true, 100, true, 200, true, 500);

        assertThat(result).isNull();
    }

    // --- Invoke helper ---

    private String[] invoke(boolean canCheck, boolean canBet, int maxBet, boolean canRaise, int maxRaise,
            boolean canAllIn, int allInAmount) throws Exception {
        return (String[]) method.invoke(instance, canCheck, canBet, maxBet, canRaise, maxRaise, canAllIn, allInAmount);
    }

    // --- Unsafe helpers ---

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void putField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        long offset = UNSAFE.objectFieldOffset(field);
        UNSAFE.putObject(target, offset, value);
    }

    private static void putStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = findField(clazz, fieldName);
        long offset = UNSAFE.staticFieldOffset(field);
        UNSAFE.putObject(UNSAFE.staticFieldBase(field), offset, value);
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
