import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { reportsApi } from '../../api/endpoints'
import type { Dashboard } from '../../api/types'
import { StatCard } from '../../components/StatCard'
import { GlobalSearch } from '../../components/GlobalSearch'
import { LoadingState } from '../../components/LoadingState'
import { AlertIcon, CarIcon, ChartIcon, CoinsIcon, HouseIcon, PersonBadgeIcon, UsersIcon, WrenchIcon } from '../../components/icons'

export function ReportsPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    reportsApi.dashboard().then(setDashboard)
  }, [])

  if (!dashboard) {
    return <LoadingState />
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Reports</h1>
          <p className="page-subtitle">CDA dashboard (spec §11), extended with worker/visitor activity.</p>
        </div>
      </div>

      <GlobalSearch />

      <div className="stat-grid">
        <StatCard label="Residents" value={dashboard.residents} icon={<UsersIcon />} tone="primary" />
        <StatCard label="Properties" value={dashboard.properties} icon={<HouseIcon />} tone="violet" />
        <StatCard
          label="Registered vehicles"
          value={dashboard.registeredVehicles}
          icon={<CarIcon />}
          tone="teal"
          onClick={() => navigate('/vehicles')}
        />
        <StatCard label="Total billing" value={`₦${dashboard.totalBilling.toLocaleString()}`} icon={<CoinsIcon />} tone="primary" />
        <StatCard label="Collected" value={`₦${dashboard.collected.toLocaleString()}`} icon={<CoinsIcon />} tone="success" />
        <StatCard label="Outstanding" value={`₦${dashboard.outstanding.toLocaleString()}`} icon={<CoinsIcon />} tone="danger" />
        <StatCard label="Collection rate" value={`${dashboard.collectionRatePercent}%`} icon={<ChartIcon />} tone="primary" />
        <StatCard label="Workers on site" value={dashboard.activeWorkersOnSite} icon={<WrenchIcon />} tone="warning" />
        <StatCard label="Active visitor passes" value={dashboard.activeVisitorPasses} icon={<PersonBadgeIcon />} tone="magenta" />
        <StatCard label="Open complaints" value={dashboard.openComplaints} icon={<AlertIcon />} tone="danger" />
      </div>
    </div>
  )
}
