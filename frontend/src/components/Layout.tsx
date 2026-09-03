import { useEffect, useState, type ComponentType, type ReactNode, type SVGProps } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { NotificationBell } from './NotificationBell'
import { ChangePasswordModal } from './ChangePasswordModal'
import {
  CarIcon,
  ChartIcon,
  ComplaintIcon,
  CoinsIcon,
  DashboardIcon,
  GateIcon,
  HouseIcon,
  MegaphoneIcon,
  MenuIcon,
  PersonBadgeIcon,
  RfidIcon,
  ShieldIcon,
  UserBadgeIcon,
  UsersIcon,
  WrenchIcon,
} from './icons'
import type { Role } from '../api/types'

interface NavItem {
  to: string
  label: string
  roles: Role[]
  icon: ComponentType<SVGProps<SVGSVGElement>>
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', roles: ['RESIDENT'], icon: DashboardIcon },
  { to: '/reports', label: 'Reports', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER'], icon: ChartIcon },
  { to: '/security', label: 'Security Dashboard', roles: ['SUPER_ADMIN', 'SECURITY'], icon: PersonBadgeIcon },
  { to: '/residents', label: 'Residents', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY'], icon: UsersIcon },
  { to: '/properties', label: 'Properties', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY'], icon: HouseIcon },
  { to: '/vehicles', label: 'Vehicles', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY'], icon: CarIcon },
  { to: '/visitors', label: 'Visitors', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'SECURITY'], icon: PersonBadgeIcon },
  { to: '/workers', label: 'Workers', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY'], icon: WrenchIcon },
  { to: '/levies', label: 'Levies & Invoices', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER'], icon: CoinsIcon },
  { to: '/payments', label: 'Payments', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER'], icon: CoinsIcon },
  { to: '/complaints', label: 'Complaints', roles: ['SUPER_ADMIN', 'SECRETARY', 'MAINTENANCE'], icon: ComplaintIcon },
  { to: '/announcements', label: 'Announcements', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY', 'MAINTENANCE', 'RESIDENT'], icon: MegaphoneIcon },
  { to: '/users', label: 'Users', roles: ['SUPER_ADMIN'], icon: UserBadgeIcon },
  { to: '/security/gates', label: 'Gates', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY'], icon: GateIcon },
  { to: '/security/rfid', label: 'RFID', roles: ['SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY'], icon: RfidIcon },
  { to: '/security/policy', label: 'Access Policy', roles: ['SUPER_ADMIN', 'CDA_ADMIN'], icon: ShieldIcon },
  { to: '/portal/profile', label: 'My Profile', roles: ['RESIDENT'], icon: UserBadgeIcon },
  { to: '/portal/visitors', label: 'My Visitors', roles: ['RESIDENT'], icon: PersonBadgeIcon },
  { to: '/portal/workers', label: 'My Workers', roles: ['RESIDENT'], icon: WrenchIcon },
  { to: '/portal/complaints', label: 'My Complaints', roles: ['RESIDENT'], icon: ComplaintIcon },
]

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth()
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)
  const [navOpen, setNavOpen] = useState(false)
  const location = useLocation()

  useEffect(() => {
    setNavOpen(false)
  }, [location.pathname])

  if (!user) return null

  const items = NAV_ITEMS.filter((item) => item.roles.includes(user.role))

  return (
    <div className="app-shell">
      <aside className={`sidebar ${navOpen ? 'open' : ''}`}>
        <div className="sidebar-brand">Nitel Estate</div>
        <div className="sidebar-section">Menu</div>
        {items.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
            >
              <Icon />
              {item.label}
            </NavLink>
          )
        })}
      </aside>
      {navOpen && <div className="sidebar-backdrop" onClick={() => setNavOpen(false)} />}
      <div className="main">
        <header className="topbar">
          <button
            type="button"
            className="icon-btn sidebar-toggle"
            aria-label="Open menu"
            onClick={() => setNavOpen((prev) => !prev)}
          >
            <MenuIcon />
          </button>
          <div className="topbar-user">
            <NotificationBell />
            <span>
              {user.fullName} · <span className="muted">{user.role.replace(/_/g, ' ')}</span>
            </span>
            <button className="btn btn-sm" onClick={() => setPasswordModalOpen(true)}>
              Change password
            </button>
            <button className="btn btn-sm" onClick={logout}>
              Log out
            </button>
          </div>
        </header>
        <main className="content">{children}</main>
      </div>

      {passwordModalOpen && <ChangePasswordModal onClose={() => setPasswordModalOpen(false)} />}
    </div>
  )
}
