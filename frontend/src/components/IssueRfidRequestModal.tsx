import { useState, type FormEvent } from 'react'
import { rfidRequestsApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { RfidRequest } from '../api/types'

/** Fulfils one pending RfidRequest at the gate: security hands over a physical tag, types its
 * id here, and that both creates the real RfidTag and marks the request issued. */
export function IssueRfidRequestModal({
  request,
  onClose,
  onIssued,
}: {
  request: RfidRequest
  onClose: () => void
  onIssued: () => Promise<void>
}) {
  const [tagId, setTagId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await rfidRequestsApi.issue(request.id, tagId.trim())
      await onIssued()
      onClose()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <h2>Issue RFID tag</h2>
            <button type="button" className="btn btn-sm" onClick={onClose}>
              ✕
            </button>
          </div>
          <div className="modal-body">
            <p className="muted" style={{ marginTop: 0 }}>
              {request.subjectType === 'VISITOR' ? 'Visitor' : 'Worker'}: {request.subjectName}
              {request.subjectPhone && ` · ${request.subjectPhone}`}
              <br />
              Requested by {request.requestedByResidentName}
            </p>
            {error && <p className="error-text">{error}</p>}
            <div className="form-field full">
              <label htmlFor="rfidRequestTagId">Physical tag ID</label>
              <input
                id="rfidRequestTagId"
                value={tagId}
                onChange={(e) => setTagId(e.target.value)}
                placeholder="Scan or type the tag's ID"
                required
                autoFocus
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Issuing…' : 'Issue tag'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
