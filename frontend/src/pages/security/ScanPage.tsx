import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { propertiesApi, residentsApi, vehiclesApi, visitorsApi, workersApi } from '../../api/endpoints'
import type { PropertyLookup, ResidentLookup, VehicleLookup, VisitorLookup, WorkerLookup } from '../../api/types'
import { StatusBadge } from '../../components/StatusBadge'
import { BalanceBreakdown } from '../../components/BalanceBreakdown'
import { apiErrorMessage, authErrorStatus, redirectToLogin } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import type { ScanKind } from '../../utils/scanUrl'

const CAN_CHECK_IN_OUT = new Set(['SUPER_ADMIN', 'SECURITY'])

const TITLES: Record<ScanKind, string> = {
  visitor: 'Visitor',
  worker: 'Worker',
  resident: 'Resident',
  property: 'Building',
  vehicle: 'Vehicle',
}

/**
 * What opens when security scans a QR pass (via camera or a shared link).
 * Visitor/worker/resident/vehicle passes are about gate entry: identity + destination +
 * Check In/Out (spec §9 / Phase 2 §4 / Phase 3 §4-5). A building pass is about enforcement, not
 * entry: it shows the owning resident's payment history/outstanding balance, read-only, no
 * check-in/out.
 */
export function ScanPage() {
  const { kind, qrToken } = useParams<{ kind: ScanKind; qrToken: string }>()
  const { user } = useAuth()
  const canCheckInOut = !!user && CAN_CHECK_IN_OUT.has(user.role)
  const [visitor, setVisitor] = useState<VisitorLookup | null>(null)
  const [worker, setWorker] = useState<WorkerLookup | null>(null)
  const [resident, setResident] = useState<ResidentLookup | null>(null)
  const [property, setProperty] = useState<PropertyLookup | null>(null)
  const [vehicle, setVehicle] = useState<VehicleLookup | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [authErrorCode, setAuthErrorCode] = useState<401 | 403 | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function load() {
    if (
      !qrToken ||
      (kind !== 'visitor' && kind !== 'worker' && kind !== 'resident' && kind !== 'property' && kind !== 'vehicle')
    ) {
      setError('Invalid scan link')
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    setAuthErrorCode(null)
    try {
      if (kind === 'visitor') setVisitor(await visitorsApi.lookup(qrToken))
      else if (kind === 'worker') setWorker(await workersApi.lookup(qrToken))
      else if (kind === 'property') setProperty(await propertiesApi.lookup(qrToken))
      else if (kind === 'vehicle') setVehicle(await vehiclesApi.lookup(qrToken))
      else setResident(await residentsApi.lookup(qrToken))
    } catch (err) {
      const code = authErrorStatus(err)
      if (code) setAuthErrorCode(code)
      else setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [kind, qrToken])

  async function act(action: 'checkin' | 'checkout') {
    if (!qrToken) return
    setSubmitting(true)
    setMessage(null)
    setError(null)
    setAuthErrorCode(null)
    try {
      if (kind === 'visitor') {
        const result = action === 'checkin' ? await visitorsApi.checkIn(qrToken) : await visitorsApi.checkOut(qrToken)
        setMessage(`${action === 'checkin' ? 'Checked in' : 'Checked out'}: ${result.name}`)
      } else if (kind === 'worker') {
        const result = action === 'checkin' ? await workersApi.checkIn(qrToken) : await workersApi.checkOut(qrToken)
        setMessage(`${action === 'checkin' ? 'Checked in' : 'Checked out'}: ${result.fullName}`)
      } else if (kind === 'vehicle') {
        const result = action === 'checkin' ? await vehiclesApi.checkIn(qrToken) : await vehiclesApi.checkOut(qrToken)
        setMessage(`${action === 'checkin' ? 'Checked in' : 'Checked out'}: ${result.plateNumber}`)
      } else {
        const result = action === 'checkin' ? await residentsApi.checkIn(qrToken) : await residentsApi.checkOut(qrToken)
        setMessage(`${action === 'checkin' ? 'Checked in' : 'Checked out'}: ${result.fullName}`)
      }
      await load()
    } catch (err) {
      const code = authErrorStatus(err)
      if (code) setAuthErrorCode(code)
      else setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  const holder = kind === 'visitor' ? visitor : kind === 'worker' ? worker : resident

  return (
    <div className="scan-page">
      <div className="scan-card">
        <h1>{kind ? TITLES[kind] : ''} Pass</h1>

        {loading && <p className="muted">Looking up pass…</p>}

        {authErrorCode === 401 && (
          <div className="info-banner">
            <p style={{ margin: '0 0 10px' }}>
              Your session has expired, or you're not logged in. Please log in and scan again.
            </p>
            <button
              className="btn btn-primary"
              onClick={() =>
                redirectToLogin('Your session has expired, or you were not logged in. Please log in and try again.')
              }
            >
              Log in
            </button>
          </div>
        )}

        {authErrorCode === 403 && (
          <div className="info-banner">
            <p style={{ margin: '0 0 10px' }}>
              You're logged in, but this account doesn't have permission to view this pass. Log in with an account
              that has access, or contact an administrator.
            </p>
            <button
              className="btn btn-primary"
              onClick={() =>
                redirectToLogin("That account doesn't have permission to view this pass. Log in with a different account if needed.")
              }
            >
              Log in with a different account
            </button>
          </div>
        )}

        {!authErrorCode && error && <p className="error-text">{error}</p>}

        {!loading && !error && kind === 'property' && property && (
          <>
            <div className="detail-list" style={{ marginTop: 16 }}>
              <div className="detail-row">
                <dt>House</dt>
                <dd>{property.houseNumber}</dd>
              </div>
              <div className="detail-row">
                <dt>Block / Plot</dt>
                <dd>
                  {property.block}, {property.plot}
                </dd>
              </div>
              <div className="detail-row">
                <dt>Address</dt>
                <dd>{property.address}</dd>
              </div>
              <div className="detail-row">
                <dt>Occupancy</dt>
                <dd>
                  <StatusBadge value={property.occupancyStatus} />
                </dd>
              </div>
            </div>

            <div className="scan-destination">
              <div className="scan-destination-label">OWNER</div>
              {property.ownerId ? (
                <>
                  <div className="scan-destination-property">{property.ownerName}</div>
                  {property.ownerPhone && <div className="muted">{property.ownerPhone}</div>}
                </>
              ) : (
                <div className="muted">No owner registered</div>
              )}
            </div>

            {property.balance && (
              <div style={{ marginTop: 16 }}>
                <div className="section-title" style={{ marginTop: 0 }}>Payment history</div>
                <BalanceBreakdown items={property.levyBreakdown} />

                {property.recentPayments.length > 0 && (
                  <div style={{ marginTop: 16 }}>
                    <div className="section-title">Recent payments</div>
                    <ul className="detail-sublist">
                      {property.recentPayments.map((p) => (
                        <li key={p.id}>
                          ₦{p.amount.toLocaleString()} · <StatusBadge value={p.method} /> ·{' '}
                          <span className="muted">{new Date(p.paidAt).toLocaleString()}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </>
        )}

        {!loading && !error && kind === 'vehicle' && vehicle && (
          <>
            <div className="scan-status">
              <StatusBadge value={vehicle.status} />
              {vehicle.flagReason && <StatusBadge value={vehicle.flagReason} />}
            </div>

            <div className="detail-list" style={{ marginTop: 16 }}>
              <div className="detail-row">
                <dt>Plate</dt>
                <dd>{vehicle.plateNumber}</dd>
              </div>
              <div className="detail-row">
                <dt>Type</dt>
                <dd>{vehicle.vehicleType ?? <span className="muted">—</span>}</dd>
              </div>
              <div className="detail-row">
                <dt>Make / Model</dt>
                <dd>
                  {[vehicle.make, vehicle.model].filter(Boolean).join(' ') || <span className="muted">—</span>}
                </dd>
              </div>
              <div className="detail-row">
                <dt>Colour</dt>
                <dd>{vehicle.colour ?? <span className="muted">—</span>}</dd>
              </div>
            </div>

            <div className="scan-destination">
              <div className="scan-destination-label">OWNER</div>
              {vehicle.residentId ? (
                <>
                  <div className="scan-destination-property">{vehicle.residentName}</div>
                  {vehicle.residentPhone && <div className="muted">{vehicle.residentPhone}</div>}
                  <div className="muted">{vehicle.propertyHouseNumber ?? 'No property on record'}</div>
                </>
              ) : (
                <div className="muted">No owner on record</div>
              )}
            </div>

            {message && <p style={{ color: 'var(--color-success)' }}>{message}</p>}

            {canCheckInOut && (
              <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
                <button className="btn btn-primary" style={{ flex: 1 }} disabled={submitting} onClick={() => act('checkin')}>
                  Check in
                </button>
                <button className="btn" style={{ flex: 1 }} disabled={submitting} onClick={() => act('checkout')}>
                  Check out
                </button>
              </div>
            )}
          </>
        )}

        {!loading && !error && holder && (
          <>
            {'photo' in holder && holder.photo && <img src={holder.photo} alt="" className="avatar-large" />}
            <div className="scan-status">
              <StatusBadge value={holder.status} />
              {holder.flagReason && <StatusBadge value={holder.flagReason} />}
            </div>

            <div className="detail-list" style={{ marginTop: 16 }}>
              <div className="detail-row">
                <dt>Name</dt>
                <dd>{'name' in holder ? holder.name : holder.fullName}</dd>
              </div>
              <div className="detail-row">
                <dt>Phone</dt>
                <dd>{holder.phone}</dd>
              </div>
              {'contractorName' in holder && (
                <>
                  <div className="detail-row">
                    <dt>Contractor</dt>
                    <dd>{holder.contractorName}</dd>
                  </div>
                  <div className="detail-row">
                    <dt>Work type</dt>
                    <dd>{holder.workType}</dd>
                  </div>
                </>
              )}
              {'vehiclePlate' in holder && holder.vehiclePlate && (
                <div className="detail-row">
                  <dt>Vehicle</dt>
                  <dd>{holder.vehiclePlate}</dd>
                </div>
              )}
              {'residentType' in holder && (
                <div className="detail-row">
                  <dt>Resident type</dt>
                  <dd>
                    <StatusBadge value={holder.residentType} />
                  </dd>
                </div>
              )}
            </div>

            <div className="scan-destination">
              <div className="scan-destination-label">{kind === 'resident' ? 'RESIDENCE' : 'DESTINATION'}</div>
              <div className="scan-destination-property">
                {holder.propertyHouseNumber ?? 'No property on record'}
              </div>
              {holder.propertyAddress && <div className="muted">{holder.propertyAddress}</div>}
              {('hostResidentName' in holder || 'sponsorResidentName' in holder) && (
                <div className="scan-destination-host">
                  Hosted by {'hostResidentName' in holder ? holder.hostResidentName : holder.sponsorResidentName}
                  {(('hostResidentPhone' in holder && holder.hostResidentPhone) ||
                    ('sponsorResidentPhone' in holder && holder.sponsorResidentPhone)) &&
                    ` · ${('hostResidentPhone' in holder ? holder.hostResidentPhone : holder.sponsorResidentPhone)}`}
                </div>
              )}
            </div>

            {message && <p style={{ color: 'var(--color-success)' }}>{message}</p>}

            {canCheckInOut && (
              <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
                <button className="btn btn-primary" style={{ flex: 1 }} disabled={submitting} onClick={() => act('checkin')}>
                  Check in
                </button>
                <button className="btn" style={{ flex: 1 }} disabled={submitting} onClick={() => act('checkout')}>
                  Check out
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
