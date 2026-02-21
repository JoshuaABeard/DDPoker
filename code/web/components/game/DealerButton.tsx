/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

interface DealerButtonProps {
  type: 'dealer' | 'small-blind' | 'big-blind'
}

const LABELS = { dealer: 'D', 'small-blind': 'SB', 'big-blind': 'BB' }
const COLORS = {
  dealer: 'bg-white text-black',
  'small-blind': 'bg-blue-500 text-white',
  'big-blind': 'bg-red-600 text-white',
}

/**
 * Renders the dealer "D" button or small/big blind marker.
 */
export function DealerButton({ type }: DealerButtonProps) {
  return (
    <span
      className={`inline-flex items-center justify-center w-6 h-6 rounded-full text-[10px] font-bold shadow-md ${COLORS[type]}`}
      aria-label={`${type} marker`}
    >
      {LABELS[type]}
    </span>
  )
}
