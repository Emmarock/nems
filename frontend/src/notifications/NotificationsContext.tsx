import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { announcementsApi, userNotificationsApi } from '../api/endpoints'
import type { Announcement, UserNotification } from '../api/types'
import { useAuth } from '../auth/AuthContext'

const POLL_INTERVAL_MS = 30_000

interface NotificationsContextValue {
  announcements: Announcement[]
  /** Personal, in-app alerts (e.g. "your visitor checked in") — distinct from announcements. */
  alerts: UserNotification[]
  /** Combined unread count across announcements and personal alerts, for the bell badge. */
  unreadCount: number
  loading: boolean
  refresh: () => Promise<void>
  markRead: (id: number) => Promise<void>
  markUnread: (id: number) => Promise<void>
  markAlertRead: (id: number) => Promise<void>
}

const NotificationsContext = createContext<NotificationsContextValue | undefined>(undefined)

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [alerts, setAlerts] = useState<UserNotification[]>([])
  const [announcementsUnread, setAnnouncementsUnread] = useState(0)
  const [alertsUnread, setAlertsUnread] = useState(0)
  const [loading, setLoading] = useState(false)
  const intervalRef = useRef<number | null>(null)

  const refresh = useCallback(async () => {
    if (!user) return
    setLoading(true)
    try {
      const [list, unread, alertList, alertUnread] = await Promise.all([
        announcementsApi.list({ size: 50 }),
        announcementsApi.unreadCount(),
        userNotificationsApi.mine({ size: 20 }),
        userNotificationsApi.unreadCount(),
      ])
      setAnnouncements(list.content)
      setAnnouncementsUnread(unread)
      setAlerts(alertList.content)
      setAlertsUnread(alertUnread)
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    if (!user) {
      setAnnouncements([])
      setAlerts([])
      setAnnouncementsUnread(0)
      setAlertsUnread(0)
      if (intervalRef.current) window.clearInterval(intervalRef.current)
      return
    }
    refresh()
    intervalRef.current = window.setInterval(refresh, POLL_INTERVAL_MS)
    return () => {
      if (intervalRef.current) window.clearInterval(intervalRef.current)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user])

  async function markRead(id: number) {
    await announcementsApi.markRead(id)
    setAnnouncements((prev) => prev.map((a) => (a.id === id ? { ...a, read: true } : a)))
    setAnnouncementsUnread((prev) => Math.max(0, prev - 1))
  }

  async function markUnread(id: number) {
    await announcementsApi.markUnread(id)
    setAnnouncements((prev) => prev.map((a) => (a.id === id ? { ...a, read: false } : a)))
    setAnnouncementsUnread((prev) => prev + 1)
  }

  async function markAlertRead(id: number) {
    await userNotificationsApi.markRead(id)
    setAlerts((prev) => prev.map((a) => (a.id === id ? { ...a, read: true } : a)))
    setAlertsUnread((prev) => Math.max(0, prev - 1))
  }

  return (
    <NotificationsContext.Provider
      value={{
        announcements,
        alerts,
        unreadCount: announcementsUnread + alertsUnread,
        loading,
        refresh,
        markRead,
        markUnread,
        markAlertRead,
      }}
    >
      {children}
    </NotificationsContext.Provider>
  )
}

export function useNotifications(): NotificationsContextValue {
  const ctx = useContext(NotificationsContext)
  if (!ctx) throw new Error('useNotifications must be used within NotificationsProvider')
  return ctx
}
