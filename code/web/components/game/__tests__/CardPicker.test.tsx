/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { CardPicker } from '../CardPicker'

describe('CardPicker', () => {
  it('renders 52 card buttons', () => {
    render(<CardPicker usedCards={[]} onSelect={vi.fn()} onClose={vi.fn()} />)
    const buttons = screen.getAllByRole('button').filter((b) => b.textContent !== 'Close')
    expect(buttons).toHaveLength(52)
  })

  it('disables used cards', () => {
    render(<CardPicker usedCards={['Ah', 'Kd']} onSelect={vi.fn()} onClose={vi.fn()} />)
    const ah = screen.getByTestId('card-Ah') as HTMLButtonElement
    const kd = screen.getByTestId('card-Kd') as HTMLButtonElement
    expect(ah.disabled).toBe(true)
    expect(kd.disabled).toBe(true)
  })

  it('calls onSelect when clicking an available card', () => {
    const onSelect = vi.fn()
    render(<CardPicker usedCards={[]} onSelect={onSelect} onClose={vi.fn()} />)
    fireEvent.click(screen.getByTestId('card-Ah'))
    expect(onSelect).toHaveBeenCalledWith('Ah')
  })

  it('does not call onSelect when clicking a used card', () => {
    const onSelect = vi.fn()
    render(<CardPicker usedCards={['Ah']} onSelect={onSelect} onClose={vi.fn()} />)
    fireEvent.click(screen.getByTestId('card-Ah'))
    expect(onSelect).not.toHaveBeenCalled()
  })

  it('calls onClose when clicking the close button', () => {
    const onClose = vi.fn()
    render(<CardPicker usedCards={[]} onSelect={vi.fn()} onClose={onClose} />)
    fireEvent.click(screen.getByText('Close'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('applies opacity styling to used cards', () => {
    render(<CardPicker usedCards={['Ts']} onSelect={vi.fn()} onClose={vi.fn()} />)
    const ts = screen.getByTestId('card-Ts')
    expect(ts.className).toContain('opacity-30')
    expect(ts.className).toContain('cursor-not-allowed')
  })

  it('does not apply opacity styling to available cards', () => {
    render(<CardPicker usedCards={[]} onSelect={vi.fn()} onClose={vi.fn()} />)
    const ah = screen.getByTestId('card-Ah')
    expect(ah.className).not.toContain('opacity-30')
    expect(ah.className).toContain('cursor-pointer')
  })
})
