/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { PlayerStatsSection } from '../PlayerStatsSection'

// Mock the tournament API
vi.mock('@/lib/api', () => ({
  tournamentApi: {
    getHistory: vi.fn(),
  },
}))

import { tournamentApi } from '@/lib/api'

const MOCK_HISTORY = {
  history: [
    { id: 1, name: 'Game 1', placement: 1, buyIn: 100, prize: 500, totalPlayers: 5, endDate: '2026-01-15' },
    { id: 2, name: 'Game 2', placement: 3, buyIn: 100, prize: 50, totalPlayers: 6, endDate: '2026-01-20' },
    { id: 3, name: 'Game 3', placement: 2, buyIn: 200, prize: 300, totalPlayers: 4, endDate: '2026-02-01' },
  ],
  total: 3,
}

describe('PlayerStatsSection', () => {
  beforeEach(() => {
    vi.mocked(tournamentApi.getHistory).mockResolvedValue(MOCK_HISTORY)
  })

  it('renders summary stats after loading', async () => {
    render(<PlayerStatsSection username="TestPlayer" />)

    await waitFor(() => {
      expect(screen.getByText('3')).toBeTruthy() // total games
    })

    // Check that key stat labels exist
    expect(screen.getByText(/Games Played/i)).toBeTruthy()
    expect(screen.getByText(/Win Rate/i)).toBeTruthy()
    expect(screen.getAllByText(/Profit/i).length).toBeGreaterThan(0)
  })

  it('renders the P/L chart SVG', async () => {
    render(<PlayerStatsSection username="TestPlayer" />)

    await waitFor(() => {
      expect(screen.getByTestId('pl-chart')).toBeTruthy()
    })
  })

  it('shows loading state initially', () => {
    render(<PlayerStatsSection username="TestPlayer" />)
    expect(screen.getByText(/Loading/i)).toBeTruthy()
  })

  it('shows empty state when no history', async () => {
    vi.mocked(tournamentApi.getHistory).mockResolvedValue({ history: [], total: 0 })
    render(<PlayerStatsSection username="TestPlayer" />)

    await waitFor(() => {
      expect(screen.getByText(/No tournament history/i)).toBeTruthy()
    })
  })
})
