export interface TourStep {
  /** Matches a data-tour attribute in the DOM. Null renders a centered card with no spotlight —
   * used only for the welcome/closing steps, which have no single element to point at. */
  target: string | null
  title: string
  body: string
  /** Hidden for a tenant — see auth/AuthContext.isLandlord. Mirrors Layout.NAV_ITEMS'
   * landlordOnly flag for the same nav items, so the tour never points at something the
   * current resident can't actually see in their own sidebar. */
  landlordOnly?: boolean
}

/**
 * The resident onboarding tour — see tour/TourContext for how/when it runs. Steps with a
 * `target` spotlight a `data-tour`-tagged element already on screen (every sidebar link, plus a
 * few Dashboard cards); TourContext.start() always navigates to "/" first so every target here
 * is guaranteed to exist in the DOM somewhere in the sidebar or on the Dashboard itself.
 */
export const TOUR_STEPS: TourStep[] = [
  {
    target: null,
    title: 'Welcome to Nitel Estate',
    body: "Here's a quick look around your resident portal. It only takes a minute, and you can replay it anytime from the \"Take a tour\" button near the top of the page.",
  },
  {
    target: 'dashboard-overview',
    title: 'Your account at a glance',
    body: 'This shows what you currently owe, what you’ve already paid, and how many vehicles you have registered.',
  },
  {
    target: 'dashboard-access-pass',
    title: 'Your access pass',
    body: 'This QR code is yours personally. Show it at the gate and security scans you straight in — no need to stop and sign a book.',
  },
  {
    target: 'dashboard-property',
    title: 'Your property',
    body: 'The house linked to your account. If anything here looks wrong, you can fix it yourself from My Profile.',
  },
  {
    target: '/portal/profile',
    title: 'My Profile',
    body: 'Update your contact details, correct your house number or road, and see your registered vehicles.',
  },
  {
    target: '/portal/visitors',
    title: 'My Visitors',
    body: 'Expecting a guest? Register them here first — they get a QR pass to show security at the gate.',
  },
  {
    target: '/portal/workers',
    title: 'My Workers',
    body: 'Register domestic staff or contractors the same way, so security knows to let them in and out.',
  },
  {
    target: '/portal/household',
    title: 'My Household',
    body: 'Add family members who live with you. Your landlord reviews the request and approves the pass.',
  },
  {
    target: '/portal/tenants',
    title: 'My Tenants',
    body: 'As the property owner, add the tenants living on your property here.',
    landlordOnly: true,
  },
  {
    target: '/portal/household-approvals',
    title: 'Household Approvals',
    body: 'Approve household passes your tenants have requested for their own family members.',
    landlordOnly: true,
  },
  {
    target: '/portal/vehicles',
    title: 'My Vehicles',
    body: 'Register your car here to get it its own QR pass for the gate.',
  },
  {
    target: '/portal/vehicle-approvals',
    title: 'Vehicle Approvals',
    body: "Approve vehicles your tenants have registered before they're allowed through the gate.",
    landlordOnly: true,
  },
  {
    target: '/portal/payments',
    title: 'My Payments',
    body: 'See your balance and levies, and submit a payment receipt for the treasurer to confirm.',
  },
  {
    target: '/portal/complaints',
    title: 'My Complaints',
    body: 'Something not working on the estate? Report it here and track what happens to it.',
  },
  {
    target: '/announcements',
    title: 'Announcements',
    body: 'Estate-wide notices from management show up here — and the bell icon near the top of the page too.',
  },
  {
    target: null,
    title: "You're all set",
    body: 'That covers the basics. Look out for the "Take a tour" button near the top of the page if you ever want to see this again.',
  },
]
