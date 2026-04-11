import { apiRequest } from './http'
import { endpoints } from './config'
import type { Session, TurnResult } from '@/domain/session/types'
import type { TaskDefinition } from '@/domain/tasks/types'

export const sessionApi = {
  create: (workspaceId = 'default') =>
    apiRequest<Session>(endpoints.sessions.create(), {
      method: 'POST',
      body: JSON.stringify({ workspaceId }),
    }),

  get: (id: string) =>
    apiRequest<Session>(endpoints.sessions.get(id)),

  executeTurn: (sessionId: string, input: string) =>
    apiRequest<TurnResult>(
      `${endpoints.sessions.get(sessionId)}/turns`,
      { method: 'POST', body: JSON.stringify({ input }) },
    ),

  close: (sessionId: string) =>
    apiRequest<Record<string, unknown>>(
      `${endpoints.sessions.get(sessionId)}/close`,
      { method: 'POST' },
    ),
}

export const taskApi = {
  list: (sessionId: string) =>
    apiRequest<TaskDefinition[]>(endpoints.tasks.list(sessionId)),

  get: (id: string) =>
    apiRequest<TaskDefinition>(endpoints.tasks.get(id)),
}

export const configApi = {
  get: () =>
    apiRequest<{ providers: string[]; version: string; runtime: string }>(endpoints.config.get()),

  health: () =>
    apiRequest<{ status: string }>(endpoints.health()),
}
