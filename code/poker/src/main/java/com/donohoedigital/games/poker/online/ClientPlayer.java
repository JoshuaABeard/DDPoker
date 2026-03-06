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

import com.donohoedigital.base.Format;
import com.donohoedigital.comms.Version;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.GamePlayer;
import com.donohoedigital.games.config.GameState;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.HandAction;
import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.ai.PlayerType;
import com.donohoedigital.games.poker.display.ClientHand;

import java.util.Comparator;

/**
 * Lightweight display-only player model for the Swing UI.
 *
 * <p>
 * Replaces {@code PokerPlayer} as the player abstraction used by all desktop UI
 * code. Contains only display state — no game engine logic (bet, raise, fold,
 * etc.). All state is populated from WebSocket server messages by
 * {@link WebSocketTournamentDirector}.
 *
 * <p>
 * Extends {@link GamePlayer} so it can be stored in {@code Game.players_} and
 * participate in the existing game framework infrastructure (property change
 * listeners, player ID management, etc.).
 */
public class ClientPlayer extends GamePlayer {

    // -------------------------------------------------------------------------
    // Constants (migrated from PokerPlayer)
    // -------------------------------------------------------------------------
    public static final int EARLY = 0;
    public static final int MIDDLE = 1;
    public static final int LATE = 2;
    public static final int SMALL = 3;
    public static final int BIG = 4;
    public static Format fStringLong = new Format("%-20s");
    public static Format fChip = new Format("%7d");

