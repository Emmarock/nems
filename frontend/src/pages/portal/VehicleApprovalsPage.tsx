import { useEffect, useState } from 'react'
import { vehiclesApi } from '../../api/endpoints'
import type { PageResponse, Vehicle } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { LoadingState } from '../../components/LoadingState'
import { Pagination } from '../../components/Pagination'
import { apiErrorMessage } from '../../api/client'

const EMPTY: PageResponse<Vehicle> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

/**
 * Landlord self-service: vehicles tenants on the caller's own property have registered, awaiting
 * approval before a gate-access QR pass can be issued for them (see VehicleService.approve).
 */
export function VehicleApprovalsPage() {
  const [result, setResult] = useState<PageResponse<Vehicle>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setResult(await vehiclesApi.pendingApproval({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleApprove(id: number) {
    setBusyId(id)
    setError(null)
    try {
      await vehiclesApi.approve(id)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
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
          <h1>Vehicle Approvals</h1>
          <p className="page-subtitle">
            Tenants' vehicles on your property wait here until you approve them — no gate-access QR pass can be
            issued until you do.
          </p>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}

      {loading ? (
        <LoadingState />
      ) : groups.length === 0 ? (
        <div className="empty-state">No vehicles awaiting your approval.</div>
      ) : (
        groups.map((g) => (
          <section key={g.residentId} className="card" style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 12 }}>
              <h2 style={{ fontSize: 16, fontWeight: 600, margin: 0 }}>
                Requested by {g.residentName ?? `Resident #${g.residentId}`}
              </h2>
              <p className="page-subtitle">
                {[g.residentPhone, `${g.vehicles.length} pending vehicle${g.vehicles.length === 1 ? '' : 's'}`]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
            </div>
            <DataTable
              rows={g.vehicles}
              rowKey={(v) => v.id}
              columns={[
                { key: 'plateNumber', label: 'Plate' },
                { key: 'make', label: 'Make', render: (v) => v.make ?? <span className="muted">—</span> },
                { key: 'model', label: 'Model', render: (v) => v.model ?? <span className="muted">—</span> },
                { key: 'colour', label: 'Colour', render: (v) => v.colour ?? <span className="muted">—</span> },
              ]}
              actions={(v) => (
                <button className="btn btn-sm btn-primary" disabled={busyId === v.id} onClick={() => handleApprove(v.id)}>
                  {busyId === v.id ? 'Approving…' : 'Approve'}
                </button>
              )}
            />
          </section>
        ))
      )}

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
