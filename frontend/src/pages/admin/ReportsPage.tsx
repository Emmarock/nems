import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { reportsApi } from '../../api/endpoints'
import type { Dashboard } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { StatCard } from '../../components/StatCard'
import { GlobalSearch } from '../../components/GlobalSearch'
import { LoadingState } from '../../components/LoadingState'
import { AlertIcon, CarIcon, ChartIcon, CoinsIcon, HouseIcon, PersonBadgeIcon, UsersIcon, WrenchIcon } from '../../components/icons'

/**
 * Reports is also visible to TREASURER/FINANCIAL_SECRETARY, but /workers, /visitors and
 * /complaints aren't (see App.tsx's route role lists) - gating these keeps the card clickable
 * only for a role that can actually land on the target page, instead of silently bouncing back.
 */
const CAN_SEE_WORKERS = new Set(['SUPER_ADMIN', 'CDA_ADMIN'])
const CAN_SEE_VISITORS = new Set(['SUPER_ADMIN', 'CDA_ADMIN'])
const CAN_SEE_COMPLAINTS = new Set(['SUPER_ADMIN'])

export function ReportsPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const navigate = useNavigate()
  const { user } = useAuth()

  useEffect(() => {
    reportsApi.dashboard().then(setDashboard)
  }, [])

  if (!dashboard) {
    return <LoadingState />
  }

  return (
    <div>
      <div className="page-header" data-tour="/reports">
        <div>
          <h1>Reports</h1>
          <p className="page-subtitle">CDA dashboard, extended with worker/visitor activity.</p>
        </div>
      </div>

      <GlobalSearch />

      <div className="stat-grid">
        <StatCard
          label="Residents"
          value={dashboard.residents}
          icon={<UsersIcon />}
          tone="primary"
          onClick={() => navigate('/residents')}
        />
        <StatCard
          label="Properties"
          value={dashboard.properties}
          icon={<HouseIcon />}
          tone="violet"
          onClick={() => navigate('/properties')}
        />
        <StatCard
          label="Registered vehicles"
          value={dashboard.registeredVehicles}
          icon={<CarIcon />}
          tone="teal"
          onClick={() => navigate('/vehicles')}
        />
        <StatCard label="Total billing" value={`₦${dashboard.totalBilling.toLocaleString()}`} icon={<CoinsIcon />} tone="primary" />
        <StatCard label="Collected" value={`₦${dashboard.collected.toLocaleString()}`} icon={<CoinsIcon />} tone="success" />
        <StatCard
          label="Outstanding"
          value={`₦${dashboard.outstanding.toLocaleString()}`}
          icon={<CoinsIcon />}
          tone="danger"
          onClick={() => navigate('/security/arrears')}
        />
        <StatCard label="Collection rate" value={`${dashboard.collectionRatePercent}%`} icon={<ChartIcon />} tone="primary" />
        <StatCard
          label="Workers on site"
          value={dashboard.activeWorkersOnSite}
          icon={<WrenchIcon />}
          tone="warning"
          onClick={user && CAN_SEE_WORKERS.has(user.role) ? () => navigate('/workers?activeOnly=1') : undefined}
        />
        <StatCard
          label="Active visitor passes"
          value={dashboard.activeVisitorPasses}
          icon={<PersonBadgeIcon />}
          tone="magenta"
          onClick={user && CAN_SEE_VISITORS.has(user.role) ? () => navigate('/visitors?status=ACTIVE') : undefined}
        />
        <StatCard
          label="Open complaints"
          value={dashboard.openComplaints}
          icon={<AlertIcon />}
          tone="danger"
          onClick={user && CAN_SEE_COMPLAINTS.has(user.role) ? () => navigate('/complaints?openOnly=1') : undefined}
        />
      </div>
    </div>
  )
}
