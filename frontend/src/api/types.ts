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
export type ResidentStatus = 'ACTIVE' | 'INACTIVE'

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
  block: string
  plot: string
  houseNumber: string
  address: string
  propertyType: PropertyType
  ownerId: number | null
  ownerName: string | null
  occupancyStatus: OccupancyStatus
}

/** What an enforcement officer sees scanning a building's QR pass. */
export interface PropertyLookup {
  id: number
  houseNumber: string
  block: string
  plot: string
  address: string
  propertyType: PropertyType
  occupancyStatus: OccupancyStatus
  ownerId: number | null
  ownerName: string | null
  ownerPhone: string | null
  balance: AccountBalance | null
  levyBreakdown: LevyBalance[]
  recentPayments: Payment[]
}

export type VehicleStatus = 'ACTIVE' | 'INACTIVE'

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
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED'

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
  qrToken: string
  status: VisitorStatus
  /** Base64 data URI, submitted at pass-creation time so security can confirm identity at the gate. */
  photo: string | null
}

/** What security sees scanning a visitor's QR pass — includes the destination to confirm. */
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
}

export type WorkerStatus = 'PENDING' | 'APPROVED' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' | 'COMPLETED'

export interface Worker {
  id: number
  fullName: string
  phone: string
  nationalId: string | null
  contractorName: string
  workType: string
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

export interface Announcement {
  id: number
  title: string
  message: string
  createdByUserId: number
  channels: NotificationChannel[]
  createdAt: string
  read: boolean
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

export type AccessSubjectType = 'RESIDENT' | 'VISITOR' | 'WORKER' | 'VEHICLE'
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
  // Populated for VISITOR rows only.
  subjectName: string | null
  subjectPhone: string | null
  expectedCheckoutAt: string | null
  // Populated for VEHICLE rows only.
  vehiclePlateNumber: string | null
  vehicleMake: string | null
  vehicleModel: string | null
  vehicleColour: string | null
}

export type RfidStatus = 'ACTIVE' | 'LOST' | 'REVOKED'

export interface RfidTag {
  id: number
  tagId: string
  assignedResidentId: number | null
  assignedWorkerId: number | null
  vehicleId: number | null
  status: RfidStatus
}

export interface SecurityDashboard {
  visitorsActive: number
  workersOnSite: number
  registeredVehicles: number
  accountsInArrears: number
}
