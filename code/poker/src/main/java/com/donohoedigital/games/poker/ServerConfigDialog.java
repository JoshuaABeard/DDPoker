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
 * For the full LICENSE text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 * First-run server configuration dialog
 */
public class ServerConfigDialog extends DialogPhase
{
    private static final String ONLINE_SERVER_REGEXP =
        "^(?:localhost|(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}|(?:\\d{1,3}\\.){3}\\d{1,3}):\\d{1,5}$";

    private DDTextField serverField;
    private DDTextField chatField;

    /**
     * Create dialog contents
     */
    @Override
    public JComponent createDialogContents()
    {
        DDPanel base = new DDPanel();
        base.setLayout(new GridBagLayout());
        base.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Welcome message
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        DDLabel welcome = new DDLabel(STYLE);
        welcome.setText(PropertyConfig.getMessage("msg.serverconfigwelcome"));
        base.add(welcome, gbc);

        // Info message
        gbc.gridy = 1;
        DDLabel info = new DDLabel(STYLE);
        info.setText(PropertyConfig.getMessage("msg.serverconfiginfo"));
        base.add(info, gbc);

        // Spacer
        gbc.gridy = 2;
        base.add(Box.createVerticalStrut(10), gbc);

        // Server field
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        DDLabel serverLabel = new DDLabel(STYLE);
        serverLabel.setText(PropertyConfig.getMessage("msg.serverconfiggame"));
        base.add(serverLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        serverField = new DDTextField(GuiManager.DEFAULT, STYLE);
        serverField.setColumns(30);
        serverField.setText("localhost:8877");
        base.add(serverField, gbc);

        // Chat field
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        DDLabel chatLabel = new DDLabel(STYLE);
        chatLabel.setText(PropertyConfig.getMessage("msg.serverconfigchat"));
        base.add(chatLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        chatField = new DDTextField(GuiManager.DEFAULT, STYLE);
        chatField.setColumns(30);
        chatField.setText("localhost:11886");
        base.add(chatField, gbc);

        return base;
    }

    /**
     * Validate and save the configuration
     */
    public boolean processButton(GameButton button)
    {
        // Only validate on okay button
        if (button.getName().equals("okay"))
        {
            String server = serverField.getText().trim();
            String chat = chatField.getText().trim();

            // Validate
            if (!server.isEmpty() && !chat.isEmpty() &&
                server.matches(ONLINE_SERVER_REGEXP) &&
                chat.matches(ONLINE_SERVER_REGEXP))
            {
                // Save to preferences (use options/ prefix to match where OptionText loads from)
                String node = Prefs.NODE_OPTIONS + PokerMain.getPokerMain().getPrefsNodeName();
                Prefs.getUserPrefs(node).put(EngineConstants.OPTION_ONLINE_SERVER, server);
                Prefs.getUserPrefs(node).put(PokerConstants.OPTION_ONLINE_CHAT, chat);
                Prefs.getUserPrefs(node).putBoolean(EngineConstants.OPTION_ONLINE_ENABLED, true);

                // Flush preferences to ensure they're saved
                try {
                    Prefs.getUserPrefs(node).flush();
                } catch (Exception e) {
                    // Log but don't fail
                }

                // Close dialog
                return super.processButton(button);
            }
            else
            {
                // Show error and keep dialog open
                EngineUtils.displayInformationDialog(context_,
                    PropertyConfig.getMessage("msg.serverconfiginvalid"),
                    PropertyConfig.getMessage("msg.serverconfiginvalid.title"));
                return false;
            }
        }

        // Cancel or other buttons
        return super.processButton(button);
    }

    /**
     * Get focus component
     */
    @Override
    protected Component getFocusComponent()
    {
        return serverField;
    }
}
