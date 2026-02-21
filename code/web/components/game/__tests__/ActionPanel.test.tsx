/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ActionPanel } from '../ActionPanel'
import type { ActionOptionsData } from '@/lib/game/types'

// BetSlider is an interactive range input — mock it to keep tests simple
vi.mock('../BetSlider', () => ({
  BetSlider: ({
    onChange,
    min,
  }: {
    onChange: (v: number) => void
    min: number
  }) => (
    <input
      type="range"
      data-testid="bet-slider"
      defaultValue={min}
      onChange={(e) => onChange(Number(e.target.value))}
    />
  ),
}))

function makeOptions(overrides: Partial<ActionOptionsData> = {}): ActionOptionsData {
  return {
    canFold: false,
    canCheck: false,
    canCall: false,
    callAmount: 0,
    canBet: false,
    minBet: 100,
    maxBet: 1000,
    canRaise: false,
    minRaise: 200,
    maxRaise: 2000,
    canAllIn: false,
    allInAmount: 5000,
    ...overrides,
  }
}

describe('ActionPanel', () => {
  // ---------------------------------------------------------------------------
  // Button visibility
  // ---------------------------------------------------------------------------

  it('shows Fold button when canFold is true', () => {
    render(<ActionPanel options={makeOptions({ canFold: true })} potSize={500} onAction={vi.fn()} />)
    expect(screen.getByRole('button', { name: /fold/i })).toBeTruthy()
  })

  it('does not show Fold button when canFold is false', () => {
    render(<ActionPanel options={makeOptions({ canFold: false })} potSize={500} onAction={vi.fn()} />)
    expect(screen.queryByRole('button', { name: /fold/i })).toBeNull()
  })

  it('shows Check button when canCheck is true', () => {
    render(<ActionPanel options={makeOptions({ canCheck: true })} potSize={500} onAction={vi.fn()} />)
    expect(screen.getByRole('button', { name: /check/i })).toBeTruthy()
  })

  it('does not show Check button when canCheck is false', () => {
    render(<ActionPanel options={makeOptions({ canCheck: false })} potSize={500} onAction={vi.fn()} />)
    expect(screen.queryByRole('button', { name: /check/i })).toBeNull()
  })

  it('shows Call button with formatted amount when canCall is true', () => {
    render(
      <ActionPanel
        options={makeOptions({ canCall: true, callAmount: 300 })}
        potSize={500}
        onAction={vi.fn()}
      />,
    )
    const callBtn = screen.getByRole('button', { name: /call/i })
    expect(callBtn).toBeTruthy()
    expect(callBtn.textContent).toContain('300')
  })

  it('shows Raise button when canRaise is true', () => {
    render(
      <ActionPanel
        options={makeOptions({ canRaise: true, minRaise: 200, maxRaise: 2000 })}
        potSize={500}
        onAction={vi.fn()}
      />,
    )
    expect(screen.getByRole('button', { name: /raise/i })).toBeTruthy()
  })

  it('shows All-In button with formatted amount when canAllIn is true', () => {
    render(
      <ActionPanel
        options={makeOptions({ canAllIn: true, allInAmount: 5000 })}
        potSize={500}
        onAction={vi.fn()}
      />,
    )
    const allInBtn = screen.getByRole('button', { name: /all.in/i })
    expect(allInBtn.textContent).toContain('5,000')
  })

  // ---------------------------------------------------------------------------
  // Click actions
  // ---------------------------------------------------------------------------

  it('calls onAction with FOLD when Fold is clicked', () => {
    const onAction = vi.fn()
    render(<ActionPanel options={makeOptions({ canFold: true })} potSize={500} onAction={onAction} />)
    fireEvent.click(screen.getByRole('button', { name: /fold/i }))
    expect(onAction).toHaveBeenCalledWith({ action: 'FOLD', amount: 0 })
  })

  it('calls onAction with CHECK when Check is clicked', () => {
    const onAction = vi.fn()
    render(<ActionPanel options={makeOptions({ canCheck: true })} potSize={500} onAction={onAction} />)
    fireEvent.click(screen.getByRole('button', { name: /check/i }))
    expect(onAction).toHaveBeenCalledWith({ action: 'CHECK', amount: 0 })
  })

  it('calls onAction with CALL and callAmount when Call is clicked', () => {
    const onAction = vi.fn()
    render(
      <ActionPanel
        options={makeOptions({ canCall: true, callAmount: 300 })}
        potSize={500}
        onAction={onAction}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /call/i }))
    expect(onAction).toHaveBeenCalledWith({ action: 'CALL', amount: 300 })
  })

  it('calls onAction with ALL_IN and allInAmount when All-In is clicked', () => {
    const onAction = vi.fn()
    render(
      <ActionPanel
        options={makeOptions({ canAllIn: true, allInAmount: 5000 })}
        potSize={500}
        onAction={onAction}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /all.in/i }))
    expect(onAction).toHaveBeenCalledWith({ action: 'ALL_IN', amount: 5000 })
  })

  // ---------------------------------------------------------------------------
  // Raise/Bet slider flow
  // ---------------------------------------------------------------------------

  it('does not show bet slider before Raise is clicked', () => {
    render(
      <ActionPanel
        options={makeOptions({ canRaise: true, minRaise: 200, maxRaise: 2000 })}
        potSize={500}
        onAction={vi.fn()}
      />,
    )
    expect(screen.queryByTestId('bet-slider')).toBeNull()
  })

  it('shows bet slider and Confirm/Cancel after Raise is clicked', () => {
    render(
      <ActionPanel
        options={makeOptions({ canRaise: true, minRaise: 200, maxRaise: 2000 })}
        potSize={500}
        onAction={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /raise/i }))
    expect(screen.getByTestId('bet-slider')).toBeTruthy()
    expect(screen.getByRole('button', { name: /confirm/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancel/i })).toBeTruthy()
  })

  it('calls onAction with RAISE and minRaise on Confirm (default slider value)', () => {
    const onAction = vi.fn()
    render(
      <ActionPanel
        options={makeOptions({ canRaise: true, minRaise: 200, maxRaise: 2000 })}
        potSize={500}
        onAction={onAction}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /raise/i }))
    fireEvent.click(screen.getByRole('button', { name: /confirm/i }))
    expect(onAction).toHaveBeenCalledWith({ action: 'RAISE', amount: 200 })
  })

  it('hides slider and shows Raise button again after Cancel', () => {
    render(
      <ActionPanel
        options={makeOptions({ canRaise: true, minRaise: 200, maxRaise: 2000 })}
        potSize={500}
        onAction={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /raise/i }))
    expect(screen.getByTestId('bet-slider')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }))
    expect(screen.queryByTestId('bet-slider')).toBeNull()
    // Raise button should be visible again
    expect(screen.getByRole('button', { name: /raise/i })).toBeTruthy()
  })

  it('shows bet slider for Bet flow', () => {
    const onAction = vi.fn()
    render(
      <ActionPanel
        options={makeOptions({ canBet: true, minBet: 100, maxBet: 1000 })}
        potSize={500}
        onAction={onAction}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /^bet$/i }))
    expect(screen.getByTestId('bet-slider')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /confirm/i }))
    expect(onAction).toHaveBeenCalledWith({ action: 'BET', amount: 100 })
  })

  // ---------------------------------------------------------------------------
  // Large chip count formatting
  // ---------------------------------------------------------------------------

  it('formats large chip amounts with thousands separator', () => {
    render(
      <ActionPanel
        options={makeOptions({ canCall: true, callAmount: 1_500_000 })}
        potSize={5_000_000}
        onAction={vi.fn()}
      />,
    )
    const callBtn = screen.getByRole('button', { name: /call/i })
    expect(callBtn.textContent).toContain('1,500,000')
  })
})
