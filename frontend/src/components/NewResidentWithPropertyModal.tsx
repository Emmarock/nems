import { useState, type FormEvent } from 'react'
import { residentsApi } from '../api/endpoints'
import type { Property, Resident, ResidentWithPropertyResult } from '../api/types'
import { apiErrorMessage } from '../api/client'

// Tenant isn't offered here - a tenant doesn't get their own property (they occupy their
// landlord's), so they're added via the landlord's own "add tenant" self-service flow instead,
// never through this admin resident+property form. See ResidentService.createTenant.
const RESIDENT_TYPE_OPTIONS: { value: Resident['residentType']; label: string }[] = [
  { value: 'OWNER', label: 'Owner' },
  { value: 'LANDLORD', label: 'Landlord' },
]

const PROPERTY_TYPE_OPTIONS: { value: Property['propertyType']; label: string }[] = [
  { value: 'DETACHED_HOUSE', label: 'Detached house' },
  { value: 'SEMI_DETACHED', label: 'Semi-detached' },
  { value: 'TERRACE', label: 'Terrace' },
  { value: 'BUNGALOW', label: 'Bungalow' },
  { value: 'APARTMENT', label: 'Apartment' },
  { value: 'VACANT_LAND', label: 'Vacant land' },
]

const OCCUPANCY_OPTIONS: { value: Property['occupancyStatus']; label: string }[] = [
  { value: 'OCCUPIED', label: 'Occupied' },
  { value: 'VACANT', label: 'Vacant' },
  { value: 'UNDER_CONSTRUCTION', label: 'Under construction' },
]

/**
 * Creates a resident and a brand-new property together, already linked both ways - the default
 * way to add a resident, since most don't have a property on file yet. See
 * ResidentService.createWithProperty: the new resident (Owner or Landlord) becomes the property's
 * owner. Tenants aren't created here - see RESIDENT_TYPE_OPTIONS above.
 */
export function NewResidentWithPropertyModal({
  onClose,
  onCreated,
}: {
  onClose: () => void
  onCreated: (result: ResidentWithPropertyResult) => Promise<void>
}) {
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [residentType, setResidentType] = useState<Resident['residentType']>('OWNER')
  const [emergencyContact, setEmergencyContact] = useState('')

  const [road, setRoad] = useState('')
  const [houseNumber, setHouseNumber] = useState('')
  const [address, setAddress] = useState('')
  const [postcode, setPostcode] = useState('')
  const [propertyType, setPropertyType] = useState<Property['propertyType']>('DETACHED_HOUSE')
  const [occupancyStatus, setOccupancyStatus] = useState<Property['occupancyStatus']>('VACANT')
  const [apartmentCount, setApartmentCount] = useState('')

  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const result = await residentsApi.createWithProperty({
        fullName,
        phone,
        email: email || undefined,
        residentType,
        emergencyContact: emergencyContact || undefined,
        road,
        houseNumber,
        address,
        postcode: postcode || undefined,
        propertyType,
        occupancyStatus,
        apartmentCount: apartmentCount ? Number(apartmentCount) : undefined,
      })
      await onCreated(result)
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
            <h2>New resident</h2>
            <button type="button" className="btn btn-sm" onClick={onClose}>
              ✕
            </button>
          </div>
          <div className="modal-body">
            {error && <p className="error-text">{error}</p>}

            <div className="section-title" style={{ marginTop: 0 }}>Resident details</div>
            <div className="form-grid">
              <div className="form-field full">
                <label htmlFor="rwp-fullName">Full name</label>
                <input id="rwp-fullName" required value={fullName} onChange={(e) => setFullName(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="rwp-phone">Phone</label>
                <input id="rwp-phone" required value={phone} onChange={(e) => setPhone(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="rwp-email">Email</label>
                <input id="rwp-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="rwp-residentType">Resident type</label>
                <select
                  id="rwp-residentType"
                  value={residentType}
                  onChange={(e) => setResidentType(e.target.value as Resident['residentType'])}
                >
                  {RESIDENT_TYPE_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-field full">
                <label htmlFor="rwp-emergencyContact">Emergency contact</label>
                <input id="rwp-emergencyContact" value={emergencyContact} onChange={(e) => setEmergencyContact(e.target.value)} />
              </div>
            </div>
            <p className="muted" style={{ fontSize: 12, margin: '6px 0 0' }}>
              They'll be set as the new property's owner, and linked to it as their own address.
            </p>

            <div className="section-title">Property details</div>
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="rwp-road">Road</label>
                <input id="rwp-road" required value={road} onChange={(e) => setRoad(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="rwp-houseNumber">House number</label>
                <input id="rwp-houseNumber" required value={houseNumber} onChange={(e) => setHouseNumber(e.target.value)} />
              </div>
              <div className="form-field full">
                <label htmlFor="rwp-address">Address</label>
                <input id="rwp-address" required value={address} onChange={(e) => setAddress(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="rwp-postcode">Postcode</label>
                <input id="rwp-postcode" value={postcode} onChange={(e) => setPostcode(e.target.value)} />
              </div>
              <div className="form-field">
                <label htmlFor="rwp-propertyType">Property type</label>
                <select
                  id="rwp-propertyType"
                  value={propertyType}
                  onChange={(e) => setPropertyType(e.target.value as Property['propertyType'])}
                >
                  {PROPERTY_TYPE_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="rwp-occupancyStatus">Occupancy status</label>
                <select
                  id="rwp-occupancyStatus"
                  value={occupancyStatus}
                  onChange={(e) => setOccupancyStatus(e.target.value as Property['occupancyStatus'])}
                >
                  {OCCUPANCY_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="rwp-apartmentCount">Apartment count (caps active tenants)</label>
                <input
                  id="rwp-apartmentCount"
                  type="number"
                  min={1}
                  value={apartmentCount}
                  onChange={(e) => setApartmentCount(e.target.value)}
                />
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create both'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
