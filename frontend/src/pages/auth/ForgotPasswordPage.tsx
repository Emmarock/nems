import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import { Footer } from '../../components/Footer'

/**
 * For anyone who already has a login (resident or staff) but can't remember the password —
 * distinct from ClaimAccountPage, which is for a resident who's never had a login at all. The
 * identifier can be an email or phone (same as login), but the reset link itself only ever goes
 * to the email on file — see UserService.startForgotPassword. Always shows the same "check your
 * email" confirmation regardless of whether the identifier actually matched an account, so this
 * page can't be used to find out which emails/phones have logins here.
 */
export function ForgotPasswordPage() {
  const [identifier, setIdentifier] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await authApi.forgotPassword(identifier.trim())
      setSent(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (sent) {
    return (
      <div className="login-page">
        <div className="login-card">
          <h1>Check your email</h1>
          <p>
            If that email or phone number has a Nitel Estate login with an email on file, we've sent a link to
            the email address to set a new password. It expires in 24 hours.
          </p>
          <Link to="/login" className="btn btn-primary" style={{ width: '100%', marginTop: 12, display: 'block', textAlign: 'center' }}>
            Back to login
          </Link>
        </div>
        <Footer />
      </div>
    )
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>Forgot your password?</h1>
        <p>Enter the email or phone number you log in with, and we'll send you a link to set a new password.</p>
        <form onSubmit={handleSubmit}>
          <div className="form-field" style={{ marginBottom: 8 }}>
            <label htmlFor="identifier">Email or phone number</label>
            <input
              id="identifier"
              type="text"
              autoComplete="username"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: 12 }} disabled={loading}>
            {loading ? 'Sending…' : 'Send reset link'}
          </button>
        </form>
        <p className="muted" style={{ textAlign: 'center', fontSize: 13, marginTop: 16, marginBottom: 0 }}>
          <Link to="/login">Back to login</Link>
        </p>
      </div>
      <Footer />
    </div>
  )
}
