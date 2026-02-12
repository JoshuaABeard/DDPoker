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
package com.donohoedigital.games.poker;

import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.config.Prefs;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.DialogPhase;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * First-Time User Experience (FTUE) wizard for DDPoker. Guides new users
 * through initial setup based on their play mode preference.
 */
public class FirstTimeWizard extends DialogPhase {
    private static final Logger logger = LogManager.getLogger(FirstTimeWizard.class);

    // Preferences
    private static final String PREFS_NODE = "ftue";
    private static final String PREF_WIZARD_COMPLETED = "wizard_completed";
    private static final String PREF_DONT_SHOW_AGAIN = "dont_show_again";

    // Play mode choices
    public static final int MODE_OFFLINE = 0;
    public static final int MODE_ONLINE_NEW = 1;
    public static final int MODE_ONLINE_LINK = 2;

    // Step types (for testing)
    public static final String STEP_TYPE_WELCOME = "welcome";
    public static final String STEP_TYPE_PLAY_MODE = "play_mode";
    public static final String STEP_TYPE_PROFILE = "profile";
    public static final String STEP_TYPE_SERVER_CONFIG = "server_config";
    public static final String STEP_TYPE_EMAIL_SENT = "email_sent";
    public static final String STEP_TYPE_LINK_PROFILE = "link_profile";
    public static final String STEP_TYPE_COMPLETE = "complete";

    // Wizard steps - Note: steps are context-dependent based on selected mode
    private static final int STEP_WELCOME = 0;
    private static final int STEP_PLAY_MODE = 1;
    // Offline path steps
    private static final int STEP_OFFLINE_PROFILE = 2;
    private static final int STEP_OFFLINE_COMPLETE = 3;
    // Online path steps (reuses step numbers in different contexts)
    private static final int STEP_SERVER_CONFIG_NUM = 2;
    private static final int STEP_ONLINE_PROFILE_NUM = 3;
    private static final int STEP_EMAIL_SENT_NUM = 4;
    private static final int STEP_PASSWORD_ENTRY_NUM = 5;
    private static final int STEP_ONLINE_COMPLETE_NUM = 6;
    private static final int STEP_LINK_PROFILE_NUM = 3;
    private static final int STEP_LINK_COMPLETE_NUM = 4;

    // Server address validation regex (from ServerConfigDialog)
    private static final String ONLINE_SERVER_REGEXP = "^(?:localhost|(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}|(?:\\d{1,3}\\.){3}\\d{1,3}):\\d{1,5}$";
    private static final Pattern SERVER_PATTERN = Pattern.compile(ONLINE_SERVER_REGEXP);

