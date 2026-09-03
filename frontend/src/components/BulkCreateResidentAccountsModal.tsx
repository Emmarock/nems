import { useState } from 'react'
import { usersApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { BulkCreateResidentUsersResult } from '../api/types'

function csvEscape(value: string): string {
  if (/[",\n]/.test(value)) {
    return '"' + value.replace(/"/g, '""') + '"'
  }
  return value
}

function downloadCsv(result: BulkCreateResidentUsersResult) {
  const rows = [
    ['Resident', 'Login email', 'Temporary password'],
    ...result.created.map((a) => [a.fullName, a.email, a.temporaryPassword]),
  ]
  const csv = rows.map((row) => row.map(csvEscape).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `resident-login-credentials-${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * Creates a RESIDENT login for every resident who doesn't already have one, each with a random
 * temporary password (mustChangePassword=true, forced reset on first login - see
 * ForcePasswordChangePage). The plaintext passwords are only ever shown here, once, for export.
 */
export function BulkCreateResidentAccountsModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [phase, setPhase] = useState<'confirm' | 'result'>('confirm')
  const [result, setResult] = useState<BulkCreateResidentUsersResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleConfirm() {
    setSubmitting(true)
    setError(null)
    try {
      const created = await usersApi.bulkCreateResidents()
      setResult(created)
      setPhase('result')
      onDone()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 640 }}>
        <div className="modal-header">
          <h2>Bulk-create resident accounts</h2>
          <button type="button" className="btn btn-sm" onClick={onClose}>
            ✕
          </button>
        </div>

        {phase === 'confirm' && (
          <>
            <div className="modal-body">
              <p className="muted" style={{ marginTop: 0 }}>
                Creates a login for every resident who doesn't already have one. Each account gets a random
                temporary password and must be changed on first login. Residents who already have an account are
                skipped.
              </p>
              {error && <p className="error-text">{error}</p>}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn" onClick={onClose}>
                Cancel
              </button>
              <button type="button" className="btn btn-primary" disabled={submitting} onClick={handleConfirm}>
                {submitting ? 'Creating…' : 'Create accounts'}
              </button>
            </div>
          </>
        )}

        {phase === 'result' && result && (
          <>
            <div className="modal-body">
              <p style={{ marginTop: 0 }}>
                Created <strong>{result.created.length}</strong> account{result.created.length === 1 ? '' : 's'}.
                {result.alreadyHadAccount > 0 && (
                  <> {result.alreadyHadAccount} resident{result.alreadyHadAccount === 1 ? '' : 's'} already had one, skipped.</>
                )}
              </p>
              {result.created.length > 0 && (
                <>
                  <p className="muted">
                    Temporary passwords are shown here once and can't be retrieved again — download or copy them now.
                  </p>
                  <div className="table-wrap" style={{ maxHeight: 320, overflowY: 'auto' }}>
                    <table>
                      <thead>
                        <tr>
                          <th>Resident</th>
                          <th>Login email</th>
                          <th>Temporary password</th>
                        </tr>
                      </thead>
                      <tbody>
                        {result.created.map((a) => (
                          <tr key={a.residentId}>
                            <td>{a.fullName}</td>
                            <td style={{ fontFamily: 'monospace' }}>{a.email}</td>
                            <td style={{ fontFamily: 'monospace' }}>{a.temporaryPassword}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>
            <div className="modal-footer">
              {result.created.length > 0 && (
                <button type="button" className="btn" onClick={() => downloadCsv(result)}>
                  Download CSV
                </button>
              )}
              <button type="button" className="btn btn-primary" onClick={onClose}>
                Done
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
