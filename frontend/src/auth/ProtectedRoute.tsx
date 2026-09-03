import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'
import type { Role } from '../api/types'
import { getToken } from '../api/client'

const FORCE_PASSWORD_CHANGE_PATH = '/change-password'

export function ProtectedRoute({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!getToken() || !user) {
    return <Navigate to="/login" replace />
  }
  if (user.mustChangePassword && location.pathname !== FORCE_PASSWORD_CHANGE_PATH) {
    return <Navigate to={FORCE_PASSWORD_CHANGE_PATH} replace />
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
