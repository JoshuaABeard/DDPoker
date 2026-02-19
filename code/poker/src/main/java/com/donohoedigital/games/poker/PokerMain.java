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
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
/*
 * Poker.java
 *
 * Created on December 7, 2003, 8:42 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.CommandLine;
import com.donohoedigital.base.Utils;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.config.GameConfigUtils;
import com.donohoedigital.games.config.GameState;
import com.donohoedigital.games.config.GameStateFactory;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.games.poker.server.EmbeddedGameServer;
import com.donohoedigital.games.poker.server.GameSaveManager;
import com.donohoedigital.gui.DDHtmlEditorKit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 * @author Doug Donohoe
 */
public class PokerMain extends GameEngine {
    private static final Logger logger;

    private static final String APP_NAME = "poker";
    private String sFileParam_ = null;
    private final boolean bLoadNames;
    private EmbeddedGameServer embeddedServer_;
    private GameSaveManager gameSaveManager_;

    static {
        // forget why I set this
        System.setProperty("sun.java2d.noddraw", "true");

        // Mac: Menu Name
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "DD Poker"); // TODO + version?
        System.setProperty("apple.awt.application.name", "DD Poker"); // TODO + version?

        // avoid java.lang.NullPointerException
        // at javax.swing.plaf.metal.MetalSliderUI.installUI(MetalSliderUI.java:110)
        System.setProperty("swing.defaultlaf", "javax.swing.plaf.metal.MetalLookAndFeel");

        // initialize logging before anything else (need version string for log file
        // directory)
        Utils.setVersionString(PokerConstants.VERSION.getMajorAsString());
        LoggingConfig loggingConfig = new LoggingConfig(APP_NAME, ApplicationType.CLIENT);
        loggingConfig.init();
        logger = LogManager.getLogger(PokerMain.class);

