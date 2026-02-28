/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { evaluateHand, compareHands } from '../handEvaluator'
import { HandRank } from '../types'

describe('evaluateHand', () => {
  it('detects royal flush', () => {
    const result = evaluateHand(['Ah', 'Kh', 'Qh', 'Jh', 'Th'])
    expect(result.rank).toBe(HandRank.ROYAL_FLUSH)
  })

  it('detects straight flush', () => {
    const result = evaluateHand(['9c', '8c', '7c', '6c', '5c'])
    expect(result.rank).toBe(HandRank.STRAIGHT_FLUSH)
  })

  it('detects four of a kind', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'As', 'Kh'])
    expect(result.rank).toBe(HandRank.FOUR_OF_A_KIND)
  })

  it('detects full house', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'Kh', 'Kd'])
    expect(result.rank).toBe(HandRank.FULL_HOUSE)
  })

  it('detects flush', () => {
    const result = evaluateHand(['Ah', 'Jh', '9h', '5h', '3h'])
    expect(result.rank).toBe(HandRank.FLUSH)
  })

  it('detects straight', () => {
    const result = evaluateHand(['Ah', 'Kd', 'Qc', 'Js', 'Th'])
    expect(result.rank).toBe(HandRank.STRAIGHT)
  })

  it('detects ace-low straight (wheel)', () => {
    const result = evaluateHand(['Ah', '2d', '3c', '4s', '5h'])
    expect(result.rank).toBe(HandRank.STRAIGHT)
  })

  it('detects three of a kind', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'Ks', '9h'])
    expect(result.rank).toBe(HandRank.THREE_OF_A_KIND)
  })

  it('detects two pair', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Kc', 'Ks', '9h'])
    expect(result.rank).toBe(HandRank.TWO_PAIR)
  })

  it('detects one pair', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Kc', 'Qs', '9h'])
    expect(result.rank).toBe(HandRank.ONE_PAIR)
  })

  it('detects high card', () => {
    const result = evaluateHand(['Ah', 'Jd', '9c', '5s', '3h'])
    expect(result.rank).toBe(HandRank.HIGH_CARD)
  })

  it('evaluates 7-card hand (picks best 5)', () => {
    const result = evaluateHand(['Ah', 'Kh', 'Qh', 'Jh', 'Th', '3c', '2d'])
    expect(result.rank).toBe(HandRank.ROYAL_FLUSH)
  })

  it('evaluates 6-card hand', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'Kh', 'Kd', '2c'])
    expect(result.rank).toBe(HandRank.FULL_HOUSE)
  })

  it('ace-low straight flush', () => {
    const result = evaluateHand(['Ah', '2h', '3h', '4h', '5h'])
    expect(result.rank).toBe(HandRank.STRAIGHT_FLUSH)
  })

  it('generates correct description for two pair', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Kc', 'Ks', '9h'])
    expect(result.description).toContain('Two Pair')
  })

  it('generates correct description for full house', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'Kh', 'Kd'])
    expect(result.description).toContain('Full House')
  })
})

describe('compareHands', () => {
  it('higher rank wins', () => {
    const flush = evaluateHand(['Ah', 'Jh', '9h', '5h', '3h'])
    const straight = evaluateHand(['Ah', 'Kd', 'Qc', 'Js', 'Th'])
    expect(compareHands(flush, straight)).toBeGreaterThan(0)
  })

  it('same rank uses kickers', () => {
    const aceHigh = evaluateHand(['Ah', 'Kd', '9c', '5s', '3h'])
    const kingHigh = evaluateHand(['Kh', 'Qd', '9c', '5s', '3h'])
    expect(compareHands(aceHigh, kingHigh)).toBeGreaterThan(0)
  })

  it('identical hands return 0', () => {
    const hand1 = evaluateHand(['Ah', 'Kd', 'Qc', 'Js', '9h'])
    const hand2 = evaluateHand(['Ac', 'Ks', 'Qh', 'Jd', '9c'])
    expect(compareHands(hand1, hand2)).toBe(0)
  })

  it('higher pair beats lower pair', () => {
    const aces = evaluateHand(['Ah', 'Ad', 'Kc', 'Qs', '9h'])
    const kings = evaluateHand(['Kh', 'Kd', 'Ac', 'Qs', '9h'])
    expect(compareHands(aces, kings)).toBeGreaterThan(0)
  })

  it('higher two pair beats lower two pair', () => {
    const akPair = evaluateHand(['Ah', 'Ad', 'Kc', 'Ks', '9h'])
    const aqPair = evaluateHand(['Ah', 'Ad', 'Qc', 'Qs', '9h'])
    expect(compareHands(akPair, aqPair)).toBeGreaterThan(0)
  })

  it('two pair kicker matters', () => {
    const nineKicker = evaluateHand(['Ah', 'Ad', 'Kc', 'Ks', '9h'])
    const eightKicker = evaluateHand(['Ah', 'Ad', 'Kc', 'Ks', '8h'])
    expect(compareHands(nineKicker, eightKicker)).toBeGreaterThan(0)
  })
})
