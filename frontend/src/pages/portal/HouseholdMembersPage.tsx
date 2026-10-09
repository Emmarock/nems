import { useEffect, useState } from 'react'
import { householdMembersApi } from '../../api/endpoints'
import type { HouseholdMember, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { QrCodeModal } from '../../components/QrCodeModal'
import { buildScanUrl } from '../../utils/scanUrl'

const EMPTY: PageResponse<HouseholdMember> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'photo', label: 'Photo', type: 'image', full: true },
  { name: 'fullName', label: 'Full name', required: true, full: true },
  { name: 'phone', label: 'Phone', required: true },
  {
    name: 'relationship',
    label: 'Relationship',
    type: 'select',
    required: true,
    options: [
      { value: 'SPOUSE', label: 'Spouse' },
      { value: 'CHILD', label: 'Child' },
      { value: 'PARENT', label: 'Parent' },
      { value: 'SIBLING', label: 'Sibling' },
      { value: 'OTHER', label: 'Other' },
    ],
  },
]

/**
 * No validity dates here — unlike a Worker request, the property's landlord sets validFrom/
 * validUntil on approval (see HouseholdApprovalsPage), so a pass stays PENDING_APPROVAL with no
 * QR until then.
 */
export function HouseholdMembersPage() {
  const [result, setResult] = useState<PageResponse<HouseholdMember>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [qrMember, setQrMember] = useState<HouseholdMember | null>(null)

  async function load() {
    setLoading(true)
    setResult(await householdMembersApi.mine({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleSubmit(values: Record<string, unknown>) {
    await householdMembersApi.request({
      fullName: values.fullName as string,
      phone: values.phone as string,
      relationship: values.relationship as string,
      photo: (values.photo as string) || undefined,
    })
    setModalOpen(false)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My Household</h1>
          <p className="page-subtitle">
            Request a pass for your spouse or another family member. Your landlord sets how long the pass stays valid.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Request household pass
        </button>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(m) => m.id}
        emptyMessage="No household members requested yet."
        columns={[
          {
            key: 'photo',
            label: '',
            render: (m) =>
              m.photo ? (
                <img src={m.photo} alt="" className="avatar-thumb" />
              ) : (
                <div className="avatar-thumb avatar-thumb-empty" />
              ),
          },
          { key: 'fullName', label: 'Name' },
          { key: 'relationship', label: 'Relationship' },
          { key: 'validFrom', label: 'Valid from', render: (m) => m.validFrom ?? <span className="muted">—</span> },
          { key: 'validUntil', label: 'Valid until', render: (m) => m.validUntil ?? <span className="muted">—</span> },
          { key: 'status', label: 'Status', render: (m) => <StatusBadge value={m.status} /> },
        ]}
        actions={(m) =>
          m.qrToken ? (
            <button className="btn btn-sm" onClick={() => setQrMember(m)}>
              View QR pass
            </button>
          ) : (
            <span className="muted">Pending landlord approval</span>
          )
        }
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal
          title="Request household pass"
          fields={FIELDS}
          onSubmit={handleSubmit}
          onClose={() => setModalOpen(false)}
        />
      )}

      {qrMember && qrMember.qrToken && (
        <QrCodeModal
          title={`Household pass — ${qrMember.fullName}`}
          subtitle={`${qrMember.relationship} · valid through ${qrMember.validUntil}`}
          value={buildScanUrl('household', qrMember.qrToken)}
          fileName={`household-pass-${qrMember.fullName.replace(/\s+/g, '-').toLowerCase()}`}
          onClose={() => setQrMember(null)}
        />
      )}
    </div>
  )
}
