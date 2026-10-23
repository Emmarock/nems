import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { visitorsApi } from '../../api/endpoints'
import type { PageResponse, Visitor } from '../../api/types'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const EMPTY: PageResponse<Visitor> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function VisitorsAdminPage() {
  const { openResident, openVisitor } = useEntityDetail()
  const [searchParams, setSearchParams] = useSearchParams()
  const [result, setResult] = useState<PageResponse<Visitor>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  /** Initialised from ?status=ACTIVE so the dashboard's "Active visitor passes" card can link straight here. */
  const [activeOnly, setActiveOnly] = useState(searchParams.get('status') === 'ACTIVE')
  const debouncedQuery = useDebouncedValue(query)

  async function load() {
    setLoading(true)
    setResult(
      await visitorsApi.listAll({ q: debouncedQuery || undefined, status: activeOnly ? 'ACTIVE' : undefined, page, size: 20 }),
    )
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, activeOnly, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery, activeOnly])

  return (
    <div>
      <div className="page-header" data-tour="/visitors">
        <div>
          <h1>Visitors</h1>
          <p className="page-subtitle">Every visitor pass issued by residents estate-wide (Visitor Module).</p>
        </div>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search visitors by name or phone…" />

      <div className="toolbar">
        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={activeOnly}
            onChange={(e) => {
              setActiveOnly(e.target.checked)
              setSearchParams(e.target.checked ? { status: 'ACTIVE' } : {})
            }}
          />
          Active passes only
        </label>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(v) => v.id}
        emptyMessage={
          query
            ? 'No visitors match your search.'
            : activeOnly
              ? 'No active visitor passes right now.'
              : 'No visitor passes have been issued yet.'
        }
        columns={[
          {
            key: 'photo',
            label: '',
            render: (v) =>
              v.photo ? (
                <img src={v.photo} alt="" className="avatar-thumb" />
              ) : (
                <div className="avatar-thumb avatar-thumb-empty" />
              ),
          },
          {
            key: 'name',
            label: 'Visitor',
            render: (v) => (
              <button type="button" className="link-button" onClick={() => openVisitor(v.id)}>
                {v.name}
              </button>
            ),
          },
          { key: 'phone', label: 'Phone' },
          { key: 'vehiclePlate', label: 'Vehicle', render: (v) => v.vehiclePlate ?? <span className="muted">—</span> },
          {
            key: 'hostResidentId',
            label: 'Host',
            render: (v) => (
              <button type="button" className="link-button" onClick={() => openResident(v.hostResidentId)}>
                {v.hostResidentName ?? `Resident #${v.hostResidentId}`}
              </button>
            ),
          },
          { key: 'validFrom', label: 'Valid from', render: (v) => new Date(v.validFrom).toLocaleString() },
          { key: 'validUntil', label: 'Valid until', render: (v) => new Date(v.validUntil).toLocaleString() },
          { key: 'status', label: 'Status', render: (v) => <StatusBadge value={v.status} /> },
        ]}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
