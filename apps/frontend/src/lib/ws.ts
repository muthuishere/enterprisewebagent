import { WS_BASE } from './config'

export type RuntimeEvent =
  | { type: 'turn_started'; sessionId: string }
  | { type: 'token_delta'; sessionId: string; text: string }
  | { type: 'tool_requested'; sessionId: string; toolName: string }
  | { type: 'tool_completed'; sessionId: string; toolName: string }
  | { type: 'turn_completed'; sessionId: string; output: string }
  | { type: 'turn_failed'; sessionId: string; error: string }
  | { type: 'task_state_changed'; sessionId: string; taskId: string; newState: string }
  | { type: 'ask_user_requested'; sessionId: string; question: string; choices: string[] }

export type ConnectionStatus = 'connected' | 'connecting' | 'disconnected'

export interface SessionConnection {
  ws: WebSocket
  close: () => void
}

export function connectSession(
  sessionId: string,
  onEvent: (event: RuntimeEvent) => void,
  onStatusChange?: (status: ConnectionStatus) => void,
): SessionConnection {
  let intentionallyClosed = false
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let currentWs: WebSocket | null = null

  function connect() {
    onStatusChange?.('connecting')
    const ws = new WebSocket(`${WS_BASE}/stream?sessionId=${sessionId}`)
    currentWs = ws

    ws.onopen = () => {
      onStatusChange?.('connected')
    }

    ws.onmessage = (e) => {
      try {
        onEvent(JSON.parse(e.data))
      } catch {
        /* ignore parse errors */
      }
    }

    ws.onclose = () => {
      if (!intentionallyClosed) {
        onStatusChange?.('disconnected')
        reconnectTimer = setTimeout(connect, 3000)
      }
    }

    ws.onerror = () => {
      // onclose will fire after onerror, which handles reconnect
    }
  }

  connect()

  return {
    get ws() { return currentWs! },
    close() {
      intentionallyClosed = true
      if (reconnectTimer) clearTimeout(reconnectTimer)
      currentWs?.close()
      onStatusChange?.('disconnected')
    },
  }
}
