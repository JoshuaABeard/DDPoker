/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { StartingHandsChart } from '../StartingHandsChart'

describe('StartingHandsChart', () => {
  it('renders 169 grid cells (13x13)', () => {
    render(<StartingHandsChart />)
    const cells = screen.getAllByTestId('hand-cell')
    expect(cells).toHaveLength(169)
  })

  it('highlights current hand cell with ring styling', () => {
    render(<StartingHandsChart currentHand={['Ah', 'Kd']} />)
    const cells = screen.getAllByTestId('hand-cell')
    // AKo = row 1 (K), col 0 (A) => cell index = 1*13 + 0 = 13
    const akCell = cells[13]
    expect(akCell.className).toContain('ring-2')
    expect(akCell.className).toContain('ring-white')
  })

  it('highlights suited hand correctly', () => {
    render(<StartingHandsChart currentHand={['Ah', 'Kh']} />)
    const cells = screen.getAllByTestId('hand-cell')
    // AKs = row 0 (A), col 1 (K) => cell index = 0*13 + 1 = 1
    const aksCell = cells[1]
    expect(aksCell.className).toContain('ring-2')
  })

  it('highlights pocket pair correctly', () => {
    render(<StartingHandsChart currentHand={['As', 'Ah']} />)
    const cells = screen.getAllByTestId('hand-cell')
    // AA = row 0, col 0 => cell index = 0
    const aaCell = cells[0]
    expect(aaCell.className).toContain('ring-2')
  })

  it('shows legend in non-compact mode', () => {
    render(<StartingHandsChart />)
    expect(screen.getByText('Premium')).toBeDefined()
    expect(screen.getByText('Strong')).toBeDefined()
    expect(screen.getByText('Playable')).toBeDefined()
    expect(screen.getByText('Fold')).toBeDefined()
  })

  it('hides hand notations in compact mode', () => {
    render(<StartingHandsChart compact />)
    const cells = screen.getAllByTestId('hand-cell')
    // In compact mode, cells should not contain text notation
    const cellsWithText = cells.filter((cell) => cell.textContent !== '')
    expect(cellsWithText).toHaveLength(0)
  })

  it('hides legend in compact mode', () => {
    render(<StartingHandsChart compact />)
    expect(screen.queryByText('Premium')).toBeNull()
    expect(screen.queryByText('Strong')).toBeNull()
  })

  it('shows close button when onClose provided', () => {
    const onClose = vi.fn()
    render(<StartingHandsChart onClose={onClose} />)
    const closeButton = screen.getByText('Close')
    expect(closeButton).toBeDefined()
    closeButton.click()
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('does not show close button when onClose not provided', () => {
    render(<StartingHandsChart />)
    expect(screen.queryByText('Close')).toBeNull()
  })

  it('does not show close button in compact mode even with onClose', () => {
    render(<StartingHandsChart compact onClose={() => {}} />)
    expect(screen.queryByText('Close')).toBeNull()
  })

  it('shows hand notations in non-compact mode', () => {
    render(<StartingHandsChart />)
    // Check a few representative notations
    expect(screen.getByText('AA')).toBeDefined()
    expect(screen.getByText('AKs')).toBeDefined()
    expect(screen.getByText('AKo')).toBeDefined()
    expect(screen.getByText('72o')).toBeDefined()
  })
})
