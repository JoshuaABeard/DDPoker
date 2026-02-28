/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { FULL_DECK, shuffle, removeCards } from './deck'
import { evaluateHand, compareHands } from './handEvaluator'
import type { EquityResult } from './types'

/**
 * Monte Carlo equity calculator.
 * Deals random remaining cards to complete the board and opponents' hands,
 * then evaluates all hands to determine win/tie/loss rates.
 */
export function calculateEquity(
  holeCards: string[],
  communityCards: string[],
  numOpponents: number,
  iterations: number = 10000,
): EquityResult {
  const knownCards = [...holeCards, ...communityCards]
  const remainingDeck = removeCards(FULL_DECK, knownCards)
  const communityNeeded = 5 - communityCards.length

  let wins = 0
  let ties = 0
  let losses = 0

  for (let i = 0; i < iterations; i++) {
    const shuffled = shuffle([...remainingDeck])
    let dealIndex = 0

    // Deal remaining community cards
    const board = [...communityCards]
    for (let j = 0; j < communityNeeded; j++) {
      board.push(shuffled[dealIndex++])
    }

    // Deal opponent hands
    const opponentHands: string[][] = []
    for (let o = 0; o < numOpponents; o++) {
      opponentHands.push([shuffled[dealIndex++], shuffled[dealIndex++]])
    }

    // Evaluate player hand
    const playerHand = evaluateHand([...holeCards, ...board])

    // Compare against all opponents
    let playerWins = true
    let playerTies = false
    for (const oppHole of opponentHands) {
      const oppHand = evaluateHand([...oppHole, ...board])
      const cmp = compareHands(playerHand, oppHand)
      if (cmp < 0) {
        playerWins = false
        break
      }
      if (cmp === 0) playerTies = true
    }

    if (!playerWins) losses++
    else if (playerTies) ties++
    else wins++
  }

  return {
    win: (wins / iterations) * 100,
    tie: (ties / iterations) * 100,
    loss: (losses / iterations) * 100,
    iterations,
  }
}
