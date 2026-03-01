/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ActionTimer } from '../ActionTimer'

describe('ActionTimer', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders initial seconds from totalSeconds when remainingSeconds is null', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={null} />)
    // Math.ceil(30) = 30
    expect(screen.getByText('30s')).toBeTruthy()
  })

  it('renders remainingSeconds when provided', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={20} />)
    expect(screen.getByText('20s')).toBeTruthy()
  })

  it('counts down each second via advanceTimersByTime', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={10} />)
    expect(screen.getByText('10s')).toBeTruthy()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('9s')).toBeTruthy()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('8s')).toBeTruthy()
  })

  it('does not go below 0', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={1} />)
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('0s')).toBeTruthy()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    // Should still be 0, not negative
    expect(screen.getByText('0s')).toBeTruthy()
  })

  it('resyncs when remainingSeconds prop changes', () => {
    const { rerender } = render(<ActionTimer totalSeconds={30} remainingSeconds={20} />)
    expect(screen.getByText('20s')).toBeTruthy()

    rerender(<ActionTimer totalSeconds={30} remainingSeconds={15} />)
    expect(screen.getByText('15s')).toBeTruthy()
  })

  it('aria-label shows seconds remaining', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={25} />)
    const timer = screen.getByRole('timer')
    expect(timer.getAttribute('aria-label')).toBe('25 seconds remaining')
  })

  it('aria-label uses Math.round for display', () => {
    // remainingSeconds is an integer so Math.round and Math.ceil are the same;
    // the label uses Math.round per the source
    render(<ActionTimer totalSeconds={30} remainingSeconds={10} />)
    const timer = screen.getByRole('timer')
    expect(timer.getAttribute('aria-label')).toBe('10 seconds remaining')
  })

  it('has role=timer', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={null} />)
    expect(screen.getByRole('timer')).toBeTruthy()
  })
})
