import { useState, type FormEvent } from 'react'
import { vehiclesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Resident } from '../api/types'
import { ResidentPicker } from './ResidentPicker'

/** Admin-side vehicle registration - every vehicle belongs to exactly one resident (who can
 * have several), so picking that resident by name replaces the old raw-numeric-id field (the
 * same footgun that let a resident-less user account slip through, see NewUserModal). */
export function RegisterVehicleModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => Promise<void> }) {
  const [plateNumber, setPlateNumber] = useState('')
  const [vehicleType, setVehicleType] = useState('')
  const [make, setMake] = useState('')
  const [model, setModel] = useState('')
  const [colour, setColour] = useState('')
  const [resident, setResident] = useState<Resident | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!resident) {
      setError('Search for and select the resident this vehicle belongs to.')
      return
    }
    setSubmitting(true)
    try {
      await vehiclesApi.create({
        plateNumber: plateNumber.toUpperCase(),
        residentId: resident.id,
        vehicleType: vehicleType || null,
        make: make || null,
        model: model || null,
        colour: colour || null,
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
            <h2>Register vehicle</h2>
            <button type="button" className="btn btn-sm" onClick={onClose}>
              ✕
            </button>
          </div>
          <div className="modal-body">
            {error && <p className="error-text">{error}</p>}
            <div className="form-grid">
              <div className="form-field full">
                <label>Resident</label>
                <ResidentPicker value={resident} onChange={setResident} required />
              </div>
              <div className="form-field">
                <label htmlFor="plateNumber">Plate number</label>
                <input id="plateNumber" value={plateNumber} onChange={(e) => setPlateNumber(e.target.value)} required />
              </div>
              <div className="form-field">
                <label htmlFor="vehicleType">Vehicle type</label>
                <input id="vehicleType" value={vehicleType} onChange={(e) => setVehicleType(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="make">Make</label>
                <input id="make" value={make} onChange={(e) => setMake(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="model">Model</label>
                <input id="model" value={model} onChange={(e) => setModel(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="colour">Colour</label>
                <input id="colour" value={colour} onChange={(e) => setColour(e.target.value)} />
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Registering…' : 'Register vehicle'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
