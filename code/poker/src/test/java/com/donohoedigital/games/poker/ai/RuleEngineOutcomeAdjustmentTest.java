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
package com.donohoedigital.games.poker.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for RuleEngine's OutcomeAdjustment inner class.
 * Uses reflection to access and test private inner class.
 */
class RuleEngineOutcomeAdjustmentTest
{
    private Class<?> adjustmentClass;
    private Constructor<?> adjustmentConstructor;
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() throws Exception
    {
        // Create a RuleEngine instance to use as outer class reference
        ruleEngine = new RuleEngine();

        // Get the OutcomeAdjustment inner class
        Class<?>[] innerClasses = RuleEngine.class.getDeclaredClasses();
        for (Class<?> innerClass : innerClasses)
        {
            if (innerClass.getSimpleName().equals("OutcomeAdjustment"))
            {
                adjustmentClass = innerClass;
                break;
            }
        }

        assertThat(adjustmentClass).isNotNull();

        // Get the constructor
        adjustmentConstructor = adjustmentClass.getDeclaredConstructor(
            RuleEngine.class, int.class, int.class, int.class, boolean.class,
            float.class, float.class, float.class, float.class);
        adjustmentConstructor.setAccessible(true);
    }

    /**
     * Helper to create an OutcomeAdjustment instance via reflection
     */
    private Object createAdjustment(int outcome, int factor, int curve, boolean invert,
                                   float weight, float min, float max, float value) throws Exception
    {
        return adjustmentConstructor.newInstance(ruleEngine, outcome, factor, curve, invert,
                                                weight, min, max, value);
    }

    /**
     * Helper to get field value via reflection
     */
    private float getFieldValue(Object obj, String fieldName) throws Exception
    {
        Field field = adjustmentClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (float) field.get(obj);
    }

    // ========================================
    // Value Normalization Tests
    // ========================================

    @Test
    void should_ClampToMax_When_ValueExceedsMax() throws Exception
    {
        // Value 100 should be clamped to max 50
        Object adjustment = createAdjustment(0, 0, 1, false, 1.0f, 0.0f, 50.0f, 100.0f);

        float x = getFieldValue(adjustment, "x");

        // x should be 1.0 since value was clamped to max
        assertThat(x).isEqualTo(1.0f);
    }

    @Test
    void should_ClampToMin_When_ValueBelowMin() throws Exception
    {
        // Value -10 should be clamped to min 0
        Object adjustment = createAdjustment(0, 0, 1, false, 1.0f, 0.0f, 100.0f, -10.0f);

        float x = getFieldValue(adjustment, "x");

        // x should be 0.0 since value was clamped to min
        assertThat(x).isEqualTo(0.0f);
    }

    @Test
    void should_NormalizeCorrectly_When_ValueInRange() throws Exception
    {
        // Value 50 in range [0, 100] should normalize to 0.5
        Object adjustment = createAdjustment(0, 0, 1, false, 1.0f, 0.0f, 100.0f, 50.0f);

        float x = getFieldValue(adjustment, "x");

        // x = (value - min) / (max - min) = (50 - 0) / (100 - 0) = 0.5
        assertThat(x).isEqualTo(0.5f);
    }

    // ========================================
    // Curve Application Tests
    // ========================================

    @Test
    void should_ReturnLinear_When_CurveIs1() throws Exception
    {
        // Curve 1 means f(x) = x^1 = x (linear)
        Object adjustment = createAdjustment(0, 0, 1, false, 1.0f, 0.0f, 100.0f, 50.0f);

        float x = getFieldValue(adjustment, "x");
        float fx = getFieldValue(adjustment, "fx");

        // f(x) = x^1 = 0.5
        assertThat(x).isEqualTo(0.5f);
        assertThat(fx).isEqualTo(0.5f);
    }

    @Test
    void should_ApplySquare_When_CurveIs2() throws Exception
    {
        // Curve 2 means f(x) = x^2
        Object adjustment = createAdjustment(0, 0, 2, false, 1.0f, 0.0f, 100.0f, 50.0f);

        float x = getFieldValue(adjustment, "x");
        float fx = getFieldValue(adjustment, "fx");

        // f(x) = 0.5^2 = 0.25
        assertThat(x).isEqualTo(0.5f);
        assertThat(fx).isCloseTo(0.25f, within(0.001f));
    }

    @Test
    void should_ApplyCubic_When_CurveIs3() throws Exception
    {
        // Curve 3 means f(x) = x^3
        Object adjustment = createAdjustment(0, 0, 3, false, 1.0f, 0.0f, 100.0f, 50.0f);

        float x = getFieldValue(adjustment, "x");
        float fx = getFieldValue(adjustment, "fx");

        // f(x) = 0.5^3 = 0.125
        assertThat(x).isEqualTo(0.5f);
        assertThat(fx).isCloseTo(0.125f, within(0.001f));
    }

    // ========================================
    // Inversion Tests
    // ========================================

    @Test
    void should_InvertResult_When_InvertIsTrue() throws Exception
    {
        // With invert=true, fx = 1.0 - f(x)
        Object adjustment = createAdjustment(0, 0, 1, true, 1.0f, 0.0f, 100.0f, 50.0f);

        float x = getFieldValue(adjustment, "x");
        float fx = getFieldValue(adjustment, "fx");

        // x = 0.5, f(x) = 0.5, inverted = 1.0 - 0.5 = 0.5
        assertThat(x).isEqualTo(0.5f);
        assertThat(fx).isEqualTo(0.5f);
    }

    @Test
    void should_NotInvertResult_When_InvertIsFalse() throws Exception
    {
        // With invert=false, fx = f(x)
        Object adjustment = createAdjustment(0, 0, 1, false, 1.0f, 0.0f, 100.0f, 75.0f);

        float x = getFieldValue(adjustment, "x");
        float fx = getFieldValue(adjustment, "fx");

        // x = 0.75, f(x) = 0.75, not inverted = 0.75
        assertThat(x).isEqualTo(0.75f);
        assertThat(fx).isEqualTo(0.75f);
    }

    // ========================================
    // Integration Test
    // ========================================

    @Test
    void should_ApplyAllTransformations_When_CombiningNormalizationCurveAndInversion() throws Exception
    {
        // Test comprehensive scenario: value=80 in [0,100], curve=2, inverted
        Object adjustment = createAdjustment(0, 0, 2, true, 1.0f, 0.0f, 100.0f, 80.0f);

        float x = getFieldValue(adjustment, "x");
        float fx = getFieldValue(adjustment, "fx");

        // x = (80 - 0) / (100 - 0) = 0.8
        assertThat(x).isEqualTo(0.8f);

        // f(x) = 0.8^2 = 0.64
        // inverted: 1.0 - 0.64 = 0.36
        assertThat(fx).isCloseTo(0.36f, within(0.001f));
    }

    @Test
    void should_HandleZeroRange_When_MinEqualsMax() throws Exception
    {
        // When min == max, x = value (no normalization)
        Object adjustment = createAdjustment(0, 0, 1, false, 1.0f, 50.0f, 50.0f, 50.0f);

        float x = getFieldValue(adjustment, "x");
        float fx = getFieldValue(adjustment, "fx");

        // x = value (50.0) when min == max
        assertThat(x).isEqualTo(50.0f);
        assertThat(fx).isEqualTo(50.0f);
    }
}
