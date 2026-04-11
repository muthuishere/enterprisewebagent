import { useState, useRef, useEffect } from 'react'
import type { FormEvent, CSSProperties } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSession } from './useSession'
import { useTheme } from '@/app/ThemeContext'
import { TaskPanel } from '@/domain/tasks/TaskPanel'
import type { ConnectionStatus } from '@/lib/ws'

const STATUS_COLORS: Record<ConnectionStatus, string> = {
  connected: '#16a34a',
  connecting: '#eab308',
  disconnected: '#dc2626',
}

const STATUS_LABELS: Record<ConnectionStatus, string> = {
  connected: 'Connected',
  connecting: 'Connecting…',
  disconnected: 'Disconnected',
}

export function SessionPage() {
  const { session, messages, isLoading, streamingText, pendingQuestion, connectionStatus, createSession, sendMessage, answerQuestion, closeSession } = useSession()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()
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
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh', gap: '1rem', padding: '1rem' }}>
        <h1 style={{ color: 'var(--color-text)' }}>Enterprise Web Agent</h1>
        <p style={{ color: 'var(--color-text-muted)' }}>Start a new session to begin.</p>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <button onClick={createSession} style={buttonStyle}>New Session</button>
          <button onClick={() => navigate('/settings')} style={headerLinkStyle}>⚙ Settings</button>
          <button onClick={toggleTheme} style={headerLinkStyle} title="Toggle theme">
            {theme === 'light' ? '🌙' : '☀️'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      {/* Header */}
      <header style={{
        padding: '0.5rem 1rem',
        borderBottom: '1px solid var(--color-border)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '0.5rem',
        backgroundColor: 'var(--color-surface)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: 0 }}>
          {/* Connection status dot */}
          <span
            title={STATUS_LABELS[connectionStatus]}
            style={{
              display: 'inline-block',
              width: 8,
              height: 8,
              borderRadius: '50%',
              backgroundColor: STATUS_COLORS[connectionStatus],
              flexShrink: 0,
            }}
          />
          <span style={{ color: 'var(--color-text)', fontSize: '0.875rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            Session: {session.id.substring(0, 8)}…
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <button onClick={() => navigate('/settings')} style={headerLinkStyle} title="Settings">⚙</button>
          <button onClick={toggleTheme} style={headerLinkStyle} title="Toggle theme">
            {theme === 'light' ? '🌙' : '☀️'}
          </button>
          <button onClick={closeSession} style={buttonSmallStyle}>Close</button>
        </div>
      </header>

      {/* Body: messages + optional side task panel */}
      <div className="session-body" style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Messages */}
        <div style={{ flex: 1, overflow: 'auto', padding: '1rem' }}>
          {messages.map((msg, i) => (
            <div key={i} style={{ marginBottom: '1rem', textAlign: msg.role === 'user' ? 'right' : 'left' }}>
              <div style={{
                display: 'inline-block',
                padding: '0.5rem 1rem',
                borderRadius: '0.75rem',
                maxWidth: '80%',
                backgroundColor: msg.role === 'user' ? 'var(--color-msg-user-bg)' : 'var(--color-msg-assistant-bg)',
                color: msg.role === 'user' ? 'var(--color-msg-user-text)' : 'var(--color-msg-assistant-text)',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
              }}>
                {msg.content}
              </div>
            </div>
          ))}
          {streamingText && (
            <div style={{ marginBottom: '1rem' }}>
              <div style={{ display: 'inline-block', padding: '0.5rem 1rem', borderRadius: '0.75rem', backgroundColor: 'var(--color-msg-assistant-bg)', color: 'var(--color-msg-assistant-text)' }}>
                {streamingText}
              </div>
            </div>
          )}
          {isLoading && !streamingText && (
            <div style={{ color: 'var(--color-loading)', fontStyle: 'italic' }}>Thinking...</div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Side task panel (collapses below messages on mobile via CSS) */}
        <div className="task-panel-side" style={{ width: 260, borderLeft: '1px solid var(--color-border)', overflow: 'auto' }}>
          <TaskPanel sessionId={session.id} />
        </div>
      </div>

      {/* Ask-user modal overlay */}
      {pendingQuestion && (
        <div style={overlayStyle}>
          <div style={modalStyle}>
            <p style={{ margin: '0 0 1rem', fontSize: '1.05rem', fontWeight: 500, color: 'var(--color-text)' }}>{pendingQuestion.question}</p>
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
                  style={{ flex: 1, padding: '0.5rem', borderRadius: '0.5rem', border: '1px solid var(--color-border)', fontSize: '1rem', backgroundColor: 'var(--color-input-bg)', color: 'var(--color-text)' }}
                />
                <button type="submit" disabled={!freeformAnswer.trim()} style={buttonStyle}>Send</button>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Input — fixed at bottom */}
      <form onSubmit={handleSubmit} style={{
        padding: '0.75rem 1rem',
        borderTop: '1px solid var(--color-border)',
        display: 'flex',
        gap: '0.5rem',
        backgroundColor: 'var(--color-surface)',
        flexShrink: 0,
      }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Type a message..."
          disabled={isLoading}
          style={{ flex: 1, padding: '0.5rem', borderRadius: '0.5rem', border: '1px solid var(--color-border)', fontSize: '1rem', backgroundColor: 'var(--color-input-bg)', color: 'var(--color-text)' }}
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
  backgroundColor: 'var(--color-primary)',
  color: '#fff',
  cursor: 'pointer',
  fontSize: '1rem',
}

const buttonSmallStyle: CSSProperties = {
  padding: '0.25rem 0.75rem',
  borderRadius: '0.5rem',
  border: 'none',
  backgroundColor: 'var(--color-danger)',
  color: '#fff',
  cursor: 'pointer',
  fontSize: '0.875rem',
}

const headerLinkStyle: CSSProperties = {
  background: 'none',
  border: 'none',
  cursor: 'pointer',
  fontSize: '1.1rem',
  padding: '0.15rem 0.35rem',
  borderRadius: '0.25rem',
  color: 'var(--color-text-muted)',
}

const overlayStyle: CSSProperties = {
  position: 'fixed',
  inset: 0,
  backgroundColor: 'var(--color-overlay)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 1000,
}

const modalStyle: CSSProperties = {
  backgroundColor: 'var(--color-modal-bg)',
  borderRadius: '0.75rem',
  padding: '1.5rem',
  maxWidth: '28rem',
  width: '90%',
  boxShadow: '0 4px 24px var(--color-shadow)',
}

const choiceButtonStyle: CSSProperties = {
  padding: '0.5rem 1rem',
  borderRadius: '0.5rem',
  border: '1px solid var(--color-primary)',
  backgroundColor: 'var(--color-surface)',
  color: 'var(--color-primary)',
  cursor: 'pointer',
  fontSize: '1rem',
  textAlign: 'left',
}
