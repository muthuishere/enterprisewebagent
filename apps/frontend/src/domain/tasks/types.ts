export type TaskStatus = 'pending' | 'running' | 'completed' | 'failed' | 'cancelled'

export interface TaskDefinition {
  id: string
  sessionId: string
  description: string
  status: TaskStatus
  created: string
}

/** @deprecated Use TaskDefinition instead */
export type Task = TaskDefinition
/** @deprecated Use TaskStatus instead */
export type TaskState = TaskStatus
