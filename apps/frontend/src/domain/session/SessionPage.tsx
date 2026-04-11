import { useState, useRef, useEffect } from 'react'
import type { FormEvent, CSSProperties } from 'react'
import { useSession } from './useSession'
import { TaskPanel } from '@/domain/tasks/TaskPanel'

export function SessionPage() {
  const { session, messages, isLoading, streamingText, pendingQuestion, createSession, sendMessage, answerQuestion, closeSession } = useSession()
  const [input, setInput] = useState('')
  const [freeformAnswer, setFreeformAnswer] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamingText])

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!input.trim() || isLoading) return
    sendMessage(input.trim())
    setInput('')
  }

  const handleFreeformSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!freeformAnswer.trim()) return
    answerQuestion(freeformAnswer.trim())
    setFreeformAnswer('')
  }

  if (!session) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh', gap: '1rem' }}>
        <h1>Enterprise Web Agent</h1>
        <p>Start a new session to begin.</p>
        <button onClick={createSession} style={buttonStyle}>New Session</button>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      {/* Header */}
      <header style={{ padding: '0.75rem 1rem', borderBottom: '1px solid #e0e0e0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>Session: {session.id.substring(0, 8)}...</span>
        <button onClick={closeSession} style={buttonSmallStyle}>Close Session</button>
      </header>

      {/* Messages */}
      <div style={{ flex: 1, overflow: 'auto', padding: '1rem' }}>
        {messages.map((msg, i) => (
          <div key={i} style={{ marginBottom: '1rem', textAlign: msg.role === 'user' ? 'right' : 'left' }}>
            <div style={{
              display: 'inline-block',
              padding: '0.5rem 1rem',
              borderRadius: '0.75rem',
              maxWidth: '80%',
              backgroundColor: msg.role === 'user' ? '#007bff' : '#f0f0f0',
              color: msg.role === 'user' ? '#fff' : '#333',
              whiteSpace: 'pre-wrap',
            }}>
              {msg.content}
            </div>
          </div>
        ))}
        {streamingText && (
          <div style={{ marginBottom: '1rem' }}>
            <div style={{ display: 'inline-block', padding: '0.5rem 1rem', borderRadius: '0.75rem', backgroundColor: '#f0f0f0', color: '#333' }}>
              {streamingText}
            </div>
          </div>
        )}
        {isLoading && !streamingText && (
          <div style={{ color: '#999', fontStyle: 'italic' }}>Thinking...</div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Ask-user modal overlay */}
      {pendingQuestion && (
        <div style={overlayStyle}>
          <div style={modalStyle}>
            <p style={{ margin: '0 0 1rem', fontSize: '1.05rem', fontWeight: 500 }}>{pendingQuestion.question}</p>
            {pendingQuestion.choices.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {pendingQuestion.choices.map((choice) => (
                  <button key={choice} onClick={() => { answerQuestion(choice); setFreeformAnswer('') }} style={choiceButtonStyle}>
                    {choice}
                  </button>
                ))}
              </div>
            ) : (
              <form onSubmit={handleFreeformSubmit} style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  value={freeformAnswer}
                  onChange={(e) => setFreeformAnswer(e.target.value)}
                  placeholder="Type your answer..."
                  autoFocus
                  style={{ flex: 1, padding: '0.5rem', borderRadius: '0.5rem', border: '1px solid #ccc', fontSize: '1rem' }}
                />
                <button type="submit" disabled={!freeformAnswer.trim()} style={buttonStyle}>Send</button>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Task panel */}
      <TaskPanel sessionId={session.id} />

      {/* Input */}
      <form onSubmit={handleSubmit} style={{ padding: '1rem', borderTop: '1px solid #e0e0e0', display: 'flex', gap: '0.5rem' }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Type a message..."
          disabled={isLoading}
          style={{ flex: 1, padding: '0.5rem', borderRadius: '0.5rem', border: '1px solid #ccc', fontSize: '1rem' }}
        />
        <button type="submit" disabled={isLoading || !input.trim()} style={buttonStyle}>
          Send
        </button>
      </form>
    </div>
  )
}

const buttonStyle: CSSProperties = {
  padding: '0.5rem 1.5rem',
  borderRadius: '0.5rem',
  border: 'none',
  backgroundColor: '#007bff',
  color: '#fff',
  cursor: 'pointer',
  fontSize: '1rem',
}

const buttonSmallStyle: CSSProperties = {
  ...buttonStyle,
  padding: '0.25rem 0.75rem',
  fontSize: '0.875rem',
  backgroundColor: '#dc3545',
}

const overlayStyle: CSSProperties = {
  position: 'fixed',
  inset: 0,
  backgroundColor: 'rgba(0, 0, 0, 0.5)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 1000,
}

const modalStyle: CSSProperties = {
  backgroundColor: '#fff',
  borderRadius: '0.75rem',
  padding: '1.5rem',
  maxWidth: '28rem',
  width: '90%',
  boxShadow: '0 4px 24px rgba(0, 0, 0, 0.2)',
}

const choiceButtonStyle: CSSProperties = {
  padding: '0.5rem 1rem',
  borderRadius: '0.5rem',
  border: '1px solid #007bff',
  backgroundColor: '#fff',
  color: '#007bff',
  cursor: 'pointer',
  fontSize: '1rem',
  textAlign: 'left',
}
