import { useEffect, useState } from 'react'
import { vehiclesApi } from '../../api/endpoints'
import type { PageResponse, Vehicle, VehicleStatus } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { LoadingState } from '../../components/LoadingState'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { apiErrorMessage } from '../../api/client'

const EMPTY: PageResponse<Vehicle> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const STATUS_OPTIONS: { value: VehicleStatus | ''; label: string; empty: string; noun: string }[] = [
  { value: 'PENDING_APPROVAL', label: 'Awaiting approval', empty: 'No vehicles awaiting your approval.', noun: 'pending' },
  { value: 'ACTIVE', label: 'Active', empty: 'No active tenant vehicles.', noun: 'active' },
  { value: 'INACTIVE', label: 'Deactivated', empty: 'No deactivated tenant vehicles.', noun: 'deactivated' },
  { value: '', label: 'All vehicles', empty: 'Your tenants have no registered vehicles.', noun: '' },
]

/**
 * Landlord self-service: tenants' vehicles on the caller's own property. Pending ones need
 * approval before a gate QR pass can be issued (VehicleService.approve); an active one can be
 * deactivated, which stops its QR pass working at the gate (VehicleService.deactivate);
 * re-approving a deactivated one issues it a fresh QR.
 */
export function VehicleApprovalsPage() {
  const [result, setResult] = useState<PageResponse<Vehicle>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<VehicleStatus | ''>('PENDING_APPROVAL')
  const [busyId, setBusyId] = useState<number | null>(null)
  const [confirmingId, setConfirmingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const option = STATUS_OPTIONS.find((o) => o.value === status) ?? STATUS_OPTIONS[0]

  async function load() {
    setLoading(true)
    setResult(await vehiclesApi.tenantVehicles({ status: status || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status])

  async function run(id: number, action: (id: number) => Promise<Vehicle>) {
    setBusyId(id)
    setError(null)
    try {
      await action(id)
      setConfirmingId(null)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  function actionsFor(v: Vehicle) {
    const busy = busyId === v.id
    if (v.status === 'PENDING_APPROVAL') {
      return (
        <button className="btn btn-sm btn-primary" disabled={busy} onClick={() => run(v.id, vehiclesApi.approve)}>
          {busy ? 'Approving…' : 'Approve'}
        </button>
      )
    }
    if (v.status === 'INACTIVE') {
      return (
        <button className="btn btn-sm" disabled={busy} onClick={() => run(v.id, vehiclesApi.approve)}>
          {busy ? 'Reactivating…' : 'Reactivate'}
        </button>
      )
    }
    // Two-step so a stray tap doesn't lock a tenant's car out of the estate.
    if (confirmingId === v.id) {
      return (
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-sm btn-danger" disabled={busy} onClick={() => run(v.id, vehiclesApi.deactivate)}>
            {busy ? 'Deactivating…' : 'Confirm deactivate'}
          </button>
          <button className="btn btn-sm" disabled={busy} onClick={() => setConfirmingId(null)}>
            Cancel
          </button>
        </div>
      )
    }
    return (
      <button className="btn btn-sm" onClick={() => setConfirmingId(v.id)}>
        Deactivate
      </button>
    )
  }

  // Group by the tenant who registered each vehicle, so the landlord can see whose it is.
  // The API sorts by tenant, so a tenant's vehicles stay together across pages.
  const groups: { residentId: number; residentName: string | null; residentPhone: string | null; vehicles: Vehicle[] }[] = []
  for (const v of result.content) {
    let g = groups.find((x) => x.residentId === v.residentId)
    if (!g) {
      g = { residentId: v.residentId, residentName: v.residentName, residentPhone: v.residentPhone, vehicles: [] }
      groups.push(g)
    }
    g.vehicles.push(v)
  }

  return (
    <div>
      <div className="page-header" data-tour="/portal/vehicle-approvals">
        <div>
          <h1>Tenant Vehicles</h1>
          <p className="page-subtitle">
            Approve vehicles your tenants register before they can get a gate QR pass. Deactivating a vehicle stops
            its QR pass working at the gate.
          </p>
        </div>
      </div>

      <div className="toolbar" style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as VehicleStatus | '')
            setPage(0)
            setConfirmingId(null)
          }}
          style={{ minWidth: 200 }}
        >
          {STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="error-text">{error}</p>}

      {loading ? (
        <LoadingState />
      ) : groups.length === 0 ? (
        <div className="empty-state">{option.empty}</div>
      ) : (
        groups.map((g) => {
          const count = `${g.vehicles.length}${option.noun ? ` ${option.noun}` : ''} vehicle${g.vehicles.length === 1 ? '' : 's'}`
          return (
            <section key={g.residentId} className="card" style={{ marginBottom: 16 }}>
              <div style={{ marginBottom: 12 }}>
                <h2 style={{ fontSize: 16, fontWeight: 600, margin: 0 }}>
                  {g.residentName ?? `Resident #${g.residentId}`}
                </h2>
                <p className="page-subtitle">{[g.residentPhone, count].filter(Boolean).join(' · ')}</p>
              </div>
              <DataTable
                rows={g.vehicles}
                rowKey={(v) => v.id}
                columns={[
                  { key: 'plateNumber', label: 'Plate' },
                  { key: 'make', label: 'Make', render: (v) => v.make ?? <span className="muted">—</span> },
                  { key: 'model', label: 'Model', render: (v) => v.model ?? <span className="muted">—</span> },
                  { key: 'colour', label: 'Colour', render: (v) => v.colour ?? <span className="muted">—</span> },
                  { key: 'status', label: 'Status', render: (v) => <StatusBadge value={v.status} /> },
                ]}
                actions={actionsFor}
              />
            </section>
          )
        })
      )}

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
