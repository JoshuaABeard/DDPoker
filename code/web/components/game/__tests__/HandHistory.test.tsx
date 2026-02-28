/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { HandHistory } from '../HandHistory'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

describe('HandHistory', () => {
  it('shows Export button when entries exist', () => {
    const entries: HandHistoryEntry[] = [
      { id: '1', handNumber: 1, type: 'hand_start', timestamp: 1000 },
    ]
    render(<HandHistory entries={entries} />)
    expect(screen.getByLabelText('Export hand history')).toBeTruthy()
  })

  it('does not show Export button when no entries', () => {
    render(<HandHistory entries={[]} />)
    expect(screen.queryByLabelText('Export hand history')).toBeNull()
  })
})
