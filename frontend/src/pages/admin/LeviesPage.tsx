import { useEffect, useState, type FormEvent } from 'react'
import { invoicesApi, leviesApi, paymentAccountApi } from '../../api/endpoints'
import type { Invoice, Levy, PageResponse, PaymentAccount, PaymentAccountChange } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { apiErrorMessage } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
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
  {
    name: 'vehicleStickerLevy',
    label: 'This is the vehicle sticker levy',
    type: 'checkbox',
    full: true,
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
      vehicleStickerLevy: Boolean(values.vehicleStickerLevy),
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

      <PaymentAccountCard />

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
          {
            key: 'vehicleStickerLevy',
            label: 'Vehicle sticker',
            render: (l) => (l.vehicleStickerLevy ? <StatusBadge value="STICKER LEVY" /> : <span className="muted">—</span>),
          },
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

const ROLE_LABELS: Record<string, string> = {
  SUPER_ADMIN: 'Super Admin',
  CDA_ADMIN: 'CDA Administrator',
  TREASURER: 'Treasurer',
  FINANCIAL_SECRETARY: 'Financial Secretary',
}

/**
 * The one account residents pay any levy into — shown to them on My Payments alongside a
 * warning to only pay an account carrying the estate's name. Under multi-party control: a
 * proposed change only takes effect once two roles OTHER than the proposer's own approve it, so
 * this card is either "propose a change" (no pending change) or "review the pending change"
 * (someone already proposed one) — never a plain edit-and-save form.
 */
function PaymentAccountCard() {
  const { user } = useAuth()
  const [account, setAccount] = useState<PaymentAccount | null>(null)
  const [pending, setPending] = useState<PaymentAccountChange | null>(null)
  const [proposing, setProposing] = useState(false)
  const [form, setForm] = useState<PaymentAccount>({ bankName: '', accountNumber: '', accountName: '' })
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function load() {
    const [acc, pend] = await Promise.all([paymentAccountApi.get(), paymentAccountApi.getPending()])
    setAccount(acc)
    setPending(pend)
  }

  useEffect(() => {
    load()
  }, [])

  function openProposeForm() {
    setForm(account ?? { bankName: '', accountNumber: '', accountName: '' })
    setError(null)
    setProposing(true)
  }

  async function submitProposal(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const change = await paymentAccountApi.propose(form)
      setPending(change)
      setProposing(false)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function decide(decision: 'approve' | 'reject') {
    if (!pending) return
    setBusy(true)
    setError(null)
    try {
      const updated = decision === 'approve' ? await paymentAccountApi.approve(pending.id, notes.trim() || undefined)
        : await paymentAccountApi.reject(pending.id, notes.trim() || undefined)
      setNotes('')
      if (updated.status === 'PENDING') {
        setPending(updated)
      } else {
        await load()
      }
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  if (!account) {
    return null
  }

  const myRole = user?.role
  const alreadyDecided = pending && myRole ? pending.approvals.some((a) => a.role === myRole) : false
  const isProposerRole = pending?.proposedByRole === myRole
  const canDecide = pending && !isProposerRole && !alreadyDecided

  return (
    <div className="card" style={{ marginBottom: 20, maxWidth: 520 }}>
      <div className="section-title" style={{ marginTop: 0 }}>
        Payment account
      </div>
      <p className="muted" style={{ marginTop: -6, marginBottom: 14 }}>
        Shown to every resident as the one account to pay any levy into. Changing it needs
        approval from two roles other than whoever proposes the change.
      </p>

      <dl className="detail-list">
        <div className="detail-row">
          <dt>Bank</dt>
          <dd>{account.bankName || <span className="muted">Not set</span>}</dd>
        </div>
        <div className="detail-row">
          <dt>Account number</dt>
          <dd>{account.accountNumber || <span className="muted">Not set</span>}</dd>
        </div>
        <div className="detail-row">
          <dt>Account name</dt>
          <dd>{account.accountName || <span className="muted">Not set</span>}</dd>
        </div>
      </dl>

      {error && <p className="error-text">{error}</p>}

      {pending ? (
        <div className="info-banner" style={{ marginTop: 12 }}>
          <p style={{ margin: '0 0 8px' }}>
            <strong>Change pending approval</strong> — proposed by {pending.proposedByUserName ?? 'someone'} (
            {ROLE_LABELS[pending.proposedByRole] ?? pending.proposedByRole}):
          </p>
          <dl className="detail-list">
            <div className="detail-row">
              <dt>New bank</dt>
              <dd>{pending.bankName}</dd>
            </div>
            <div className="detail-row">
              <dt>New account number</dt>
              <dd>{pending.accountNumber}</dd>
            </div>
            <div className="detail-row">
              <dt>New account name</dt>
              <dd>{pending.accountName}</dd>
            </div>
          </dl>
          {pending.approvals.length > 0 && (
            <ul className="detail-sublist">
              {pending.approvals.map((a) => (
                <li key={a.role}>
                  {ROLE_LABELS[a.role] ?? a.role} ({a.userName ?? 'unknown'}) — <StatusBadge value={a.decision} />
                </li>
              ))}
            </ul>
          )}
          <p style={{ margin: '8px 0 0' }}>
            {pending.approvalsStillNeeded > 0
              ? `${pending.approvalsStillNeeded} more approval(s) needed before this takes effect.`
              : 'Fully approved.'}
          </p>

          {canDecide ? (
            <div style={{ marginTop: 10 }}>
              <div className="form-field full" style={{ marginBottom: 8 }}>
                <label htmlFor="decisionNotes">Notes (optional)</label>
                <input id="decisionNotes" value={notes} onChange={(e) => setNotes(e.target.value)} />
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-danger" disabled={busy} onClick={() => decide('reject')}>
                  Reject
                </button>
                <button className="btn btn-primary" disabled={busy} onClick={() => decide('approve')}>
                  Approve
                </button>
              </div>
            </div>
          ) : (
            <p className="muted" style={{ marginTop: 10 }}>
              {isProposerRole ? "You proposed this change — it needs approval from other roles." : 'You have already responded to this change.'}
            </p>
          )}
        </div>
      ) : proposing ? (
        <form onSubmit={submitProposal} style={{ marginTop: 12 }}>
          <div className="form-grid">
            <div className="form-field full">
              <label htmlFor="bankName">Bank name</label>
              <input
                id="bankName"
                value={form.bankName}
                onChange={(e) => setForm({ ...form, bankName: e.target.value })}
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="accountNumber">Account number</label>
              <input
                id="accountNumber"
                value={form.accountNumber}
                onChange={(e) => setForm({ ...form, accountNumber: e.target.value })}
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="accountName">Account name</label>
              <input
                id="accountName"
                value={form.accountName}
                onChange={(e) => setForm({ ...form, accountName: e.target.value })}
                required
              />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
            <button type="button" className="btn" onClick={() => setProposing(false)} disabled={busy}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Submitting…' : 'Propose change'}
            </button>
          </div>
        </form>
      ) : (
        <button className="btn btn-primary" style={{ marginTop: 12 }} onClick={openProposeForm}>
          Propose a change
        </button>
      )}
    </div>
  )
}
