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

import com.donohoedigital.games.config.BaseProfile;
import com.donohoedigital.games.poker.PlayerProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog that lists all player profiles and lets the user pick one, create a
 * new one, or cancel.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * ProfilePickerDialog dialog = new ProfilePickerDialog(parentFrame);
 * dialog.showAndWait();
 * if (dialog.getPickerResult() == ProfilePickerDialog.PickerResult.SELECTED) {
 * 	PlayerProfile selected = dialog.getSelectedProfile();
 * 	// use selected
 * }
 * </pre>
 *
 * <p>
 * For online profiles that are not yet authenticated (no JWT cached),
 * double-clicking shows an inline panel with "Practice" and "Sign In" options.
 */
public class ProfilePickerDialog extends JDialog {

    /** Outcome of the picker dialog. */
    public enum PickerResult {
        SELECTED, CANCELLED, NEW_PROFILE
    }

    private static final Logger logger = LogManager.getLogger(ProfilePickerDialog.class);

    private final Frame ownerFrame;
    private final JList<PlayerProfile> profileList;
    private final DefaultListModel<PlayerProfile> listModel;
    private final JPanel inlineAuthPanel;
    private final JLabel inlineAuthLabel;

    private PickerResult pickerResult = PickerResult.CANCELLED;
    private PlayerProfile selectedProfile;

