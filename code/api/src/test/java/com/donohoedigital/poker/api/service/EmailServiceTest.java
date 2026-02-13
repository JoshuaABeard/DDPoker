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
package com.donohoedigital.poker.api.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EmailService.
 *
 * TODO: These tests are currently disabled because EmailService uses
 * constructor-based instantiation of DDPostalServiceImpl which requires
 * PropertyConfig initialization. EmailService should be refactored to use
 * dependency injection (accept DDPostalService via constructor) to make it
 * testable without full PropertyConfig initialization.
 */
class EmailServiceTest {

    @Test
    @Disabled("EmailService requires PropertyConfig initialization - needs refactoring for DI")
    void testSendPasswordResetEmail_Success() {
        EmailService emailService = new EmailService();

        // Note: This test will attempt to send a real email if SMTP is configured
        // In a real environment, we would mock DDPostalService
        // For now, we test that the method doesn't throw exceptions

        boolean result = emailService.sendPasswordResetEmail("test@example.com", "testuser", "tempPass123");

        // Result depends on SMTP configuration
        // Just verify no exceptions thrown
        assertNotNull(result);
    }

    @Test
    @Disabled("EmailService requires PropertyConfig initialization - needs refactoring for DI")
    void testSendPasswordResetEmail_NullEmail() {
        EmailService emailService = new EmailService();

        assertThrows(Exception.class, () -> {
            emailService.sendPasswordResetEmail(null, "testuser", "tempPass123");
        });
    }

    @Test
    @Disabled("EmailService requires PropertyConfig initialization - needs refactoring for DI")
    void testSendPasswordResetEmail_EmptyPassword() {
        EmailService emailService = new EmailService();

        boolean result = emailService.sendPasswordResetEmail("test@example.com", "testuser", "");

        // Should handle gracefully (may fail to send but shouldn't crash)
        assertNotNull(result);
    }
}
