import { useEffect, useState, type FormEvent } from 'react'
import { accessPolicyApi } from '../../api/endpoints'
import type { AccessPolicy } from '../../api/types'
import { apiErrorMessage } from '../../api/client'
import { LoadingState } from '../../components/LoadingState'

export function AccessPolicyPage() {
  const [policy, setPolicy] = useState<AccessPolicy | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    accessPolicyApi.get().then(setPolicy)
  }, [])

  async function save(e: FormEvent) {
    e.preventDefault()
    if (!policy) return
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      const updated = await accessPolicyApi.update(policy)
      setPolicy(updated)
      setSaved(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  if (!policy) {
    return <LoadingState />
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Access Policy</h1>
          <p className="page-subtitle">
            Configurable settings, not a rule engine (spec Phase 3 §3) — flags residents whose outstanding balance
            exceeds the threshold at the gate.
          </p>
        </div>
      </div>

      <form className="card" style={{ maxWidth: 420 }} onSubmit={save}>
        <div className="form-field checkbox-field" style={{ marginBottom: 14 }}>
          <input
            type="checkbox"
            id="enforceArrears"
            checked={policy.enforceArrears}
            onChange={(e) => setPolicy({ ...policy, enforceArrears: e.target.checked })}
          />
          <label htmlFor="enforceArrears">Enforce arrears policy</label>
        </div>
        <div className="form-field" style={{ marginBottom: 14 }}>
          <label htmlFor="threshold">Arrears threshold (₦)</label>
          <input
            id="threshold"
            type="number"
            step="0.01"
            value={policy.arrearsThreshold}
            onChange={(e) => setPolicy({ ...policy, arrearsThreshold: e.target.valueAsNumber })}
          />
        </div>
        {error && <p className="error-text">{error}</p>}
        {saved && <p style={{ color: 'var(--color-success)' }}>Saved.</p>}
        <button className="btn btn-primary" type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Save policy'}
        </button>
      </form>
    </div>
  )
}
