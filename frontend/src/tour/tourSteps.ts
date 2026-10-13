import type { Role } from '../api/types'

export interface TourStep {
  /** Route to be on for this step — the tour navigates here before showing it (see
   * tour/TourContext). Null only for the welcome/closing cards, which have no page of their own
   * and simply stay wherever the tour already is. */
  route: string | null
  /** Matches a data-tour attribute in the DOM, scoped to whatever `route` navigated to. Null
   * renders a centered card with no spotlight. */
  target: string | null
  title: string
  body: string
}

/**
 * Per-route copy, shared across every staff tour that includes that route - one page means the
 * same thing regardless of which role is looking at it, so the wording lives here once rather
 * than being duplicated per role. Each entry's `target` matches the data-tour the page's own
 * header carries (see e.g. pages/admin/ResidentsPage.tsx) - the tour spotlights the real page
 * now, not the sidebar link that got you there. RESIDENT's own tour (dashboard cards + portal
 * pages) is built separately below since its content and structure differ enough not to fit
 * this table.
 */
const ROUTE_CONTENT: Record<string, { title: string; body: string }> = {
  '/reports': {
    title: 'Reports',
    body: 'Your landing page — estate-wide stats (billing, collections, outstanding balances) plus a quick search for any resident, property, vehicle, or worker.',
  },
  '/security': {
    title: 'Security Dashboard',
    body: 'Live gate activity, accounts in arrears, and who’s currently on site, estate-wide.',
  },
  '/residents': {
    title: 'Residents',
    body: 'The full resident directory — add, edit, suspend, or reactivate any resident’s account.',
  },
  '/properties': {
    title: 'Properties',
    body: 'The estate’s property registry — house numbers, roads, owners, and occupancy.',
  },
  '/vehicles': {
    title: 'Vehicles',
    body: 'Every registered vehicle on the estate, searchable by resident or plate number.',
  },
  '/visitors': {
    title: 'Visitors',
    body: 'Active and past visitor passes across the whole estate.',
  },
  '/workers': {
    title: 'Workers',
    body: 'Domestic staff and contractor passes across the whole estate.',
  },
  '/household-members': {
    title: 'Household Members',
    body: 'Spouse/family passes sponsored by a resident and approved by their landlord.',
  },
  '/levies': {
    title: 'Levies & Invoices',
    body: 'Define what residents are billed for, and generate invoices against a levy.',
  },
  '/payments': {
    title: 'Payments',
    body: 'Confirm a resident-submitted payment receipt, or record an offline payment yourself.',
  },
  '/stickers': {
    title: 'Vehicle Stickers',
    body: 'Review and approve vehicle sticker requests.',
  },
  '/complaints': {
    title: 'Complaints',
    body: 'Estate issues residents have reported, and their current status.',
  },
  '/announcements': {
    title: 'Announcements',
    body: 'Estate-wide notices — write one here, or read what’s already gone out.',
  },
  '/users': {
    title: 'Users',
    body: 'Every staff login on the system — create new accounts and manage roles.',
  },
  '/notification-logs': {
    title: 'Notification log',
    body: 'Every email, SMS, and WhatsApp message the system has sent, and whether it was delivered.',
  },
  '/security/gates': {
    title: 'Gates',
    body: 'The estate’s physical gates/entry points.',
  },
  '/security/rfid': {
    title: 'RFID',
    body: 'RFID tags issued for vehicle or resident access.',
  },
  '/security/policy': {
    title: 'Access Policy',
    body: 'Configure arrears enforcement — the outstanding-balance threshold that flags an account at the gate.',
  },
}

/** Each role's visible nav items, in the same order Layout's sidebar shows them, limited to the
 * routes ROUTE_CONTENT above actually covers. Deliberately duplicated from Layout.NAV_ITEMS
 * rather than imported from it — a tour importing the component it points at risks a cycle, and
 * this list changes far less often than the sidebar's own styling/markup. */
const ROLE_ROUTES: Partial<Record<Role, string[]>> = {
  SUPER_ADMIN: [
    '/reports', '/security', '/residents', '/properties', '/vehicles', '/visitors', '/workers',
    '/household-members', '/levies', '/payments', '/stickers', '/complaints', '/announcements',
    '/users', '/notification-logs', '/security/gates', '/security/rfid', '/security/policy',
  ],
  CDA_ADMIN: [
    '/reports', '/residents', '/properties', '/vehicles', '/visitors', '/workers',
    '/household-members', '/levies', '/payments', '/stickers', '/announcements',
    '/security/gates', '/security/rfid', '/security/policy',
  ],
  TREASURER: ['/reports', '/residents', '/properties', '/vehicles', '/levies', '/payments', '/stickers', '/announcements'],
  FINANCIAL_SECRETARY: ['/reports', '/residents', '/properties', '/vehicles', '/levies', '/payments', '/stickers', '/announcements'],
  SECRETARY: ['/residents', '/properties', '/vehicles', '/visitors', '/complaints', '/announcements'],
  SECURITY: [
    '/security', '/residents', '/properties', '/vehicles', '/visitors', '/workers',
    '/household-members', '/stickers', '/announcements', '/security/gates', '/security/rfid',
  ],
  MAINTENANCE: ['/complaints', '/announcements'],
}

/** Where restart()/the first-login auto-start sends this role before the tour begins — mirrors
 * App.tsx's own STAFF_LANDING map (every staff tour step lives on a page reachable from there). */
