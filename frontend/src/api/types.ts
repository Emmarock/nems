export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type Role =
  | 'SUPER_ADMIN'
  | 'CDA_ADMIN'
  | 'TREASURER'
  | 'FINANCIAL_SECRETARY'
  | 'SECRETARY'
  | 'SECURITY'
  | 'MAINTENANCE'
  | 'RESIDENT'

export interface LoginResponse {
  token: string
  email: string
  fullName: string
  role: Role
  residentId: number | null
  /** Null for non-resident roles — lets the frontend tell a tenant from an owner/landlord. */
  residentType: ResidentType | null
  mustChangePassword: boolean
}

export interface User {
  id: number
  email: string
  phone: string | null
  fullName: string
  role: Role
  status: 'ACTIVE' | 'DISABLED'
  residentId: number | null
}

export type ResidentType = 'OWNER' | 'TENANT' | 'LANDLORD'
export type ResidentStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

export interface Resident {
  id: number
  fullName: string
  phone: string
  email: string | null
  propertyId: number | null
  propertyHouseNumber: string | null
  residentType: ResidentType
  emergencyContact: string | null
  status: ResidentStatus
  registrationDate: string
}

/**
 * A landlord adding a tenant also provisions their login in the same step — the plaintext
 * temporaryPassword is returned here once, same convention as BulkCreatedAccount, so the
 * landlord can hand it to the tenant directly. mustChangePassword is always true on this account.
 */
export interface TenantAddResult {
  resident: Resident
  loginEmail: string | null
  loginPhone: string | null
  temporaryPassword: string | null
}

export interface BulkCreatedAccount {
  residentId: number
  fullName: string
  email: string
  phone: string | null
  temporaryPassword: string
}

export interface BulkCreateResidentUsersResult {
  created: BulkCreatedAccount[]
  alreadyHadAccount: number
}

/** One row of the "Accounts in arrears" drill-through from the security dashboard. */
export interface ResidentArrears {
  id: number
  fullName: string
  phone: string
  propertyId: number | null
  propertyHouseNumber: string | null
  outstanding: number
}

/** What security sees scanning a resident's own QR pass — the resident IS the destination. */
export interface ResidentLookup {
  id: number
  fullName: string
  phone: string
  residentType: ResidentType
  status: ResidentStatus
  flagReason: string | null
  propertyId: number | null
  propertyHouseNumber: string | null
  propertyAddress: string | null
}

export type PropertyType =
  | 'DETACHED_HOUSE'
  | 'SEMI_DETACHED'
  | 'TERRACE'
  | 'BUNGALOW'
  | 'APARTMENT'
  | 'VACANT_LAND'
export type OccupancyStatus = 'OCCUPIED' | 'VACANT' | 'UNDER_CONSTRUCTION'

export interface Property {
  id: number
  /** The estate's own nomenclature — properties are addressed by road, not block/plot. */
  road: string
  houseNumber: string
  address: string
  /** Optional — Nigeria's federal postcode rollout is recent, most properties won't have one yet. */
  postcode: string | null
  propertyType: PropertyType
  ownerId: number | null
  ownerName: string | null
  occupancyStatus: OccupancyStatus
  /** Landlord-configured unit count — caps how many ACTIVE tenants they can add. Null = not configured, no cap. */
  apartmentCount: number | null
}

/** What an enforcement officer sees scanning a building's QR pass. */
export interface PropertyLookup {
  id: number
  houseNumber: string
  road: string
  address: string
  postcode: string | null
  propertyType: PropertyType
  occupancyStatus: OccupancyStatus
  ownerId: number | null
  ownerName: string | null
  ownerPhone: string | null
  balance: AccountBalance | null
  levyBreakdown: LevyBalance[]
  recentPayments: Payment[]
}

export type VehicleStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'INACTIVE'

export interface Vehicle {
  id: number
  plateNumber: string
  vehicleType: string | null
  make: string | null
  model: string | null
  colour: string | null
  residentId: number
  residentName: string | null
  status: VehicleStatus
}

/** What security sees scanning a vehicle's QR pass — identifies it and its owner before granting gate access. */
export interface VehicleLookup {
  id: number
  plateNumber: string
  vehicleType: string | null
  make: string | null
  model: string | null
  colour: string | null
  status: VehicleStatus
  flagReason: string | null
  residentId: number | null
  residentName: string | null
  residentPhone: string | null
  propertyId: number | null
  propertyHouseNumber: string | null
  propertyAddress: string | null
}

