import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/endpoints'
import { clearToken, setToken } from '../api/client'
import type { Role } from '../api/types'

interface AuthUser {
  email: string
  fullName: string
  role: Role
  residentId: number | null
  mustChangePassword: boolean
}

interface AuthContextValue {
  user: AuthUser | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  /** Called after a forced first-login password change succeeds, so ProtectedRoute stops
   * redirecting to /change-password without requiring a fresh login. */
  markPasswordChanged: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

const USER_KEY = 'nems.user'

function loadStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser)

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      login: async (email, password) => {
        const response = await authApi.login(email, password)
        setToken(response.token)
        const authUser: AuthUser = {
          email: response.email,
          fullName: response.fullName,
          role: response.role,
          residentId: response.residentId,
          mustChangePassword: response.mustChangePassword,
        }
        localStorage.setItem(USER_KEY, JSON.stringify(authUser))
        setUser(authUser)
      },
      logout: () => {
        clearToken()
        localStorage.removeItem(USER_KEY)
        setUser(null)
      },
      markPasswordChanged: () => {
        setUser((prev) => {
          if (!prev) return prev
          const next = { ...prev, mustChangePassword: false }
          localStorage.setItem(USER_KEY, JSON.stringify(next))
          return next
        })
      },
    }),
    [user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
