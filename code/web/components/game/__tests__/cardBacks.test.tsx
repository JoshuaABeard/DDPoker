/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { CardBack, CARD_BACKS } from '../cardBacks'
import type { CardBackId } from '@/lib/theme/useCardBack'

const CARD_BACK_IDS = Object.keys(CARD_BACKS) as CardBackId[]

describe('CardBack', () => {
  it.each(CARD_BACK_IDS)('renders card back "%s" without crashing', (id) => {
    const { container } = render(<CardBack id={id} />)
    expect(container.querySelector('svg')).toBeTruthy()
  })

  it('renders with custom dimensions', () => {
    const { container } = render(<CardBack id="classic-red" width={32} height={45} />)
    const svg = container.querySelector('svg')
    expect(svg?.getAttribute('width')).toBe('32')
    expect(svg?.getAttribute('height')).toBe('45')
  })
})