export type LevyFrequency = 'ONE_TIME' | 'ANNUAL'

export interface Levy {
  id: number
  name: string
  amount: number
  frequency: LevyFrequency
  active: boolean
  /** Marks this as *the* vehicle sticker fee, if any levy is — see StickerRequest. */
  vehicleStickerLevy: boolean
  /** Which payment account this levy's payments go to — there isn't one shared account. */
  paymentAccountId: number | null
  paymentAccountLabel: string | null
}

export type InvoiceStatus = 'ISSUED' | 'CANCELLED'

export interface Invoice {
  id: number
  residentId: number
  residentName: string | null
  levyId: number
  description: string
  amount: number
  issueDate: string
  dueDate: string
  status: InvoiceStatus
}

export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CHEQUE' | 'ONLINE_GATEWAY'
export type PaymentStatus = 'PENDING' | 'PENDING_APPROVAL' | 'SUCCESS' | 'FAILED' | 'REJECTED'

export interface Payment {
  id: number
  residentId: number
  residentName: string | null
  invoiceId: number | null
  amount: number
  method: PaymentMethod
  provider: string | null
  providerReference: string | null
  status: PaymentStatus
  paidAt: string
  /** A resident-submitted proof of payment (base64 data URI) — set only for the receipt-review flow. */
  receiptImage: string | null
  approvedByUserId: number | null
  approvedByUserName: string | null
  approvedAt: string | null
  reviewNotes: string | null
}

export type SharedReceiptStatus = 'PENDING' | 'SUBMITTED' | 'DISMISSED'

/** What a tenant hands over instead of submitting a receipt directly — see PaymentsPage. */
export interface SharedReceipt {
  id: number
  tenantResidentId: number
  tenantName: string | null
  landlordResidentId: number
  levyId: number
  levyName: string | null
  amount: number
  method: PaymentMethod
  receiptImage: string | null
  status: SharedReceiptStatus
  createdAt: string
}

export interface LevyBalance {
  levyId: number
  levyName: string
  totalDue: number
  totalPaid: number
  outstanding: number
}

export interface AccountBalance {
  residentId: number
  totalDue: number
  totalPaid: number
  penalties: number
  outstanding: number
}

export interface MeDashboard {
  resident: Resident
  property: Property | null
  account: AccountBalance
  vehicles: Vehicle[]
}

export type StickerStatus = 'PENDING_PAYMENT' | 'ISSUED'

export interface StickerRequest {
  id: number
  vehicleId: number
  plateNumber: string | null
  residentId: number
  residentName: string | null
  invoiceId: number
  invoiceAmount: number | null
  status: StickerStatus
  requestedAt: string
  issuedAt: string | null
}

export type VisitorStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED'

export interface Visitor {
  id: number
  name: string
  phone: string
  vehiclePlate: string | null
  hostResidentId: number
  hostResidentName: string | null
  validFrom: string
  validUntil: string
  /** Scanning this always checks the visitor in. */
  qrTokenEntry: string
  /** Null until the visitor's first check-in — see VisitorsPage. Scanning it always checks the visitor out. */
  qrTokenExit: string | null
  status: VisitorStatus
  /** Base64 data URI, submitted at pass-creation time so security can confirm identity at the gate. */
  photo: string | null
}

/** What security sees scanning a visitor's QR pass — includes the destination to confirm, and
 * which direction this particular code is for (fixed by the token, not a choice). */
export interface VisitorLookup {
  id: number
  name: string
  phone: string
  vehiclePlate: string | null
  validFrom: string
  validUntil: string
  status: VisitorStatus
  flagReason: string | null
  photo: string | null
  hostResidentId: number
  hostResidentName: string | null
  hostResidentPhone: string | null
  propertyId: number | null
  propertyHouseNumber: string | null
  propertyAddress: string | null
  direction: AccessDirection
}

/** What a gate scan of a visitor's QR pass actually did. */
export interface VisitorScanResult {
  visitor: Visitor
  direction: AccessDirection
}

