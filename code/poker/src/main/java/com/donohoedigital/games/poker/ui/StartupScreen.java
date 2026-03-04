/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker.ui;

import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.online.RestAuthClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * Startup screen shown when the last-used profile is an online profile.
 *
 * <p>
 * Offers three actions:
 * <ul>
 * <li><b>Practice</b> — skip authentication and go directly to the main
 * menu</li>
 * <li><b>Sign In</b> — authenticate with the server before proceeding</li>
 * <li><b>Switch / New Profile</b> — go to the profile picker</li>
 * </ul>
 *
 * <p>
 * Call {@link #showAndWait()} to display the dialog modally. The result is
 * available via {@link #getResult()} after {@code showAndWait()} returns.
 */
public class StartupScreen extends JDialog {

    /** Outcome of the startup screen. */
    public enum ScreenResult {
        PRACTICE, AUTHENTICATED, SWITCH_PROFILE
    }

    private static final Logger logger = LogManager.getLogger(StartupScreen.class);

    private final String serverUrl;
    private final String username;
    private final String profileName;

    private JPasswordField passwordField;
    private JCheckBox rememberMeCheckBox;
    private JLabel errorLabel;
    private JButton signInButton;

    private ScreenResult result = ScreenResult.SWITCH_PROFILE;

    // After a successful sign-in these are populated
    private String jwt;
    private Long profileId;
    private String email;

    /**
     * Constructs the startup screen.
     *
     * @param parent
     *            parent frame; may be null
     * @param serverUrl
     *            the base URL of the server (e.g. {@code http://localhost:8877})
     * @param username
     *            the username of the last-used online profile
     * @param profileName
     *            the local profile name, used to locate the persisted JWT
     */
    public StartupScreen(Frame parent, String serverUrl, String username, String profileName) {
        super(parent, "DD Poker", true);
        this.serverUrl = serverUrl;
        this.username = username;
        this.profileName = profileName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildSignInPanel(), BorderLayout.CENTER);
        root.add(buildFooterPanel(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(360, 260));
        pack();
        if (parent != null) {
            setLocationRelativeTo(parent);
        } else {
            setLocationByPlatform(true);
        }
    }

    /**
     * Constructs the startup screen with {@code profileName} equal to
     * {@code username}. Provided for backward compatibility.
     *
     * @param parent
     *            parent frame; may be null
     * @param serverUrl
     *            the base URL of the server
     * @param username
     *            the username of the last-used online profile
     */
    public StartupScreen(Frame parent, String serverUrl, String username) {
        this(parent, serverUrl, username, username);
    }

    // -------------------------------------------------------------------------
    // Panel builders
    // -------------------------------------------------------------------------

    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel welcomeLabel = new JLabel("Welcome back, " + username);
        welcomeLabel.setFont(welcomeLabel.getFont().deriveFont(Font.BOLD, 16f));
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(welcomeLabel);

        panel.add(Box.createVerticalStrut(4));

        // Extract hostname from the server URL for display
        String displayHost = extractHost(serverUrl);
        JLabel serverLabel = new JLabel(displayHost);
        serverLabel.setFont(serverLabel.getFont().deriveFont(11f));
        serverLabel.setForeground(Color.GRAY);
        serverLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(serverLabel);

        return panel;
    }

    private JPanel buildSignInPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(18);
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        rememberMeCheckBox = new JCheckBox("Remember me");
        panel.add(rememberMeCheckBox, gbc);

        gbc.gridy = 2;
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        panel.add(errorLabel, gbc);

        gbc.gridy = 3;
        signInButton = new JButton("Sign In");
        signInButton.addActionListener(e -> doSignIn());
        panel.add(signInButton, gbc);

        return panel;
    }

    private JPanel buildFooterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 6));

        JButton practiceButton = new JButton("Practice (skip sign-in)");
        practiceButton.addActionListener(e -> {
            result = ScreenResult.PRACTICE;
            dispose();
        });
        panel.add(practiceButton);

        JButton switchButton = new JButton("Switch / New Profile");
        switchButton.addActionListener(e -> {
            result = ScreenResult.SWITCH_PROFILE;
            dispose();
        });
        panel.add(switchButton);

        return panel;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void doSignIn() {
        errorLabel.setText(" ");
        String password = new String(passwordField.getPassword());
        if (password.isEmpty()) {
            errorLabel.setText("Password is required.");
            return;
        }

        boolean rememberMe = rememberMeCheckBox.isSelected();
        setInputEnabled(false);

        Thread t = new Thread(() -> {
            LoginResponse resp;
            String error = null;
            try {
                resp = RestAuthClient.getInstance().login(serverUrl, username, password, rememberMe);
            } catch (RestAuthClient.RestAuthException ex) {
                error = ex.getMessage() != null ? ex.getMessage() : "Invalid username or password";
                resp = null;
            }

            final LoginResponse finalResp = resp;
            final String finalError = error;

            SwingUtilities.invokeLater(() -> {
                setInputEnabled(true);
                if (finalError != null) {
                    errorLabel.setText("Invalid username or password");
                    return;
                }

                jwt = finalResp.token();
                profileId = finalResp.profileId();
                email = finalResp.email();
                if (rememberMe) {
                    RestAuthClient.getInstance().persistJwt(profileName, jwt);
                }
                result = ScreenResult.AUTHENTICATED;
                dispose();
            });
        }, "StartupScreen-SignIn");
        t.setDaemon(true);
        t.start();
    }

    private void setInputEnabled(boolean enabled) {
        passwordField.setEnabled(enabled);
        rememberMeCheckBox.setEnabled(enabled);
        signInButton.setEnabled(enabled);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String extractHost(String url) {
        if (url == null)
            return "";
        // Strip scheme
        String stripped = url;
        if (stripped.startsWith("https://")) {
            stripped = stripped.substring("https://".length());
        } else if (stripped.startsWith("http://")) {
            stripped = stripped.substring("http://".length());
        }
        return stripped;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Shows the dialog modally and blocks until the user makes a selection.
     *
     * <p>
     * Before displaying the dialog, attempts silent re-authentication using a
     * persisted JWT. If the JWT is valid the dialog is dismissed automatically and
     * {@link ScreenResult#AUTHENTICATED} is returned. The check is performed on a
     * background thread to avoid blocking the EDT.
     *
     * @return the screen result
     */
    public ScreenResult showAndWait() {
        // Attempt silent re-auth from a persisted JWT before showing the dialog.
        Optional<String> saved = RestAuthClient.getInstance().loadPersistedJwt(profileName);
        if (saved.isPresent()) {
            final String savedJwt = saved.get();
            setInputEnabled(false); // disable UI while checking in the background
            new Thread(() -> {
                try {
                    RestAuthClient.getInstance().setCachedJwt(savedJwt);
                    RestAuthClient.getInstance().getCurrentUser(serverUrl, savedJwt); // off-EDT
                    // Silent re-auth succeeded — store JWT and close the dialog.
                    SwingUtilities.invokeLater(() -> {
                        jwt = savedJwt;
                        result = ScreenResult.AUTHENTICATED;
                        dispose();
                    });
                } catch (RestAuthClient.RestAuthException e) {
                    logger.debug("Persisted JWT invalid or expired, clearing: {}", e.getMessage());
                    RestAuthClient.getInstance().clearPersistedJwt(profileName);
                    RestAuthClient.getInstance().clearSession();
                    SwingUtilities.invokeLater(() -> setInputEnabled(true)); // show the sign-in form
                }
            }, "startup-jwt-check").start();
            super.setVisible(true); // show dialog (inputs disabled) and block until disposed or enabled
            return result;
        }

        setVisible(true); // blocks because the dialog is modal
        return result;
    }

    /** Returns the result after {@link #showAndWait()} returns. */
    public ScreenResult getResult() {
        return result;
    }

    /** Returns the JWT token after a successful sign-in, or {@code null}. */
    public String getJwt() {
        return jwt;
    }

    /**
     * Returns the server-side profile ID after a successful sign-in, or
     * {@code null}.
     */
    public Long getProfileId() {
        return profileId;
    }

    /** Returns the email address after a successful sign-in, or {@code null}. */
    public String getEmail() {
        return email;
    }

    // -------------------------------------------------------------------------
    // Test hooks
    // -------------------------------------------------------------------------

    /**
     * Simulates selecting the Practice option without displaying the dialog. Used
     * in unit tests running in headless mode.
     *
     * @return {@link ScreenResult#PRACTICE}
     */
    public ScreenResult simulatePracticeSelected() {
        result = ScreenResult.PRACTICE;
        return result;
    }

    /**
     * Simulates a successful sign-in without network I/O or displaying the dialog.
     * Used in unit tests.
     *
     * @param jwtToken
     *            JWT token from the server
     * @param id
     *            server-side profile ID
     * @param userEmail
     *            user's email address
     * @return {@link ScreenResult#AUTHENTICATED}
     */
    public ScreenResult simulateSignIn(String jwtToken, Long id, String userEmail) {
        jwt = jwtToken;
        profileId = id;
        email = userEmail;
        result = ScreenResult.AUTHENTICATED;
        return result;
    }
}
