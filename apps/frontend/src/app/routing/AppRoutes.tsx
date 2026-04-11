import { Routes, Route, Navigate } from 'react-router-dom'
import { SessionPage } from '@/domain/session/SessionPage'
import { SettingsPage } from '@/domain/settings/SettingsPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route index element={<Navigate to="/session" replace />} />
      <Route path="/session" element={<SessionPage />} />
      <Route path="/session/:sessionId" element={<SessionPage />} />
      <Route path="/settings" element={<SettingsPage />} />
    </Routes>
  )
}
