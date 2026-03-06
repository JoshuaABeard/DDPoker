/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmailVerificationDialog} logic (non-UI, headless safe).
 *
 * <p>
 * These tests exercise the dialog's state transitions via the {@code simulate*}
 * test hooks, without creating any visible Swing windows or making network
 * calls.
 */
@EnabledIfDisplay
class EmailVerificationDialogTest {

    private static final String SERVER_URL = "http://poker.example.com:8877";
    private static final String EMAIL = "alice@example.com";

    /**
     * Dialog constructs without error and resend button is initially enabled.
     */
    @Test
    void initialState_resendButtonIsEnabled() {
        EmailVerificationDialog dialog = new EmailVerificationDialog(null, SERVER_URL, EMAIL);

        assertThat(dialog.isResendButtonEnabled()).isTrue();
        assertThat(dialog.getResendButtonText()).isEqualTo("Resend Email");
    }

    /**
     * Status label is initially blank (or whitespace).
     */
    @Test
    void initialState_statusLabelIsBlank() {
        EmailVerificationDialog dialog = new EmailVerificationDialog(null, SERVER_URL, EMAIL);

        assertThat(dialog.getStatusLabelText().trim()).isEmpty();
    }

    /**
     * simulateResendSuccess disables the button and updates its label.
     */
    @Test
    void simulateResendSuccess_buttonDisabledWithSuccessLabel() {
        EmailVerificationDialog dialog = new EmailVerificationDialog(null, SERVER_URL, EMAIL);

        dialog.simulateResendSuccess();

        assertThat(dialog.isResendButtonEnabled()).isFalse();
        assertThat(dialog.getResendButtonText()).contains("Email Sent");
    }

    /**
     * simulateResendSuccess clears the status label text.
     */
    @Test
    void simulateResendSuccess_statusLabelIsBlank() {
        EmailVerificationDialog dialog = new EmailVerificationDialog(null, SERVER_URL, EMAIL);

        dialog.simulateResendSuccess();

        assertThat(dialog.getStatusLabelText().trim()).isEmpty();
    }

    /**
     * simulateResendRateLimited keeps button enabled and shows the wait message.
     */
    @Test
    void simulateResendRateLimited_buttonEnabledWithWaitMessage() {
        EmailVerificationDialog dialog = new EmailVerificationDialog(null, SERVER_URL, EMAIL);

        dialog.simulateResendRateLimited();

        assertThat(dialog.isResendButtonEnabled()).isTrue();
        assertThat(dialog.getStatusLabelText()).contains("Please wait");
    }

    /**
     * simulateResendFailure keeps button enabled and shows the failure message.
     */
    @Test
    void simulateResendFailure_buttonEnabledWithFailureMessage() {
        EmailVerificationDialog dialog = new EmailVerificationDialog(null, SERVER_URL, EMAIL);

        dialog.simulateResendFailure();

        assertThat(dialog.isResendButtonEnabled()).isTrue();
        assertThat(dialog.getStatusLabelText()).contains("Failed to send");
    }
}
