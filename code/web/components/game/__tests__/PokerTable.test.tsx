/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { PokerTable } from '../PokerTable'

// --- Mock all child components ---

vi.mock('../TournamentInfoBar', () => ({
  TournamentInfoBar: () => <div data-testid="tournament-info-bar" />,
}))
vi.mock('../TableFelt', () => ({
  TableFelt: () => <div data-testid="table-felt" />,
}))
vi.mock('../PlayerSeat', () => ({
  PlayerSeat: ({ seat }: { seat: { playerName: string } }) => (
    <div data-testid="player-seat">{seat.playerName}</div>
  ),
}))
vi.mock('../ActionPanel', () => ({
  ActionPanel: () => <div data-testid="action-panel" />,
}))
vi.mock('../ActionTimer', () => ({
  ActionTimer: () => <div data-testid="action-timer" />,
}))
vi.mock('../HandHistory', () => ({
  HandHistory: () => <div data-testid="hand-history" />,
}))
vi.mock('../ChatPanel', () => ({
  ChatPanel: () => <div data-testid="chat-panel" />,
}))
vi.mock('../ObserverPanel', () => ({
  ObserverPanel: () => <div data-testid="observer-panel" />,
}))
vi.mock('../ChipLeaderMini', () => ({
  ChipLeaderMini: () => <div data-testid="chip-leader" />,
}))
vi.mock('../VolumeControl', () => ({
  VolumeControl: () => <div data-testid="volume-control" />,
}))
vi.mock('../ThemePicker', () => ({
  ThemePicker: () => <div data-testid="theme-picker" />,
}))
vi.mock('@/components/ui/Dialog', () => ({
  Dialog: () => <div data-testid="dialog" />,
}))
vi.mock('../HandRankings', () => ({
  HandRankings: () => <div data-testid="hand-rankings" />,
}))
vi.mock('../HandReplay', () => ({
  HandReplay: () => <div data-testid="hand-replay" />,
}))
vi.mock('../Simulator', () => ({
  Simulator: () => <div data-testid="simulator" />,
}))
vi.mock('../AdvisorPanel', () => ({
  AdvisorPanel: () => <div data-testid="advisor-panel" />,
}))
vi.mock('../Dashboard', () => ({
  Dashboard: () => <div data-testid="dashboard" />,
}))

// --- Mock all hooks ---

const mockSendAction = vi.fn()
const mockSendChat = vi.fn()
const mockSendContinueRunout = vi.fn()
const mockSendAdminResume = vi.fn()
const mockSendAdminPause = vi.fn()
const mockSendAdminKick = vi.fn()
const mockSendSitOut = vi.fn()
const mockSendComeBack = vi.fn()

const defaultActions = {
  sendAction: mockSendAction,
  sendChat: mockSendChat,
  sendContinueRunout: mockSendContinueRunout,
  sendAdminResume: mockSendAdminResume,
  sendAdminPause: mockSendAdminPause,
  sendAdminKick: mockSendAdminKick,
  sendSitOut: mockSendSitOut,
  sendComeBack: mockSendComeBack,
  sendRebuyDecision: vi.fn(),
  sendAddonDecision: vi.fn(),
  sendNeverBrokeDecision: vi.fn(),
  sendRequestState: vi.fn(),
}

function makeSeat(overrides = {}) {
  return {
    seatIndex: 0,
    playerId: 1,
    playerName: 'Alice',
    chipCount: 5000,
    status: 'ACTIVE',
    isDealer: true,
    isSmallBlind: false,
    isBigBlind: false,
    currentBet: 0,
    holeCards: [],
    isCurrentActor: false,
    ...overrides,
  }
}

