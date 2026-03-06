/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.display.ClientCard;
import com.donohoedigital.games.poker.display.ClientHand;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandSorted;

/**
 * Temporary bridge between ClientHand (display) and engine Hand/HandSorted
 * types. Used where poker module code still needs to call engine evaluation
 * methods (HandInfoFast, HandUtils) that require engine Hand parameters.
 *
 * <p>
 * This adapter will be removed when hand evaluation is either moved to the
 * server or wrapped in a display-friendly API.
 */
public final class EngineAdapter {

    private EngineAdapter() {
    }

    /** Convert a ClientHand to an engine Hand. */
    public static Hand toHand(ClientHand clientHand) {
        if (clientHand == null) {
            return new Hand();
        }
        Hand h = new Hand(clientHand.size());
        for (int i = 0; i < clientHand.size(); i++) {
            ClientCard cc = clientHand.getCard(i);
            h.addCard(Card.getCard(cc.getSuit(), cc.getRank()));
        }
        return h;
    }

    /** Convert a ClientHand to an engine HandSorted. */
    public static HandSorted toHandSorted(ClientHand clientHand) {
        return new HandSorted(toHand(clientHand));
    }

    /** Convert an engine Hand to a ClientHand. */
    public static ClientHand toClientHand(Hand hand) {
        if (hand == null) {
            return ClientHand.empty();
        }
        ClientHand ch = ClientHand.empty();
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.getCard(i);
            ch.addCard(ClientCard.getCard(c.getSuit(), c.getRank()));
        }
        return ch;
    }
}
