import { useEffect, useState } from 'react'
import { propertiesApi } from '../../api/endpoints'
import type { PageResponse, Property } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { QrCodeModal } from '../../components/QrCodeModal'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { buildScanUrl } from '../../utils/scanUrl'

const EMPTY: PageResponse<Property> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'block', label: 'Block', required: true },
  { name: 'plot', label: 'Plot', required: true },
  { name: 'houseNumber', label: 'House number', required: true },
  { name: 'address', label: 'Address', required: true, full: true },
  {
    name: 'propertyType',
    label: 'Property type',
    type: 'select',
    required: true,
    options: [
      { value: 'DETACHED_HOUSE', label: 'Detached house' },
      { value: 'SEMI_DETACHED', label: 'Semi-detached' },
      { value: 'TERRACE', label: 'Terrace' },
      { value: 'BUNGALOW', label: 'Bungalow' },
      { value: 'APARTMENT', label: 'Apartment' },
      { value: 'VACANT_LAND', label: 'Vacant land' },
    ],
  },
  {
    name: 'occupancyStatus',
    label: 'Occupancy status',
    type: 'select',
    options: [
      { value: 'OCCUPIED', label: 'Occupied' },
      { value: 'VACANT', label: 'Vacant' },
      { value: 'UNDER_CONSTRUCTION', label: 'Under construction' },
    ],
  },
  { name: 'ownerId', label: 'Owner resident ID', type: 'number' },
]

export function PropertiesPage() {
  const [result, setResult] = useState<PageResponse<Property>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Property | null>(null)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)
  const { openResident } = useEntityDetail()
  const [qrProperty, setQrProperty] = useState<Property | null>(null)
  const [qrToken, setQrToken] = useState<string | null>(null)

  async function openAccessPass(p: Property) {
    setQrProperty(p)
    setQrToken(await propertiesApi.accessPass(p.id))
  }

  async function load() {
    setLoading(true)
    setResult(await propertiesApi.list({ q: debouncedQuery || undefined, page, size: 20 }))
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
    const body = {
      block: values.block as string,
      plot: values.plot as string,
      houseNumber: values.houseNumber as string,
      address: values.address as string,
      propertyType: values.propertyType as Property['propertyType'],
      occupancyStatus: (values.occupancyStatus as Property['occupancyStatus']) || 'VACANT',
      ownerId: values.ownerId ? Number(values.ownerId) : null,
    }
    if (editing) {
      await propertiesApi.update(editing.id, body)
    } else {
      await propertiesApi.create(body)
    }
    setModalOpen(false)
    setEditing(null)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Properties</h1>
          <p className="page-subtitle">The estate's property registry (spec §2).</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setEditing(null); setModalOpen(true) }}>
          + New property
        </button>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search properties by house, block, plot, address…" />

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(p) => p.id}
        emptyMessage={query ? 'No properties match your search.' : 'No properties yet.'}
        columns={[
          { key: 'houseNumber', label: 'House' },
          { key: 'block', label: 'Block' },
          { key: 'plot', label: 'Plot' },
          { key: 'propertyType', label: 'Type', render: (p) => <StatusBadge value={p.propertyType} /> },
          { key: 'occupancyStatus', label: 'Occupancy', render: (p) => <StatusBadge value={p.occupancyStatus} /> },
          {
            key: 'ownerId',
            label: 'Owner',
            render: (p) =>
              p.ownerId ? (
                <button type="button" className="link-button" onClick={() => openResident(p.ownerId!)}>
                  {p.ownerName ?? `Resident #${p.ownerId}`}
                </button>
              ) : (
                <span className="muted">—</span>
              ),
          },
        ]}
        actions={(p) => (
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-sm" onClick={() => openAccessPass(p)}>
              View QR pass
            </button>
            <button className="btn btn-sm" onClick={() => { setEditing(p); setModalOpen(true) }}>
              Edit
            </button>
          </div>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal
          title={editing ? 'Edit property' : 'New property'}
          fields={FIELDS}
          initial={editing ?? {}}
          onSubmit={handleSubmit}
          onClose={() => { setModalOpen(false); setEditing(null) }}
        />
      )}

      {qrProperty && qrToken && (
        <QrCodeModal
          title={`Building pass — ${qrProperty.houseNumber}`}
          subtitle={`${qrProperty.block}, ${qrProperty.plot}`}
          value={buildScanUrl('property', qrToken)}
          fileName={`building-pass-${qrProperty.houseNumber.replace(/\s+/g, '-').toLowerCase()}`}
          helpText="Scan this at the building during enforcement rounds to pull up its payment history."
          onClose={() => { setQrProperty(null); setQrToken(null) }}
        />
      )}
    </div>
  )
}
