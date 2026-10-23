import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { complaintsApi } from '../../api/endpoints'
import type { Complaint, ComplaintStatus, PageResponse, Role } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { ASSIGNABLE_COMPLAINT_ROLES, ROLE_LABELS } from '../../utils/roles'

const STATUSES: ComplaintStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']
const EMPTY: PageResponse<Complaint> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function ComplaintsAdminPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [result, setResult] = useState<PageResponse<Complaint>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  /** Initialised from ?openOnly=1 so the dashboard's "Open complaints" card can link straight here. */
  const [openOnly, setOpenOnly] = useState(searchParams.get('openOnly') === '1')
  const { openResident, openComplaint, stack } = useEntityDetail()
  const wasDetailOpenRef = useRef(false)

  async function load() {
    setLoading(true)
    setResult(await complaintsApi.listAll({ openOnly, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openOnly, page])

  useEffect(() => {
    setPage(0)
  }, [openOnly])

  // The detail modal can advance a complaint's status itself (see EntityDetailModal) — reload
  // once it closes so this table doesn't keep showing the pre-advance status.
  useEffect(() => {
    if (stack.length > 0) {
      wasDetailOpenRef.current = true
    } else if (wasDetailOpenRef.current) {
      wasDetailOpenRef.current = false
      load()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stack])

  async function advance(c: Complaint) {
    const nextIndex = Math.min(STATUSES.indexOf(c.status) + 1, STATUSES.length - 1)
    await complaintsApi.updateStatus(c.id, STATUSES[nextIndex])
    await load()
  }

  async function assign(c: Complaint, role: string) {
    await complaintsApi.assign(c.id, (role || null) as Role | null)
    await load()
  }

  return (
    <div>
      <div className="page-header" data-tour="/complaints">
        <div>
          <h1>Complaints</h1>
          <p className="page-subtitle">OPEN → ASSIGNED → IN PROGRESS → RESOLVED → CLOSED.</p>
        </div>
      </div>

      <div className="toolbar">
        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={openOnly}
            onChange={(e) => {
              setOpenOnly(e.target.checked)
              setSearchParams(e.target.checked ? { openOnly: '1' } : {})
            }}
          />
          Open complaints only
        </label>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(c) => c.id}
        emptyMessage={openOnly ? 'No open complaints right now.' : 'No complaints filed yet.'}
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
          {
            key: 'description',
            label: 'Description',
            render: (c) => (
              <button type="button" className="link-button cell-truncate" onClick={() => openComplaint(c.id)}>
                {c.description}
              </button>
            ),
          },
          { key: 'priority', label: 'Priority', render: (c) => <StatusBadge value={c.priority} /> },
          { key: 'status', label: 'Status', render: (c) => <StatusBadge value={c.status} /> },
          {
            key: 'assignedRole',
            label: 'Assigned to',
            render: (c) => (
              <select value={c.assignedRole ?? ''} onChange={(e) => assign(c, e.target.value)}>
                <option value="">Unassigned</option>
                {ASSIGNABLE_COMPLAINT_ROLES.map((role) => (
                  <option key={role} value={role}>
                    {ROLE_LABELS[role]}
                  </option>
                ))}
              </select>
            ),
          },
        ]}
        actions={(c) => (
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-sm" onClick={() => openComplaint(c.id)}>
              View
            </button>
            {c.status !== 'CLOSED' && (
              <button className="btn btn-sm" onClick={() => advance(c)}>
                Advance status
              </button>
            )}
          </div>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
