import { useEffect, useState } from 'react'
import { officeHoldersApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { OfficeHolder, Resident, Role } from '../api/types'
import { DataTable } from './DataTable'
import { ResidentPicker } from './ResidentPicker'

const OFFICE_LABELS: Partial<Record<Role, string>> = {
  SUPER_ADMIN: 'Super Admin',
  CDA_ADMIN: 'CDA Administrator',
  TREASURER: 'Treasurer',
  FINANCIAL_SECRETARY: 'Financial Secretary',
  SECRETARY: 'Secretary',
  PRO: 'PRO (Public Relations Officer)',
  SECURITY: 'Security',
  MAINTENANCE: 'Maintenance',
}

/**
 * Super admin: assign the resident who holds each estate office. Office emails (e.g. a new
 * payment receipt to review) go to the personal email on the office account's profile if one is
 * set, otherwise to this resident's email, otherwise to the office login email.
 */
export function OfficeHoldersSection() {
  const [holders, setHolders] = useState<OfficeHolder[] | null>(null)
  const [editing, setEditing] = useState<Role | null>(null)
  const [picked, setPicked] = useState<Resident | null>(null)
  const [busy, setBusy] = useState<Role | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    try {
      setHolders(await officeHoldersApi.list())
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function run(role: Role, action: () => Promise<unknown>) {
    setBusy(role)
    setError(null)
    try {
      await action()
      setEditing(null)
      setPicked(null)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(null)
    }
  }

  return (
    <section className="card" style={{ marginBottom: 24 }}>
      <h2 style={{ fontSize: 16, fontWeight: 600, margin: 0 }}>Office holders</h2>
      <p className="page-subtitle" style={{ marginBottom: 12 }}>
        The resident who holds each office. Office emails go to the personal email on the office account if set,
        otherwise to this resident's email.
      </p>
      {error && <p className="error-text">{error}</p>}
      <DataTable
        loading={holders === null}
        rows={holders ?? []}
        rowKey={(h) => h.role}
        columns={[
          { key: 'role', label: 'Office', render: (h) => OFFICE_LABELS[h.role] ?? h.role },
          {
            key: 'residentName',
            label: 'Office holder',
            render: (h) =>
              editing === h.role ? (
                <div style={{ minWidth: 240 }}>
                  <ResidentPicker value={picked} onChange={setPicked} />
                </div>
              ) : h.residentName ? (
                <>
                  {h.residentName}
                  {h.propertyHouseNumber && <span className="muted"> · {h.propertyHouseNumber}</span>}
                </>
              ) : (
                <span className="muted">Not assigned</span>
              ),
          },
          {
            key: 'residentEmail',
            label: 'Email used',
            render: (h) =>
              h.residentId == null ? (
                <span className="muted">—</span>
              ) : h.residentEmail ? (
                h.residentEmail
              ) : (
                <span className="error-text" style={{ margin: 0 }}>No email on file</span>
              ),
          },
        ]}
        actions={(h) =>
          editing === h.role ? (
            <div style={{ display: 'flex', gap: 6 }}>
              <button
                className="btn btn-sm btn-primary"
                disabled={!picked || busy === h.role}
                onClick={() => picked && run(h.role, () => officeHoldersApi.assign(h.role, picked.id))}
              >
                {busy === h.role ? 'Saving…' : 'Save'}
              </button>
              <button
                className="btn btn-sm"
                onClick={() => {
                  setEditing(null)
                  setPicked(null)
                }}
              >
                Cancel
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: 6 }}>
              <button
                className="btn btn-sm"
                onClick={() => {
                  setEditing(h.role)
                  setPicked(null)
                }}
              >
                {h.residentId == null ? 'Assign' : 'Change'}
              </button>
              {h.residentId != null && (
                <button className="btn btn-sm" disabled={busy === h.role} onClick={() => run(h.role, () => officeHoldersApi.unassign(h.role))}>
                  Remove
                </button>
              )}
            </div>
          )
        }
      />
    </section>
  )
}
