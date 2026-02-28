/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { VolumeControl } from '../VolumeControl'

const mockToggleMute = vi.fn()
const mockSetVolume = vi.fn()
let mockIsMuted = false
let mockVolume = 0.7

vi.mock('@/lib/audio/useAudio', () => ({
  useAudio: () => ({
    playSound: vi.fn(),
    volume: mockVolume,
    setVolume: mockSetVolume,
    isMuted: mockIsMuted,
    toggleMute: mockToggleMute,
  }),
}))

describe('VolumeControl', () => {
  beforeEach(() => {
    mockToggleMute.mockClear()
    mockSetVolume.mockClear()
    mockIsMuted = false
    mockVolume = 0.7
  })

  it('renders a mute toggle button', () => {
    render(<VolumeControl />)
    expect(screen.getByRole('button', { name: /mute/i })).toBeDefined()
  })

  it('calls toggleMute on click', () => {
    render(<VolumeControl />)
    fireEvent.click(screen.getByRole('button', { name: /mute/i }))
    expect(mockToggleMute).toHaveBeenCalledOnce()
  })

  it('shows muted label when muted', () => {
    mockIsMuted = true
    render(<VolumeControl />)
    expect(screen.getByRole('button', { name: /unmute/i })).toBeDefined()
  })

  it('shows volume slider on hover', () => {
    render(<VolumeControl />)
    const container = screen.getByTestId('volume-control')
    fireEvent.mouseEnter(container)
    expect(screen.getByRole('slider')).toBeDefined()
  })
})