    /**
     * Constructs the profile picker dialog.
     *
     * @param parent
     *            parent frame; may be null
     */
    public ProfilePickerDialog(Frame parent) {
        super(parent, "Select Profile", true);
        ownerFrame = parent;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        listModel = new DefaultListModel<>();
        loadProfiles();

        profileList = new JList<>(listModel);
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.setCellRenderer(new ProfileCellRenderer());
        profileList.setVisibleRowCount(6);

        profileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = profileList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        handleProfileDoubleClick(listModel.getElementAt(index));
                    }
                }
            }
        });

        // Inline panel shown when an unauthenticated online profile is double-clicked
        inlineAuthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        inlineAuthPanel.setVisible(false);
        inlineAuthLabel = new JLabel("Sign in or practice:");
        inlineAuthPanel.add(inlineAuthLabel);

        JButton practiceBtn = new JButton("Practice");
        practiceBtn.setName("practice");
        practiceBtn.addActionListener(e -> {
            PlayerProfile profile = profileList.getSelectedValue();
            if (profile != null) {
                selectedProfile = profile;
                pickerResult = PickerResult.SELECTED;
                dispose();
            }
        });
        inlineAuthPanel.add(practiceBtn);

        JButton signInBtn = new JButton("Sign In");
        signInBtn.setName("signIn");
        signInBtn.addActionListener(e -> {
            PlayerProfile profile = profileList.getSelectedValue();
            if (profile != null && profile.getServerUrl() != null) {
                StartupScreen screen = new StartupScreen(ownerFrame, profile.getServerUrl(), profile.getName());
                StartupScreen.ScreenResult screenResult = screen.showAndWait();
                if (screenResult == StartupScreen.ScreenResult.AUTHENTICATED) {
                    profile.setJwt(screen.getJwt());
                    profile.setProfileId(screen.getProfileId());
                    selectedProfile = profile;
                    pickerResult = PickerResult.SELECTED;
                    dispose();
                } else if (screenResult == StartupScreen.ScreenResult.PRACTICE) {
                    selectedProfile = profile;
                    pickerResult = PickerResult.SELECTED;
                    dispose();
                }
                // SWITCH_PROFILE: stay in the picker
            }
        });
        inlineAuthPanel.add(signInBtn);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JScrollPane scroll = new JScrollPane(profileList);
        scroll.setBorder(BorderFactory.createTitledBorder("Profiles"));
        root.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.add(inlineAuthPanel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        JButton newProfileBtn = new JButton("+ New Profile");
        newProfileBtn.setName("newProfile");
        newProfileBtn.addActionListener(e -> {
            pickerResult = PickerResult.NEW_PROFILE;
            dispose();
        });
        buttons.add(newProfileBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setName("cancel");
        cancelBtn.addActionListener(e -> {
            pickerResult = PickerResult.CANCELLED;
            dispose();
        });
        buttons.add(cancelBtn);

        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(420, 280));
        pack();
        if (parent != null) {
            setLocationRelativeTo(parent);
        } else {
            setLocationByPlatform(true);
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void loadProfiles() {
        listModel.clear();
        List<BaseProfile> profiles = PlayerProfile.getProfileList();
        if (profiles != null) {
            for (BaseProfile bp : profiles) {
                if (bp instanceof PlayerProfile pp) {
                    listModel.addElement(pp);
                }
            }
        }
    }

    private void handleProfileDoubleClick(PlayerProfile profile) {
        profileList.setSelectedValue(profile, true);

        if (!profile.isOnline()) {
            // Local profile: select immediately
            selectedProfile = profile;
            pickerResult = PickerResult.SELECTED;
            dispose();
            return;
        }

        // Online profile with a cached JWT: select immediately
        if (profile.getJwt() != null) {
            selectedProfile = profile;
            pickerResult = PickerResult.SELECTED;
            dispose();
            return;
        }

        // Online profile without a JWT: show inline auth options
        String host = profile.getServerUrl() != null ? extractHost(profile.getServerUrl()) : "unknown server";
        inlineAuthLabel.setText("Sign in to " + host + " or practice:");
        inlineAuthPanel.setVisible(true);
        pack();
    }

    private static String extractHost(String url) {
        if (url == null)
            return "";
        String stripped = url;
        if (stripped.startsWith("https://"))
            stripped = stripped.substring("https://".length());
        else if (stripped.startsWith("http://"))
            stripped = stripped.substring("http://".length());
        return stripped;
    }

    // -------------------------------------------------------------------------
    // Custom renderer
    // -------------------------------------------------------------------------

    /**
     * Renders each profile row showing the name, a type badge (Local/Online), an
     * auth status indicator, and the server hostname for online profiles.
     */
    private static class ProfileCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof PlayerProfile profile) {
                StringBuilder sb = new StringBuilder("<html>");
                if (profile.isOnline()) {
                    sb.append("[Online]  <b>").append(htmlEncode(profile.getName())).append("</b>");
                    if (profile.getJwt() != null) {
                        sb.append("  <font color='green'>\u2713</font>"); // check mark
                    } else {
                        sb.append("  <font color='gray'>\uD83D\uDD12</font>"); // lock symbol
                    }
                    String host = profile.getServerUrl() != null ? extractHost(profile.getServerUrl()) : "";
                    if (!host.isEmpty()) {
                        sb.append("  <font color='gray' size='-1'>").append(htmlEncode(host)).append("</font>");
                    }
                } else {
                    sb.append("[Local]  <b>").append(htmlEncode(profile.getName())).append("</b>");
                }
                sb.append("</html>");
                label.setText(sb.toString());
            }

            return label;
        }

        private static String htmlEncode(String s) {
            if (s == null)
                return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        private static String extractHost(String url) {
            if (url == null)
                return "";
            String stripped = url;
            if (stripped.startsWith("https://"))
                stripped = stripped.substring("https://".length());
            else if (stripped.startsWith("http://"))
                stripped = stripped.substring("http://".length());
            return stripped;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Shows the dialog modally and blocks until the user makes a selection.
     *
     * @return the picker result
     */
    public PickerResult showAndWait() {
        setVisible(true); // blocks because the dialog is modal
        return pickerResult;
    }

    /** Returns the picker result after {@link #showAndWait()} returns. */
    public PickerResult getPickerResult() {
        return pickerResult;
    }

    /**
     * Returns the profile selected by the user, or {@code null} if the result is
     * not {@link PickerResult#SELECTED}.
     */
    public PlayerProfile getSelectedProfile() {
        return selectedProfile;
    }

    /**
     * Returns the number of profiles currently shown in the list. Useful for tests.
     */
    public int getProfileCount() {
        return listModel.getSize();
    }

    // -------------------------------------------------------------------------
    // Test hooks
    // -------------------------------------------------------------------------

    /**
     * Simulates the user clicking Cancel without displaying the dialog. Used in
     * unit tests running in headless mode.
     *
     * @return {@link PickerResult#CANCELLED}
     */
    public PickerResult simulateCancelled() {
        pickerResult = PickerResult.CANCELLED;
        return pickerResult;
    }

    /**
     * Simulates selecting a profile by index without displaying the dialog. Used in
     * unit tests.
     *
     * @param index
     *            index into the profile list (0-based)
     * @return {@link PickerResult#SELECTED}, or {@link PickerResult#CANCELLED} if
     *         index is out of range
     */
    public PickerResult simulateSelect(int index) {
        if (index >= 0 && index < listModel.getSize()) {
            selectedProfile = listModel.getElementAt(index);
            pickerResult = PickerResult.SELECTED;
        } else {
            pickerResult = PickerResult.CANCELLED;
        }
        return pickerResult;
    }

    /**
     * Simulates clicking "+ New Profile" without displaying the dialog.
     *
     * @return {@link PickerResult#NEW_PROFILE}
     */
    public PickerResult simulateNewProfile() {
        pickerResult = PickerResult.NEW_PROFILE;
        return pickerResult;
    }

    /**
     * Returns the profiles loaded into the list. Used for test assertions.
     */
    public List<PlayerProfile> getLoadedProfiles() {
        List<PlayerProfile> result = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            result.add(listModel.getElementAt(i));
        }
        return result;
    }
}
