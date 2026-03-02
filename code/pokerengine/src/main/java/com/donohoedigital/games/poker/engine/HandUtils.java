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
package com.donohoedigital.games.poker.engine;

import java.util.ArrayList;

/**
 * Utility methods for hand evaluation display in the poker client.
 */
public class HandUtils {

    private HandUtils() {
    }

    /**
     * Return the best 5-card hand from the given hole cards and community cards.
     * Implements the same categorization logic as HandInfo.categorize().
     */
    public static Hand getBestFive(HandSorted hole, HandSorted community) {
        // Build sorted combined hand
        HandSorted all = new HandSorted(hole);
        if (community != null)
            all.addAll(community);

        int[] nNumRank = new int[Card.ACE + 1];
        int nSpades = 0, nHearts = 0, nDiamonds = 0, nClubs = 0;
        int nPairs = 0, nTrips = 0, nQuads = 0;
        ArrayList<Hand> seq_ = new ArrayList<>();
        ArrayList<Hand> seqFlush_ = new ArrayList<>();
        Hand best = new Hand();

        Hand seq = new Hand();
        seq_.add(seq);
        Hand seqFlush;
        Card last;
        Card c = null;

        for (int i = 0; i < all.size(); i++) {
            last = c;
            c = all.getCard(i);

            // straight sequences
            if (last == null || (last.getRank() + 1) == c.getRank()) {
                seq.addCard(c);
            } else if (last.getRank() != c.getRank()) {
                seq = new Hand();
                seq_.add(seq);
                seq.addCard(c);
            }

            // straight flush sequences
            last = null;
            seqFlush = getSeqForSuit(seqFlush_, c);
            if (seqFlush != null && !seqFlush.isEmpty())
                last = seqFlush.getCard(seqFlush.size() - 1);
            if (seqFlush != null && (last == null || (last.getRank() + 1) == c.getRank())) {
                seqFlush.addCard(c);
            } else {
                seqFlush = new Hand();
                seqFlush_.add(seqFlush);
                seqFlush.addCard(c);
            }

            // count suits
            if (c.isSpades())
                nSpades++;
            if (c.isHearts())
                nHearts++;
            if (c.isClubs())
                nClubs++;
            if (c.isDiamonds())
                nDiamonds++;

            nNumRank[c.getRank()]++;
        }

        // handle low aces for straights and straight flushes
        for (int i = all.size() - 1; i >= 0; i--) {
            c = all.getCard(i);
            if (c.getRank() < Card.ACE)
                break;
            for (Hand s : seq_) {
                if (!s.isEmpty() && s.getCard(0).getRank() == Card.TWO)
                    s.insertCard(c);
            }
            for (Hand s : seqFlush_) {
                if (!s.isEmpty() && s.getCard(0).getRank() == Card.TWO && s.getCard(0).isSameSuit(c))
                    s.insertCard(c);
            }
        }

        // count pairs, trips, quads
        for (int i = Card.TWO; i <= Card.ACE; i++) {
            if (nNumRank[i] == 2)
                nPairs++;
            if (nNumRank[i] == 3)
                nTrips++;
            if (nNumRank[i] == 4)
                nQuads++;
        }

        // determine best hand
        if (hasRoyalFlush(seqFlush_, best) || hasStraightFlush(seqFlush_, best)) {
            // best filled
        } else if (nQuads > 0) {
            fillQuads(all, nNumRank, best);
        } else if ((nTrips > 0 && nPairs > 0) || nTrips > 1) {
            fillFullHouse(all, nNumRank, best);
        } else if (nSpades >= 5) {
            fillSuit(all, CardSuit.SPADES, best);
        } else if (nHearts >= 5) {
            fillSuit(all, CardSuit.HEARTS, best);
        } else if (nDiamonds >= 5) {
            fillSuit(all, CardSuit.DIAMONDS, best);
        } else if (nClubs >= 5) {
            fillSuit(all, CardSuit.CLUBS, best);
        } else if (hasStraight(seq_, best)) {
            // best filled
        } else if (nTrips > 0) {
            fillTrips(all, nNumRank, best);
        } else if (nPairs >= 2) {
            fillTwoPair(all, nNumRank, best);
        } else if (nPairs == 1) {
            fillPair(all, nNumRank, best);
        } else {
            fillHighCard(all, best);
        }

        return best;
    }

    private static Hand getSeqForSuit(ArrayList<Hand> seqFlush_, Card c) {
        for (int i = seqFlush_.size() - 1; i >= 0; i--) {
            Hand s = seqFlush_.get(i);
            if (!s.isEmpty() && s.getCard(0).isSameSuit(c))
                return s;
        }
        return null;
    }

