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
  isCommand?: boolean
}

export interface ChatMessage {
  role: 'user' | 'assistant' | 'command'
  content: string
  timestamp: Date
}

export type PlanStepStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
export type PlanModeType = 'OFF' | 'PLANNING' | 'REVIEWING' | 'EXECUTING'

export interface PlanStep {
  id: string
  description: string
  status: PlanStepStatus
  result?: string
}

export interface Plan {
  sessionId: string
  goal: string
  mode: PlanModeType
  created: string
  steps: PlanStep[]
}