    /**
     * Get position name for debugging.
     */
    public static String getPositionName(int n) {
        return switch (n) {
            case EARLY -> "early";
            case MIDDLE -> "middle";
            case LATE -> "late";
            case SMALL -> "small";
            case BIG -> "big";
            default -> "none";
        };
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------
    private int seat_;
    private boolean human_;
    private String playerId_; // engine player key
    private Version version_;

    // -------------------------------------------------------------------------
    // Game state
    // -------------------------------------------------------------------------
    private int chipCount_;
    private int chipCountAtStart_;
    private boolean folded_;
    private boolean sittingOut_;
    private boolean cardsExposed_;
    private boolean disconnected_;
    private boolean booted_;
    private boolean waiting_;
    private boolean onlineActivated_;
    private long waitListTimeStamp_;
    private int handsPlayedAtLastMove_;
    private long thinkBankAccessed_;
    private int rebuysPending_;
    private int rebuysChipsPending_;
    private int handsPlayedDisconnected_;
    private int handsPlayedSitout_;

    // -------------------------------------------------------------------------
    // Cards
    // -------------------------------------------------------------------------
    private ClientHand hand_ = ClientHand.empty();
    private ClientHand handSorted_;

    // -------------------------------------------------------------------------
    // Display info (server-calculated)
    // -------------------------------------------------------------------------
    private String allInPerc_;
    private int allInScore_;
    private int allInWin_;
    private int handScore_ = -1;

    // -------------------------------------------------------------------------
    // Tournament results
    // -------------------------------------------------------------------------
    private int prize_;
    private int place_;
    private int buyin_;
    private int addon_;
    private int rebuy_;
    private int numRebuys_;
    private int bountyCollected_;
    private int bountyCount_;
    private int oddChips_;
    private boolean wonChipRace_;
    private boolean brokeChipRace_;

    // -------------------------------------------------------------------------
    // Options / display prefs
    // -------------------------------------------------------------------------
    private boolean showWinning_;
    private boolean muckLosing_ = true;
    private boolean askShowLosing_;
    private boolean askShowWinning_;
    private int thinkBankMillis_;
    private int timeoutMillis_;
    private String onlineSettings_;

    // -------------------------------------------------------------------------
    // Profile / type
    // -------------------------------------------------------------------------
    private PlayerProfile profile_;
    private PlayerType playerType_;
    private String profilePath_;
    private ClientPokerTable table_;
    private float handStrength_;
    private float handPotential_;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Empty constructor needed for demarshalling. */
    public ClientPlayer() {
    }

    /**
     * Creates a client player with basic identity.
     */
    public ClientPlayer(int id, String name, boolean human) {
        this(null, id, name, human);
    }

    /**
     * Creates a client player with a player key for locally-controlled detection.
     */
    public ClientPlayer(String playerKey, int id, String name, boolean human) {
        super(id, name);
        playerId_ = playerKey;
        human_ = human;
    }

    /**
     * Creates a client player from a profile.
     */
    public ClientPlayer(String playerKey, int id, PlayerProfile profile, boolean human) {
        super(id, profile.getName());
        playerId_ = playerKey;
        human_ = human;
        setProfile(profile);
    }

    // -------------------------------------------------------------------------
    // Identity getters/setters
    // -------------------------------------------------------------------------

    public int getSeat() {
        return seat_;
    }

    public void setSeat(int seat) {
        seat_ = seat;
    }

    public boolean isHuman() {
        return human_;
    }

    public void setHuman(boolean human) {
        human_ = human;
    }

    @Override
    public boolean isComputer() {
        return !human_ && !isObserver();
    }

    public String getPlayerId() {
        return playerId_;
    }

    public void setPlayerId(String playerId) {
        playerId_ = playerId;
    }

    public Version getVersion() {
        return version_;
    }

    public void setVersion(Version version) {
        version_ = version;
    }

    /**
     * Returns whether this player is locally controlled. True when the player key
     * matches the local engine's player ID.
     */
    public boolean isLocallyControlled() {
        if (playerId_ == null)
            return false;
        GameEngine ge = GameEngine.getGameEngine();
        if (ge == null)
            return false;
        return playerId_.equals(ge.getPlayerId());
    }

    /**
     * Returns whether this player is human-controlled (human and locally
     * controlled).
     */
    public boolean isHumanControlled() {
        return human_ && isLocallyControlled();
    }

    // -------------------------------------------------------------------------
    // Game state getters/setters
    // -------------------------------------------------------------------------

    public int getChipCount() {
        return chipCount_;
    }

    public void setChipCount(int n) {
        chipCount_ = n;
    }

    public int getChipCountAtStart() {
        return chipCountAtStart_;
    }

    public void setChipCountAtStart(int n) {
        chipCountAtStart_ = n;
    }

    public void addChips(int n) {
        chipCount_ += n;
    }

    public void adjustChipCountAtStart(int n) {
        chipCountAtStart_ += n;
    }

    public boolean isFolded() {
        return folded_;
    }

    public void setFolded(boolean folded) {
        folded_ = folded;
    }

    public boolean isAllIn() {
        return !folded_ && chipCount_ == 0 && hand_ != null && hand_.size() > 0;
    }

    public boolean isSittingOut() {
        return sittingOut_;
    }

    public void setSittingOut(boolean sittingOut) {
        sittingOut_ = sittingOut;
    }

    public boolean isCardsExposed() {
        return cardsExposed_;
    }

    public void setCardsExposed(boolean cardsExposed) {
        cardsExposed_ = cardsExposed;
    }

    public boolean isDisconnected() {
        return disconnected_;
    }

    public void setDisconnected(boolean disconnected) {
        disconnected_ = disconnected;
    }

    public boolean isBooted() {
        return booted_;
    }

    public void setBooted(boolean booted) {
        booted_ = booted;
    }

    public boolean isWaiting() {
        return waiting_;
    }

    public void setWaiting(boolean waiting) {
        waiting_ = waiting;
    }

    public boolean isOnlineActivated() {
        return onlineActivated_;
    }

    public void setOnlineActivated(boolean onlineActivated) {
        onlineActivated_ = onlineActivated;
    }

    public long getWaitListTimeStamp() {
        return waitListTimeStamp_;
    }

    public void setWaitListTimeStamp(long waitListTimeStamp) {
        waitListTimeStamp_ = waitListTimeStamp;
    }

    public int getHandsPlayedAtLastMove() {
        return handsPlayedAtLastMove_;
    }

    public void setHandsPlayedAtLastMove(int handsPlayedAtLastMove) {
        handsPlayedAtLastMove_ = handsPlayedAtLastMove;
    }

    public long getThinkBankAccessed() {
        return thinkBankAccessed_;
    }

    public void setThinkBankAccessed(long thinkBankAccessed) {
        thinkBankAccessed_ = thinkBankAccessed;
    }

    public int getNumRebuysPending() {
        return rebuysPending_;
    }

    public void setNumRebuysPending(int rebuysPending) {
        rebuysPending_ = rebuysPending;
    }

    public int getRebuysChipsPending() {
        return rebuysChipsPending_;
    }

    public void setRebuysChipsPending(int rebuysChipsPending) {
        rebuysChipsPending_ = rebuysChipsPending;
    }

    public int getHandsPlayedDisconnected() {
        return handsPlayedDisconnected_;
    }

    public void setHandsPlayedDisconnected(int n) {
        handsPlayedDisconnected_ = n;
    }

    public int getHandsPlayedSitout() {
        return handsPlayedSitout_;
    }

    public void setHandsPlayedSitout(int n) {
        handsPlayedSitout_ = n;
    }

    /**
     * No-op for save/load compatibility. AI creation is server-side.
     */
    public void createPokerAI() {
        // no-op
    }

    /**
     * No-op for save/load compatibility. Pending rebuys are handled server-side.
     */
    public void addPendingRebuys() {
        // no-op
    }

    /**
     * No-op for save/load compatibility. Game-loaded init is server-side.
     */
    public void gameLoaded() {
        // no-op
    }

    /**
     * Creates a new hand for this player (e.g., for deal-high or color-up display).
     */
    public ClientHand newHand(char type) {
        hand_ = ClientHand.ofType(type);
        handSorted_ = null;
        return hand_;
    }

    /**
     * Get action for AI decision-making. In client-server mode, AI decisions are
     * handled by the server. Returns null.
     */
    public HandAction getAction(boolean bRecalc) {
        return null;
    }

    /**
     * No-op fold for save/load compatibility. Folding is handled server-side.
     */
    public void fold(String sDebug, int nFoldType) {
        setFolded(true);
    }

    /**
     * No-op betTest for simulation compatibility.
     */
    public void betTest(int nAmount) {
        // no-op
    }

    // -------------------------------------------------------------------------
    // Cards
    // -------------------------------------------------------------------------

    public ClientHand getHand() {
        return hand_;
    }

    public ClientHand getHandSorted() {
        if (handSorted_ == null || handSorted_.fingerprint() != hand_.fingerprint()) {
            handSorted_ = hand_.sorted();
        }
        return handSorted_;
    }

    /**
     * Removes all cards from the player's hand and resets the sorted hand.
     */
    public void removeHand() {
        hand_.clear();
        handSorted_ = null;
    }

    // -------------------------------------------------------------------------
    // Display info
    // -------------------------------------------------------------------------

    public String getAllInPerc() {
        return allInPerc_;
    }

    public void setAllInPerc(String allInPerc) {
        allInPerc_ = allInPerc;
    }

    public int getAllInScore() {
        return allInScore_;
    }

    public void setAllInScore(int allInScore) {
        allInScore_ = allInScore;
    }

    public int getAllInWin() {
        return allInWin_;
    }

    public void addAllInWin() {
        allInWin_++;
    }

    public void clearAllInWin() {
        allInWin_ = 0;
    }

    public int getHandScore() {
        return handScore_;
    }

    public void setHandScore(int handScore) {
        handScore_ = handScore;
    }

    // -------------------------------------------------------------------------
    // Tournament results
    // -------------------------------------------------------------------------

    public int getPrize() {
        return prize_;
    }

    public void setPrize(int prize) {
        prize_ = prize;
    }

    public int getPlace() {
        return place_;
    }

    public void setPlace(int place) {
        place_ = place;
    }

    public int getBuyin() {
        return buyin_;
    }

    public void setBuyin(int buyin) {
        buyin_ = buyin;
    }

    public int getAddon() {
        return addon_;
    }

    public void setAddon(int addon) {
        addon_ = addon;
    }

    public int getRebuy() {
        return rebuy_;
    }

    public void setRebuy(int rebuy) {
        rebuy_ = rebuy;
    }

    public int getNumRebuys() {
        return numRebuys_;
    }

    public void setNumRebuys(int numRebuys) {
        numRebuys_ = numRebuys;
    }

    /**
     * Adds a rebuy to this player.
     */
    public void addRebuy(int nAmount, int nChips, boolean bPending) {
        numRebuys_++;
        rebuy_ += nAmount;
        chipCount_ += nChips;
    }

    /**
     * Adds an add-on to this player.
     */
    public void addAddon(int nAmount, int nChips) {
        addon_ += nAmount;
        chipCount_ += nChips;
    }

    public int getTotalSpent() {
        return buyin_ + rebuy_ + addon_;
    }

    public int getBountyCollected() {
        return bountyCollected_;
    }

    public void setBountyCollected(int bountyCollected) {
        bountyCollected_ = bountyCollected;
    }

    public int getBountyCount() {
        return bountyCount_;
    }

    public void setBountyCount(int bountyCount) {
        bountyCount_ = bountyCount;
    }

    public int getOddChips() {
        return oddChips_;
    }

    public void setOddChips(int oddChips) {
        oddChips_ = oddChips;
    }

    public boolean isWonChipRace() {
        return wonChipRace_;
    }

    public void setWonChipRace(boolean wonChipRace) {
        wonChipRace_ = wonChipRace;
    }

    public boolean isBrokeChipRace() {
        return brokeChipRace_;
    }

    public void setBrokeChipRace(boolean brokeChipRace) {
        brokeChipRace_ = brokeChipRace;
    }

    // -------------------------------------------------------------------------
    // Options / display prefs
    // -------------------------------------------------------------------------

    public boolean isShowWinning() {
        return showWinning_;
    }

    public void setShowWinning(boolean showWinning) {
        showWinning_ = showWinning;
    }

    public boolean isMuckLosing() {
        return muckLosing_;
    }

    public void setMuckLosing(boolean muckLosing) {
        muckLosing_ = muckLosing;
    }

    public boolean isAskShowLosing() {
        return askShowLosing_;
    }

    public void setAskShowLosing(boolean askShowLosing) {
        askShowLosing_ = askShowLosing;
    }

    public boolean isAskShowWinning() {
        return askShowWinning_;
    }

    public void setAskShowWinning(boolean askShowWinning) {
        askShowWinning_ = askShowWinning;
    }

    public int getThinkBankMillis() {
        return thinkBankMillis_;
    }

    public void setThinkBankMillis(int thinkBankMillis) {
        thinkBankMillis_ = thinkBankMillis;
    }

    public int getTimeoutMillis() {
        return timeoutMillis_;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        timeoutMillis_ = timeoutMillis;
    }

    public void setTimeoutMessageSecondsLeft(int seconds) {
        // display-only — no-op on client
    }

    public String getOnlineSettings() {
        return onlineSettings_;
    }

    public void setOnlineSettings(String onlineSettings) {
        onlineSettings_ = onlineSettings;
    }

    // -------------------------------------------------------------------------
    // Profile / type
    // -------------------------------------------------------------------------

    public PlayerProfile getProfile() {
        return profile_;
    }

    public void setProfile(PlayerProfile profile) {
        profile_ = profile;
        if (profile != null) {
            setName(profile.getName());
        }
    }

    public boolean isProfileDefined() {
        return profile_ != null;
    }

    public PlayerType getPlayerType() {
        return playerType_;
    }

    public void setPlayerType(PlayerType playerType) {
        playerType_ = playerType;
    }

    public String getProfilePath() {
        return profilePath_;
    }

    public void setProfilePath(String profilePath) {
        profilePath_ = profilePath;
    }

    /**
     * No-op for save/load compatibility. Profile loading is handled by the server
     * in client-server mode.
     */
    public void loadProfile(GameState state) {
        // no-op — profiles are set by server messages
    }

    // -------------------------------------------------------------------------
    // Table linkage
    // -------------------------------------------------------------------------

    public ClientPokerTable getTable() {
        return table_;
    }

    public void setTable(ClientPokerTable table, int seat) {
        table_ = table;
        seat_ = seat;
    }

    // -------------------------------------------------------------------------
    // ClientHand strength (for dashboard display)
    // -------------------------------------------------------------------------

    public float getHandStrength() {
        return handStrength_;
    }

    public void setHandStrength(float handStrength) {
        handStrength_ = handStrength;
    }

    public float getHandPotential() {
        return handPotential_;
    }

    public void setHandPotential(float handPotential) {
        handPotential_ = handPotential;
    }

    public float getHandPotentialDisplay() {
        return handPotential_;
    }

    public float getEffectiveHandStrength() {
        return handStrength_ + (1.0f - handStrength_) * handPotential_;
    }

    // -------------------------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------------------------

    /**
     * Comparator to sort players by name.
     */
    public static final Comparator<ClientPlayer> SORTBYNAME = Comparator.comparing(ClientPlayer::getName);

    /**
     * Returns whether this player is in the current hand (has cards and is not
     * folded).
     */
    public boolean isInHand() {
        return hand_ != null && hand_.size() > 0 && !folded_;
    }

    /**
     * Returns whether this player should show a folded hand display.
     */
    public boolean showFoldedHand() {
        return folded_ && cardsExposed_;
    }

    /**
     * Returns display name, with optional AI/demo/host suffix for online games.
     */
    public String getDisplayName(boolean bOnline) {
        return getDisplayName(bOnline, true);
    }

    /**
     * Returns display name with optional AI/demo/host suffix.
     */
    public String getDisplayName(boolean bOnline, boolean bLong) {
        if (!bOnline)
            return getName();

        String sExtra = bLong ? "" : ".s";

        if (isComputer()) {
            return PropertyConfig.getMessage("msg.playername.ai" + sExtra, getName());
        }
        if (isHost()) {
            return PropertyConfig.getMessage("msg.playername.host" + sExtra, getName());
        }
        return getName();
    }

    /**
     * Fires settings changed event for online settings updates.
     */
    public void fireSettingsChanged() {
        firePropertyChange("settings", null, onlineSettings_);
    }

    /**
     * Short string representation for debug/logging.
     */
    public String toStringShort() {
        return getName() + " (seat=" + seat_ + " chips=" + chipCount_ + ")";
    }

    @Override
    public String toString() {
        return getName() != null ? getName() : "[unnamed-" + getID() + "]";
    }
}
