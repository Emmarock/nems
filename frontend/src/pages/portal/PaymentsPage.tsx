import { useEffect, useState } from 'react'
import { leviesApi, meApi, paymentAccountApi } from '../../api/endpoints'
import type { Levy, LevyBalance, PageResponse, Payment, PaymentAccount, SharedReceipt } from '../../api/types'
import { isLandlord, useAuth } from '../../auth/AuthContext'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { LoadingState } from '../../components/LoadingState'
import { apiErrorMessage } from '../../api/client'

const EMPTY: PageResponse<Payment> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
const EMPTY_SHARED: PageResponse<SharedReceipt> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

/**
 * One route for every resident, branched by isLandlord(user): billing lives on the landlord's
 * account (see AccountService — it's purely resident-id-keyed, with "the resident who's billed"
 * decided entirely by estate-office convention), so a tenant sees their LANDLORD's balance and
 * payment history, read-only, and hands over a receipt instead of submitting one directly (see
 * PaymentService.submitReceipt's TENANT guard). A landlord sees their own account, exactly as
 * before, plus a queue of receipts their tenants have shared for review.
 */
export function PaymentsPage() {
  const { user } = useAuth()
  const landlord = isLandlord(user)

  const [accounts, setAccounts] = useState<PaymentAccount[]>([])
  const [breakdown, setBreakdown] = useState<LevyBalance[] | null>(null)
  const [levies, setLevies] = useState<Levy[]>([])
  const [result, setResult] = useState<PageResponse<Payment>>(EMPTY)
  const [shared, setShared] = useState<PageResponse<SharedReceipt>>(EMPTY_SHARED)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  async function load() {
    setLoading(true)
    const [accs, b, l, p, s] = await Promise.all([
      paymentAccountApi.list(),
      landlord ? meApi.balanceBreakdown() : meApi.landlordBalanceBreakdown(),
      leviesApi.list({ size: 100 }),
      landlord ? meApi.payments({ page, size: 20 }) : meApi.landlordPayments({ page, size: 20 }),
      landlord ? meApi.sharedReceipts({ size: 20 }) : Promise.resolve(EMPTY_SHARED),
    ])
    setAccounts(accs)
    setBreakdown(b)
    setLevies(l.content.filter((lv) => lv.active))
    setResult(p)
    setShared(s)
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, landlord])

  const FIELDS: FieldConfig[] = [
    {
      name: 'levyId',
      label: 'Which levy is this for?',
      type: 'select',
      required: true,
      full: true,
      options: levies.map((l) => ({ value: String(l.id), label: `${l.name} (₦${l.amount.toLocaleString()})` })),
    },
    { name: 'amount', label: 'Amount paid (₦)', type: 'number', step: '0.01', required: true },
    {
      name: 'method',
      label: 'How did you pay?',
      type: 'select',
      required: true,
      options: [
        { value: 'BANK_TRANSFER', label: 'Bank transfer' },
        { value: 'CASH', label: 'Cash' },
        { value: 'CHEQUE', label: 'Cheque' },
      ],
    },
    { name: 'receiptImage', label: 'Receipt / bank alert photo', type: 'image', full: true },
  ]

  async function handleSubmit(values: Record<string, unknown>) {
    const body = {
      levyId: Number(values.levyId),
      amount: Number(values.amount),
      method: values.method as string,
      receiptImage: (values.receiptImage as string) || undefined,
    }
    if (landlord) {
      await meApi.submitReceipt(body)
    } else {
      await meApi.shareReceipt(body)
    }
    setModalOpen(false)
    await load()
  }

  async function handleSubmitShared(id: number) {
    setBusyId(id)
    setError(null)
    try {
      await meApi.submitSharedReceipt(id)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  async function handleDismissShared(id: number) {
    setBusyId(id)
    setError(null)
    try {
      await meApi.dismissSharedReceipt(id)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  if (loading && !breakdown) {
    return <LoadingState />
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{landlord ? 'My Payments' : "My Landlord's Payments"}</h1>
          <p className="page-subtitle">
            {landlord
              ? 'Submit a receipt for any levy — a treasurer or financial secretary reviews it against the bank alert before it counts as paid.'
              : "Billing is on your landlord's account. Pay as usual, then share your receipt here — your landlord reviews and submits it from their own account."}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          {landlord ? '+ Submit payment receipt' : '+ Share receipt with landlord'}
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}

      {accounts.length > 0 && (
        <div className="card" style={{ marginBottom: 20 }}>
          <div className="section-title" style={{ marginTop: 0 }}>
            Where to pay
          </div>
          <p className="muted" style={{ marginTop: -6, marginBottom: 14 }}>
            Different levies go into different accounts — check which one applies before you pay.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {accounts.map((acc) => {
              const leviesForAccount = levies.filter((lv) => lv.paymentAccountId === acc.id)
              return (
                <div key={acc.id}>
                  <div style={{ fontWeight: 600, marginBottom: 6 }}>{acc.label || `Account #${acc.id}`}</div>
                  {acc.accountNumber ? (
                    <dl className="detail-list">
                      <div className="detail-row">
                        <dt>Bank</dt>
                        <dd>{acc.bankName}</dd>
                      </div>
                      <div className="detail-row">
                        <dt>Account number</dt>
                        <dd>{acc.accountNumber}</dd>
                      </div>
                      <div className="detail-row">
                        <dt>Account name</dt>
                        <dd>{acc.accountName}</dd>
                      </div>
                    </dl>
                  ) : (
                    <p className="muted">Not yet published — contact the estate office.</p>
                  )}
                  {leviesForAccount.length > 0 && (
                    <p className="muted" style={{ margin: '6px 0 0' }}>
                      Used for: {leviesForAccount.map((lv) => lv.name).join(', ')}
                    </p>
                  )}
                </div>
              )
            })}
          </div>
          <div className="info-banner" style={{ marginTop: 14, marginBottom: 0 }}>
            ⚠️ Only pay into an account that carries the estate's name. Do not send money to any other account, even
            if someone claims to be from the estate.
          </div>
        </div>
      )}

      {breakdown && breakdown.length > 0 && (
        <div className="card" style={{ marginBottom: 20 }}>
          <div className="section-title" style={{ marginTop: 0 }}>
            {landlord ? 'Balance by levy' : "Your landlord's balance by levy"}
          </div>
          <dl className="detail-list">
            {breakdown.map((b) => (
              <div className="detail-row" key={b.levyId}>
                <dt>{b.levyName}</dt>
                <dd>
                  ₦{b.outstanding.toLocaleString()} outstanding
                  <span className="muted">
                    {' '}
                    (₦{b.totalDue.toLocaleString()} due · ₦{b.totalPaid.toLocaleString()} paid)
                  </span>
                </dd>
              </div>
            ))}
          </dl>
        </div>
      )}

      {landlord && shared.content.length > 0 && (
        <div className="card" style={{ marginBottom: 20 }}>
          <div className="section-title" style={{ marginTop: 0 }}>
            Receipts shared by your tenants
          </div>
          <DataTable
            loading={false}
            rows={shared.content}
            rowKey={(r) => r.id}
            emptyMessage="No shared receipts awaiting review."
            columns={[
              { key: 'tenantName', label: 'Tenant', render: (r) => r.tenantName ?? <span className="muted">—</span> },
              { key: 'levyName', label: 'Levy', render: (r) => r.levyName ?? <span className="muted">—</span> },
              { key: 'amount', label: 'Amount', render: (r) => `₦${r.amount.toLocaleString()}` },
              { key: 'method', label: 'Method', render: (r) => <StatusBadge value={r.method} /> },
              { key: 'createdAt', label: 'Shared', render: (r) => new Date(r.createdAt).toLocaleString() },
            ]}
            actions={(r) => (
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                <button
                  className="btn btn-sm btn-primary"
                  disabled={busyId === r.id}
                  onClick={() => handleSubmitShared(r.id)}
                >
                  Submit
                </button>
                <button className="btn btn-sm" disabled={busyId === r.id} onClick={() => handleDismissShared(r.id)}>
                  Dismiss
                </button>
              </div>
            )}
          />
        </div>
      )}

      <div className="section-title">{landlord ? 'Payment history' : "Your landlord's payment history"}</div>
      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(p) => p.id}
        emptyMessage="No payments submitted yet."
        columns={[
          { key: 'amount', label: 'Amount', render: (p) => `₦${p.amount.toLocaleString()}` },
          { key: 'method', label: 'Method', render: (p) => <StatusBadge value={p.method} /> },
          { key: 'status', label: 'Status', render: (p) => <StatusBadge value={p.status} /> },
          { key: 'paidAt', label: 'Submitted', render: (p) => new Date(p.paidAt).toLocaleString() },
          { key: 'reviewNotes', label: 'Note', render: (p) => p.reviewNotes ?? <span className="muted">—</span> },
        ]}
      />
      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal
          title={landlord ? 'Submit payment receipt' : 'Share receipt with your landlord'}
          fields={FIELDS}
          onSubmit={handleSubmit}
          onClose={() => setModalOpen(false)}
        />
      )}
    </div>
  )
}
