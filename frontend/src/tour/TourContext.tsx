import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { isLandlord, useAuth } from '../auth/AuthContext'
import { buildTourSteps, ROLE_HOME, type TourStep } from './tourSteps'

const STORAGE_PREFIX = 'nems.tour.completed.'

interface TourContextValue {
  active: boolean
  stepIndex: number
  steps: TourStep[]
  currentStep: TourStep | null
  next: () => void
  back: () => void
  skip: () => void
  /** Also used to replay the tour on demand (see Layout's "Take a tour" button) — jumps to this
   * role's own landing page first since every step's target lives there or in the sidebar it carries. */
  restart: () => void
}

const TourContext = createContext<TourContextValue | null>(null)

/**
 * Per-role onboarding walkthrough, for every role (resident and staff alike). Auto-starts once
 * per person (tracked in localStorage by email, not the backend - losing this on a new device/
 * browser just means a harmless replay, not a meaningful data loss) the first time they're seen
 * with no completed-tour record, and can be replayed anytime via restart(). See
 * components/TourOverlay for the actual rendering, tour/tourSteps for each role's content.
 */
export function TourProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [active, setActive] = useState(false)
  const [stepIndex, setStepIndex] = useState(0)

  const steps = useMemo(() => (user ? buildTourSteps(user.role, isLandlord(user)) : []), [user])

  const storageKey = user ? `${STORAGE_PREFIX}${user.email}` : null
  const homeRoute = user ? ROLE_HOME[user.role] : null

  function finish() {
    setActive(false)
    setStepIndex(0)
    if (storageKey) {
      try {
        localStorage.setItem(storageKey, '1')
      } catch {
        // Private window / storage disabled - the tour just offers to replay next load, harmless.
      }
    }
  }

  function start() {
    setStepIndex(0)
    setActive(true)
    if (homeRoute && location.pathname !== homeRoute) {
      navigate(homeRoute)
    }
  }

  useEffect(() => {
    if (!user || !storageKey) {
      return
    }
    let alreadySeen: string | null = null
    try {
      alreadySeen = localStorage.getItem(storageKey)
    } catch {
      return // Can't read storage - skip auto-start rather than risk nagging on every load.
    }
    if (!alreadySeen) {
      start()
    }
    // Only meant to fire once per person becoming known (e.g. right after login) - not on every
    // location/steps change, which would re-trigger it while they're mid-tour.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.email])

  function next() {
    if (stepIndex + 1 >= steps.length) {
      finish()
    } else {
      setStepIndex(stepIndex + 1)
    }
  }

  function back() {
    setStepIndex((i) => Math.max(0, i - 1))
  }

  const value: TourContextValue = {
    active,
    stepIndex,
    steps,
    currentStep: active ? (steps[stepIndex] ?? null) : null,
    next,
    back,
    skip: finish,
    restart: start,
  }

  return <TourContext.Provider value={value}>{children}</TourContext.Provider>
}

export function useTour(): TourContextValue {
  const ctx = useContext(TourContext)
  if (!ctx) {
    throw new Error('useTour must be used within a TourProvider')
  }
  return ctx
}
