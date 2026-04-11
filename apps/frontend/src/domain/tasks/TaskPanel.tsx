import { useEffect, useState } from 'react'
import { taskApi } from '@/lib/api'
import type { TaskDefinition } from './types'

const statusColors: Record<string, string> = {
  pending: '#888',
  running: '#2563eb',
  completed: '#16a34a',
  failed: '#dc2626',
  cancelled: '#9ca3af',
}

export function TaskPanel({ sessionId }: { sessionId: string }) {
  const [tasks, setTasks] = useState<TaskDefinition[]>([])

  useEffect(() => {
    if (!sessionId) return
    taskApi.list(sessionId).then(setTasks).catch(() => {})
    const interval = setInterval(() => {
      taskApi.list(sessionId).then(setTasks).catch(() => {})
    }, 5000)
    return () => clearInterval(interval)
  }, [sessionId])

  if (tasks.length === 0) return null

  return (
    <div style={{ padding: '0.5rem 1rem', borderTop: '1px solid #e0e0e0' }}>
      <h3 style={{ margin: '0 0 0.5rem', fontSize: '0.875rem' }}>Tasks</h3>
      {tasks.map((task) => (
        <div key={task.id} style={{ fontSize: '0.8rem', padding: '0.25rem 0', display: 'flex', gap: '0.5rem' }}>
          <span style={{ fontWeight: 'bold', color: statusColors[task.status] ?? '#888' }}>[{task.status}]</span>
          <span>{task.description}</span>
        </div>
      ))}
    </div>
  )
}
