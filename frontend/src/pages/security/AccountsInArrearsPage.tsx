import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { residentsApi } from '../../api/endpoints'
import type { PageResponse, ResidentArrears } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { DataTable } from '../../components/DataTable'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { apiErrorMessage } from '../../api/client'
import { downloadBlob } from '../../utils/downloadBlob'

const EMPTY: PageResponse<ResidentArrears> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

/** Security, finance (treasurer/financial secretary) and admin roles all land here with equal access. */
const BACK_DESTINATION: Record<string, { to: string; label: string }> = {
  SECURITY: { to: '/security', label: 'Back to security dashboard' },
}

/** Drill-through from the security dashboard's "Accounts in arrears" stat card — also reachable
 * by finance/admin roles from the Reports page, since they share this same list. */
export function AccountsInArrearsPage() {
  const [result, setResult] = useState<PageResponse<ResidentArrears>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [downloading, setDownloading] = useState(false)
  const debouncedQuery = useDebouncedValue(query)
  const navigate = useNavigate()
  const { user } = useAuth()
  const { openResident, openProperty } = useEntityDetail()
  const back = (user && BACK_DESTINATION[user.role]) ?? { to: '/reports', label: 'Back to reports' }

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

  async function handleDownload() {
    setDownloading(true)
    setError(null)
    try {
      const blob = await residentsApi.arrearsReport()
      downloadBlob(blob, `arrears-report-${new Date().toISOString().slice(0, 10)}.pdf`)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Accounts in arrears</h1>
          <p className="page-subtitle">
            Residents whose outstanding balance is above the enforcement threshold set in Access Policy, biggest balance first.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button className="btn btn-primary" disabled={downloading} onClick={handleDownload}>
            {downloading ? 'Generating…' : 'Download PDF report'}
          </button>
          <button className="btn" onClick={() => navigate(back.to)}>
            {back.label}
          </button>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}

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
