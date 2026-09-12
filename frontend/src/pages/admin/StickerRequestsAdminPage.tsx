import { useEffect, useState } from 'react'
import { stickerRequestsApi } from '../../api/endpoints'
import type { PageResponse, StickerRequest } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'

const EMPTY: PageResponse<StickerRequest> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

/** Read-only: a sticker is issued automatically the moment the linked payment is approved (see
 * Payments — approving a PENDING_APPROVAL receipt tied to a sticker's invoice issues it), so
 * there's no separate "issue" action here, only visibility into the queue. */
export function StickerRequestsAdminPage() {
  const { openResident } = useEntityDetail()
  const [result, setResult] = useState<PageResponse<StickerRequest>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)

  async function load() {
    setLoading(true)
    setResult(await stickerRequestsApi.list({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Vehicle Stickers</h1>
          <p className="page-subtitle">
            Every sticker requested estate-wide. Issued automatically once the linked payment is approved under
            Payments.
          </p>
        </div>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(sr) => sr.id}
        emptyMessage="No sticker requests yet."
        columns={[
          { key: 'plateNumber', label: 'Vehicle', render: (sr) => sr.plateNumber ?? `Vehicle #${sr.vehicleId}` },
          {
            key: 'residentId',
            label: 'Resident',
            render: (sr) => (
              <button type="button" className="link-button" onClick={() => openResident(sr.residentId)}>
                {sr.residentName ?? `Resident #${sr.residentId}`}
              </button>
            ),
          },
          {
            key: 'invoiceAmount',
            label: 'Fee',
            render: (sr) => (sr.invoiceAmount != null ? `₦${sr.invoiceAmount.toLocaleString()}` : <span className="muted">—</span>),
          },
          { key: 'status', label: 'Status', render: (sr) => <StatusBadge value={sr.status} /> },
          { key: 'requestedAt', label: 'Requested', render: (sr) => new Date(sr.requestedAt).toLocaleString() },
          {
            key: 'issuedAt',
            label: 'Issued',
            render: (sr) => (sr.issuedAt ? new Date(sr.issuedAt).toLocaleString() : <span className="muted">—</span>),
          },
        ]}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
