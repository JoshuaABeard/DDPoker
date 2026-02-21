import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { WebSocketClient, deriveWsProtocol, CLOSE_CONNECTION_REPLACED } from '../WebSocketClient'

// ---------------------------------------------------------------------------
// Mock WebSocket
// ---------------------------------------------------------------------------

class MockWebSocket {
  static CONNECTING = 0
  static OPEN = 1
  static CLOSING = 2
  static CLOSED = 3

  readyState = MockWebSocket.OPEN
  onopen: ((e: Event) => void) | null = null
  onmessage: ((e: MessageEvent) => void) | null = null
  onerror: ((e: Event) => void) | null = null
  onclose: ((e: CloseEvent) => void) | null = null

  sentMessages: string[] = []
  closeCalls: Array<[number?, string?]> = []

  constructor(public url: string) {
    MockWebSocket.instances.push(this)
  }

  send(data: string) {
    this.sentMessages.push(data)
  }

  close(code?: number, reason?: string) {
    this.closeCalls.push([code, reason])
    this.readyState = MockWebSocket.CLOSED
    // Trigger onclose
    this.onclose?.({ code: code ?? 1000, reason: reason ?? '', wasClean: true } as CloseEvent)
  }

  // Simulate server sending a message
  simulateMessage(data: object) {
    this.onmessage?.({ data: JSON.stringify(data) } as MessageEvent)
  }

  // Simulate server closing connection
  simulateClose(code = 1000, reason = '') {
    this.readyState = MockWebSocket.CLOSED
    this.onclose?.({ code, reason, wasClean: code === 1000 } as CloseEvent)
  }

  // Simulate connection open
  simulateOpen() {
    this.readyState = MockWebSocket.OPEN
    this.onopen?.(new Event('open'))
  }

  static instances: MockWebSocket[] = []
  static reset() {
    MockWebSocket.instances = []
  }

  static latest(): MockWebSocket {
    return MockWebSocket.instances[MockWebSocket.instances.length - 1]
  }
}

// ---------------------------------------------------------------------------
// Setup
// ---------------------------------------------------------------------------

