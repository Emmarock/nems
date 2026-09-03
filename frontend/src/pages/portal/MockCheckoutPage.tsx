import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { paymentsApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'

/**
 * Stands in for a real Paystack/Flutterwave/Monnify checkout page (spec §5). The
 * MockPaymentProvider redirects here; "paying" simply calls our own /payments/webhook,
 * exercising the full Resident -> Gateway -> Webhook -> NEMS -> Account Updated flow.
 */
export function MockCheckoutPage() {
  const [params] = useSearchParams()
  const ref = params.get('ref') ?? ''
  const amount = params.get('amount') ?? '0'
  const [status, setStatus] = useState<'idle' | 'processing' | 'done' | 'error'>('idle')
  const [error, setError] = useState<string | null>(null)

  async function pay(outcome: 'SUCCESS' | 'FAILED') {
    setStatus('processing')
    setError(null)
    try {
      await paymentsApi.webhook(ref, outcome)
      setStatus('done')
    } catch (err) {
      setError(apiErrorMessage(err))
      setStatus('error')
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>Mock Payment Gateway</h1>
        <p>Simulated Paystack/Flutterwave/Monnify checkout — no real charge is made.</p>
        <div className="card" style={{ marginBottom: 16 }}>
          <p style={{ margin: '4px 0' }}>
            <strong>Amount:</strong> ₦{Number(amount).toLocaleString()}
          </p>
          <p style={{ margin: '4px 0' }} className="muted">
            Reference: {ref}
          </p>
        </div>

        {status === 'done' ? (
          <p>Payment processed. You can close this tab and return to the portal.</p>
        ) : (
          <>
            {error && <p className="error-text">{error}</p>}
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-primary" style={{ flex: 1 }} onClick={() => pay('SUCCESS')} disabled={status === 'processing'}>
                {status === 'processing' ? 'Processing…' : 'Simulate success'}
              </button>
              <button className="btn btn-danger" style={{ flex: 1 }} onClick={() => pay('FAILED')} disabled={status === 'processing'}>
                Simulate failure
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
