import { useEffect, useState, type ReactNode } from 'react'
import {
  accessEventsApi,
  accountsApi,
  complaintsApi,
  gatesApi,
  propertiesApi,
  residentsApi,
  vehiclesApi,
  visitorsApi,
  workersApi,
} from '../api/endpoints'
import type {
  AccessEvent,
  AccessSubjectType,
  Complaint,
  Gate,
  LevyBalance,
  Property,
  Resident,
  Vehicle,
  Visitor,
  Worker,
} from '../api/types'
import { useEntityDetail } from '../entityDetail/EntityDetailContext'
import { apiErrorMessage } from '../api/client'
import { BalanceBreakdown } from './BalanceBreakdown'
import { LoadingState } from './LoadingState'
import { StatusBadge } from './StatusBadge'

const TITLES = {
  resident: 'Resident',
  property: 'Property',
  gate: 'Gate',
  visitor: 'Visitor',
  worker: 'Worker',
  vehicle: 'Vehicle',
} as const

export function EntityDetailModal() {
  const { stack, openResident, openProperty, openVisitor, openWorker, openVehicle, back, close } = useEntityDetail()
  const current = stack[stack.length - 1]

  const [resident, setResident] = useState<Resident | null>(null)
  const [property, setProperty] = useState<Property | null>(null)
  const [gate, setGate] = useState<Gate | null>(null)
  const [visitor, setVisitor] = useState<Visitor | null>(null)
  const [workerDetail, setWorkerDetail] = useState<Worker | null>(null)
  const [vehicleDetail, setVehicleDetail] = useState<Vehicle | null>(null)
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [workers, setWorkers] = useState<Worker[]>([])
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [gateEvents, setGateEvents] = useState<AccessEvent[]>([])
  const [balanceBreakdown, setBalanceBreakdown] = useState<LevyBalance[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!current) return
    let cancelled = false
    setLoading(true)
    setError(null)
    setResident(null)
    setProperty(null)
    setGate(null)
    setVisitor(null)
    setWorkerDetail(null)
    setVehicleDetail(null)
    setVehicles([])
    setWorkers([])
    setComplaints([])
    setGateEvents([])
    setBalanceBreakdown([])

    let request: Promise<void>
    if (current.type === 'resident') {
      request = Promise.all([
        residentsApi.get(current.id),
        vehiclesApi.list({ residentId: current.id, size: 20 }).then((r) => r.content).catch(() => []),
        workersApi.listBySponsor(current.id, { size: 20 }).then((r) => r.content).catch(() => []),
        complaintsApi.listByResident(current.id, { size: 20 }).then((r) => r.content).catch(() => []),
        accountsApi.balanceBreakdown(current.id).catch(() => []),
      ]).then(([r, v, w, c, b]) => {
        if (cancelled) return
        setResident(r)
        setVehicles(v)
        setWorkers(w)
        setComplaints(c)
        setBalanceBreakdown(b)
      })
    } else if (current.type === 'property') {
      request = propertiesApi.get(current.id).then(async (p) => {
        if (cancelled) return
        setProperty(p)
        if (p.ownerId) {
          const b = await accountsApi.balanceBreakdown(p.ownerId).catch(() => [])
          if (!cancelled) setBalanceBreakdown(b)
        }
      })
    } else if (current.type === 'gate') {
      request = Promise.all([
        gatesApi.get(current.id),
        accessEventsApi.listByGate(current.id, { size: 10 }).then((r) => r.content).catch(() => []),
      ]).then(([g, events]) => {
        if (cancelled) return
        setGate(g)
        setGateEvents(events)
      })
    } else if (current.type === 'visitor') {
      request = visitorsApi.get(current.id).then((v) => {
        if (!cancelled) setVisitor(v)
      })
    } else if (current.type === 'worker') {
      request = workersApi.get(current.id).then((w) => {
        if (!cancelled) setWorkerDetail(w)
      })
    } else {
      request = vehiclesApi.get(current.id).then((v) => {
        if (!cancelled) setVehicleDetail(v)
      })
    }

    request
      .catch((err) => {
        if (!cancelled) setError(apiErrorMessage(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [current])

  if (!current) return null

  function openSubject(subjectType: AccessSubjectType, subjectId: number) {
    if (subjectType === 'RESIDENT') openResident(subjectId)
    else if (subjectType === 'VISITOR') openVisitor(subjectId)
    else if (subjectType === 'WORKER') openWorker(subjectId)
    else openVehicle(subjectId)
  }

  return (
    <div className="modal-overlay" onClick={close}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{TITLES[current.type]} details</h2>
          <div style={{ display: 'flex', gap: 6 }}>
            {stack.length > 1 && (
              <button type="button" className="btn btn-sm" onClick={back}>
                ← Back
              </button>
            )}
            <button type="button" className="btn btn-sm" onClick={close}>
              ✕
            </button>
          </div>
        </div>
        <div className="modal-body">
          {loading && <LoadingState />}
          {error && <p className="error-text">{error}</p>}

          {!loading && !error && current.type === 'resident' && resident && (
            <dl className="detail-list">
              <Row label="Full name" value={resident.fullName} />
              <Row label="Phone" value={resident.phone} />
              <Row label="Email" value={resident.email} />
              <Row label="Resident type" value={<StatusBadge value={resident.residentType} />} />
              <Row label="Status" value={<StatusBadge value={resident.status} />} />
              <Row label="Emergency contact" value={resident.emergencyContact} />
              <Row label="Registration date" value={resident.registrationDate} />
              <Row
                label="Property"
                value={
                  resident.propertyId ? (
                    <button type="button" className="link-button" onClick={() => openProperty(resident.propertyId!)}>
                      {resident.propertyHouseNumber ?? `Property #${resident.propertyId}`} →
                    </button>
                  ) : (
                    <span className="muted">Not linked to a property</span>
                  )
                }
              />
              <Row
                label="Vehicles"
                value={
                  vehicles.length === 0 ? (
                    <span className="muted">No vehicles registered</span>
                  ) : (
                    <ul className="detail-sublist">
                      {vehicles.map((v) => (
                        <li key={v.id}>
                          <strong>{v.plateNumber}</strong>
                          {(v.make || v.model) && ` — ${[v.make, v.model].filter(Boolean).join(' ')}`}
                          {v.colour && ` (${v.colour})`}
                        </li>
                      ))}
                    </ul>
                  )
                }
              />
              <Row
                label="Workers sponsored"
                value={
                  workers.length === 0 ? (
                    <span className="muted">No worker access requests</span>
                  ) : (
                    <ul className="detail-sublist">
                      {workers.map((w) => (
                        <li key={w.id}>
                          <strong>{w.fullName}</strong> — {w.contractorName} <StatusBadge value={w.status} />
                        </li>
                      ))}
                    </ul>
                  )
                }
              />
              <Row
                label="Complaints"
                value={
                  complaints.length === 0 ? (
                    <span className="muted">No complaints filed</span>
                  ) : (
                    <ul className="detail-sublist">
                      {complaints.map((c) => (
                        <li key={c.id}>
                          <StatusBadge value={c.category} /> {c.description} <StatusBadge value={c.status} />
                        </li>
                      ))}
                    </ul>
                  )
                }
              />
              <Row label="Amount owed by levy" value={<BalanceBreakdown items={balanceBreakdown} />} />
            </dl>
          )}

          {!loading && !error && current.type === 'property' && property && (
            <dl className="detail-list">
              <Row label="House number" value={property.houseNumber} />
              <Row label="Block" value={property.block} />
              <Row label="Plot" value={property.plot} />
              <Row label="Address" value={property.address} />
              <Row label="Property type" value={<StatusBadge value={property.propertyType} />} />
              <Row label="Occupancy" value={<StatusBadge value={property.occupancyStatus} />} />
              <Row
                label="Owner"
                value={
                  property.ownerId ? (
                    <button type="button" className="link-button" onClick={() => openResident(property.ownerId!)}>
                      {property.ownerName ?? `Resident #${property.ownerId}`} →
                    </button>
                  ) : (
                    <span className="muted">No owner registered</span>
                  )
                }
              />
              {property.ownerId && (
                <Row label="Amount owed by levy" value={<BalanceBreakdown items={balanceBreakdown} />} />
              )}
            </dl>
          )}

          {!loading && !error && current.type === 'gate' && gate && (
            <dl className="detail-list">
              <Row label="Name" value={gate.name} />
              <Row label="Code" value={gate.code} />
              <Row label="Location" value={gate.location} />
              <Row label="Type" value={<StatusBadge value={gate.type} />} />
              <Row label="Status" value={<StatusBadge value={gate.status} />} />
              <Row
                label="Recent access events"
                value={
                  gateEvents.length === 0 ? (
                    <span className="muted">No access events logged at this gate</span>
                  ) : (
                    <ul className="detail-sublist">
                      {gateEvents.map((e) => (
                        <li key={e.id}>
                          <StatusBadge value={e.subjectType} />{' '}
                          <button
                            type="button"
                            className="link-button"
                            onClick={() => openSubject(e.subjectType, e.subjectId)}
                          >
                            #{e.subjectId}
                          </button>{' '}
                          — <StatusBadge value={e.direction} />{' '}
                          <span className="muted">{new Date(e.occurredAt).toLocaleString()}</span>
                          {e.flagReason && (
                            <>
                              {' '}
                              <StatusBadge value={e.flagReason} />
                            </>
                          )}
                        </li>
                      ))}
                    </ul>
                  )
                }
              />
            </dl>
          )}

          {!loading && !error && current.type === 'visitor' && visitor && (
            <dl className="detail-list">
              <Row label="Name" value={visitor.name} />
              <Row label="Phone" value={visitor.phone} />
              <Row label="Vehicle" value={visitor.vehiclePlate} />
              <Row label="Valid from" value={new Date(visitor.validFrom).toLocaleString()} />
              <Row label="Valid until" value={new Date(visitor.validUntil).toLocaleString()} />
              <Row label="Status" value={<StatusBadge value={visitor.status} />} />
              <Row
                label="Host"
                value={
                  <button type="button" className="link-button" onClick={() => openResident(visitor.hostResidentId)}>
                    {visitor.hostResidentName ?? `Resident #${visitor.hostResidentId}`}
                  </button>
                }
              />
            </dl>
          )}

          {!loading && !error && current.type === 'worker' && workerDetail && (
            <dl className="detail-list">
              {workerDetail.photo && <img src={workerDetail.photo} alt="" className="avatar-large" />}
              <Row label="Full name" value={workerDetail.fullName} />
              <Row label="Phone" value={workerDetail.phone} />
              <Row label="National / Work ID" value={workerDetail.nationalId} />
              <Row label="Contractor" value={workerDetail.contractorName} />
              <Row label="Work type" value={workerDetail.workType} />
              <Row label="Start date" value={workerDetail.startDate} />
              <Row label="Expected end date" value={workerDetail.expectedEndDate} />
              <Row label="Status" value={<StatusBadge value={workerDetail.status} />} />
              <Row
                label="Sponsor"
                value={
                  <button
                    type="button"
                    className="link-button"
                    onClick={() => openResident(workerDetail.sponsorResidentId)}
                  >
                    View resident #{workerDetail.sponsorResidentId} →
                  </button>
                }
              />
              <Row
                label="Site"
                value={
                  workerDetail.siteId ? (
                    <button type="button" className="link-button" onClick={() => openProperty(workerDetail.siteId!)}>
                      {workerDetail.siteHouseNumber ?? `Property #${workerDetail.siteId}`} →
                    </button>
                  ) : (
                    <span className="muted">No property on record</span>
                  )
                }
              />
            </dl>
          )}

          {!loading && !error && current.type === 'vehicle' && vehicleDetail && (
            <dl className="detail-list">
              <Row label="Plate number" value={vehicleDetail.plateNumber} />
              <Row label="Vehicle type" value={vehicleDetail.vehicleType} />
              <Row label="Make" value={vehicleDetail.make} />
              <Row label="Model" value={vehicleDetail.model} />
              <Row label="Colour" value={vehicleDetail.colour} />
              <Row label="Status" value={<StatusBadge value={vehicleDetail.status} />} />
              <Row
                label="Resident"
                value={
                  <button
                    type="button"
                    className="link-button"
                    onClick={() => openResident(vehicleDetail.residentId)}
                  >
                    {vehicleDetail.residentName ?? `Resident #${vehicleDetail.residentId}`}
                  </button>
                }
              />
            </dl>
          )}
        </div>
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="detail-row">
      <dt>{label}</dt>
      <dd>{value || <span className="muted">—</span>}</dd>
    </div>
  )
}

