import { useEffect, useState } from 'react'
import { householdMembersApi } from '../../api/endpoints'
import type { HouseholdMember, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { LoadingState } from '../../components/LoadingState'
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

  // Group by the tenant who requested each pass, so the landlord can see whose household it is.
  // The API sorts by sponsor, so a tenant's requests stay together across pages.
  const groups: { sponsorResidentId: number; sponsorName: string | null; sponsorPhone: string | null; members: HouseholdMember[] }[] = []
  for (const m of result.content) {
    let g = groups.find((x) => x.sponsorResidentId === m.sponsorResidentId)
    if (!g) {
      g = { sponsorResidentId: m.sponsorResidentId, sponsorName: m.sponsorResidentName, sponsorPhone: m.sponsorResidentPhone, members: [] }
      groups.push(g)
    }
    g.members.push(m)
  }

  return (
    <div>
      <div className="page-header" data-tour="/portal/household-approvals">
        <div>
          <h1>Household Approvals</h1>
          <p className="page-subtitle">
            Approve a requested household pass to issue its QR code. Once approved, it stays valid until revoked.
          </p>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}

      {loading ? (
        <LoadingState />
      ) : groups.length === 0 ? (
        <div className="empty-state">No household passes awaiting your approval.</div>
      ) : (
        groups.map((g) => (
          <section key={g.sponsorResidentId} className="card" style={{ marginBottom: 16 }}>
            <div style={{ marginBottom: 12 }}>
              <h2 style={{ fontSize: 16, fontWeight: 600, margin: 0 }}>
                Requested by {g.sponsorName ?? `Resident #${g.sponsorResidentId}`}
              </h2>
              <p className="page-subtitle">
                {[g.sponsorPhone, `${g.members.length} pending request${g.members.length === 1 ? '' : 's'}`]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
            </div>
            <DataTable
              rows={g.members}
              rowKey={(m) => m.id}
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
          </section>
        ))
      )}

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
