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

import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.RestAuthClient.ResendRateLimitedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Modal dialog informing the user that email verification is required and
 * offering a "Resend Email" button.
 *
 * <p>
 * Construct with the parent frame, server URL, and the user's email address:
 *
 * <pre>
 * new EmailVerificationDialog(parentFrame, serverUrl, email).setVisible(true);
 * </pre>
 *
 * <p>
 * "Resend Email" calls {@link RestAuthClient#resendVerification(String)} on a
 * background thread. On success the button is disabled and labelled "Email
 * Sent". On a rate-limit error a friendly message is shown. On any other error,
 * a generic failure message is shown.
 */
public class EmailVerificationDialog extends JDialog {

    private static final Logger logger = LogManager.getLogger(EmailVerificationDialog.class);

    private final String serverUrl;
    private final String email;

    private JButton resendButton;
    private JLabel statusLabel;

    /**
     * Constructs the email verification dialog.
     *
     * @param parent
     *            parent frame; may be null
     * @param serverUrl
     *            base URL of the server (e.g. {@code http://localhost:8877})
     * @param email
     *            the user's email address to display
     */
    public EmailVerificationDialog(Frame parent, String serverUrl, String email) {
        super(parent, "Email Verification Required", true);
        this.serverUrl = serverUrl;
        this.email = email;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        setContentPane(root);

        root.add(buildMessagePanel(), BorderLayout.CENTER);
        root.add(buildButtonPanel(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(380, 200));
        pack();
        if (parent != null) {
            setLocationRelativeTo(parent);
        } else {
            setLocationByPlatform(true);
        }
    }

    // -------------------------------------------------------------------------
    // Panel builders
    // -------------------------------------------------------------------------

    private JPanel buildMessagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("Online features require a verified email address.");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(header);

        panel.add(Box.createVerticalStrut(8));

        JLabel sent = new JLabel("A verification link was sent to:");
        sent.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sent);

        panel.add(Box.createVerticalStrut(4));

        JLabel emailLabel = new JLabel(email);
        emailLabel.setFont(emailLabel.getFont().deriveFont(Font.BOLD));
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(emailLabel);

        panel.add(Box.createVerticalStrut(8));

        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusLabel);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        resendButton = new JButton("Resend Email");
        resendButton.setName("resend");
        resendButton.addActionListener(e -> doResend());
        panel.add(resendButton);

        JButton closeButton = new JButton("Close");
        closeButton.setName("close");
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);

        return panel;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void doResend() {
        resendButton.setEnabled(false);
        statusLabel.setText(" ");

        Thread t = new Thread(() -> {
            String error = null;
            boolean rateLimited = false;
            try {
                RestAuthClient.getInstance().resendVerification(serverUrl);
            } catch (ResendRateLimitedException ex) {
                rateLimited = true;
                error = ex.getMessage();
            } catch (RestAuthClient.RestAuthException ex) {
                error = ex.getMessage() != null ? ex.getMessage() : "Failed to send. Please try again.";
            }

            final String finalError = error;
            final boolean finalRateLimited = rateLimited;

            SwingUtilities.invokeLater(() -> {
                if (finalError == null) {
                    resendButton.setText("Email Sent \u2713");
                    resendButton.setEnabled(false);
                    statusLabel.setText(" ");
                } else if (finalRateLimited) {
                    resendButton.setEnabled(true);
                    statusLabel.setForeground(Color.DARK_GRAY);
                    statusLabel.setText("Please wait before requesting another email.");
                } else {
                    resendButton.setEnabled(true);
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Failed to send. Please try again.");
                }
            });
        }, "EmailVerificationDialog-Resend");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------------------
    // Accessors (for tests)
    // -------------------------------------------------------------------------

    /** Returns the current text of the resend button. */
    public String getResendButtonText() {
        return resendButton.getText();
    }

    /** Returns whether the resend button is currently enabled. */
    public boolean isResendButtonEnabled() {
        return resendButton.isEnabled();
    }

    /** Returns the current status label text. */
    public String getStatusLabelText() {
        return statusLabel.getText();
    }

    // -------------------------------------------------------------------------
    // Test hooks
    // -------------------------------------------------------------------------

    /**
     * Simulates a successful resend without network I/O or displaying the dialog.
     * Updates the button state as if the server returned success. Used in unit
     * tests running in headless mode.
     */
    public void simulateResendSuccess() {
        resendButton.setText("Email Sent \u2713");
        resendButton.setEnabled(false);
        statusLabel.setText(" ");
    }

    /**
     * Simulates a rate-limited resend response without network I/O. Updates the
     * status label as if the server returned a rate-limit error. Used in unit
     * tests.
     */
    public void simulateResendRateLimited() {
        resendButton.setEnabled(true);
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setText("Please wait before requesting another email.");
    }

    /**
     * Simulates a generic resend failure without network I/O. Used in unit tests.
     */
    public void simulateResendFailure() {
        resendButton.setEnabled(true);
        statusLabel.setForeground(Color.RED);
        statusLabel.setText("Failed to send. Please try again.");
    }
}
