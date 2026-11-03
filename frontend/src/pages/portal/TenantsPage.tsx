import { useEffect, useState } from 'react'
import { meApi } from '../../api/endpoints'
import type { PageResponse, Resident, ResidentStatus, TenantAddResult } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { apiErrorMessage } from '../../api/client'

const EMPTY: PageResponse<Resident> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const STATUS_OPTIONS: { value: ResidentStatus | ''; label: string }[] = [
  { value: 'ACTIVE', label: 'Active tenants' },
  { value: 'INACTIVE', label: 'Deactivated / moved out' },
  { value: 'SUSPENDED', label: 'Suspended' },
  { value: '', label: 'All tenants' },
]

const EMAIL_FIELDS: FieldConfig[] = [{ name: 'email', label: 'Correct email address', required: true, full: true }]

const FIELDS: FieldConfig[] = [
  { name: 'fullName', label: 'Tenant full name', required: true, full: true },
  { name: 'phone', label: 'Phone', required: true },
  { name: 'email', label: 'Email' },
  { name: 'emergencyContact', label: 'Emergency contact', full: true },
]

/** Landlord self-service: add/remove tenants living on the caller's own property. */
export function TenantsPage() {
  const [result, setResult] = useState<PageResponse<Resident>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<ResidentStatus | ''>('ACTIVE')
  const [modalOpen, setModalOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [newAccount, setNewAccount] = useState<TenantAddResult | null>(null)
  const [editingEmail, setEditingEmail] = useState<Resident | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setResult(await meApi.tenants({ status: status || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status])

  async function handleSubmit(values: Record<string, unknown>) {
    const result = await meApi.addTenant({
      fullName: values.fullName as string,
      phone: values.phone as string,
      email: (values.email as string) || undefined,
      emergencyContact: (values.emergencyContact as string) || undefined,
    })
    setModalOpen(false)
    setNewAccount(result)
    await load()
  }

  async function handleEmailSubmit(values: Record<string, unknown>) {
    if (!editingEmail) return
    const email = ((values.email as string) ?? '').trim()
    const updated = await meApi.updateTenantEmail(editingEmail.id, email)
    setEditingEmail(null)
    setNotice(`Email updated — a new invite has been sent to ${updated.email}.`)
    await load()
  }

  async function handleDeactivate(id: number) {
    setBusyId(id)
    setError(null)
    try {
      await meApi.deactivateTenant(id)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div>
      <div className="page-header" data-tour="/portal/tenants">
        <div>
          <h1>My Tenants</h1>
          <p className="page-subtitle">Add or remove tenants living on your property.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Add tenant
        </button>
      </div>

      <div className="toolbar" style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as ResidentStatus | '')
            setPage(0)
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
      {notice && <p className="success-text">{notice}</p>}

      {newAccount && (
        <div className="card" style={{ marginBottom: 20 }}>
          <div className="section-title" style={{ marginTop: 0 }}>
            {newAccount.resident.fullName}'s portal login
          </div>
          <p className="muted" style={{ marginTop: 0 }}>
            Shown here once — share these with your tenant now. They'll be asked to set their own password on first login.
          </p>
          <div className="detail-list">
            <div className="detail-row">
              <dt>Login</dt>
              <dd style={{ fontFamily: 'monospace' }}>{newAccount.loginEmail ?? newAccount.loginPhone ?? '—'}</dd>
            </div>
            <div className="detail-row">
              <dt>Temporary password</dt>
              <dd style={{ fontFamily: 'monospace' }}>{newAccount.temporaryPassword ?? '—'}</dd>
            </div>
          </div>
          <div className="modal-footer" style={{ paddingLeft: 0, paddingRight: 0 }}>
            <button type="button" className="btn" onClick={() => setNewAccount(null)}>
              Done
            </button>
          </div>
        </div>
      )}

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(r) => r.id}
        emptyMessage={status === 'ACTIVE' ? 'No active tenants on your property.' : 'No tenants match this filter.'}
        columns={[
          { key: 'fullName', label: 'Name' },
          { key: 'phone', label: 'Phone' },
          {
            key: 'email',
            label: 'Email',
            render: (r) => (
              <>
                {r.email ?? <span className="muted">—</span>}
                {r.status === 'ACTIVE' && r.invitePending && (
                  <div className="muted" style={{ fontSize: 12 }}>
                    Invite pending
                  </div>
                )}
              </>
            ),
          },
          { key: 'status', label: 'Status', render: (r) => <StatusBadge value={r.status} /> },
        ]}
        actions={(r) =>
          r.status === 'ACTIVE' ? (
            <div style={{ display: 'flex', gap: 8 }}>
              {r.invitePending && (
                <button
                  className="btn btn-sm"
                  onClick={() => {
                    setNotice(null)
                    setEditingEmail(r)
                  }}
                >
                  Edit email
                </button>
              )}
              <button className="btn btn-sm" disabled={busyId === r.id} onClick={() => handleDeactivate(r.id)}>
                Deactivate
              </button>
            </div>
          ) : (
            <span className="muted">Inactive</span>
          )
        }
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {editingEmail && (
        <FormModal
          title={`Edit ${editingEmail.fullName}'s email`}
          fields={EMAIL_FIELDS}
          initial={{ email: editingEmail.email ?? '' }}
          submitLabel="Save & resend invite"
          onSubmit={handleEmailSubmit}
          onClose={() => setEditingEmail(null)}
        />
      )}

      {modalOpen && (
        <FormModal title="Add tenant" fields={FIELDS} onSubmit={handleSubmit} onClose={() => setModalOpen(false)} />
      )}
    </div>
  )
}
