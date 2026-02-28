/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { AvatarId } from '@/lib/theme/useAvatar'

interface AvatarSvgProps {
  size?: number
}

function Bear({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      {/* Ears */}
      <circle cx="7" cy="7" r="4" fill="#a0522d" />
      <circle cx="21" cy="7" r="4" fill="#a0522d" />
      <circle cx="7" cy="7" r="2" fill="#d2a679" />
      <circle cx="21" cy="7" r="2" fill="#d2a679" />
      {/* Head */}
      <circle cx="14" cy="15" r="9" fill="#a0522d" />
      {/* Snout */}
      <ellipse cx="14" cy="18" rx="4" ry="3" fill="#d2a679" />
      {/* Eyes */}
      <circle cx="10" cy="13" r="1.5" fill="#222" />
      <circle cx="18" cy="13" r="1.5" fill="#222" />
      {/* Nose */}
      <circle cx="14" cy="17" r="1.2" fill="#222" />
    </svg>
  )
}

function Eagle({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      {/* Wings spread */}
      <path d="M14,10 L2,6 L6,14 L14,12 Z" fill="#8B6914" />
      <path d="M14,10 L26,6 L22,14 L14,12 Z" fill="#8B6914" />
      {/* Body */}
      <ellipse cx="14" cy="16" rx="4" ry="6" fill="#6B4F12" />
      {/* Head */}
      <circle cx="14" cy="10" r="3.5" fill="#fff" />
      {/* Beak */}
      <polygon points="14,12 12.5,13.5 14,16 15.5,13.5" fill="#e8a317" />
      {/* Eyes */}
      <circle cx="12.5" cy="9.5" r="0.8" fill="#222" />
      <circle cx="15.5" cy="9.5" r="0.8" fill="#222" />
    </svg>
  )
}

function Fox({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      {/* Ears */}
      <polygon points="5,3 9,13 3,11" fill="#d4602a" />
      <polygon points="23,3 19,13 25,11" fill="#d4602a" />
      <polygon points="6,5 9,12 4,10" fill="#f5c9a0" />
      <polygon points="22,5 19,12 24,10" fill="#f5c9a0" />
      {/* Head */}
      <ellipse cx="14" cy="16" rx="8" ry="7" fill="#d4602a" />
      {/* White face markings */}
      <ellipse cx="14" cy="19" rx="5" ry="5" fill="#fff" />
      {/* Eyes */}
      <circle cx="10" cy="14" r="1.5" fill="#222" />
      <circle cx="18" cy="14" r="1.5" fill="#222" />
      {/* Nose */}
      <circle cx="14" cy="17" r="1" fill="#222" />
    </svg>
  )
}

function Wolf({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      {/* Ears */}
      <polygon points="6,2 10,12 3,10" fill="#666" />
      <polygon points="22,2 18,12 25,10" fill="#666" />
      {/* Head */}
      <ellipse cx="14" cy="14" rx="8" ry="7" fill="#777" />
      {/* Snout raised — howling */}
      <ellipse cx="14" cy="18" rx="3.5" ry="4" fill="#999" />
      {/* Open mouth howling */}
      <ellipse cx="14" cy="21" rx="2" ry="1.5" fill="#333" />
      {/* Eyes — closed/squinting */}
      <line x1="9" y1="12" x2="12" y2="12" stroke="#222" strokeWidth="1.5" strokeLinecap="round" />
      <line x1="16" y1="12" x2="19" y2="12" stroke="#222" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  )
}

function Shark({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      {/* Water line */}
      <path d="M0,20 Q7,18 14,20 Q21,22 28,20 L28,28 L0,28 Z" fill="#1a5276" opacity="0.4" />
      {/* Fin */}
      <polygon points="14,4 10,20 18,20" fill="#5d6d7e" />
      <polygon points="14,4 12,20 17,20" fill="#7b8d9e" />
    </svg>
  )
}

