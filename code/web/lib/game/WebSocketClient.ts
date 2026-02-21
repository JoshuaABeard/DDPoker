/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { ClientMessage, ClientMessageType, ConnectionState, ServerMessage } from './types'

/** Close code sent by server when another tab opens the same game. */
export const CLOSE_CONNECTION_REPLACED = 4409

/** Exponential backoff delays (ms): 1s, 2s, 4s, 8s, 16s, 30s, 30s, ... */
const BACKOFF_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000]

export interface WebSocketClientOptions {
  onMessage: (message: ServerMessage) => void
  onStateChange: (state: ConnectionState) => void
  onError: (error: Event) => void
}

/**
 * Thin wrapper around the browser WebSocket API.
 *
 * Manages connection lifecycle, reconnection with exponential backoff,
 * and message serialization. Consumers receive typed server messages
 * and dispatch typed client messages.
 *
 * Token flow:
 * 1. Initial connection: caller provides a short-lived ws-connect token
 * 2. Server sends CONNECTED message containing a reconnectToken
 * 3. Caller stores the reconnect token via setReconnectToken()
 * 4. On reconnect: uses the reconnect token (no cookie required)
 */
export class WebSocketClient {
  private ws: WebSocket | null = null
  private wsUrl: string | null = null
  private initialToken: string | null = null
  private reconnectToken: string | null = null
  private sequenceNumber = 0
  private reconnectAttempt = 0
  private reconnectTimerId: ReturnType<typeof setTimeout> | null = null
  private connectionState: ConnectionState = 'DISCONNECTED'
  private intentionalClose = false

  constructor(private readonly options: WebSocketClientOptions) {}

  /**
   * Open a WebSocket connection to the game server.
   *
   * @param wsUrl Base WebSocket URL (ws:// or wss://)
   * @param token Short-lived ws-connect token from GET /api/v1/auth/ws-token
   */
  connect(wsUrl: string, token: string): void {
    this.wsUrl = wsUrl
    this.initialToken = token
    this.intentionalClose = false
    this.reconnectAttempt = 0
    this.openSocket(token)
  }

  /**
   * Intentionally close the connection. Does not trigger reconnection.
   */
  disconnect(): void {
    this.intentionalClose = true
    this.clearReconnectTimer()
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect')
      this.ws = null
    }
    this.setState('DISCONNECTED')
  }

  /**
   * Send a typed client message to the server.
   *
   * @param type The client message type
   * @param data Optional payload
   */
  send<T>(type: ClientMessageType, data?: T): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    const message: ClientMessage<T> = {
      type,
      sequenceNumber: ++this.sequenceNumber,
      ...(data !== undefined && { data }),
    }
    this.ws.send(JSON.stringify(message))
  }

  /**
   * Store the reconnect token received in the CONNECTED message.
   * Used for all subsequent reconnection attempts (bypasses cookie auth).
   */
  setReconnectToken(token: string): void {
    this.reconnectToken = token
  }

  getConnectionState(): ConnectionState {
    return this.connectionState
  }

  // ---------------------------------------------------------------------------
  // Private
  // ---------------------------------------------------------------------------

  private openSocket(token: string): void {
    if (!this.wsUrl) return

    this.setState(this.reconnectAttempt === 0 ? 'CONNECTING' : 'RECONNECTING')

    const url = `${this.wsUrl}?token=${encodeURIComponent(token)}`
    const ws = new WebSocket(url)
    this.ws = ws

    ws.onopen = () => {
      this.reconnectAttempt = 0
      this.setState('CONNECTED')
    }

    ws.onmessage = (event: MessageEvent) => {
      try {
        const message = JSON.parse(event.data as string) as ServerMessage
        this.options.onMessage(message)
      } catch {
        // Malformed JSON from server — log and ignore
        console.warn('[WebSocketClient] Failed to parse server message:', event.data)
      }
    }

    ws.onerror = (event: Event) => {
      this.options.onError(event)
    }

    ws.onclose = (event: CloseEvent) => {
      this.ws = null

      // Code 4409: replaced by a newer tab — do not reconnect
      if (event.code === CLOSE_CONNECTION_REPLACED) {
        this.setState('DISCONNECTED')
        return
      }

      if (this.intentionalClose) {
        this.setState('DISCONNECTED')
        return
      }

      // Schedule reconnect with exponential backoff
      this.scheduleReconnect()
    }
  }

  private scheduleReconnect(): void {
    const maxAttempts = BACKOFF_DELAYS.length + 4 // ~10 attempts total
    if (this.reconnectAttempt >= maxAttempts) {
      this.setState('DISCONNECTED')
      return
    }

    this.setState('RECONNECTING')
    const delayIndex = Math.min(this.reconnectAttempt, BACKOFF_DELAYS.length - 1)
    const delay = BACKOFF_DELAYS[delayIndex]

    this.reconnectTimerId = setTimeout(() => {
      this.reconnectAttempt++
      // Use reconnect token if available (bypasses cookie), otherwise fall back to initial token
      const token = this.reconnectToken ?? this.initialToken
      if (!token) {
        this.setState('DISCONNECTED')
        return
      }
      this.openSocket(token)
    }, delay)
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimerId !== null) {
      clearTimeout(this.reconnectTimerId)
      this.reconnectTimerId = null
    }
  }

  private setState(state: ConnectionState): void {
    if (this.connectionState !== state) {
      this.connectionState = state
      this.options.onStateChange(state)
    }
  }
}

/**
 * Derive the WebSocket protocol from the current page protocol.
 * Never hardcoded — follows the page's TLS status.
 */
export function deriveWsProtocol(): 'ws:' | 'wss:' {
  if (typeof window === 'undefined') return 'ws:'
  return window.location.protocol === 'https:' ? 'wss:' : 'ws:'
}
