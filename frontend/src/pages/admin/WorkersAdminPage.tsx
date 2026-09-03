import { useEffect, useState } from 'react'
import { workersApi } from '../../api/endpoints'
import type { PageResponse, Worker } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { apiErrorMessage } from '../../api/client'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const CAN_APPROVE = new Set(['SUPER_ADMIN', 'CDA_ADMIN'])
const EMPTY: PageResponse<Worker> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function WorkersAdminPage() {
  const { user } = useAuth()
  const { openResident } = useEntityDetail()
  const [result, setResult] = useState<PageResponse<Worker>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [activeOnly, setActiveOnly] = useState(false)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  async function load() {
    setLoading(true)
    setResult(await workersApi.listAll({ q: debouncedQuery || undefined, activeOnly, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeOnly, debouncedQuery, page])

  useEffect(() => {
    setPage(0)
  }, [activeOnly, debouncedQuery])

  async function run(id: number, action: (id: number) => Promise<Worker>) {
    setBusyId(id)
    setError(null)
    try {
      await action(id)
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
          <h1>Workers</h1>
          <p className="page-subtitle">
            Approve and manage contractor/labourer access requests (spec Phase 2 §4 — Worker Module).
          </p>
        </div>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search workers by name, contractor, work type…" />

      <div className="toolbar">
        <label className="checkbox-field">
          <input type="checkbox" checked={activeOnly} onChange={(e) => setActiveOnly(e.target.checked)} />
          Active on site only
        </label>
      </div>

      {error && <p className="error-text">{error}</p>}

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(w) => w.id}
        emptyMessage={query ? 'No workers match your search.' : 'No worker access requests yet.'}
        columns={[
          {
            key: 'photo',
            label: '',
            render: (w) =>
              w.photo ? (
                <img src={w.photo} alt="" className="avatar-thumb" />
              ) : (
                <div className="avatar-thumb avatar-thumb-empty" />
              ),
          },
          { key: 'fullName', label: 'Worker' },
          { key: 'contractorName', label: 'Contractor' },
          { key: 'workType', label: 'Work type' },
          {
            key: 'sponsorResidentId',
            label: 'Sponsor',
            render: (w) => (
              <button type="button" className="link-button" onClick={() => openResident(w.sponsorResidentId)}>
                Resident #{w.sponsorResidentId}
              </button>
            ),
          },
          { key: 'startDate', label: 'Start' },
          { key: 'expectedEndDate', label: 'Expected end' },
          { key: 'status', label: 'Status', render: (w) => <StatusBadge value={w.status} /> },
        ]}
        actions={(w) => (
          <div style={{ display: 'flex', gap: 6 }}>
            {w.status === 'PENDING' && user && CAN_APPROVE.has(user.role) && (
              <button className="btn btn-sm" disabled={busyId === w.id} onClick={() => run(w.id, workersApi.approve)}>
                Approve
              </button>
            )}
            {w.status !== 'SUSPENDED' && w.status !== 'COMPLETED' && (
              <button className="btn btn-sm" disabled={busyId === w.id} onClick={() => run(w.id, workersApi.suspend)}>
                Suspend
              </button>
            )}
            {w.status !== 'COMPLETED' && user && CAN_APPROVE.has(user.role) && (
              <button className="btn btn-sm" disabled={busyId === w.id} onClick={() => run(w.id, workersApi.complete)}>
                Complete
              </button>
            )}
          </div>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
