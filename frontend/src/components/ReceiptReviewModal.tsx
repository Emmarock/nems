import { useState } from 'react'
import { paymentsApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Payment } from '../api/types'
import { StatusBadge } from './StatusBadge'

/** What a reviewer (treasurer/financial secretary/secretary) sees to decide a resident-submitted
 * receipt: the claimed amount/method, the receipt image itself (if one was attached), and an
 * approve/reject decision with an optional note - recorded as Payment.reviewNotes either way.
 * The first decision is final; only the super admin can revert it (canRevert). Offices that can
 * view but not decide (CDA admin, PRO) see it read-only (canDecide false). */
export function ReceiptReviewModal({
  payment,
  onClose,
  onDone,
  canDecide = true,
  canRevert = false,
}: {
  payment: Payment
  onClose: () => void
  onDone: () => Promise<void>
  canDecide?: boolean
  canRevert?: boolean
}) {
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState<'approve' | 'reject' | 'revert' | null>(null)
  const decided = payment.status !== 'PENDING_APPROVAL'
  const revertable = canRevert && (payment.status === 'SUCCESS' || payment.status === 'REJECTED')

  async function act(action: 'approve' | 'reject' | 'revert') {
    setSubmitting(action)
    setError(null)
    try {
      if (action === 'approve') await paymentsApi.approve(payment.id, notes.trim() || undefined)
      else if (action === 'reject') await paymentsApi.reject(payment.id, notes.trim() || undefined)
      else await paymentsApi.revert(payment.id, notes.trim() || undefined)
      await onDone()
      onClose()
    } catch (err) {
      setError(apiErrorMessage(err))
      setSubmitting(null)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Review payment receipt</h2>
          <button type="button" className="btn btn-sm" onClick={onClose}>
            ✕
          </button>
        </div>
        <div className="modal-body">
          {error && <p className="error-text">{error}</p>}
          <div className="detail-list">
            <div className="detail-row">
              <dt>Resident</dt>
              <dd>{payment.residentName ?? `Resident #${payment.residentId}`}</dd>
            </div>
            <div className="detail-row">
              <dt>Amount claimed</dt>
              <dd>₦{payment.amount.toLocaleString()}</dd>
            </div>
            <div className="detail-row">
              <dt>Method</dt>
              <dd>{payment.method.replace(/_/g, ' ')}</dd>
            </div>
            <div className="detail-row">
              <dt>Submitted</dt>
              <dd>{new Date(payment.paidAt).toLocaleString()}</dd>
            </div>
          </div>

          {payment.receiptImage ? (
            <img
              src={payment.receiptImage}
              alt="Submitted payment receipt"
              style={{ maxWidth: '100%', marginTop: 16, borderRadius: 8, border: '1px solid var(--color-border)' }}
            />
          ) : (
            <p className="muted" style={{ marginTop: 16 }}>
              No receipt image was attached — confirm the bank alert some other way before deciding.
            </p>
          )}

          {decided ? (
            <div className="detail-list" style={{ marginTop: 16 }}>
              <div className="detail-row">
                <dt>Decision</dt>
                <dd>
                  <StatusBadge value={payment.status} />
                </dd>
              </div>
              <div className="detail-row">
                <dt>Approved by</dt>
                <dd>{payment.approvedByUserName ?? <span className="muted">—</span>}</dd>
              </div>
              {payment.reviewNotes && (
                <div className="detail-row">
                  <dt>Notes</dt>
                  <dd>{payment.reviewNotes}</dd>
                </div>
              )}
              {(payment.status === 'SUCCESS' || payment.status === 'REJECTED') && (
                <p className="muted" style={{ fontSize: 12, margin: '8px 0 0' }}>
                  This decision is final. Only the super admin can send it back for review.
                </p>
              )}
              {revertable && (
                <div className="form-field full" style={{ marginTop: 12 }}>
                  <label htmlFor="revertNotes">Reason for reverting (optional)</label>
                  <textarea id="revertNotes" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
                </div>
              )}
            </div>
          ) : !canDecide ? (
            <p className="muted" style={{ marginTop: 16 }}>
              Awaiting approval by the treasurer, financial secretary or secretary.
            </p>
          ) : (
            <div className="form-field full" style={{ marginTop: 16 }}>
              <label htmlFor="reviewNotes">Notes (optional — shown to the resident)</label>
              <textarea
                id="reviewNotes"
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="e.g. Confirmed against bank alert dated..."
              />
            </div>
          )}
        </div>
        <div className="modal-footer">
          <button type="button" className="btn" onClick={onClose} disabled={submitting !== null}>
            {decided ? 'Close' : 'Cancel'}
          </button>
          {revertable && (
            <button type="button" className="btn btn-danger" disabled={submitting !== null} onClick={() => act('revert')}>
              {submitting === 'revert' ? 'Reverting…' : 'Revert to awaiting approval'}
            </button>
          )}
          {!decided && canDecide && (
            <>
              <button type="button" className="btn btn-danger" disabled={submitting !== null} onClick={() => act('reject')}>
                {submitting === 'reject' ? 'Rejecting…' : 'Reject'}
              </button>
              <button type="button" className="btn btn-primary" disabled={submitting !== null} onClick={() => act('approve')}>
                {submitting === 'approve' ? 'Approving…' : 'Approve'}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
