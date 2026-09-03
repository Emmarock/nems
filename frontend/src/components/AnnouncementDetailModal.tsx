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
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
            <StatusBadge value={announcement.read ? 'READ' : 'UNREAD'} />
            <span className="muted" style={{ fontSize: 13 }}>
              {new Date(announcement.createdAt).toLocaleString()} · {announcement.channels.join(', ')}
            </span>
          </div>
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
