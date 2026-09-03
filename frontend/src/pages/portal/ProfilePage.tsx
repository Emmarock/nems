import { useEffect, useState } from 'react'
import { meApi } from '../../api/endpoints'
import type { MeDashboard } from '../../api/types'
import { LoadingState } from '../../components/LoadingState'
import { StatusBadge } from '../../components/StatusBadge'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { QrCodeModal } from '../../components/QrCodeModal'
import { CarIcon } from '../../components/icons'
import { apiErrorMessage } from '../../api/client'
import { buildScanUrl } from '../../utils/scanUrl'

const PROFILE_FIELDS: FieldConfig[] = [
  { name: 'fullName', label: 'Full name', required: true, full: true },
  { name: 'phone', label: 'Phone', required: true },
  { name: 'email', label: 'Email' },
  { name: 'emergencyContact', label: 'Emergency contact', full: true },
]

const PROPERTY_FIELDS: FieldConfig[] = [
  { name: 'houseNumber', label: 'House number', required: true },
  { name: 'address', label: 'Address', full: true },
]

const VEHICLE_FIELDS: FieldConfig[] = [
  { name: 'plateNumber', label: 'Plate number', required: true },
  { name: 'vehicleType', label: 'Vehicle type' },
  { name: 'make', label: 'Make' },
  { name: 'model', label: 'Model' },
  { name: 'colour', label: 'Colour' },
]

