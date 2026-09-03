import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { authApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'

/**
 * Blocks the app entirely (see ProtectedRoute) until a bulk-created or admin-reset account sets
 * its own password. Reuses the existing self-service change-password endpoint - the "current
 * password" here is just whatever temporary one they logged in with.
 */
export function ForcePasswordChangePage() {
  const { user, markPasswordChanged } = useAuth()
  const navigate = useNavigate()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters.')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.')
      return
    }
    setLoading(true)
    try {
      await authApi.changePassword(currentPassword, newPassword)
      markPasswordChanged()
      navigate('/')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>Set a new password</h1>
        <p>
          {user ? `Welcome, ${user.fullName}. ` : ''}
          Your account was created with a temporary password. Choose your own password to continue.
        </p>
        <form onSubmit={handleSubmit}>
          <div className="form-field" style={{ marginBottom: 14 }}>
            <label htmlFor="currentPassword">Temporary password</label>
            <input
              id="currentPassword"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
            />
          </div>
          <div className="form-field" style={{ marginBottom: 14 }}>
            <label htmlFor="newPassword">New password</label>
            <input
              id="newPassword"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={8}
              required
            />
          </div>
          <div className="form-field" style={{ marginBottom: 8 }}>
            <label htmlFor="confirmPassword">Confirm new password</label>
            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              minLength={8}
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: 12 }} disabled={loading}>
            {loading ? 'Saving…' : 'Set password and continue'}
          </button>
        </form>
      </div>
    </div>
  )
}
