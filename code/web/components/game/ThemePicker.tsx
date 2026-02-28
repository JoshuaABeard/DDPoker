/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useRef, useState } from 'react'
import { useTheme } from '@/lib/theme/useTheme'
import { THEMES } from '@/lib/theme/themes'
import { useCardBack, CARD_BACK_IDS } from '@/lib/theme/useCardBack'
import { useAvatar, AVATAR_IDS } from '@/lib/theme/useAvatar'
import { CardBack } from './cardBacks'
import { AvatarIcon } from './avatarIcons'
import type { CardBackId } from '@/lib/theme/useCardBack'
import type { AvatarId } from '@/lib/theme/useAvatar'
import { useGamePrefs } from '@/lib/game/useGamePrefs'

type Tab = 'Table' | 'Cards' | 'Avatar' | 'Gameplay'
const TABS: Tab[] = ['Table', 'Cards', 'Avatar', 'Gameplay']

export function ThemePicker() {
  const [open, setOpen] = useState(false)
  const [tab, setTab] = useState<Tab>('Table')
  const panelRef = useRef<HTMLDivElement>(null)

  const { themeId, setTheme } = useTheme()
  const { cardBackId, setCardBack } = useCardBack()
  const { avatarId, setAvatar } = useAvatar()
  const { prefs, setPref } = useGamePrefs()

  // Close when clicking outside
  useEffect(() => {
    if (!open) return
    function handleClick(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  return (
    <div className="relative inline-flex" ref={panelRef}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="px-2 py-1.5 text-sm rounded-lg transition-colors bg-gray-700 hover:bg-gray-600 text-gray-200"
        aria-label="Settings"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
          <path d="M6.5.5A.5.5 0 0 1 7 0h2a.5.5 0 0 1 .5.5v1.05a5.5 5.5 0 0 1 1.36.56l.74-.74a.5.5 0 0 1 .7 0l1.42 1.42a.5.5 0 0 1 0 .7l-.74.74c.24.43.42.88.56 1.36H14.5a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5h-1.05a5.5 5.5 0 0 1-.56 1.36l.74.74a.5.5 0 0 1 0 .7l-1.42 1.42a.5.5 0 0 1-.7 0l-.74-.74a5.5 5.5 0 0 1-1.36.56V15.5a.5.5 0 0 1-.5.5H7a.5.5 0 0 1-.5-.5v-1.05a5.5 5.5 0 0 1-1.36-.56l-.74.74a.5.5 0 0 1-.7 0L2.28 13.2a.5.5 0 0 1 0-.7l.74-.74A5.5 5.5 0 0 1 2.46 10.4H1.5A.5.5 0 0 1 1 9.9v-2a.5.5 0 0 1 .5-.5h1.05c.14-.48.32-.93.56-1.36l-.74-.74a.5.5 0 0 1 0-.7L3.79 3.18a.5.5 0 0 1 .7 0l.74.74A5.5 5.5 0 0 1 6.6 3.36V.5ZM8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
        </svg>
      </button>

      {open && (
        <div className="absolute bottom-full left-0 mb-2 w-56 rounded-lg bg-gray-800/95 shadow-xl p-3 z-50">
          {/* Tabs */}
          <div className="flex gap-1 mb-3">
            {TABS.map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setTab(t)}
                className={[
                  'flex-1 text-xs font-semibold py-1 rounded transition-colors',
                  tab === t ? 'bg-gray-600 text-white' : 'text-gray-400 hover:text-gray-200',
                ].join(' ')}
              >
                {t}
              </button>
            ))}
          </div>

          {/* Table tab — theme color circles */}
          {tab === 'Table' && (
            <div className="flex flex-wrap gap-2 justify-center">
              {Object.values(THEMES).map((theme) => (
                <button
                  key={theme.id}
                  type="button"
                  onClick={() => setTheme(theme.id)}
                  aria-label={theme.name}
                  className="rounded-full transition-all"
                  style={{
                    width: 40,
                    height: 40,
                    backgroundColor: theme.colors.center,
                    border: themeId === theme.id ? '3px solid white' : '3px solid transparent',
                  }}
                />
              ))}
            </div>
          )}

          {/* Cards tab — card back previews */}
          {tab === 'Cards' && (
            <div className="flex flex-wrap gap-2 justify-center">
              {CARD_BACK_IDS.map((id) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setCardBack(id as CardBackId)}
                  aria-label={id}
                  className="rounded transition-all"
                  style={{
                    padding: 2,
                    border: cardBackId === id ? '2px solid white' : '2px solid transparent',
                  }}
                >
                  <CardBack id={id as CardBackId} width={40} height={56} />
                </button>
              ))}
            </div>
          )}

          {/* Avatar tab — 4x3 grid of avatar icons */}
          {tab === 'Avatar' && (
            <div className="grid grid-cols-4 gap-2 justify-items-center">
              {AVATAR_IDS.map((id) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setAvatar(id as AvatarId)}
                  aria-label={id}
                  className="rounded-full p-1 transition-all"
                  style={{
                    border: avatarId === id ? '2px solid white' : '2px solid transparent',
                  }}
                >
                  <AvatarIcon id={id} size={32} />
                </button>
              ))}
            </div>
          )}

          {/* Gameplay tab — game preferences */}
          {tab === 'Gameplay' && (
            <div className="space-y-3">
              <label className="flex items-center justify-between cursor-pointer">
                <span className="text-sm">Four-Color Deck</span>
                <input type="checkbox" checked={prefs.fourColorDeck} onChange={(e) => setPref('fourColorDeck', e.target.checked)} className="w-4 h-4 rounded" />
              </label>
              <label className="flex items-center justify-between cursor-pointer">
                <span className="text-sm">Check-Fold</span>
                <input type="checkbox" checked={prefs.checkFold} onChange={(e) => setPref('checkFold', e.target.checked)} className="w-4 h-4 rounded" />
              </label>
              <label className="flex items-center justify-between cursor-pointer">
                <span className="text-sm">Disable Shortcuts</span>
                <input type="checkbox" checked={prefs.disableShortcuts} onChange={(e) => setPref('disableShortcuts', e.target.checked)} className="w-4 h-4 rounded" />
              </label>
              <div className="flex items-center justify-between">
                <span className="text-sm">Dealer Chat</span>
                <select value={prefs.dealerChat} onChange={(e) => setPref('dealerChat', e.target.value as 'all' | 'actions' | 'none')} className="bg-gray-700 text-white text-xs rounded px-2 py-1">
                  <option value="all">All</option>
                  <option value="actions">Actions Only</option>
                  <option value="none">None</option>
                </select>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
