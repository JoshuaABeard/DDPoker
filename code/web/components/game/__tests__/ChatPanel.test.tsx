/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ChatPanel } from '../ChatPanel'
import type { ChatEntry } from '@/lib/game/gameReducer'

function makeEntry(id: string, playerId: number, playerName: string, message: string): ChatEntry {
  return { id, playerId, playerName, message, tableChat: true }
}

describe('ChatPanel', () => {
  it('renders messages', () => {
    const messages = [makeEntry('1', 1, 'Alice', 'Hello')]
    render(<ChatPanel messages={messages} onSend={vi.fn()} open={true} onToggle={vi.fn()} />)
    expect(screen.getByText('Hello')).toBeTruthy()
  })

  it('filters muted player messages', () => {
    const messages = [
      makeEntry('1', 1, 'Alice', 'Hello'),
      makeEntry('2', 2, 'Bob', 'Hi there'),
    ]
    const mutedIds = new Set([2])
    render(
      <ChatPanel messages={messages} onSend={vi.fn()} open={true} onToggle={vi.fn()} mutedIds={mutedIds} />
    )
    expect(screen.getByText('Hello')).toBeTruthy()
    expect(screen.queryByText('Hi there')).toBeNull()
  })

  it('shows hidden message count for muted players', () => {
    const messages = [
      makeEntry('1', 1, 'Alice', 'Hello'),
      makeEntry('2', 2, 'Bob', 'Hi'),
    ]
    const mutedIds = new Set([2])
    render(
      <ChatPanel messages={messages} onSend={vi.fn()} open={true} onToggle={vi.fn()} mutedIds={mutedIds} onUnmute={vi.fn()} />
    )
    expect(screen.getByText(/1 muted player/)).toBeTruthy()
  })

  it('calls onMute with correct playerId', () => {
    const onMute = vi.fn()
    const messages = [makeEntry('1', 42, 'Alice', 'Hello')]
    render(
      <ChatPanel messages={messages} onSend={vi.fn()} open={true} onToggle={vi.fn()} onMute={onMute} />
    )
    fireEvent.click(screen.getByLabelText('Mute Alice'))
    expect(onMute).toHaveBeenCalledWith(42)
  })
})
