/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
/*
 * ChangePasswordDialog.java
 *
 * Created on January 25, 2003, 10:11 AM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.beans.*;

/**
 * @author Doug Donohoe
 */
public class ChangePasswordDialog extends DialogPhase implements PropertyChangeListener {
    static Logger logger = LogManager.getLogger(ChangePasswordDialog.class);

    private PlayerProfile profile_;

    private TablePanel.TextWidgets currentText_ = null;
    private TablePanel.TextWidgets newText_ = null;

    /**
     * create chat ui
     */
    @Override
    public JComponent createDialogContents() {
        // Use original profile information to determine how to update values.
        profile_ = (PlayerProfile) gamephase_.getObject(ProfileList.PARAM_PROFILE);
        ApplicationError.assertNotNull(profile_, "No 'profile' in params");

        // add fields
        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        TablePanel panel = new TablePanel();
        base.add(panel, BorderLayout.NORTH);

        currentText_ = panel.addTextField("currentpassword", null, STYLE, 16, null, null, null);
        newText_ = panel.addTextField("newpassword", null, STYLE, 16, null, null, null);

        // add listeners
        currentText_.text_.addPropertyChangeListener(this);
        newText_.text_.addPropertyChangeListener(this);

        checkButtons();

        return base;
    }

    /**
     * Focus to text field
     */
    @Override
    protected Component getFocusComponent() {
        return currentText_.text_;
    }

    /**
     * Closes the dialog unless an error occurs saving the profile information
     */
    @Override
    public boolean processButton(GameButton button) {
        boolean bResult = false;
        boolean bSuccess = true;

        if (button.getName().equals(okayButton_.getName())) {
            String serverUrl = getServerUrl();
            String jwt = profile_.getJwt();
            Long profileId = profile_.getProfileId();

            if (serverUrl == null || serverUrl.isEmpty() || jwt == null || profileId == null) {
                logger.warn("Cannot change password: not connected to server (serverUrl={}, jwt={})", serverUrl,
                        jwt != null ? "set" : "null");
                bSuccess = false;
            } else {
                try {
                    RestAuthClient.getInstance().changePassword(serverUrl, jwt, profileId, currentText_.getText(),
                            newText_.getText());

                    // update local profile values
                    profile_.setPassword(newText_.getText());
                    bResult = true;
                } catch (RestAuthClient.RestAuthException e) {
                    logger.warn("Change password failed: {}", e.getMessage());
                    EngineUtils.displayInformationDialog(context_, e.getMessage());
                    bSuccess = false;
                }
            }
        }

        if (bSuccess) {
            removeDialog();
        }

        setResult(bResult);

        return bSuccess;
    }

    /**
     * msg text change
     */
    public void propertyChange(PropertyChangeEvent evt) {
        checkButtons();
    }

    /**
     * Enable buttons
     */
    private void checkButtons() {
        boolean bEnabled = false;

        bEnabled = profile_.isMatchingPassword(currentText_.getText());

        if (bEnabled) {
            bEnabled = newText_.getText().length() > 0;
        }

        okayButton_.setEnabled(bEnabled);
    }

    /**
     * Resolve the REST server URL from the configured online server preference.
     */
    private String getServerUrl() {
        try {
            String node = Prefs.NODE_OPTIONS + PokerMain.getPokerMain().getPrefsNodeName();
            String server = Prefs.getUserPrefs(node).get(EngineConstants.OPTION_ONLINE_SERVER, "");
            if (server == null || server.isEmpty()) {
                return null;
            }
            return "http://" + server;
        } catch (Exception e) {
            logger.warn("Could not get server URL from preferences", e);
            return null;
        }
    }
}
