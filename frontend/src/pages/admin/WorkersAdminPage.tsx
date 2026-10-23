import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
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

const CAN_MANAGE = new Set(['SUPER_ADMIN', 'CDA_ADMIN'])
const EMPTY: PageResponse<Worker> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function WorkersAdminPage() {
  const { user } = useAuth()
  const { openResident } = useEntityDetail()
  const [searchParams, setSearchParams] = useSearchParams()
  const [result, setResult] = useState<PageResponse<Worker>>(EMPTY)
  const [loading, setLoading] = useState(true)
  /** Initialised from ?activeOnly=1 so the dashboard's "Workers on site" card can link straight here. */
  const [activeOnly, setActiveOnly] = useState(searchParams.get('activeOnly') === '1')
  /** Due (or overdue) for checkout by their own contract end date, but still checked in right now - see CheckoutReminderJob. */
  const [dueOnSiteOnly, setDueOnSiteOnly] = useState(false)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  async function load() {
    setLoading(true)
    setResult(
      dueOnSiteOnly
        ? await workersApi.dueCheckoutOnSite({ page, size: 20 })
        : await workersApi.listAll({ q: debouncedQuery || undefined, activeOnly, page, size: 20 }),
    )
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeOnly, dueOnSiteOnly, debouncedQuery, page])

  useEffect(() => {
    setPage(0)
  }, [activeOnly, dueOnSiteOnly, debouncedQuery])

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
      <div className="page-header" data-tour="/workers">
        <div>
          <h1>Workers</h1>
          <p className="page-subtitle">
            Contractor/labourer access, issued immediately by the sponsoring resident.
          </p>
        </div>
      </div>

      {!dueOnSiteOnly && (
        <SearchInput value={query} onChange={setQuery} placeholder="Search workers by name, contractor, work type…" />
      )}

      <div className="toolbar">
        {!dueOnSiteOnly && (
          <label className="checkbox-field">
            <input
              type="checkbox"
              checked={activeOnly}
              onChange={(e) => {
                setActiveOnly(e.target.checked)
                setSearchParams(e.target.checked ? { activeOnly: '1' } : {})
              }}
            />
            Active on site only
          </label>
        )}
        {user && CAN_MANAGE.has(user.role) && (
          <label className="checkbox-field">
            <input type="checkbox" checked={dueOnSiteOnly} onChange={(e) => setDueOnSiteOnly(e.target.checked)} />
            Due for checkout &amp; still on site
          </label>
        )}
      </div>
      {dueOnSiteOnly && (
        <p className="muted" style={{ marginTop: -8, marginBottom: 14, fontSize: 13 }}>
          Workers whose contract end date has arrived or passed, but who haven't checked out yet — search and the
          active-on-site filter don't apply here.
        </p>
      )}

      {error && <p className="error-text">{error}</p>}

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(w) => w.id}
        emptyMessage={
          dueOnSiteOnly
            ? 'No workers are due for checkout and still on site.'
            : query
              ? 'No workers match your search.'
              : 'No worker access requests yet.'
        }
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
          { key: 'workerCategory', label: 'Category', render: (w) => <StatusBadge value={w.workerCategory} /> },
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
            {w.status !== 'SUSPENDED' && w.status !== 'COMPLETED' && (
              <button className="btn btn-sm" disabled={busyId === w.id} onClick={() => run(w.id, workersApi.suspend)}>
                Suspend
              </button>
            )}
            {w.status !== 'COMPLETED' && user && CAN_MANAGE.has(user.role) && (
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
