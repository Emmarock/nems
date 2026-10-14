import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import { Footer } from '../../components/Footer'

type Step = 'phone' | 'code' | 'done'

/**
 * Public, unauthenticated entry point for a resident who already exists in the system but never
 * got (or lost) their login. Two steps: a WhatsApp + SMS code to the phone already on file proves
 * the caller actually controls that phone (not just that they know the number), then the
 * verified code unlocks sending a reset-password link to whatever email they supply. Both steps
 * report a real error back — an unrecognized phone in step 1, a wrong/expired code in step 2 —
 * see UserService.startClaimAccount/verifyClaimAccount.
 */
export function ClaimAccountPage() {
  const [step, setStep] = useState<Step>('phone')
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleStart(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await authApi.claimAccountStart(phone.trim())
      setStep('code')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  async function handleVerify(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await authApi.claimAccountVerify(phone.trim(), code.trim(), email.trim())
      setStep('done')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (step === 'done') {
    return (
      <div className="login-page">
        <div className="login-card">
          <h1>Check your email</h1>
          <p>We've sent a link to the email address you provided with instructions to set up your access.</p>
          <Link to="/login" className="btn btn-primary" style={{ width: '100%', marginTop: 12, display: 'block', textAlign: 'center' }}>
            Back to login
          </Link>
        </div>
        <Footer />
      </div>
    )
  }

  if (step === 'code') {
    return (
      <div className="login-page">
        <div className="login-card">
          <h1>Enter your code</h1>
          <p>We've sent a 6-digit code to the WhatsApp number on file for that phone number. Enter it below along with the email you'd like your access link sent to.</p>
          <form onSubmit={handleVerify}>
            <div className="form-field" style={{ marginBottom: 14 }}>
              <label htmlFor="code">WhatsApp code</label>
              <input
                id="code"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="123456"
                required
              />
            </div>
            <div className="form-field" style={{ marginBottom: 8 }}>
              <label htmlFor="email">Email address</label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Where we should send your access link"
                required
              />
            </div>
            {error && <p className="error-text">{error}</p>}
            <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: 12 }} disabled={loading}>
              {loading ? 'Verifying…' : 'Verify & send access link'}
            </button>
          </form>
          <p className="muted" style={{ textAlign: 'center', fontSize: 13, marginTop: 16, marginBottom: 0 }}>
            Didn't get a code?{' '}
            <button
              type="button"
              className="link-button"
              style={{ background: 'none', border: 'none', padding: 0, color: 'inherit', textDecoration: 'underline', cursor: 'pointer' }}
              onClick={() => { setStep('phone'); setError(null) }}
            >
              Try a different number
            </button>
          </p>
        </div>
        <Footer />
      </div>
    )
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>Claim your account</h1>
        <p>Already a resident but don't have a login yet? Enter your phone number and we'll send a verification code to your WhatsApp.</p>
        <form onSubmit={handleStart}>
          <div className="form-field" style={{ marginBottom: 8 }}>
            <label htmlFor="phone">Phone number on file</label>
            <input
              id="phone"
              type="tel"
              autoComplete="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="e.g. 08023797036"
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: 12 }} disabled={loading}>
            {loading ? 'Sending…' : 'Send me a WhatsApp code'}
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
