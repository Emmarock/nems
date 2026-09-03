import { useEffect, useState } from 'react'
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
  const [result, setResult] = useState<PageResponse<Visitor>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)

  async function load() {
    setLoading(true)
    setResult(await visitorsApi.listAll({ q: debouncedQuery || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Visitors</h1>
          <p className="page-subtitle">Every visitor pass issued by residents estate-wide (spec Phase 2 §4 — Visitor Module).</p>
        </div>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search visitors by name or phone…" />

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(v) => v.id}
        emptyMessage={query ? 'No visitors match your search.' : 'No visitor passes have been issued yet.'}
        columns={[
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
