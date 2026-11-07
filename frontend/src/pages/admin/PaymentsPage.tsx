import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { apiErrorMessage } from '../../api/client'
import { paymentsApi } from '../../api/endpoints'
import type { PageResponse, Payment, PaymentStatus, Role } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { ReceiptReviewModal } from '../../components/ReceiptReviewModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const EMPTY: PageResponse<Payment> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'residentId', label: 'Resident ID', type: 'number', required: true },
  { name: 'invoiceId', label: 'Invoice ID (optional)', type: 'number' },
  { name: 'amount', label: 'Amount (₦)', type: 'number', step: '0.01', required: true },
  {
    name: 'method',
    label: 'Method',
    type: 'select',
    required: true,
    options: [
      { value: 'CASH', label: 'Cash' },
      { value: 'BANK_TRANSFER', label: 'Bank transfer' },
      { value: 'CHEQUE', label: 'Cheque' },
    ],
  },
]

const STATUS_OPTIONS: { value: PaymentStatus | ''; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'PENDING_APPROVAL', label: 'Pending approval' },
  { value: 'SUCCESS', label: 'Success' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'PENDING', label: 'Pending (gateway)' },
]

/** Offices that can record payments and approve/reject receipts (PaymentController). */
const REVIEWER_ROLES: Role[] = ['SUPER_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECRETARY']

export function PaymentsPage() {
  const { user } = useAuth()
  const canReview = !!user && REVIEWER_ROLES.includes(user.role)
  const canRevert = user?.role === 'SUPER_ADMIN'
  const [searchParams, setSearchParams] = useSearchParams()
  const [linkError, setLinkError] = useState<string | null>(null)
  const [result, setResult] = useState<PageResponse<Payment>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [reviewing, setReviewing] = useState<Payment | null>(null)
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  // The digest email's "See them all" link opens /payments?status=PENDING_APPROVAL.
  const [status, setStatus] = useState<PaymentStatus | ''>(() => {
    const fromLink = new URLSearchParams(window.location.search).get('status')
    return STATUS_OPTIONS.some((o) => o.value === fromLink) ? (fromLink as PaymentStatus) : ''
  })
  const [digestBusy, setDigestBusy] = useState(false)
  const [digestNotice, setDigestNotice] = useState<string | null>(null)
  const debouncedQuery = useDebouncedValue(query)
  const { openResident } = useEntityDetail()

  async function load() {
    setLoading(true)
    setResult(await paymentsApi.list({ q: debouncedQuery || undefined, status: status || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, status, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery, status])

  // "Review it here" link from the new-receipt email: /payments?review=<id> opens that payment.
  const reviewId = Number(searchParams.get('review'))
  useEffect(() => {
    if (!reviewId) return
    setLinkError(null)
    paymentsApi
      .get(reviewId)
      .then(setReviewing)
      .catch((err) => setLinkError(apiErrorMessage(err)))
  }, [reviewId])

  /** Same digest the offices get every morning at 8am, sent immediately. */
  async function sendDigest() {
    setDigestBusy(true)
    setDigestNotice(null)
    setLinkError(null)
    try {
      const { pending } = await paymentsApi.sendPendingDigest()
      setDigestNotice(
        pending === 0
          ? 'No payments are awaiting approval, so nothing was sent.'
          : `Emailed the review offices a list of ${pending} payment${pending === 1 ? '' : 's'} awaiting approval.`,
      )
    } catch (err) {
      setLinkError(apiErrorMessage(err))
    } finally {
      setDigestBusy(false)
    }
  }

  function closeReview() {
    setReviewing(null)
    if (searchParams.has('review')) {
      searchParams.delete('review')
      setSearchParams(searchParams, { replace: true })
    }
  }

  async function handleSubmit(values: Record<string, unknown>) {
    await paymentsApi.recordManual({
      residentId: Number(values.residentId),
      invoiceId: values.invoiceId ? Number(values.invoiceId) : undefined,
      amount: Number(values.amount),
      method: values.method as string,
    })
    setModalOpen(false)
    await load()
  }

  return (
    <div>
      <div className="page-header" data-tour="/payments">
        <div>
          <h1>Payments</h1>
          <p className="page-subtitle">
            Back-office payments and resident-submitted receipts awaiting review.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {canRevert && (
            <button className="btn" disabled={digestBusy} onClick={sendDigest}>
              {digestBusy ? 'Sending…' : 'Email pending payments now'}
            </button>
          )}
          {canReview && (
            <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
              + Record payment
            </button>
          )}
        </div>
      </div>

      <div className="toolbar" style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
        <SearchInput value={query} onChange={setQuery} placeholder="Search payments by resident name or provider reference…" />
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as PaymentStatus | '')}
          style={{ minWidth: 180 }}
        >
          {STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      {linkError && <p className="error-text">{linkError}</p>}
      {digestNotice && <p className="success-text">{digestNotice}</p>}

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(p) => p.id}
        emptyMessage={query || status ? 'No payments match this filter.' : 'No payments recorded yet.'}
        columns={[
          { key: 'id', label: 'ID' },
          {
            key: 'residentId',
            label: 'Resident',
            render: (p) => (
              <button type="button" className="link-button" onClick={() => openResident(p.residentId)}>
                {p.residentName ?? `Resident #${p.residentId}`}
              </button>
            ),
          },
          { key: 'amount', label: 'Amount', render: (p) => `₦${p.amount.toLocaleString()}` },
          { key: 'method', label: 'Method', render: (p) => <StatusBadge value={p.method} /> },
          { key: 'status', label: 'Status', render: (p) => <StatusBadge value={p.status} /> },
          {
            key: 'approvedByUserName',
            label: 'Approved by',
            render: (p) => p.approvedByUserName ?? <span className="muted">—</span>,
          },
          { key: 'paidAt', label: 'Paid at', render: (p) => new Date(p.paidAt).toLocaleString() },
        ]}
        actions={(p) =>
          p.status === 'PENDING_APPROVAL' && canReview ? (
            <button className="btn btn-sm btn-primary" onClick={() => setReviewing(p)}>
              Review
            </button>
          ) : p.receiptImage || p.status === 'PENDING_APPROVAL' || (canRevert && (p.status === 'SUCCESS' || p.status === 'REJECTED')) ? (
            <button className="btn btn-sm" onClick={() => setReviewing(p)}>
              {p.receiptImage ? 'View receipt' : 'View'}
            </button>
          ) : null
        }
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal title="Record payment" fields={FIELDS} onSubmit={handleSubmit} onClose={() => setModalOpen(false)} />
      )}

      {reviewing && (
        <ReceiptReviewModal
          payment={reviewing}
          onClose={closeReview}
          onDone={load}
          canDecide={canReview}
          canRevert={canRevert}
        />
      )}
    </div>
  )
}
