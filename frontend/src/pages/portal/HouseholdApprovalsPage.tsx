import { useEffect, useState } from 'react'
import { householdMembersApi } from '../../api/endpoints'
import type { HouseholdMember, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { Pagination } from '../../components/Pagination'
import { apiErrorMessage } from '../../api/client'

const EMPTY: PageResponse<HouseholdMember> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

/**
 * Landlord self-service: household members tenants on the caller's own property have requested,
 * awaiting the landlord's approval. Approving issues the pass's QR code immediately — there's no
 * validity window to set; it stays valid, same as the home owner's own standing, until revoked.
 */
export function HouseholdApprovalsPage() {
  const [result, setResult] = useState<PageResponse<HouseholdMember>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  async function load() {
    setLoading(true)
    setResult(await householdMembersApi.pendingApproval({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleApprove(id: number) {
    setBusyId(id)
    setError(null)
    try {
      await householdMembersApi.approve(id)
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
          <h1>Household Approvals</h1>
          <p className="page-subtitle">
            Approve a requested household pass to issue its QR code. Once approved, it stays valid until revoked.
          </p>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(m) => m.id}
        emptyMessage="No household passes awaiting your approval."
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
          { key: 'phone', label: 'Phone' },
        ]}
        actions={(m) => (
          <button className="btn btn-sm btn-primary" disabled={busyId === m.id} onClick={() => handleApprove(m.id)}>
            Approve
          </button>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
