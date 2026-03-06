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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.protocol.dto.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

import org.apache.logging.log4j.*;

/**
 * Account management phase -- allows the logged-in user to view their profile,
 * change their password, and request an email address change. Reached from the
 * OnlineMenu via the "account" button.
 */
public class AccountManagement extends MenuPhase {

    private static final Logger logger = LogManager.getLogger(AccountManagement.class);

    private DDLabel usernameLabel_;
    private DDLabel emailLabel_;
    private DDLabel verifiedLabel_;
    private DDLabel memberSinceLabel_;

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase) {
        super.init(engine, context, gamephase);

        // Profile info panel above the help text area
        DDPanel infoBox = new DDPanel();
        infoBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoBox.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 6, VerticalFlowLayout.LEFT));

        usernameLabel_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        usernameLabel_.setText(PropertyConfig.getMessage("msg.account.loading"));
        infoBox.add(usernameLabel_);

        emailLabel_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        infoBox.add(emailLabel_);

        verifiedLabel_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        infoBox.add(verifiedLabel_);

        memberSinceLabel_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        infoBox.add(memberSinceLabel_);

        centerPanel_.add(infoBox, BorderLayout.NORTH);
    }

    @Override
    public void start() {
        super.start();
        loadProfile();
    }

    /**
     * Fetches the current user profile in a background thread and updates the
     * labels on the EDT.
     */
    private void loadProfile() {
        RestAuthClient auth = RestAuthClient.getInstance();
        String serverUrl = auth.getCachedServerUrl();
        String jwt = auth.getCachedJwt();

        if (serverUrl == null || jwt == null) {
            usernameLabel_.setText(PropertyConfig.getMessage("msg.account.notloggedin"));
            return;
        }

        Thread t = new Thread(() -> {
            try {
                ProfileResponse profile = auth.getCurrentUser(serverUrl, jwt);
                SwingUtilities.invokeLater(() -> updateProfileDisplay(profile));
            } catch (RestAuthClient.RestAuthException ex) {
                logger.warn("Failed to load profile: {}", ex.getMessage());
                SwingUtilities.invokeLater(() -> usernameLabel_
                        .setText(PropertyConfig.getMessage("msg.account.loaderror", ex.getMessage())));
            }
        }, "AccountManagement-LoadProfile");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Updates the profile display labels with the fetched data.
     */
    private void updateProfileDisplay(ProfileResponse profile) {
        usernameLabel_.setText(PropertyConfig.getMessage("msg.account.username", profile.username()));
        emailLabel_.setText(PropertyConfig.getMessage("msg.account.email", profile.email()));
        verifiedLabel_.setText(PropertyConfig
                .getMessage(profile.emailVerified() ? "msg.account.verified.yes" : "msg.account.verified.no"));
        if (profile.createDate() != null) {
            memberSinceLabel_.setText(PropertyConfig.getMessage("msg.account.membersince", profile.createDate()));
        }
    }

    @Override
    public boolean processButton(GameButton button) {
        if (button.getName().equals("changepassword")) {
            showChangePasswordDialog();
            return false;
        }
        if (button.getName().equals("changeemail")) {
            showChangeEmailDialog();
            return false;
        }
        return true;
    }

    /**
     * Shows a dialog to change the user's password. Collects old password, new
     * password, and confirmation, then fires the change in a background thread.
     */
    private void showChangePasswordDialog() {
        JPasswordField oldPassField = new JPasswordField(20);
        JPasswordField newPassField = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);

        Object[] message = {PropertyConfig.getMessage("msg.account.chpass.old"), oldPassField,
                PropertyConfig.getMessage("msg.account.chpass.new"), newPassField,
                PropertyConfig.getMessage("msg.account.chpass.confirm"), confirmField};

        int result = JOptionPane.showConfirmDialog(context_.getFrame(), message,
                PropertyConfig.getMessage("msg.account.chpass.title"), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String oldPassword = new String(oldPassField.getPassword());
        String newPassword = new String(newPassField.getPassword());
        String confirmPassword = new String(confirmField.getPassword());

        if (newPassword.isEmpty()) {
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.account.chpass.empty"));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.account.chpass.mismatch"));
            return;
        }

        RestAuthClient auth = RestAuthClient.getInstance();
        String serverUrl = auth.getCachedServerUrl();
        String jwt = auth.getCachedJwt();

        Thread t = new Thread(() -> {
            try {
                // Fetch profile to get the profile ID
                ProfileResponse profile = auth.getCurrentUser(serverUrl, jwt);
                auth.changePassword(serverUrl, jwt, profile.id(), oldPassword, newPassword);
                SwingUtilities.invokeLater(() -> EngineUtils.displayInformationDialog(context_,
                        PropertyConfig.getMessage("msg.account.chpass.success")));
            } catch (RestAuthClient.RestAuthException ex) {
                SwingUtilities.invokeLater(() -> EngineUtils.displayInformationDialog(context_,
                        PropertyConfig.getMessage("msg.account.chpass.error", ex.getMessage())));
            }
        }, "AccountManagement-ChangePassword");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Shows a dialog to request an email address change. Collects the new email,
     * then fires the request in a background thread.
     */
    private void showChangeEmailDialog() {
        JTextField emailField = new JTextField(20);

        Object[] message = {PropertyConfig.getMessage("msg.account.chemail.prompt"), emailField};

        int result = JOptionPane.showConfirmDialog(context_.getFrame(), message,
                PropertyConfig.getMessage("msg.account.chemail.title"), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String newEmail = emailField.getText().trim();
        if (newEmail.isEmpty()) {
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.account.chemail.empty"));
            return;
        }

        RestAuthClient auth = RestAuthClient.getInstance();
        String serverUrl = auth.getCachedServerUrl();

        Thread t = new Thread(() -> {
            try {
                auth.requestEmailChange(serverUrl, newEmail);
                SwingUtilities.invokeLater(() -> EngineUtils.displayInformationDialog(context_,
                        PropertyConfig.getMessage("msg.account.chemail.success")));
            } catch (RestAuthClient.RestAuthException ex) {
                SwingUtilities.invokeLater(() -> EngineUtils.displayInformationDialog(context_,
                        PropertyConfig.getMessage("msg.account.chemail.error", ex.getMessage())));
            }
        }, "AccountManagement-ChangeEmail");
        t.setDaemon(true);
        t.start();
    }
}
