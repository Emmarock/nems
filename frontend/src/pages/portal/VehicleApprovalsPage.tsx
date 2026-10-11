import { useEffect, useState } from 'react'
import { vehiclesApi } from '../../api/endpoints'
import type { PageResponse, Vehicle } from '../../api/types'
import { DataTable } from '../../components/DataTable'
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

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Vehicle Approvals</h1>
          <p className="page-subtitle">
            Tenants' vehicles on your property wait here until you approve them — no gate-access QR pass can be
            issued until you do.
          </p>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(v) => v.id}
        emptyMessage="No vehicles awaiting your approval."
        columns={[
          { key: 'plateNumber', label: 'Plate' },
          { key: 'residentName', label: 'Tenant', render: (v) => v.residentName ?? <span className="muted">—</span> },
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

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
