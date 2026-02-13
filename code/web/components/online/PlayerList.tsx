/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Player List Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { PlayerLink } from './PlayerLink'

interface PlayerListProps {
  players: string[]
  separator?: string
}

export function PlayerList({ players, separator = ', ' }: PlayerListProps) {
  if (players.length === 0) {
    return <span>-</span>
  }

  return (
    <>
      {players.map((player, index) => (
        <span key={`${player}-${index}`}>
          <PlayerLink playerName={player} />
          {index < players.length - 1 && separator}
        </span>
      ))}
    </>
  )
}
