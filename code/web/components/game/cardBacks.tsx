/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { CardBackId } from '@/lib/theme/useCardBack'

interface CardBackProps {
  width?: number
  height?: number
}

function ClassicRed({ width = 65, height = 91 }: CardBackProps) {
  return (
    <svg width={width} height={height} viewBox="0 0 65 91" xmlns="http://www.w3.org/2000/svg">
      <rect width="65" height="91" rx="4" fill="#c41e3a" />
      <rect x="4" y="4" width="57" height="83" rx="2" fill="none" stroke="#fff" strokeWidth="1.5" />
      {/* Diamond grid pattern */}
      {Array.from({ length: 7 }, (_, row) =>
        Array.from({ length: 5 }, (_, col) => {
          const cx = 10 + col * 11
          const cy = 10 + row * 11
          return (
            <polygon
              key={`${row}-${col}`}
              points={`${cx},${cy - 4} ${cx + 4},${cy} ${cx},${cy + 4} ${cx - 4},${cy}`}
              fill="none"
              stroke="rgba(255,255,255,0.4)"
              strokeWidth="0.8"
            />
          )
        })
      )}
      {/* Center diamond accent */}
      <polygon points="32.5,35 42,45.5 32.5,56 23,45.5" fill="none" stroke="#fff" strokeWidth="1.5" />
      <polygon points="32.5,38 39,45.5 32.5,53 26,45.5" fill="rgba(255,255,255,0.15)" stroke="none" />
    </svg>
  )
}

function BlueDiamond({ width = 65, height = 91 }: CardBackProps) {
  return (
    <svg width={width} height={height} viewBox="0 0 65 91" xmlns="http://www.w3.org/2000/svg">
      <rect width="65" height="91" rx="4" fill="#1b4b8a" />
      <rect x="3" y="3" width="59" height="85" rx="2" fill="none" stroke="rgba(255,255,255,0.3)" strokeWidth="1" />
      {/* Interlocking diamond shapes */}
      {Array.from({ length: 5 }, (_, row) =>
        Array.from({ length: 3 }, (_, col) => {
          const cx = 12 + col * 20
          const cy = 11 + row * 17
          return (
            <polygon
              key={`${row}-${col}`}
              points={`${cx},${cy - 7} ${cx + 9},${cy} ${cx},${cy + 7} ${cx - 9},${cy}`}
              fill="none"
              stroke="#4a8fd4"
              strokeWidth="1.2"
            />
          )
        })
      )}
      {/* Offset row of diamonds */}
      {Array.from({ length: 4 }, (_, row) =>
        Array.from({ length: 2 }, (_, col) => {
          const cx = 22 + col * 20
          const cy = 19.5 + row * 17
          return (
            <polygon
              key={`off-${row}-${col}`}
              points={`${cx},${cy - 7} ${cx + 9},${cy} ${cx},${cy + 7} ${cx - 9},${cy}`}
              fill="none"
              stroke="#6aace6"
              strokeWidth="0.8"
            />
          )
        })
      )}
    </svg>
  )
}

function GreenCeltic({ width = 65, height = 91 }: CardBackProps) {
  return (
    <svg width={width} height={height} viewBox="0 0 65 91" xmlns="http://www.w3.org/2000/svg">
      <rect width="65" height="91" rx="4" fill="#1a5c2a" />
      <rect x="4" y="4" width="57" height="83" rx="2" fill="none" stroke="rgba(255,255,255,0.3)" strokeWidth="1" />
      {/* Celtic-inspired interwoven lines — horizontal waves */}
      {Array.from({ length: 6 }, (_, i) => {
        const y = 14 + i * 12
        return (
          <line
            key={`h-${i}`}
            x1="8" y1={y} x2="57" y2={y}
            stroke="#3da85c"
            strokeWidth="1"
            strokeDasharray="6 4"
          />
        )
      })}
      {/* Vertical crossing lines */}
      {Array.from({ length: 4 }, (_, i) => {
        const x = 15 + i * 10
        return (
          <line
            key={`v-${i}`}
            x1={x} y1="8" x2={x} y2="83"
            stroke="#3da85c"
            strokeWidth="1"
            strokeDasharray="4 6"
          />
        )
      })}
      {/* Center knot circles */}
      <circle cx="32.5" cy="45.5" r="12" fill="none" stroke="#5ec47a" strokeWidth="1.5" />
      <circle cx="32.5" cy="45.5" r="7" fill="none" stroke="#5ec47a" strokeWidth="1" />
      <circle cx="32.5" cy="45.5" r="2.5" fill="#5ec47a" />
    </svg>
  )
}

function GoldRoyal({ width = 65, height = 91 }: CardBackProps) {
  return (
    <svg width={width} height={height} viewBox="0 0 65 91" xmlns="http://www.w3.org/2000/svg">
      <rect width="65" height="91" rx="4" fill="#b8860b" />
      <rect x="4" y="4" width="57" height="83" rx="2" fill="none" stroke="rgba(255,255,255,0.4)" strokeWidth="1.5" />
      {/* Shield shape */}
      <polygon
        points="32.5,25 48,35 48,55 32.5,68 17,55 17,35"
        fill="none"
        stroke="#ffd700"
        strokeWidth="1.5"
      />
      <polygon
        points="32.5,29 44,37 44,53 32.5,63 21,53 21,37"
        fill="rgba(255,215,0,0.1)"
        stroke="none"
      />
      {/* Crown above shield */}
      <polygon
        points="24,27 26,20 29,24 32.5,18 36,24 39,20 41,27"
        fill="none"
        stroke="#ffd700"
        strokeWidth="1.2"
      />
      {/* Crown jewels */}
      <circle cx="26" cy="22" r="1" fill="#ffd700" />
      <circle cx="32.5" cy="19.5" r="1.2" fill="#ffd700" />
      <circle cx="39" cy="22" r="1" fill="#ffd700" />
    </svg>
  )
}

const CARD_BACKS: Record<CardBackId, React.FC<CardBackProps>> = {
  'classic-red': ClassicRed,
  'blue-diamond': BlueDiamond,
  'green-celtic': GreenCeltic,
  'gold-royal': GoldRoyal,
}

export function CardBack({
  id,
  width,
  height,
}: CardBackProps & { id: CardBackId }) {
  const Component = CARD_BACKS[id] ?? ClassicRed
  return <Component width={width} height={height} />
}

export { CARD_BACKS }
