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
import { fileToCompressedDataUrl, fileToDataUrl } from '../../utils/imageResize'

const CHANNEL_OPTIONS = ['PORTAL', 'EMAIL', 'SMS', 'WHATSAPP']
const CAN_CREATE = new Set(['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'PRO'])
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
      <div className="page-header" data-tour="/announcements">
        <div>
          <h1>Announcements</h1>
          <p className="page-subtitle">Estate-wide notices via portal, email, SMS, WhatsApp. Click a title to read it in full.</p>
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
          {
            key: 'message',
            label: 'Message',
            render: (a) => (
              <button type="button" className="link-button cell-truncate" onClick={() => openDetail(a.id, a.read)}>
                {a.message}
              </button>
            ),
          },
          { key: 'channels', label: 'Channels', render: (a) => a.channels.join(', ') },
          { key: 'audience', label: 'Audience', render: (a) => (a.includeTenants ? 'Everyone' : 'Owners/landlords only') },
          { key: 'createdAt', label: 'Posted', render: (a) => new Date(a.createdAt).toLocaleString() },
          { key: 'status', label: 'Status', render: (a) => <StatusBadge value={a.read ? 'READ' : 'UNREAD'} /> },
        ]}
        actions={(a) => (
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-sm" onClick={() => openDetail(a.id, a.read)}>
              View
            </button>
            <button className="btn btn-sm" onClick={() => toggleRead(a)}>
              Mark {a.read ? 'unread' : 'read'}
            </button>
          </div>
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

      {selected && (
        <AnnouncementDetailModal
          announcement={selected}
          onClose={() => {
            setSelectedId(null)
            // The modal's own Mark read/unread button goes through NotificationsContext directly
            // (so the bell updates too) rather than this page's toggleRead/patchLocal, so this
            // table's local state wouldn't otherwise pick up a change made from inside it.
            load()
          }}
        />
      )}
    </div>
  )
}

function AnnouncementModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => Promise<void> }) {
  const [title, setTitle] = useState('')
  const [message, setMessage] = useState('')
  const [channels, setChannels] = useState<string[]>(['PORTAL'])
  const [attachment, setAttachment] = useState<string | null>(null)
  const [attachmentFilename, setAttachmentFilename] = useState<string | null>(null)
  const [attachmentIsPdf, setAttachmentIsPdf] = useState(false)
  const [includeTenants, setIncludeTenants] = useState(false)
  const [imageBusy, setImageBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function toggleChannel(channel: string) {
    setChannels((prev) => (prev.includes(channel) ? prev.filter((c) => c !== channel) : [...prev, channel]))
  }

  function clearAttachment() {
    setAttachment(null)
    setAttachmentFilename(null)
    setAttachmentIsPdf(false)
  }

  async function handleAttachmentPick(file: File | undefined) {
    if (!file) return
    setImageBusy(true)
    setError(null)
    try {
      const isPdf = file.type === 'application/pdf'
      // A PDF (e.g. meeting minutes) can't be re-encoded through canvas like a photo can - it's
      // read as-is, just capped on size instead of compressed.
      setAttachment(isPdf ? await fileToDataUrl(file) : await fileToCompressedDataUrl(file))
      setAttachmentFilename(file.name)
      setAttachmentIsPdf(isPdf)
      // SMS can't carry a file - dropping it here rather than leaving a stale selection the
      // backend would reject anyway (AnnouncementService.create enforces the same rule).
      setChannels((prev) => prev.filter((c) => c !== 'SMS'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not process the selected file')
    } finally {
      setImageBusy(false)
    }
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
      await announcementsApi.create({
        title,
        message,
        channels,
        attachment: attachment ?? undefined,
        attachmentFilename: attachmentFilename ?? undefined,
        includeTenants,
      })
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
                <label htmlFor="attachment">Image or PDF (optional)</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  {attachment && !attachmentIsPdf && (
                    <img
                      src={attachment}
                      alt=""
                      style={{ width: 48, height: 48, borderRadius: 8, objectFit: 'cover', border: '1px solid var(--color-border)' }}
                    />
                  )}
                  {attachment && attachmentIsPdf && (
                    <span className="muted" style={{ fontSize: 13 }}>📄 {attachmentFilename}</span>
                  )}
                  <input
                    id="attachment"
                    type="file"
                    accept="image/*,application/pdf"
                    onChange={(e) => handleAttachmentPick(e.target.files?.[0])}
                  />
                  {attachment && (
                    <button type="button" className="btn btn-sm" onClick={clearAttachment}>
                      Remove
                    </button>
                  )}
                </div>
                <p className="muted" style={{ fontSize: 12, margin: '4px 0 0' }}>
                  A PDF works well for sharing meeting minutes.
                </p>
                {imageBusy && <span className="muted" style={{ fontSize: 12 }}>Processing file…</span>}
              </div>
              <div className="form-field full">
                <label>Channels</label>
                <div style={{ display: 'flex', gap: 14 }}>
                  {CHANNEL_OPTIONS.map((channel) => {
                    const disabled = channel === 'SMS' && attachment !== null
                    return (
                      <label key={channel} className="checkbox-field" style={{ fontWeight: 400, opacity: disabled ? 0.5 : 1 }}>
                        <input
                          type="checkbox"
                          checked={channels.includes(channel)}
                          disabled={disabled}
                          onChange={() => toggleChannel(channel)}
                        />
                        {channel}
                      </label>
                    )
                  })}
                </div>
                {attachment && <p className="muted" style={{ fontSize: 12, margin: '6px 0 0' }}>SMS can't carry a file, so it's unavailable while one is attached.</p>}
              </div>
              <div className="form-field full">
                <label>Audience</label>
                <label className="checkbox-field" style={{ fontWeight: 400 }}>
                  <input type="checkbox" checked={includeTenants} onChange={(e) => setIncludeTenants(e.target.checked)} />
                  Include tenants
                </label>
                <p className="muted" style={{ fontSize: 12, margin: '4px 0 0' }}>
                  {includeTenants
                    ? 'Every resident sees this, including tenants.'
                    : "Tenants won't see this or be notified — owners and landlords only."}
                </p>
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
