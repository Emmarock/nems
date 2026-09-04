import { useState, type FormEvent } from 'react'
import { rfidApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Resident, Worker } from '../api/types'
import { ResidentPicker } from './ResidentPicker'
import { WorkerPicker } from './WorkerPicker'

/**
 * A tag can be handed to a resident directly, or to one of that resident's own workers (the same
 * sponsor relationship WorkerService already enforces) - so the worker picker only appears once a
 * resident is chosen, and only ever shows that resident's workers. Vehicle stays a raw id for now
 * (no vehicle picker exists yet elsewhere in the admin UI).
 */
export function IssueRfidModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => Promise<void> }) {
  const [tagId, setTagId] = useState('')
  const [resident, setResident] = useState<Resident | null>(null)
  const [worker, setWorker] = useState<Worker | null>(null)
  const [vehicleId, setVehicleId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function changeResident(next: Resident | null) {
    setResident(next)
    setWorker(null) // a worker only makes sense under the resident who sponsors them
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await rfidApi.issue({
        tagId: tagId.trim(),
        assignedResidentId: resident?.id,
        assignedWorkerId: worker?.id,
        vehicleId: vehicleId ? Number(vehicleId) : undefined,
      })
      await onCreated()
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
            {error && <p className="error-text">{error}</p>}
            <div className="form-grid">
              <div className="form-field full">
                <label htmlFor="tagId">Tag ID</label>
                <input id="tagId" value={tagId} onChange={(e) => setTagId(e.target.value)} required />
              </div>
              <div className="form-field full">
                <label>Resident (optional)</label>
                <ResidentPicker value={resident} onChange={changeResident} />
              </div>
              {resident && (
                <div className="form-field full">
                  <label>Worker (optional — one of {resident.fullName}'s workers)</label>
                  <WorkerPicker sponsorResidentId={resident.id} value={worker} onChange={setWorker} />
                </div>
              )}
              <div className="form-field">
                <label htmlFor="vehicleId">Vehicle ID (optional)</label>
                <input
                  id="vehicleId"
                  type="number"
                  value={vehicleId}
                  onChange={(e) => setVehicleId(e.target.value)}
                />
              </div>
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
