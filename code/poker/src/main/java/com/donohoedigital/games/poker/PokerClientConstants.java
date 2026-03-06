/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.base.Format;
import com.donohoedigital.comms.Version;

/**
 * Client-side constants extracted from {@code PokerConstants}.
 *
 * <p>
 * These constants are used only by the desktop client (poker module) for
 * display options, chat settings, cheat/debug flags, and other client-specific
 * concerns. Values are identical to the originals in {@code PokerConstants} to
 * preserve config file compatibility.
 * </p>
 */
public class PokerClientConstants {

    private PokerClientConstants() {
    }

    // Version

    public static final Version VERSION = new Version(3, 3, 0, true, " Community Edition");
    public static final Version VERSION_COUNTDOWN_CHANGED = new Version(2, 5, 0, true);

    // Format

    public static final Format fTimeNum1 = new Format("%1d");
    public static final Format fTimeNum2 = new Format("%02d");

    public static String formatPercent(double pct) {
        return new Format("%3.2f").form(pct);
    }

    // Game piece type numbers (order dictates drawing order)

    public static final int PIECE_CARD = 5;
    public static final int PIECE_BUTTON = 10;
    public static final int PIECE_RESULTS = 15;

    // Game options node

    public static final String NODE_OPTION = "poker";

    // Display options

    public static final String OPTION_SHOW_PLAYER_TYPE = "showplayertype";
    public static final String OPTION_AUTO_CHECK_UPDATE = "autocheckupdate";
    public static final String OPTION_RIGHT_CLICK_ONLY = "rightclickonly";
    public static final String OPTION_DISABLE_SHORTCUTS = "disableshortcuts";
    public static final String OPTION_HOLE_CARDS_DOWN = "holecarddown";
    public static final String OPTION_PAUSE_ALLIN = "pauseallin";
    public static final String OPTION_PAUSE_COLOR = "pausecolor";
    public static final String OPTION_ZIP_MODE = "zipmode";
    public static final String OPTION_CHECKFOLD = "checkfold";
    public static final String OPTION_LARGE_CARDS = "largecards";
    public static final String OPTION_FOUR_COLOR_DECK = "fourcolordeck";
    public static final String OPTION_STYLIZED_FACE_CARDS = "stylized";
    public static final String OPTION_HANDS_PER_HOUR = "handsperhour";
    public static final String OPTION_DELAY = "delay";
    public static final String OPTION_AUTODEAL = "autodeal";
    public static final String OPTION_AUTODEALHAND = "autodealhand";
    public static final String OPTION_AUTODEALONLINE = "autodealonline";
    public static final String OPTION_ONLINESTART = "onlinestart";
    public static final String OPTION_CLOCK_COLOUP = "clockcolorup";
    public static final String OPTION_CLOCK_PAUSE = "clockpause";
    public static final String OPTION_DEFAULT_ADVISOR = "defaultadvisor";
    public static final String OPTION_SCREENSHOT_MAX_WIDTH = "screenshotmaxw";
    public static final String OPTION_SCREENSHOT_MAX_HEIGHT = "screenshotmaxh";

    // Online display options

    public static final String OPTION_ONLINE_AUDIO = "onlineaudio";
    public static final String OPTION_ONLINE_FRONT = "onlinefront";
    public static final String OPTION_ONLINE_COUNTDOWN = "countdown";
    public static final String OPTION_ONLINE_CHAT = "onlinechat";

    // Chat options

    public static final String OPTION_CHAT_PLAYERS = "chatplayers";
    public static final String OPTION_CHAT_OBSERVERS = "chatobservers";
    public static final String OPTION_CHAT_TIMEOUT = "chattimeout";
    public static final String OPTION_CHAT_DEALER = "chatdealer";
    public static final String OPTION_CHAT_DISPLAY = "chatdisplay";
    public static final String OPTION_CHAT_FONT_SIZE = "chatfontsize";
    public static final int DEFAULT_CHAT_FONT_SIZE = 12;
    public static final int MIN_CHAT_FONT_SIZE = 8;
    public static final int MAX_CHAT_FONT_SIZE = 24;

    // Cheat/debug display options

