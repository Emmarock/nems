import { useState } from 'react'
import { createPortal } from 'react-dom'
import { rfidRequestsApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { RfidRequest, RfidRequestSubjectType } from '../api/types'

interface RfidRequestActionProps {
  subjectType: RfidRequestSubjectType
  subjectId: number
  subjectName: string
  /** This resident's own RFID requests, already loaded by the parent page — avoids a per-row fetch. */
  requests: RfidRequest[]
  onChanged: () => Promise<void>
}

/**
 * Lets a landlord/tenant request a physical RFID tag for one visitor or worker who has no
 * smartphone to show a QR pass — security sees it in their pending queue and issues a tag at the
 * gate (see pages/security/RfidPage). Shown as a row action next to "View entry QR"/"View QR
 * pass", one per visitor/worker, so it reflects whatever request already exists for that one
 * person rather than a separate page. Requesting goes through a short disclaimer first — the
 * resident is vouching for this person and taking responsibility for them while on the estate.
 */
export function RfidRequestAction({ subjectType, subjectId, subjectName, requests, onChanged }: RfidRequestActionProps) {
  const [confirming, setConfirming] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const issued = requests.find((r) => r.subjectType === subjectType && r.subjectId === subjectId && r.status === 'ISSUED')
  const pending = requests.find((r) => r.subjectType === subjectType && r.subjectId === subjectId && r.status === 'PENDING')
  const noun = subjectType === 'VISITOR' ? 'visitor' : 'worker'

  async function confirmRequest() {
    setSubmitting(true)
    setError(null)
    try {
      await rfidRequestsApi.create({ subjectType, subjectId })
      await onChanged()
      setConfirming(false)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  async function cancel() {
    if (!pending) return
    setSubmitting(true)
    setError(null)
    try {
      await rfidRequestsApi.cancel(pending.id)
      await onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (issued) {
    return (
      <span className="muted" style={{ fontSize: 12, alignSelf: 'center' }}>
        RFID issued ({issued.issuedTagTagId})
      </span>
    )
  }

  if (pending) {
    return (
      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span className="muted" style={{ fontSize: 12 }}>
          RFID requested
        </span>
        {error && <span className="error-text" style={{ fontSize: 12 }}>{error}</span>}
        <button className="btn btn-sm" disabled={submitting} onClick={cancel}>
          Cancel RFID request
        </button>
      </span>
    )
  }

  return (
    <>
      <button className="btn btn-sm" onClick={() => setConfirming(true)}>
        Request RFID
      </button>

      {confirming &&
        createPortal(
          <div className="modal-overlay" onClick={() => !submitting && setConfirming(false)}>
            <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 420 }}>
              <div className="modal-header">
                <h2>Request RFID tag</h2>
                <button type="button" className="btn btn-sm" onClick={() => setConfirming(false)} disabled={submitting}>
                  ✕
                </button>
              </div>
              <div className="modal-body">
                <p style={{ marginTop: 0 }}>
                  This is a makeshift option for a {noun} who has no smartphone to show a QR pass. For regular
                  access, a {noun} should use their own entry and exit QR codes instead — an RFID tag is a physical
                  fallback, not the normal way in or out.
                </p>
                <p className="muted" style={{ fontSize: 13 }}>
                  By requesting this tag, you take full responsibility for <strong>{subjectName}</strong> while they
                  are on the estate.
                </p>
                {error && <p className="error-text">{error}</p>}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn" onClick={() => setConfirming(false)} disabled={submitting}>
                  Cancel
                </button>
                <button type="button" className="btn btn-primary" onClick={confirmRequest} disabled={submitting}>
                  {submitting ? 'Requesting…' : 'Request RFID tag'}
                </button>
              </div>
            </div>
          </div>,
          document.body,
        )}
    </>
  )
}
