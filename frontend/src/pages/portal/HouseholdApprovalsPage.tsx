import { useEffect, useState } from 'react'
import { householdMembersApi } from '../../api/endpoints'
import type { HouseholdMember, HouseholdMemberStatus, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { LoadingState } from '../../components/LoadingState'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { apiErrorMessage } from '../../api/client'

const EMPTY: PageResponse<HouseholdMember> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const STATUS_OPTIONS: { value: HouseholdMemberStatus | ''; label: string; empty: string; noun: string }[] = [
  { value: 'PENDING_APPROVAL', label: 'Awaiting approval', empty: 'No household passes awaiting your approval.', noun: 'pending' },
  { value: 'ACTIVE', label: 'Active', empty: 'No active household passes.', noun: 'active' },
  { value: 'REVOKED', label: 'Revoked', empty: 'No revoked household passes.', noun: 'revoked' },
  { value: '', label: 'All passes', empty: 'Your tenants have no household passes.', noun: '' },
]

/**
 * Landlord self-service: household passes on the caller's own property, grouped by the tenant
 * who requested them. Approving issues the QR; revoking switches it off at the gate (e.g. while
 * the member is travelling); reinstating turns the same QR back on (HouseholdMemberService.reinstate).
 */
export function HouseholdApprovalsPage() {
  const [result, setResult] = useState<PageResponse<HouseholdMember>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<HouseholdMemberStatus | ''>('PENDING_APPROVAL')
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [confirmingId, setConfirmingId] = useState<number | null>(null)

  const option = STATUS_OPTIONS.find((o) => o.value === status) ?? STATUS_OPTIONS[0]

  async function load() {
    setLoading(true)
    setResult(await householdMembersApi.forMyProperty({ status: status || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status])

  async function run(id: number, action: (id: number) => Promise<HouseholdMember>) {
    setBusyId(id)
    setError(null)
    try {
      await action(id)
      setConfirmingId(null)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  function actionsFor(m: HouseholdMember) {
    const busy = busyId === m.id
    if (m.status === 'PENDING_APPROVAL') {
      return (
        <button className="btn btn-sm btn-primary" disabled={busy} onClick={() => run(m.id, householdMembersApi.approve)}>
          {busy ? 'Approving…' : 'Approve'}
        </button>
      )
    }
    if (m.status === 'REVOKED') {
      return (
        <button className="btn btn-sm btn-primary" disabled={busy} onClick={() => run(m.id, householdMembersApi.reinstate)}>
          {busy ? 'Reinstating…' : 'Reinstate'}
        </button>
      )
    }
    // Two-step so a stray tap doesn't lock a family member out of the estate.
    if (confirmingId === m.id) {
      return (
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-sm btn-danger" disabled={busy} onClick={() => run(m.id, householdMembersApi.revoke)}>
            {busy ? 'Revoking…' : 'Confirm revoke'}
          </button>
          <button className="btn btn-sm" disabled={busy} onClick={() => setConfirmingId(null)}>
            Cancel
          </button>
        </div>
      )
    }
    return (
      <button className="btn btn-sm" onClick={() => setConfirmingId(m.id)}>
        Revoke
      </button>
    )
  }

  // Group by the tenant who requested each pass, so the landlord can see whose household it is.
  // The API sorts by sponsor, so a tenant's passes stay together across pages.
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
          <h1>Tenant Households</h1>
          <p className="page-subtitle">
            Approve a requested household pass to issue its QR code. Revoke one to switch it off at the gate — e.g. while
            the member is travelling — and reinstate it when they're back; their existing QR works again.
          </p>
        </div>
      </div>

      <div className="toolbar" style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as HouseholdMemberStatus | '')
            setPage(0)
            setConfirmingId(null)
          }}
          style={{ minWidth: 200 }}
        >
          {STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="error-text">{error}</p>}

      {loading ? (
        <LoadingState />
      ) : groups.length === 0 ? (
        <div className="empty-state">{option.empty}</div>
      ) : (
        groups.map((g) => {
          const count = `${g.members.length}${option.noun ? ` ${option.noun}` : ''} pass${g.members.length === 1 ? '' : 'es'}`
          return (
            <section key={g.sponsorResidentId} className="card" style={{ marginBottom: 16 }}>
              <div style={{ marginBottom: 12 }}>
                <h2 style={{ fontSize: 16, fontWeight: 600, margin: 0 }}>
                  Requested by {g.sponsorName ?? `Resident #${g.sponsorResidentId}`}
                </h2>
                <p className="page-subtitle">{[g.sponsorPhone, count].filter(Boolean).join(' · ')}</p>
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
                  { key: 'status', label: 'Status', render: (m) => <StatusBadge value={m.status} /> },
                ]}
                actions={actionsFor}
              />
            </section>
          )
        })
      )}

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
