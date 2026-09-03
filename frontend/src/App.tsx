import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { NotificationsProvider } from './notifications/NotificationsContext'
import { EntityDetailProvider } from './entityDetail/EntityDetailContext'
import { EntityDetailModal } from './components/EntityDetailModal'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/auth/LoginPage'
import { ForcePasswordChangePage } from './pages/auth/ForcePasswordChangePage'
import { MockCheckoutPage } from './pages/portal/MockCheckoutPage'
import { DashboardPage } from './pages/portal/DashboardPage'
import { VisitorsPage } from './pages/portal/VisitorsPage'
import { ProfilePage } from './pages/portal/ProfilePage'
import { WorkersPage } from './pages/portal/WorkersPage'
import { ComplaintsPage } from './pages/portal/ComplaintsPage'
import { ResidentsPage } from './pages/admin/ResidentsPage'
import { PropertiesPage } from './pages/admin/PropertiesPage'
import { VehiclesPage } from './pages/admin/VehiclesPage'
import { WorkersAdminPage } from './pages/admin/WorkersAdminPage'
import { VisitorsAdminPage } from './pages/admin/VisitorsAdminPage'
import { LeviesPage } from './pages/admin/LeviesPage'
import { PaymentsPage } from './pages/admin/PaymentsPage'
import { UsersPage } from './pages/admin/UsersPage'
import { ComplaintsAdminPage } from './pages/admin/ComplaintsAdminPage'
import { AnnouncementsPage } from './pages/admin/AnnouncementsPage'
import { ReportsPage } from './pages/admin/ReportsPage'
import { SecurityDashboardPage } from './pages/security/SecurityDashboardPage'
import { AccountsInArrearsPage } from './pages/security/AccountsInArrearsPage'
import { GatesPage } from './pages/security/GatesPage'
import { RfidPage } from './pages/security/RfidPage'
import { AccessPolicyPage } from './pages/security/AccessPolicyPage'
import { ScanPage } from './pages/security/ScanPage'
import type { Role } from './api/types'

const STAFF_LANDING: Record<Exclude<Role, 'RESIDENT'>, string> = {
  SUPER_ADMIN: '/reports',
  CDA_ADMIN: '/reports',
  TREASURER: '/reports',
  SECRETARY: '/residents',
  SECURITY: '/security',
  MAINTENANCE: '/complaints',
}

function Home() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.role === 'RESIDENT') return <DashboardPage />
  return <Navigate to={STAFF_LANDING[user.role]} replace />
}

function withLayout(node: ReactNode) {
  return <Layout>{node}</Layout>
}

export default function App() {
  return (
    <AuthProvider>
      <NotificationsProvider>
      <EntityDetailProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/mock-checkout" element={<MockCheckoutPage />} />
        <Route
          path="/scan/:kind/:qrToken"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'SECURITY', 'TREASURER', 'CDA_ADMIN']}>
              <ScanPage />
            </ProtectedRoute>
          }
        />

        <Route path="/" element={<ProtectedRoute>{withLayout(<Home />)}</ProtectedRoute>} />
        <Route
          path="/change-password"
          element={
            <ProtectedRoute>
              <ForcePasswordChangePage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/reports"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER']}>
              {withLayout(<ReportsPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/residents"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY']}>
              {withLayout(<ResidentsPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/properties"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY']}>
              {withLayout(<PropertiesPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/vehicles"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY']}>
              {withLayout(<VehiclesPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/workers"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY']}>
              {withLayout(<WorkersAdminPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/visitors"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'SECURITY']}>
              {withLayout(<VisitorsAdminPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/levies"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER']}>
              {withLayout(<LeviesPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/payments"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER']}>
              {withLayout(<PaymentsPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/users"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN']}>{withLayout(<UsersPage />)}</ProtectedRoute>
          }
        />
        <Route
          path="/complaints"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'SECRETARY', 'MAINTENANCE']}>
              {withLayout(<ComplaintsAdminPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/announcements"
          element={<ProtectedRoute>{withLayout(<AnnouncementsPage />)}</ProtectedRoute>}
        />

        <Route
          path="/portal/visitors"
          element={<ProtectedRoute roles={['RESIDENT']}>{withLayout(<VisitorsPage />)}</ProtectedRoute>}
        />
        <Route
          path="/portal/profile"
          element={<ProtectedRoute roles={['RESIDENT']}>{withLayout(<ProfilePage />)}</ProtectedRoute>}
        />
        <Route
          path="/portal/workers"
          element={<ProtectedRoute roles={['RESIDENT']}>{withLayout(<WorkersPage />)}</ProtectedRoute>}
        />
        <Route
          path="/portal/complaints"
          element={<ProtectedRoute roles={['RESIDENT']}>{withLayout(<ComplaintsPage />)}</ProtectedRoute>}
        />

        <Route
          path="/security"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'SECURITY']}>
              {withLayout(<SecurityDashboardPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/security/gates"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY']}>
              {withLayout(<GatesPage />)}
            </ProtectedRoute>
          }
        />
        <Route path="/security/access-events" element={<Navigate to="/security" replace />} />
        <Route
          path="/security/arrears"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'SECURITY']}>
              {withLayout(<AccountsInArrearsPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/security/rfid"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY']}>
              {withLayout(<RfidPage />)}
            </ProtectedRoute>
          }
        />
        <Route
          path="/security/policy"
          element={
            <ProtectedRoute roles={['SUPER_ADMIN', 'CDA_ADMIN']}>
              {withLayout(<AccessPolicyPage />)}
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <EntityDetailModal />
      </EntityDetailProvider>
      </NotificationsProvider>
    </AuthProvider>
  )
}
