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

import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.donohoedigital.games.poker.online.OnlineServerUrl;
import com.donohoedigital.games.poker.online.RestAuthClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Multi-panel first-run wizard shown when no player profiles exist.
 *
 * <p>
 * Panel flow:
 * <ol>
 * <li>Welcome — choose local or online path</li>
 * <li>Local Profile Creation — enter a name</li>
 * <li>Online Server Configuration — enter host and port</li>
 * <li>Register / Login — tab-based panel to create or access an online
 * account</li>
 * </ol>
 *
 * <p>
 * Call {@link #showAndWait()} to display the dialog and block until the user
 * completes or cancels. Inspect {@link #getResult()} afterward.
 */
public class FirstRunWizard extends JDialog {

    /** Outcome of the wizard. */
    public enum WizardResult {
        CANCELLED, LOCAL_PROFILE_CREATED, ONLINE_PROFILE_CREATED
    }

    private static final Logger logger = LogManager.getLogger(FirstRunWizard.class);

    // Panel identifiers used with CardLayout
    private static final String PANEL_WELCOME = "WELCOME";
    private static final String PANEL_LOCAL = "LOCAL";
    private static final String PANEL_SERVER = "SERVER";
    private static final String PANEL_REGISTER_LOGIN = "REGISTER_LOGIN";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // Local profile panel state
    private JTextField localNameField;

    // Server config panel state
    private JTextField serverHostField;
    private JTextField serverPortField;

    // Register tab state
    private JTextField regUsernameField;
    private JTextField regEmailField;
    private JPasswordField regPasswordField;
    private JLabel regErrorLabel;

    // Login tab state
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JLabel loginErrorLabel;

    private WizardResult result = WizardResult.CANCELLED;

    // Created profile — set when LOCAL_PROFILE_CREATED
    private PlayerProfile createdProfile;

    // Online session info — set when ONLINE_PROFILE_CREATED
    private String onlineServerUrl;
    private String onlineUsername;
    private String onlineJwt;
    private Long onlineProfileId;
    private String onlineEmail;

    /**
     * Constructs the wizard.
     *
     * @param parent
     *            parent frame; may be null
     */
    public FirstRunWizard(Frame parent) {
        super(parent, "DD Poker — Welcome", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(cardPanel, BorderLayout.CENTER);
        setContentPane(root);

        cardPanel.add(buildWelcomePanel(), PANEL_WELCOME);
        cardPanel.add(buildLocalPanel(), PANEL_LOCAL);
        cardPanel.add(buildServerPanel(), PANEL_SERVER);
        cardPanel.add(buildRegisterLoginPanel(), PANEL_REGISTER_LOGIN);

        cardLayout.show(cardPanel, PANEL_WELCOME);
        pack();
        setMinimumSize(new Dimension(440, 320));
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

    private JPanel buildWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Welcome to DD Poker");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("<html><center>How would you like to play?</center></html>",
                SwingConstants.CENTER);
        panel.add(subtitle, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));

        JButton localButton = new JButton("Play Locally");
        localButton.addActionListener(e -> cardLayout.show(cardPanel, PANEL_LOCAL));
        buttons.add(localButton);

        JButton onlineButton = new JButton("Play Online");
        onlineButton.addActionListener(e -> cardLayout.show(cardPanel, PANEL_SERVER));
        buttons.add(onlineButton);

        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonWrapper.add(buttons);
        panel.add(buttonWrapper, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildLocalPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Create a Local Profile");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        localNameField = new JTextField(20);
        localNameField.setToolTipText("Enter your name");
        form.add(localNameField, gbc);

        panel.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, PANEL_WELCOME));
        buttons.add(backButton);

        JButton createButton = new JButton("Create Profile");
        createButton.addActionListener(e -> doCreateLocalProfile());
        buttons.add(createButton);

        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildServerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Online Server Configuration");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Server:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        serverHostField = new JTextField("poker.yourdomain.com", 22);
        form.add(serverHostField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        serverPortField = new JTextField("8877", 8);
        form.add(serverPortField, gbc);

        panel.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, PANEL_WELCOME));
        buttons.add(backButton);

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(e -> cardLayout.show(cardPanel, PANEL_REGISTER_LOGIN));
        buttons.add(nextButton);

        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRegisterLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Register", buildRegisterTab());
        tabs.addTab("Login", buildLoginTab());
        panel.add(tabs, BorderLayout.CENTER);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, PANEL_SERVER));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomBar.add(backButton);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildRegisterTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        regUsernameField = new JTextField(18);
        form.add(regUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        regEmailField = new JTextField(18);
        form.add(regEmailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        regPasswordField = new JPasswordField(18);
        form.add(regPasswordField, gbc);

        tab.add(form, BorderLayout.CENTER);

        regErrorLabel = new JLabel(" ");
        regErrorLabel.setForeground(Color.RED);

        JButton registerButton = new JButton("Create Account");
        registerButton.addActionListener(e -> doRegister());

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.add(regErrorLabel, BorderLayout.NORTH);
        south.add(registerButton, BorderLayout.SOUTH);
        tab.add(south, BorderLayout.SOUTH);

        return tab;
    }

    private JPanel buildLoginTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        loginUsernameField = new JTextField(18);
        form.add(loginUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        loginPasswordField = new JPasswordField(18);
        form.add(loginPasswordField, gbc);

        tab.add(form, BorderLayout.CENTER);

        loginErrorLabel = new JLabel(" ");
        loginErrorLabel.setForeground(Color.RED);

        JButton signInButton = new JButton("Sign In");
        signInButton.addActionListener(e -> doLogin());

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.add(loginErrorLabel, BorderLayout.NORTH);
        south.add(signInButton, BorderLayout.SOUTH);
        tab.add(south, BorderLayout.SOUTH);

        return tab;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void doCreateLocalProfile() {
        String name = localNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a name.", "Name required", JOptionPane.WARNING_MESSAGE);
            localNameField.requestFocus();
            return;
        }

        PlayerProfile profile = new PlayerProfile(name);
        profile.initCheck();
        try {
            profile.initFile();
            profile.save();
        } catch (Exception ex) {
            logger.warn("Failed to save new local profile", ex);
            JOptionPane.showMessageDialog(this, "Failed to save profile: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        createdProfile = profile;
        result = WizardResult.LOCAL_PROFILE_CREATED;
        dispose();
    }

    private void doRegister() {
        regErrorLabel.setText(" ");

        String username = regUsernameField.getText().trim();
        String email = regEmailField.getText().trim();
        String password = new String(regPasswordField.getPassword());

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            regErrorLabel.setText("All fields are required.");
            return;
        }

        String serverUrl = buildServerUrl();
        if (serverUrl == null) {
            regErrorLabel.setText("Invalid server address.");
            return;
        }

        setRegisterPanelEnabled(false);

        Thread t = new Thread(() -> {
            LoginResponse resp;
            String error = null;
            try {
                resp = RestAuthClient.getInstance().register(serverUrl, username, password, email);
            } catch (RestAuthClient.RestAuthException ex) {
                error = ex.getMessage() != null ? ex.getMessage() : "Registration failed.";
                resp = null;
            }

            final LoginResponse finalResp = resp;
            final String finalError = error;

            SwingUtilities.invokeLater(() -> {
                setRegisterPanelEnabled(true);
                if (finalError != null) {
                    regErrorLabel.setText(finalError);
                    return;
                }

                onlineServerUrl = serverUrl;
                onlineUsername = username;
                onlineJwt = finalResp.token();
                onlineProfileId = finalResp.profileId();
                onlineEmail = email;
                result = WizardResult.ONLINE_PROFILE_CREATED;

                if (!finalResp.emailVerified()) {
                    JOptionPane.showMessageDialog(FirstRunWizard.this,
                            "A verification email has been sent to " + email
                                    + ".\nVerify your email to access online features.",
                            "Verify Email", JOptionPane.INFORMATION_MESSAGE);
                }

                dispose();
            });
        }, "FirstRunWizard-Register");
        t.setDaemon(true);
        t.start();
    }

    private void doLogin() {
        loginErrorLabel.setText(" ");

        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            loginErrorLabel.setText("Username and password are required.");
            return;
        }

        String serverUrl = buildServerUrl();
        if (serverUrl == null) {
            loginErrorLabel.setText("Invalid server address.");
            return;
        }

        setLoginPanelEnabled(false);

        Thread t = new Thread(() -> {
            LoginResponse resp;
            String error = null;
            try {
                resp = RestAuthClient.getInstance().login(serverUrl, username, password);
            } catch (RestAuthClient.RestAuthException ex) {
                error = ex.getMessage() != null ? ex.getMessage() : "Login failed.";
                resp = null;
            }

            final LoginResponse finalResp = resp;
            final String finalError = error;

            SwingUtilities.invokeLater(() -> {
                setLoginPanelEnabled(true);
                if (finalError != null) {
                    loginErrorLabel.setText(finalError);
                    return;
                }

                onlineServerUrl = serverUrl;
                onlineUsername = username;
                onlineJwt = finalResp.token();
                onlineProfileId = finalResp.profileId();
                onlineEmail = finalResp.email();
                result = WizardResult.ONLINE_PROFILE_CREATED;
                dispose();
            });
        }, "FirstRunWizard-Login");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the server base URL from the host/port fields. Returns null if the
     * configured value is invalid.
     */
    private String buildServerUrl() {
        String host = serverHostField.getText().trim();
        String port = serverPortField.getText().trim();
        if (host.isEmpty()) {
            return null;
        }
        String raw = port.isEmpty() ? host : host + ":" + port;
        return OnlineServerUrl.normalizeBaseUrl(raw);
    }

    private void setRegisterPanelEnabled(boolean enabled) {
        regUsernameField.setEnabled(enabled);
        regEmailField.setEnabled(enabled);
        regPasswordField.setEnabled(enabled);
    }

    private void setLoginPanelEnabled(boolean enabled) {
        loginUsernameField.setEnabled(enabled);
        loginPasswordField.setEnabled(enabled);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Shows the wizard and blocks until the user completes or cancels.
     *
     * @return the wizard result
     */
    public WizardResult showAndWait() {
        setVisible(true); // blocks because the dialog is modal
        return result;
    }

    /** Returns the result after {@link #showAndWait()} returns. */
    public WizardResult getResult() {
        return result;
    }

    /** Returns the local profile created, or {@code null} if not applicable. */
    public PlayerProfile getCreatedProfile() {
        return createdProfile;
    }

    /**
     * Returns the online server URL after a successful online flow, or
     * {@code null}.
     */
    public String getOnlineServerUrl() {
        return onlineServerUrl;
    }

    /**
     * Returns the online username after a successful online flow, or {@code null}.
     */
    public String getOnlineUsername() {
        return onlineUsername;
    }

    /** Returns the JWT token after a successful online flow, or {@code null}. */
    public String getOnlineJwt() {
        return onlineJwt;
    }

    /**
     * Returns the server-side profile ID after a successful online flow, or
     * {@code null}.
     */
    public Long getOnlineProfileId() {
        return onlineProfileId;
    }

    /**
     * Returns the email address after a successful online flow, or {@code null}.
     */
    public String getOnlineEmail() {
        return onlineEmail;
    }

    // -------------------------------------------------------------------------
    // Test hooks
    // -------------------------------------------------------------------------

    /**
     * Simulates creating a local profile without displaying the dialog. Used in
     * unit tests running in headless mode.
     *
     * @param name
     *            profile name to use
     * @return {@link WizardResult#LOCAL_PROFILE_CREATED}
     */
    public WizardResult simulateLocalProfileCreation(String name) {
        PlayerProfile profile = new PlayerProfile(name);
        profile.initCheck();
        createdProfile = profile;
        result = WizardResult.LOCAL_PROFILE_CREATED;
        return result;
    }

    /**
     * Simulates a successful online login without network I/O or displaying the
     * dialog. Used in unit tests.
     *
     * @param serverUrl
     *            server base URL
     * @param username
     *            username
     * @param jwt
     *            JWT token
     * @param profileId
     *            server-side profile ID
     * @param email
     *            email address
     * @return {@link WizardResult#ONLINE_PROFILE_CREATED}
     */
    public WizardResult simulateOnlineLogin(String serverUrl, String username, String jwt, Long profileId,
            String email) {
        onlineServerUrl = serverUrl;
        onlineUsername = username;
        onlineJwt = jwt;
        onlineProfileId = profileId;
        onlineEmail = email;
        result = WizardResult.ONLINE_PROFILE_CREATED;
        return result;
    }
}
