import { useEffect, useState } from 'react'
import { householdMembersApi } from '../../api/endpoints'
import type { HouseholdMember, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { Pagination } from '../../components/Pagination'
import { apiErrorMessage } from '../../api/client'

const EMPTY: PageResponse<HouseholdMember> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const APPROVAL_FIELDS: FieldConfig[] = [
  { name: 'validFrom', label: 'Valid from', type: 'date', required: true },
  { name: 'validUntil', label: 'Valid until', type: 'date', required: true },
]

/**
 * Landlord self-service: household members tenants on the caller's own property have requested,
 * awaiting the landlord to set how long the pass stays valid (see HouseholdMemberService.approve).
 */
export function HouseholdApprovalsPage() {
  const [result, setResult] = useState<PageResponse<HouseholdMember>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [approving, setApproving] = useState<HouseholdMember | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setResult(await householdMembersApi.pendingApproval({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleApprove(values: Record<string, unknown>) {
    if (!approving) return
    try {
      await householdMembersApi.approve(approving.id, {
        validFrom: values.validFrom as string,
        validUntil: values.validUntil as string,
      })
      setApproving(null)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Household Approvals</h1>
          <p className="page-subtitle">
            Set how long each requested household pass stays valid. Nothing is issued until you approve it.
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
          <button className="btn btn-sm btn-primary" onClick={() => setApproving(m)}>
            Approve
          </button>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {approving && (
        <FormModal
          title={`Approve pass — ${approving.fullName}`}
          fields={APPROVAL_FIELDS}
          submitLabel="Approve"
          onSubmit={handleApprove}
          onClose={() => setApproving(null)}
        />
      )}
    </div>
  )
}