export const ROLE_HOME: Record<Role, string> = {
  SUPER_ADMIN: '/reports',
  CDA_ADMIN: '/reports',
  TREASURER: '/reports',
  FINANCIAL_SECRETARY: '/payments',
  SECRETARY: '/residents',
  SECURITY: '/security',
  MAINTENANCE: '/complaints',
  RESIDENT: '/',
}

const RESIDENT_STEPS: TourStep[] = [
  {
    route: '/',
    target: 'dashboard-overview',
    title: 'Your account at a glance',
    body: 'This shows what you currently owe, what you’ve already paid, and how many vehicles you have registered.',
  },
  {
    route: '/',
    target: 'dashboard-access-pass',
    title: 'Your access pass',
    body: 'This QR code is yours personally. Show it at the gate and security scans you straight in — no need to stop and sign a book.',
  },
  {
    route: '/',
    target: 'dashboard-property',
    title: 'Your property',
    body: 'The house linked to your account. If anything here looks wrong, you can fix it yourself from My Profile.',
  },
  {
    route: '/portal/profile',
    target: '/portal/profile',
    title: 'My Profile',
    body: 'Update your contact details, correct your house number or road, and see your registered vehicles.',
  },
  {
    route: '/portal/visitors',
    target: '/portal/visitors',
    title: 'My Visitors',
    body: 'Expecting a guest? Register them here first — they get a QR pass to show security at the gate.',
  },
  {
    route: '/portal/workers',
    target: '/portal/workers',
    title: 'My Workers',
    body: 'Register domestic staff or contractors the same way, so security knows to let them in and out.',
  },
  {
    route: '/portal/household',
    target: '/portal/household',
    title: 'My Household',
    body: 'Add family members who live with you. Your landlord reviews the request and approves the pass.',
  },
  {
    route: '/portal/tenants',
    target: '/portal/tenants',
    title: 'My Tenants',
    body: 'As the property owner, add the tenants living on your property here.',
  },
  {
    route: '/portal/household-approvals',
    target: '/portal/household-approvals',
    title: 'Household Approvals',
    body: 'Approve household passes your tenants have requested for their own family members.',
  },
  {
    route: '/portal/vehicles',
    target: '/portal/vehicles',
    title: 'My Vehicles',
    body: 'Register your car here to get it its own QR pass for the gate.',
  },
  {
    route: '/portal/vehicle-approvals',
    target: '/portal/vehicle-approvals',
    title: 'Vehicle Approvals',
    body: "Approve vehicles your tenants have registered before they're allowed through the gate.",
  },
  {
    route: '/portal/payments',
    target: '/portal/payments',
    title: 'My Payments',
    body: 'See your balance and levies, and submit a payment receipt for the treasurer to confirm.',
  },
  {
    route: '/portal/complaints',
    target: '/portal/complaints',
    title: 'My Complaints',
    body: 'Something not working on the estate? Report it here and track what happens to it.',
  },
  {
    route: '/announcements',
    target: '/announcements',
    title: 'Announcements',
    body: 'Estate-wide notices from management show up here — and the bell icon near the top of the page too.',
  },
]

/** Hidden for a tenant (a landlord-only page in their own sidebar) — see auth/AuthContext.isLandlord. */
const RESIDENT_LANDLORD_ONLY_ROUTES = new Set(['/portal/tenants', '/portal/household-approvals', '/portal/vehicle-approvals'])

const ROLE_LABEL: Partial<Record<Role, string>> = {
  SUPER_ADMIN: 'Super Admin',
  CDA_ADMIN: 'CDA Admin',
  TREASURER: 'Treasurer',
  FINANCIAL_SECRETARY: 'Financial Secretary',
  SECRETARY: 'Secretary',
  SECURITY: 'Security',
  MAINTENANCE: 'Maintenance',
}

/**
 * Builds the ordered step list for whoever's currently logged in — see tour/TourContext for how
 * this gets run (it navigates to each step's `route` before showing it). RESIDENT gets its own
 * hand-written tour (dashboard cards + portal pages, trimmed to what a tenant can actually see);
 * every staff role gets one assembled from ROLE_ROUTES + ROUTE_CONTENT, since those sidebars are
 * just a flat list of estate-wide pages.
 */
export function buildTourSteps(role: Role, isLandlordUser: boolean): TourStep[] {
  if (role === 'RESIDENT') {
    const steps = isLandlordUser
      ? RESIDENT_STEPS
      : RESIDENT_STEPS.filter((s) => s.route === null || !RESIDENT_LANDLORD_ONLY_ROUTES.has(s.route))
    return [welcomeStep('your resident portal'), ...steps, closingStep()]
  }

  const routes = ROLE_ROUTES[role] ?? []
  const steps = routes
    .filter((route) => ROUTE_CONTENT[route])
    .map((route): TourStep => ({ route, target: route, title: ROUTE_CONTENT[route].title, body: ROUTE_CONTENT[route].body }))

  return [welcomeStep(`the ${ROLE_LABEL[role] ?? role} workspace`), ...steps, closingStep()]
}

function welcomeStep(subject: string): TourStep {
  return {
    route: null,
    target: null,
    title: 'Welcome to Nitel Estate',
    body: `Here's a quick look around ${subject}. It only takes a minute, and you can replay it anytime from the "Take a tour" button near the top of the page.`,
  }
}

function closingStep(): TourStep {
  return {
    route: null,
    target: null,
    title: "You're all set",
    body: 'That covers the basics. Look out for the "Take a tour" button near the top of the page if you ever want to see this again.',
  }
}
