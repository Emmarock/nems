import { useEffect, useState } from 'react'
import { complaintsApi } from '../../api/endpoints'
import type { Complaint, ComplaintStatus, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'

const STATUSES: ComplaintStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']
const EMPTY: PageResponse<Complaint> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function ComplaintsAdminPage() {
  const [result, setResult] = useState<PageResponse<Complaint>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const { openResident } = useEntityDetail()

  async function load() {
    setLoading(true)
    setResult(await complaintsApi.listAll({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function advance(c: Complaint) {
    const nextIndex = Math.min(STATUSES.indexOf(c.status) + 1, STATUSES.length - 1)
    await complaintsApi.updateStatus(c.id, STATUSES[nextIndex])
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Complaints</h1>
          <p className="page-subtitle">OPEN → ASSIGNED → IN PROGRESS → RESOLVED → CLOSED (spec §7).</p>
        </div>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(c) => c.id}
        emptyMessage="No complaints filed yet."
        columns={[
          { key: 'id', label: 'ID' },
          {
            key: 'residentId',
            label: 'Resident',
            render: (c) => (
              <button type="button" className="link-button" onClick={() => openResident(c.residentId)}>
                Resident #{c.residentId}
              </button>
            ),
          },
          { key: 'category', label: 'Category', render: (c) => <StatusBadge value={c.category} /> },
          { key: 'description', label: 'Description', render: (c) => <span className="cell-truncate">{c.description}</span> },
          { key: 'priority', label: 'Priority', render: (c) => <StatusBadge value={c.priority} /> },
          { key: 'status', label: 'Status', render: (c) => <StatusBadge value={c.status} /> },
        ]}
        actions={(c) =>
          c.status !== 'CLOSED' ? (
            <button className="btn btn-sm" onClick={() => advance(c)}>
              Advance status
            </button>
          ) : null
        }
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