beforeEach(() => {
  MockWebSocket.reset()
  vi.stubGlobal('WebSocket', MockWebSocket)
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('WebSocketClient', () => {
  it('connects and transitions to CONNECTED state on open', () => {
    const states: string[] = []
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: (s) => states.push(s),
      onError: vi.fn(),
    })

    client.connect('ws://localhost:8080/ws/games/game-1', 'test-token')
    expect(states).toContain('CONNECTING')

    MockWebSocket.latest().simulateOpen()
    expect(states).toContain('CONNECTED')
  })

  it('appends token to WebSocket URL', () => {
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: vi.fn(),
      onError: vi.fn(),
    })
    client.connect('ws://localhost:8080/ws/games/g1', 'my-token')
    expect(MockWebSocket.latest().url).toBe('ws://localhost:8080/ws/games/g1?token=my-token')
  })

  it('dispatches parsed server messages to onMessage', () => {
    const messages: object[] = []
    const client = new WebSocketClient({
      onMessage: (m) => messages.push(m),
      onStateChange: vi.fn(),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'tok')
    const ws = MockWebSocket.latest()
    ws.simulateOpen()
    ws.simulateMessage({ type: 'CONNECTED', gameId: 'g1', sequenceNumber: 1, data: {} })
    expect(messages).toHaveLength(1)
    expect((messages[0] as { type: string }).type).toBe('CONNECTED')
  })

  it('ignores malformed JSON without crashing', () => {
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: vi.fn(),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'tok')
    const ws = MockWebSocket.latest()
    ws.simulateOpen()
    ws.onmessage?.({ data: 'not-valid-json' } as MessageEvent)
    // Should not throw
  })

  it('send() serializes message with sequenceNumber', () => {
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: vi.fn(),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'tok')
    const ws = MockWebSocket.latest()
    ws.simulateOpen()

    client.send('PLAYER_ACTION', { action: 'FOLD', amount: 0 })
    expect(ws.sentMessages).toHaveLength(1)
    const parsed = JSON.parse(ws.sentMessages[0])
    expect(parsed.type).toBe('PLAYER_ACTION')
    expect(parsed.sequenceNumber).toBe(1)
    expect(parsed.data).toEqual({ action: 'FOLD', amount: 0 })
  })

  it('send() increments sequenceNumber on each call', () => {
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: vi.fn(),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'tok')
    const ws = MockWebSocket.latest()
    ws.simulateOpen()

    client.send('CHAT', { message: 'hi', tableChat: true })
    client.send('CHAT', { message: 'bye', tableChat: true })

    const seq1 = JSON.parse(ws.sentMessages[0]).sequenceNumber
    const seq2 = JSON.parse(ws.sentMessages[1]).sequenceNumber
    expect(seq2).toBe(seq1 + 1)
  })

  it('send() does nothing when not connected', () => {
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: vi.fn(),
      onError: vi.fn(),
    })
    client.send('PLAYER_ACTION', { action: 'FOLD', amount: 0 })
    // No crash, no socket created
    expect(MockWebSocket.instances).toHaveLength(0)
  })

  it('disconnect() closes socket and transitions to DISCONNECTED', () => {
    const states: string[] = []
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: (s) => states.push(s),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'tok')
    const ws = MockWebSocket.latest()
    ws.simulateOpen()

    client.disconnect()
    expect(states).toContain('DISCONNECTED')
  })

  it('does not reconnect after intentional disconnect', () => {
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: vi.fn(),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'tok')
    const ws = MockWebSocket.latest()
    ws.simulateOpen()

    client.disconnect()
    vi.runAllTimers()

    // Still only 1 WebSocket instance created
    expect(MockWebSocket.instances).toHaveLength(1)
  })

  it('reconnects on unexpected close with exponential backoff', () => {
    const states: string[] = []
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: (s) => states.push(s),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'tok')
    const ws = MockWebSocket.latest()
    ws.simulateOpen()

    // Simulate unexpected server close
    ws.simulateClose(1006, 'Connection lost')

    // Should schedule reconnect
    expect(states).toContain('RECONNECTING')

    // Advance timer to trigger reconnect (1000ms backoff)
    vi.advanceTimersByTime(1500)
    expect(MockWebSocket.instances).toHaveLength(2)
  })

  it('uses reconnect token (not initial token) for reconnection', () => {
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: vi.fn(),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'initial-token')
    client.setReconnectToken('reconnect-token')

    MockWebSocket.latest().simulateOpen()
    MockWebSocket.latest().simulateClose(1006)

    vi.advanceTimersByTime(1500)
    expect(MockWebSocket.latest().url).toContain('token=reconnect-token')
  })

  it('does not reconnect after close code 4409 (connection replaced)', () => {
    const states: string[] = []
    const client = new WebSocketClient({
      onMessage: vi.fn(),
      onStateChange: (s) => states.push(s),
      onError: vi.fn(),
    })
    client.connect('ws://localhost/ws/games/g1', 'tok')
    MockWebSocket.latest().simulateOpen()
    MockWebSocket.latest().simulateClose(CLOSE_CONNECTION_REPLACED, 'Connection replaced')

    vi.runAllTimers()

    // Should not reconnect — only one WS created
    expect(MockWebSocket.instances).toHaveLength(1)
    expect(states[states.length - 1]).toBe('DISCONNECTED')
  })
})

// ---------------------------------------------------------------------------
// deriveWsProtocol
// ---------------------------------------------------------------------------

describe('deriveWsProtocol', () => {
  it('returns ws: in test environment (no window)', () => {
    // jsdom sets window.location.protocol to 'about:' by default
    // deriveWsProtocol returns 'ws:' for any non-https protocol
    const result = deriveWsProtocol()
    expect(result).toMatch(/^wss?:$/)
  })

  it('returns wss: when location is https', () => {
    vi.stubGlobal('window', { location: { protocol: 'https:' } })
    const result = deriveWsProtocol()
    expect(result).toBe('wss:')
    vi.unstubAllGlobals()
  })

  it('returns ws: when location is http', () => {
    vi.stubGlobal('window', { location: { protocol: 'http:' } })
    const result = deriveWsProtocol()
    expect(result).toBe('ws:')
    vi.unstubAllGlobals()
  })
})
