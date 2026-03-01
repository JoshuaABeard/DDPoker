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
  knownOpponentHands?: string[][],
): EquityResult {
  const knownOppHands = knownOpponentHands || []
  const randomOpponents = numOpponents - knownOppHands.length

  // Remove all known cards from deck
  const knownCards = [...holeCards, ...communityCards, ...knownOppHands.flat()]
  const remainingDeck = removeCards(FULL_DECK, knownCards)
  const communityNeeded = 5 - communityCards.length

  let wins = 0
  let ties = 0
  let losses = 0
  const oppWins = new Array(numOpponents).fill(0)
  const oppTies = new Array(numOpponents).fill(0)
  const oppLosses = new Array(numOpponents).fill(0)

  for (let i = 0; i < iterations; i++) {
    const shuffled = shuffle([...remainingDeck])
    let dealIndex = 0

    // Deal remaining community cards
    const board = [...communityCards]
    for (let j = 0; j < communityNeeded; j++) {
      board.push(shuffled[dealIndex++])
    }

    // Build opponent hands: known first, then random
    const opponentHands: string[][] = []
    for (const known of knownOppHands) {
      opponentHands.push(known)
    }
    for (let o = 0; o < randomOpponents; o++) {
      opponentHands.push([shuffled[dealIndex++], shuffled[dealIndex++]])
    }

    // Evaluate player hand
    const playerHand = evaluateHand([...holeCards, ...board])

    // Compare against all opponents
    let playerWins = true
    let playerTies = false
    for (let o = 0; o < opponentHands.length; o++) {
      const oppHand = evaluateHand([...opponentHands[o], ...board])
      const cmp = compareHands(playerHand, oppHand)
      if (cmp < 0) {
        playerWins = false
        oppWins[o]++
      } else if (cmp === 0) {
        playerTies = true
        oppTies[o]++
      } else {
        oppLosses[o]++
      }
    }

    if (!playerWins) losses++
    else if (playerTies) ties++
    else wins++
  }

  const result: EquityResult = {
    win: (wins / iterations) * 100,
    tie: (ties / iterations) * 100,
    loss: (losses / iterations) * 100,
    iterations,
  }

  if (knownOpponentHands && knownOpponentHands.length > 0) {
    result.opponentResults = []
    for (let i = 0; i < numOpponents; i++) {
      result.opponentResults.push({
        win: (oppWins[i] / iterations) * 100,
        tie: (oppTies[i] / iterations) * 100,
        loss: (oppLosses[i] / iterations) * 100,
      })
    }
  }

  return result
}