function Owl({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      {/* Ear tufts */}
      <polygon points="6,4 9,10 4,9" fill="#8B6914" />
      <polygon points="22,4 19,10 24,9" fill="#8B6914" />
      {/* Body */}
      <ellipse cx="14" cy="16" rx="9" ry="9" fill="#8B6914" />
      {/* Face disc */}
      <ellipse cx="14" cy="14" rx="7" ry="6" fill="#d2a679" />
      {/* Eye rings */}
      <circle cx="10" cy="13" r="3.5" fill="#fff" />
      <circle cx="18" cy="13" r="3.5" fill="#fff" />
      {/* Pupils */}
      <circle cx="10" cy="13" r="1.8" fill="#222" />
      <circle cx="18" cy="13" r="1.8" fill="#222" />
      {/* Beak */}
      <polygon points="14,15 12.5,17 15.5,17" fill="#e8a317" />
    </svg>
  )
}

function Crown({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      <polygon
        points="3,20 5,8 10,14 14,4 18,14 23,8 25,20"
        fill="#ffd700"
        stroke="#b8860b"
        strokeWidth="1"
      />
      <rect x="3" y="20" width="22" height="4" rx="1" fill="#ffd700" stroke="#b8860b" strokeWidth="1" />
      {/* Jewels */}
      <circle cx="14" cy="15" r="1.5" fill="#c41e3a" />
      <circle cx="8" cy="17" r="1" fill="#1a5276" />
      <circle cx="20" cy="17" r="1" fill="#1a5276" />
    </svg>
  )
}

function Diamond({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      <polygon
        points="14,2 26,14 14,26 2,14"
        fill="#c41e3a"
        stroke="#8b0000"
        strokeWidth="1"
      />
      <polygon
        points="14,5 23,14 14,23 5,14"
        fill="none"
        stroke="rgba(255,255,255,0.3)"
        strokeWidth="0.8"
      />
    </svg>
  )
}

function Spade({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M14,3 C14,3 4,12 4,17 C4,20 6.5,22 9,22 C11,22 12.5,21 14,19 C15.5,21 17,22 19,22 C21.5,22 24,20 24,17 C24,12 14,3 14,3 Z"
        fill="#1a1a2e"
        stroke="#444"
        strokeWidth="0.8"
      />
      <polygon points="12,21 14,26 16,21" fill="#1a1a2e" />
    </svg>
  )
}

function Star({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      <polygon
        points="14,2 17,10 26,10 19,16 21.5,25 14,20 6.5,25 9,16 2,10 11,10"
        fill="#ffd700"
        stroke="#b8860b"
        strokeWidth="0.8"
      />
    </svg>
  )
}

function Flame({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      {/* Outer flame */}
      <path
        d="M14,2 C14,2 22,10 22,18 C22,23 18,26 14,26 C10,26 6,23 6,18 C6,10 14,2 14,2 Z"
        fill="#e25822"
      />
      {/* Inner flame */}
      <path
        d="M14,8 C14,8 19,14 19,19 C19,22 17,24 14,24 C11,24 9,22 9,19 C9,14 14,8 14,8 Z"
        fill="#ffa500"
      />
      {/* Core */}
      <path
        d="M14,14 C14,14 17,17 17,20 C17,22 16,23 14,23 C12,23 11,22 11,20 C11,17 14,14 14,14 Z"
        fill="#ffd700"
      />
    </svg>
  )
}

function Lightning({ size = 28 }: AvatarSvgProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 28 28" xmlns="http://www.w3.org/2000/svg">
      <polygon
        points="16,1 8,15 13,15 11,27 21,12 16,12 19,1"
        fill="#ffd700"
        stroke="#b8860b"
        strokeWidth="0.8"
      />
    </svg>
  )
}

export const AVATARS: Record<AvatarId, React.FC<AvatarSvgProps>> = {
  bear: Bear,
  eagle: Eagle,
  fox: Fox,
  wolf: Wolf,
  shark: Shark,
  owl: Owl,
  crown: Crown,
  diamond: Diamond,
  spade: Spade,
  star: Star,
  flame: Flame,
  lightning: Lightning,
}

export function AvatarIcon({ id, size = 28 }: { id: string; size?: number }) {
  const Component = AVATARS[id as AvatarId] ?? Spade
  return <Component size={size} />
}
