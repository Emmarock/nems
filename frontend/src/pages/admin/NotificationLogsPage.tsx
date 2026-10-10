import { useEffect, useState } from 'react'
import { notificationLogsApi } from '../../api/endpoints'
import type { NotificationLog, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const EMPTY: PageResponse<NotificationLog> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function NotificationLogsPage() {
  const [result, setResult] = useState<PageResponse<NotificationLog>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [channel, setChannel] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)

  async function load() {
    setLoading(true)
    setResult(
      await notificationLogsApi.list({
        q: debouncedQuery || undefined,
        channel: channel || undefined,
        status: status || undefined,
        page,
        size: 20,
      }),
    )
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, channel, status, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery, channel, status])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Notification log</h1>
          <p className="page-subtitle">
            Every SMS, WhatsApp, and email the app has attempted to send — newest first. "Sent" means the request to
            the provider didn't fail, not that it was delivered; check the message content and timestamp against
            what the resident reports receiving. Credentials, reset links, and verification codes are redacted.
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 14 }}>
        <div style={{ flex: '1 1 240px' }}>
          <SearchInput value={query} onChange={setQuery} placeholder="Search by recipient phone or email…" />
        </div>
        <select value={channel} onChange={(e) => setChannel(e.target.value)}>
          <option value="">All channels</option>
          <option value="EMAIL">Email</option>
          <option value="SMS">SMS</option>
          <option value="WHATSAPP">WhatsApp</option>
          <option value="PORTAL">Portal</option>
        </select>
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          <option value="SENT">Sent</option>
          <option value="FAILED">Failed</option>
        </select>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(n) => n.id}
        emptyMessage={query || channel || status ? 'No notifications match your filters.' : 'No notifications sent yet.'}
        columns={[
          { key: 'recipient', label: 'Recipient' },
          { key: 'channel', label: 'Channel', render: (n) => <StatusBadge value={n.channel} /> },
          { key: 'message', label: 'Message' },
          { key: 'createdAt', label: 'Sent at', render: (n) => new Date(n.createdAt).toLocaleString() },
          { key: 'status', label: 'Status', render: (n) => <StatusBadge value={n.status} /> },
        ]}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />
    </div>
  )
}