    private static boolean hasRoyalFlush(ArrayList<Hand> seqFlush_, Hand best) {
        for (Hand sf : seqFlush_) {
            if (sf.size() >= 5) {
                Card last = sf.getCard(sf.size() - 1);
                if (last.getRank() == Card.ACE) {
                    best.clear();
                    for (int j = 0; j < 5; j++)
                        best.addCard(sf.getCard(sf.size() - (j + 1)));
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasStraightFlush(ArrayList<Hand> seqFlush_, Hand best) {
        Card bestCard = null;
        for (Hand sf : seqFlush_) {
            if (sf.size() >= 5) {
                Card last = sf.getCard(sf.size() - 1);
                if (bestCard == null || last.getRank() > bestCard.getRank()) {
                    bestCard = last;
                    best.clear();
                    for (int j = 0; j < 5; j++)
                        best.addCard(sf.getCard(sf.size() - (j + 1)));
                }
            }
        }
        return bestCard != null;
    }

    private static void fillQuads(HandSorted all, int[] nNumRank, Hand best) {
        best.clear();
        for (int i = Card.ACE; i >= Card.TWO; i--) {
            if (nNumRank[i] == 4) {
                Hand dup = new Hand(all);
                for (int j = dup.size() - 1; j >= 0; j--) {
                    Card card = dup.getCard(j);
                    if (card.getRank() == i) {
                        dup.remove(j);
                        best.addCard(card);
                    }
                }
                best.addCard(dup.getCard(dup.size() - 1));
                return;
            }
        }
    }

    private static void fillFullHouse(HandSorted all, int[] nNumRank, Hand best) {
        best.clear();
        Hand dup = new Hand(all);
        int topset = 0;
        for (int i = Card.ACE; i >= Card.TWO; i--) {
            if (nNumRank[i] == 3) {
                topset = i;
                for (int j = dup.size() - 1; j >= 0; j--) {
                    Card card = dup.getCard(j);
                    if (card.getRank() == i) {
                        dup.remove(j);
                        best.addCard(card);
                    }
                }
                break;
            }
        }
        for (int i = Card.ACE; i >= Card.TWO; i--) {
            if (i == topset)
                continue;
            if (nNumRank[i] >= 2) {
                for (int j = dup.size() - 1; j >= 0; j--) {
                    Card card = dup.getCard(j);
                    if (card.getRank() == i && best.size() < 5) {
                        dup.remove(j);
                        best.addCard(card);
                    }
                }
                break;
            }
        }
    }

    private static void fillSuit(HandSorted all, CardSuit suit, Hand best) {
        best.clear();
        for (int j = all.size() - 1; best.size() < 5; j--) {
            Card c = all.getCard(j);
            if (c.getCardSuit() == suit)
                best.addCard(c);
        }
    }

    private static boolean hasStraight(ArrayList<Hand> seq_, Hand best) {
        Card bestCard = null;
        for (Hand s : seq_) {
            if (s.size() >= 5) {
                Card last = s.getCard(s.size() - 1);
                if (bestCard == null || last.getRank() > bestCard.getRank()) {
                    bestCard = last;
                    best.clear();
                    for (int j = 0; j < 5; j++)
                        best.addCard(s.getCard(s.size() - (j + 1)));
                }
            }
        }
        return bestCard != null;
    }

    private static void fillTrips(HandSorted all, int[] nNumRank, Hand best) {
        best.clear();
        for (int i = Card.ACE; i >= Card.TWO; i--) {
            if (nNumRank[i] == 3) {
                Hand dup = new Hand(all);
                for (int j = dup.size() - 1; j >= 0; j--) {
                    Card card = dup.getCard(j);
                    if (card.getRank() == i) {
                        dup.remove(j);
                        best.addCard(card);
                    }
                }
                best.addCard(dup.removeCard(dup.size() - 1));
                best.addCard(dup.removeCard(dup.size() - 1));
                return;
            }
        }
    }

    private static void fillTwoPair(HandSorted all, int[] nNumRank, Hand best) {
        best.clear();
        Hand dup = new Hand(all);
        int toppair = 0;
        for (int i = Card.ACE; i >= Card.TWO; i--) {
            if (nNumRank[i] == 2) {
                toppair = i;
                for (int j = dup.size() - 1; j >= 0; j--) {
                    Card card = dup.getCard(j);
                    if (card.getRank() == i) {
                        dup.remove(j);
                        best.addCard(card);
                    }
                }
                break;
            }
        }
        for (int i = Card.ACE; i >= Card.TWO; i--) {
            if (i == toppair)
                continue;
            if (nNumRank[i] == 2) {
                for (int j = dup.size() - 1; j >= 0; j--) {
                    Card card = dup.getCard(j);
                    if (card.getRank() == i) {
                        dup.remove(j);
                        best.addCard(card);
                    }
                }
                break;
            }
        }
        best.addCard(dup.removeCard(dup.size() - 1));
    }

    private static void fillPair(HandSorted all, int[] nNumRank, Hand best) {
        best.clear();
        Hand dup = new Hand(all);
        for (int i = Card.ACE; i >= Card.TWO; i--) {
            if (nNumRank[i] == 2) {
                for (int j = dup.size() - 1; j >= 0; j--) {
                    Card card = dup.getCard(j);
                    if (card.getRank() == i) {
                        dup.remove(j);
                        best.addCard(card);
                    }
                }
                break;
            }
        }
        best.addCard(dup.removeCard(dup.size() - 1));
        best.addCard(dup.removeCard(dup.size() - 1));
        best.addCard(dup.removeCard(dup.size() - 1));
    }

    private static void fillHighCard(HandSorted all, Hand best) {
        best.clear();
        for (int i = 0; i < all.size() && i < 5; i++) {
            best.addCard(all.getCard(all.size() - (1 + i)));
        }
    }
}
