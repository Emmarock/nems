import { useEffect, useState } from 'react'
import { paymentsApi } from '../../api/endpoints'
import type { PageResponse, Payment } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
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

export function PaymentsPage() {
  const [result, setResult] = useState<PageResponse<Payment>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query)
  const { openResident } = useEntityDetail()

  async function load() {
    setLoading(true)
    setResult(await paymentsApi.list({ q: debouncedQuery || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery])

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
      <div className="page-header">
        <div>
          <h1>Payments</h1>
          <p className="page-subtitle">Back-office payments recorded by Treasury (spec §5).</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Record payment
        </button>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search payments by resident name or provider reference…" />

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(p) => p.id}
        emptyMessage={query ? 'No payments match your search.' : 'No payments recorded yet.'}
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
          { key: 'provider', label: 'Provider' },
          { key: 'status', label: 'Status', render: (p) => <StatusBadge value={p.status} /> },
          { key: 'paidAt', label: 'Paid at', render: (p) => new Date(p.paidAt).toLocaleString() },
        ]}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal title="Record payment" fields={FIELDS} onSubmit={handleSubmit} onClose={() => setModalOpen(false)} />
      )}
    </div>
  )
}
