package com.donohoedigital.games.poker.ui;

import org.junit.jupiter.api.Test;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quick test to verify display detection logic.
 */
public class DisplayCheckTest
{
    @Test
    void should_DetectDisplay()
    {
        boolean headless = GraphicsEnvironment.isHeadless();
        System.out.println("Headless mode: " + headless);

        if (!headless)
        {
            try {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice[] devices = ge.getScreenDevices();
                System.out.println("Screen devices found: " + (devices != null ? devices.length : 0));

                assertThat(devices).isNotNull();
                assertThat(devices.length).isGreaterThan(0);
            } catch (Exception e) {
                System.err.println("Error getting graphics environment: " + e.getMessage());
                throw e;
            }
        }
        else
        {
            System.out.println("Running in headless mode - UI tests would be skipped");
        }
    }
}
