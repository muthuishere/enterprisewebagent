import { useState } from 'react'
import type { CSSProperties } from 'react'
import type { Plan, PlanStep, PlanModeType } from './types'

interface PlanPanelProps {
  plan: Plan | null
  onApproveStep: (stepId: string) => void
  onRejectStep: (stepId: string) => void
  onExecutePlan: () => void
  onCollapse: () => void
}

const MODE_COLORS: Record<PlanModeType, string> = {
  OFF: 'var(--color-text-muted)',
  PLANNING: '#eab308',
  REVIEWING: '#3b82f6',
  EXECUTING: '#16a34a',
}

const MODE_LABELS: Record<PlanModeType, string> = {
  OFF: 'Off',
  PLANNING: '📝 Planning',
  REVIEWING: '👀 Reviewing',
  EXECUTING: '⚡ Executing',
}

const STATUS_ICONS: Record<string, string> = {
  PENDING: '⏳',
  APPROVED: '✅',
  REJECTED: '❌',
  IN_PROGRESS: '🔄',
  COMPLETED: '✔️',
  FAILED: '💥',
}

export function PlanPanel({ plan, onApproveStep, onRejectStep, onExecutePlan, onCollapse }: PlanPanelProps) {
  if (!plan) return null

  const hasApprovedSteps = plan.steps.some(s => s.status === 'APPROVED')
  const allReviewed = plan.steps.length > 0 && plan.steps.every(s => s.status !== 'PENDING')

  return (
    <div style={panelStyle}>
      <div style={headerStyle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ ...badgeStyle, backgroundColor: MODE_COLORS[plan.mode] }}>
            {MODE_LABELS[plan.mode]}
          </span>
        </div>
        <button onClick={onCollapse} style={collapseBtnStyle} title="Collapse plan panel">✕</button>
      </div>

      <div style={{ padding: '0.75rem', fontSize: '0.875rem' }}>
        <div style={{ marginBottom: '0.75rem' }}>
          <strong style={{ color: 'var(--color-text)' }}>Goal:</strong>
          <p style={{ margin: '0.25rem 0 0', color: 'var(--color-text-muted)' }}>{plan.goal}</p>
        </div>

        {plan.steps.length === 0 && (
          <p style={{ color: 'var(--color-text-muted)', fontStyle: 'italic' }}>No steps yet…</p>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {plan.steps.map((step, i) => (
            <PlanStepCard
              key={step.id}
              step={step}
              index={i + 1}
              mode={plan.mode}
              onApprove={() => onApproveStep(step.id)}
              onReject={() => onRejectStep(step.id)}
            />
          ))}
        </div>

        {plan.mode === 'REVIEWING' && hasApprovedSteps && allReviewed && (
          <button onClick={onExecutePlan} style={executeBtnStyle}>
            ⚡ Execute Approved Steps
          </button>
        )}
      </div>
    </div>
  )
}

function PlanStepCard({ step, index, mode, onApprove, onReject }: {
  step: PlanStep
  index: number
  mode: PlanModeType
  onApprove: () => void
  onReject: () => void
}) {
  return (
    <div style={stepCardStyle}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem' }}>
        <span style={{ flexShrink: 0, fontSize: '0.75rem' }}>{STATUS_ICONS[step.status] ?? '❓'}</span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--color-text)' }}>
            {index}. {step.description}
          </span>
          {step.result && (
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
              → {step.result}
            </p>
          )}
        </div>
      </div>

      {mode === 'REVIEWING' && step.status === 'PENDING' && (
        <div style={{ display: 'flex', gap: '0.25rem', marginTop: '0.35rem', marginLeft: '1.25rem' }}>
          <button onClick={onApprove} style={approveBtn}>Approve</button>
          <button onClick={onReject} style={rejectBtn}>Reject</button>
        </div>
      )}
    </div>
  )
}

const panelStyle: CSSProperties = {
  borderLeft: '1px solid var(--color-border)',
  backgroundColor: 'var(--color-surface)',
  width: 280,
  display: 'flex',
  flexDirection: 'column',
  overflow: 'auto',
}

const headerStyle: CSSProperties = {
  padding: '0.5rem 0.75rem',
  borderBottom: '1px solid var(--color-border)',
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
}

const badgeStyle: CSSProperties = {
  padding: '0.15rem 0.5rem',
  borderRadius: '0.25rem',
  color: '#fff',
  fontSize: '0.75rem',
  fontWeight: 600,
}

const collapseBtnStyle: CSSProperties = {
  background: 'none',
  border: 'none',
  cursor: 'pointer',
  color: 'var(--color-text-muted)',
  fontSize: '1rem',
  padding: '0.15rem 0.3rem',
}

const stepCardStyle: CSSProperties = {
  padding: '0.5rem',
  borderRadius: '0.375rem',
  border: '1px solid var(--color-border)',
  backgroundColor: 'var(--color-input-bg)',
}

const approveBtn: CSSProperties = {
  padding: '0.15rem 0.5rem',
  fontSize: '0.7rem',
  borderRadius: '0.25rem',
  border: '1px solid #16a34a',
  backgroundColor: 'transparent',
  color: '#16a34a',
  cursor: 'pointer',
}

const rejectBtn: CSSProperties = {
  padding: '0.15rem 0.5rem',
  fontSize: '0.7rem',
  borderRadius: '0.25rem',
  border: '1px solid #dc2626',
  backgroundColor: 'transparent',
  color: '#dc2626',
  cursor: 'pointer',
}

const executeBtnStyle: CSSProperties = {
  marginTop: '0.75rem',
  width: '100%',
  padding: '0.5rem',
  borderRadius: '0.5rem',
  border: 'none',
  backgroundColor: '#16a34a',
  color: '#fff',
  cursor: 'pointer',
  fontSize: '0.875rem',
  fontWeight: 600,
}
