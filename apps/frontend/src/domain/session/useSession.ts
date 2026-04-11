import { useState, useCallback, useRef, useEffect } from 'react'
import { sessionApi } from '@/lib/api'
import { connectSession } from '@/lib/ws'
import type { Session, ChatMessage } from './types'
import type { RuntimeEvent } from '@/lib/ws'

export interface PendingQuestion {
  question: string
  choices: string[]
}

export function useSession() {
  const [session, setSession] = useState<Session | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [streamingText, setStreamingText] = useState('')
  const [events, setEvents] = useState<RuntimeEvent[]>([])
  const [pendingQuestion, setPendingQuestion] = useState<PendingQuestion | null>(null)
  const wsRef = useRef<WebSocket | null>(null)

  const createSession = useCallback(async () => {
    const newSession = await sessionApi.create()
    setSession(newSession)
    setMessages([])

    const ws = connectSession(newSession.id, (event) => {
      setEvents((prev) => [...prev, event])
      if (event.type === 'token_delta') {
        setStreamingText((prev) => prev + event.text)
      }
      if (event.type === 'ask_user_requested') {
        setPendingQuestion({ question: event.question, choices: event.choices })
      }
    })
    wsRef.current = ws

    return newSession
  }, [])

  const sendMessage = useCallback(async (input: string) => {
    if (!session) return

    setIsLoading(true)
    setStreamingText('')
    setMessages((prev) => [...prev, { role: 'user', content: input, timestamp: new Date() }])

    try {
      const result = await sessionApi.executeTurn(session.id, input)
      setMessages((prev) => [...prev, {
        role: 'assistant',
        content: result.output,
        timestamp: new Date(),
      }])
    } catch (err) {
      setMessages((prev) => [...prev, {
        role: 'assistant',
        content: `Error: ${err instanceof Error ? err.message : 'Unknown error'}`,
        timestamp: new Date(),
      }])
    } finally {
      setIsLoading(false)
      setStreamingText('')
    }
  }, [session])

  const answerQuestion = useCallback(async (answer: string) => {
    setPendingQuestion(null)
    await sendMessage(answer)
  }, [sendMessage])

  const closeSession = useCallback(async () => {
    if (!session) return
    wsRef.current?.close()
    await sessionApi.close(session.id)
    setSession(null)
    setMessages([])
  }, [session])

  useEffect(() => {
    return () => { wsRef.current?.close() }
  }, [])

  return { session, messages, isLoading, streamingText, events, pendingQuestion, createSession, sendMessage, answerQuestion, closeSession }
}
