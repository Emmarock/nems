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
  const [accounts, setAccounts] = useState<PaymentAccount[]>([])
  const [loading, setLoading] = useState(true)
  const [levyPage, setLevyPage] = useState(0)
  const [invoicePage, setInvoicePage] = useState(0)
  const [levyModal, setLevyModal] = useState<'closed' | 'create' | Levy>('closed')
  const [invoiceModalOpen, setInvoiceModalOpen] = useState(false)
  const [invoiceQuery, setInvoiceQuery] = useState('')
  const debouncedInvoiceQuery = useDebouncedValue(invoiceQuery)
  const { openResident } = useEntityDetail()

  async function load() {
    setLoading(true)
    const [l, i, a] = await Promise.all([
      leviesApi.list({ page: levyPage, size: 20 }),
      invoicesApi.list({ q: debouncedInvoiceQuery || undefined, page: invoicePage, size: 20 }),
      paymentAccountApi.list(),
    ])
    setLevies(l)
    setInvoices(i)
    setAccounts(a)
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [levyPage, invoicePage, debouncedInvoiceQuery])

  useEffect(() => {
    setInvoicePage(0)
  }, [debouncedInvoiceQuery])

  const levyFields = (): FieldConfig[] => [
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
      name: 'paymentAccountId',
      label: 'Pays into',
      type: 'select',
      full: true,
      options: [
        { value: '', label: 'Unassigned' },
        ...accounts.map((a) => ({ value: String(a.id), label: a.label || `Account #${a.id}` })),
      ],
    },
    {
      name: 'vehicleStickerLevy',
      label: 'This is the vehicle sticker levy',
      type: 'checkbox',
      full: true,
    },
  ]

  async function saveLevy(values: Record<string, unknown>) {
    const body = {
      name: values.name as string,
      amount: Number(values.amount),
      frequency: values.frequency as Levy['frequency'],
      active: true,
      vehicleStickerLevy: Boolean(values.vehicleStickerLevy),
      paymentAccountId: values.paymentAccountId ? Number(values.paymentAccountId) : null,
    }
    if (levyModal !== 'closed' && levyModal !== 'create') {
      await leviesApi.update(levyModal.id, body)
    } else {
      await leviesApi.create(body)
    }
    setLevyModal('closed')
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
          <button className="btn btn-primary" onClick={() => setLevyModal('create')}>
            + New levy
          </button>
        </div>
      </div>

      <PaymentAccountsSection onAccountsChanged={load} />

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
            key: 'paymentAccountLabel',
            label: 'Pays into',
            render: (l) => l.paymentAccountLabel ?? <span className="muted">Unassigned</span>,
          },
          {
            key: 'vehicleStickerLevy',
            label: 'Vehicle sticker',
            render: (l) => (l.vehicleStickerLevy ? <StatusBadge value="STICKER LEVY" /> : <span className="muted">—</span>),
          },
        ]}
        actions={(l) => (
          <button className="btn btn-sm" onClick={() => setLevyModal(l)}>
            Edit
          </button>
        )}
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

      {levyModal !== 'closed' && (
        <FormModal
          title={levyModal === 'create' ? 'New levy' : 'Edit levy'}
          fields={levyFields()}
          initial={
            levyModal === 'create'
              ? {}
              : {
                  name: levyModal.name,
                  amount: levyModal.amount,
                  frequency: levyModal.frequency,
                  paymentAccountId: levyModal.paymentAccountId ? String(levyModal.paymentAccountId) : '',
                  vehicleStickerLevy: levyModal.vehicleStickerLevy,
                }
          }
          onSubmit={saveLevy}
          onClose={() => setLevyModal('closed')}
        />
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
 * The estate's payment accounts (e.g. Electricity, Development — there isn't one shared
 * account). Each is under multi-party control: a proposed change or a brand new account only
 * takes effect once two roles OTHER than the proposer's own approve it.
 */
function PaymentAccountsSection({ onAccountsChanged }: { onAccountsChanged: () => Promise<void> }) {
  const [accounts, setAccounts] = useState<PaymentAccount[]>([])
  const [pendingChanges, setPendingChanges] = useState<PaymentAccountChange[]>([])
  const [showNewAccountForm, setShowNewAccountForm] = useState(false)

  async function load() {
    const [accs, pending] = await Promise.all([paymentAccountApi.list(), paymentAccountApi.listPending()])
    setAccounts(accs)
    setPendingChanges(pending)
  }

  useEffect(() => {
    load()
  }, [])

  async function refresh() {
    await Promise.all([load(), onAccountsChanged()])
  }

  const newAccountPending = pendingChanges.find((c) => c.targetAccountId === null)

  return (
    <div style={{ marginBottom: 20 }}>
      <div className="section-title" style={{ marginTop: 0 }}>
        Payment accounts
      </div>
      <p className="muted" style={{ marginTop: -6, marginBottom: 14 }}>
        Each levy pays into one of these — there isn't one shared account. Creating or changing an
        account needs approval from two roles other than whoever proposes it.
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {accounts.map((account) => (
          <AccountPanel
            key={account.id}
            account={account}
            pending={pendingChanges.find((c) => c.targetAccountId === account.id)}
            onChanged={refresh}
          />
        ))}
        {newAccountPending || showNewAccountForm ? (
          <AccountPanel
            account={null}
            pending={newAccountPending}
            onChanged={refresh}
            onCancelNew={() => setShowNewAccountForm(false)}
          />
        ) : (
          <button className="btn" style={{ alignSelf: 'flex-start' }} onClick={() => setShowNewAccountForm(true)}>
            + Propose a new account
          </button>
        )}
      </div>
    </div>
  )
}

function AccountPanel({
  account,
  pending,
  onChanged,
  onCancelNew,
}: {
  account: PaymentAccount | null
  pending: PaymentAccountChange | undefined
  onChanged: () => Promise<void>
  onCancelNew?: () => void
}) {
  const { user } = useAuth()
  const [proposing, setProposing] = useState(false)
  const [form, setForm] = useState({
    label: account?.label ?? '',
    bankName: account?.bankName ?? '',
    accountNumber: account?.accountNumber ?? '',
    accountName: account?.accountName ?? '',
  })
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  function openProposeForm() {
    setForm({
      label: account?.label ?? '',
      bankName: account?.bankName ?? '',
      accountNumber: account?.accountNumber ?? '',
      accountName: account?.accountName ?? '',
    })
    setError(null)
    setProposing(true)
  }

  async function submitProposal(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await paymentAccountApi.propose({ targetAccountId: account?.id, ...form })
      setProposing(false)
      await onChanged()
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
      if (decision === 'approve') await paymentAccountApi.approve(pending.id, notes.trim() || undefined)
      else await paymentAccountApi.reject(pending.id, notes.trim() || undefined)
      setNotes('')
      await onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const myRole = user?.role
  const alreadyDecided = pending && myRole ? pending.approvals.some((a) => a.role === myRole) : false
  const isProposerRole = pending?.proposedByRole === myRole
  const canDecide = !!pending && !isProposerRole && !alreadyDecided

  const formFields = (
    <form onSubmit={submitProposal} style={{ marginTop: account ? 12 : 0 }}>
      <div className="form-grid">
        <div className="form-field full">
          <label htmlFor={`label-${account?.id ?? 'new'}`}>Label (e.g. Electricity, Development)</label>
          <input
            id={`label-${account?.id ?? 'new'}`}
            value={form.label}
            onChange={(e) => setForm({ ...form, label: e.target.value })}
            required
          />
        </div>
        <div className="form-field full">
          <label htmlFor={`bank-${account?.id ?? 'new'}`}>Bank name</label>
          <input
            id={`bank-${account?.id ?? 'new'}`}
            value={form.bankName}
            onChange={(e) => setForm({ ...form, bankName: e.target.value })}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor={`num-${account?.id ?? 'new'}`}>Account number</label>
          <input
            id={`num-${account?.id ?? 'new'}`}
            value={form.accountNumber}
            onChange={(e) => setForm({ ...form, accountNumber: e.target.value })}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor={`name-${account?.id ?? 'new'}`}>Account name</label>
          <input
            id={`name-${account?.id ?? 'new'}`}
            value={form.accountName}
            onChange={(e) => setForm({ ...form, accountName: e.target.value })}
            required
          />
        </div>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
        <button
          type="button"
          className="btn"
          disabled={busy}
          onClick={() => (account ? setProposing(false) : onCancelNew?.())}
        >
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Submitting…' : account ? 'Propose change' : 'Propose account'}
        </button>
      </div>
    </form>
  )

  return (
    <div className="card" style={{ maxWidth: 520 }}>
      <div className="section-title" style={{ marginTop: 0 }}>
        {account ? account.label || `Account #${account.id}` : 'New account'}
      </div>

      {account && !pending && (
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
      )}

      {error && <p className="error-text">{error}</p>}

      {pending ? (
        <div className="info-banner" style={{ marginTop: account ? 12 : 0 }}>
          <p style={{ margin: '0 0 8px' }}>
            <strong>{account ? 'Change pending approval' : 'New account pending approval'}</strong> — proposed by{' '}
            {pending.proposedByUserName ?? 'someone'} ({ROLE_LABELS[pending.proposedByRole] ?? pending.proposedByRole}
            ):
          </p>
          <dl className="detail-list">
            <div className="detail-row">
              <dt>Label</dt>
              <dd>{pending.label}</dd>
            </div>
            <div className="detail-row">
              <dt>Bank</dt>
              <dd>{pending.bankName}</dd>
            </div>
            <div className="detail-row">
              <dt>Account number</dt>
              <dd>{pending.accountNumber}</dd>
            </div>
            <div className="detail-row">
              <dt>Account name</dt>
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
                <label htmlFor={`notes-${pending.id}`}>Notes (optional)</label>
                <input id={`notes-${pending.id}`} value={notes} onChange={(e) => setNotes(e.target.value)} />
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
              {isProposerRole
                ? 'You proposed this — it needs approval from other roles.'
                : 'You have already responded to this.'}
            </p>
          )}
        </div>
      ) : account && !proposing ? (
        <button className="btn btn-primary" style={{ marginTop: 12 }} onClick={openProposeForm}>
          Propose a change
        </button>
      ) : (
        formFields
      )}
    </div>
  )
}
