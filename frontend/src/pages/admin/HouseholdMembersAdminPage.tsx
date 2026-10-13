import { useEffect, useState } from 'react'
import { householdMembersApi } from '../../api/endpoints'
import type { HouseholdMember, PageResponse } from '../../api/types'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { apiErrorMessage } from '../../api/client'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const EMPTY: PageResponse<HouseholdMember> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function HouseholdMembersAdminPage() {
  const { openResident } = useEntityDetail()
  const [result, setResult] = useState<PageResponse<HouseholdMember>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  async function load() {
    setLoading(true)
    setResult(await householdMembersApi.listAll({ q: debouncedQuery || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery])

  async function handleRevoke(id: number) {
    setBusyId(id)
    setError(null)
    try {
      await householdMembersApi.revoke(id)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div>
      <div className="page-header" data-tour="/household-members">
        <div>
          <h1>Household Members</h1>
          <p className="page-subtitle">Spouse/family passes, sponsored by a resident and approved by the property's landlord — valid until revoked.</p>
        </div>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search household members by name or phone…" />

      {error && <p className="error-text">{error}</p>}

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(m) => m.id}
        emptyMessage={query ? 'No household members match your search.' : 'No household members yet.'}
        columns={[
          {
            key: 'photo',
            label: '',
            render: (m) =>
              m.photo ? (
                <img src={m.photo} alt="" className="avatar-thumb" />
              ) : (
                <div className="avatar-thumb avatar-thumb-empty" />
              ),
          },
          { key: 'fullName', label: 'Name' },
          { key: 'relationship', label: 'Relationship' },
          {
            key: 'sponsorResidentId',
            label: 'Sponsor',
            render: (m) => (
              <button type="button" className="link-button" onClick={() => openResident(m.sponsorResidentId)}>
                Resident #{m.sponsorResidentId}
              </button>
            ),
          },
          { key: 'propertyHouseNumber', label: 'Property', render: (m) => m.propertyHouseNumber ?? <span className="muted">—</span> },
          { key: 'status', label: 'Status', render: (m) => <StatusBadge value={m.status} /> },
        ]}
        actions={(m) =>
          m.status !== 'REVOKED' && (
            <button className="btn btn-sm" disabled={busyId === m.id} onClick={() => handleRevoke(m.id)}>
              Revoke
            </button>
          )
        }
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
