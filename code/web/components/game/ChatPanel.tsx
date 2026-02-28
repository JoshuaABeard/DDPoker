/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useRef, useState } from 'react'
import type { ChatEntry } from '@/lib/game/gameReducer'

interface ChatPanelProps {
  messages: ChatEntry[]
  onSend: (message: string) => void
  /** If false, panel is collapsed. */
  open: boolean
  onToggle: () => void
  /** Set of muted player IDs — messages from these players are hidden */
  mutedIds?: Set<number>
  /** Called when user clicks mute on a message */
  onMute?: (playerId: number) => void
  /** Called when user clicks unmute */
  onUnmute?: (playerId: number) => void
}

/**
 * Chat panel with message history and input.
 *
 * XSS safety: message content and playerName are rendered as React text nodes.
 * NEVER use dangerouslySetInnerHTML for any user-supplied content here.
 */
export function ChatPanel({ messages, onSend, open, onToggle, mutedIds, onMute, onUnmute }: ChatPanelProps) {
  const [input, setInput] = useState('')
  const listRef = useRef<HTMLDivElement>(null)

  const visibleMessages = mutedIds && mutedIds.size > 0
    ? messages.filter((m) => !mutedIds.has(m.playerId))
    : messages
  const hiddenCount = messages.length - visibleMessages.length

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (open && listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight
    }
  }, [messages, open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = input.trim()
    if (!trimmed) return
    onSend(trimmed)
    setInput('')
  }

  return (
    <div className="flex flex-col bg-gray-900 bg-opacity-90 rounded-xl shadow-xl w-72">
      {/* Header toggle */}
      <button
        type="button"
        onClick={onToggle}
        className="flex items-center justify-between px-3 py-2 text-sm font-semibold text-gray-300 hover:text-white"
      >
        <span>Chat</span>
        <span>{open ? '▾' : '▸'}</span>
      </button>

      {open && (
        <>
          {/* Message list */}
          <div
            ref={listRef}
            className="flex-1 overflow-y-auto px-2 py-1 space-y-1 max-h-48 min-h-[80px]"
            role="log"
            aria-live="polite"
            aria-label="Chat messages"
          >
            {visibleMessages.length === 0 && (
              <p className="text-gray-500 text-xs text-center py-2">No messages yet</p>
            )}
            {visibleMessages.map((entry) => (
              <div key={entry.id} className="text-xs group flex items-start gap-1">
                <div className="flex-1">
                  <span className={`font-semibold ${entry.optimistic ? 'text-blue-300' : 'text-yellow-300'}`}>
                    {entry.playerName}:
                  </span>{' '}
                  <span className="text-gray-200">{entry.message}</span>
                </div>
                {onMute && !entry.optimistic && (
                  <button
                    type="button"
                    onClick={() => onMute(entry.playerId)}
                    aria-label={`Mute ${entry.playerName}`}
                    className="text-gray-600 hover:text-red-400 opacity-0 group-hover:opacity-100 text-[10px] flex-shrink-0"
                  >
                    Mute
                  </button>
                )}
              </div>
            ))}
          </div>

          {hiddenCount > 0 && (
            <div className="px-2 py-1 text-[10px] text-gray-500 border-t border-gray-700">
              {hiddenCount} message{hiddenCount > 1 ? 's' : ''} from muted players hidden
            </div>
          )}

          {/* Input */}
          <form onSubmit={handleSubmit} className="flex gap-1 px-2 pb-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Type a message..."
              maxLength={500}
              className="flex-1 bg-gray-700 text-white text-xs rounded px-2 py-1 outline-none focus:ring-1 focus:ring-blue-500"
              aria-label="Chat input"
            />
            <button
              type="submit"
              className="px-2 py-1 bg-blue-600 hover:bg-blue-500 text-white text-xs rounded transition-colors"
            >
              Send
            </button>
          </form>
        </>
      )}
    </div>
  )
}
