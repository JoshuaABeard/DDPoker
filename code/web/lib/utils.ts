/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { HandHistoryEntry } from '@/lib/game/gameReducer'

export function formatChips(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
}

export function formatHandHistoryForExport(entries: HandHistoryEntry[]): string {
  if (entries.length === 0) return ''

  const lines: string[] = []
  for (const entry of entries) {
    switch (entry.type) {
      case 'hand_start':
        if (lines.length > 0) lines.push('') // blank line between hands
        lines.push(`--- Hand #${entry.handNumber} ---`)
        break
      case 'action': {
        const amt = entry.amount && entry.amount > 0 ? ` ${formatChips(entry.amount)}` : ''
        lines.push(`${entry.playerName ?? 'Player'}: ${entry.action ?? ''}${amt}`)
        break
      }
      case 'community':
        lines.push(`${entry.round ?? 'Dealt'}: ${(entry.cards ?? []).join(' ')}`)
        break
      case 'result': {
        if (entry.winners && entry.winners.length > 0) {
          for (const winner of entry.winners) {
            lines.push(`${winner.playerName} wins ${formatChips(winner.amount)} with ${winner.hand}`)
          }
        } else {
          lines.push('Hand complete')
        }
        break
      }
    }
  }
  return lines.join('\n')
}
