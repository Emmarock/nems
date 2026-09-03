import { useEffect, useState } from 'react'
import { invoicesApi, leviesApi } from '../../api/endpoints'
import type { Invoice, Levy, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const LEVY_FIELDS: FieldConfig[] = [
  { name: 'name', label: 'Levy name', required: true, full: true },
  { name: 'amount', label: 'Amount (₦)', type: 'number', step: '0.01', required: true },
  {
    name: 'frequency',
    label: 'Frequency',
    type: 'select',
    required: true,
    options: [
      { value: 'ANNUAL', label: 'Annual' },
      { value: 'ONE_TIME', label: 'One-time' },
    ],
  },
]

const INVOICE_FIELDS: FieldConfig[] = [
  { name: 'residentId', label: 'Resident ID', type: 'number', required: true },
  { name: 'levyId', label: 'Levy ID', type: 'number', required: true },
  { name: 'dueDate', label: 'Due date', type: 'date' },
]

const EMPTY_LEVIES: PageResponse<Levy> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
const EMPTY_INVOICES: PageResponse<Invoice> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function LeviesPage() {
  const [levies, setLevies] = useState<PageResponse<Levy>>(EMPTY_LEVIES)
  const [invoices, setInvoices] = useState<PageResponse<Invoice>>(EMPTY_INVOICES)
  const [loading, setLoading] = useState(true)
  const [levyPage, setLevyPage] = useState(0)
  const [invoicePage, setInvoicePage] = useState(0)
  const [levyModalOpen, setLevyModalOpen] = useState(false)
  const [invoiceModalOpen, setInvoiceModalOpen] = useState(false)
  const [invoiceQuery, setInvoiceQuery] = useState('')
  const debouncedInvoiceQuery = useDebouncedValue(invoiceQuery)
  const { openResident } = useEntityDetail()

  async function load() {
    setLoading(true)
    const [l, i] = await Promise.all([
      leviesApi.list({ page: levyPage, size: 20 }),
      invoicesApi.list({ q: debouncedInvoiceQuery || undefined, page: invoicePage, size: 20 }),
    ])
    setLevies(l)
    setInvoices(i)
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [levyPage, invoicePage, debouncedInvoiceQuery])

  useEffect(() => {
    setInvoicePage(0)
  }, [debouncedInvoiceQuery])

  async function createLevy(values: Record<string, unknown>) {
    await leviesApi.create({
      name: values.name as string,
      amount: Number(values.amount),
      frequency: values.frequency as Levy['frequency'],
      active: true,
    })
    setLevyModalOpen(false)
    await load()
  }

  async function generateInvoice(values: Record<string, unknown>) {
    await invoicesApi.generate({
      residentId: Number(values.residentId),
      levyId: Number(values.levyId),
      dueDate: (values.dueDate as string) || undefined,
    })
    setInvoiceModalOpen(false)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Levies &amp; Invoices</h1>
          <p className="page-subtitle">Define estate charges and raise invoices against residents (spec §4).</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn" onClick={() => setInvoiceModalOpen(true)}>
            + Generate invoice
          </button>
          <button className="btn btn-primary" onClick={() => setLevyModalOpen(true)}>
            + New levy
          </button>
        </div>
      </div>

      <div className="section-title">Levies</div>
      <DataTable
        loading={loading}
        rows={levies.content}
        rowKey={(l) => l.id}
        emptyMessage="No levies configured yet."
        columns={[
          { key: 'id', label: 'ID' },
          { key: 'name', label: 'Name' },
          { key: 'amount', label: 'Amount', render: (l) => `₦${l.amount.toLocaleString()}` },
          { key: 'frequency', label: 'Frequency', render: (l) => <StatusBadge value={l.frequency} /> },
          { key: 'active', label: 'Active', render: (l) => (l.active ? 'Yes' : 'No') },
        ]}
      />
      <Pagination page={levies.page} totalPages={levies.totalPages} totalElements={levies.totalElements} onPageChange={setLevyPage} />

      <div className="section-title">Invoices</div>
      <SearchInput value={invoiceQuery} onChange={setInvoiceQuery} placeholder="Search invoices by resident name or description…" />
      <DataTable
        loading={loading}
        rows={invoices.content}
        rowKey={(i) => i.id}
        emptyMessage={invoiceQuery ? 'No invoices match your search.' : 'No invoices issued yet.'}
        columns={[
          { key: 'id', label: 'ID' },
          {
            key: 'residentId',
            label: 'Resident',
            render: (i) => (
              <button type="button" className="link-button" onClick={() => openResident(i.residentId)}>
                {i.residentName ?? `Resident #${i.residentId}`}
              </button>
            ),
          },
          { key: 'description', label: 'Description' },
          { key: 'amount', label: 'Amount', render: (i) => `₦${i.amount.toLocaleString()}` },
          { key: 'dueDate', label: 'Due date' },
          { key: 'status', label: 'Status', render: (i) => <StatusBadge value={i.status} /> },
        ]}
      />
      <Pagination page={invoices.page} totalPages={invoices.totalPages} totalElements={invoices.totalElements} onPageChange={setInvoicePage} />

      {levyModalOpen && (
        <FormModal title="New levy" fields={LEVY_FIELDS} onSubmit={createLevy} onClose={() => setLevyModalOpen(false)} />
      )}
      {invoiceModalOpen && (
        <FormModal
          title="Generate invoice"
          fields={INVOICE_FIELDS}
          onSubmit={generateInvoice}
          onClose={() => setInvoiceModalOpen(false)}
        />
      )}
    </div>
  )
}
