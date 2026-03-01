/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { BetSlider } from '../BetSlider'

describe('BetSlider', () => {
  it('always shows Min and All-In buttons', () => {
    render(<BetSlider min={100} max={1000} value={500} potSize={400} onChange={vi.fn()} />)
    expect(screen.getByRole('button', { name: /^min$/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /^all-in$/i })).toBeTruthy()
  })

  it('shows ½ Pot when strictly between min and max', () => {
    // potSize=400, halfPot = floor(400/2) = 200; min=100, max=1000 => 100 < 200 < 1000
    render(<BetSlider min={100} max={1000} value={500} potSize={400} onChange={vi.fn()} />)
    expect(screen.getByRole('button', { name: /½ pot/i })).toBeTruthy()
  })

  it('hides ½ Pot when equals min', () => {
    // potSize=200, halfPot = floor(200/2) = 100; min=100 => halfPot === min, not strictly >
    render(<BetSlider min={100} max={1000} value={100} potSize={200} onChange={vi.fn()} />)
    expect(screen.queryByRole('button', { name: /½ pot/i })).toBeNull()
  })

  it('hides ½ Pot when equals max (clamp brings it to max)', () => {
    // potSize=2500, halfPot = clamp(floor(2500/2)=1250) = clamp(1250), min=100, max=1250 => halfPot===max
    render(<BetSlider min={100} max={1250} value={100} potSize={2500} onChange={vi.fn()} />)
    expect(screen.queryByRole('button', { name: /½ pot/i })).toBeNull()
  })

  it('shows Pot when strictly between min and max', () => {
    // potSize=500, pot = clamp(500), min=100, max=1000 => 100 < 500 < 1000
    render(<BetSlider min={100} max={1000} value={500} potSize={500} onChange={vi.fn()} />)
    expect(screen.getByRole('button', { name: /^pot$/i })).toBeTruthy()
  })

  it('hides Pot when equals max (clamp brings it to max)', () => {
    // potSize=1000, pot = clamp(1000), min=100, max=1000 => pot === max
    render(<BetSlider min={100} max={1000} value={100} potSize={1000} onChange={vi.fn()} />)
    expect(screen.queryByRole('button', { name: /^pot$/i })).toBeNull()
  })

  it('hides Pot when equals min', () => {
    // potSize=100, pot = clamp(100), min=100 => pot === min
    render(<BetSlider min={100} max={1000} value={100} potSize={100} onChange={vi.fn()} />)
    expect(screen.queryByRole('button', { name: /^pot$/i })).toBeNull()
  })

  it('clicking Min fires onChange(min)', () => {
    const onChange = vi.fn()
    render(<BetSlider min={100} max={1000} value={500} potSize={400} onChange={onChange} />)
    fireEvent.click(screen.getByRole('button', { name: /^min$/i }))
    expect(onChange).toHaveBeenCalledWith(100)
  })

  it('clicking All-In fires onChange(max)', () => {
    const onChange = vi.fn()
    render(<BetSlider min={100} max={1000} value={500} potSize={400} onChange={onChange} />)
    fireEvent.click(screen.getByRole('button', { name: /^all-in$/i }))
    expect(onChange).toHaveBeenCalledWith(1000)
  })

  it('range input has correct min/max/value attributes', () => {
    render(<BetSlider min={100} max={1000} value={500} potSize={400} onChange={vi.fn()} />)
    const rangeInput = screen.getByRole('slider')
    expect(rangeInput.getAttribute('min')).toBe('100')
    expect(rangeInput.getAttribute('max')).toBe('1000')
    expect((rangeInput as HTMLInputElement).value).toBe('500')
  })
})
