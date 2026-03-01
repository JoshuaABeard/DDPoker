/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { DealerButton } from '../DealerButton'

describe('DealerButton', () => {
  it('renders D for dealer type', () => {
    render(<DealerButton type="dealer" />)
    expect(screen.getByText('D')).toBeTruthy()
  })

  it('renders SB for small-blind type', () => {
    render(<DealerButton type="small-blind" />)
    expect(screen.getByText('SB')).toBeTruthy()
  })

  it('renders BB for big-blind type', () => {
    render(<DealerButton type="big-blind" />)
    expect(screen.getByText('BB')).toBeTruthy()
  })

  it('has correct aria-label for dealer', () => {
    render(<DealerButton type="dealer" />)
    expect(screen.getByLabelText('dealer marker')).toBeTruthy()
  })

  it('has correct aria-label for small-blind', () => {
    render(<DealerButton type="small-blind" />)
    expect(screen.getByLabelText('small-blind marker')).toBeTruthy()
  })

  it('has correct aria-label for big-blind', () => {
    render(<DealerButton type="big-blind" />)
    expect(screen.getByLabelText('big-blind marker')).toBeTruthy()
  })
})
