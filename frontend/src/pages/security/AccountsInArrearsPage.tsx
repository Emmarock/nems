import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { residentsApi } from '../../api/endpoints'
import type { PageResponse, ResidentArrears } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const EMPTY: PageResponse<ResidentArrears> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

/** Drill-through from the security dashboard's "Accounts in arrears" stat card. */
export function AccountsInArrearsPage() {
  const [result, setResult] = useState<PageResponse<ResidentArrears>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query)
  const navigate = useNavigate()
  const { openResident, openProperty } = useEntityDetail()

  async function load() {
    setLoading(true)
    setResult(await residentsApi.arrears({ q: debouncedQuery || undefined, page, size: 20 }))
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
          <h1>Accounts in arrears</h1>
          <p className="page-subtitle">
            Residents whose outstanding balance is above the enforcement threshold set in Access Policy, biggest balance first.
          </p>
        </div>
        <button className="btn" onClick={() => navigate('/security')}>
          Back to dashboard
        </button>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search by resident name or phone…" />

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(r) => r.id}
        emptyMessage={query ? 'No accounts in arrears match your search.' : 'No accounts are currently in arrears.'}
        columns={[
          {
            key: 'fullName',
            label: 'Resident',
            render: (r) => (
              <button type="button" className="link-button" onClick={() => openResident(r.id)}>
                {r.fullName}
              </button>
            ),
          },
          {
            key: 'propertyHouseNumber',
            label: 'Property',
            render: (r) =>
              r.propertyId ? (
                <button type="button" className="link-button" onClick={() => openProperty(r.propertyId!)}>
                  {r.propertyHouseNumber ?? `Property #${r.propertyId}`}
                </button>
              ) : (
                <span className="muted">—</span>
              ),
          },
          { key: 'phone', label: 'Phone' },
          { key: 'outstanding', label: 'Outstanding', render: (r) => `₦${r.outstanding.toLocaleString()}` },
        ]}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
