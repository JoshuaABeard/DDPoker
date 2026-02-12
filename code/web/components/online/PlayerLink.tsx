/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Player Link Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'

interface PlayerLinkProps {
  playerName: string
  className?: string
}

export function PlayerLink({ playerName, className = '' }: PlayerLinkProps) {
  return (
    <Link
      href={`/online/history?name=${encodeURIComponent(playerName)}`}
      className={`text-blue-600 hover:underline ${className}`}
    >
      {playerName}
    </Link>
  )
}
