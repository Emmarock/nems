import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useNotifications } from '../notifications/NotificationsContext'
import { AnnouncementDetailModal } from './AnnouncementDetailModal'
import type { Announcement } from '../api/types'

const DROPDOWN_LIMIT = 6

export function NotificationBell() {
  const { announcements, unreadCount, markRead, markUnread } = useNotifications()
  const [open, setOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const selected = announcements.find((a) => a.id === selectedId) ?? null

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const recent = announcements.slice(0, DROPDOWN_LIMIT)

  function openDetail(a: Announcement) {
    if (!a.read) markRead(a.id)
    setSelectedId(a.id)
    setOpen(false)
  }

  return (
    <div className="notification-bell" ref={containerRef}>
      <button
        type="button"
        className="bell-button"
        aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ''}`}
        onClick={() => setOpen((prev) => !prev)}
      >
        <BellIcon />
        {unreadCount > 0 && <span className="bell-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
      </button>

      {open && (
        <div className="bell-dropdown">
          <div className="bell-dropdown-header">
            <span>Announcements</span>
            {unreadCount > 0 && <span className="muted">{unreadCount} unread</span>}
          </div>

          {recent.length === 0 ? (
            <div className="bell-empty">No announcements yet.</div>
          ) : (
            <ul className="bell-list">
              {recent.map((a) => (
                <li key={a.id} className={`bell-item ${a.read ? '' : 'unread'}`}>
                  <div className="bell-item-main" onClick={() => openDetail(a)}>
                    <div className="bell-item-title">
                      {!a.read && <span className="bell-dot" />}
                      {a.title}
                    </div>
                    <div className="bell-item-message">{a.message}</div>
                    <div className="bell-item-time">{new Date(a.createdAt).toLocaleString()}</div>
                  </div>
                  <button
                    type="button"
                    className="btn btn-sm"
                    onClick={() => (a.read ? markUnread(a.id) : markRead(a.id))}
                  >
                    Mark {a.read ? 'unread' : 'read'}
                  </button>
                </li>
              ))}
            </ul>
          )}

          <button
            type="button"
            className="bell-view-all"
            onClick={() => {
              setOpen(false)
              navigate('/announcements')
            }}
          >
            View all announcements
          </button>
        </div>
      )}

      {selected && <AnnouncementDetailModal announcement={selected} onClose={() => setSelectedId(null)} />}
    </div>
  )
}

function BellIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>
  )
}
