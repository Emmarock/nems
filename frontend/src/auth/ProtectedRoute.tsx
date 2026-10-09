import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { isLandlord, useAuth } from './AuthContext'
import type { Role } from '../api/types'
import { getToken } from '../api/client'

const FORCE_PASSWORD_CHANGE_PATH = '/change-password'

/**
 * landlordOnly gates a page a tenant should never land on even via a direct URL (e.g.
 * /portal/tenants) — the hidden nav item alone isn't enough, since the backend's own ownership
 * check (see ResidentService.requireOwnedProperty) is the real authority, but the UI should
 * redirect before even trying rather than showing an empty/error page.
 */
export function ProtectedRoute({
  roles,
  landlordOnly,
  children,
}: {
  roles?: Role[]
  landlordOnly?: boolean
  children: ReactNode
}) {
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
  if (landlordOnly && !isLandlord(user)) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