function makeState(overrides = {}) {
  return {
    connectionState: 'CONNECTED',
    gamePhase: 'playing',
    gameId: 'game-1',
    myPlayerId: 1,
    reconnectToken: null,
    lobbyState: null,
    currentTable: {
      tableId: 1,
      seats: [makeSeat(), makeSeat({ seatIndex: 1, playerId: 2, playerName: 'Bob' })],
      communityCards: [],
      pots: [{ amount: 100, eligiblePlayers: [1, 2] }],
      currentRound: 'PREFLOP',
      handNumber: 1,
    },
    holeCards: ['Ah', 'Kd'],
    actionRequired: null,
    actionTimeoutSeconds: null,
    actionTimer: null,
    chatMessages: [],
    standings: [],
    rebuyOffer: null,
    addonOffer: null,
    eliminatedPosition: null,
    error: null,
    isOwner: false,
    gameState: {
      status: 'RUNNING',
      level: 1,
      blinds: { small: 25, big: 50, ante: 0 },
      nextLevelIn: 300,
      players: [],
      totalPlayers: 9,
      playersRemaining: 6,
      numTables: 1,
      playerRank: 3,
    },
    totalPlayers: 9,
    playersRemaining: 6,
    numTables: 1,
    playerRank: 3,
    handHistory: [],
    observers: [],
    colorUpData: null,
    advisorData: null,
    neverBrokeOffer: null,
    continueRunoutPending: false,
    lastSequenceNumber: null,
    sequenceGapDetected: false,
    ...overrides,
  }
}

let mockState = makeState()

vi.mock('@/lib/game/hooks', () => ({
  useGameState: () => mockState,
  useGameActions: () => defaultActions,
}))

vi.mock('@/lib/game/useMutedPlayers', () => ({
  useMutedPlayers: () => ({ mutedIds: new Set(), mute: vi.fn(), unmute: vi.fn() }),
}))

vi.mock('@/lib/theme/useTheme', () => ({
  useTheme: () => ({ colors: { center: '#000', mid: '#111', edge: '#222', border: '#333' } }),
}))

vi.mock('@/lib/theme/useCardBack', () => ({
  useCardBack: () => ({ cardBackId: 'classic' }),
}))

vi.mock('@/lib/theme/useAvatar', () => ({
  useAvatar: () => ({ avatarId: 'default' }),
}))

vi.mock('@/lib/game/useGamePrefs', () => ({
  useGamePrefs: () => ({
    prefs: { fourColorDeck: false, disableShortcuts: false, checkFold: false, dealerChat: true },
  }),
}))

vi.mock('@/lib/audio/useSoundEffects', () => ({
  useSoundEffects: vi.fn(),
}))

beforeEach(() => {
  vi.clearAllMocks()
  mockState = makeState()
})

