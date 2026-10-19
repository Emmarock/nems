import { useEffect, useState } from 'react'
import { usersApi } from '../../api/endpoints'
import type { PageResponse, User } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { NewUserModal } from '../../components/NewUserModal'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { ResetPasswordModal } from '../../components/ResetPasswordModal'
import { BulkCreateResidentAccountsModal } from '../../components/BulkCreateResidentAccountsModal'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { apiErrorMessage } from '../../api/client'

function inviteStatus(user: User): string | null {
  if (!user.mustChangePassword) return null
  return user.inviteLinkExpired ? 'EXPIRED' : 'PENDING'
}

const EMPTY: PageResponse<User> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function UsersPage() {
  const [result, setResult] = useState<PageResponse<User>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)
  const [resetTarget, setResetTarget] = useState<User | null>(null)
  const [bulkCreateOpen, setBulkCreateOpen] = useState(false)
  const [resendingId, setResendingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [resentLink, setResentLink] = useState<{ userName: string; resetUrl: string } | null>(null)
  const [copied, setCopied] = useState(false)

  async function load() {
    setLoading(true)
    setResult(await usersApi.list({ q: debouncedQuery || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery])

  async function toggleStatus(user: User) {
    await usersApi.setStatus(user.id, user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')
    await load()
  }

  async function resendInvite(user: User) {
    setResendingId(user.id)
    setError(null)
    try {
      const { resetUrl } = await usersApi.resendInvite(user.id)
      setResentLink({ userName: user.fullName, resetUrl })
      setCopied(false)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setResendingId(null)
    }
  }

  async function copyResentLink() {
    if (!resentLink) return
    try {
      await navigator.clipboard.writeText(resentLink.resetUrl)
      setCopied(true)
    } catch {
      // Clipboard access can fail (permissions, insecure context) - the link is still shown as
      // plain text, so the admin can select and copy it by hand either way.
    }
  }

  return (
    <div>
      <div className="page-header" data-tour="/users">
        <div>
          <h1>Users</h1>
          <p className="page-subtitle">Staff and resident login accounts (Super Admin only).</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn" onClick={() => setBulkCreateOpen(true)}>
            Bulk-create resident accounts
          </button>
          <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
            + New user
          </button>
        </div>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search users by name, email, role…" />

      {error && <p className="error-text">{error}</p>}

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(u) => u.id}
        emptyMessage={query ? 'No users match your search.' : 'No users yet.'}
        columns={[
          { key: 'fullName', label: 'Name' },
          { key: 'email', label: 'Email' },
          { key: 'phone', label: 'Phone', render: (u) => u.phone ?? <span className="muted">—</span> },
          { key: 'role', label: 'Role', render: (u) => <StatusBadge value={u.role} /> },
          { key: 'status', label: 'Status', render: (u) => <StatusBadge value={u.status} /> },
          { key: 'invite', label: 'Invite', render: (u) => <StatusBadge value={inviteStatus(u)} /> },
        ]}
        actions={(u) => (
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-sm" onClick={() => setResetTarget(u)}>
              Reset password
            </button>
            <button className="btn btn-sm" onClick={() => toggleStatus(u)}>
              {u.status === 'ACTIVE' ? 'Disable' : 'Enable'}
            </button>
            {u.mustChangePassword && (
              <button className="btn btn-sm" disabled={resendingId === u.id} onClick={() => resendInvite(u)}>
                {resendingId === u.id ? 'Sending…' : 'Resend invite'}
              </button>
            )}
          </div>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && <NewUserModal onClose={() => setModalOpen(false)} onCreated={load} />}

      {resetTarget && (
        <ResetPasswordModal userId={resetTarget.id} userName={resetTarget.fullName} onClose={() => setResetTarget(null)} />
      )}

      {bulkCreateOpen && (
        <BulkCreateResidentAccountsModal onClose={() => setBulkCreateOpen(false)} onDone={load} />
      )}

      {resentLink && (
        <div className="modal-overlay" onClick={() => setResentLink(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 560 }}>
            <div className="modal-header">
              <h2>Invite resent — {resentLink.userName}</h2>
              <button type="button" className="btn btn-sm" onClick={() => setResentLink(null)}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              <p className="muted" style={{ marginTop: 0 }}>
                A fresh link was sent on every channel this account has (email/SMS/WhatsApp). If none of those
                are actually configured yet, it's shown here once, unredacted, so you can share it yourself —
                it can't be retrieved again after you close this.
              </p>
              <div style={{ display: 'flex', gap: 8 }}>
                <input type="text" readOnly value={resentLink.resetUrl} style={{ fontFamily: 'monospace', fontSize: 12 }} />
                <button type="button" className="btn btn-sm" onClick={copyResentLink}>
                  {copied ? 'Copied!' : 'Copy link'}
                </button>
              </div>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-primary" onClick={() => setResentLink(null)}>
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
