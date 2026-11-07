import { useEffect, useState, type FormEvent } from 'react'
import { officeProfileApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { OfficeProfile } from '../api/types'
import { LoadingState } from './LoadingState'

/**
 * Staff self-service: set the personal email that notifications for their office (e.g. a new
 * payment receipt to review) are sent to, instead of the shared office login email.
 */
export function OfficeProfileModal({ onClose }: { onClose: () => void }) {
  const [profile, setProfile] = useState<OfficeProfile | null>(null)
  const [personalEmail, setPersonalEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    officeProfileApi
      .get()
      .then((p) => {
        setProfile(p)
        setPersonalEmail(p.personalEmail ?? '')
      })
      .catch((err) => setError(apiErrorMessage(err)))
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      setProfile(await officeProfileApi.update(personalEmail.trim() || null))
      setSaved(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <h2>Office profile</h2>
            <button type="button" className="btn btn-sm" onClick={onClose}>
              ✕
            </button>
          </div>
          <div className="modal-body">
            {error && <p className="error-text">{error}</p>}
            {saved && <p className="success-text">Saved. Notifications for your office will go to this address.</p>}
            {!profile && !error ? (
              <LoadingState />
            ) : (
              profile && (
                <>
                  <div className="detail-list">
                    <div className="detail-row">
                      <dt>Name</dt>
                      <dd>{profile.fullName}</dd>
                    </div>
                    <div className="detail-row">
                      <dt>Office</dt>
                      <dd>{profile.role.replace(/_/g, ' ')}</dd>
                    </div>
                    <div className="detail-row">
                      <dt>Login email</dt>
                      <dd>{profile.email}</dd>
                    </div>
                  </div>
                  <div className="form-field full" style={{ marginTop: 16 }}>
                    <label htmlFor="personalEmail">Personal email for notifications</label>
                    <input
                      id="personalEmail"
                      type="email"
                      value={personalEmail}
                      onChange={(e) => setPersonalEmail(e.target.value)}
                      placeholder="you@example.com"
                    />
                    <p className="muted" style={{ fontSize: 12, margin: '4px 0 0' }}>
                      Tasks for your office, such as new payment receipts to review, are emailed here. If you leave
                      it blank, they go to the email of the resident the admin assigned to your office, or else to your
                      login email.
                    </p>
                  </div>
                </>
              )
            )}
          </div>
          <div className="modal-footer">
            <button type="button" className="btn" onClick={onClose}>
              Close
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving || !profile}>
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
