/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { TableData } from '@/lib/game/types'
import { CommunityCards } from './CommunityCards'
import { PotDisplay } from './PotDisplay'

interface TableFeltProps {
  table: TableData
}

/**
 * Oval green felt surface at the center of the poker table.
 * Contains community cards and pot display.
 *
 * Uses a radial gradient and inset shadow to simulate the felt texture.
 */
export function TableFelt({ table }: TableFeltProps) {
  return (
    <div
      className="absolute inset-x-[8%] inset-y-[15%] rounded-[50%] flex flex-col items-center justify-center gap-4"
      style={{
        background:
          'radial-gradient(ellipse at center, #2d5a1b 0%, #1e3d12 60%, #152d0d 100%)',
        boxShadow:
          'inset 0 0 80px rgba(0,0,0,0.5), 0 0 40px rgba(0,0,0,0.4)',
        border: '4px solid #1a2e0f',
      }}
      role="region"
      aria-label="Poker table felt"
    >
      <CommunityCards cards={table.communityCards} />
      <PotDisplay pots={table.pots} />
    </div>
  )
}
