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
import { AvatarIcon, AVATARS } from '../avatarIcons'

const AVATAR_IDS = Object.keys(AVATARS) as string[]

describe('AvatarIcon', () => {
  it.each(AVATAR_IDS)('renders avatar "%s" without crashing', (id) => {
    const { container } = render(<AvatarIcon id={id} />)
    expect(container.querySelector('svg')).toBeTruthy()
  })

  it('renders with custom size', () => {
    const { container } = render(<AvatarIcon id="bear" size={48} />)
    const svg = container.querySelector('svg')
    expect(svg?.getAttribute('width')).toBe('48')
    expect(svg?.getAttribute('height')).toBe('48')
  })

  it('falls back to Spade for unknown avatar id', () => {
    const { container } = render(<AvatarIcon id="unknown-id" />)
    expect(container.querySelector('svg')).toBeTruthy()
  })
})
