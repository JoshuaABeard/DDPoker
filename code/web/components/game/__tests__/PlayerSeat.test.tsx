/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { PlayerSeat } from '../PlayerSeat'
import type { SeatData } from '@/lib/game/types'

// next/image doesn't work in jsdom — replace with a plain <img>
vi.mock('next/image', () => ({
  default: ({
    src,
    alt,
    width,
  }: {
    src: string
    alt: string
    width: number
  }) => (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={alt} width={width} />
  ),
}))

function makeSeat(overrides: Partial<SeatData> = {}): SeatData {
  return {
    seatIndex: 0,
    playerId: 1,
    playerName: 'TestPlayer',
    chipCount: 1000,
    status: 'ACTIVE',
    isDealer: false,
    isSmallBlind: false,
    isBigBlind: false,
    currentBet: 0,
    holeCards: [],
    isCurrentActor: false,
    ...overrides,
  }
}

const positionStyle = { top: '50%', left: '50%' }

describe('PlayerSeat', () => {
  it('renders nothing for an empty seat (no playerName)', () => {
    const { container } = render(
      <PlayerSeat seat={makeSeat({ playerName: '' })} isMe={false} positionStyle={positionStyle} />,
    )
    expect(container.firstChild).toBeNull()
  })

  it('renders player name', () => {
    render(<PlayerSeat seat={makeSeat()} isMe={false} positionStyle={positionStyle} />)
    expect(screen.getByText('TestPlayer')).toBeTruthy()
  })

  it('shows "Sitting Out" indicator for SAT_OUT status', () => {
    render(
      <PlayerSeat
        seat={makeSeat({ status: 'SAT_OUT' })}
        isMe={false}
        positionStyle={positionStyle}
      />,
    )
    expect(screen.getByText('Sitting Out')).toBeTruthy()
  })

  it('shows "Disconnected" indicator for DISCONNECTED status', () => {
    render(
      <PlayerSeat
        seat={makeSeat({ status: 'DISCONNECTED' })}
        isMe={false}
        positionStyle={positionStyle}
      />,
    )
    expect(screen.getByText('Disconnected')).toBeTruthy()
  })

  it('does not show "Sitting Out" for ACTIVE status', () => {
    render(
      <PlayerSeat
        seat={makeSeat({ status: 'ACTIVE' })}
        isMe={false}
        positionStyle={positionStyle}
      />,
    )
    expect(screen.queryByText('Sitting Out')).toBeNull()
  })

  it('does not show "Disconnected" for ACTIVE status', () => {
    render(
      <PlayerSeat
        seat={makeSeat({ status: 'ACTIVE' })}
        isMe={false}
        positionStyle={positionStyle}
      />,
    )
    expect(screen.queryByText('Disconnected')).toBeNull()
  })

  it('shows face-down cards for active non-me player', () => {
    render(
      <PlayerSeat
        seat={makeSeat({ status: 'ACTIVE' })}
        isMe={false}
        positionStyle={positionStyle}
      />,
    )
    // Two face-down cards should render (blank card images)
    const imgs = screen.getAllByRole('img')
    expect(imgs.length).toBe(2)
    imgs.forEach((img) => {
      expect(img.getAttribute('src')).toContain('card_blank')
    })
  })

  it('does not show face-down cards for SAT_OUT player', () => {
    render(
      <PlayerSeat
        seat={makeSeat({ status: 'SAT_OUT' })}
        isMe={false}
        positionStyle={positionStyle}
      />,
    )
    expect(screen.queryByRole('img')).toBeNull()
  })

  it('does not show face-down cards for FOLDED player', () => {
    render(
      <PlayerSeat
        seat={makeSeat({ status: 'FOLDED' })}
        isMe={false}
        positionStyle={positionStyle}
      />,
    )
    expect(screen.queryByRole('img')).toBeNull()
  })

  it('shows hole cards face-up for isMe player', () => {
    render(
      <PlayerSeat
        seat={makeSeat({ status: 'ACTIVE', holeCards: ['Ah', 'Kd'] })}
        isMe={true}
        positionStyle={positionStyle}
      />,
    )
    const imgs = screen.getAllByRole('img')
    expect(imgs.length).toBe(2)
    // Should show actual card images, not blanks
    expect(imgs[0].getAttribute('src')).toContain('card_Ah')
    expect(imgs[1].getAttribute('src')).toContain('card_Kd')
  })
})
