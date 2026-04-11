import { useState, useEffect } from 'react'
import type { FormEvent, CSSProperties } from 'react'
import { useNavigate } from 'react-router-dom'
import { configApi } from '@/lib/api'
import type { ModelInfo } from '@/lib/api'

const STORAGE_KEY = 'ewa-settings'

export interface AppSettings {
  serverUrl: string
  apiKey: string
  defaultModel: string
}

const DEFAULT_SETTINGS: AppSettings = {
  serverUrl: '',
  apiKey: '',
  defaultModel: '',
}

export function loadSettings(): AppSettings {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) return { ...DEFAULT_SETTINGS, ...JSON.parse(stored) }
  } catch { /* ignore */ }
  return DEFAULT_SETTINGS
}

function saveSettings(settings: AppSettings) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(settings)) } catch { /* ignore */ }
}

function groupByProvider(models: ModelInfo[]): Record<string, ModelInfo[]> {
  const grouped: Record<string, ModelInfo[]> = {}
  for (const m of models) {
    if (!grouped[m.provider]) grouped[m.provider] = []
    grouped[m.provider].push(m)
  }
  return grouped
}

export function SettingsPage() {
  const navigate = useNavigate()
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS)
  const [saved, setSaved] = useState(false)
  const [models, setModels] = useState<ModelInfo[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setSettings(loadSettings())
    configApi.get()
      .then((cfg) => setModels(cfg.models ?? []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    saveSettings(settings)
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  const handleRefresh = async () => {
    setLoading(true)
    try {
      const cfg = await configApi.refresh()
      setModels(cfg.models ?? [])
    } catch { /* ignore */ }
    setLoading(false)
  }

  const grouped = groupByProvider(models)

  return (
    <div style={containerStyle}>
      <div style={cardStyle}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h1 style={{ margin: 0, fontSize: '1.25rem', color: 'var(--color-text)' }}>Settings</h1>
          <button onClick={() => navigate('/session')} style={linkButtonStyle}>← Back to Session</button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <label style={labelStyle}>
            Server URL
            <input
              type="text"
              value={settings.serverUrl}
              onChange={(e) => setSettings((s) => ({ ...s, serverUrl: e.target.value }))}
              placeholder="http://localhost:8080"
              style={inputStyle}
            />
          </label>

          <label style={labelStyle}>
            API Key
            <input
              type="password"
              value={settings.apiKey}
              onChange={(e) => setSettings((s) => ({ ...s, apiKey: e.target.value }))}
              placeholder="Enter API key..."
              style={inputStyle}
            />
          </label>

          <label style={labelStyle}>
            Default Model
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <select
                value={settings.defaultModel}
                onChange={(e) => setSettings((s) => ({ ...s, defaultModel: e.target.value }))}
                style={{ ...inputStyle, flex: 1 }}
                disabled={loading}
              >
                <option value="">Use server default</option>
                {Object.entries(grouped).map(([provider, providerModels]) => (
                  <optgroup key={provider} label={provider.charAt(0).toUpperCase() + provider.slice(1)}>
                    {providerModels.map((m) => (
                      <option key={m.id} value={m.id} disabled={!m.available}>
                        {m.displayName}{!m.available ? ' (unavailable)' : ''}
                      </option>
                    ))}
                  </optgroup>
                ))}
              </select>
              <button type="button" onClick={handleRefresh} style={refreshButtonStyle} disabled={loading}>
                ↻
              </button>
            </div>
          </label>

          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', marginTop: '0.5rem' }}>
            <button type="submit" style={saveButtonStyle}>Save Settings</button>
            {saved && <span style={{ color: 'var(--color-success)', fontSize: '0.875rem' }}>✓ Saved</span>}
          </div>
        </form>
      </div>
    </div>
  )
}

const containerStyle: CSSProperties = {
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'flex-start',
  minHeight: '100vh',
  padding: '3rem 1rem',
}

const cardStyle: CSSProperties = {
  width: '100%',
  maxWidth: '32rem',
  padding: '1.5rem',
  borderRadius: '0.75rem',
  backgroundColor: 'var(--color-surface)',
  border: '1px solid var(--color-border)',
  boxShadow: '0 4px 24px var(--color-shadow)',
}

const labelStyle: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '0.35rem',
  fontSize: '0.875rem',
  fontWeight: 600,
  color: 'var(--color-text-muted)',
}

const inputStyle: CSSProperties = {
  padding: '0.5rem 0.75rem',
  borderRadius: '0.5rem',
  border: '1px solid var(--color-border)',
  fontSize: '1rem',
  backgroundColor: 'var(--color-input-bg)',
  color: 'var(--color-text)',
  outline: 'none',
}

const saveButtonStyle: CSSProperties = {
  padding: '0.5rem 1.5rem',
  borderRadius: '0.5rem',
  border: 'none',
  backgroundColor: 'var(--color-primary)',
  color: '#fff',
  cursor: 'pointer',
  fontSize: '1rem',
  fontWeight: 500,
}

const refreshButtonStyle: CSSProperties = {
  padding: '0.5rem 0.75rem',
  borderRadius: '0.5rem',
  border: '1px solid var(--color-border)',
  backgroundColor: 'var(--color-input-bg)',
  color: 'var(--color-text)',
  cursor: 'pointer',
  fontSize: '1rem',
  lineHeight: 1,
}

const linkButtonStyle: CSSProperties = {
  background: 'none',
  border: 'none',
  color: 'var(--color-primary)',
  cursor: 'pointer',
  fontSize: '0.875rem',
  padding: 0,
}
