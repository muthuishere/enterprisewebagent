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

export function connectSession(sessionId: string, onEvent: (event: RuntimeEvent) => void): WebSocket {
  const ws = new WebSocket(`${WS_BASE}/stream?sessionId=${sessionId}`)
  ws.onmessage = (e) => {
    try {
      onEvent(JSON.parse(e.data))
    } catch {
      /* ignore parse errors */
    }
  }
  return ws
}