/** Resident self-service: personal details, house number, and vehicle registration (spec §6). */
export function ProfilePage() {
  const [dashboard, setDashboard] = useState<MeDashboard | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [editingProfile, setEditingProfile] = useState(false)
  const [editingProperty, setEditingProperty] = useState(false)
  const [addingVehicle, setAddingVehicle] = useState(false)
  const [vehicleQr, setVehicleQr] = useState<{ plate: string; token: string } | null>(null)

  async function load() {
    setDashboard(await meApi.dashboard())
  }

  useEffect(() => {
    load()
  }, [])

  async function handleProfileSubmit(values: Record<string, unknown>) {
    await meApi.updateProfile({
      fullName: values.fullName as string,
      phone: values.phone as string,
      email: (values.email as string) || undefined,
      emergencyContact: (values.emergencyContact as string) || undefined,
    })
    setEditingProfile(false)
    await load()
  }

  async function handlePropertySubmit(values: Record<string, unknown>) {
    await meApi.updateProperty({
      houseNumber: values.houseNumber as string,
      address: (values.address as string) || undefined,
    })
    setEditingProperty(false)
    await load()
  }

  async function handleVehicleSubmit(values: Record<string, unknown>) {
    await meApi.registerVehicle({
      plateNumber: (values.plateNumber as string).toUpperCase(),
      vehicleType: (values.vehicleType as string) || null,
      make: (values.make as string) || null,
      model: (values.model as string) || null,
      colour: (values.colour as string) || null,
    })
    setAddingVehicle(false)
    await load()
  }

  async function viewVehicleQr(id: number, plate: string) {
    setError(null)
    try {
      const token = await meApi.vehicleAccessPass(id)
      setVehicleQr({ plate, token })
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  if (!dashboard) {
    return <LoadingState />
  }

  const { resident, property } = dashboard

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My Profile</h1>
          <p className="page-subtitle">Your personal details, house number, and registered vehicles (spec §6).</p>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="page-header" style={{ marginBottom: 0 }}>
          <div className="section-title" style={{ marginTop: 0 }}>Personal details</div>
          <button className="btn btn-sm" onClick={() => setEditingProfile(true)}>
            Edit
          </button>
        </div>
        <dl className="detail-list">
          <div className="detail-row">
            <dt>Full name</dt>
            <dd>{resident.fullName}</dd>
          </div>
          <div className="detail-row">
            <dt>Phone</dt>
            <dd>{resident.phone}</dd>
          </div>
          <div className="detail-row">
            <dt>Email</dt>
            <dd>{resident.email ?? <span className="muted">Not set</span>}</dd>
          </div>
          <div className="detail-row">
            <dt>Emergency contact</dt>
            <dd>{resident.emergencyContact ?? <span className="muted">Not set</span>}</dd>
          </div>
          <div className="detail-row">
            <dt>Resident type</dt>
            <dd><StatusBadge value={resident.residentType} /></dd>
          </div>
          <div className="detail-row">
            <dt>Status</dt>
            <dd><StatusBadge value={resident.status} /></dd>
          </div>
          <div className="detail-row">
            <dt>Registered</dt>
            <dd>{resident.registrationDate}</dd>
          </div>
        </dl>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="page-header" style={{ marginBottom: 0 }}>
          <div className="section-title" style={{ marginTop: 0 }}>Property</div>
          {property && (
            <button className="btn btn-sm" onClick={() => setEditingProperty(true)}>
              Update house number
            </button>
          )}
        </div>
        {property ? (
          <dl className="detail-list">
            <div className="detail-row">
              <dt>House number</dt>
              <dd>{property.houseNumber}</dd>
            </div>
            <div className="detail-row">
              <dt>Block / Plot</dt>
              <dd>{property.block}, {property.plot}</dd>
            </div>
            <div className="detail-row">
              <dt>Address</dt>
              <dd>{property.address}</dd>
            </div>
            <div className="detail-row">
              <dt>Occupancy</dt>
              <dd><StatusBadge value={property.occupancyStatus} /></dd>
            </div>
          </dl>
        ) : (
          <p className="muted">No property on file yet — contact an admin to get one linked to your account.</p>
        )}
      </div>

      <div className="card">
        <div className="page-header" style={{ marginBottom: 0 }}>
          <div className="section-title" style={{ marginTop: 0 }}>Vehicles</div>
          <button className="btn btn-sm btn-primary" onClick={() => setAddingVehicle(true)}>
            + Register vehicle
          </button>
        </div>
        {dashboard.vehicles.length === 0 ? (
          <p className="muted">No vehicles registered yet.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {dashboard.vehicles.map((v) => (
              <div className="media-row" key={v.id}>
                <div className="media-icon media-icon-teal">
                  <CarIcon />
                </div>
                <div style={{ flex: 1 }}>
                  <div className="media-row-title">{v.plateNumber}</div>
                  <div className="muted">
                    {[v.make, v.model, v.colour].filter(Boolean).join(' · ') || v.vehicleType || '—'}
                  </div>
                </div>
                <StatusBadge value={v.status} />
                <button className="btn btn-sm" onClick={() => viewVehicleQr(v.id, v.plateNumber)}>
                  View QR
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {editingProfile && (
        <FormModal
          title="Edit profile"
          fields={PROFILE_FIELDS}
          initial={resident}
          onSubmit={handleProfileSubmit}
          onClose={() => setEditingProfile(false)}
        />
      )}

      {editingProperty && property && (
        <FormModal
          title="Update house number"
          fields={PROPERTY_FIELDS}
          initial={property}
          onSubmit={handlePropertySubmit}
          onClose={() => setEditingProperty(false)}
        />
      )}

      {addingVehicle && (
        <FormModal
          title="Register vehicle"
          fields={VEHICLE_FIELDS}
          onSubmit={handleVehicleSubmit}
          onClose={() => setAddingVehicle(false)}
        />
      )}

      {vehicleQr && (
        <QrCodeModal
          title="Vehicle QR code"
          subtitle={vehicleQr.plate}
          value={buildScanUrl('vehicle', vehicleQr.token)}
          fileName={`vehicle-${vehicleQr.plate}`}
          helpText="Present this QR code at the gate. Security will scan it to identify your vehicle before granting access."
          onClose={() => setVehicleQr(null)}
        />
      )}
    </div>
  )
}