describe('PokerTable', () => {
  // --- Loading state ---

  it('shows connecting message when currentTable is null', () => {
    mockState = makeState({ currentTable: null })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByText('Connecting to game...')).toBeTruthy()
  })

  it('shows connecting message when gameState is null', () => {
    mockState = makeState({ gameState: null })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByText('Connecting to game...')).toBeTruthy()
  })

  // --- Core rendering ---

  it('renders player seats for each player', () => {
    render(<PokerTable gameName="Test Game" />)
    const seats = screen.getAllByTestId('player-seat')
    expect(seats).toHaveLength(2)
    expect(screen.getByText('Alice')).toBeTruthy()
    expect(screen.getByText('Bob')).toBeTruthy()
  })

  it('renders table felt', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('table-felt')).toBeTruthy()
  })

  it('renders chat panel', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('chat-panel')).toBeTruthy()
  })

  it('renders tournament info bar', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('tournament-info-bar')).toBeTruthy()
  })

  it('renders hand history', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('hand-history')).toBeTruthy()
  })

  it('renders observer panel', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('observer-panel')).toBeTruthy()
  })

  // --- Action panel ---

  it('shows action panel when actionRequired is set', () => {
    mockState = makeState({
      actionRequired: {
        canFold: true,
        canCheck: false,
        canCall: true,
        callAmount: 50,
        canBet: false,
        minBet: 0,
        maxBet: 0,
        canRaise: false,
        minRaise: 0,
        maxRaise: 0,
        canAllIn: true,
        allInAmount: 5000,
      },
    })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('action-panel')).toBeTruthy()
  })

  it('hides action panel when actionRequired is null', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('action-panel')).toBeNull()
  })

  // --- Action timer ---

  it('shows action timer when actionTimeoutSeconds is set', () => {
    mockState = makeState({ actionTimeoutSeconds: 30 })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('action-timer')).toBeTruthy()
  })

  it('hides action timer when actionTimeoutSeconds is null', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('action-timer')).toBeNull()
  })

  // --- Chip leader mini ---

  it('renders chip leader mini when seats are present', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('chip-leader')).toBeTruthy()
  })

  // --- Paused banner ---

  it('shows "Game Paused" banner when game status is PAUSED', () => {
    mockState = makeState({
      gameState: {
        status: 'PAUSED',
        level: 1,
        blinds: { small: 25, big: 50, ante: 0 },
        nextLevelIn: 300,
        players: [],
        totalPlayers: 9,
        playersRemaining: 6,
        numTables: 1,
        playerRank: 3,
      },
    })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByText('Game Paused')).toBeTruthy()
  })

  it('does not show "Game Paused" banner when game is running', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByText('Game Paused')).toBeNull()
  })

  // --- Admin controls ---

  it('shows Pause Game button when owner and game is running', () => {
    mockState = makeState({ isOwner: true })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByRole('button', { name: /pause game/i })).toBeTruthy()
  })

  it('shows Resume Game button when owner and game is PAUSED', () => {
    mockState = makeState({
      isOwner: true,
      gameState: {
        status: 'PAUSED',
        level: 1,
        blinds: { small: 25, big: 50, ante: 0 },
        nextLevelIn: 300,
        players: [],
        totalPlayers: 9,
        playersRemaining: 6,
        numTables: 1,
        playerRank: 3,
      },
    })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByRole('button', { name: /resume game/i })).toBeTruthy()
  })

  it('does not show admin controls when not owner', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByRole('button', { name: /pause game/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /resume game/i })).toBeNull()
  })

  it('calls sendAdminPause when Pause Game is clicked', () => {
    mockState = makeState({ isOwner: true })
    render(<PokerTable gameName="Test Game" />)
    fireEvent.click(screen.getByRole('button', { name: /pause game/i }))
    expect(mockSendAdminPause).toHaveBeenCalledTimes(1)
  })

  it('calls sendAdminResume when Resume Game is clicked', () => {
    mockState = makeState({
      isOwner: true,
      gameState: {
        status: 'PAUSED',
        level: 1,
        blinds: { small: 25, big: 50, ante: 0 },
        nextLevelIn: 300,
        players: [],
        totalPlayers: 9,
        playersRemaining: 6,
        numTables: 1,
        playerRank: 3,
      },
    })
    render(<PokerTable gameName="Test Game" />)
    fireEvent.click(screen.getByRole('button', { name: /resume game/i }))
    expect(mockSendAdminResume).toHaveBeenCalledTimes(1)
  })

  // --- Sit out / come back ---

  it('shows Sit Out button when player has a seat', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByRole('button', { name: /sit out/i })).toBeTruthy()
  })

  it('shows "I\'m Back" button when player is sat out', () => {
    mockState = makeState({
      currentTable: {
        tableId: 1,
        seats: [
          makeSeat({ status: 'SAT_OUT' }),
          makeSeat({ seatIndex: 1, playerId: 2, playerName: 'Bob' }),
        ],
        communityCards: [],
        pots: [],
        currentRound: 'PREFLOP',
        handNumber: 1,
      },
    })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByRole('button', { name: /i'm back/i })).toBeTruthy()
  })

  it('calls sendSitOut when Sit Out is clicked', () => {
    render(<PokerTable gameName="Test Game" />)
    fireEvent.click(screen.getByRole('button', { name: /sit out/i }))
    expect(mockSendSitOut).toHaveBeenCalledTimes(1)
  })

  it('calls sendComeBack when "I\'m Back" is clicked', () => {
    mockState = makeState({
      currentTable: {
        tableId: 1,
        seats: [
          makeSeat({ status: 'SAT_OUT' }),
          makeSeat({ seatIndex: 1, playerId: 2, playerName: 'Bob' }),
        ],
        communityCards: [],
        pots: [],
        currentRound: 'PREFLOP',
        handNumber: 1,
      },
    })
    render(<PokerTable gameName="Test Game" />)
    fireEvent.click(screen.getByRole('button', { name: /i'm back/i }))
    expect(mockSendComeBack).toHaveBeenCalledTimes(1)
  })

  // --- Keyboard shortcuts ---

  it('H key toggles hand rankings', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('hand-rankings')).toBeNull()

    fireEvent.keyDown(window, { key: 'h' })
    expect(screen.getByTestId('hand-rankings')).toBeTruthy()

    fireEvent.keyDown(window, { key: 'h' })
    expect(screen.queryByTestId('hand-rankings')).toBeNull()
  })

  it('H key (uppercase) also toggles hand rankings', () => {
    render(<PokerTable gameName="Test Game" />)
    fireEvent.keyDown(window, { key: 'H' })
    expect(screen.getByTestId('hand-rankings')).toBeTruthy()
  })

  it('D key toggles dashboard', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('dashboard')).toBeNull()

    fireEvent.keyDown(window, { key: 'd' })
    expect(screen.getByTestId('dashboard')).toBeTruthy()

    fireEvent.keyDown(window, { key: 'd' })
    expect(screen.queryByTestId('dashboard')).toBeNull()
  })

  it('V key toggles advisor (only visible when holeCards present)', () => {
    render(<PokerTable gameName="Test Game" />)
    // holeCards = ['Ah', 'Kd'] by default, so advisor should appear
    fireEvent.keyDown(window, { key: 'v' })
    expect(screen.getByTestId('advisor-panel')).toBeTruthy()
  })

  it('N key calls sendContinueRunout when continueRunoutPending is true', () => {
    mockState = makeState({ continueRunoutPending: true })
    render(<PokerTable gameName="Test Game" />)
    fireEvent.keyDown(window, { key: 'n' })
    expect(mockSendContinueRunout).toHaveBeenCalledTimes(1)
  })

  it('N key does not call sendContinueRunout when continueRunoutPending is false', () => {
    render(<PokerTable gameName="Test Game" />)
    fireEvent.keyDown(window, { key: 'n' })
    expect(mockSendContinueRunout).not.toHaveBeenCalled()
  })

  // --- Toolbar buttons ---

  it('Sim button opens simulator', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('simulator')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: /open simulator/i }))
    expect(screen.getByTestId('simulator')).toBeTruthy()
  })

  it('Advisor button toggles advisor panel (only when holeCards present)', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('advisor-panel')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: /toggle advisor/i }))
    expect(screen.getByTestId('advisor-panel')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: /toggle advisor/i }))
    expect(screen.queryByTestId('advisor-panel')).toBeNull()
  })

  it('Dashboard button toggles dashboard', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('dashboard')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: /toggle dashboard/i }))
    expect(screen.getByTestId('dashboard')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: /toggle dashboard/i }))
    expect(screen.queryByTestId('dashboard')).toBeNull()
  })

  // --- Overlay prop ---

  it('renders overlay prop when provided', () => {
    render(
      <PokerTable
        gameName="Test Game"
        overlay={<div data-testid="test-overlay">Overlay</div>}
      />,
    )
    expect(screen.getByTestId('test-overlay')).toBeTruthy()
  })

  it('renders without overlay when overlay prop is omitted', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('test-overlay')).toBeNull()
  })

})

describe('PokerTable - check-fold shortcut', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  it('F key with checkFold pref enabled queues check-fold when no action is required', async () => {
    // Re-mock useGamePrefs to have checkFold: true
    vi.doMock('@/lib/game/useGamePrefs', () => ({
      useGamePrefs: () => ({
        prefs: { fourColorDeck: false, disableShortcuts: false, checkFold: true, dealerChat: true },
      }),
    }))

    // Import the component freshly after mock update
    const { PokerTable: FreshPokerTable } = await import('../PokerTable')

    mockState = makeState({ actionRequired: null })
    render(<FreshPokerTable gameName="Test Game" />)

    fireEvent.keyDown(window, { key: 'f' })
    expect(screen.getByText('Check/Fold Queued')).toBeTruthy()
  })
})
