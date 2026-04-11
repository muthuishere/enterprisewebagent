export const API_BASE = '/api/v1'
export const WS_BASE = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`

export const endpoints = {
  sessions: {
    list: () => `${API_BASE}/sessions`,
    create: () => `${API_BASE}/sessions`,
    get: (id: string) => `${API_BASE}/sessions/${id}`,
    delete: (id: string) => `${API_BASE}/sessions/${id}`,
  },
  chat: {
    stream: () => `${API_BASE}/chat`,
    answer: () => `${API_BASE}/chat/answer`,
  },
  tasks: {
    list: (sessionId: string) => `${API_BASE}/tasks?sessionId=${sessionId}`,
    get: (id: string) => `${API_BASE}/tasks/${id}`,
  },
  config: {
    get: () => `${API_BASE}/config`,
    update: () => `${API_BASE}/config`,
    refresh: () => `${API_BASE}/config/refresh`,
  },
  health: () => '/actuator/health',
} as const

export const routes = {
  home: '/',
  session: (id: string) => `/session/${id}`,
} as const