    public static final String OPTION_CHEAT_POPUP = "popups";
    public static final String OPTION_CHEAT_SHOWWINNINGHAND = "showdown";
    public static final String OPTION_CHEAT_RABBITHUNT = "river";
    public static final String OPTION_CHEAT_MOUSEOVER = "mouseover";
    public static final String OPTION_CHEAT_AIFACEUP = "aifaceup";
    public static final String OPTION_CHEAT_SHOWFOLD = "showfold";
    public static final String OPTION_CHEAT_NEVERBROKE = "neverbroke";
    public static final String OPTION_CHEAT_SHOW_MUCKED = "showmuck";

    // Dealer chat levels

    public static final int DEALER_NONE = 1;
    public static final int DEALER_NO_PLAYER_ACTION = 2;
    public static final int DEALER_ALL = 3;

    // Display chat options

    public static final int DISPLAY_ONE = 1;
    public static final int DISPLAY_TAB = 2;
    public static final int DISPLAY_SPLIT = 3;

    // Chat levels

    public static final int CHAT_PRIVATE = -1;
    public static final int CHAT_ALWAYS = 0;
    public static final int CHAT_1 = 1;
    public static final int CHAT_2 = 2;
    public static final int CHAT_TIMEOUT = 3;
    public static final int CHAT_DIRECTOR_MSG_ID = -2;
    public static final int CHAT_DEALER_MSG_ID = -3;

    // Chat/info server

    @SuppressWarnings({"PublicStaticArrayField"})
    public static final byte CHAT_BYTES[] = {'6', 'e', 'h', 'g', '@', '!', 'T', 'A', 'Z', 'D', 'C', '%'};

    // Payout options

    public static final int PAYOUT_SPOTS = 1;
    public static final int PAYOUT_PERC = 2;
    public static final int PAYOUT_SATELLITE = 3;

    // House cut

    public static final int HOUSE_AMOUNT = 1;
    public static final int HOUSE_PERC = 2;

    // Allocation

    public static final int ALLOC_AUTO = 1;
    public static final int ALLOC_PERC = 2;
    public static final int ALLOC_AMOUNT = 3;

    // Rebuy expression

    public static final int REBUY_LT = 1;
    public static final int REBUY_LTE = 2;

    // Late registration chip modes

    public static final int LATE_REG_CHIPS_STARTING = 1;
    public static final int LATE_REG_CHIPS_AVERAGE = 2;

    // Other prefs

    public static final String PREF_DASHBOARD = "dashboard";

    // Online

    public static final String ID_PASS_DELIM = "/";
    public static final String URL_START = "poker://";
    public static final String REGEXP_DOLLAR_AMOUNT = "^\\$?[0-9\\,]*$";
    public static final String REGEXP_IP_ADDRESS = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";

    // Misc

    public static final int PROFILE_RETRY_MILLIS = 3000;
    public static final int VERTICAL_SCREEN_FREE_SPACE = 200;
    public static final int START_OTHER_ID = 11000;

    // Debug/testing flags

    public static final boolean DEBUG_EVENT_DISPLAY = false;
    public static final boolean DEBUG_CLEANUP_TABLE = false;

    public static final String TESTING_LEVELS = "settings.debug.levels";
    public static final String TESTING_DOUG_CONTROLS_AI = "settings.debug.dougcontrolsai";
    public static final String TESTING_PAUSE_AI = "settings.debug.pauseai";
    public static final String TESTING_AUTOPILOT_INIT = "settings.debug.autopilot";
    public static final String TESTING_AUTOPILOT = "settings.debug.autopilot.on";
    public static final String TESTING_FAST_SAVE = "settings.debug.fastsave";
    public static final String TESTING_ONLINE_AUTO_DEAL_OFF = "settings.debug.onlineautodealoff";
    public static final String TESTING_ALLOW_CHEAT_ONLINE = "settings.debug.cheatonline";
    public static final String TESTING_CHAT_PERF = "settings.debug.chat.perf";
    public static final String TESTING_ALLOW_CHANGE_LEVEL = "settings.debug.changelevel";
    public static final String TESTING_SPLIT_HUMANS = "settings.debug.onlinesplithumans";
    public static final String TESTING_ONLINE_AI_NO_WAIT = "settings.debug.onlineainowait";

    // Game constants migrated from TournamentDirector

    public static final String TD_PHASE_NAME = "TournamentDirector";
}
