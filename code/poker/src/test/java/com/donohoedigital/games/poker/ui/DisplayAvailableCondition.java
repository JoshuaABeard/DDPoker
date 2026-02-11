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
package com.donohoedigital.games.poker.ui;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.awt.*;

/**
 * JUnit 5 extension that checks if a graphical display is available.
 * Used by {@link EnabledIfDisplay} annotation to skip UI tests in headless environments.
 */
public class DisplayAvailableCondition implements ExecutionCondition
{
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        // Check if display is available
        boolean hasDisplay = hasGraphicsEnvironment();

        if (hasDisplay)
        {
            return ConditionEvaluationResult.enabled("Display is available");
        }
        else
        {
            return ConditionEvaluationResult.disabled(
                "Test disabled: No display available (headless environment). " +
                "UI tests require a graphics environment. " +
                "See README-UI-TESTS.md for details."
            );
        }
    }

    /**
     * Check if a graphics environment is available.
     */
    private boolean hasGraphicsEnvironment()
    {
        try
        {
            // Check for headless mode
            if (GraphicsEnvironment.isHeadless())
            {
                return false;
            }

            // Try to get the graphics environment
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = ge.getScreenDevices();

            // Must have at least one screen device
            return devices != null && devices.length > 0;
        }
        catch (HeadlessException e)
        {
            return false;
        }
        catch (Exception e)
        {
            // If we can't determine, assume no display
            System.err.println("Warning: Could not determine if display is available: " + e.getMessage());
            return false;
        }
    }
}
