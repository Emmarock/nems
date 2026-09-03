import { useEffect, useState, type FormEvent } from 'react'
import { announcementsApi } from '../../api/endpoints'
import type { Announcement, PageResponse } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useNotifications } from '../../notifications/NotificationsContext'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { AnnouncementDetailModal } from '../../components/AnnouncementDetailModal'
import { apiErrorMessage } from '../../api/client'

const CHANNEL_OPTIONS = ['PORTAL', 'EMAIL', 'SMS', 'WHATSAPP']
const CAN_CREATE = new Set(['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY'])
const EMPTY: PageResponse<Announcement> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function AnnouncementsPage() {
  const { user } = useAuth()
  const { refresh, markRead, markUnread } = useNotifications()
  const [result, setResult] = useState<PageResponse<Announcement>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const selected = result.content.find((a) => a.id === selectedId) ?? null

  async function load() {
    setLoading(true)
    setResult(await announcementsApi.list({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  function patchLocal(id: number, read: boolean) {
    setResult((prev) => ({ ...prev, content: prev.content.map((a) => (a.id === id ? { ...a, read } : a)) }))
  }

  async function openDetail(id: number, read: boolean) {
    if (!read) {
      await markRead(id)
      patchLocal(id, true)
    }
    setSelectedId(id)
  }

  async function toggleRead(a: Announcement) {
    if (a.read) {
      await markUnread(a.id)
      patchLocal(a.id, false)
    } else {
      await markRead(a.id)
      patchLocal(a.id, true)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Announcements</h1>
          <p className="page-subtitle">Estate-wide notices via portal, email, SMS, WhatsApp (spec §8). Click a title to read it in full.</p>
        </div>
        {user && CAN_CREATE.has(user.role) && (
          <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
            + New announcement
          </button>
        )}
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(a) => a.id}
        emptyMessage="No announcements yet."
        columns={[
          { key: 'read', label: '', render: (a) => (a.read ? null : <span className="bell-dot" />) },
          {
            key: 'title',
            label: 'Title',
            render: (a) => (
              <button type="button" className="link-button" onClick={() => openDetail(a.id, a.read)}>
                {a.title}
              </button>
            ),
          },
          { key: 'message', label: 'Message', render: (a) => <span className="cell-truncate">{a.message}</span> },
          { key: 'channels', label: 'Channels', render: (a) => a.channels.join(', ') },
          { key: 'createdAt', label: 'Posted', render: (a) => new Date(a.createdAt).toLocaleString() },
          { key: 'status', label: 'Status', render: (a) => <StatusBadge value={a.read ? 'READ' : 'UNREAD'} /> },
        ]}
        actions={(a) => (
          <button className="btn btn-sm" onClick={() => toggleRead(a)}>
            Mark {a.read ? 'unread' : 'read'}
          </button>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <AnnouncementModal
          onClose={() => setModalOpen(false)}
          onCreated={async () => {
            setModalOpen(false)
            await refresh()
            if (page === 0) {
              await load()
            } else {
              setPage(0)
            }
          }}
        />
      )}

      {selected && <AnnouncementDetailModal announcement={selected} onClose={() => setSelectedId(null)} />}
    </div>
  )
}

function AnnouncementModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => Promise<void> }) {
  const [title, setTitle] = useState('')
  const [message, setMessage] = useState('')
  const [channels, setChannels] = useState<string[]>(['PORTAL'])
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function toggleChannel(channel: string) {
    setChannels((prev) => (prev.includes(channel) ? prev.filter((c) => c !== channel) : [...prev, channel]))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (channels.length === 0) {
      setError('Select at least one channel')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await announcementsApi.create({ title, message, channels })
      await onCreated()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <h2>New announcement</h2>
            <button type="button" className="btn btn-sm" onClick={onClose}>
              ✕
            </button>
          </div>
          <div className="modal-body">
            {error && <p className="error-text">{error}</p>}
            <div className="form-grid">
              <div className="form-field full">
                <label htmlFor="title">Title</label>
                <input id="title" required value={title} onChange={(e) => setTitle(e.target.value)} />
              </div>
              <div className="form-field full">
                <label htmlFor="message">Message</label>
                <textarea id="message" required value={message} onChange={(e) => setMessage(e.target.value)} />
              </div>
              <div className="form-field full">
                <label>Channels</label>
                <div style={{ display: 'flex', gap: 14 }}>
                  {CHANNEL_OPTIONS.map((channel) => (
                    <label key={channel} className="checkbox-field" style={{ fontWeight: 400 }}>
                      <input
                        type="checkbox"
                        checked={channels.includes(channel)}
                        onChange={() => toggleChannel(channel)}
                      />
                      {channel}
                    </label>
                  ))}
                </div>
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Posting…' : 'Post announcement'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