export type WorkerStatus = 'PENDING' | 'APPROVED' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' | 'COMPLETED'
/** Chosen at request time — distinct from workType ("Plumbing", "Electrical", ...), which describes the job, not who the worker works for. */
export type WorkerCategory = 'DOMESTIC_STAFF' | 'NON_DOMESTIC_STAFF'

export interface Worker {
  id: number
  fullName: string
  phone: string
  nationalId: string | null
  contractorName: string
  workType: string
  workerCategory: WorkerCategory
  siteId: number | null
  siteHouseNumber: string | null
  sponsorResidentId: number
  startDate: string
  expectedEndDate: string
  status: WorkerStatus
  qrToken: string | null
  /** Base64 data URI, submitted at request time so security can confirm identity at the gate. */
  photo: string | null
}

/** What security sees scanning a worker's QR pass — includes the destination to confirm. */
export interface WorkerLookup {
  id: number
  fullName: string
  phone: string
  contractorName: string
  workType: string
  workerCategory: WorkerCategory
  startDate: string
  expectedEndDate: string
  status: WorkerStatus
  flagReason: string | null
  photo: string | null
  sponsorResidentId: number
  sponsorResidentName: string | null
  sponsorResidentPhone: string | null
  propertyId: number | null
  propertyHouseNumber: string | null
  propertyAddress: string | null
}

export type Relationship = 'SPOUSE' | 'CHILD' | 'PARENT' | 'SIBLING' | 'OTHER'
export type HouseholdMemberStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'REVOKED'

/**
 * A tenant/resident's spouse or other family member — the property's landlord approves the
 * request (via householdMembersApi.approve), issuing a qrToken that stays null until then. There's
 * no validity window: once approved, the pass is valid indefinitely, same as the home owner's own
 * standing, until revoked.
 */
export interface HouseholdMember {
  id: number
  fullName: string
  phone: string
  relationship: Relationship
  propertyId: number
  propertyHouseNumber: string | null
  sponsorResidentId: number
  status: HouseholdMemberStatus
  qrToken: string | null
  /** Base64 data URI, submitted at request time so security can confirm identity at the gate. */
  photo: string | null
}

/** What security sees scanning a household member's QR pass — includes the destination to confirm. */
export interface HouseholdMemberLookup {
  id: number
  fullName: string
  phone: string
  relationship: Relationship
  status: HouseholdMemberStatus
  flagReason: string | null
  photo: string | null
  sponsorResidentId: number
  sponsorResidentName: string | null
  sponsorResidentPhone: string | null
  propertyId: number | null
  propertyHouseNumber: string | null
  propertyAddress: string | null
}

export type ComplaintCategory =
  | 'ELECTRICITY'
  | 'WATER'
  | 'SECURITY'
  | 'WASTE_DISPOSAL'
  | 'ROAD'
  | 'DRAINAGE'
  | 'STREETLIGHT'
  | 'GENERAL'
export type ComplaintStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type ComplaintPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Complaint {
  id: number
  residentId: number
  category: ComplaintCategory
  description: string
  status: ComplaintStatus
  priority: ComplaintPriority
  assignedTo: string | null
  createdAt: string
  resolvedAt: string | null
}

export type NotificationChannel = 'PORTAL' | 'EMAIL' | 'SMS' | 'WHATSAPP'
export type NotificationStatus = 'SENT' | 'FAILED'

/**
 * message is already redacted server-side (passwords, reset tokens, OTP codes all stripped)
 * before this is ever persisted — see Redaction.java. SENT only means the dispatch call didn't
 * throw, not that a carrier actually delivered it — a real provider has to be configured
 * (Twilio/BulkSMS/Resend) for anything to leave the app at all; otherwise every channel logs SENT
 * from its mock fallback without sending anything.
 */
export interface NotificationLog {
  id: number
  channel: NotificationChannel
  recipient: string
  message: string
  status: NotificationStatus
  createdAt: string
}

export interface Announcement {
  id: number
  title: string
  message: string
  createdByUserId: number
  channels: NotificationChannel[]
  createdAt: string
  read: boolean
}

/** A personal, in-app alert for the signed-in user — e.g. "your visitor checked in" — distinct
 * from Announcement, which is estate-wide and every resident sees the same set. */
