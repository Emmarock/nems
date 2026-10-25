import type { Announcement } from '../api/types'
import { useNotifications } from '../notifications/NotificationsContext'
import { StatusBadge } from './StatusBadge'

export function AnnouncementDetailModal({ announcement, onClose }: { announcement: Announcement; onClose: () => void }) {
  const { markRead, markUnread } = useNotifications()

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{announcement.title}</h2>
          <button type="button" className="btn btn-sm" onClick={onClose}>
            ✕
          </button>
        </div>
        <div className="modal-body">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14, flexWrap: 'wrap' }}>
            <StatusBadge value={announcement.read ? 'READ' : 'UNREAD'} />
            <span className="muted" style={{ fontSize: 13 }}>
              {new Date(announcement.createdAt).toLocaleString()} · {announcement.channels.join(', ')}
            </span>
            {!announcement.includeTenants && <StatusBadge value="OWNERS/LANDLORDS ONLY" />}
          </div>
          {announcement.attachment && announcement.attachment.startsWith('data:application/pdf') ? (
            <div style={{ marginBottom: 14 }}>
              {/* Browsers render a PDF data URI with their own built-in viewer here, so a
                  resident can read it (e.g. meeting minutes) without downloading anything. */}
              <iframe
                src={announcement.attachment}
                title={announcement.attachmentFilename ?? 'Attachment'}
                style={{ width: '100%', height: 420, border: '1px solid var(--color-border)', borderRadius: 8 }}
              />
              <a
                href={announcement.attachment}
                target="_blank"
                rel="noopener noreferrer"
                className="link-button"
                style={{ display: 'inline-block', marginTop: 6, fontSize: 13 }}
              >
                Open {announcement.attachmentFilename ?? 'PDF'} in a new tab
              </a>
            </div>
          ) : (
            announcement.attachment && (
              <img
                src={announcement.attachment}
                alt=""
                style={{ width: '100%', maxHeight: 320, objectFit: 'cover', borderRadius: 8, marginBottom: 14 }}
              />
            )
          )}
          <p style={{ whiteSpace: 'pre-wrap', margin: 0, lineHeight: 1.6 }}>{announcement.message}</p>
        </div>
        <div className="modal-footer">
          <button
            type="button"
            className="btn"
            onClick={() => (announcement.read ? markUnread(announcement.id) : markRead(announcement.id))}
          >
            Mark {announcement.read ? 'unread' : 'read'}
          </button>
          <button type="button" className="btn btn-primary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  )
}
