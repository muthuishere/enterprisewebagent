import { apiRequest } from './http'
import { endpoints } from './config'
import type { Session, TurnResult, Plan } from '@/domain/session/types'
import type { TaskDefinition } from '@/domain/tasks/types'

export interface ModelInfo {
  id: string
  displayName: string
  provider: string
  available: boolean
}

export interface AppConfig {
  providers: string[]
  defaultProvider: string
  models: ModelInfo[]
  version: string
  runtime: string
}

export const sessionApi = {
  create: (workspaceId = 'default') =>
    apiRequest<Session>(endpoints.sessions.create(), {
      method: 'POST',
      body: JSON.stringify({ workspaceId }),
    }),

  get: (id: string) =>
    apiRequest<Session>(endpoints.sessions.get(id)),

  executeTurn: (sessionId: string, input: string, model?: string) =>
    apiRequest<TurnResult>(
      `${endpoints.sessions.get(sessionId)}/turns`,
      { method: 'POST', body: JSON.stringify({ input, ...(model ? { model } : {}) }) },
    ),

  close: (sessionId: string) =>
    apiRequest<Record<string, unknown>>(
      `${endpoints.sessions.get(sessionId)}/close`,
      { method: 'POST' },
    ),
}

export const planApi = {
  createOrGet: (sessionId: string, goal: string) =>
    apiRequest<Plan>(
      `${endpoints.sessions.get(sessionId)}/plan`,
      { method: 'POST', body: JSON.stringify({ goal }) },
    ),

  get: (sessionId: string) =>
    apiRequest<Plan>(`${endpoints.sessions.get(sessionId)}/plan`),

  updateStep: (sessionId: string, stepId: string, status: string) =>
    apiRequest<Plan>(
      `${endpoints.sessions.get(sessionId)}/plan/steps/${stepId}`,
      { method: 'PUT', body: JSON.stringify({ status }) },
    ),

  execute: (sessionId: string) =>
    apiRequest<Plan>(
      `${endpoints.sessions.get(sessionId)}/plan/execute`,
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
    apiRequest<AppConfig>(endpoints.config.get()),

  refresh: () =>
    apiRequest<AppConfig>(endpoints.config.refresh(), { method: 'POST' }),

  health: () =>
    apiRequest<{ status: string }>(endpoints.health()),
}
