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
package com.donohoedigital.poker.api.service;

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.mail.DDPostalService;
import com.donohoedigital.mail.DDPostalServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails using DDPostalService.
 */
@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final DDPostalService postalService;
    private final String fromEmail;

    public EmailService() {
        this.postalService = new DDPostalServiceImpl(false);
        this.fromEmail = PropertyConfig.getStringProperty("settings.email.from", "noreply@ddpoker.com", false);
    }

    /**
     * Send password reset email to user.
     *
     * @param toEmail
     *            recipient email address
     * @param username
     *            user's profile name
     * @param password
     *            user's password
     * @return true if email sent successfully
     */
    public boolean sendPasswordResetEmail(String toEmail, String username, String password) {
        String subject = "DD Poker - Password Reset";
        String body = buildPasswordResetEmailBody(username, password);

        try {
            logger.info("Sending password reset email to {} for user {}", toEmail, username);
            postalService.sendMail(toEmail, fromEmail, fromEmail, subject, body, null, null, null);
            logger.info("Password reset email sent successfully to {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            logger.debug("Email send error details", e);
            return false;
        }
    }

    private String buildPasswordResetEmailBody(String username, String password) {
        return String.format("Hello %s,\n\n" + "You requested a password reset for your DD Poker account.\n\n"
                + "Your temporary password is: %s\n\n"
                + "Please log in with this temporary password and change it immediately "
                + "in your profile settings.\n\n"
                + "If you did not request this, please contact support immediately.\n\n" + "Thanks,\n"
                + "The DD Poker Team", username, password);
    }
}