        // initialize file-based preferences (replaces Java Preferences API)
        Prefs.initialize();
        logger.info("File-based preferences initialized at: {}", FilePrefs.getInstance().getConfigDir());
    }

    /**
     * Run Poker
     */
    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public static void main(String[] args) {
        try {
            PokerMain main = new PokerMain(APP_NAME, "poker", args);
            main.init();
        } catch (ApplicationError ae) {
            System.err.println("Poker ending due to ApplicationError: " + ae);
            System.exit(1);
        } catch (OutOfMemoryError nomem) {
            System.err.println("Out of memory: " + nomem);
            System.err.println(Utils.formatExceptionText(nomem));
            System.exit(1);
        }
    }

    /**
     * Get profile override
     */
    String getProfileOverride() {
        return getCommandLineOptions().getString("profile", null);
    }

    /**
     * cast getGameEngine to PokerMain
     */
    public static PokerMain getPokerMain() {
        return (PokerMain) GameEngine.getGameEngine();
    }

    /**
     * Create Poker from config file
     */
    public PokerMain(String sConfigName, String sMainModule, String[] args) throws ApplicationError {
        this(sConfigName, sMainModule, args, false, true);
    }

    /**
     * Create Poker from config file
     */
    public PokerMain(String sConfigName, String sMainModule, String[] args, boolean bHeadless, boolean bLoadNames)
            throws ApplicationError {
        super(sConfigName, sMainModule, PokerConstants.VERSION.getMajorAsString(), args, bHeadless);
        this.bLoadNames = bLoadNames;
    }

    /**
     * Accessor for the embedded game server (for WebSocketTournamentDirector and
     * PracticeGameLauncher).
     */
    public EmbeddedGameServer getEmbeddedServer() {
        return embeddedServer_;
    }

    /**
     * Accessor for the game save manager (for resume-game UI).
     */
    public GameSaveManager getGameSaveManager() {
        return gameSaveManager_;
    }

    /**
     * init
     */
    @Override
    public void init() {
        super.init();
        if (bCheckFailed_)
            return;

        // Start the embedded Spring Boot game server.
        // Called after super.init() so PropertyConfig is already loaded.
        // Runs synchronously; completes in ~1-2 seconds during splash screen.
        embeddedServer_ = new EmbeddedGameServer();
        try {
            embeddedServer_.start();
        } catch (EmbeddedGameServer.EmbeddedServerStartupException e) {
            logger.error("Failed to start embedded game server", e);
            javax.swing.JOptionPane.showMessageDialog(null,
                    "DD Poker could not start its game server:\n" + e.getMessage()
                            + "\n\nPlease check logs for details.",
                    "Startup Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        gameSaveManager_ = new GameSaveManager(embeddedServer_);
        gameSaveManager_.loadResumableGames();

        // Register shutdown hook to cleanly stop the embedded server when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (embeddedServer_ != null) {
                embeddedServer_.stop();
            }
        }, "embedded-server-shutdown"));

        // init
        GameState.setDelegate(new PokerGameStateDelegate());

        // get args
        String[] otherargs = CommandLine.getRemainingArgs();
        if (otherargs != null && otherargs.length > 0) {
            sFileParam_ = otherargs[0];
        }

        // load names for computer players
        if (bLoadNames)
            loadNames();

        // show main window
        if (!bHeadless_)
            initMainWindow();

        // register our custom tag display classes
        DDHtmlEditorKit.registerTagViewClass("ddcard", DDCardView.class);
        DDHtmlEditorKit.registerTagViewClass("ddhandgroup", DDHandGroupView.class);
    }

    /**
     * copy any v2 files save files
     */
    @Override
    protected void copySaveFiles() {
        // get standard files
        super.copySaveFiles();

        // copy v2 changes
        File userSaveV3 = GameConfigUtils.getSaveDir();
        File userSaveV2 = new File(userSaveV3.getAbsolutePath().replaceAll("poker3", "poker2"));
        if (userSaveV2.exists() && userSaveV2.isDirectory()) {
            ConfigUtils.copyDir(userSaveV2, userSaveV3, new UpgradeFilter());
        }

    }

    /**
     * Filter for upgrade to skip legacy dirs and database files
     */
    private static class UpgradeFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            // skip these files
            return !name.startsWith(".") && !name.equals("db") && !name.equals("advisors")
                    && !name.equals("preflopstrategies");
        }
    }

    /**
     * debug options
     */
    @Override
    protected void setupApplicationCommandLineOptions() {
        super.setupApplicationCommandLineOptions();
        CommandLine.addStringOption("profile", null);
    }

    /**
     * Get version
     */
    @Override
    public Version getVersion() {
        return PokerConstants.VERSION;
    }

    /**
     * See if we can load the default profile. If not, another copy of the game is
     * already in use
     */
    @Override
    protected boolean checkPreReq() {
        // skip check in headless mode
        if (bHeadless_)
            return true;

        // in dev, allow one failure, then wait 3 seconds in case
        // we just killed and restarted right away
        if (DebugConfig.isTestingOn()) {
            try {
                PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
                if (profile != null)
                    profile.testDB();
                return true;
            } catch (Throwable t) {
                // FIX: if database driver is missing, this fails silently.
                // This code is kind of a mess. Log message for now
                logger.warn(Utils.formatExceptionText(t));
            }

            // if first attempt failed, try again after 3 seconds to see
            // if lock clears
            Utils.sleepMillis(PokerConstants.PROFILE_RETRY_MILLIS);
        }

        try {
            PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
            if (profile != null)
                profile.testDB();
        } catch (ApplicationError ae) {
            Throwable source = ae.getException();
            if (source != null) {
                String sMessage = source.getMessage();
                if (source instanceof SQLException && sMessage != null
                        && (sMessage.contains("database is already in use")
                                || sMessage.contains("File input/output error"))) {
                    logger.warn("Another copy running (database already in use).  Showing warning splash.");
                    String sMsg = PropertyConfig.getMessage("msg.2ndcopy");
                    splashscreen_.changeUI(this, true, sMsg);
                    return false;
                }
            }
            throw ae;
        }
        return true;
    }

    /**
     * initial splash file name
     */
    @Override
    protected String getSplashBackgroundFile() {
        return "poker-splash-nochoice.jpg";
    }

    /**
     * initial splash file name
     */
    @Override
    protected String getSplashIconFile() {
        return "pokericon32.gif";
    }

    /**
     * initial splash title
     */
    @Override
    protected String getSplashTitle() {
        return "DD Poker";
    }

    /**
     * Create a context - create our PokerContext
     */
    @Override
    protected GameContext createGameContext(Game game, String sName, int nDesiredMinWidth, int nDesiredMinHeight,
            boolean bQuitOnClose) {

        return new PokerContext(this, (PokerGame) game, sName, nDesiredMinWidth, nDesiredMinHeight, bQuitOnClose);
    }

    /**
     * Create a context - here for overriding
     */
    @Override
    protected GameContext createInternalGameContext(GameContext context, String sName, int nDesiredMinWidth,
            int nDesiredMinHeight) {
        return new PokerContext((PokerContext) context, sName, nDesiredMinWidth, nDesiredMinHeight);
    }

    /**
     * We use gameboard config
     */
    @Override
    protected boolean loadGameboardConfig() {
        return true;
    }

    /**
     * start P2P after showing main window If we had a command line param, handle it
     */
    @Override
    protected void initialStart() {
        // Server config check moved to PokerStartMenu (after license, before profile)

        /*
         * handle load
         */
        if (sFileParam_ != null) {
            Logger log = LogManager.getLogger(PokerMain.class);
            if (sFileParam_.endsWith(GameListPanel.SAVE_EXT)) {
                log.info("Loading saved game: {}", sFileParam_);
                File file = new File(sFileParam_).getAbsoluteFile();
                try {
                    ConfigUtils.verifyFile(file);
                    GameState state = GameStateFactory.createGameState(file, false);
                    LoadSavedGame.loadGame(getDefaultContext(), state);
                    return;
                } catch (ApplicationError ae) {
                    log.error("Unable to load saved game: {}", sFileParam_);
                    log.error(ae.toString());
                    // bad save file, so we just do normal initialStart
                }
            }
        }

        super.initialStart();
    }

    /**
     * Called in some OS when Preferences menu item selected
     */
    @Override
    public void showPrefs() {
        boolean bShowDialog = true;
        Phase current = getDefaultContext().getCurrentUIPhase();
        if (current != null) {
            String sName = current.getGamePhase().getName();
            if (sName.equals("GamePrefs"))
                return; // already displayed
            if (sName.equals("StartMenu"))
                bShowDialog = false; // go to prefs screen
        }

        if (!bShowDialog) {
            getDefaultContext().processPhase("GamePrefs"); // TODO: active context?
        } else {
            getDefaultContext().processPhase("GamePrefsDialog"); // TODO: active context?
        }
    }

    /**
     * Message for expired copies
     */
    @Override
    protected String getExpiredMessage() {
        return "<font color=\"white\"> Version " + getVersion() + "</font>"
                + " of DD Poker has expired.  Please contact Donohoe Digital to get the " + " most recent version.";
    }

    /**
     * Get starting size - we set proportional to 800x600, but at a size 200 less
     * than screen height
     */
    @Override
    protected Dimension getStartingSize() {
        DisplayMode mode = frame_.getDisplayMode();
        int height = Math.max(DESIRED_MIN_HEIGHT, mode.getHeight() - PokerConstants.VERTICAL_SCREEN_FREE_SPACE);
        int width = Math.max(DESIRED_MIN_WIDTH, (DESIRED_MIN_WIDTH * height) / DESIRED_MIN_HEIGHT);
        return new Dimension(width, height);
    }

    ////
    //// Player names
    ////

    private final List<String> names_ = new ArrayList<>();

    /**
     * get names array list
     */
    public List<String> getNames() {
        return Collections.unmodifiableList(names_);
    }

    /**
     * Load names
     */
    private void loadNames() {
        URL names = new MatchingResources("classpath*:config/" + sAppName + "/names.txt")
                .getSingleRequiredResourceURL();
        Reader reader = ConfigUtils.getReader(names);

        try {
            BufferedReader buf = new BufferedReader(reader);

            String sName;
            while ((sName = buf.readLine()) != null) {
                names_.add(sName);
            }
        } catch (IOException ioe) {
            throw new ApplicationError(ioe);
        } finally {
            ConfigUtils.close(reader);
        }
    }

    ////
    //// LanControllerInterface methods
    ////

    //
    // Interface methods implemented by super class:
    //
    // + public String getGUID()
    // + public String getLicenseKey()
    //

    /**
     * player name (current player)
     */
    public String getPlayerName() {
        // note: PlayerProfileOptions cache's the profile, so this isn't
        // expensive
        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
        if (profile == null)
            return PropertyConfig.getMessage("msg.playername.undefined");
        return profile.getName();
    }

    /**
     * return whether key in message is valid
     */
    public boolean isValid(DDMessage msg) {
        // look for key
        String sKey = msg.getKey();
        if (sKey == null) {
            logger.warn("Message has no key: {}", msg);
            return false;
        }

        // Get version
        Version version = msg.getVersion();
        if (version == null) {
            logger.warn("Message has no version: {}", msg);
            return false;
        }

        return true;
    }

    /**
     * Allow duplicate keys?
     */
    public boolean allowDuplicate() {
        return TESTING(EngineConstants.TESTING_SKIP_DUP_KEY_CHECK);
    }

    /**
     * handle duplicate key case
     */
    public void handleDuplicateKey(String sName, String sHost, String sIP) {
        handleDuplicateIp(sName, sHost, sIP);
    }

    /**
     * Handle same IP (disallows running 2.x and 3.x at same time)
     */
    public void handleDuplicateIp(String sName, String sHost, String sIP) {
        if (bDup_)
            return; // already handling
        bDup_ = true;

        final String sMsg = PropertyConfig.getMessage("msg.dupip.exit", sName, sHost, sIP);
        // do processing
        SwingUtilities.invokeLater(() -> {
            PokerUtils.displayInformationDialog(getDefaultContext(), Utils.fixHtmlTextFor15(sMsg), "msg.title.dupdd",
                    null);
            System.exit(1);
        });
    }

    private boolean bDup_ = false;

    // constants for online game description
    public static final String ONLINE_GAME_LAN_CONNECT = "lan-connect";
    public static final String ONLINE_GAME_PROFILE = "profile";
    public static final String ONLINE_GAME_STATUS = "status";

    /**
     * Get online game description for LAN broadcast. Returns null in M7 — games are
     * hosted by the embedded server, not broadcast via LAN P2P.
     */
    public DataMarshal getOnlineGame() {
        return null;
    }

    /**
     * Compare games
     */
    public boolean isEquivalentOnlineGame(DataMarshal one, DataMarshal two) {
        if (one == null && two == null)
            return true;

        if (one == null || two == null)
            return false;

        DMTypedHashMap d1 = (DMTypedHashMap) one;
        DMTypedHashMap d2 = (DMTypedHashMap) two;

        // Zero: game status must be same
        if (d1.getInteger(ONLINE_GAME_STATUS, -1) != d2.getInteger(ONLINE_GAME_STATUS, -1))
            return false;

        // One: lan string must be same
        if (!d1.getString(ONLINE_GAME_LAN_CONNECT, "").equals(d2.getString(ONLINE_GAME_LAN_CONNECT, "")))
            return false;

        // Two: profile name and create date must be the same
        TournamentProfile p1 = (TournamentProfile) d1.getObject(ONLINE_GAME_PROFILE);
        TournamentProfile p2 = (TournamentProfile) d2.getObject(ONLINE_GAME_PROFILE);

        if (p1 == null && p2 == null)
            return true;
        if (p1 == null || p2 == null)
            return false;

        return p1.getName().equals(p2.getName()) && p1.getCreateDate() == p2.getCreateDate()
                && p1.getUpdateDate() == p2.getUpdateDate();
    }
}
