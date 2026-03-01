/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { Simulator } from '../Simulator'
import type { EquityResult } from '@/lib/poker/types'

// Mock the Card component to avoid image loading in tests
vi.mock('../Card', () => ({
  Card: ({ card }: { card?: string }) => <span data-testid={`card-img-${card}`}>{card}</span>,
}))

// Mock the API module following project conventions
vi.mock('@/lib/api', () => ({
  gameServerApi: {
    simulate: vi.fn(),
  },
}))

import { gameServerApi } from '@/lib/api'

const mockResult: EquityResult = {
  win: 65.3,
  tie: 2.1,
  loss: 32.6,
  iterations: 10000,
}

function mockSimulateSuccess(result: EquityResult = mockResult) {
  vi.mocked(gameServerApi.simulate).mockResolvedValue(result)
}

function mockSimulateError() {
  vi.mocked(gameServerApi.simulate).mockRejectedValue(new Error('Simulation failed'))
}

describe('Simulator', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    mockSimulateSuccess()
  })

  it('renders with title "Poker Simulator"', () => {
    render(<Simulator onClose={vi.fn()} />)
    expect(screen.getByText('Poker Simulator')).toBeDefined()
  })

  it('shows 7 card slots (2 hole + 5 community)', () => {
    render(<Simulator onClose={vi.fn()} />)
    const slots = screen.getAllByTestId('card-slot')
    expect(slots).toHaveLength(7)
  })

  it('close button calls onClose', () => {
    const onClose = vi.fn()
    render(<Simulator onClose={onClose} />)
    fireEvent.click(screen.getByLabelText('Close simulator'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('Load from Game button populates cards from props', () => {
    render(
      <Simulator
        currentHoleCards={['Ah', 'Kd']}
        currentCommunityCards={['Qs', 'Jc', '2h']}
        onClose={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByText('Load from Game'))
    expect(screen.getByTestId('card-img-Ah')).toBeDefined()
    expect(screen.getByTestId('card-img-Kd')).toBeDefined()
    expect(screen.getByTestId('card-img-Qs')).toBeDefined()
    expect(screen.getByTestId('card-img-Jc')).toBeDefined()
    expect(screen.getByTestId('card-img-2h')).toBeDefined()
  })

  it('Calculate button is disabled when no hole cards selected', () => {
    render(<Simulator onClose={vi.fn()} />)
    const calcButton = screen.getByText('Calculate')
    expect((calcButton as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows results after calculation', async () => {
    render(
      <Simulator
        currentHoleCards={['Ah', 'Kd']}
        onClose={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByText('Load from Game'))
    fireEvent.click(screen.getByText('Calculate'))

    await waitFor(() => {
      expect(screen.getByTestId('results')).toBeDefined()
    })
    expect(screen.getByText('65.3%')).toBeDefined()
    expect(screen.getByText('2.1%')).toBeDefined()
    expect(screen.getByText('32.6%')).toBeDefined()
    expect(screen.getByText(/10,000/)).toBeDefined()
  })

  it('sends correct request body to server', async () => {
    render(
      <Simulator
        currentHoleCards={['Ah', 'Kd']}
        currentCommunityCards={['Qs', 'Jc', '2h']}
        onClose={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByText('Load from Game'))
    fireEvent.click(screen.getByText('Calculate'))

    await waitFor(() => {
      expect(gameServerApi.simulate).toHaveBeenCalledOnce()
    })

    const params = vi.mocked(gameServerApi.simulate).mock.calls[0][0]
    expect(params.holeCards).toEqual(['Ah', 'Kd'])
    expect(params.communityCards).toEqual(['Qs', 'Jc', '2h'])
    expect(params.numOpponents).toBe(1)
    expect(params.iterations).toBe(10000)
  })

  it('shows loading state during calculation', async () => {
    let resolveSimulate: (value: EquityResult) => void
    vi.mocked(gameServerApi.simulate).mockReturnValue(
      new Promise((resolve) => {
        resolveSimulate = resolve
      }),
    )

    render(
      <Simulator
        currentHoleCards={['Ah', 'Kd']}
        onClose={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByText('Load from Game'))
    fireEvent.click(screen.getByText('Calculate'))

    expect(screen.getByText('Calculating...')).toBeDefined()

    resolveSimulate!(mockResult)

    await waitFor(() => {
      expect(screen.getByText('Calculate')).toBeDefined()
    })
  })

  it('handles server error gracefully and displays error message', async () => {
    mockSimulateError()
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <Simulator
        currentHoleCards={['Ah', 'Kd']}
        onClose={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByText('Load from Game'))
    fireEvent.click(screen.getByText('Calculate'))

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith('Simulation error:', expect.any(Error))
    })
    // Error message should be displayed to the user
    expect(screen.getByRole('alert')).toBeDefined()
    expect(screen.getByText('Simulation failed. Please try again.')).toBeDefined()
    // Button should be re-enabled after error
    expect((screen.getByText('Calculate') as HTMLButtonElement).disabled).toBe(false)
    // No results should be displayed
    expect(screen.queryByTestId('results')).toBeNull()

    consoleSpy.mockRestore()
  })

  it('clears error on next successful calculation', async () => {
    mockSimulateError()
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <Simulator
        currentHoleCards={['Ah', 'Kd']}
        onClose={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByText('Load from Game'))
    fireEvent.click(screen.getByText('Calculate'))

    await waitFor(() => {
      expect(screen.getByText('Simulation failed. Please try again.')).toBeDefined()
    })

    // Now succeed
    mockSimulateSuccess()
    fireEvent.click(screen.getByText('Calculate'))

    await waitFor(() => {
      expect(screen.getByTestId('results')).toBeDefined()
    })
    expect(screen.queryByText('Simulation failed. Please try again.')).toBeNull()

    consoleSpy.mockRestore()
  })

  it('displays opponent results when returned', async () => {
    mockSimulateSuccess({
      win: 75.3,
      tie: 1.2,
      loss: 23.5,
      iterations: 10000,
      opponentResults: [{ win: 75.3, tie: 1.2, loss: 23.5 }],
    })

    render(
      <Simulator
        currentHoleCards={['Ah', 'Kd']}
        onClose={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByText('Load from Game'))
    fireEvent.click(screen.getByText('Calculate'))

    await waitFor(() => {
      expect(screen.getByTestId('results')).toBeDefined()
    })
    expect(screen.getByText('Per-Opponent Breakdown')).toBeDefined()
    expect(screen.getByText('75.3% W')).toBeDefined()
  })

  it('does not show Load from Game button without game cards', () => {
    render(<Simulator onClose={vi.fn()} />)
    expect(screen.queryByText('Load from Game')).toBeNull()
  })

  it('opens card picker when clicking an empty slot', () => {
    render(<Simulator onClose={vi.fn()} />)
    const slots = screen.getAllByTestId('card-slot')
    // Click the first hole card slot
    fireEvent.click(slots[0].querySelector('button')!)
    // CardPicker should appear — look for its close button text
    expect(screen.getByText('Close')).toBeDefined()
  })

  it('adjusts opponent count with +/- buttons', () => {
    render(<Simulator onClose={vi.fn()} />)
    const count = screen.getByTestId('opponent-count')
    expect(count.textContent).toBe('1')
    fireEvent.click(screen.getByLabelText('Increase opponents'))
    expect(count.textContent).toBe('2')
    fireEvent.click(screen.getByLabelText('Decrease opponents'))
    expect(count.textContent).toBe('1')
  })

  it('does not decrease opponents below 1', () => {
    render(<Simulator onClose={vi.fn()} />)
    const count = screen.getByTestId('opponent-count')
    fireEvent.click(screen.getByLabelText('Decrease opponents'))
    expect(count.textContent).toBe('1')
  })

  it('does not increase opponents above 9', () => {
    render(<Simulator onClose={vi.fn()} />)
    const count = screen.getByTestId('opponent-count')
    for (let i = 0; i < 10; i++) {
      fireEvent.click(screen.getByLabelText('Increase opponents'))
    }
    expect(count.textContent).toBe('9')
  })

  it('Clear All resets all card slots', () => {
    render(
      <Simulator
        currentHoleCards={['Ah', 'Kd']}
        currentCommunityCards={['Qs']}
        onClose={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByText('Load from Game'))
    expect(screen.getByTestId('card-img-Ah')).toBeDefined()
    fireEvent.click(screen.getByText('Clear All'))
    expect(screen.queryByTestId('card-img-Ah')).toBeNull()
    expect(screen.queryByTestId('card-img-Kd')).toBeNull()
    expect(screen.queryByTestId('card-img-Qs')).toBeNull()
  })
})
