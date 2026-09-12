import { useEffect, useState } from 'react'
import { leviesApi, meApi } from '../../api/endpoints'
import type { Levy, LevyBalance, PageResponse, Payment } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { LoadingState } from '../../components/LoadingState'

const EMPTY: PageResponse<Payment> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function PaymentsPage() {
  const [breakdown, setBreakdown] = useState<LevyBalance[] | null>(null)
  const [levies, setLevies] = useState<Levy[]>([])
  const [result, setResult] = useState<PageResponse<Payment>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)

  async function load() {
    setLoading(true)
    const [b, l, p] = await Promise.all([
      meApi.balanceBreakdown(),
      leviesApi.list({ size: 100 }),
      meApi.payments({ page, size: 20 }),
    ])
    setBreakdown(b)
    setLevies(l.content.filter((lv) => lv.active))
    setResult(p)
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

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
    await meApi.submitReceipt({
      levyId: Number(values.levyId),
      amount: Number(values.amount),
      method: values.method as string,
      receiptImage: (values.receiptImage as string) || undefined,
    })
    setModalOpen(false)
    await load()
  }

  if (loading && !breakdown) {
    return <LoadingState />
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My Payments</h1>
          <p className="page-subtitle">
            Submit a receipt for any levy — a treasurer or financial secretary reviews it against the bank alert
            before it counts as paid.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Submit payment receipt
        </button>
      </div>

      {breakdown && breakdown.length > 0 && (
        <div className="card" style={{ marginBottom: 20 }}>
          <div className="section-title" style={{ marginTop: 0 }}>
            Balance by levy
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

      <div className="section-title">Payment history</div>
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
          title="Submit payment receipt"
          fields={FIELDS}
          onSubmit={handleSubmit}
          onClose={() => setModalOpen(false)}
        />
      )}
    </div>
  )
}