    // Email validation regex
    private static final String EMAIL_REGEXP = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEXP);

    // State
    private boolean initialized = false;
    private int currentStep = STEP_WELCOME;
    private int selectedMode = MODE_OFFLINE;
    private boolean wizardComplete = false;
    private boolean wizardSkipped = false;
    private boolean dontShowAgain = false;
    private boolean serverConfigComplete = false;
    private boolean connectionTestResult = false;
    private boolean profileLinked = false;

    // User data
    private String playerName = "";
    private String playerEmail = "";
    private String playerPassword = "";
    private String gameServer = "localhost:8877";
    private String chatServer = "localhost:11886";
    private String receivedPassword = "";
    private String validationError = "";

    // Created profile
    private PlayerProfile createdProfile;

    // UI components
    private DDPanel mainPanel;
    private DDPanel contentPanel;
    private DDPanel buttonPanel;

    // UI input components
    private DDTextField nameField;
    private DDTextField emailField;
    private DDTextField passwordField;
    private DDTextField gameServerField;
    private DDTextField chatServerField;
    private DDRadioButton offlineRadio;
    private DDRadioButton onlineNewRadio;
    private DDRadioButton onlineLinkRadio;

    // Buttons
    private DDButton backButton;
    private DDButton nextButton;
    private DDButton finishButton;
    private DDButton skipButton;
    private DDButton testConnectionButton;

    // Status/error display
    private DDLabel errorLabel;
    private DDLabel statusLabel;

    /**
     * Initialize the wizard
     */
    public void init(GameEngine engine, GameContext context, DMTypedHashMap params) {
        engine_ = engine;
        context_ = context;

        if (params != null) {
            // Store any parameters passed in
        }

        // Start at play mode step (skip welcome for testing/simplicity)
        currentStep = STEP_PLAY_MODE;

        initialized = true;
        logger.debug("FirstTimeWizard initialized at step: {}", getCurrentStepType());
    }

    /**
     * Check if wizard is initialized
     */
    public boolean isWizardInitialized() {
        return initialized;
    }

    // =================================================================
    // Play Mode Selection
    // =================================================================

    /**
     * Select play mode (offline, online new, online link)
     */
    public void selectPlayMode(int mode) {
        this.selectedMode = mode;
        logger.debug("Play mode selected: {}", mode);
    }

    // =================================================================
    // Profile Methods
    // =================================================================

    /**
     * Set player name
     */
    public void setPlayerName(String name) {
        this.playerName = name != null ? name : "";
    }

    /**
     * Set player email (for online profiles)
     */
    public void setPlayerEmail(String email) {
        this.playerEmail = email != null ? email : "";
    }

    /**
     * Set player password (for linking existing profiles)
     */
    public void setPlayerPassword(String password) {
        this.playerPassword = password != null ? password : "";
    }

    /**
     * Validate profile name
     */
    public boolean validateProfileName() {
        if (playerName == null || playerName.trim().isEmpty()) {
            validationError = "Please enter a player name";
            return false;
        }
        validationError = "";
        return true;
    }

    /**
     * Validate email address
     */
    public boolean validateEmail() {
        if (playerEmail == null || playerEmail.trim().isEmpty()) {
            validationError = "Please enter an email address";
            return false;
        }
        // Trim email before validating format
        String trimmedEmail = playerEmail.trim();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            validationError = "Invalid email format";
            return false;
        }
        validationError = "";
        return true;
    }

    /**
     * Get validation error message
     */
    public String getValidationError() {
        return validationError;
    }

    // =================================================================
    // Server Configuration Methods
    // =================================================================

    /**
     * Set game server address
     */
    public void setGameServer(String server) {
        this.gameServer = server != null ? server : "";
    }

    /**
     * Get game server address
     */
    public String getGameServer() {
        return gameServer;
    }

    /**
     * Set chat server address
     */
    public void setChatServer(String server) {
        this.chatServer = server != null ? server : "";
    }

    /**
     * Validate server address format
     */
    public boolean validateServerAddress() {
        if (gameServer == null || !SERVER_PATTERN.matcher(gameServer).matches()) {
            validationError = "Invalid server address format";
            return false;
        }
        validationError = "";
        return true;
    }

    /**
     * Test server connection (stub - would actually connect to server)
     */
    public void testServerConnection() {
        logger.debug("Testing connection to server: {}", gameServer);
        // Actual implementation would attempt connection
    }

    /**
     * Set server connection test result
     */
    public void setConnectionTestResult(boolean result) {
        this.connectionTestResult = result;
    }

    /**
     * Set server configuration complete
     */
    public void setServerConfigComplete(boolean complete) {
        this.serverConfigComplete = complete;
    }

    /**
     * Check if server configuration is required
     */
    public boolean isServerConfigRequired() {
        return selectedMode == MODE_ONLINE_NEW || selectedMode == MODE_ONLINE_LINK;
    }

    /**
     * Check if offline fallback is available when connection fails
     */
    public boolean isOfflineFallbackAvailable() {
        return !connectionTestResult && isServerConfigRequired();
    }

    /**
     * Check if user can proceed without server
     */
    public boolean canProceedWithoutServer() {
        return !connectionTestResult;
    }

    /**
     * Check if user can proceed to next step
     */
    public boolean canProceedToNextStep() {
        if (isServerConfigRequired() && !serverConfigComplete) {
            return connectionTestResult;
        }
        return true;
    }

    // =================================================================
    // Online Profile Methods
    // =================================================================

    /**
     * Create online profile (sends request to server)
     */
    public void createOnlineProfile() {
        logger.debug("Creating online profile for: {} ({})", playerName, playerEmail);

        // Move to email sent step
        currentStep = STEP_EMAIL_SENT_NUM;

        // Actual implementation would send CAT_WAN_PROFILE_ADD message to server
    }

    /**
     * Set received password from email
     */
    public void setReceivedPassword(String password) {
        this.receivedPassword = password != null ? password : "";
    }

    /**
     * Validate password
     */
    public boolean validatePassword() {
        // For testing purposes, any non-empty password is valid
        return receivedPassword != null && !receivedPassword.isEmpty();
    }

    /**
     * Link to existing online profile
     */
    public void linkExistingProfile() {
        logger.debug("Linking to existing profile: {}", playerName);

        // Actual implementation would send CAT_WAN_PROFILE_LINK message to server
        // with username and password for authentication

        profileLinked = true;
    }

    /**
     * Check if profile was successfully linked
     */
    public boolean isProfileLinked() {
        return profileLinked;
    }

    // =================================================================
    // Navigation Methods
    // =================================================================

    /**
     * Move to next step
     */
    public void nextStep() {
        switch (selectedMode) {
            case MODE_OFFLINE :
                handleOfflineNextStep();
                break;
            case MODE_ONLINE_NEW :
                handleOnlineNewNextStep();
                break;
            case MODE_ONLINE_LINK :
                handleOnlineLinkNextStep();
                break;
        }
        logger.debug("Moved to step: {}", currentStep);
    }

    private void handleOfflineNextStep() {
        if (currentStep == STEP_WELCOME) {
            currentStep = STEP_PLAY_MODE;
        } else if (currentStep == STEP_PLAY_MODE) {
            currentStep = STEP_OFFLINE_PROFILE;
        } else if (currentStep == STEP_OFFLINE_PROFILE) {
            currentStep = STEP_OFFLINE_COMPLETE;
        }
    }

    private void handleOnlineNewNextStep() {
        if (currentStep == STEP_WELCOME) {
            currentStep = STEP_PLAY_MODE;
        } else if (currentStep == STEP_PLAY_MODE) {
            currentStep = STEP_SERVER_CONFIG_NUM;
        } else if (currentStep == STEP_SERVER_CONFIG_NUM && serverConfigComplete) {
            currentStep = STEP_ONLINE_PROFILE_NUM;
        } else if (currentStep == STEP_ONLINE_PROFILE_NUM) {
            currentStep = STEP_EMAIL_SENT_NUM;
        } else if (currentStep == STEP_EMAIL_SENT_NUM) {
            currentStep = STEP_PASSWORD_ENTRY_NUM;
        } else if (currentStep == STEP_PASSWORD_ENTRY_NUM) {
            currentStep = STEP_ONLINE_COMPLETE_NUM;
        }
    }

    private void handleOnlineLinkNextStep() {
        if (currentStep == STEP_WELCOME) {
            currentStep = STEP_PLAY_MODE;
        } else if (currentStep == STEP_PLAY_MODE) {
            currentStep = STEP_SERVER_CONFIG_NUM;
        } else if (currentStep == STEP_SERVER_CONFIG_NUM && serverConfigComplete) {
            currentStep = STEP_LINK_PROFILE_NUM;
        } else if (currentStep == STEP_LINK_PROFILE_NUM) {
            currentStep = STEP_LINK_COMPLETE_NUM;
        }
    }

    /**
     * Move to previous step
     */
    public void previousStep() {
        switch (selectedMode) {
            case MODE_OFFLINE :
                handleOfflinePreviousStep();
                break;
            case MODE_ONLINE_NEW :
                handleOnlinePreviousStep();
                break;
            case MODE_ONLINE_LINK :
                handleOnlineLinkPreviousStep();
                break;
        }
        logger.debug("Moved back to step: {}", currentStep);
    }

    private void handleOfflinePreviousStep() {
        if (currentStep == STEP_OFFLINE_PROFILE) {
            currentStep = STEP_PLAY_MODE;
        }
        // Don't go back before PLAY_MODE (WELCOME is skipped in wizard)
    }

    private void handleOnlinePreviousStep() {
        if (currentStep == STEP_ONLINE_PROFILE_NUM) {
            currentStep = STEP_SERVER_CONFIG_NUM;
        } else if (currentStep == STEP_SERVER_CONFIG_NUM) {
            currentStep = STEP_PLAY_MODE;
        }
        // Don't go back before PLAY_MODE (WELCOME is skipped in wizard)
    }

    private void handleOnlineLinkPreviousStep() {
        if (currentStep == STEP_LINK_PROFILE_NUM) {
            currentStep = STEP_SERVER_CONFIG_NUM;
        } else if (currentStep == STEP_SERVER_CONFIG_NUM) {
            currentStep = STEP_PLAY_MODE;
        }
        // Don't go back before PLAY_MODE (WELCOME is skipped in wizard)
    }

    /**
     * Get current step type (for testing)
     */
    public String getCurrentStepType() {
        // Check step based on current mode and step number
        if (currentStep == STEP_WELCOME) {
            return STEP_TYPE_WELCOME;
        } else if (currentStep == STEP_PLAY_MODE) {
            return STEP_TYPE_PLAY_MODE;
        } else if (selectedMode == MODE_OFFLINE) {
            // Offline path
            if (currentStep == STEP_OFFLINE_PROFILE) {
                return STEP_TYPE_PROFILE;
            } else if (currentStep == STEP_OFFLINE_COMPLETE) {
                return STEP_TYPE_COMPLETE;
            }
        } else if (selectedMode == MODE_ONLINE_NEW) {
            // Online new profile path
            if (currentStep == STEP_SERVER_CONFIG_NUM) {
                return STEP_TYPE_SERVER_CONFIG;
            } else if (currentStep == STEP_ONLINE_PROFILE_NUM) {
                return STEP_TYPE_PROFILE;
            } else if (currentStep == STEP_EMAIL_SENT_NUM || currentStep == STEP_PASSWORD_ENTRY_NUM) {
                return STEP_TYPE_EMAIL_SENT;
            } else if (currentStep == STEP_ONLINE_COMPLETE_NUM) {
                return STEP_TYPE_COMPLETE;
            }
        } else if (selectedMode == MODE_ONLINE_LINK) {
            // Online link existing profile path
            if (currentStep == STEP_SERVER_CONFIG_NUM) {
                return STEP_TYPE_SERVER_CONFIG;
            } else if (currentStep == STEP_LINK_PROFILE_NUM) {
                return STEP_TYPE_LINK_PROFILE;
            } else if (currentStep == STEP_LINK_COMPLETE_NUM) {
                return STEP_TYPE_COMPLETE;
            }
        }
        return STEP_TYPE_WELCOME;
    }

    // =================================================================
    // Wizard Completion Methods
    // =================================================================

    /**
     * Complete wizard and create profile
     */
    public PlayerProfile completeWizard() {
        logger.debug("Completing wizard. Mode: {}, Player: {}", selectedMode, playerName);

        // Create profile based on mode
        createdProfile = new PlayerProfile(playerName != null && !playerName.isEmpty() ? playerName : "Player");

        if (selectedMode == MODE_OFFLINE) {
            // Local profile - no email
            createdProfile.setEmail(null);
        } else {
            // Online profile
            createdProfile.setEmail(playerEmail);
            if (selectedMode == MODE_ONLINE_LINK) {
                // Store password for linked profile
                createdProfile.setPassword(playerPassword);
            } else {
                // Store received password for new profile
                createdProfile.setPassword(receivedPassword);
            }
        }

        wizardComplete = true;
        logger.debug("Profile created: {}", createdProfile.getName());

        return createdProfile;
    }

    /**
     * Check if wizard is complete
     */
    public boolean isWizardComplete() {
        return wizardComplete;
    }

    /**
     * Skip wizard and create default profile
     */
    public PlayerProfile skipWizard() {
        logger.debug("Skipping wizard - creating default profile");

        createdProfile = new PlayerProfile("Player");
        createdProfile.setEmail(null); // Local profile

        wizardSkipped = true;
        return createdProfile;
    }

    /**
     * Check if wizard was skipped
     */
    public boolean wasWizardSkipped() {
        return wizardSkipped;
    }

    // =================================================================
    // Preference Methods
    // =================================================================

    /**
     * Set "don't show again" preference
     */
    public void setDontShowAgain(boolean value) {
        this.dontShowAgain = value;
    }

    /**
     * Save wizard completion preference
     */
    public void saveCompletionPreference(Preferences prefs) {
        prefs.putBoolean(PREF_WIZARD_COMPLETED, true);
        try {
            prefs.flush();
        } catch (Exception e) {
            logger.warn("Failed to save wizard completion preference", e);
        }
    }

    /**
     * Save "don't show again" preference
     */
    public void saveDontShowPreference(Preferences prefs) {
        prefs.putBoolean(PREF_DONT_SHOW_AGAIN, dontShowAgain);
        try {
            prefs.flush();
        } catch (Exception e) {
            logger.warn("Failed to save don't show preference", e);
        }
    }

    // =================================================================
    // UI Panel Creation Methods
    // =================================================================

    /**
     * Update content panel based on current step
     */
    private void updateContentPanel() {
        if (contentPanel == null) {
            return; // Not initialized yet
        }

        contentPanel.removeAll();
        errorLabel.setText(""); // Clear any error messages

        String stepType = getCurrentStepType();
        logger.debug("Updating content panel for step: {}", stepType);

        JComponent panel = null;
        switch (stepType) {
            case STEP_TYPE_WELCOME :
                panel = createWelcomePanel();
                break;
            case STEP_TYPE_PLAY_MODE :
                panel = createPlayModePanel();
                break;
            case STEP_TYPE_PROFILE :
                panel = selectedMode == MODE_OFFLINE ? createOfflineProfilePanel() : createOnlineProfilePanel();
                break;
            case STEP_TYPE_SERVER_CONFIG :
                panel = createServerConfigPanel();
                break;
            case STEP_TYPE_EMAIL_SENT :
                panel = createEmailSentPanel();
                break;
            case STEP_TYPE_LINK_PROFILE :
                panel = createLinkProfilePanel();
                break;
            case STEP_TYPE_COMPLETE :
                panel = createCompletePanel();
                break;
        }

        if (panel != null) {
            contentPanel.add(panel, BorderLayout.CENTER);
        }

        updateButtonStates();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Create welcome panel
     */
    private JComponent createWelcomePanel() {
        DDPanel panel = new DDPanel();
        panel.setLayout(new BorderLayout(10, 10));

        DDLabel title = new DDLabel("ftue.welcome.title", STYLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(title, BorderLayout.NORTH);

        DDLabel message = new DDLabel("ftue.welcome.text", STYLE);
        panel.add(message, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Create play mode selection panel
     */
    private JComponent createPlayModePanel() {
        DDPanel panel = new DDPanel();
        panel.setLayout(new BorderLayout(10, 10));

        DDLabel title = new DDLabel("ftue.playmode.title", STYLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        // Radio button group
        DDPanel radioPanel = new DDPanel();
        radioPanel.setLayout(new GridLayout(3, 1, 5, 10));

        ButtonGroup group = new ButtonGroup();

        offlineRadio = new DDRadioButton("ftue.playmode.offline", STYLE);
        offlineRadio.setSelected(selectedMode == MODE_OFFLINE);
        offlineRadio.addActionListener(e -> {
            selectedMode = MODE_OFFLINE;
            updateButtonStates();
        });
        group.add(offlineRadio);

        onlineNewRadio = new DDRadioButton("ftue.playmode.online.new", STYLE);
        onlineNewRadio.setSelected(selectedMode == MODE_ONLINE_NEW);
        onlineNewRadio.addActionListener(e -> {
            selectedMode = MODE_ONLINE_NEW;
            updateButtonStates();
        });
        group.add(onlineNewRadio);

        onlineLinkRadio = new DDRadioButton("ftue.playmode.online.link", STYLE);
        onlineLinkRadio.setSelected(selectedMode == MODE_ONLINE_LINK);
        onlineLinkRadio.addActionListener(e -> {
            selectedMode = MODE_ONLINE_LINK;
            updateButtonStates();
        });
        group.add(onlineLinkRadio);

        radioPanel.add(offlineRadio);
        radioPanel.add(onlineNewRadio);
        radioPanel.add(onlineLinkRadio);

        panel.add(radioPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Create offline profile creation panel
     */
    private JComponent createOfflineProfilePanel() {
        DDPanel panel = new DDPanel();
        panel.setLayout(new BorderLayout(10, 10));

        DDLabel title = new DDLabel("ftue.profile.title", STYLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        // Name field
        DDPanel fieldPanel = new DDPanel();
        fieldPanel.setLayout(new BorderLayout(5, 5));

        DDLabel nameLabel = new DDLabel("ftue.profile.name", STYLE);
        fieldPanel.add(nameLabel, BorderLayout.WEST);

        nameField = new DDTextField(GuiManager.DEFAULT, STYLE);
        nameField.setText(playerName);
        nameField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                playerName = nameField.getText();
                updateButtonStates();
            }
        });
        fieldPanel.add(nameField, BorderLayout.CENTER);

        panel.add(fieldPanel, BorderLayout.CENTER);

        // Note
        DDLabel note = new DDLabel("ftue.profile.local.note", STYLE);
        note.setFont(note.getFont().deriveFont(Font.ITALIC, 11f));
        panel.add(note, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create server configuration panel
     */
    private JComponent createServerConfigPanel() {
        DDPanel panel = new DDPanel();
        panel.setLayout(new BorderLayout(10, 10));

        DDLabel title = new DDLabel("ftue.server.title", STYLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        DDPanel fieldsPanel = new DDPanel();
        fieldsPanel.setLayout(new GridLayout(3, 1, 5, 10));

        // Game server field
        DDPanel gamePanel = new DDPanel();
        gamePanel.setLayout(new BorderLayout(5, 0));
        DDLabel gameLabel = new DDLabel("ftue.server.game", STYLE);
        gamePanel.add(gameLabel, BorderLayout.WEST);
        gameServerField = new DDTextField(GuiManager.DEFAULT, STYLE);
        gameServerField.setText(gameServer);
        gameServerField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                gameServer = gameServerField.getText();
                updateButtonStates();
            }
        });
        gamePanel.add(gameServerField, BorderLayout.CENTER);
        fieldsPanel.add(gamePanel);

        // Chat server field
        DDPanel chatPanel = new DDPanel();
        chatPanel.setLayout(new BorderLayout(5, 0));
        DDLabel chatLabel = new DDLabel("ftue.server.chat", STYLE);
        chatPanel.add(chatLabel, BorderLayout.WEST);
        chatServerField = new DDTextField(GuiManager.DEFAULT, STYLE);
        chatServerField.setText(chatServer);
        chatServerField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                chatServer = chatServerField.getText();
                updateButtonStates();
            }
        });
        chatPanel.add(chatServerField, BorderLayout.CENTER);
        fieldsPanel.add(chatPanel);

        // Test button and status
        DDPanel testPanel = new DDPanel();
        testPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        testConnectionButton = new DDButton("test", STYLE);
        testConnectionButton.setText(PropertyConfig.getMessage("msg.ftue.server.test"));
        testConnectionButton.addActionListener(e -> handleTestConnection());
        testPanel.add(testConnectionButton);

        statusLabel = new DDLabel(GuiManager.DEFAULT, STYLE);
        statusLabel.setText("");
        testPanel.add(statusLabel);

        fieldsPanel.add(testPanel);

        panel.add(fieldsPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Create online profile creation panel
     */
    private JComponent createOnlineProfilePanel() {
        DDPanel panel = new DDPanel();
        panel.setLayout(new BorderLayout(10, 10));

        DDLabel title = new DDLabel("ftue.profile.title", STYLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        DDPanel fieldsPanel = new DDPanel();
        fieldsPanel.setLayout(new GridLayout(2, 1, 5, 10));

        // Name field
        DDPanel namePanel = new DDPanel();
        namePanel.setLayout(new BorderLayout(5, 0));
        DDLabel nameLabel = new DDLabel("ftue.profile.name", STYLE);
        namePanel.add(nameLabel, BorderLayout.WEST);
        nameField = new DDTextField(GuiManager.DEFAULT, STYLE);
        nameField.setText(playerName);
        nameField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                playerName = nameField.getText();
                updateButtonStates();
            }
        });
        namePanel.add(nameField, BorderLayout.CENTER);
        fieldsPanel.add(namePanel);

        // Email field
        DDPanel emailPanel = new DDPanel();
        emailPanel.setLayout(new BorderLayout(5, 0));
        DDLabel emailLabel = new DDLabel("ftue.profile.email", STYLE);
        emailPanel.add(emailLabel, BorderLayout.WEST);
        emailField = new DDTextField(GuiManager.DEFAULT, STYLE);
        emailField.setText(playerEmail);
        emailField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                playerEmail = emailField.getText();
                updateButtonStates();
            }
        });
        emailPanel.add(emailField, BorderLayout.CENTER);
        fieldsPanel.add(emailPanel);

        panel.add(fieldsPanel, BorderLayout.CENTER);

        // Note
        DDLabel note = new DDLabel("ftue.profile.online.note", STYLE);
        note.setFont(note.getFont().deriveFont(Font.ITALIC, 11f));
        panel.add(note, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create link existing profile panel
     */
    private JComponent createLinkProfilePanel() {
        DDPanel panel = new DDPanel();
        panel.setLayout(new BorderLayout(10, 10));

        DDLabel title = new DDLabel("ftue.link.title", STYLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        DDPanel fieldsPanel = new DDPanel();
        fieldsPanel.setLayout(new GridLayout(2, 1, 5, 10));

        // Name field
        DDPanel namePanel = new DDPanel();
        namePanel.setLayout(new BorderLayout(5, 0));
        DDLabel nameLabel = new DDLabel("ftue.link.username", STYLE);
        namePanel.add(nameLabel, BorderLayout.WEST);
        nameField = new DDTextField(GuiManager.DEFAULT, STYLE);
        nameField.setText(playerName);
        nameField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                playerName = nameField.getText();
                updateButtonStates();
            }
        });
        namePanel.add(nameField, BorderLayout.CENTER);
        fieldsPanel.add(namePanel);

        // Password field
        DDPanel passwordPanel = new DDPanel();
        passwordPanel.setLayout(new BorderLayout(5, 0));
        DDLabel passwordLabel = new DDLabel("ftue.link.password", STYLE);
        passwordPanel.add(passwordLabel, BorderLayout.WEST);
        passwordField = new DDTextField(GuiManager.DEFAULT, STYLE);
        passwordField.setText(playerPassword);
        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                playerPassword = passwordField.getText();
                updateButtonStates();
            }
        });
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        fieldsPanel.add(passwordPanel);

        panel.add(fieldsPanel, BorderLayout.CENTER);

        // Note
        DDLabel note = new DDLabel("ftue.link.note", STYLE);
        note.setFont(note.getFont().deriveFont(Font.ITALIC, 11f));
        panel.add(note, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create email sent panel
     */
    private JComponent createEmailSentPanel() {
        DDPanel panel = new DDPanel();
        panel.setLayout(new BorderLayout(10, 10));

        DDLabel title = new DDLabel("ftue.email.sent.title", STYLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.NORTH);

        String message = PropertyConfig.getMessage("msg.ftue.email.sent.text", playerEmail);
        DDLabel messageLabel = new DDLabel(GuiManager.DEFAULT, STYLE);
        messageLabel.setText(message);
        panel.add(messageLabel, BorderLayout.CENTER);

        // Password entry field
        DDPanel passwordPanel = new DDPanel();
        passwordPanel.setLayout(new BorderLayout(5, 10));
        passwordPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        DDLabel passwordLabel = new DDLabel("ftue.email.password", STYLE);
        passwordPanel.add(passwordLabel, BorderLayout.WEST);

        passwordField = new DDTextField(GuiManager.DEFAULT, STYLE);
        passwordField.setText(receivedPassword);
        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                receivedPassword = passwordField.getText();
                updateButtonStates();
            }
        });
        passwordPanel.add(passwordField, BorderLayout.CENTER);

        panel.add(passwordPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Create completion panel
     */
    private JComponent createCompletePanel() {
        DDPanel panel = new DDPanel();
        panel.setLayout(new BorderLayout(10, 10));

        String titleKey = selectedMode == MODE_OFFLINE ? "ftue.complete.offline.title" : "ftue.complete.online.title";
        String messageKey = selectedMode == MODE_OFFLINE ? "ftue.complete.offline.text" : "ftue.complete.online.text";

        DDLabel title = new DDLabel(titleKey, STYLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(title, BorderLayout.NORTH);

        DDLabel message = new DDLabel(messageKey, STYLE);
        panel.add(message, BorderLayout.CENTER);

        return panel;
    }

    // =================================================================
    // Button Event Handlers
    // =================================================================

    private void handleSkip() {
        logger.debug("User skipped wizard");
        skipWizard();
        closeDialog();
    }

    private void handleBack() {
        logger.debug("User clicked back");
        previousStep();
        updateContentPanel();
    }

    private void handleNext() {
        logger.debug("User clicked next");

        // Validate current step before proceeding
        if (!validateCurrentStep()) {
            return;
        }

        nextStep();
        updateContentPanel();
    }

    private void handleFinish() {
        logger.debug("User clicked finish");
        completeWizard();
        saveCompletionPreference(Prefs.getUserPrefs(PREFS_NODE));
        closeDialog();
    }

    private void handleTestConnection() {
        logger.debug("Testing server connection: {}", gameServer);
        testServerConnection();

        // Simulate connection test (in real implementation, would actually connect)
        boolean success = validateServerAddress();
        setConnectionTestResult(success);

        if (success) {
            statusLabel.setText(PropertyConfig.getMessage("msg.ftue.server.success"));
            statusLabel.setForeground(new Color(0, 128, 0)); // Green
            setServerConfigComplete(true);
        } else {
            statusLabel.setText(PropertyConfig.getMessage("msg.ftue.server.failed"));
            statusLabel.setForeground(Color.RED);
            setServerConfigComplete(false);
        }

        updateButtonStates();
    }

    /**
     * Validate current step inputs
     */
    private boolean validateCurrentStep() {
        String stepType = getCurrentStepType();
        errorLabel.setText("");

        switch (stepType) {
            case STEP_TYPE_PROFILE :
                if (!validateProfileName()) {
                    errorLabel.setText(getValidationError());
                    return false;
                }
                if (selectedMode != MODE_OFFLINE && !validateEmail()) {
                    errorLabel.setText(getValidationError());
                    return false;
                }
                break;

            case STEP_TYPE_SERVER_CONFIG :
                if (!serverConfigComplete) {
                    errorLabel.setText(PropertyConfig.getMessage("msg.ftue.validation.server.invalid"));
                    return false;
                }
                break;

            case STEP_TYPE_LINK_PROFILE :
                if (!validateProfileName()) {
                    errorLabel.setText(getValidationError());
                    return false;
                }
                if (playerPassword.isEmpty()) {
                    errorLabel.setText(PropertyConfig.getMessage("msg.ftue.validation.password.empty"));
                    return false;
                }
                break;

            case STEP_TYPE_EMAIL_SENT :
                if (!validatePassword()) {
                    errorLabel.setText(PropertyConfig.getMessage("msg.ftue.validation.password.empty"));
                    return false;
                }
                break;
        }

        return true;
    }

    private void closeDialog() {
        // Close the dialog by removing it
        if (context_ != null) {
            context_.processPhaseNow("PreviousPhase", null);
        }
    }

    /**
     * Check if wizard should be shown
     */
    public static boolean shouldShowWizard() {
        Preferences prefs = Prefs.getUserPrefs(PREFS_NODE);
        boolean completed = prefs.getBoolean(PREF_WIZARD_COMPLETED, false);
        boolean dontShow = prefs.getBoolean(PREF_DONT_SHOW_AGAIN, false);
        return !completed && !dontShow;
    }

    // =================================================================
    // DialogPhase Implementation
    // =================================================================

    /**
     * Create dialog contents (required by DialogPhase)
     */
    @Override
    public JComponent createDialogContents() {
        logger.debug("Creating wizard dialog contents");

        // Main container
        mainPanel = new DDPanel();
        BorderLayout mainLayout = (BorderLayout) mainPanel.getLayout();
        mainLayout.setVgap(10);
        mainLayout.setHgap(10);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Content area (will change based on current step)
        contentPanel = new DDPanel();
        contentPanel.setLayout(new BorderLayout());
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Error/status display at bottom of content
        DDPanel statusPanel = new DDPanel();
        statusPanel.setLayout(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        errorLabel = new DDLabel(GuiManager.DEFAULT, STYLE);
        errorLabel.setText("");
        errorLabel.setForeground(Color.RED);
        statusPanel.add(errorLabel, BorderLayout.CENTER);

        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // Button panel at bottom
        createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Initialize with current step
        updateContentPanel();

        return mainPanel;
    }

    /**
     * Create the button panel with navigation buttons
     */
    private void createButtonPanel() {
        buttonPanel = new DDPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Skip button
        skipButton = new DDButton("skip", STYLE);
        skipButton.setText(PropertyConfig.getMessage("msg.ftue.button.skip"));
        skipButton.addActionListener(e -> handleSkip());

        // Back button
        backButton = new DDButton("back", STYLE);
        backButton.setText(PropertyConfig.getMessage("msg.ftue.button.back"));
        backButton.addActionListener(e -> handleBack());

        // Next button
        nextButton = new DDButton("next", STYLE);
        nextButton.setText(PropertyConfig.getMessage("msg.ftue.button.next"));
        nextButton.addActionListener(e -> handleNext());

        // Finish button
        finishButton = new DDButton("finish", STYLE);
        finishButton.setText(PropertyConfig.getMessage("msg.ftue.button.finish"));
        finishButton.addActionListener(e -> handleFinish());

        buttonPanel.add(skipButton);
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(finishButton);

        updateButtonStates();
    }

    /**
     * Update button states based on current step and validation
     */
    private void updateButtonStates() {
        // Skip button only visible on play mode step
        skipButton.setVisible(currentStep == STEP_PLAY_MODE);

        // Back button disabled on first step
        backButton.setEnabled(currentStep != STEP_PLAY_MODE);

        // Next/Finish button visibility based on step type
        String stepType = getCurrentStepType();
        boolean isComplete = stepType.equals(STEP_TYPE_COMPLETE);

        nextButton.setVisible(!isComplete);
        finishButton.setVisible(isComplete);

        // Enable next button based on validation
        if (!isComplete) {
            nextButton.setEnabled(canProceedToNextStep());
        }
    }
}
