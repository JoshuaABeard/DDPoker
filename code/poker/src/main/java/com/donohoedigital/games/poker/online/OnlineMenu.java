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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 * Online menu phase — entry point for the online play section. Shows the
 * current player profile and provides navigation to host or join online games.
 */
public class OnlineMenu extends MenuPhase {

    PlayerProfile profile_;

    public void init(GameEngine engine, GameContext context, GamePhase gamephase) {
        super.init(engine, context, gamephase);

        // current profile status
        DDPanel gbox = new DDPanel();
        gbox.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        gbox.setBorderLayoutGap(-4, 10);
        menu_.getMenuBox().add(GuiUtils.CENTER(gbox), BorderLayout.NORTH);
        profile_ = PlayerProfileOptions.getDefaultProfile();

        // add profile
        EngineButtonListener listener = new EngineButtonListener(context_, this,
                gamephase_.getButtonNameFromParam("profile"));
        DDImageButton button = new DDImageButton(listener.getGameButton().getName());
        button.addActionListener(listener);
        gbox.add(button, BorderLayout.WEST);

        DDPanel profilepanel = new DDPanel();
        profilepanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, 3, VerticalFlowLayout.LEFT));
        gbox.add(profilepanel, BorderLayout.CENTER);

        DDLabel label = new DDLabel(GuiManager.DEFAULT, "StartMenuSmall");
        String profileText = PropertyConfig.getMessage("msg.onlinemenu.profile", Utils.encodeHTML(profile_.getName()));
        label.setText(profileText);
        profilepanel.add(label);

        label = new DDLabel("onlinemenu.enabled", "StartMenuSmall");
        profilepanel.add(label);
    }

    @Override
    public void start() {
        if (!RestAuthClient.getInstance().hasSession()) {
            showLoginDialog();
            return;
        }
        super.start();
    }

    /**
     * Shows a username/password credential dialog. If the user enters credentials
     * and clicks OK, fires a background thread to attempt login. On success,
     * re-invokes {@link #start()} (which will now find an active session and show
     * the menu). On failure, shows an error and lets the user try again (by calling
     * showLoginDialog again) or cancel back to StartMenu.
     */
    private void showLoginDialog() {
        String node = Prefs.NODE_OPTIONS + engine_.getPrefsNodeName();
        String server = Prefs.getUserPrefs(node).get(EngineConstants.OPTION_ONLINE_SERVER, "");
        String baseUrl = OnlineServerUrl.normalizeBaseUrl(server);

        if (baseUrl == null) {
            JOptionPane.showMessageDialog(null, PropertyConfig.getMessage("msg.online.noserver"),
                    PropertyConfig.getMessage("msg.online.login.title"), JOptionPane.WARNING_MESSAGE);
            context_.processPhase("StartMenu");
            return;
        }

        JTextField userField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        int result = JOptionPane.showConfirmDialog(null,
                new Object[]{PropertyConfig.getMessage("msg.online.login.prompt", baseUrl),
                        PropertyConfig.getMessage("msg.online.login.username"), userField,
                        PropertyConfig.getMessage("msg.online.login.password"), passField},
                PropertyConfig.getMessage("msg.online.login.title"), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            context_.processPhase("StartMenu");
            return;
        }

        // Capture credentials before handing off to background thread
        final String capturedBaseUrl = baseUrl;
        final String username = userField.getText().trim();
        final char[] password = passField.getPassword();

        // Fire login off the EDT
        Thread t = new Thread(() -> {
            try {
                RestAuthClient.getInstance().login(capturedBaseUrl, username, new String(password));
                SwingUtilities.invokeLater(this::start); // session is now cached — start() will call super.start()
            } catch (RestAuthClient.RestAuthException ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, ex.getMessage(),
                            PropertyConfig.getMessage("msg.online.login.title"), JOptionPane.ERROR_MESSAGE);
                    showLoginDialog(); // retry: show the dialog again
                });
            }
        }, "OnlineMenu-Login");
        t.setDaemon(true);
        t.start();
    }

    // No processButton override needed - no lobby check in new architecture
}
