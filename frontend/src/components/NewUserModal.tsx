import { useState, type FormEvent } from 'react'
import { usersApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Resident, Role } from '../api/types'
import { ResidentPicker } from './ResidentPicker'

const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'SUPER_ADMIN', label: 'Super Admin' },
  { value: 'CDA_ADMIN', label: 'CDA Administrator' },
  { value: 'TREASURER', label: 'Treasurer' },
  { value: 'SECRETARY', label: 'Secretary' },
  { value: 'SECURITY', label: 'Security' },
  { value: 'MAINTENANCE', label: 'Maintenance' },
  { value: 'RESIDENT', label: 'Resident' },
]

/**
 * A resident-role account with no resident to link to is a real bug we hit: it 400s the moment
 * that resident tries to log in (MeController can't resolve a residentId). Picking the resident
 * by name here - rather than typing a raw numeric id - makes that mistake structurally hard to
 * make, and pre-fills name/phone from the resident record so there's less to retype.
 */
export function NewUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => Promise<void> }) {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role>('RESIDENT')
  const [resident, setResident] = useState<Resident | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function handleRoleChange(next: Role) {
    setRole(next)
    if (next !== 'RESIDENT') {
      setResident(null)
    }
  }

  function handleResidentChange(next: Resident | null) {
    setResident(next)
    if (next) {
      setFullName(next.fullName)
      if (next.phone && next.phone !== 'UNKNOWN') {
        setPhone(next.phone)
      }
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (role === 'RESIDENT' && !resident) {
      setError('Search for and select the resident this account belongs to.')
      return
    }
    setSubmitting(true)
    try {
      await usersApi.create({
        email,
        phone: phone || undefined,
        password,
        fullName,
        role,
        residentId: resident?.id,
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
            <h2>New user</h2>
            <button type="button" className="btn btn-sm" onClick={onClose}>
              ✕
            </button>
          </div>
          <div className="modal-body">
            {error && <p className="error-text">{error}</p>}
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="role">Role</label>
                <select id="role" value={role} onChange={(e) => handleRoleChange(e.target.value as Role)} required>
                  {ROLE_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>

              {role === 'RESIDENT' && (
                <div className="form-field full">
                  <label>Resident</label>
                  <ResidentPicker value={resident} onChange={handleResidentChange} required />
                </div>
              )}

              <div className="form-field full">
                <label htmlFor="fullName">Full name</label>
                <input id="fullName" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
              </div>
              <div className="form-field">
                <label htmlFor="newUserEmail">Email</label>
                <input id="newUserEmail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </div>
              <div className="form-field">
                <label htmlFor="newUserPhone">Phone (optional, alternative login)</label>
                <input id="newUserPhone" value={phone} onChange={(e) => setPhone(e.target.value)} />
              </div>
              <div className="form-field full">
                <label htmlFor="newUserPassword">Password</label>
                <input
                  id="newUserPassword"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={8}
                />
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create user'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
