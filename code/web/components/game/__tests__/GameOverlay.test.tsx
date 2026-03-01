/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { GameOverlay } from '../GameOverlay'

describe('GameOverlay - paused', () => {
  it('renders reason and pausedBy with correct dialog label', () => {
    render(<GameOverlay type="paused" reason="Break time" pausedBy="Alice" />)
    const dialog = screen.getByRole('dialog')
    expect(dialog.getAttribute('aria-label')).toBe('Game Paused')
    expect(screen.getByText('Break time')).toBeTruthy()
    expect(screen.getByText(/paused by alice/i)).toBeTruthy()
  })
})

describe('GameOverlay - eliminated', () => {
  it('renders finish position and fires onClose on button click', () => {
    const onClose = vi.fn()
    render(<GameOverlay type="eliminated" finishPosition={3} onClose={onClose} />)
    const dialog = screen.getByRole('dialog')
    expect(dialog.getAttribute('aria-label')).toBe('You Have Been Eliminated')
    expect(screen.getByText(/position 3/i)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /continue watching/i }))
    expect(onClose).toHaveBeenCalled()
  })
})

describe('GameOverlay - tab-replaced', () => {
  it('renders with correct dialog label', () => {
    render(<GameOverlay type="tab-replaced" />)
    const dialog = screen.getByRole('dialog')
    expect(dialog.getAttribute('aria-label')).toBe('Game Opened in Another Tab')
  })
})

describe('GameOverlay - continueRunout', () => {
  it('fires onContinue on button click', () => {
    const onContinue = vi.fn()
    render(<GameOverlay type="continueRunout" onContinue={onContinue} />)
    const dialog = screen.getByRole('dialog')
    expect(dialog.getAttribute('aria-label')).toBe('Continue Runout')
    fireEvent.click(screen.getByRole('button', { name: /^continue$/i }))
    expect(onContinue).toHaveBeenCalled()
  })
})

describe('GameOverlay - rebuy', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders chip and cost info', () => {
    render(
      <GameOverlay
        type="rebuy"
        cost={500}
        chips={1000}
        timeoutSeconds={30}
        onDecision={vi.fn()}
      />,
    )
    const dialog = screen.getByRole('dialog')
    expect(dialog.getAttribute('aria-label')).toBe('Rebuy Available')
    // formatChips(1000) = "1,000", formatChips(500) = "500"
    expect(screen.getByText(/1,000/)).toBeTruthy()
    expect(screen.getByText(/500/)).toBeTruthy()
  })

  it('countdown ticks each second', () => {
    render(
      <GameOverlay
        type="rebuy"
        cost={500}
        chips={1000}
        timeoutSeconds={10}
        onDecision={vi.fn()}
      />,
    )
    expect(screen.getByText('Time remaining: 10s')).toBeTruthy()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('Time remaining: 9s')).toBeTruthy()
  })

  it('Accept button calls onDecision(true)', () => {
    const onDecision = vi.fn()
    render(
      <GameOverlay
        type="rebuy"
        cost={500}
        chips={1000}
        timeoutSeconds={30}
        onDecision={onDecision}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /^accept$/i }))
    expect(onDecision).toHaveBeenCalledWith(true)
  })

  it('Decline button calls onDecision(false)', () => {
    const onDecision = vi.fn()
    render(
      <GameOverlay
        type="rebuy"
        cost={500}
        chips={1000}
        timeoutSeconds={30}
        onDecision={onDecision}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /^decline$/i }))
    expect(onDecision).toHaveBeenCalledWith(false)
  })
})

describe('GameOverlay - addon', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders with correct dialog label', () => {
    render(
      <GameOverlay
        type="addon"
        cost={200}
        chips={500}
        timeoutSeconds={30}
        onDecision={vi.fn()}
      />,
    )
    const dialog = screen.getByRole('dialog')
    expect(dialog.getAttribute('aria-label')).toBe('Add-On Available')
  })

  it('Accept button calls onDecision(true)', () => {
    const onDecision = vi.fn()
    render(
      <GameOverlay
        type="addon"
        cost={200}
        chips={500}
        timeoutSeconds={30}
        onDecision={onDecision}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /^accept$/i }))
    expect(onDecision).toHaveBeenCalledWith(true)
  })
})

describe('GameOverlay - neverBroke', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders with correct dialog label and countdown ticks', () => {
    render(
      <GameOverlay type="neverBroke" timeoutSeconds={15} onDecision={vi.fn()} />,
    )
    const dialog = screen.getByRole('dialog')
    expect(dialog.getAttribute('aria-label')).toBe('Never Broke Offer')
    expect(screen.getByText('Time remaining: 15s')).toBeTruthy()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('Time remaining: 14s')).toBeTruthy()
  })

  it('Accept calls onDecision(true)', () => {
    const onDecision = vi.fn()
    render(
      <GameOverlay type="neverBroke" timeoutSeconds={15} onDecision={onDecision} />,
    )
    fireEvent.click(screen.getByRole('button', { name: /^accept$/i }))
    expect(onDecision).toHaveBeenCalledWith(true)
  })

  it('Decline calls onDecision(false)', () => {
    const onDecision = vi.fn()
    render(
      <GameOverlay type="neverBroke" timeoutSeconds={15} onDecision={onDecision} />,
    )
    fireEvent.click(screen.getByRole('button', { name: /^decline$/i }))
    expect(onDecision).toHaveBeenCalledWith(false)
  })
})
