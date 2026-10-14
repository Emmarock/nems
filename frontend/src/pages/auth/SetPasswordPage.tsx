import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import { Footer } from '../../components/Footer'

/**
 * Lands here from the one-time link in an onboarding email (?token=...) — unauthenticated, the
 * token itself proves who's setting the password (see UserService.resetPasswordWithToken).
 * Unlike ForcePasswordChangePage, there's no "current password" field: the whole point of this
 * link is skipping that step.
 */
export function SetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const navigate = useNavigate()
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!token) {
      setError('This link is missing its token — ask for a new one.')
      return
    }
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
      await authApi.resetPassword(token, newPassword)
      setDone(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (done) {
    return (
      <div className="login-page">
        <div className="login-card">
          <h1>Password set</h1>
          <p>Your password has been set. You can log in with it now.</p>
          <button type="button" className="btn btn-primary" style={{ width: '100%', marginTop: 12 }} onClick={() => navigate('/login')}>
            Go to login
          </button>
        </div>
        <Footer />
      </div>
    )
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>Set your password</h1>
        <p>Choose a password for your Nitel Estate account.</p>
        {!token && <p className="error-text">This link is missing its token — ask for a new one.</p>}
        <form onSubmit={handleSubmit}>
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
          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: 12 }} disabled={loading || !token}>
            {loading ? 'Saving…' : 'Set password'}
          </button>
        </form>
      </div>
      <Footer />
    </div>
  )
}
