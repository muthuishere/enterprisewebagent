export interface Session {
  id: string
  workspaceId: string
  status: 'ACTIVE' | 'PAUSED' | 'CLOSED'
  created: string
  lastActive: string
}

export interface TranscriptEntry {
  role: 'user' | 'assistant' | 'system' | 'tool_call' | 'tool_result'
  content: string
  timestamp: string
}

export interface TurnResult {
  sessionId: string
  output: string
  completed: boolean
  toolCalls: number
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}