export interface UserNotification {
  id: number
  title: string
  message: string
  read: boolean
  createdAt: string
}

export interface Dashboard {
  residents: number
  properties: number
  registeredVehicles: number
  totalBilling: number
  collected: number
  outstanding: number
  collectionRatePercent: number
  activeWorkersOnSite: number
  activeVisitorPasses: number
  openComplaints: number
}

export type GateType = 'VEHICLE' | 'PEDESTRIAN'
export type GateStatus = 'ACTIVE' | 'INACTIVE'

export interface Gate {
  id: number
  name: string
  code: string
  location: string | null
  type: GateType
  status: GateStatus
}

export interface AccessPolicy {
  enforceArrears: boolean
  arrearsThreshold: number
}

/** The one bank account residents should pay any levy into — admin-configurable, no per-levy account. */
/** One of the estate's bank accounts — there isn't a single shared account, e.g. Electricity and
 * Development are collected separately (see Levy.paymentAccountId). */
export interface PaymentAccount {
  id: number
  label: string
  bankName: string
  accountNumber: string
  accountName: string
  active: boolean
}

export type ChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type ApprovalDecision = 'APPROVED' | 'REJECTED'

export interface PaymentAccountApproval {
  userId: number
  userName: string | null
  role: Role
  decision: ApprovalDecision
  notes: string | null
  decidedAt: string
}

/** A proposed change to the payment account — see PaymentAccountService: takes effect only once
 * two roles other than proposedByRole have approved it. */
export interface PaymentAccountChange {
  id: number
  /** Null when this proposes creating a brand new account rather than editing one. */
  targetAccountId: number | null
  label: string
  bankName: string
  accountNumber: string
  accountName: string
  proposedByUserId: number
  proposedByUserName: string | null
  proposedByRole: Role
  status: ChangeRequestStatus
  approvals: PaymentAccountApproval[]
  approvalsStillNeeded: number
  createdAt: string
  decidedAt: string | null
}

export type AccessSubjectType = 'RESIDENT' | 'VISITOR' | 'WORKER' | 'VEHICLE' | 'HOUSEHOLD_MEMBER'
export type AccessDirection = 'IN' | 'OUT'

export interface AccessEvent {
  id: number
  subjectType: AccessSubjectType
  subjectId: number
  gateId: number | null
  direction: AccessDirection
  occurredAt: string
  verifiedByUserId: number | null
  flagReason: string | null
  // Populated for VISITOR and WORKER rows (expectedCheckoutAt for VISITOR only).
  subjectName: string | null
  subjectPhone: string | null
  expectedCheckoutAt: string | null
  // Populated for VEHICLE rows only.
  vehiclePlateNumber: string | null
  vehicleMake: string | null
  vehicleModel: string | null
  vehicleColour: string | null
  // The resident tied to this row: host (VISITOR), sponsor (WORKER), or owner (VEHICLE).
  residentName: string | null
}

export type RfidStatus = 'ACTIVE' | 'LOST' | 'REVOKED'

export interface RfidTag {
  id: number
  tagId: string
  assignedResidentId: number | null
  assignedResidentName: string | null
  assignedWorkerId: number | null
  assignedWorkerName: string | null
  assignedVisitorId: number | null
  assignedVisitorName: string | null
  vehicleId: number | null
  vehiclePlateNumber: string | null
  status: RfidStatus
}

export type RfidRequestStatus = 'PENDING' | 'ISSUED' | 'CANCELLED'
/** Only VISITOR and WORKER are valid for an RFID request — reuses AccessSubjectType's wider set. */
export type RfidRequestSubjectType = 'VISITOR' | 'WORKER'

/** A landlord/tenant's request for a physical RFID tag for a visitor or worker without a
 * smartphone to show a QR pass — see pages/security/RfidPage for security's fulfilment queue. */
export interface RfidRequest {
  id: number
  requestedByResidentId: number
  requestedByResidentName: string | null
  subjectType: RfidRequestSubjectType
  subjectId: number
  subjectName: string | null
  subjectPhone: string | null
  status: RfidRequestStatus
  issuedTagId: number | null
  issuedTagTagId: string | null
  createdAt: string
}

export interface SecurityDashboard {
  visitorsActive: number
  workersOnSite: number
  registeredVehicles: number
  accountsInArrears: number
}
