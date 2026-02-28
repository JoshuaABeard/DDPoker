/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { ObserverPanel } from '../ObserverPanel'

describe('ObserverPanel', () => {
  it('renders nothing when no observers', () => {
    const { container } = render(<ObserverPanel observers={[]} />)
    expect(container.textContent).toBe('')
  })

  it('shows observer count', () => {
    render(
      <ObserverPanel observers={[
        { observerId: 1, observerName: 'Alice' },
        { observerId: 2, observerName: 'Bob' },
      ]} />
    )
    expect(screen.getByText('2 watching')).toBeTruthy()
  })

  it('expands to show observer names on click', () => {
    render(
      <ObserverPanel observers={[
        { observerId: 1, observerName: 'Alice' },
        { observerId: 2, observerName: 'Bob' },
      ]} />
    )
    fireEvent.click(screen.getByText('2 watching'))
    expect(screen.getByText('Alice')).toBeTruthy()
    expect(screen.getByText('Bob')).toBeTruthy()
  })

  it('collapses on second click', () => {
    render(
      <ObserverPanel observers={[
        { observerId: 1, observerName: 'Alice' },
      ]} />
    )
    fireEvent.click(screen.getByText('1 watching'))
    expect(screen.getByText('Alice')).toBeTruthy()
    fireEvent.click(screen.getByText('1 watching'))
    expect(screen.queryByText('Alice')).toBeNull()
  })
})
